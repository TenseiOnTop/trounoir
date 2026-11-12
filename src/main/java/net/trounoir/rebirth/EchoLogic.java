package net.trounoir.rebirth;

import net.trounoir.absorption.LifeData;
import net.trounoir.config.TNConfig;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.TNData;

import java.util.List;

/** Les Échos : à chaque mort, un seul trait absorbé peut être conservé, affaibli. */
public final class EchoLogic {
    private EchoLogic() {}

    /** Nombre d'emplacements d'Échos après un certain nombre de morts. */
    public static int slotsFor(int deaths) {
        List<Integer> thresholds = TNConfig.ints(TNConfig.ECHO_SLOT_THRESHOLDS);
        int slots = 0;
        for (int t : thresholds) if (deaths >= t) slots++;
        return Math.max(1, slots);
    }

    /**
     * Applique le choix fait à la mort. Le trait doit réellement avoir été absorbé pendant la vie qui s'achève.
     *
     * @param replace index de l'Écho à remplacer si tous les emplacements sont pris ; -1 pour le premier libre
     */
    public static boolean choose(PersistentStore.Memory memory, LifeData endedLife, String id, int replace) {
        if (id == null || id.isEmpty()) return false;
        AbsorbableDef def = TNData.absorbable(id);
        if (def == null || def.kind == AbsorbableDef.Kind.KNOWLEDGE) return false;
        int level = endedLife.level(id);
        if (level <= 0) return false;
        level = Math.min(level, TNConfig.ECHO_MAX_LEVEL.get());

        for (PersistentStore.Echo echo : memory.echoes) {
            if (echo.id.equals(id)) {
                echo.level = Math.max(echo.level, level);
                return true;
            }
        }
        PersistentStore.Echo echo = new PersistentStore.Echo();
        echo.id = id;
        echo.level = level;
        int slots = slotsFor(memory.deaths);
        while (memory.echoes.size() > slots) memory.echoes.remove(memory.echoes.size() - 1);
        if (memory.echoes.size() < slots) {
            memory.echoes.add(echo);
        } else if (replace >= 0 && replace < memory.echoes.size()) {
            memory.echoes.set(replace, echo);
        } else {
            return false;
        }
        return true;
    }
}
