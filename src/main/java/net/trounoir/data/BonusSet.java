package net.trounoir.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ensemble de bonus passifs décrit en JSON : modificateurs d'attributs, effets permanents,
 * statistiques propres au mod et points de soulagement de la Fragilité divine.
 */
public final class BonusSet {
    public static final BonusSet EMPTY = new BonusSet(List.of(), List.of(), Map.of(), Map.of());

    public record AttributeBonus(ResourceLocation attribute, double amount, AttributeModifier.Operation operation) {}

    public record EffectBonus(ResourceLocation effect, int amplifier, int minLevel) {}

    public final List<AttributeBonus> attributes;
    public final List<EffectBonus> effects;
    /** Statistiques du mod (melee_damage_pct, lifesteal_pct, stealth…), additionnées entre toutes les sources. */
    public final Map<String, Double> stats;
    /** Points de soulagement par handicap de la Fragilité divine. */
    public final Map<String, Double> relief;

    private BonusSet(List<AttributeBonus> attributes, List<EffectBonus> effects, Map<String, Double> stats, Map<String, Double> relief) {
        this.attributes = attributes;
        this.effects = effects;
        this.stats = stats;
        this.relief = relief;
    }

    public static BonusSet parse(JsonObject json) {
        if (json == null || json.size() == 0) return EMPTY;
        List<AttributeBonus> attributes = new ArrayList<>();
        for (JsonObject a : JsonUtil.objects(json, "attributes")) {
            attributes.add(new AttributeBonus(
                    JsonUtil.id(JsonUtil.str(a, "attribute", "minecraft:generic.max_health")),
                    JsonUtil.num(a, "amount", 0),
                    operation(JsonUtil.str(a, "operation", "add_value"))));
        }
        List<EffectBonus> effects = new ArrayList<>();
        for (JsonObject e : JsonUtil.objects(json, "effects")) {
            effects.add(new EffectBonus(
                    JsonUtil.id(JsonUtil.str(e, "effect", "minecraft:night_vision")),
                    JsonUtil.integer(e, "amplifier", 0),
                    JsonUtil.integer(e, "min_level", 1)));
        }
        return new BonusSet(List.copyOf(attributes), List.copyOf(effects),
                Map.copyOf(JsonUtil.numberMap(json, "stats")), Map.copyOf(JsonUtil.numberMap(json, "relief")));
    }

    public static AttributeModifier.Operation operation(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
    }
}
