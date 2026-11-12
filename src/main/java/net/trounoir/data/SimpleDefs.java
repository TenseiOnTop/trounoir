package net.trounoir.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

/** Petites définitions de données regroupées : étiquettes, handicaps, profils d'absorption, factions, lieux. */
public final class SimpleDefs {
    private SimpleDefs() {}

    /** Étiquette de trait (lame, feu, ombre…) : sert aux synergies et à la « voie dominante » d'une vie. */
    public record TagDef(String id, int color, JsonObject raw) {
        public static TagDef parse(ResourceLocation id, JsonObject json) {
            return new TagDef(id.getPath(), JsonUtil.color(JsonUtil.str(json, "color", "#ffffff"), 0xFFFFFF), json);
        }

        public String nameKey() {
            return "tntag.trounoir." + id;
        }
    }

    /** Un handicap de la Fragilité divine. « needed » points de soulagement l'effacent entièrement. */
    public record HandicapDef(String id, ResourceLocation attribute, double amount, AttributeModifier.Operation operation,
                              double needed, boolean hunger) {
        public static HandicapDef parse(ResourceLocation id, JsonObject json) {
            String attr = JsonUtil.str(json, "attribute", "");
            return new HandicapDef(id.getPath(), attr.isEmpty() ? null : JsonUtil.id(attr), JsonUtil.num(json, "amount", 0),
                    BonusSet.operation(JsonUtil.str(json, "operation", "add_value")),
                    Math.max(0, JsonUtil.num(json, "relief_needed", 0)), JsonUtil.bool(json, "hunger", false));
        }
    }

    /** Ce qu'une mise à mort peut donner. */
    public record Grant(ResourceLocation id, double chance) {
        public static List<Grant> parseList(JsonObject json, String key) {
            List<Grant> out = new ArrayList<>();
            for (JsonObject g : JsonUtil.objects(json, key)) {
                out.add(new Grant(JsonUtil.id(JsonUtil.str(g, "id", "")), JsonUtil.num(g, "chance", 1.0)));
            }
            for (String s : JsonUtil.strings(json, key)) out.add(new Grant(JsonUtil.id(s), 1.0));
            return List.copyOf(out);
        }
    }

    /** Profil d'absorption d'un type d'entité (tous les mobs vanilla en ont un). */
    public record ProfileDef(ResourceLocation entity, int rank, List<Grant> grants, String faction, int reputation) {
        public static ProfileDef parse(ResourceLocation id, JsonObject json) {
            return new ProfileDef(JsonUtil.id(JsonUtil.str(json, "entity", "minecraft:" + id.getPath())),
                    JsonUtil.integer(json, "rank", 1), Grant.parseList(json, "grants"),
                    JsonUtil.str(json, "faction", ""), JsonUtil.integer(json, "kill_reputation", 0));
        }
    }

    public record SquadEntry(ResourceLocation character, int count) {
        public static List<SquadEntry> parseList(JsonObject json, String key) {
            List<SquadEntry> out = new ArrayList<>();
            for (JsonObject s : JsonUtil.objects(json, key)) {
                out.add(new SquadEntry(JsonUtil.id(JsonUtil.str(s, "character", "")), Math.max(1, JsonUtil.integer(s, "count", 1))));
            }
            return List.copyOf(out);
        }
    }

    /** Faction : réputation, escouade de traque. */
    public record FactionDef(String id, List<SquadEntry> huntSquad, int color) {
        public static FactionDef parse(ResourceLocation id, JsonObject json) {
            return new FactionDef(id.getPath(), SquadEntry.parseList(json, "hunt_squad"),
                    JsonUtil.color(JsonUtil.str(json, "color", "#ffffff"), 0xFFFFFF));
        }

        public String nameKey() {
            return "faction.trounoir." + id;
        }
    }

    /** Lieu construit par le mod : position (distance et angle depuis l'origine du continent), plan, habitants. */
    public record SiteDef(ResourceLocation id, ResourceLocation dimension, String blueprint, double distance, double angle,
                          boolean crossing, List<SquadEntry> residents, ResourceLocation primary, ResourceLocation secondary,
                          ResourceLocation accent, String faction) {
        public static SiteDef parse(ResourceLocation id, JsonObject json) {
            JsonObject palette = JsonUtil.obj(json, "palette");
            return new SiteDef(id, JsonUtil.id(JsonUtil.str(json, "dimension", "minecraft:overworld")),
                    JsonUtil.str(json, "blueprint", "village"), JsonUtil.num(json, "distance", 200), JsonUtil.num(json, "angle", 0),
                    JsonUtil.bool(json, "crossing", false), SquadEntry.parseList(json, "residents"),
                    JsonUtil.id(JsonUtil.str(palette, "primary", "minecraft:stone_bricks")),
                    JsonUtil.id(JsonUtil.str(palette, "secondary", "minecraft:oak_planks")),
                    JsonUtil.id(JsonUtil.str(palette, "accent", "minecraft:lantern")),
                    JsonUtil.str(json, "faction", ""));
        }

        public String nameKey() {
            return "site." + id.getNamespace() + "." + id.getPath();
        }
    }
}
