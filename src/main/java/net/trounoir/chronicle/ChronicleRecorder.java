package net.trounoir.chronicle;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.trounoir.absorption.LifeData;
import net.trounoir.character.Characters;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.faction.Reputation;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Text;
import net.trounoir.world.Crossing;
import net.trounoir.world.SiteManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Transforme une vie qui s'achève en une entrée de la Chronique. */
public final class ChronicleRecorder {
    private ChronicleRecorder() {}

    public static LifeRecord finish(ServerPlayer player, LifeData life, DamageSource source, PersistentStore.Memory memory) {
        LifeRecord record = new LifeRecord();
        record.number = life.lifeNumber;
        record.name = life.name;
        record.durationTicks = life.ticksLived;
        record.power = (int) Math.round(life.power);
        record.rank = life.rank;
        record.kills = life.kills;
        record.feats = new ArrayList<>(life.feats);
        record.allies = Reputation.allies(player.server);
        record.victory = life.uniqueKills.contains("trounoir:higgs");

        // Voie dominante : l'étiquette qui pèse le plus parmi les traits absorbés.
        Map<String, Integer> weights = new HashMap<>();
        List<AbsorbableDef> absorbed = new ArrayList<>();
        for (String id : life.xp.keySet()) {
            AbsorbableDef def = TNData.absorbable(id);
            int level = life.level(id);
            if (def == null || level <= 0) continue;
            if (def.kind != AbsorbableDef.Kind.KNOWLEDGE) absorbed.add(def);
            if (def.kind == AbsorbableDef.Kind.TRAIT) {
                for (String tag : def.tags) weights.merge(tag, level, Integer::sum);
            }
        }
        record.path = weights.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");

        // Absorptions notables : les signatures d'abord, puis les plus puissantes.
        absorbed.sort(Comparator.<AbsorbableDef, Boolean>comparing(d -> !d.signature)
                .thenComparingDouble(d -> -d.power * life.level(d.id.toString())));
        for (int i = 0; i < Math.min(5, absorbed.size()); i++) record.notable.add(absorbed.get(i).id.toString());

        // Cause et tueur.
        record.cause = source.getLocalizedDeathMessage(player).getString();
        String killerCategory = "world";
        String killerName = "";
        if (life.lastSniper != null) {
            CharacterDef sniper = TNData.character(life.lastSniper);
            if (sniper != null) {
                record.killerId = sniper.id.toString();
                record.killer = "@" + sniper.nameKey();
                killerName = Component.translatable(sniper.nameKey()).getString();
                killerCategory = sniper.category;
            }
        } else {
            Entity killer = source.getEntity();
            if (killer instanceof LivingEntity living && killer != player) {
                CharacterDef def = Characters.defOf(living);
                if (def != null) {
                    record.killerId = def.id.toString();
                    record.killer = "@" + def.nameKey();
                    killerName = Component.translatable(def.nameKey()).getString();
                    killerCategory = def.category;
                } else {
                    record.killer = living.getDisplayName().getString();
                    killerName = record.killer;
                    killerCategory = "beast";
                }
            }
        }

        // Lieu.
        SimpleDefs.SiteDef site = SiteManager.nearest(player, 160);
        String placeKey = site != null ? site.nameKey()
                : player.level().dimension().location().equals(Crossing.DIVINIARTH) ? "trounoir.place.diviniarth"
                : player.level().dimension().location().equals(SiteManager.OVERWORLD) ? "trounoir.place.nebulosia" : "trounoir.place.elsewhere";
        record.place = "@" + placeKey;

        // Épitaphe, dans la langue du joueur.
        String language = player.clientInformation().language().toLowerCase(Locale.ROOT);
        boolean nameless = record.name.isEmpty();
        Map<String, String> vars = new HashMap<>();
        vars.put("name", nameless ? Component.translatable("trounoir.nameless").getString() : record.name);
        vars.put("killer", killerName.isEmpty() ? Component.translatable("trounoir.killer.world").getString() : killerName);
        vars.put("place", Component.translatable(placeKey).getString());
        vars.put("path", record.path.isEmpty() ? Component.translatable("trounoir.path.none").getString()
                : Component.translatable("tntag.trounoir." + record.path).getString());
        vars.put("absorption", record.notable.isEmpty() ? Component.translatable("trounoir.absorption.none").getString()
                : Component.translatable(absorbed.get(0).nameKey()).getString());
        vars.put("kills", Integer.toString(record.kills));
        vars.put("number", Integer.toString(record.number));
        vars.put("rank", Text.roman(record.rank));
        vars.put("duration", Text.duration(record.durationTicks,
                Component.translatable("trounoir.unit.day").getString(),
                Component.translatable("trounoir.unit.hour").getString(),
                Component.translatable("trounoir.unit.minute").getString()));
        EpitaphGenerator.Facts facts = new EpitaphGenerator.Facts(record.path, killerCategory, record.kills,
                record.durationTicks / 1200L, record.number, nameless, record.victory, record.rank, life.uniqueKills.size());
        record.epitaph = EpitaphGenerator.generate(language, facts, vars, player.getRandom());
        return record;
    }
}
