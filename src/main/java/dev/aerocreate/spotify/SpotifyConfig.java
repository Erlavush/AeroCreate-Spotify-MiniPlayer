package dev.aerocreate.spotify;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SpotifyConfig {
    public static final int CALLBACK_PORT = 12589;
    public static final String REDIRECT_URI = "http://127.0.0.1:" + CALLBACK_PORT + "/callback";
    public static final String DEFAULT_MINECRAFT_CONTEXT_URI = "spotify:album:1TtdtRpeNYliHviOnhWdL7";

    private static final String CLIENT_ID = "client_id";
    private static final String OVERLAY_ENABLED = "overlay_enabled";
    private static final String MINECRAFT_CONTEXT_URI = "minecraft_context_uri";
    private static SpotifyConfig cached;

    private String clientId = "";
    private boolean overlayEnabled = true;
    private String minecraftContextUri = DEFAULT_MINECRAFT_CONTEXT_URI;

    private SpotifyConfig() {
    }

    public static synchronized SpotifyConfig load() {
        if (cached != null) {
            return cached;
        }

        SpotifyConfig config = new SpotifyConfig();
        Path path = configPath();

        if (!Files.exists(path)) {
            save(config);
            return config;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            config.clientId = properties.getProperty(CLIENT_ID, "").trim();
            config.overlayEnabled = Boolean.parseBoolean(properties.getProperty(OVERLAY_ENABLED, "true"));
            config.minecraftContextUri = properties.getProperty(MINECRAFT_CONTEXT_URI, DEFAULT_MINECRAFT_CONTEXT_URI).trim();
            if (config.minecraftContextUri.isEmpty()) {
                config.minecraftContextUri = DEFAULT_MINECRAFT_CONTEXT_URI;
            }
        } catch (IOException e) {
            AeroCreateSpotify.LOGGER.warn("Failed to load Spotify config", e);
        }

        cached = config;
        return config;
    }

    public static synchronized void save(SpotifyConfig config) {
        cached = config;
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            properties.setProperty(CLIENT_ID, config.clientId == null ? "" : config.clientId.trim());
            properties.setProperty(OVERLAY_ENABLED, Boolean.toString(config.overlayEnabled));
            properties.setProperty(MINECRAFT_CONTEXT_URI, config.minecraftContextUri == null || config.minecraftContextUri.isBlank()
                ? DEFAULT_MINECRAFT_CONTEXT_URI
                : config.minecraftContextUri.trim());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "AeroCreate Spotify Mini Player");
            }
        } catch (IOException e) {
            AeroCreateSpotify.LOGGER.warn("Failed to save Spotify config", e);
        }
    }

    public static Path configDir() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("aerocreate-spotify");
    }

    public static Path configPath() {
        return configDir().resolve("config.properties");
    }

    public String getClientId() {
        return clientId == null ? "" : clientId.trim();
    }

    public void setClientId(String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
    }

    public boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public void setOverlayEnabled(boolean overlayEnabled) {
        this.overlayEnabled = overlayEnabled;
    }

    public String getMinecraftContextUri() {
        return minecraftContextUri == null || minecraftContextUri.isBlank()
            ? DEFAULT_MINECRAFT_CONTEXT_URI
            : minecraftContextUri.trim();
    }

    public void setMinecraftContextUri(String minecraftContextUri) {
        this.minecraftContextUri = minecraftContextUri == null ? DEFAULT_MINECRAFT_CONTEXT_URI : minecraftContextUri.trim();
    }
}
