package net.trounoir.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.TrouNoir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lecture tolérante de JSON : une valeur absente ou mal typée retombe sur la valeur par défaut. */
public final class JsonUtil {
    private JsonUtil() {}

    public static String str(JsonObject o, String key, String def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    public static double num(JsonObject o, String key, double def) {
        JsonElement e = o.get(key);
        try {
            return e != null && e.isJsonPrimitive() ? e.getAsDouble() : def;
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static int integer(JsonObject o, String key, int def) {
        return (int) Math.round(num(o, key, def));
    }

    public static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : def;
    }

    public static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    public static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : new JsonArray();
    }

    public static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr(o, key)) if (e.isJsonPrimitive()) out.add(e.getAsString());
        return out;
    }

    public static List<JsonObject> objects(JsonObject o, String key) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : arr(o, key)) if (e.isJsonObject()) out.add(e.getAsJsonObject());
        return out;
    }

    public static Map<String, Double> numberMap(JsonObject o, String key) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : obj(o, key).entrySet()) {
            if (e.getValue().isJsonPrimitive()) {
                try {
                    out.put(e.getKey(), e.getValue().getAsDouble());
                } catch (NumberFormatException ignored) {
                    // valeur ignorée
                }
            }
        }
        return out;
    }

    /** Identifiant ; sans espace de noms, celui du mod est utilisé. Jamais nul. */
    public static ResourceLocation id(String s) {
        if (s == null || s.isEmpty()) return TrouNoir.rl("none");
        ResourceLocation parsed = s.indexOf(':') >= 0 ? ResourceLocation.tryParse(s) : ResourceLocation.tryBuild(TrouNoir.MODID, s);
        return parsed != null ? parsed : TrouNoir.rl("none");
    }

    public static ResourceLocation id(JsonObject o, String key, String def) {
        return id(str(o, key, def));
    }

    public static List<ResourceLocation> ids(JsonObject o, String key) {
        List<ResourceLocation> out = new ArrayList<>();
        for (String s : strings(o, key)) out.add(id(s));
        return out;
    }

    /** Couleur "#rrggbb" vers entier RGB. */
    public static int color(String hex, int def) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (RuntimeException e) {
            return def;
        }
    }
}
