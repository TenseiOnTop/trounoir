package net.trounoir.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.SynergyDef;
import net.trounoir.data.TNData;
import net.trounoir.rebirth.EchoLogic;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.world.SiteManager;
import net.trounoir.world.WorldState;

import java.util.Map;

/** Canal réseau du mod et constructeurs des messages de synchronisation. */
public final class Net {
    private Net() {}

    private static final int PROTOCOL = 1;
    private static SimpleChannel channel;

    public static void init() {
        channel = ChannelBuilder.named(TrouNoir.rl("main")).networkProtocolVersion(PROTOCOL).simpleChannel();
        channel.messageBuilder(TNPacket.class)
                .encoder(TNPacket::encode)
                .decoder(TNPacket::decode)
                .consumerMainThread(TNPacket::handle)
                .add();
    }

    public static void toClient(ServerPlayer player, String type, JsonObject json) {
        if (player.connection == null) return;
        channel.send(new TNPacket(type, json), PacketDistributor.PLAYER.with(player));
    }

    public static void toServer(String type, JsonObject json) {
        channel.send(new TNPacket(type, json), PacketDistributor.SERVER.noArg());
    }

    /** Envoie au client les définitions dont l'interface a besoin (après connexion et après /reload). */
    public static void syncDefs(ServerPlayer player) {
        JsonObject root = new JsonObject();
        JsonObject absorbables = new JsonObject();
        for (AbsorbableDef def : TNData.ABSORBABLES.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("kind", def.kind.name());
            entry.add("def", def.raw);
            absorbables.add(def.id.toString(), entry);
        }
        root.add("absorbables", absorbables);
        JsonObject tags = new JsonObject();
        for (SimpleDefs.TagDef tag : TNData.TAGS.values()) tags.add(tag.id(), tag.raw());
        root.add("tags", tags);
        JsonObject synergies = new JsonObject();
        for (SynergyDef synergy : TNData.SYNERGIES.values()) synergies.add(synergy.id.toString(), synergy.raw);
        root.add("synergies", synergies);
        JsonObject characters = new JsonObject();
        for (CharacterDef def : TNData.CHARACTERS.values()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("texture", def.texture.toString());
            entry.addProperty("rank", def.rank);
            entry.addProperty("category", def.category);
            entry.addProperty("variants", def.variants);
            entry.addProperty("weakness", def.weakness);
            characters.add(def.id.toString(), entry);
        }
        root.add("characters", characters);
        toClient(player, "defs", root);
    }

    /** Envoie au client l'état de la vie en cours. */
    public static void syncLife(ServerPlayer player) {
        LifeData life = LifeManager.getOrNull(player);
        if (life == null) return;
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        long now = player.serverLevel().getGameTime();

        JsonObject root = new JsonObject();
        root.addProperty("life", life.lifeNumber);
        root.addProperty("name", life.name);
        root.addProperty("deaths", memory.deaths);
        root.addProperty("power", life.power);
        root.addProperty("rank", life.rank);
        double[] thresholds = net.trounoir.config.TNConfig.doubles(net.trounoir.config.TNConfig.RANK_POWER_THRESHOLDS);
        root.addProperty("nextRank", life.rank < thresholds.length ? thresholds[life.rank] : -1);
        root.addProperty("notoriety", life.notoriety);
        root.addProperty("revealed", life.hasEverAbsorbed);
        root.addProperty("echoSlots", EchoLogic.slotsFor(memory.deaths));
        root.addProperty("sukeAlive", !WorldState.get(player.server).isDead("trounoir:suke"));

        JsonObject xp = new JsonObject();
        life.xp.forEach(xp::addProperty);
        root.add("xp", xp);

        JsonArray slots = new JsonArray();
        for (String s : life.slots) slots.add(s == null ? "" : s);
        root.add("slots", slots);

        JsonObject cooldowns = new JsonObject();
        for (Map.Entry<String, Long> e : life.cooldowns.entrySet()) {
            long left = e.getValue() - now;
            if (left > 0) cooldowns.addProperty(e.getKey(), left);
        }
        root.add("cooldowns", cooldowns);

        JsonArray echoes = new JsonArray();
        for (PersistentStore.Echo echo : life.echoes) {
            JsonObject e = new JsonObject();
            e.addProperty("id", echo.id);
            e.addProperty("level", echo.level);
            echoes.add(e);
        }
        root.add("echoes", echoes);

        JsonArray synergies = new JsonArray();
        life.synergies.forEach(synergies::add);
        root.add("synergies", synergies);

        JsonObject handicaps = new JsonObject();
        life.handicapRemaining.forEach(handicaps::addProperty);
        root.add("handicaps", handicaps);

        JsonArray knowledge = new JsonArray();
        memory.knowledge.forEach(knowledge::add);
        root.add("knowledge", knowledge);

        JsonArray weaknesses = new JsonArray();
        memory.weaknesses.forEach(weaknesses::add);
        root.add("weaknesses", weaknesses);
        JsonObject relics = new JsonObject();
        memory.relics.forEach(relics::addProperty);
        root.add("relics", relics);

        JsonObject places = new JsonObject();
        for (String place : memory.places) {
            int[] pos = SiteManager.positionOf(player.server, place);
            if (pos == null) continue;
            JsonArray p = new JsonArray();
            p.add(pos[0]);
            p.add(pos[1]);
            p.add(pos[2]);
            SimpleDefs.SiteDef site = SiteManager.site(place);
            JsonObject entry = new JsonObject();
            entry.add("pos", p);
            entry.addProperty("dimension", site == null ? "" : site.dimension().toString());
            places.add(place, entry);
        }
        root.add("places", places);

        toClient(player, "life", root);
    }
}
