package dev.aerocreate.spotify;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SpotifyAlbumArtCache {
    public static final int TEXTURE_SIZE = 64;

    private static final int CENTER = TEXTURE_SIZE / 2;
    private static final double OUTER_RADIUS = 31.25D;
    private static final double ART_RADIUS = 19.75D;
    private static final double HOLE_RADIUS = 2.8D;
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "AeroCreate Spotify Album Art");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();

    private static ResourceLocation placeholder;

    private SpotifyAlbumArtCache() {
    }

    public static ResourceLocation textureFor(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return placeholder();
        }

        Entry entry = CACHE.computeIfAbsent(imageUrl, Entry::new);
        entry.request();
        ResourceLocation location = entry.location;
        return location == null ? placeholder() : location;
    }

    private static ResourceLocation placeholder() {
        if (placeholder == null) {
            placeholder = Minecraft.getInstance()
                .getTextureManager()
                .register("aerocreate_spotify_record", new DynamicTexture(createRecordImage(null)));
        }
        return placeholder;
    }

    private static void load(Entry entry) {
        NativeImage image = null;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(entry.imageUrl))
                .timeout(Duration.ofSeconds(12))
                .GET()
                .build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                entry.failed = true;
                return;
            }

            BufferedImage album = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (album == null) {
                entry.failed = true;
                return;
            }

            image = createRecordImage(album);
            NativeImage finalImage = image;
            image = null;
            Minecraft.getInstance().execute(() -> register(entry, finalImage));
        } catch (Exception e) {
            entry.failed = true;
            AeroCreateSpotify.LOGGER.warn("Failed to load Spotify album art", e);
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    private static void register(Entry entry, NativeImage image) {
        try {
            entry.location = Minecraft.getInstance()
                .getTextureManager()
                .register("aerocreate_spotify_record", new DynamicTexture(image));
        } catch (RuntimeException e) {
            image.close();
            entry.failed = true;
            AeroCreateSpotify.LOGGER.warn("Failed to register Spotify album art texture", e);
        }
    }

    private static NativeImage createRecordImage(BufferedImage album) {
        NativeImage output = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);

        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                double dx = x + 0.5D - CENTER;
                double dy = y + 0.5D - CENTER;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance > OUTER_RADIUS) {
                    output.setPixelRGBA(x, y, 0);
                } else if (distance <= HOLE_RADIUS) {
                    output.setPixelRGBA(x, y, abgr(edgeAlpha(distance, HOLE_RADIUS), 5, 6, 5));
                } else if (distance <= ART_RADIUS) {
                    output.setPixelRGBA(x, y, album == null ? placeholderArt(distance) : sampleAlbum(album, dx, dy));
                } else {
                    output.setPixelRGBA(x, y, vinylPixel(distance, dx, dy));
                }
            }
        }

        return output;
    }

    private static int vinylPixel(double distance, double dx, double dy) {
        int alpha = edgeAlpha(distance, OUTER_RADIUS);
        double groove = Math.sin(distance * 1.65D) * 4.0D + Math.sin(distance * 4.8D) * 1.5D;
        double highlight = Math.max(0.0D, 1.0D - Math.abs((dx + dy + 9.0D) / 18.0D)) * 18.0D;
        int shade = clamp(16 + (int)Math.round(groove + highlight), 7, 58);

        if (Math.abs(distance - ART_RADIUS) < 1.2D) {
            shade = clamp(shade + 26, 0, 96);
        }
        if (distance > OUTER_RADIUS - 1.4D) {
            shade = clamp(shade + 18, 0, 82);
        }

        return abgr(alpha, shade, shade, shade);
    }

    private static int placeholderArt(double distance) {
        int shade = clamp(108 + (int)Math.round((1.0D - distance / ART_RADIUS) * 82.0D), 90, 210);
        return abgr(255, 84, shade, 106);
    }

    private static int sampleAlbum(BufferedImage album, double dx, double dy) {
        int crop = Math.min(album.getWidth(), album.getHeight());
        int cropX = (album.getWidth() - crop) / 2;
        int cropY = (album.getHeight() - crop) / 2;
        double normalizedX = (dx + ART_RADIUS) / (ART_RADIUS * 2.0D);
        double normalizedY = (dy + ART_RADIUS) / (ART_RADIUS * 2.0D);
        int sourceX = cropX + clamp((int)Math.floor(normalizedX * crop), 0, crop - 1);
        int sourceY = cropY + clamp((int)Math.floor(normalizedY * crop), 0, crop - 1);
        return argbToAbgr(album.getRGB(sourceX, sourceY));
    }

    private static int edgeAlpha(double distance, double radius) {
        if (distance <= radius - 1.0D) {
            return 255;
        }
        return clamp((int)Math.round((radius - distance) * 255.0D), 0, 255);
    }

    private static int argbToAbgr(int argb) {
        int alpha = (argb >>> 24) & 255;
        int red = (argb >>> 16) & 255;
        int green = (argb >>> 8) & 255;
        int blue = argb & 255;
        return abgr(alpha, blue, green, red);
    }

    private static int abgr(int alpha, int blue, int green, int red) {
        return (alpha & 255) << 24 | (blue & 255) << 16 | (green & 255) << 8 | red & 255;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Entry {
        private final String imageUrl;
        private volatile ResourceLocation location;
        private volatile boolean requested;
        private volatile boolean failed;

        private Entry(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        private void request() {
            if (requested || failed) {
                return;
            }
            synchronized (this) {
                if (requested || failed) {
                    return;
                }
                requested = true;
                WORKER.submit(() -> load(this));
            }
        }
    }
}
