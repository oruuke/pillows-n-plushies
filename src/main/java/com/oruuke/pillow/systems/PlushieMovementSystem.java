package com.oruuke.pillow.systems;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.oruuke.pillow.interactions.ApplyEffectRandomModelInteraction;

import javax.annotation.Nonnull;

public class PlushieMovementSystem extends EntityTickingSystem<EntityStore> {
    private static final String[] PLUSHIE_EFFECTS = {"Potion_Morph_Hex_Limited", "Potion_Morph_Hex_Infinite"};
    private static final float JUMP_FORCE_MULTIPLIER = 1.5F;
    private static final float SWIM_JUMP_FORCE_MULTIPLIER = 0.04F;
    private static final float DRAG_MULTIPLIER = 0.40F;
    private static final float CROUCH_MULTIPLIER = 0.2F;
    private static final float MASS_FLAT = 16.0F;

    private static int[] effectIndexes;

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(MovementManager.getComponentType(), EffectControllerComponent.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        final var movementManager = chunk.getComponent(index, MovementManager.getComponentType());
        final var effects = chunk.getComponent(index, EffectControllerComponent.getComponentType());
        final var playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (movementManager == null || effects == null || playerRef == null) return;

        final var settings = movementManager.getSettings();
        final var defaults = movementManager.getDefaultSettings();
        if (settings == null || defaults == null) return;

        final boolean active = hasPlushieEffect(effects);
        if (!active) resetLeftoverScale(index, chunk, buffer);

        final float jumpForce = active ? defaults.jumpForce * JUMP_FORCE_MULTIPLIER : defaults.jumpForce;
        final float swimJumpForce = active ? defaults.swimJumpForce * SWIM_JUMP_FORCE_MULTIPLIER : defaults.swimJumpForce;
        final float drag = active ? defaults.dragCoefficient * DRAG_MULTIPLIER : defaults.dragCoefficient;
        final float crouch = active ? CROUCH_MULTIPLIER : defaults.forwardCrouchSpeedMultiplier;
        final float mass = active ? MASS_FLAT : defaults.mass;

        if (settings.jumpForce == jumpForce && settings.swimJumpForce == swimJumpForce && settings.dragCoefficient == drag && settings.forwardCrouchSpeedMultiplier == crouch && settings.mass == mass) return;

        settings.jumpForce = jumpForce;
        settings.swimJumpForce = swimJumpForce;
        settings.dragCoefficient = drag;
        settings.forwardCrouchSpeedMultiplier = crouch;
        settings.backwardCrouchSpeedMultiplier = crouch;
        settings.strafeCrouchSpeedMultiplier = crouch;
        settings.mass = mass;
        movementManager.update(playerRef.getPacketHandler());
    }

    static boolean hasPlushieEffect(EffectControllerComponent effects) {
        if (effectIndexes == null) {
            effectIndexes = new int[PLUSHIE_EFFECTS.length];
            for (int i = 0; i < PLUSHIE_EFFECTS.length; i++) {
                effectIndexes[i] = EntityEffect.getAssetMap().getIndex(PLUSHIE_EFFECTS[i]);
            }
        }
        for (int effectIndex : effectIndexes) {
            if (effectIndex != AssetMapWithIndexes.NOT_FOUND && effects.hasEffect(effectIndex)) return true;
        }
        return false;
    }

    private static void resetLeftoverScale(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull CommandBuffer<EntityStore> buffer) {
        final var scale = chunk.getComponent(index, EntityScaleComponent.getComponentType());
        if (scale == null || scale.getScale() != ApplyEffectRandomModelInteraction.BLOCK_UPSCALE) return;

        buffer.putComponent(chunk.getReferenceTo(index), EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0F));

        final var playerSkin = chunk.getComponent(index, PlayerSkinComponent.getComponentType());
        if (playerSkin != null) playerSkin.setNetworkOutdated();
    }
}