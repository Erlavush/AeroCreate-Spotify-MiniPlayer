package dev.aerocreate.spotify;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(value = AeroCreateSpotify.MOD_ID, dist = Dist.CLIENT)
public final class AeroCreateSpotify {
    public static final String MOD_ID = "aerocreate_spotify";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroCreateSpotify() {
    }
}
