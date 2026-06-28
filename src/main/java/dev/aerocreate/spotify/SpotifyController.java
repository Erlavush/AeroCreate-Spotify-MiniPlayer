package dev.aerocreate.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SpotifyController {
    public static final SpotifyState STATE = new SpotifyState();
    private static final String SCOPES = "user-read-playback-state user-read-currently-playing user-modify-playback-state";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "AeroCreate Spotify");
        thread.setDaemon(true);
        return thread;
    });

    private static LocalCallbackServer callbackServer;
    private static String pendingCodeVerifier;
    private static String pendingState;
    private static int ticksUntilSync;

    private SpotifyController() {
    }

    public static void tick() {
        if (ticksUntilSync-- <= 0) {
            ticksUntilSync = 100;
            if (SpotifyTokenStore.hasToken() && !STATE.isBusy()) {
                sync();
            }
        }
    }

    public static void startLogin() {
        SpotifyConfig config = SpotifyConfig.load();
        if (config.getClientId().isBlank()) {
            STATE.setMessage("Enter a Spotify Client ID first");
            return;
        }

        run("Opening Spotify login", () -> {
            pendingCodeVerifier = randomString(64);
            pendingState = UUID.randomUUID().toString();
            String challenge = codeChallenge(pendingCodeVerifier);
            restartCallbackServer();

            String authUrl = "https://accounts.spotify.com/authorize"
                + "?response_type=code"
                + "&client_id=" + SpotifyHttp.urlEncode(config.getClientId())
                + "&scope=" + SpotifyHttp.urlEncode(SCOPES)
                + "&code_challenge_method=S256"
                + "&code_challenge=" + SpotifyHttp.urlEncode(challenge)
                + "&redirect_uri=" + SpotifyHttp.urlEncode(SpotifyConfig.REDIRECT_URI)
                + "&state=" + SpotifyHttp.urlEncode(pendingState);

            Util.getPlatform().openUri(URI.create(authUrl));
            STATE.setMessage("Authorize Spotify in browser");
        });
    }

    public static void handleCallback(String code, String state, String error) {
        finishLogin(code, state, error, true);
    }

    public static void finishLoginFromText(String input) {
        Map<String, String> query = parseCallbackInput(input);
        finishLogin(query.get("code"), query.get("state"), query.get("error"), false);
    }

    public static void handleCallbackTimeout() {
        if (pendingCodeVerifier != null) {
            STATE.setMessage("Spotify login timed out. Try Login again.");
            pendingCodeVerifier = null;
            pendingState = null;
        }
    }

    private static void finishLogin(String code, String state, String error, boolean requireState) {
        run("Finishing Spotify login", () -> {
            if (error != null && !error.isBlank()) {
                STATE.setMessage("Spotify login denied: " + error);
                return;
            }
            if (code == null || code.isBlank()) {
                STATE.setMessage("Paste the callback URL or code first");
                return;
            }
            if (pendingCodeVerifier == null) {
                STATE.setMessage("Click Login before finishing auth");
                return;
            }
            if (pendingState == null || (requireState && (state == null || state.isBlank()))) {
                STATE.setMessage("Spotify login failed: state mismatch");
                return;
            }
            if (state != null && !state.isBlank() && !pendingState.equals(state)) {
                STATE.setMessage("Spotify login failed: state mismatch");
                return;
            }

            SpotifyConfig config = SpotifyConfig.load();
            SpotifyTokenStore.Token token = SpotifyHttp.exchangeCode(config.getClientId(), code, pendingCodeVerifier);
            SpotifyTokenStore.save(token);
            pendingCodeVerifier = null;
            pendingState = null;
            stopCallbackServer();
            STATE.setMessage("Spotify linked. Open Spotify on a device.");
            syncBlocking();
        });
    }

    public static void sync() {
        run("Syncing Spotify", SpotifyController::syncBlocking);
    }

    public static void togglePlayback() {
        run("Toggling playback", () -> {
            SpotifyPlayback playback = STATE.getPlayback();
            if (playback != null && playback.playing) {
                SpotifyHttp.putJson("/me/player/pause", "");
                STATE.setMessage("Paused Spotify");
            } else {
                SpotifyHttp.putJson("/me/player/play", "");
                STATE.setMessage("Playing Spotify");
            }
            syncBlocking();
        });
    }

    public static void nextTrack() {
        run("Skipping track", () -> {
            SpotifyHttp.postJson("/me/player/next");
            STATE.setMessage("Skipped track");
            sleepBriefly();
            syncBlocking();
        });
    }

    public static void previousTrack() {
        run("Previous track", () -> {
            SpotifyHttp.postJson("/me/player/previous");
            STATE.setMessage("Previous track");
            sleepBriefly();
            syncBlocking();
        });
    }

    public static void playMinecraftAlbum() {
        run("Playing Minecraft Volume Alpha", () -> {
            String contextUri = SpotifyConfig.load().getMinecraftContextUri();
            SpotifyHttp.putJson("/me/player/play", "{\"context_uri\":\"" + escapeJson(contextUri) + "\"}");
            STATE.setMessage("Playing Minecraft Volume Alpha");
            sleepBriefly();
            syncBlocking();
        });
    }

    public static void clearLogin() {
        SpotifyTokenStore.clear();
        stopCallbackServer();
        pendingCodeVerifier = null;
        pendingState = null;
        STATE.setPlayback(null);
        STATE.setMessage("Spotify login cleared");
    }

    private static void syncBlocking() throws IOException, InterruptedException, SpotifyHttp.SpotifyApiException {
        SpotifyConfig config = SpotifyConfig.load();
        if (config.getClientId().isBlank()) {
            STATE.setPlayback(null);
            STATE.setMessage("Press O and enter a Spotify Client ID");
            return;
        }
        if (!SpotifyTokenStore.hasToken()) {
            STATE.setPlayback(null);
            STATE.setMessage("Press O to link Spotify");
            return;
        }

        JsonObject json = SpotifyHttp.getJson("/me/player");
        if (json == null || !json.has("item") || json.get("item").isJsonNull()) {
            STATE.setPlayback(null);
            STATE.setMessage("Open Spotify on a device first");
            return;
        }

        SpotifyPlayback playback = parsePlayback(json);
        STATE.setPlayback(playback);
        STATE.setMessage(playback.playing ? "Spotify playing" : "Spotify paused");
    }

    private static SpotifyPlayback parsePlayback(JsonObject json) {
        JsonObject item = json.getAsJsonObject("item");
        SpotifyPlayback playback = new SpotifyPlayback();
        playback.trackName = string(item, "name");
        playback.durationMs = integer(item, "duration_ms");
        playback.progressMs = integer(json, "progress_ms");
        playback.playing = bool(json, "is_playing");
        playback.syncedAtMillis = System.currentTimeMillis();

        if (item.has("artists") && item.get("artists").isJsonArray()) {
            JsonArray artists = item.getAsJsonArray("artists");
            if (!artists.isEmpty() && artists.get(0).isJsonObject()) {
                playback.artistName = string(artists.get(0).getAsJsonObject(), "name");
            }
        }

        if (item.has("album") && item.get("album").isJsonObject()) {
            JsonObject album = item.getAsJsonObject("album");
            playback.albumName = string(album, "name");
            playback.albumImageUrl = albumImageUrl(album);
        }

        return playback;
    }

    private static void restartCallbackServer() throws IOException {
        if (callbackServer != null) {
            callbackServer.stop();
        }
        callbackServer = new LocalCallbackServer(SpotifyConfig.CALLBACK_PORT);
        callbackServer.start();
    }

    private static void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop();
            callbackServer = null;
        }
    }

    private static Map<String, String> parseCallbackInput(String input) {
        Map<String, String> result = new HashMap<>();
        if (input == null || input.isBlank()) {
            return result;
        }

        String text = input.trim();
        int question = text.indexOf('?');
        if (question >= 0 && question + 1 < text.length()) {
            text = text.substring(question + 1);
        }
        int fragment = text.indexOf('#');
        if (fragment >= 0) {
            text = text.substring(0, fragment);
        }

        if (!text.contains("=")) {
            result.put("code", text);
            return result;
        }

        for (String pair : text.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                String key = URLDecoder.decode(pair.substring(0, equals), java.nio.charset.StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(equals + 1), java.nio.charset.StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }

    private static void run(String busyMessage, SpotifyTask task) {
        STATE.setBusy(true);
        STATE.setMessage(busyMessage);
        WORKER.submit(() -> {
            try {
                task.run();
            } catch (SpotifyHttp.SpotifyApiException e) {
                STATE.setMessage(messageForSpotifyError(e));
            } catch (Exception e) {
                STATE.setMessage("Spotify error: " + e.getMessage());
                AeroCreateSpotify.LOGGER.warn("Spotify mini-player task failed", e);
            } finally {
                STATE.setBusy(false);
            }
        });
    }

    private static String messageForSpotifyError(SpotifyHttp.SpotifyApiException e) {
        if (e.getStatusCode() == 403) {
            return "Spotify rejected request. Check Premium and app allowlist.";
        }
        if (e.getStatusCode() == 404) {
            return "No active Spotify device";
        }
        if (e.getStatusCode() == 401) {
            return "Spotify login expired. Re-link in settings.";
        }
        return e.getMessage();
    }

    private static String randomString(int length) {
        SecureRandom random = new SecureRandom();
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private static String codeChallenge(String verifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private static int integer(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
    }

    private static boolean bool(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
    }

    private static String albumImageUrl(JsonObject album) {
        if (!album.has("images") || !album.get("images").isJsonArray()) {
            return "";
        }

        String fallback = "";
        String best = "";
        int bestSize = Integer.MAX_VALUE;
        JsonArray images = album.getAsJsonArray("images");
        for (JsonElement element : images) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject image = element.getAsJsonObject();
            String url = string(image, "url");
            if (url.isBlank()) {
                continue;
            }
            if (fallback.isBlank()) {
                fallback = url;
            }

            int size = Math.max(integer(image, "width"), integer(image, "height"));
            if (size >= 128 && size < bestSize) {
                best = url;
                bestSize = size;
            }
        }

        return best.isBlank() ? fallback : best;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sleepBriefly() throws InterruptedException {
        Thread.sleep(600L);
    }

    @FunctionalInterface
    private interface SpotifyTask {
        void run() throws Exception;
    }
}
