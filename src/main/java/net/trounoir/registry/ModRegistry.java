package net.trounoir.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.trounoir.data.AbsorbableDef;
import net.trounoir.data.TNData;
import net.trounoir.item.CharacterEggItem;
import net.trounoir.item.EssenceItem;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.trounoir.TrouNoir;
import net.trounoir.character.CharacterEntity;
import net.trounoir.world.HelmBlock;

/** Enregistrements statiques (entité, bloc, objet). Tout le reste du contenu est piloté par les données. */
public final class ModRegistry {
    private ModRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TrouNoir.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TrouNoir.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TrouNoir.MODID);

    /** Entité unique servant à tous les personnages humains (PNJ, Supernovas, Hypernovas, Dieux), définie par JSON. */
    public static final RegistryObject<EntityType<CharacterEntity>> CHARACTER = ENTITIES.register("character",
            () -> EntityType.Builder.<CharacterEntity>of(CharacterEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build("character"));

    /** Le gouvernail : bloc d'interaction de la Traversée entre Nébulosia et Diviniarth. */
    public static final RegistryObject<Block> HELM = BLOCKS.register("helm",
            () -> new HelmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(50.0F, 1200.0F).noLootTable()));
    public static final RegistryObject<Item> HELM_ITEM = ITEMS.register("helm",
            () -> new BlockItem(HELM.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHARACTER_EGG = ITEMS.register("character_egg", () -> new CharacterEggItem(new Item.Properties()));
    public static final RegistryObject<Item> ESSENCE = ITEMS.register("essence", () -> new EssenceItem(new Item.Properties()));

    /** Identifiants connus du côté courant (remplacés côté client par ceux reçus du serveur). */
    public static Supplier<Collection<ResourceLocation>> characterIds = () -> TNData.CHARACTERS.keySet();
    public static Supplier<Collection<AbsorbableDef>> absorbables = () -> TNData.ABSORBABLES.values();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrouNoir.MODID);
    /** Onglet créatif : gouvernail, un œuf par personnage, une essence par trait, technique et savoir. */
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.trounoir"))
            .icon(() -> new ItemStack(HELM_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(HELM_ITEM.get());
                for (ResourceLocation id : characterIds.get()) output.accept(CharacterEggItem.of(id));
                for (AbsorbableDef def : absorbables.get()) {
                    output.accept(EssenceItem.of(def.id, def.kind.name().toLowerCase(Locale.ROOT)));
                }
            }).build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
        ENTITIES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(ModRegistry::onAttributes);
        modBus.addListener(ModRegistry::onCreativeTab);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(CHARACTER.get(), CharacterEntity.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(HELM_ITEM);
        }
    }
}
