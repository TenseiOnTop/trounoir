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
import net.trounoir.item.ArtifactItem;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public static final RegistryObject<Block> GATE = BLOCKS.register("gate",
            () -> new net.trounoir.world.GateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3600000.0F).lightLevel(state -> 13).noLootTable()));
    public static final RegistryObject<Item> GATE_ITEM = ITEMS.register("gate",
            () -> new BlockItem(GATE.get(), new Item.Properties()));
    public static final RegistryObject<Item> HELM_ITEM = ITEMS.register("helm",
            () -> new BlockItem(HELM.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHARACTER_EGG = ITEMS.register("character_egg", () -> new CharacterEggItem(new Item.Properties()));
    public static final RegistryObject<Item> ESSENCE = ITEMS.register("essence", () -> new EssenceItem(new Item.Properties()));

    /** Armes signatures (lâchées par leur porteur) et reliques des Dieux (cachées dans des coffres). Dégâts, cadence. */
    public static final List<ArtifactItem.Spec> ARTIFACTS = List.of(
            new ArtifactItem.Spec("solbrand", 8, 1.6, false, false),
            new ArtifactItem.Spec("baton_de_givre", 4, 1.6, false, false),
            new ArtifactItem.Spec("epee_de_cledith", 13, 1.8, false, false),
            new ArtifactItem.Spec("fusil_de_suke", 3, 1.6, false, true),
            new ArtifactItem.Spec("sceptre_d_ignarelle", 5, 1.6, false, false),
            new ArtifactItem.Spec("lame_de_noctalys", 11, 2.0, false, false),
            new ArtifactItem.Spec("pavois_de_bragor", 10, 1.0, false, false),
            new ArtifactItem.Spec("trident_de_zephyrine", 10, 2.2, false, false),
            new ArtifactItem.Spec("compas_d_oriel", 4, 1.6, false, false),
            new ArtifactItem.Spec("hache_de_valdrek", 14, 0.9, false, false),
            new ArtifactItem.Spec("grimoire_de_segaroth", 6, 1.6, false, false),
            new ArtifactItem.Spec("noyau_de_masse", 0, 0, true, false),
            new ArtifactItem.Spec("anneau_de_saturne", 0, 0, true, false),
            new ArtifactItem.Spec("graal_de_rouille", 0, 0, true, false),
            new ArtifactItem.Spec("sablier_du_trou_blanc", 0, 0, true, false),
            new ArtifactItem.Spec("eclat_de_sol", 0, 0, true, false),
            new ArtifactItem.Spec("larme_de_selene", 0, 0, true, false),
            new ArtifactItem.Spec("metronome_de_pulsar", 0, 0, true, false),
            new ArtifactItem.Spec("fragment_de_comete", 0, 0, true, false),
            new ArtifactItem.Spec("graine_de_nebula", 0, 0, true, false),
            new ArtifactItem.Spec("oeil_de_quasar", 0, 0, true, false));
    public static final Map<String, RegistryObject<Item>> ARTIFACT_ITEMS = new LinkedHashMap<>();

    static {
        for (ArtifactItem.Spec spec : ARTIFACTS) ARTIFACT_ITEMS.put(spec.id(), ITEMS.register(spec.id(), () -> new ArtifactItem(spec)));
    }

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
                output.accept(GATE_ITEM.get());
                for (RegistryObject<Item> artifact : ARTIFACT_ITEMS.values()) output.accept(artifact.get());
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
            event.accept(GATE_ITEM);
        }
    }
}
