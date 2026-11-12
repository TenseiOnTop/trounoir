package net.trounoir.data;

import com.google.gson.JsonObject;
import net.trounoir.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;

/** Modèles d'épitaphes d'une langue : ouverture + corps + clôture, chacun choisi selon les données de la vie. */
public final class EpitaphDef {
    /** Un fragment de phrase, avec ses conditions d'emploi. */
    public record Fragment(String text, JsonObject when, int weight) {}

    public final String language;
    public final List<Fragment> openings = new ArrayList<>();
    public final List<Fragment> bodies = new ArrayList<>();
    public final List<Fragment> closings = new ArrayList<>();

    public EpitaphDef(String language) {
        this.language = language;
    }

    public void merge(JsonObject json) {
        read(json, "openings", openings);
        read(json, "bodies", bodies);
        read(json, "closings", closings);
    }

    private static void read(JsonObject json, String key, List<Fragment> into) {
        for (JsonObject f : JsonUtil.objects(json, key)) {
            into.add(new Fragment(JsonUtil.str(f, "text", ""), JsonUtil.obj(f, "when"), Math.max(1, JsonUtil.integer(f, "weight", 1))));
        }
        for (String s : JsonUtil.strings(json, key)) into.add(new Fragment(s, new JsonObject(), 1));
    }
}
