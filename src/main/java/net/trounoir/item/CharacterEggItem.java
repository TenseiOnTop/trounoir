package net.trounoir.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.trounoir.character.Characters;
import net.trounoir.data.CharacterDef;
import net.trounoir.data.TNData;
import net.trounoir.registry.ModRegistry;

/** Œuf d'apparition d'un personnage du mod (mode créatif). Le personnage visé est porté par l'objet. */
public class CharacterEggItem extends Item {
    public static final String KEY = "trounoir_character";

    public CharacterEggItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation character) {
        ItemStack stack = new ItemStack(ModRegistry.CHARACTER_EGG.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY, character.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.trounoir.character_egg.named",
                Component.translatable("character." + character.getNamespace() + "." + character.getPath())));
        return stack;
    }

    public static String idOf(ItemStack stack, String key) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString(key);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        CharacterDef def = TNData.character(idOf(context.getItemInHand(), KEY));
        if (def == null) return InteractionResult.FAIL;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (Characters.spawn(level, def, pos, pos) == null) {
            player.displayClientMessage(Component.translatable("trounoir.egg.unique_taken"), true);
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
