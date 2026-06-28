package dev.aerocreate.spotify;

public final class SpotifyState {
    private String message = "Press O to set up Spotify";
    private boolean busy;
    private SpotifyPlayback playback;

    public synchronized String getMessage() {
        return message;
    }

    public synchronized void setMessage(String message) {
        this.message = message == null || message.isBlank() ? "" : message;
    }

    public synchronized boolean isBusy() {
        return busy;
    }

    public synchronized void setBusy(boolean busy) {
        this.busy = busy;
    }

    public synchronized SpotifyPlayback getPlayback() {
        return playback;
    }

    public synchronized void setPlayback(SpotifyPlayback playback) {
        this.playback = playback;
    }
}
