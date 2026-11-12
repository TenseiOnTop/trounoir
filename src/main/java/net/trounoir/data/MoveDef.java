package net.trounoir.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Attaque de personnage : toujours annoncée (telegraph) puis exécutée sur une forme lisible.
 * Les schémas sont donc apprenables d'une vie à l'autre.
 */
public final class MoveDef {
    public enum Shape { CIRCLE, LINE, CONE }

    public record EffectSpec(ResourceLocation effect, int duration, int amplifier) {
        public static List<EffectSpec> parseList(JsonObject json, String key) {
            List<EffectSpec> out = new ArrayList<>();
            for (JsonObject e : JsonUtil.objects(json, key)) {
                out.add(new EffectSpec(JsonUtil.id(JsonUtil.str(e, "effect", "minecraft:slowness")),
                        JsonUtil.integer(e, "duration", 60), JsonUtil.integer(e, "amplifier", 0)));
            }
            return List.copyOf(out);
        }
    }

    public final String id;
    public final int weight;
    public final int cooldown;
    public final double minRange;
    public final double maxRange;
    public final int telegraph;
    public final Shape shape;
    /** Rayon (cercle), portée (cône) ou longueur (ligne). */
    public final double size;
    public final double width;
    public final double angle;
    /** "self" ou "target" : centre des cercles. */
    public final boolean anchorTarget;
    public final int count;
    public final double spread;
    public final double damage;
    public final double knockback;
    public final double pull;
    public final List<EffectSpec> effects;
    /** "none", "teleport" (surgir sur la zone annoncée), "dash" (traverser la ligne), "retreat" (se replier à distance). */
    public final String pre;
    public final String projectile;
    public final int projectileCount;
    public final ResourceLocation summon;
    public final int summonCount;
    public final double heal;
    public final int minPhase;
    public final int maxPhase;
    public final ResourceLocation sound;
    public final ResourceLocation particle;
    public final int timeStopTicks;

    public MoveDef(JsonObject json) {
        this.id = JsonUtil.str(json, "id", "move");
        this.weight = Math.max(1, JsonUtil.integer(json, "weight", 1));
        this.cooldown = JsonUtil.integer(json, "cooldown", 60);
        this.minRange = JsonUtil.num(json, "min_range", 0);
        this.maxRange = JsonUtil.num(json, "max_range", 4);
        this.telegraph = Math.max(4, JsonUtil.integer(json, "telegraph", 20));
        this.shape = switch (JsonUtil.str(json, "shape", "cone")) {
            case "circle" -> Shape.CIRCLE;
            case "line" -> Shape.LINE;
            default -> Shape.CONE;
        };
        this.size = JsonUtil.num(json, "size", 3);
        this.width = JsonUtil.num(json, "width", 1.5);
        this.angle = JsonUtil.num(json, "angle", 90);
        this.anchorTarget = "target".equals(JsonUtil.str(json, "anchor", "self"));
        this.count = Math.max(1, JsonUtil.integer(json, "count", 1));
        this.spread = JsonUtil.num(json, "spread", 6);
        this.damage = JsonUtil.num(json, "damage", 4);
        this.knockback = JsonUtil.num(json, "knockback", 0.4);
        this.pull = JsonUtil.num(json, "pull", 0);
        this.effects = EffectSpec.parseList(json, "effects");
        this.pre = JsonUtil.str(json, "pre", "none");
        this.projectile = JsonUtil.str(json, "projectile", "");
        this.projectileCount = Math.max(1, JsonUtil.integer(json, "projectile_count", 1));
        String s = JsonUtil.str(json, "summon", "");
        this.summon = s.isEmpty() ? null : JsonUtil.id(s);
        this.summonCount = JsonUtil.integer(json, "summon_count", 2);
        this.heal = JsonUtil.num(json, "heal", 0);
        this.minPhase = JsonUtil.integer(json, "min_phase", 0);
        this.maxPhase = JsonUtil.integer(json, "max_phase", 99);
        this.sound = JsonUtil.id(JsonUtil.str(json, "sound", "minecraft:entity.player.attack.sweep"));
        this.particle = JsonUtil.id(JsonUtil.str(json, "particle", "minecraft:crit"));
        this.timeStopTicks = JsonUtil.integer(json, "time_stop", 0);
    }

    /** Frappe de base utilisée par un personnage hostile sans attaque définie. */
    public static MoveDef basic(double damage) {
        JsonObject json = new JsonObject();
        json.addProperty("id", "basic_strike");
        json.addProperty("damage", damage);
        json.addProperty("telegraph", 12);
        json.addProperty("cooldown", 30);
        json.addProperty("size", 2.6);
        json.addProperty("max_range", 2.6);
        return new MoveDef(json);
    }
}
