package com.oruuke.pillow.event;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;

public class EffectRejoinEvent {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String[] PLUSHIE_EFFECTS = {"Potion_Morph_Hex_Limited", "Potion_Morph_Hex_Infinite"};

    public static void onPlayerReady(PlayerReadyEvent event) {
        final var ref = event.getPlayer().getReference();
        final var store = ref.getStore();
        final var world = store.getExternalData().getWorld();

        world.execute(() -> {
            if (!ref.isValid()) {
                LOGGER.atWarning().log("Player not in world to remove leftover effect!");
                return;
            }

            final var effectControllerComponent = store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (effectControllerComponent == null) return;

            boolean removed = false;
            for (String effectId : PLUSHIE_EFFECTS) {
                final int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
                if (effectIndex == AssetMapWithIndexes.NOT_FOUND || !effectControllerComponent.hasEffect(effectIndex)) continue;
                effectControllerComponent.removeEffect(ref, effectIndex, store);
                removed = true;
            }
            if (!removed) return;

            LOGGER.atInfo().log("Cleaning up leftover effect...");

            store.putComponent(ref, EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0F));
            final var playerSkinComponent = store.getComponent(ref, PlayerSkinComponent.getComponentType());
            if (playerSkinComponent != null) playerSkinComponent.setNetworkOutdated();

            LOGGER.atInfo().log("Cleaning complete!");
        });
    }
}