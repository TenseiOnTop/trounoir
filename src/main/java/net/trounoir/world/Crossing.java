package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Fx;

/**
 * La Traversée : le passage maritime entre Nébulosia (Overworld) et Diviniarth (dimension trounoir:diviniarth).
 * On l'accomplit en saisissant le gouvernail d'un navire, dans un port.
 */
public final class Crossing {
    private Crossing() {}

    public static final ResourceLocation DIVINIARTH = TrouNoir.rl("diviniarth");

    public static void cross(ServerPlayer player) {
        ResourceLocation here = player.level().dimension().location();
        SimpleDefs.SiteDef destination = null;
        for (SimpleDefs.SiteDef def : TNData.SITES.values()) {
            if (def.crossing() && !def.dimension().equals(here)) {
                destination = def;
                break;
            }
        }
        if (destination == null) return;
        ServerLevel target = SiteManager.level(player.server, destination.dimension());
        if (target == null) {
            player.displayClientMessage(Component.translatable("trounoir.crossing.unavailable"), true);
            return;
        }
        StructureBuilders.Built built = SiteManager.ensureBuilt(target, destination);
        if (built == null) return;
        BlockPos arrival = built.arrival();

        Fx.effect(player, "blindness", 80, 0);
        Fx.effect(player, "slowness", 60, 3);
        boolean toDiviniarth = destination.dimension().equals(DIVINIARTH);
        player.teleportTo(target, arrival.getX() + 0.5, arrival.getY() + 0.1, arrival.getZ() + 0.5, 180F, 0F);
        Fx.title(player, Component.translatable(toDiviniarth ? "trounoir.crossing.diviniarth" : "trounoir.crossing.nebulosia"),
                Component.translatable("trounoir.crossing.subtitle"), 70);
        Fx.playTo(player, "ambient.underwater.exit", 1.0F, 0.6F);

        PersistentStore store = PersistentStore.get(player.server);
        store.memory(player.getUUID()).places.add(destination.id().toString());
        store.save();
        LifeData life = LifeManager.getOrNull(player);
        if (life != null && toDiviniarth && !life.reachedDiviniarth) {
            life.reachedDiviniarth = true;
            life.feat("feat.trounoir.crossed");
        }
    }
}
