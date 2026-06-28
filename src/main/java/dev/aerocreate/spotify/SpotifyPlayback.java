package dev.aerocreate.spotify;

public final class SpotifyPlayback {
    public String trackName = "";
    public String artistName = "";
    public String albumName = "";
    public boolean playing;
    public int progressMs;
    public int durationMs;
    public long syncedAtMillis;

    public boolean hasTrack() {
        return !trackName.isBlank();
    }

    public int currentProgressMs() {
        if (!playing || durationMs <= 0) {
            return progressMs;
        }
        long elapsed = System.currentTimeMillis() - syncedAtMillis;
        return Math.min(durationMs, progressMs + (int) Math.max(0L, elapsed));
    }
}
