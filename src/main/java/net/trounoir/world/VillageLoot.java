package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.character.CharacterEntity;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.util.Fx;

/**
 * Butin des villages. Les coffres des maisons sont gardés : les ouvrir attire les gardes proches.
 * Les barils, plus pauvres, ne regardent personne. Le butin est tiré d'une graine fixe : le même à chaque vie.
 */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillageLoot {
    private VillageLoot() {}

    private static final ResourceKey<LootTable> CHEST = ResourceKey.create(Registries.LOOT_TABLE, TrouNoir.rl("chests/village_house"));
    private static final ResourceKey<LootTable> BARREL = ResourceKey.create(Registries.LOOT_TABLE, TrouNoir.rl("chests/village_barrel"));

    public static void place(ServerLevel level, SimpleDefs.SiteDef site, StructureBuilders.Built built) {
        WorldState state = WorldState.get(level.getServer());
        for (BlockPos pos : built.lootChests()) {
            if (level.getBlockEntity(pos) instanceof RandomizableContainer box) box.setLootTable(CHEST, pos.asLong());
            state.guardedChests.put(key(pos), site.id().toString());
        }
        for (BlockPos pos : built.barrels()) {
            if (level.getBlockEntity(pos) instanceof RandomizableContainer box) box.setLootTable(BARREL, pos.asLong());
        }
    }

    private static String key(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    @SubscribeEvent
    public static void onOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) return;
        BlockPos pos = event.getPos();
        WorldState state = WorldState.get(player.server);
        if (!state.guardedChests.containsKey(key(pos))) return;
        ServerLevel level = player.serverLevel();
        boolean seen = false;
        for (CharacterEntity guard : level.getEntitiesOfClass(CharacterEntity.class, new AABB(pos).inflate(28))) {
            CharacterDef def = guard.def();
            if (def == null || !"guard".equals(def.behavior) || guard.isProvoked()) continue;
            guard.provoke(player);
            seen = true;
        }
        if (seen) {
            player.displayClientMessage(Component.translatable("trounoir.loot.caught"), true);
            Fx.playTo(player, "entity.villager.no", 1.0F, 0.8F);
        }
    }
}
