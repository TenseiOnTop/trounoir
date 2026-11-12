package net.trounoir.chronicle;

import com.google.gson.JsonObject;
import net.minecraft.util.RandomSource;
import net.trounoir.data.EpitaphDef;
import net.trounoir.data.TNData;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Épitaphes procédurales : ouverture + corps + clôture, tirés parmi les modèles du datapack
 * dont les conditions correspondent à la vie qui s'achève. Aucune IA en jeu.
 */
public final class EpitaphGenerator {
    private EpitaphGenerator() {}

    /** Faits d'une vie sur lesquels portent les conditions des modèles. */
    public record Facts(String path, String killerCategory, int kills, long minutes, int lifeNumber, boolean nameless,
                        boolean victory, int rank, int uniques) {}

    public static String generate(String language, Facts facts, Map<String, String> variables, RandomSource random) {
        EpitaphDef def = TNData.EPITAPHS.get(language);
        if (def == null) def = TNData.EPITAPHS.get("en_us");
        if (def == null && !TNData.EPITAPHS.isEmpty()) def = TNData.EPITAPHS.values().iterator().next();
        if (def == null) return "";
        StringBuilder out = new StringBuilder();
        for (List<EpitaphDef.Fragment> pool : List.of(def.openings, def.bodies, def.closings)) {
            String part = pick(pool, facts, random);
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(part);
        }
        String text = out.toString();
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            text = text.replace("{" + variable.getKey() + "}", variable.getValue());
        }
        return text;
    }

    private static String pick(List<EpitaphDef.Fragment> pool, Facts facts, RandomSource random) {
        List<EpitaphDef.Fragment> fitting = new ArrayList<>();
        int total = 0;
        for (EpitaphDef.Fragment fragment : pool) {
            if (matches(fragment.when(), facts)) {
                // Un fragment conditionnel est plus précis qu'un fragment générique : il pèse davantage.
                int weight = fragment.weight() * (fragment.when().size() > 0 ? 3 : 1);
                fitting.add(new EpitaphDef.Fragment(fragment.text(), fragment.when(), weight));
                total += weight;
            }
        }
        if (fitting.isEmpty()) return "";
        int roll = random.nextInt(total);
        for (EpitaphDef.Fragment fragment : fitting) {
            roll -= fragment.weight();
            if (roll < 0) return fragment.text();
        }
        return fitting.get(0).text();
    }

    private static boolean matches(JsonObject when, Facts f) {
        if (when.size() == 0) return true;
        if (when.has("path") && !JsonUtil.str(when, "path", "").equals(f.path())) return false;
        if (when.has("killer_category") && !JsonUtil.str(when, "killer_category", "").equals(f.killerCategory())) return false;
        if (when.has("min_kills") && f.kills() < JsonUtil.integer(when, "min_kills", 0)) return false;
        if (when.has("max_kills") && f.kills() > JsonUtil.integer(when, "max_kills", 0)) return false;
        if (when.has("min_minutes") && f.minutes() < JsonUtil.integer(when, "min_minutes", 0)) return false;
        if (when.has("max_minutes") && f.minutes() > JsonUtil.integer(when, "max_minutes", 0)) return false;
        if (when.has("min_life") && f.lifeNumber() < JsonUtil.integer(when, "min_life", 0)) return false;
        if (when.has("max_life") && f.lifeNumber() > JsonUtil.integer(when, "max_life", 0)) return false;
        if (when.has("min_rank") && f.rank() < JsonUtil.integer(when, "min_rank", 0)) return false;
        if (when.has("max_rank") && f.rank() > JsonUtil.integer(when, "max_rank", 0)) return false;
        if (when.has("min_uniques") && f.uniques() < JsonUtil.integer(when, "min_uniques", 0)) return false;
        if (when.has("nameless") && JsonUtil.bool(when, "nameless", false) != f.nameless()) return false;
        if (when.has("victory") && JsonUtil.bool(when, "victory", false) != f.victory()) return false;
        return true;
    }
}
