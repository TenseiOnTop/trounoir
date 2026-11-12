package net.trounoir.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Réglages d'équilibrage principaux (fichier config/trounoir-common.toml).
 * Le contenu lui-même (traits, créatures, boss…) se règle dans les JSON du datapack.
 */
public final class TNConfig {
    private TNConfig() {}

    public enum RebirthMode { AUTO, WORLD_RESET, SOFT }

    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    // --- Absorption ---
    public static final ForgeConfigSpec.DoubleValue LEVEL_DECAY;
    public static final ForgeConfigSpec.DoubleValue SPAWNER_ABSORPTION_CHANCE;
    public static final ForgeConfigSpec.DoubleValue MIN_PLAYER_DAMAGE_SHARE;
    public static final ForgeConfigSpec.IntValue TECHNIQUE_SLOTS_UNUSED;
    // --- Fragilité ---
    public static final ForgeConfigSpec.DoubleValue HUNGER_EXHAUSTION_PER_SECOND;
    // --- Échos ---
    public static final ForgeConfigSpec.DoubleValue ECHO_STRENGTH;
    public static final ForgeConfigSpec.IntValue ECHO_MAX_LEVEL;
    public static final ForgeConfigSpec.DoubleValue ECHO_COOLDOWN_FACTOR;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> ECHO_SLOT_THRESHOLDS;
    // --- Rangs ---
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> RANK_POWER_THRESHOLDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> OUTGOING_GAP_MULTIPLIERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> INCOMING_GAP_MULTIPLIERS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_REDUCTION_CAP;
    public static final ForgeConfigSpec.DoubleValue COOLDOWN_REDUCTION_CAP;
    public static final ForgeConfigSpec.DoubleValue TECHNIQUE_COOLDOWN_SCALE;
    // --- Suke ---
    public static final ForgeConfigSpec.DoubleValue SUKE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SUKE_DAMAGE;
    public static final ForgeConfigSpec.IntValue SUKE_TELEGRAPH_TICKS;
    public static final ForgeConfigSpec.IntValue SUKE_MIN_INTERVAL;
    public static final ForgeConfigSpec.IntValue SUKE_MAX_INTERVAL;
    public static final ForgeConfigSpec.IntValue SUKE_DUEL_RADIUS;
    // --- Monde ---
    public static final ForgeConfigSpec.DoubleValue SITE_DISTANCE_SCALE;
    public static final ForgeConfigSpec.IntValue SITE_BUILD_RADIUS;
    public static final ForgeConfigSpec.IntValue HOSTILE_REPUTATION;
    public static final ForgeConfigSpec.IntValue ALLY_REPUTATION;
    public static final ForgeConfigSpec.BooleanValue HUNTS_ENABLED;
    // --- Renaissance ---
    public static final ForgeConfigSpec.EnumValue<RebirthMode> REBIRTH_MODE;
    public static final ForgeConfigSpec.DoubleValue FAMILIARITY_TELEGRAPH_BONUS;

    public static final ForgeConfigSpec SPEC;

    static {
        B.push("absorption");
        LEVEL_DECAY = B.comment("Rendement décroissant : chaque niveau d'un trait vaut (decay ^ (niveau-1)) fois le premier. 1.0 = linéaire.")
                .defineInRange("levelDecay", 0.7, 0.1, 1.0);
        SPAWNER_ABSORPTION_CHANCE = B.comment("Chance d'absorber une créature issue d'un spawner (0 = jamais).")
                .defineInRange("spawnerAbsorptionChance", 0.0, 0.0, 1.0);
        MIN_PLAYER_DAMAGE_SHARE = B.comment("Part minimale de la vie de la créature qui doit avoir été retirée par le joueur pour qu'elle soit absorbée (anti-ferme).")
                .defineInRange("minPlayerDamageShare", 0.5, 0.0, 1.0);
        TECHNIQUE_SLOTS_UNUSED = B.comment("Réservé. Le nombre d'emplacements de techniques est fixé à 4 (4 touches).")
                .defineInRange("techniqueSlots", 4, 4, 4);
        B.pop();

        B.push("fragilite");
        HUNGER_EXHAUSTION_PER_SECOND = B.comment("Épuisement de faim ajouté chaque seconde tant que le handicap de faim n'est pas soulagé (0.1 ≈ double la faim au repos).")
                .defineInRange("hungerExhaustionPerSecond", 0.12, 0.0, 2.0);
        B.pop();

        B.push("echos");
        ECHO_STRENGTH = B.comment("Force d'un Écho par rapport au trait d'origine (0.5 = moitié).")
                .defineInRange("echoStrength", 0.5, 0.05, 1.0);
        ECHO_MAX_LEVEL = B.comment("Niveau maximal conservé par un Écho.")
                .defineInRange("echoMaxLevel", 2, 1, 10);
        ECHO_COOLDOWN_FACTOR = B.comment("Multiplicateur de recharge d'une technique utilisée seulement grâce à un Écho.")
                .defineInRange("echoCooldownFactor", 2.0, 1.0, 10.0);
        ECHO_SLOT_THRESHOLDS = B.comment("Nombre de vies vécues nécessaires pour chaque emplacement d'Écho (le premier vaut 0).")
                .defineList("echoSlotThresholds", List.of(0, 3, 7, 12, 20, 30), o -> o instanceof Number);
        B.pop();

        B.push("rangs");
        RANK_POWER_THRESHOLDS = B.comment("Puissance requise pour atteindre les rangs I à X.")
                .defineList("rankPowerThresholds", List.of(8, 25, 60, 120, 200, 300, 420, 560, 720, 900), o -> o instanceof Number);
        OUTGOING_GAP_MULTIPLIERS = B.comment("Multiplicateur des dégâts infligés selon (rang attaquant - rang cible), de -3 (ou moins) à +3 (ou plus).")
                .defineList("outgoingGapMultipliers", List.of(0.1, 0.4, 0.75, 1.0, 1.25, 1.6, 2.5), o -> o instanceof Number);
        INCOMING_GAP_MULTIPLIERS = B.comment("Multiplicateur des dégâts subis par le joueur selon (rang attaquant - rang joueur), de -3 à +3.")
                .defineList("incomingGapMultipliers", List.of(0.3, 0.55, 0.8, 1.0, 1.25, 1.6, 2.5), o -> o instanceof Number);
        DAMAGE_REDUCTION_CAP = B.comment("Plafond de la réduction de dégâts cumulée (en %).")
                .defineInRange("damageReductionCap", 60.0, 0.0, 95.0);
        COOLDOWN_REDUCTION_CAP = B.comment("Plafond de la réduction de recharge cumulée (en %).")
                .defineInRange("cooldownReductionCap", 50.0, 0.0, 90.0);
        TECHNIQUE_COOLDOWN_SCALE = B.comment("Multiplicateur global des recharges de techniques.")
                .defineInRange("techniqueCooldownScale", 1.0, 0.1, 10.0);
        B.pop();

        B.push("suke");
        SUKE_THRESHOLD = B.comment("Notoriété à partir de laquelle Suke commence à tirer (0-100).")
                .defineInRange("threshold", 20.0, 0.0, 100.0);
        SUKE_DAMAGE = B.comment("Dégâts d'un tir de Suke avant modificateurs de rang.")
                .defineInRange("damage", 18.0, 1.0, 1000.0);
        SUKE_TELEGRAPH_TICKS = B.comment("Durée du reflet avant le tir (en ticks).")
                .defineInRange("telegraphTicks", 50, 10, 400);
        SUKE_MIN_INTERVAL = B.comment("Intervalle minimal entre deux tirs, à notoriété maximale (ticks).")
                .defineInRange("minInterval", 240, 40, 72000);
        SUKE_MAX_INTERVAL = B.comment("Intervalle entre deux tirs juste au-dessus du seuil (ticks).")
                .defineInRange("maxInterval", 1200, 40, 72000);
        SUKE_DUEL_RADIUS = B.comment("En dessous de cette distance de sa tour, Suke cesse le tir systémique et se bat en duel.")
                .defineInRange("duelRadius", 70, 8, 512);
        B.pop();

        B.push("monde");
        SITE_DISTANCE_SCALE = B.comment("Multiplicateur des distances entre le point de renaissance et les lieux.")
                .defineInRange("siteDistanceScale", 1.0, 0.25, 8.0);
        SITE_BUILD_RADIUS = B.comment("Distance à laquelle un lieu est construit et peuplé.")
                .defineInRange("siteBuildRadius", 150, 48, 512);
        HOSTILE_REPUTATION = B.comment("Réputation en dessous de laquelle une faction attaque à vue.")
                .defineInRange("hostileReputation", -30, -1000, 0);
        ALLY_REPUTATION = B.comment("Réputation à partir de laquelle une faction est comptée comme alliée.")
                .defineInRange("allyReputation", 30, 0, 1000);
        HUNTS_ENABLED = B.comment("Autorise les escouades de traque envoyées après la mort d'un personnage unique.")
                .define("huntsEnabled", true);
        B.pop();

        B.push("renaissance");
        REBIRTH_MODE = B.comment("AUTO : retour dans le passé complet en solo, renaissance douce sur serveur dédié.",
                        "WORLD_RESET : le monde est toujours ramené à son état initial (le serveur dédié s'arrête et doit être relancé).",
                        "SOFT : seul l'état géré par le mod est réinitialisé (le terrain modifié reste).")
                .defineEnum("mode", RebirthMode.AUTO);
        FAMILIARITY_TELEGRAPH_BONUS = B.comment("Allongement des annonces d'attaque d'un boss dont la faiblesse est connue (0.2 = +20 %).")
                .defineInRange("familiarityTelegraphBonus", 0.2, 0.0, 2.0);
        B.pop();

        SPEC = B.build();
    }

    public static double[] doubles(ForgeConfigSpec.ConfigValue<List<? extends Number>> value) {
        List<? extends Number> list = value.get();
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i).doubleValue();
        return out;
    }

    public static List<Integer> ints(ForgeConfigSpec.ConfigValue<List<? extends Number>> value) {
        List<Integer> out = new ArrayList<>();
        for (Number n : value.get()) out.add(n.intValue());
        return out;
    }

    /** Multiplicateur d'écart de rang : gap borné à [-3, 3], table de 7 valeurs. */
    public static double gapMultiplier(ForgeConfigSpec.ConfigValue<List<? extends Number>> table, int gap) {
        double[] t = doubles(table);
        if (t.length != 7) return 1.0;
        return t[Math.max(-3, Math.min(3, gap)) + 3];
    }
}
