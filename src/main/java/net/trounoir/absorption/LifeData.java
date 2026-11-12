package net.trounoir.absorption;

import net.trounoir.chronicle.LifeRecord;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.TNData;
import net.trounoir.rebirth.PersistentStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * État d'une vie en cours : ce que le Trou-Noir a absorbé, ses techniques équipées, ses statistiques de vie.
 * Enregistré DANS le monde (&lt;monde&gt;/trounoir/players/) : il disparaît donc avec lui à la Renaissance.
 */
public final class LifeData {
    public static final int SLOTS = 4;

    public int lifeNumber = 1;
    public String name = "";
    public long ticksLived;
    /** identifiant d'absorbable -> nombre d'absorptions. */
    public Map<String, Integer> xp = new LinkedHashMap<>();
    public String[] slots = new String[SLOTS];
    /** identifiant de technique -> tick de jeu à partir duquel elle est de nouveau disponible. */
    public Map<String, Long> cooldowns = new HashMap<>();
    public List<PersistentStore.Echo> echoes = new ArrayList<>();
    public int kills;
    public List<String> uniqueKills = new ArrayList<>();
    public List<LifeRecord.Line> feats = new ArrayList<>();
    public double notoriety;
    public int sukeDodged;
    public boolean reachedDiviniarth;
    public boolean fragilityDisabled;
    public boolean hasEverAbsorbed;

    // --- État dérivé, recalculé par Bonuses.recompute ---
    public transient Map<String, Double> stats = new HashMap<>();
    public transient Map<String, Double> handicapRemaining = new LinkedHashMap<>();
    public transient List<String> synergies = new ArrayList<>();
    public transient double power;
    public transient int rank;
    public transient double hungerFactor;
    public transient List<String> appliedAttributes = new ArrayList<>();
    public transient boolean dirty = true;

    // --- État transitoire de techniques et de systèmes ---
    public transient long noFallUntil;
    public transient int dashTicks;
    public transient double dashDamage;
    public transient List<Integer> dashHit = new ArrayList<>();
    public transient long sukeNextShot;
    public transient long sukeFireAt;
    public transient double[] sukeGlint;
    public transient double[] sukeAim;
    public transient String lastSniper;
    public transient long lastUntouchableNotice;

    /** Répare les champs absents après lecture JSON. */
    public void sanitize() {
        if (xp == null) xp = new LinkedHashMap<>();
        if (slots == null || slots.length != SLOTS) slots = new String[SLOTS];
        if (cooldowns == null) cooldowns = new HashMap<>();
        if (echoes == null) echoes = new ArrayList<>();
        if (uniqueKills == null) uniqueKills = new ArrayList<>();
        if (feats == null) feats = new ArrayList<>();
        if (name == null) name = "";
        stats = new HashMap<>();
        handicapRemaining = new LinkedHashMap<>();
        synergies = new ArrayList<>();
        appliedAttributes = new ArrayList<>();
        dashHit = new ArrayList<>();
        dirty = true;
    }

    /** Niveau réellement absorbé dans cette vie (0 si jamais absorbé). */
    public int level(String id) {
        Integer count = xp.get(id);
        if (count == null) return 0;
        AbsorbableDef def = TNData.absorbable(id);
        return def == null ? 0 : def.levelForXp(count);
    }

    public PersistentStore.Echo echo(String id) {
        for (PersistentStore.Echo e : echoes) if (e.id.equals(id)) return e;
        return null;
    }

    /** Vrai si la capacité est utilisable, par absorption ou par Écho. */
    public boolean has(String id) {
        return level(id) > 0 || echo(id) != null;
    }

    public double stat(String key) {
        Double v = stats.get(key);
        return v == null ? 0 : v;
    }

    public void feat(String key, String... args) {
        for (LifeRecord.Line line : feats) {
            if (line.key.equals(key) && line.args.equals(List.of(args))) return;
        }
        feats.add(new LifeRecord.Line(key, args));
    }
}
