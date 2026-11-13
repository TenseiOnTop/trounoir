package net.trounoir.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;

/**
 * L'invisibilité est réelle : tant qu'il ne frappe pas, un joueur invisible n'est la cible de personne.
 * Frapper dissipe l'effet (voir RankCombat).
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Stealth {
    private Stealth() {}

    public static boolean hidden(LivingEntity entity) {
        return entity.hasEffect(MobEffects.INVISIBILITY);
    }

    @SubscribeEvent
    public static void onRetarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && hidden(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 0) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!hidden(player)) continue;
            for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.getTarget() == player)) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }
        }
    }
}
