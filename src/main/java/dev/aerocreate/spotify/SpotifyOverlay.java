package dev.aerocreate.spotify;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class SpotifyOverlay {
    private static final int WIDTH = 190;
    private static final int HEIGHT = 56;

    private SpotifyOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !SpotifyConfig.load().isOverlayEnabled()) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int x = screenWidth - WIDTH - 10;
        int y = screenHeight - HEIGHT - 10;

        SpotifyPlayback playback = SpotifyController.STATE.getPlayback();
        String title;
        String subtitle;
        int progress = 0;
        int duration = 0;
        boolean playing = false;

        if (playback != null && playback.hasTrack()) {
            title = trim(minecraft, playback.trackName, 118);
            subtitle = trim(minecraft, playback.artistName, 118);
            progress = playback.currentProgressMs();
            duration = playback.durationMs;
            playing = playback.playing;
        } else {
            title = "AeroCreate Spotify";
            subtitle = trim(minecraft, SpotifyController.STATE.getMessage(), 150);
        }

        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xCC101510);
        graphics.fill(x, y, x + 3, y + HEIGHT, 0xFF54D36A);
        graphics.fill(x + 10, y + 10, x + 42, y + 42, 0xFF243224);
        graphics.drawString(minecraft.font, "SP", x + 19, y + 22, 0xFF54D36A, false);

        graphics.drawString(minecraft.font, playing ? ">" : "||", x + 49, y + 8, 0xFF54D36A, false);
        graphics.drawString(minecraft.font, title, x + 66, y + 8, 0xFFFFFFFF, false);
        graphics.drawString(minecraft.font, subtitle, x + 66, y + 21, 0xFFB8C4B8, false);

        String hint = SpotifyController.STATE.isBusy() ? "working..." : "O settings";
        graphics.drawString(minecraft.font, hint, x + 66, y + 35, 0xFF7FA88A, false);

        if (duration > 0) {
            int barX = x + 10;
            int barY = y + HEIGHT - 6;
            int barWidth = WIDTH - 20;
            graphics.fill(barX, barY, barX + barWidth, barY + 2, 0xFF384438);
            int filled = Math.max(0, Math.min(barWidth, (int) (barWidth * (progress / (float) duration))));
            graphics.fill(barX, barY, barX + filled, barY + 2, 0xFF54D36A);
        }
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
