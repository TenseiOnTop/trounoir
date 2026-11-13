package net.trounoir.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.TrouNoir;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Personnage défini par données : PNJ générique (paysan, chevalier…) ou personnage unique
 * (Supernova, Hypernova, Segaroth, demi-dieu, Dieu). data/&lt;ns&gt;/trounoir/characters/*.json
 */
public final class CharacterDef {
    public static final ResourceLocation DEFAULT_ENTITY = TrouNoir.rl("character");

    public final ResourceLocation id;
    public final boolean unique;
    /** peasant, soldier, knight, mage, king, supernova, hypernova, emperor, demigod, god… */
    public final String category;
    public final String faction;
    public final int rank;
    /** trounoir:character pour un humanoïde du mod, sinon une entité vanilla renforcée (repaires de demi-dieux). */
    public final ResourceLocation baseEntity;
    public final double health;
    public final double armor;
    public final double damage;
    public final double speed;
    public final double scale;
    public final double knockbackResistance;
    public final double followRange;
    public final ResourceLocation texture;
    public final ResourceLocation heldItem;
    /** civilian, guard, hostile, boss, neutral_boss. */
    public final String behavior;
    /** Un civil qui rend les coups quand on l'attaque. */
    public final boolean fightsBack;
    public final List<MoveDef> moves;
    /** Fractions de vie auxquelles le personnage change de phase (ex. [0.66, 0.33]). */
    public final List<Double> phases;
    public final List<SimpleDefs.Grant> grants;
    /** Type de dégâts auquel le personnage est vulnérable : fire, projectile, magic, melee, backstab, explosion. */
    public final String weakness;
    public final double weaknessMultiplier;
    public final Map<String, Double> killReputation;
    public final List<JsonObject> onDeath;
    public final int notoriety;
    public final String language;
    public final int dialogueLines;
    public final boolean bossBar;
    public final boolean hasTitle;
    /** Objets lâchés à la mort (armes signatures). */
    public final List<ResourceLocation> drops;
    /** Nombre d'apparences (skins) parmi lesquelles chaque individu choisit. */
    public final int variants;
    public final JsonObject raw;

    public CharacterDef(ResourceLocation id, JsonObject json) {
        this.id = id;
        this.raw = json;
        this.unique = JsonUtil.bool(json, "unique", false);
        this.category = JsonUtil.str(json, "category", "peasant");
        this.faction = JsonUtil.str(json, "faction", "");
        this.rank = JsonUtil.integer(json, "rank", 1);
        this.baseEntity = JsonUtil.id(JsonUtil.str(json, "base_entity", DEFAULT_ENTITY.toString()));
        this.health = JsonUtil.num(json, "health", 20);
        this.armor = JsonUtil.num(json, "armor", 0);
        this.damage = JsonUtil.num(json, "damage", 3);
        this.speed = JsonUtil.num(json, "speed", 0.28);
        this.scale = JsonUtil.num(json, "scale", 1.0);
        this.knockbackResistance = JsonUtil.num(json, "knockback_resistance", 0);
        this.followRange = JsonUtil.num(json, "follow_range", 24);
        this.texture = JsonUtil.id(JsonUtil.str(json, "texture", "trounoir:textures/entity/character/default.png"));
        String held = JsonUtil.str(json, "held_item", "");
        this.heldItem = held.isEmpty() ? null : JsonUtil.id(held);
        this.behavior = JsonUtil.str(json, "behavior", "civilian");
        this.fightsBack = JsonUtil.bool(json, "fights_back", false);
        List<MoveDef> moves = new ArrayList<>();
        for (JsonObject m : JsonUtil.objects(json, "moves")) moves.add(new MoveDef(m));
        this.moves = List.copyOf(moves);
        List<Double> phases = new ArrayList<>();
        JsonUtil.arr(json, "phases").forEach(e -> phases.add(e.getAsDouble()));
        this.phases = List.copyOf(phases);
        this.grants = SimpleDefs.Grant.parseList(json, "grants");
        this.weakness = JsonUtil.str(json, "weakness", "");
        this.weaknessMultiplier = JsonUtil.num(json, "weakness_multiplier", 1.5);
        this.killReputation = Map.copyOf(JsonUtil.numberMap(json, "kill_reputation"));
        this.onDeath = List.copyOf(JsonUtil.objects(json, "on_death"));
        this.notoriety = JsonUtil.integer(json, "notoriety", 0);
        this.language = JsonUtil.str(json, "language", "");
        this.dialogueLines = JsonUtil.integer(json, "dialogue_lines", 0);
        this.bossBar = JsonUtil.bool(json, "boss_bar", unique);
        this.hasTitle = JsonUtil.bool(json, "title", false);
        this.drops = List.copyOf(JsonUtil.ids(json, "drops"));
        this.variants = Math.max(1, JsonUtil.integer(json, "variants", 1));
    }

    /** Nom affiché au-dessus de la tête et sur la barre de vie : « Nom — Titre » si le personnage a un titre. */
    public net.minecraft.network.chat.Component display() {
        net.minecraft.network.chat.Component name = net.minecraft.network.chat.Component.translatable(nameKey());
        return hasTitle ? net.minecraft.network.chat.Component.translatable("trounoir.name_title", name,
                net.minecraft.network.chat.Component.translatable(titleKey())) : name;
    }

    public boolean isModEntity() {
        return DEFAULT_ENTITY.equals(baseEntity);
    }

    public String nameKey() {
        return "character." + id.getNamespace() + "." + id.getPath();
    }

    public String titleKey() {
        return nameKey() + ".title";
    }

    public boolean alwaysHostile() {
        return behavior.equals("hostile") || behavior.equals("boss");
    }

    public boolean isBoss() {
        return behavior.equals("boss") || behavior.equals("neutral_boss");
    }
}
