package net.trounoir.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.TrouNoir;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.SynergyDef;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ce que le client sait : définitions reçues du serveur et état de la vie en cours. */
public final class ClientState {
    private ClientState() {}

    public record CharacterInfo(ResourceLocation texture, int rank, String category, String weakness, int variants) {}

    public record EchoInfo(String id, int level) {}

    public record PlaceInfo(String id, int x, int y, int z, String dimension) {}

    private static final ResourceLocation DEFAULT_TEXTURE = TrouNoir.rl("textures/entity/character/default.png");

    // --- Définitions ---
    public static final Map<String, AbsorbableDef> ABSORBABLES = new LinkedHashMap<>();
    public static final Map<String, SimpleDefs.TagDef> TAGS = new LinkedHashMap<>();
    public static final Map<String, SynergyDef> SYNERGIES = new LinkedHashMap<>();
    public static final Map<String, CharacterInfo> CHARACTERS = new LinkedHashMap<>();

    // --- Vie en cours ---
    public static int lifeNumber = 1;
    public static String lifeName = "";
    public static int deaths;
    public static double power;
    public static int rank;
    public static double nextRank = -1;
    public static double notoriety;
    public static boolean revealed;
    public static boolean sukeAlive = true;
    public static int echoSlots = 1;
    public static final Map<String, Integer> XP = new LinkedHashMap<>();
    public static final String[] SLOTS = new String[4];
    /** identifiant -> tick client auquel la technique redevient disponible. */
    public static final Map<String, Long> COOLDOWN_END = new LinkedHashMap<>();
    public static final Map<String, Long> COOLDOWN_TOTAL = new LinkedHashMap<>();
    public static final List<EchoInfo> ECHOES = new ArrayList<>();
    public static final List<String> ACTIVE_SYNERGIES = new ArrayList<>();
    public static final Map<String, Double> HANDICAPS = new LinkedHashMap<>();
    public static final List<String> KNOWLEDGE = new ArrayList<>();
    public static final List<String> WEAKNESSES = new ArrayList<>();
    public static final List<PlaceInfo> PLACES = new ArrayList<>();
    /** Route choisie (identifiant de lieu), ou nul. */
    public static String road;

    public static PlaceInfo roadPlace() {
        if (road == null || net.minecraft.client.Minecraft.getInstance().level == null) return null;
        String dim = net.minecraft.client.Minecraft.getInstance().level.dimension().location().toString();
        for (PlaceInfo p : PLACES) if (p.id().equals(road) && p.dimension().equals(dim)) return p;
        return null;
    }

    public static String siteName(String id) {
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        return rl == null ? id : "site." + rl.getNamespace() + "." + rl.getPath();
    }
    public static final Map<String, String> RELICS = new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> VARIANT_TEXTURES = new java.util.HashMap<>();

    // --- Mort et Chronique ---
    public static JsonObject death;
    public static JsonObject chronicle;
    /** Compteur de ticks client, base des recharges affichées. */
    public static long clientTick;

    public static void reset() {
        road = null;
        XP.clear();
        COOLDOWN_END.clear();
        COOLDOWN_TOTAL.clear();
        ECHOES.clear();
        ACTIVE_SYNERGIES.clear();
        HANDICAPS.clear();
        KNOWLEDGE.clear();
        WEAKNESSES.clear();
        PLACES.clear();
        for (int i = 0; i < SLOTS.length; i++) SLOTS[i] = null;
        lifeNumber = 1;
        lifeName = "";
        power = 0;
        rank = 0;
        notoriety = 0;
        revealed = false;
        death = null;
        chronicle = null;
    }

    static {
        // L'onglet créatif liste ce que le client connaît (reçu du serveur), y compris sur un serveur dédié.
        net.trounoir.registry.ModRegistry.characterIds = () -> {
            List<ResourceLocation> ids = new ArrayList<>();
            for (String id : CHARACTERS.keySet()) ids.add(JsonUtil.id(id));
            return ids;
        };
        net.trounoir.registry.ModRegistry.absorbables = () -> new ArrayList<>(ABSORBABLES.values());
    }

    public static void readDefs(JsonObject json) {
        ABSORBABLES.clear();
        TAGS.clear();
        SYNERGIES.clear();
        CHARACTERS.clear();
        for (Map.Entry<String, JsonElement> e : JsonUtil.obj(json, "absorbables").entrySet()) {
            JsonObject entry = e.getValue().getAsJsonObject();
            AbsorbableDef.Kind kind;
            try {
                kind = AbsorbableDef.Kind.valueOf(JsonUtil.str(entry, "kind", "TRAIT"));
            } catch (IllegalArgumentException ex) {
                kind = AbsorbableDef.Kind.TRAIT;
            }
            ABSORBABLES.put(e.getKey(), new AbsorbableDef(JsonUtil.id(e.getKey()), kind, JsonUtil.obj(entry, "def")));
        }
        for (Map.Entry<String, JsonElement> e : JsonUtil.obj(json, "tags").entrySet()) {
            TAGS.put(e.getKey(), SimpleDefs.TagDef.parse(TrouNoir.rl(e.getKey()), e.getValue().getAsJsonObject()));
        }
        for (Map.Entry<String, JsonElement> e : JsonUtil.obj(json, "synergies").entrySet()) {
            SYNERGIES.put(e.getKey(), new SynergyDef(JsonUtil.id(e.getKey()), e.getValue().getAsJsonObject()));
        }
        for (Map.Entry<String, JsonElement> e : JsonUtil.obj(json, "characters").entrySet()) {
            JsonObject c = e.getValue().getAsJsonObject();
            CHARACTERS.put(e.getKey(), new CharacterInfo(JsonUtil.id(JsonUtil.str(c, "texture", DEFAULT_TEXTURE.toString())),
                    JsonUtil.integer(c, "rank", 1), JsonUtil.str(c, "category", ""), JsonUtil.str(c, "weakness", ""), JsonUtil.integer(c, "variants", 1)));
        }
    }

    public static void readLife(JsonObject json) {
        lifeNumber = JsonUtil.integer(json, "life", 1);
        lifeName = JsonUtil.str(json, "name", "");
        deaths = JsonUtil.integer(json, "deaths", 0);
        power = JsonUtil.num(json, "power", 0);
        rank = JsonUtil.integer(json, "rank", 0);
        nextRank = JsonUtil.num(json, "nextRank", -1);
        notoriety = JsonUtil.num(json, "notoriety", 0);
        revealed = JsonUtil.bool(json, "revealed", false);
        sukeAlive = JsonUtil.bool(json, "sukeAlive", true);
        echoSlots = JsonUtil.integer(json, "echoSlots", 1);

        XP.clear();
        JsonUtil.numberMap(json, "xp").forEach((k, v) -> XP.put(k, (int) Math.round(v)));
        int i = 0;
        for (String s : JsonUtil.strings(json, "slots")) {
            if (i < SLOTS.length) SLOTS[i++] = s.isEmpty() ? null : s;
        }
        COOLDOWN_END.clear();
        JsonUtil.numberMap(json, "cooldowns").forEach((k, v) -> {
            long left = Math.round(v);
            COOLDOWN_END.put(k, clientTick + left);
            COOLDOWN_TOTAL.merge(k, left, Math::max);
        });
        COOLDOWN_TOTAL.keySet().retainAll(COOLDOWN_END.keySet());
        ECHOES.clear();
        for (JsonObject e : JsonUtil.objects(json, "echoes")) {
            ECHOES.add(new EchoInfo(JsonUtil.str(e, "id", ""), JsonUtil.integer(e, "level", 1)));
        }
        ACTIVE_SYNERGIES.clear();
        ACTIVE_SYNERGIES.addAll(JsonUtil.strings(json, "synergies"));
        HANDICAPS.clear();
        HANDICAPS.putAll(JsonUtil.numberMap(json, "handicaps"));
        KNOWLEDGE.clear();
        KNOWLEDGE.addAll(JsonUtil.strings(json, "knowledge"));
        WEAKNESSES.clear();
        WEAKNESSES.addAll(JsonUtil.strings(json, "weaknesses"));
        RELICS.clear();
        for (Map.Entry<String, JsonElement> e : JsonUtil.obj(json, "relics").entrySet()) RELICS.put(e.getKey(), e.getValue().getAsString());
        PLACES.clear();
        for (Map.Entry<String, JsonElement> e : JsonUtil.obj(json, "places").entrySet()) {
            JsonObject place = e.getValue().getAsJsonObject();
            List<Integer> pos = new ArrayList<>();
            JsonUtil.arr(place, "pos").forEach(p -> pos.add(p.getAsInt()));
            if (pos.size() == 3) PLACES.add(new PlaceInfo(e.getKey(), pos.get(0), pos.get(1), pos.get(2), JsonUtil.str(place, "dimension", "")));
        }
    }

    /** Niveau absorbé dans cette vie. */
    public static int level(String id) {
        AbsorbableDef def = ABSORBABLES.get(id);
        Integer xp = XP.get(id);
        return def == null || xp == null ? 0 : def.levelForXp(xp);
    }

    public static EchoInfo echo(String id) {
        for (EchoInfo e : ECHOES) if (e.id().equals(id)) return e;
        return null;
    }

    public static boolean has(String id) {
        return level(id) > 0 || echo(id) != null;
    }

    /** Fraction de recharge restante, de 0 (prête) à 1. */
    public static float cooldownFraction(String id) {
        Long end = COOLDOWN_END.get(id);
        if (end == null || end <= clientTick) return 0F;
        long total = Math.max(1L, COOLDOWN_TOTAL.getOrDefault(id, end - clientTick));
        return Math.min(1F, (end - clientTick) / (float) total);
    }

    /** Texture d'un individu : les PNJ ordinaires ont plusieurs apparences, choisies d'après leur identifiant unique. */
    public static ResourceLocation textureOf(String characterId, java.util.UUID uuid) {
        CharacterInfo info = CHARACTERS.get(characterId);
        if (info == null) return DEFAULT_TEXTURE;
        int variant = info.variants() <= 1 ? 0 : Math.floorMod(uuid.hashCode(), info.variants());
        if (variant == 0) return info.texture();
        String key = characterId + "#" + variant;
        return VARIANT_TEXTURES.computeIfAbsent(key, k -> ResourceLocation.fromNamespaceAndPath(info.texture().getNamespace(),
                info.texture().getPath().replace(".png", "_" + variant + ".png")));
    }

    public static int tagColor(String tag) {
        SimpleDefs.TagDef def = TAGS.get(tag);
        return def == null ? 0xDDDDDD : def.color();
    }

    public static boolean hasAny(AbsorbableDef.Kind kind) {
        for (String id : XP.keySet()) {
            AbsorbableDef def = ABSORBABLES.get(id);
            if (def != null && def.kind == kind && level(id) > 0) return true;
        }
        if (kind == AbsorbableDef.Kind.KNOWLEDGE) return !KNOWLEDGE.isEmpty() || !PLACES.isEmpty() || !WEAKNESSES.isEmpty() || !RELICS.isEmpty();
        for (EchoInfo echo : ECHOES) {
            AbsorbableDef def = ABSORBABLES.get(echo.id());
            if (def != null && def.kind == kind) return true;
        }
        return false;
    }
}
