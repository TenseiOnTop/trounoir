package net.trounoir.absorption;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.trounoir.TrouNoir;
import net.trounoir.config.TNConfig;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.BonusSet;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.SynergyDef;
import net.trounoir.data.TNData;
import net.trounoir.rebirth.PersistentStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Recalcule tout ce qui découle de l'Absorption : attributs, effets, statistiques du mod,
 * synergies, Fragilité restante, puissance et rang.
 */
public final class Bonuses {
    private Bonuses() {}

    private static final ResourceLocation ID_ADD = TrouNoir.rl("absorption_add");
    private static final ResourceLocation ID_BASE = TrouNoir.rl("absorption_base");
    private static final ResourceLocation ID_TOTAL = TrouNoir.rl("absorption_total");

    /** Accumulateur d'une passe de calcul. */
    private static final class Sum {
        final Map<ResourceLocation, double[]> attributes = new LinkedHashMap<>();
        final Map<ResourceLocation, Integer> effects = new HashMap<>();
        final Map<String, Double> stats = new HashMap<>();
        final Map<String, Double> relief = new HashMap<>();

        void add(BonusSet bonus, double factor, int level) {
            for (BonusSet.AttributeBonus a : bonus.attributes) {
                addAttribute(a.attribute(), a.operation(), a.amount() * factor);
            }
            for (BonusSet.EffectBonus e : bonus.effects) {
                if (level >= e.minLevel()) effects.merge(e.effect(), e.amplifier(), Math::max);
            }
            bonus.stats.forEach((k, v) -> stats.merge(k, v * factor, Double::sum));
            bonus.relief.forEach((k, v) -> relief.merge(k, v * factor, Double::sum));
        }

        void addAttribute(ResourceLocation attribute, AttributeModifier.Operation op, double value) {
            double[] slot = attributes.computeIfAbsent(attribute, k -> new double[3]);
            switch (op) {
                case ADD_VALUE -> slot[0] += value;
                case ADD_MULTIPLIED_BASE -> slot[1] += value;
                case ADD_MULTIPLIED_TOTAL -> slot[2] += value;
            }
        }
    }

    public static void recompute(ServerPlayer player) {
        LifeData life = LifeManager.get(player);
        double decay = TNConfig.LEVEL_DECAY.get();
        Sum sum = new Sum();
        double power = 0;
        Map<String, Integer> tagCounts = new HashMap<>();

        // 1. Ce qui a été absorbé dans cette vie.
        for (Map.Entry<String, Integer> entry : life.xp.entrySet()) {
            AbsorbableDef def = TNData.absorbable(entry.getKey());
            if (def == null) continue;
            int level = def.levelForXp(entry.getValue());
            if (level <= 0) continue;
            double scale = AbsorbableDef.scale(level, decay);
            sum.add(def.bonus, scale, level);
            power += def.power * scale;
            if (def.kind == AbsorbableDef.Kind.TRAIT) {
                for (String tag : def.tags) tagCounts.merge(tag, 1, Integer::sum);
            }
        }

        // 2. Les Échos des vies passées : affaiblis, et sans effet si le trait a été réabsorbé.
        double echoStrength = TNConfig.ECHO_STRENGTH.get();
        int echoMax = TNConfig.ECHO_MAX_LEVEL.get();
        for (PersistentStore.Echo echo : life.echoes) {
            AbsorbableDef def = TNData.absorbable(echo.id);
            if (def == null || life.level(echo.id) > 0) continue;
            int level = Math.max(1, Math.min(Math.min(echo.level, echoMax), def.maxLevel));
            double scale = AbsorbableDef.scale(level, decay) * echoStrength;
            sum.add(def.bonus, scale, level);
            power += def.power * scale;
        }

        // 3. Synergies.
        List<String> active = new ArrayList<>();
        for (SynergyDef synergy : TNData.SYNERGIES.values()) {
            if (isActive(synergy, life, tagCounts)) {
                active.add(synergy.id.toString());
                sum.add(synergy.bonus, 1.0, 99);
                power += synergy.power;
            }
        }

        // 4. Fragilité divine : chaque handicap s'efface à mesure que les traits le soulagent.
        Map<String, Double> remaining = new LinkedHashMap<>();
        double hunger = 0;
        if (!life.fragilityDisabled) {
            for (SimpleDefs.HandicapDef handicap : TNData.HANDICAPS.values()) {
                double got = sum.relief.getOrDefault(handicap.id(), 0.0);
                double left = handicap.needed() <= 0 ? 1.0 : Math.max(0.0, 1.0 - got / handicap.needed());
                remaining.put(handicap.id(), left);
                if (handicap.attribute() != null && left > 0) {
                    sum.addAttribute(handicap.attribute(), handicap.operation(), handicap.amount() * left);
                }
                if (handicap.hunger()) hunger = Math.max(hunger, left);
            }
        }

        applyAttributes(player, life, sum);
        life.stats = sum.stats;
        life.handicapRemaining = remaining;
        life.hungerFactor = hunger;
        life.synergies = active;
        life.power = power;
        life.rank = rankFor(power);
        life.dirty = false;
        PERMANENT_EFFECTS.put(player.getUUID(), sum.effects);
        refreshEffects(player);
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static final Map<java.util.UUID, Map<ResourceLocation, Integer>> PERMANENT_EFFECTS = new HashMap<>();

    public static void forget(ServerPlayer player) {
        PERMANENT_EFFECTS.remove(player.getUUID());
    }

    /** Réapplique les effets passifs (vision nocturne, respiration…) ; appelé toutes les quelques secondes. */
    public static void refreshEffects(ServerPlayer player) {
        Map<ResourceLocation, Integer> effects = PERMANENT_EFFECTS.get(player.getUUID());
        if (effects == null) return;
        for (Map.Entry<ResourceLocation, Integer> e : effects.entrySet()) {
            Optional<Holder.Reference<MobEffect>> holder = BuiltInRegistries.MOB_EFFECT.getHolder(e.getKey());
            if (holder.isPresent()) {
                player.addEffect(new MobEffectInstance(holder.get(), 400, e.getValue(), true, false, false));
            }
        }
    }

    private static boolean isActive(SynergyDef synergy, LifeData life, Map<String, Integer> tagCounts) {
        if (synergy.requiresTags.isEmpty() && synergy.requiresAll.isEmpty()) return false;
        for (Map.Entry<String, Integer> need : synergy.requiresTags.entrySet()) {
            if (tagCounts.getOrDefault(need.getKey(), 0) < need.getValue()) return false;
        }
        for (ResourceLocation id : synergy.requiresAll) {
            if (life.level(id.toString()) <= 0) return false;
        }
        return true;
    }

    private static void applyAttributes(ServerPlayer player, LifeData life, Sum sum) {
        // Retire les modificateurs de la passe précédente.
        for (String previous : life.appliedAttributes) {
            AttributeInstance instance = instance(player, ResourceLocation.tryParse(previous));
            if (instance != null) {
                instance.removeModifier(ID_ADD);
                instance.removeModifier(ID_BASE);
                instance.removeModifier(ID_TOTAL);
            }
        }
        List<String> applied = new ArrayList<>();
        for (Map.Entry<ResourceLocation, double[]> entry : sum.attributes.entrySet()) {
            AttributeInstance instance = instance(player, entry.getKey());
            if (instance == null) continue;
            double[] v = entry.getValue();
            if (v[0] != 0) instance.addOrUpdateTransientModifier(new AttributeModifier(ID_ADD, v[0], AttributeModifier.Operation.ADD_VALUE));
            if (v[1] != 0) instance.addOrUpdateTransientModifier(new AttributeModifier(ID_BASE, v[1], AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if (v[2] != 0) instance.addOrUpdateTransientModifier(new AttributeModifier(ID_TOTAL, v[2], AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            applied.add(entry.getKey().toString());
        }
        life.appliedAttributes = applied;
    }

    private static AttributeInstance instance(ServerPlayer player, ResourceLocation attribute) {
        if (attribute == null) return null;
        Optional<Holder.Reference<Attribute>> holder = BuiltInRegistries.ATTRIBUTE.getHolder(attribute);
        return holder.map(player::getAttribute).orElse(null);
    }

    /** Rang (0 à 10) correspondant à une puissance. */
    public static int rankFor(double power) {
        double[] thresholds = TNConfig.doubles(TNConfig.RANK_POWER_THRESHOLDS);
        int rank = 0;
        for (double t : thresholds) {
            if (power >= t) rank++;
            else break;
        }
        return rank;
    }
}
