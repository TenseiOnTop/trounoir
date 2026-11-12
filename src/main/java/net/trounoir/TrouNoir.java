package net.trounoir;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.trounoir.config.TNConfig;
import net.trounoir.network.Net;
import net.trounoir.registry.ModRegistry;
import org.slf4j.Logger;

/**
 * Point d'entrée du mod Trou-Noir.
 * Chaque système vit dans son propre package et s'abonne lui-même au bus Forge
 * via {@code @Mod.EventBusSubscriber}.
 */
@Mod(TrouNoir.MODID)
public final class TrouNoir {
    public static final String MODID = "trounoir";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TrouNoir(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModRegistry.register(modBus);
        Net.init();
        net.trounoir.world.TestMode.init();
        context.registerConfig(ModConfig.Type.COMMON, TNConfig.SPEC);
    }

    /** Identifiant dans l'espace de noms du mod. */
    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
