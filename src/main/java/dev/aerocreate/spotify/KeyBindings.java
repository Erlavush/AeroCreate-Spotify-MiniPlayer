package dev.aerocreate.spotify;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AeroCreateSpotify.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class KeyBindings {
    private static final String CATEGORY = "key.categories." + AeroCreateSpotify.MOD_ID;

    public static KeyMapping open;
    public static KeyMapping toggleHud;
    public static KeyMapping playPause;
    public static KeyMapping next;
    public static KeyMapping previous;
    public static KeyMapping playMinecraft;

    private KeyBindings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        open = new KeyMapping("key." + AeroCreateSpotify.MOD_ID + ".open", GLFW.GLFW_KEY_O, CATEGORY);
        toggleHud = unbound("toggle");
        playPause = unbound("play_pause");
        next = unbound("next");
        previous = unbound("previous");
        playMinecraft = unbound("minecraft");

        event.register(open);
        event.register(toggleHud);
        event.register(playPause);
        event.register(next);
        event.register(previous);
        event.register(playMinecraft);
    }

    private static KeyMapping unbound(String name) {
        return new KeyMapping(
            "key." + AeroCreateSpotify.MOD_ID + "." + name,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
        );
    }
}
