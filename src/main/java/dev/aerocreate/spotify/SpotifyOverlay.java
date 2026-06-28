package dev.aerocreate.spotify;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class SpotifyOverlay {
    private static final int WIDTH = 152;
    private static final int HEIGHT = 42;

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
            title = trim(minecraft, playback.trackName, 104);
            subtitle = trim(minecraft, playback.artistName, 112);
            progress = playback.currentProgressMs();
            duration = playback.durationMs;
            playing = playback.playing;
        } else {
            title = "AeroCreate Spotify";
            subtitle = trim(minecraft, SpotifyController.STATE.getMessage(), 118);
        }

        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xCC101510);
        graphics.fill(x, y, x + WIDTH, y + 2, 0xFF54D36A);

        String hint = SpotifyController.STATE.isBusy() ? "working..." : "O settings";
        graphics.drawString(minecraft.font, playing ? ">" : "II", x + 8, y + 7, 0xFF54D36A, true);
        graphics.drawString(minecraft.font, title, x + 25, y + 6, 0xFFFFFFFF, true);
        graphics.drawString(minecraft.font, subtitle, x + 25, y + 17, 0xFFB8C4B8, true);
        graphics.drawString(minecraft.font, hint, x + 8, y + 29, 0xFF7FA88A, true);

        if (duration > 0) {
            int barX = x + 70;
            int barY = y + HEIGHT - 8;
            int barWidth = WIDTH - 78;
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
