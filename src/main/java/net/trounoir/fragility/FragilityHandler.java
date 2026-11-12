package net.trounoir.fragility;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.Bonuses;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.config.TNConfig;
import net.trounoir.network.Net;
import net.trounoir.util.Fx;

/**
 * La Fragilité divine au quotidien. Les handicaps d'attributs (un seul cœur, lenteur, faiblesse,
 * minage pénible) sont des modificateurs permanents calculés par {@link Bonuses} ; ici vivent
 * ceux qui s'expriment dans le temps (faim) et l'entretien périodique des effets passifs.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FragilityHandler {
    private FragilityHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player) || !player.isAlive()) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        life.ticksLived++;
        if (life.dirty) {
            Bonuses.recompute(player);
            Net.syncLife(player);
        }
        int tick = player.tickCount;
        if (tick % 20 == 0) {
            // Faim accélérée, tant que rien ne l'a soulagée.
            if (life.hungerFactor > 0 && !player.isCreative() && !player.isSpectator()) {
                player.causeFoodExhaustion((float) (TNConfig.HUNGER_EXHAUSTION_PER_SECOND.get() * life.hungerFactor));
            }
        }
        if (tick % 40 == 0) {
            double perception = life.stat("perception_range");
            if (perception > 0) {
                // Perception : les êtres vivants proches se détachent du monde.
                for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(Math.min(128, perception)), Mob::isAlive)) {
                    Fx.effect(mob, "glowing", 60, 0);
                }
            }
        }
        if (tick % 100 == 0) {
            Bonuses.refreshEffects(player);
            double regen = life.stat("regen");
            if (regen > 0 && player.getHealth() < player.getMaxHealth()) player.heal((float) regen);
        }
        if (tick % 600 == 0) LifeManager.save(player);
    }
}
