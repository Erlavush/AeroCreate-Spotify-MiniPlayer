# AeroCreate Spotify Mini Player

Client-only NeoForge `1.21.1` Spotify remote mini-player for the AeroCreate pack.

This mod does not stream Spotify audio inside Minecraft. It controls the player's active
Spotify app/device through the Spotify Web API.

## Requirements

- Spotify Premium, including Premium Student.
- A Spotify app with Web API enabled.
- Redirect URI set exactly to:
  `http://127.0.0.1:12589/callback`
- The Spotify app owner must have Premium.
- In Spotify Development Mode, the account using the mod must be allowlisted in the app.
- Spotify must be open on at least one device before playback commands work.

## Usage

1. Put `aerocreate-spotify-miniplayer-1.0.1.jar` in the client `mods/` folder only.
2. Launch Minecraft.
3. Press `O`.
4. Paste the Spotify app Client ID.
5. Click `Save`, then `Login`.
6. Authorize Spotify in the browser.
7. Open Spotify on desktop, browser, or phone so there is an active device.

If the browser redirects to `127.0.0.1` but shows "refused to connect", copy the full
URL from the browser address bar, paste it into the `Callback URL or code` field in
Minecraft, and click `Finish Login`.

The lower-right HUD shows the current Spotify state. The settings screen can play,
pause, skip, sync, clear login, or start Minecraft - Volume Alpha by C418.

## Files

- Config: `.minecraft/aerocreate-spotify/config.properties`
- Token store: `.minecraft/aerocreate-spotify/spotify_tokens.json`

Do not share the token file. If login behaves strangely, delete
`spotify_tokens.json` and log in again.

## Build

```bash
./gradlew clean jar
```

The jar is produced at:

```text
build/libs/aerocreate-spotify-miniplayer-1.0.1.jar
```
