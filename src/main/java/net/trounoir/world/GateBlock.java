package net.trounoir.world;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.trounoir.TrouNoir;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.util.Fx;

/**
 * Le Seuil : au centre d'une Porte des Astres. Qui connaît le Secret des Dieux passe dans la Voûte ;
 * les autres n'entendent qu'un silence. Depuis la Voûte, le Seuil ramène en Diviniarth.
 */
public class GateBlock extends Block {
    private static final String SECRET = "trounoir:secret_des_dieux";

    public GateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.sidedSuccess(level.isClientSide);
        boolean inVoute = level.dimension().location().equals(SiteManager.VOUTE);
        PersistentStore store = PersistentStore.get(serverPlayer.server);
        if (!inVoute && !store.memory(serverPlayer.getUUID()).knowledge.contains(SECRET)) {
            serverPlayer.displayClientMessage(Component.translatable("trounoir.gate.sealed"), true);
            Fx.playTo(serverPlayer, "block.respawn_anchor.deplete", 0.8F, 0.6F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        String destinationId = inVoute ? "trounoir:porte_des_astres" : "trounoir:seuil_de_la_voute";
        SimpleDefs.SiteDef destination = TNData.SITES.get(net.minecraft.resources.ResourceLocation.parse(destinationId));
        ServerLevel target = destination == null ? null : SiteManager.level(serverPlayer.server, destination.dimension());
        if (target == null) return InteractionResult.sidedSuccess(level.isClientSide);
        StructureBuilders.Built built = SiteManager.ensureBuilt(target, destination);
        if (built == null) return InteractionResult.sidedSuccess(level.isClientSide);
        BlockPos arrival = built.arrival();
        Fx.effect(serverPlayer, "blindness", 60, 0);
        serverPlayer.teleportTo(target, arrival.getX() + 0.5, arrival.getY() + 0.1, arrival.getZ() + 0.5, serverPlayer.getYRot(), 0F);
        Fx.playTo(serverPlayer, "block.end_portal.spawn", 0.7F, 1.4F);
        if (!inVoute) {
            Fx.title(serverPlayer, Component.translatable("trounoir.gate.enter"), Component.translatable("trounoir.gate.enter_sub"), 70);
            store.memory(serverPlayer.getUUID()).places.add("trounoir:seuil_de_la_voute");
            store.save();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
