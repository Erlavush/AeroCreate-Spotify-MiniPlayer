package dev.aerocreate.spotify;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = AeroCreateSpotify.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        SpotifyController.tick();

        while (KeyBindings.open.consumeClick()) {
            minecraft.setScreen(new SpotifySettingsScreen());
        }

        while (KeyBindings.toggleHud.consumeClick()) {
            SpotifyConfig config = SpotifyConfig.load();
            config.setOverlayEnabled(!config.isOverlayEnabled());
            SpotifyConfig.save(config);
        }

        while (KeyBindings.playPause.consumeClick()) {
            SpotifyController.togglePlayback();
        }

        while (KeyBindings.next.consumeClick()) {
            SpotifyController.nextTrack();
        }

        while (KeyBindings.previous.consumeClick()) {
            SpotifyController.previousTrack();
        }

        while (KeyBindings.playMinecraft.consumeClick()) {
            SpotifyController.playMinecraftAlbum();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        SpotifyOverlay.render(event.getGuiGraphics());
    }
}
