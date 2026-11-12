package net.trounoir.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.util.JsonUtil;

import java.util.List;
import java.util.Locale;

/**
 * Tout ce que l'Absorption peut donner : un Trait (passif), une Technique (active) ou un Savoir (connaissance).
 * Défini dans data/&lt;ns&gt;/trounoir/traits|techniques|knowledge/*.json.
 */
public final class AbsorbableDef {
    public enum Kind { TRAIT, TECHNIQUE, KNOWLEDGE }

    public final ResourceLocation id;
    public final Kind kind;
    public final List<String> tags;
    public final int maxLevel;
    /** Puissance apportée par niveau (soumise aux rendements décroissants). */
    public final double power;
    public final ResourceLocation icon;
    public final BonusSet bonus;
    /** Capacité signature d'un personnage unique. */
    public final boolean signature;

    // Technique
    public final String techniqueType;
    public final int cooldownTicks;
    public final int hungerCost;
    public final JsonObject params;

    // Savoir
    public final String knowledgeType;
    public final String target;
    public final List<ResourceLocation> recipes;

    /** JSON d'origine, renvoyé tel quel au client pour l'affichage. */
    public final JsonObject raw;

    public AbsorbableDef(ResourceLocation id, Kind kind, JsonObject json) {
        this.id = id;
        this.kind = kind;
        this.raw = json;
        this.tags = List.copyOf(JsonUtil.strings(json, "tags"));
        this.maxLevel = kind == Kind.KNOWLEDGE ? 1 : Math.max(1, JsonUtil.integer(json, "max_level", 1));
        this.power = JsonUtil.num(json, "power", kind == Kind.KNOWLEDGE ? 0 : 1);
        this.icon = JsonUtil.id(JsonUtil.str(json, "icon", "minecraft:nether_star"));
        this.bonus = BonusSet.parse(JsonUtil.obj(json, "bonus"));
        this.signature = JsonUtil.bool(json, "signature", false);
        this.techniqueType = JsonUtil.str(json, "type", "");
        this.cooldownTicks = (int) Math.round(JsonUtil.num(json, "cooldown", 10) * 20);
        this.hungerCost = JsonUtil.integer(json, "hunger_cost", 0);
        this.params = JsonUtil.obj(json, "params");
        this.knowledgeType = JsonUtil.str(json, "knowledge_type", "lore");
        this.target = JsonUtil.str(json, "target", "");
        this.recipes = List.copyOf(JsonUtil.ids(json, "recipes"));
    }

    /** Clé de traduction du nom : trait.trounoir.x, technique.trounoir.x ou knowledge.trounoir.x. */
    public String nameKey() {
        return kind.name().toLowerCase(Locale.ROOT) + "." + id.getNamespace() + "." + id.getPath();
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }

    /** Nombre total d'absorptions nécessaires pour atteindre un niveau : 1, 3, 6, 10, 15… */
    public static int xpForLevel(int level) {
        return level * (level + 1) / 2;
    }

    public int levelForXp(int xp) {
        int level = 0;
        while (level < maxLevel && xpForLevel(level + 1) <= xp) level++;
        return level;
    }

    /** Valeur cumulée d'un niveau avec rendements décroissants : 1 + d + d² + … */
    public static double scale(int level, double decay) {
        double total = 0;
        double step = 1;
        for (int i = 0; i < level; i++) {
            total += step;
            step *= decay;
        }
        return total;
    }
}
