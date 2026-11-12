package net.trounoir.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.character.Characters;
import net.trounoir.config.TNConfig;
import net.trounoir.data.CharacterDef;
import net.trounoir.util.Fx;

/**
 * Le combat selon les rangs : l'écart de puissance se ressent dans les deux sens.
 * Un ennemi trop fort est visiblement intouchable ; un Trou-Noir devenu puissant balaye ce qui le menaçait.
 * Applique aussi les statistiques du mod (dégâts, réduction, vol de vie, touchers élémentaires…).
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RankCombat {
    private RankCombat() {}

    /** Nature d'un coup, utilisée pour les statistiques et les faiblesses : fire, projectile, explosion, magic, melee, other. */
    public static String kindOf(DamageSource source, Entity attacker) {
        if (source.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) return "magic";
        if (attacker != null && source.getDirectEntity() == attacker) return "melee";
        return "other";
    }

    private static boolean isBehind(Entity attacker, LivingEntity victim) {
        Vec3 facing = victim.getLookAngle();
        Vec3 to = attacker.position().subtract(victim.position());
        double len = Math.sqrt(to.x * to.x + to.z * to.z);
        if (len < 1.0E-3) return false;
        return (facing.x * to.x + facing.z * to.z) / len < -0.3;
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life != null && life.stat("fire_immunity") >= 1 && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            player.clearFire();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        double amount = event.getAmount();

        if (attacker instanceof ServerPlayer player && victim != player) {
            amount = outgoing(player, victim, source, amount);
        }
        if (victim instanceof ServerPlayer player) {
            amount = incoming(player, attacker, source, amount);
        } else if (attacker instanceof LivingEntity living && !(attacker instanceof ServerPlayer)) {
            // Créature contre créature : l'écart de rang compte aussi (un chevalier ne fait qu'égratigner un Dieu).
            amount *= TNConfig.gapMultiplier(TNConfig.OUTGOING_GAP_MULTIPLIERS, Ranks.of(living) - Ranks.of(victim));
        }
        event.setAmount((float) Math.max(0, amount));
    }

    private static double outgoing(ServerPlayer player, LivingEntity victim, DamageSource source, double amount) {
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return amount;
        String kind = kindOf(source, player);
        boolean behind = isBehind(player, victim);
        double bonus = 1.0;
        if (kind.equals("melee")) bonus += life.stat("melee_damage_pct") / 100.0;
        if (kind.equals("projectile")) bonus += life.stat("ranged_damage_pct") / 100.0;
        if (behind && !kind.equals("fire")) bonus += life.stat("backstab_pct") / 100.0;
        bonus += life.stat("all_damage_pct") / 100.0;

        CharacterDef def = Characters.defOf(victim);
        if (def != null && !def.weakness.isEmpty()) {
            boolean exposed = def.weakness.equals(kind)
                    || (def.weakness.equals("fire") && victim.isOnFire())
                    || (def.weakness.equals("backstab") && behind);
            if (exposed) bonus *= def.weaknessMultiplier;
        }

        int gap = life.rank - Ranks.of(victim);
        double gapMultiplier = TNConfig.gapMultiplier(TNConfig.OUTGOING_GAP_MULTIPLIERS, gap);
        double result = amount * bonus * gapMultiplier;

        ServerLevel level = player.serverLevel();
        if (gap <= -3) {
            // Intouchable : le coup sonne creux. Le joueur comprend sans qu'on le lui explique.
            Vec3 at = victim.position().add(0, victim.getBbHeight() * 0.6, 0);
            Fx.play(level, at, "block.anvil.land", 0.25F, 1.9F);
            Fx.burst(level, "enchanted_hit", at, 6, 0.3, 0.05);
            long now = level.getGameTime();
            if (now - life.lastUntouchableNotice > 100) {
                life.lastUntouchableNotice = now;
                player.displayClientMessage(Component.translatable("trounoir.combat.untouchable"), true);
            }
        }

        if (kind.equals("melee")) {
            if (life.stat("fire_touch") >= 1) victim.igniteForSeconds(3.0F);
            if (life.stat("frost_touch") >= 1) Fx.effect(victim, "slowness", 60, 1);
            if (life.stat("poison_touch") >= 1) Fx.effect(victim, "poison", 80, 0);
            if (life.stat("wither_touch") >= 1) Fx.effect(victim, "wither", 80, 0);
        }
        double lifesteal = life.stat("lifesteal_pct");
        if (lifesteal > 0 && !kind.equals("fire")) player.heal((float) Math.min(4.0, result * lifesteal / 100.0));
        return result;
    }

    private static double incoming(ServerPlayer player, Entity attacker, DamageSource source, double amount) {
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return amount;
        if (attacker instanceof LivingEntity living && attacker != player) {
            amount *= TNConfig.gapMultiplier(TNConfig.INCOMING_GAP_MULTIPLIERS, Ranks.of(living) - life.rank);
            double thorns = life.stat("thorns");
            if (thorns > 0 && source.getDirectEntity() == attacker) {
                living.hurt(player.damageSources().thorns(player), (float) thorns);
            }
        }
        double reduction = Math.min(TNConfig.DAMAGE_REDUCTION_CAP.get(), life.stat("damage_reduction_pct"));
        amount *= 1.0 - reduction / 100.0;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            amount *= 1.0 - Math.min(90.0, life.stat("explosion_reduction_pct")) / 100.0;
        }
        return amount;
    }

    /** Discrétion : les créatures repèrent le joueur de moins loin. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        double stealth = Math.min(80.0, life.stat("stealth"));
        if (stealth > 0) event.modifyVisibility(1.0 - stealth / 100.0);
    }
}
