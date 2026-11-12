package net.trounoir.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.trounoir.TrouNoir;
import net.trounoir.absorption.Absorption;
import net.trounoir.absorption.Bonuses;
import net.trounoir.absorption.LifeData;
import net.trounoir.absorption.LifeManager;
import net.trounoir.character.Characters;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.SimpleDefs;
import net.trounoir.data.TNData;
import net.trounoir.faction.ReactionEngine;
import net.trounoir.faction.Reputation;
import net.trounoir.network.Net;
import net.trounoir.rebirth.PersistentStore;
import net.trounoir.rebirth.RebirthManager;
import net.trounoir.util.Text;
import net.trounoir.world.Crossing;
import net.trounoir.world.SiteManager;
import net.trounoir.world.StructureBuilders;
import net.trounoir.world.WorldState;

/** Commandes de debug : /trounoir … (niveau opérateur 2). Voir le README pour la liste. */
@Mod.EventBusSubscriber(modid = TrouNoir.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TNCommands {
    private TNCommands() {}

    private static final SuggestionProvider<CommandSourceStack> ABSORBABLES =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(TNData.ABSORBABLES.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> CHARACTERS =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(TNData.CHARACTERS.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> SITES =
            (ctx, builder) -> SharedSuggestionProvider.suggestResource(TNData.SITES.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> FACTIONS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(TNData.FACTIONS.keySet(), builder);

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("trounoir").requires(source -> source.hasPermission(2))
                .then(Commands.literal("absorb")
                        .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(ABSORBABLES)
                                .executes(ctx -> absorb(ctx, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> absorb(ctx, IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(Commands.literal("status").executes(TNCommands::status))
                .then(Commands.literal("rebirth").executes(ctx -> {
                    RebirthManager.softRebirth(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("fragility")
                        .then(Commands.literal("on").executes(ctx -> fragility(ctx, false)))
                        .then(Commands.literal("off").executes(ctx -> fragility(ctx, true))))
                .then(Commands.literal("site")
                        .then(Commands.literal("list").executes(TNCommands::siteList))
                        .then(Commands.literal("tp").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(SITES)
                                .executes(TNCommands::siteTeleport))))
                .then(Commands.literal("spawn").then(Commands.argument("character", ResourceLocationArgument.id()).suggests(CHARACTERS)
                        .executes(TNCommands::spawn)))
                .then(Commands.literal("reputation").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                        .executes(ctx -> reputation(ctx, 0))
                        .then(Commands.argument("delta", IntegerArgumentType.integer(-200, 200))
                                .executes(ctx -> reputation(ctx, IntegerArgumentType.getInteger(ctx, "delta"))))))
                .then(Commands.literal("notoriety").then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                        .executes(TNCommands::notoriety)))
                .then(Commands.literal("hunt").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTIONS)
                        .executes(TNCommands::hunt)))
                .then(Commands.literal("cross").executes(ctx -> {
                    Crossing.cross(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("chronicle").then(Commands.literal("clear").executes(TNCommands::clearChronicle))));
    }

    private static void reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static int absorb(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        if (!TNData.ABSORBABLES.containsKey(id)) {
            ctx.getSource().sendFailure(Component.literal("Inconnu : " + id));
            return 0;
        }
        int gained = 0;
        for (int i = 0; i < count; i++) if (Absorption.absorb(player, id.toString(), i == count - 1)) gained++;
        Bonuses.recompute(player);
        LifeManager.save(player);
        Net.syncLife(player);
        reply(ctx, id + " x" + gained);
        return gained;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LifeData life = LifeManager.get(player);
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        reply(ctx, "Vie " + life.lifeNumber + " « " + life.name + " » — rang " + Text.roman(life.rank) + ", puissance " + Math.round(life.power)
                + ", " + life.xp.size() + " absorptions, " + life.kills + " victimes");
        reply(ctx, "Morts : " + memory.deaths + ", Échos : " + memory.echoes.size() + ", savoirs : " + memory.knowledge.size()
                + ", lieux : " + memory.places.size() + ", faiblesses : " + memory.weaknesses.size());
        reply(ctx, "Synergies : " + life.synergies + " | Fragilité restante : " + life.handicapRemaining);
        reply(ctx, "Retour dans le passé complet : " + RebirthManager.worldResetApplies(player.server) + " (monde lié : " + store.bound + ")");
        return 1;
    }

    private static int fragility(CommandContext<CommandSourceStack> ctx, boolean disabled) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LifeManager.get(player).fragilityDisabled = disabled;
        Bonuses.recompute(player);
        Net.syncLife(player);
        reply(ctx, "Fragilité divine : " + (disabled ? "désactivée" : "active"));
        return 1;
    }

    private static int siteList(CommandContext<CommandSourceStack> ctx) {
        WorldState state = WorldState.get(ctx.getSource().getServer());
        for (SimpleDefs.SiteDef def : TNData.SITES.values()) {
            int[] pos = SiteManager.positionOf(ctx.getSource().getServer(), def.id().toString());
            String where = pos == null ? "?" : pos[0] + " " + pos[1] + " " + pos[2];
            reply(ctx, def.id() + " [" + def.blueprint() + "] " + def.dimension() + " @ " + where
                    + (state.builtSites.contains(def.id().toString()) ? " (bâti)" : ""));
        }
        return TNData.SITES.size();
    }

    private static int siteTeleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
        SimpleDefs.SiteDef def = TNData.SITES.get(id);
        ServerLevel level = def == null ? null : SiteManager.level(player.server, def.dimension());
        if (level == null) {
            ctx.getSource().sendFailure(Component.literal("Lieu ou dimension introuvable : " + id));
            return 0;
        }
        StructureBuilders.Built built = SiteManager.ensureBuilt(level, def);
        if (built == null) return 0;
        BlockPos at = built.arrival();
        player.teleportTo(level, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 180F, 0F);
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CharacterDef def = TNData.CHARACTERS.get(ResourceLocationArgument.getId(ctx, "character"));
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("Personnage inconnu"));
            return 0;
        }
        BlockPos pos = player.blockPosition().offset(2, 0, 0);
        if (Characters.spawn(player.serverLevel(), def, pos, pos) == null) {
            ctx.getSource().sendFailure(Component.literal("Impossible : unique déjà présent ou déjà mort dans cette vie"));
            return 0;
        }
        return 1;
    }

    private static int reputation(CommandContext<CommandSourceStack> ctx, int delta) {
        String faction = StringArgumentType.getString(ctx, "faction");
        Reputation.add(ctx.getSource().getServer(), faction, delta);
        reply(ctx, faction + " : " + Reputation.get(ctx.getSource().getServer(), faction));
        return 1;
    }

    private static int notoriety(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        LifeManager.get(player).notoriety = IntegerArgumentType.getInteger(ctx, "value");
        Net.syncLife(player);
        return 1;
    }

    private static int hunt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SimpleDefs.FactionDef faction = TNData.FACTIONS.get(StringArgumentType.getString(ctx, "faction"));
        if (faction == null) return 0;
        ReactionEngine.spawnSquad(player, faction);
        return 1;
    }

    private static int clearChronicle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PersistentStore store = PersistentStore.get(player.server);
        PersistentStore.Memory memory = store.memory(player.getUUID());
        memory.chronicle.clear();
        memory.echoes.clear();
        memory.knowledge.clear();
        memory.places.clear();
        memory.weaknesses.clear();
        memory.deaths = 0;
        memory.victories = 0;
        store.save();
        Net.syncLife(player);
        reply(ctx, "Mémoires effacées.");
        return 1;
    }
}
