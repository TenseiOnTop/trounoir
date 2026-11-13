package net.trounoir.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.character.CharacterEntity;

/** Rendu humanoïde de tous les personnages ; la texture (format skin 64x64) vient de leur définition. */
public class CharacterRenderer extends HumanoidMobRenderer<CharacterEntity, PlayerModel<CharacterEntity>> {
    public CharacterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CharacterEntity entity) {
        return ClientState.textureOf(entity.defId(), entity.getUUID());
    }

    /** La couleur du nom dit le danger : vert gagnable, orange égal, rouge difficile, sombre sans espoir. */
    public static int difficultyColor(int characterRank) {
        int diff = characterRank - ClientState.rank;
        if (diff <= -1) return 0x6EE787;
        if (diff == 0) return 0xFFB24D;
        if (diff <= 2) return 0xFF5050;
        return 0x16101E;
    }

    @Override
    protected void renderNameTag(CharacterEntity entity, Component name, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTick) {
        ClientState.CharacterInfo info = ClientState.CHARACTERS.get(entity.defId());
        Component colored = info == null ? name : name.copy().withColor(difficultyColor(info.rank()));
        super.renderNameTag(entity, colored, poseStack, buffer, packedLight, partialTick);
    }
}
