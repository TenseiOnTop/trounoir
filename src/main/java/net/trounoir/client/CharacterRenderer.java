package net.trounoir.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.trounoir.character.CharacterEntity;

/** Rendu humanoïde de tous les personnages ; la texture (format skin 64x64) vient de leur définition. */
public class CharacterRenderer extends HumanoidMobRenderer<CharacterEntity, PlayerModel<CharacterEntity>> {
    public CharacterRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CharacterEntity entity) {
        return ClientState.textureOf(entity.defId());
    }
}
