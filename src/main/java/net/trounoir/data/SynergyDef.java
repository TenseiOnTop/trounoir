package net.trounoir.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Synergie : bonus débloqué par un nombre de traits partageant une étiquette, ou par une combinaison précise. */
public final class SynergyDef {
    public final ResourceLocation id;
    /** étiquette -> nombre de traits distincts requis. */
    public final Map<String, Integer> requiresTags;
    public final List<ResourceLocation> requiresAll;
    public final BonusSet bonus;
    public final double power;
    public final JsonObject raw;

    public SynergyDef(ResourceLocation id, JsonObject json) {
        this.id = id;
        this.raw = json;
        Map<String, Integer> tags = new LinkedHashMap<>();
        JsonUtil.numberMap(json, "requires_tags").forEach((k, v) -> tags.put(k, (int) Math.round(v)));
        this.requiresTags = Map.copyOf(tags);
        this.requiresAll = List.copyOf(JsonUtil.ids(json, "requires_all"));
        this.bonus = BonusSet.parse(JsonUtil.obj(json, "bonus"));
        this.power = JsonUtil.num(json, "power", 0);
    }

    public String nameKey() {
        return "synergy." + id.getNamespace() + "." + id.getPath();
    }
}
