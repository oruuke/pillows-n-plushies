package com.oruuke.pillow.systems;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PlushieHandGlowSystem extends EntityTickingSystem<EntityStore> {
    private static final String RIGHT_GLOW_EFFECT = "Plush_Hand_Glow_Right";
    private static final String LEFT_GLOW_EFFECT = "Plush_Hand_Glow_Left";

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(EffectControllerComponent.getComponentType(), PlayerRef.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        final var effects = chunk.getComponent(index, EffectControllerComponent.getComponentType());
        if (effects == null) return;

        final var ref = chunk.getReferenceTo(index);
        final boolean active = PlushieMovementSystem.hasPlushieEffect(effects);

        final var itemInHand = active ? InventoryComponent.getItemInHand(buffer, ref) : null;
        final boolean holdingItem = !ItemStack.isEmpty(itemInHand);
        final boolean offHanded = holdingItem && itemInHand.getItem().getUtility().isUsable();
        final boolean rightHand = holdingItem && !offHanded;
        final var utility = chunk.getComponent(index, InventoryComponent.Utility.getComponentType());
        final boolean twoHanded = rightHand && !itemInHand.getItem().getUtility().isCompatible();
        final boolean leftHand = offHanded || (active && !twoHanded && utility != null && !ItemStack.isEmpty(utility.getActiveItem()));

        updateGlow(ref, effects, RIGHT_GLOW_EFFECT, rightHand, buffer);
        updateGlow(ref, effects, LEFT_GLOW_EFFECT, leftHand, buffer);
    }

    private static void updateGlow(@Nonnull Ref<EntityStore> ref, @Nonnull EffectControllerComponent effects, @Nonnull String effectId, boolean shouldGlow, @Nonnull CommandBuffer<EntityStore> buffer) {
        final int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
        if (effectIndex == AssetMapWithIndexes.NOT_FOUND) return;

        final boolean glowing = effects.hasEffect(effectIndex);
        if (shouldGlow == glowing) return;

        if (shouldGlow) {
            final var effect = EntityEffect.getAssetMap().getAsset(effectIndex);
            if (effect != null) effects.addInfiniteEffect(ref, effectIndex, effect, buffer);
        } else {
            effects.removeEffect(ref, effectIndex, buffer);
        }
    }
}
