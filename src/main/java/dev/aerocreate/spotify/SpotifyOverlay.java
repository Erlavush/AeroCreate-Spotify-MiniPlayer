package dev.aerocreate.spotify;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class SpotifyOverlay {
    private static final int DISC_SIZE = SpotifyAlbumArtCache.TEXTURE_SIZE;
    private static final int TEXT_WIDTH = 134;
    private static final int TEXT_HEIGHT = 50;

    private SpotifyOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !SpotifyConfig.load().isOverlayEnabled()) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int discX = screenWidth - DISC_SIZE;
        int discY = screenHeight - DISC_SIZE;
        int panelX = Math.max(0, screenWidth - DISC_SIZE - TEXT_WIDTH + 8);
        int panelY = Math.max(0, screenHeight - TEXT_HEIGHT);
        int panelRight = Math.min(screenWidth, discX + 18);

        SpotifyPlayback playback = SpotifyController.STATE.getPlayback();
        String title;
        String subtitle;
        String status;
        int progress = 0;
        int duration = 0;
        boolean playing = false;
        String imageUrl = "";

        if (playback != null && playback.hasTrack()) {
            int textMaxWidth = Math.max(48, panelRight - panelX - 16);
            title = trim(minecraft, playback.trackName, textMaxWidth);
            subtitle = trim(minecraft, playback.artistName, textMaxWidth);
            status = playback.playing ? "Playing" : "Paused";
            progress = playback.currentProgressMs();
            duration = playback.durationMs;
            playing = playback.playing;
            imageUrl = playback.albumImageUrl;
        } else {
            title = "AeroCreate Spotify";
            subtitle = trim(minecraft, SpotifyController.STATE.getMessage(), Math.max(48, panelRight - panelX - 16));
            status = SpotifyController.STATE.isBusy() ? "Working" : "Press O";
        }

        graphics.fill(panelX, panelY, panelRight, screenHeight, 0xD00A100C);
        graphics.fill(panelX, panelY, panelRight, panelY + 2, 0xFF54D36A);
        graphics.fill(panelRight - 1, panelY + 2, panelRight, screenHeight, 0x7054D36A);

        int textX = panelX + 8;
        graphics.drawString(minecraft.font, title, textX, panelY + 7, 0xFFFFFFFF, true);
        graphics.drawString(minecraft.font, subtitle, textX, panelY + 19, 0xFFB8C4B8, true);
        graphics.drawString(minecraft.font, status, textX, panelY + 32, playing ? 0xFF54D36A : 0xFF7FA88A, true);

        if (duration > 0) {
            int barX = textX;
            int barY = screenHeight - 4;
            int barWidth = Math.max(0, panelRight - barX - 8);
            graphics.fill(barX, barY, barX + barWidth, barY + 2, 0xFF384438);
            int filled = Math.max(0, Math.min(barWidth, (int) (barWidth * (progress / (float) duration))));
            graphics.fill(barX, barY, barX + filled, barY + 2, 0xFF54D36A);
        }

        ResourceLocation texture = SpotifyAlbumArtCache.textureFor(imageUrl);
        renderDisc(graphics, texture, discX, discY, rotationDegrees(playing, playback != null && playback.hasTrack()));
    }

    private static void renderDisc(GuiGraphics graphics, ResourceLocation texture, int x, int y, float degrees) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(x + DISC_SIZE / 2.0F, y + DISC_SIZE / 2.0F, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        graphics.pose().translate(-(x + DISC_SIZE / 2.0F), -(y + DISC_SIZE / 2.0F), 0.0F);
        graphics.blit(texture, x, y, 0, 0.0F, 0.0F, DISC_SIZE, DISC_SIZE, DISC_SIZE, DISC_SIZE);
        graphics.pose().popPose();
    }

    private static float rotationDegrees(boolean playing, boolean hasTrack) {
        long period = playing ? 5200L : hasTrack ? 12000L : 8500L;
        return (System.currentTimeMillis() % period) * 360.0F / period;
    }

    private static String trim(Minecraft minecraft, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (minecraft.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        for (int i = text.length(); i > 0; i--) {
            String candidate = text.substring(0, i).trim() + suffix;
            if (minecraft.font.width(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return suffix;
    }
}
