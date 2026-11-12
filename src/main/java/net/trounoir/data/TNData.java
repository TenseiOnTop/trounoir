package net.trounoir.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.util.JsonUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Registre de tout le contenu piloté par les données (côté serveur).
 * Rechargé avec les datapacks (/reload) : l'équilibrage se fait sans recompiler.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TNData {
    private TNData() {}

    private static final Gson GSON = new Gson();

    public static volatile Map<ResourceLocation, AbsorbableDef> ABSORBABLES = Map.of();
    public static volatile Map<String, SimpleDefs.TagDef> TAGS = Map.of();
    public static volatile Map<ResourceLocation, SynergyDef> SYNERGIES = Map.of();
    public static volatile Map<String, SimpleDefs.HandicapDef> HANDICAPS = Map.of();
    public static volatile Map<ResourceLocation, SimpleDefs.ProfileDef> PROFILES = Map.of();
    public static volatile Map<ResourceLocation, CharacterDef> CHARACTERS = Map.of();
    public static volatile Map<String, SimpleDefs.FactionDef> FACTIONS = Map.of();
    public static volatile Map<ResourceLocation, SimpleDefs.SiteDef> SITES = Map.of();
    public static volatile Map<String, EpitaphDef> EPITAPHS = Map.of();

    // Les trois sortes d'absorbables partagent un même espace d'identifiants.
    private static final Map<AbsorbableDef.Kind, Map<ResourceLocation, AbsorbableDef>> BY_KIND = new HashMap<>();

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(absorbableLoader("trounoir/traits", AbsorbableDef.Kind.TRAIT));
        event.addListener(absorbableLoader("trounoir/techniques", AbsorbableDef.Kind.TECHNIQUE));
        event.addListener(absorbableLoader("trounoir/knowledge", AbsorbableDef.Kind.KNOWLEDGE));
        event.addListener(new Loader("trounoir/tags", (files) -> {
            Map<String, SimpleDefs.TagDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id.getPath(), SimpleDefs.TagDef.parse(id, json)));
            TAGS = java.util.Collections.unmodifiableMap(out);
        }));
        event.addListener(new Loader("trounoir/synergies", (files) -> {
            Map<ResourceLocation, SynergyDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id, new SynergyDef(id, json)));
            SYNERGIES = java.util.Collections.unmodifiableMap(out);
        }));
        event.addListener(new Loader("trounoir/fragility", (files) -> {
            Map<String, SimpleDefs.HandicapDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id.getPath(), SimpleDefs.HandicapDef.parse(id, json)));
            HANDICAPS = java.util.Collections.unmodifiableMap(out);
        }));
        event.addListener(new Loader("trounoir/profiles", (files) -> {
            Map<ResourceLocation, SimpleDefs.ProfileDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> {
                SimpleDefs.ProfileDef def = SimpleDefs.ProfileDef.parse(id, json);
                out.put(def.entity(), def);
            });
            PROFILES = Map.copyOf(out);
        }));
        event.addListener(new Loader("trounoir/characters", (files) -> {
            Map<ResourceLocation, CharacterDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id, new CharacterDef(id, json)));
            CHARACTERS = java.util.Collections.unmodifiableMap(out);
        }));
        event.addListener(new Loader("trounoir/factions", (files) -> {
            Map<String, SimpleDefs.FactionDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id.getPath(), SimpleDefs.FactionDef.parse(id, json)));
            FACTIONS = Map.copyOf(out);
        }));
        event.addListener(new Loader("trounoir/sites", (files) -> {
            Map<ResourceLocation, SimpleDefs.SiteDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id, SimpleDefs.SiteDef.parse(id, json)));
            SITES = java.util.Collections.unmodifiableMap(out);
        }));
        event.addListener(new Loader("trounoir/epitaphs", (files) -> {
            Map<String, EpitaphDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> {
                String lang = JsonUtil.str(json, "language", "en_us");
                out.computeIfAbsent(lang, EpitaphDef::new).merge(json);
            });
            EPITAPHS = Map.copyOf(out);
        }));
    }

    private static Loader absorbableLoader(String directory, AbsorbableDef.Kind kind) {
        return new Loader(directory, (files) -> {
            Map<ResourceLocation, AbsorbableDef> out = new LinkedHashMap<>();
            files.forEach((id, json) -> out.put(id, new AbsorbableDef(id, kind, json)));
            synchronized (BY_KIND) {
                BY_KIND.put(kind, out);
                Map<ResourceLocation, AbsorbableDef> all = new LinkedHashMap<>();
                for (AbsorbableDef.Kind k : AbsorbableDef.Kind.values()) {
                    Map<ResourceLocation, AbsorbableDef> part = BY_KIND.get(k);
                    if (part != null) all.putAll(part);
                }
                ABSORBABLES = java.util.Collections.unmodifiableMap(all);
            }
        });
    }

    public static AbsorbableDef absorbable(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : ABSORBABLES.get(rl);
    }

    public static CharacterDef character(String id) {
        if (id == null || id.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : CHARACTERS.get(rl);
    }

    /** Un répertoire de JSON du datapack -> une table de définitions. */
    private static final class Loader extends SimpleJsonResourceReloadListener {
        private final String directory;
        private final java.util.function.Consumer<Map<ResourceLocation, JsonObject>> sink;

        Loader(String directory, java.util.function.Consumer<Map<ResourceLocation, JsonObject>> sink) {
            super(GSON, directory);
            this.directory = directory;
            this.sink = sink;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            Map<ResourceLocation, JsonObject> objects = new LinkedHashMap<>();
            BiConsumer<ResourceLocation, JsonElement> add = (id, element) -> {
                if (element.isJsonObject()) objects.put(id, element.getAsJsonObject());
                else TrouNoir.LOGGER.warn("[Trou-Noir] {} ignoré dans {} : objet JSON attendu", id, directory);
            };
            files.forEach(add);
            try {
                sink.accept(objects);
            } catch (RuntimeException e) {
                TrouNoir.LOGGER.error("[Trou-Noir] Échec du chargement de {}", directory, e);
            }
            TrouNoir.LOGGER.info("[Trou-Noir] {} : {} définitions", directory, objects.size());
        }
    }
}
