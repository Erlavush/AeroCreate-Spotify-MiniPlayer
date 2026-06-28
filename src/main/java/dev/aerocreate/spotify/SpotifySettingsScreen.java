package dev.aerocreate.spotify;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SpotifySettingsScreen extends Screen {
    private EditBox clientIdBox;
    private EditBox contextUriBox;
    private EditBox callbackBox;

    public SpotifySettingsScreen() {
        super(Component.literal("AeroCreate Spotify"));
    }

    @Override
    protected void init() {
        SpotifyConfig config = SpotifyConfig.load();
        int center = this.width / 2;
        int panelWidth = Math.min(300, this.width - 24);
        int left = center - panelWidth / 2;
        int fieldWidth = panelWidth - 24;
        int y = Math.max(4, this.height / 2 - 116);

        clientIdBox = new EditBox(this.font, left + 12, y + 29, fieldWidth, 20, Component.literal("Spotify Client ID"));
        clientIdBox.setMaxLength(128);
        clientIdBox.setValue(config.getClientId());
        addRenderableWidget(clientIdBox);

        contextUriBox = new EditBox(this.font, left + 12, y + 63, fieldWidth, 20, Component.literal("Minecraft album URI"));
        contextUriBox.setMaxLength(180);
        contextUriBox.setValue(config.getMinecraftContextUri());
        addRenderableWidget(contextUriBox);

        callbackBox = new EditBox(this.font, left + 12, y + 97, fieldWidth, 20, Component.literal("Callback URL or code"));
        callbackBox.setMaxLength(4096);
        addRenderableWidget(callbackBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveConfig())
            .bounds(left + 12, y + 124, 70, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Login"), button -> {
            saveConfig();
            SpotifyController.startLogin();
        }).bounds(left + 88, y + 124, 70, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Finish Login"), button -> SpotifyController.finishLoginFromText(callbackBox.getValue()))
            .bounds(left + 164, y + 124, 112, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Previous"), button -> SpotifyController.previousTrack())
            .bounds(left + 12, y + 148, 82, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Play/Pause"), button -> SpotifyController.togglePlayback())
            .bounds(left + 100, y + 148, 82, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Next"), button -> SpotifyController.nextTrack())
            .bounds(left + 188, y + 148, 88, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Play Minecraft"), button -> {
            saveConfig();
            SpotifyController.playMinecraftAlbum();
        }).bounds(left + 12, y + 172, 112, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Sync"), button -> SpotifyController.sync())
            .bounds(left + 130, y + 172, 70, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> Minecraft.getInstance().setScreen(null))
            .bounds(left + 206, y + 172, 70, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Clear Login"), button -> SpotifyController.clearLogin())
            .bounds(left + 12, y + 196, fieldWidth, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int center = this.width / 2;
        int panelWidth = Math.min(300, this.width - 24);
        int left = center - panelWidth / 2;
        int y = Math.max(4, this.height / 2 - 116);

        graphics.fill(left - 8, y - 4, left + panelWidth + 8, y + 236, 0xD00A0F0B);
        graphics.fill(left - 8, y - 6, left + panelWidth + 8, y - 4, 0xFF54D36A);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, center, y + 6, 0xFFFFFFFF);
        graphics.drawString(this.font, "Spotify Client ID", left + 12, y + 18, 0xFFE8F2E8, true);
        graphics.drawString(this.font, "Minecraft context URI", left + 12, y + 52, 0xFFE8F2E8, true);
        graphics.drawString(this.font, "Callback URL or code", left + 12, y + 86, 0xFFE8F2E8, true);
        graphics.drawCenteredString(this.font, SpotifyController.STATE.getMessage(), center, y + 222, 0xFFB8C4B8);
    }

    private void saveConfig() {
        SpotifyConfig config = SpotifyConfig.load();
        config.setClientId(clientIdBox.getValue());
        config.setMinecraftContextUri(contextUriBox.getValue());
        SpotifyConfig.save(config);
        SpotifyController.STATE.setMessage("Spotify settings saved");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
