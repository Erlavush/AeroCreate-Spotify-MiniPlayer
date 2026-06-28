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

    public SpotifySettingsScreen() {
        super(Component.literal("AeroCreate Spotify"));
    }

    @Override
    protected void init() {
        SpotifyConfig config = SpotifyConfig.load();
        int center = this.width / 2;
        int y = this.height / 2 - 78;

        clientIdBox = new EditBox(this.font, center - 120, y + 18, 240, 20, Component.literal("Spotify Client ID"));
        clientIdBox.setMaxLength(128);
        clientIdBox.setValue(config.getClientId());
        addRenderableWidget(clientIdBox);

        contextUriBox = new EditBox(this.font, center - 120, y + 58, 240, 20, Component.literal("Minecraft album URI"));
        contextUriBox.setMaxLength(180);
        contextUriBox.setValue(config.getMinecraftContextUri());
        addRenderableWidget(contextUriBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveConfig())
            .bounds(center - 120, y + 88, 76, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Login"), button -> {
            saveConfig();
            SpotifyController.startLogin();
        }).bounds(center - 38, y + 88, 76, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Clear Login"), button -> SpotifyController.clearLogin())
            .bounds(center + 44, y + 88, 76, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Previous"), button -> SpotifyController.previousTrack())
            .bounds(center - 120, y + 116, 76, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Play/Pause"), button -> SpotifyController.togglePlayback())
            .bounds(center - 38, y + 116, 76, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Next"), button -> SpotifyController.nextTrack())
            .bounds(center + 44, y + 116, 76, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Play Minecraft"), button -> {
            saveConfig();
            SpotifyController.playMinecraftAlbum();
        }).bounds(center - 120, y + 144, 117, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Sync"), button -> SpotifyController.sync())
            .bounds(center + 3, y + 144, 56, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> Minecraft.getInstance().setScreen(null))
            .bounds(center + 64, y + 144, 56, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int center = this.width / 2;
        int y = this.height / 2 - 78;

        graphics.drawCenteredString(this.font, this.title, center, y - 8, 0xFFFFFF);
        graphics.drawString(this.font, "Spotify Client ID", center - 120, y + 6, 0xA8D8A8, false);
        graphics.drawString(this.font, "Minecraft context URI", center - 120, y + 46, 0xA8D8A8, false);
        graphics.drawCenteredString(this.font, SpotifyController.STATE.getMessage(), center, y + 174, 0xB8C4B8);
        graphics.drawCenteredString(this.font, "Premium + active Spotify device required", center, y + 188, 0x7FA88A);

        super.render(graphics, mouseX, mouseY, partialTick);
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
