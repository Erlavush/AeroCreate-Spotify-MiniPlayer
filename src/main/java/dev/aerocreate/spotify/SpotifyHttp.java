package dev.aerocreate.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.StringJoiner;

public final class SpotifyHttp {
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private SpotifyHttp() {
    }

    public static SpotifyTokenStore.Token exchangeCode(String clientId, String code, String codeVerifier) throws IOException, InterruptedException {
        String body = form(
            "client_id", clientId,
            "grant_type", "authorization_code",
            "code", code,
            "redirect_uri", SpotifyConfig.REDIRECT_URI,
            "code_verifier", codeVerifier
        );
        JsonObject json = postToken(body);
        return tokenFromJson(json, null);
    }

    public static SpotifyTokenStore.Token refresh(String clientId, SpotifyTokenStore.Token oldToken) throws IOException, InterruptedException {
        String body = form(
            "client_id", clientId,
            "grant_type", "refresh_token",
            "refresh_token", oldToken.refreshToken
        );
        JsonObject json = postToken(body);
        return tokenFromJson(json, oldToken.refreshToken);
    }

    public static JsonObject getJson(String endpoint) throws IOException, InterruptedException, SpotifyApiException {
        HttpResponse<String> response = sendWithAuth("GET", endpoint, null);
        return parseNullableJson(response.body());
    }

    public static JsonObject putJson(String endpoint, String body) throws IOException, InterruptedException, SpotifyApiException {
        HttpResponse<String> response = sendWithAuth("PUT", endpoint, body == null ? "" : body);
        return parseNullableJson(response.body());
    }

    public static JsonObject postJson(String endpoint) throws IOException, InterruptedException, SpotifyApiException {
        HttpResponse<String> response = sendWithAuth("POST", endpoint, "");
        return parseNullableJson(response.body());
    }

    private static HttpResponse<String> sendWithAuth(String method, String endpoint, String body)
        throws IOException, InterruptedException, SpotifyApiException {
        SpotifyTokenStore.Token token = ensureFreshToken();
        HttpResponse<String> response = send(method, endpoint, body, token.accessToken);

        if (response.statusCode() == 401) {
            token = forceRefreshToken();
            response = send(method, endpoint, body, token.accessToken);
        }

        if (response.statusCode() == 204) {
            return response;
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SpotifyApiException(response.statusCode(), readableError(response.body()));
        }

        return response;
    }

    private static SpotifyTokenStore.Token ensureFreshToken() throws IOException, InterruptedException, SpotifyApiException {
        SpotifyTokenStore.Token token = SpotifyTokenStore.load();
        if (token == null) {
            throw new SpotifyApiException(0, "Login required");
        }
        if (!token.isExpired()) {
            return token;
        }
        return forceRefreshToken();
    }

    private static synchronized SpotifyTokenStore.Token forceRefreshToken() throws IOException, InterruptedException, SpotifyApiException {
        SpotifyConfig config = SpotifyConfig.load();
        SpotifyTokenStore.Token token = SpotifyTokenStore.load();
        if (token == null) {
            throw new SpotifyApiException(0, "Login required");
        }
        if (config.getClientId().isBlank()) {
            throw new SpotifyApiException(0, "Spotify Client ID missing");
        }
        try {
            SpotifyTokenStore.Token refreshed = refresh(config.getClientId(), token);
            SpotifyTokenStore.save(refreshed);
            return refreshed;
        } catch (IOException e) {
            throw e;
        }
    }

    private static HttpResponse<String> send(String method, String endpoint, String body, String accessToken)
        throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(API_BASE + endpoint))
            .timeout(Duration.ofSeconds(12))
            .header("Authorization", "Bearer " + accessToken);

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }

        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject postToken(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL))
            .timeout(Duration.ofSeconds(12))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Spotify token request failed: " + readableError(response.body()));
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static SpotifyTokenStore.Token tokenFromJson(JsonObject json, String fallbackRefreshToken) {
        String accessToken = json.get("access_token").getAsString();
        String refreshToken = fallbackRefreshToken;
        if (json.has("refresh_token") && !json.get("refresh_token").isJsonNull()) {
            refreshToken = json.get("refresh_token").getAsString();
        }
        int expiresIn = json.has("expires_in") ? json.get("expires_in").getAsInt() : 3600;
        long expiresAt = System.currentTimeMillis() + Math.max(60, expiresIn - 120) * 1000L;
        return new SpotifyTokenStore.Token(accessToken, refreshToken, expiresAt);
    }

    private static JsonObject parseNullableJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonElement element = JsonParser.parseString(body);
        return element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String readableError(String body) {
        if (body == null || body.isBlank()) {
            return "Empty Spotify response";
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) {
                JsonElement error = json.get("error");
                if (error.isJsonObject()) {
                    JsonObject errorObject = error.getAsJsonObject();
                    String status = errorObject.has("status") ? errorObject.get("status").getAsString() + " " : "";
                    String message = errorObject.has("message") ? errorObject.get("message").getAsString() : body;
                    return status + message;
                }
                return error.getAsString();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    public static String form(String... pairs) {
        StringJoiner joiner = new StringJoiner("&");
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            joiner.add(urlEncode(pairs[i]) + "=" + urlEncode(pairs[i + 1]));
        }
        return joiner.toString();
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public static final class SpotifyApiException extends Exception {
        private final int statusCode;

        public SpotifyApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
