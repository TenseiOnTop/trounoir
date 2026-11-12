package net.trounoir.technique;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.config.TNConfig;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.TNData;
import net.trounoir.network.Net;

/** Emplacements de techniques, recharges, et suivi des techniques qui durent plusieurs ticks. */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TechniqueHandler {
    private TechniqueHandler() {}

    public static void use(ServerPlayer player, int slot) {
        LifeData life = LifeManager.getOrNull(player);
        if (life == null || slot < 0 || slot >= LifeData.SLOTS) return;
        if (!player.isAlive() || player.isSpectator()) return;
        String id = life.slots[slot];
        if (id == null || id.isEmpty()) return;
        AbsorbableDef def = TNData.absorbable(id);
        if (def == null || def.kind != AbsorbableDef.Kind.TECHNIQUE || !life.has(id)) return;

        long now = player.serverLevel().getGameTime();
        Long ready = life.cooldowns.get(id);
        if (ready != null && ready > now) return;

        if (def.hungerCost > 0 && !player.isCreative()) {
            int food = player.getFoodData().getFoodLevel();
            if (food < def.hungerCost) {
                player.displayClientMessage(Component.translatable("trounoir.technique.hungry"), true);
                return;
            }
            player.getFoodData().setFoodLevel(food - def.hungerCost);
        }

        int level = life.level(id);
        boolean echoOnly = level <= 0;
        if (echoOnly) level = 1;
        double potency = (1.0 + 0.15 * (level - 1)) * (1.0 + life.stat("magic_power") / 100.0);
        if (echoOnly) potency *= 0.5 + 0.5 * TNConfig.ECHO_STRENGTH.get();

        if (!TechniqueExecutor.execute(player, life, def, potency)) return;

        double cdr = Math.min(TNConfig.COOLDOWN_REDUCTION_CAP.get(), life.stat("cooldown_reduction_pct")) / 100.0;
        double cooldown = def.cooldownTicks * TNConfig.TECHNIQUE_COOLDOWN_SCALE.get() * (1.0 - cdr) * (1.0 - 0.06 * (level - 1));
        if (echoOnly) cooldown *= TNConfig.ECHO_COOLDOWN_FACTOR.get();
        life.cooldowns.put(id, now + Math.max(10L, Math.round(cooldown)));
        Net.syncLife(player);
    }

    public static void assign(ServerPlayer player, int slot, String id) {
        LifeData life = LifeManager.getOrNull(player);
        if (life == null || slot < 0 || slot >= LifeData.SLOTS) return;
        if (id.isEmpty()) {
            life.slots[slot] = null;
        } else {
            AbsorbableDef def = TNData.absorbable(id);
            if (def == null || def.kind != AbsorbableDef.Kind.TECHNIQUE || !life.has(id)) return;
            for (int i = 0; i < life.slots.length; i++) {
                if (id.equals(life.slots[i])) life.slots[i] = null;
            }
            life.slots[slot] = id;
        }
        LifeManager.save(player);
        Net.syncLife(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player instanceof ServerPlayer player)) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null || life.dashTicks <= 0) return;
        life.dashTicks--;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(1.3),
                e -> TechniqueExecutor.canHit(player, e))) {
            if (life.dashHit.contains(target.getId())) continue;
            life.dashHit.add(target.getId());
            target.hurt(player.damageSources().playerAttack(player), (float) life.dashDamage);
        }
    }

    /** Œil de Suke : les flèches du joueur partent beaucoup plus vite, donc presque droit et très loin. */
    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()) return;
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer player) {
            LifeData life = LifeManager.getOrNull(player);
            if (life != null && life.stat("sniper") >= 1) {
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(2.5));
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        if (player.serverLevel().getGameTime() < life.noFallUntil) {
            event.setDamageMultiplier(0);
            return;
        }
        double reduction = Math.min(100, life.stat("fall_reduction_pct"));
        if (reduction > 0) event.setDamageMultiplier(event.getDamageMultiplier() * (float) (1.0 - reduction / 100.0));
    }
}
