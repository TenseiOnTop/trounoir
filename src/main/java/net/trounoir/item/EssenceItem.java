package net.trounoir.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.trounoir.absorption.Absorption;
import net.trounoir.absorption.Bonuses;
import net.trounoir.absorption.LifeManager;
import net.trounoir.network.Net;
import net.trounoir.registry.ModRegistry;

/** Essence (mode créatif) : clic droit pour absorber directement un trait, une technique ou un savoir. */
public class EssenceItem extends Item {
    public static final String KEY = "trounoir_absorbable";

    public EssenceItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(ResourceLocation absorbable, String kind) {
        ItemStack stack = new ItemStack(ModRegistry.ESSENCE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY, absorbable.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.trounoir.essence.named",
                Component.translatable(kind + "." + absorbable.getNamespace() + "." + absorbable.getPath())));
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user instanceof ServerPlayer player) {
            String id = CharacterEggItem.idOf(stack, KEY);
            if (Absorption.absorb(player, id, true)) {
                Bonuses.recompute(player);
                LifeManager.save(player);
                Net.syncLife(player);
                if (!player.isCreative()) stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
