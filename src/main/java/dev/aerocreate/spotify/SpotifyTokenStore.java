package dev.aerocreate.spotify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public final class SpotifyTokenStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Token cachedToken;

    private SpotifyTokenStore() {
    }

    public static synchronized Token load() {
        if (cachedToken != null) {
            return cachedToken;
        }

        Path path = tokenPath();
        if (!Files.exists(path)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            cachedToken = GSON.fromJson(reader, Token.class);
            if (cachedToken == null || cachedToken.accessToken == null || cachedToken.refreshToken == null) {
                cachedToken = null;
            }
            return cachedToken;
        } catch (IOException e) {
            AeroCreateSpotify.LOGGER.warn("Failed to load Spotify token file", e);
            return null;
        }
    }

    public static synchronized void save(Token token) {
        cachedToken = token;
        Path path = tokenPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(token, writer);
            }
            restrictOwnerOnly(path);
        } catch (IOException e) {
            AeroCreateSpotify.LOGGER.warn("Failed to save Spotify token file", e);
        }
    }

    public static synchronized void clear() {
        cachedToken = null;
        try {
            Files.deleteIfExists(tokenPath());
        } catch (IOException e) {
            AeroCreateSpotify.LOGGER.warn("Failed to delete Spotify token file", e);
        }
    }

    public static synchronized boolean hasToken() {
        return load() != null;
    }

    private static Path tokenPath() {
        return SpotifyConfig.configDir().resolve("spotify_tokens.json");
    }

    private static void restrictOwnerOnly(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows and some filesystems do not expose POSIX permissions.
        }
    }

    public static final class Token {
        public String accessToken;
        public String refreshToken;
        public long expiresAtMillis;

        public Token() {
        }

        public Token(String accessToken, String refreshToken, long expiresAtMillis) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAtMillis = expiresAtMillis;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAtMillis - 60_000L;
        }
    }
}
