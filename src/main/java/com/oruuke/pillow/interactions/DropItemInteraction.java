package com.oruuke.pillow.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DropItemInteraction extends SimpleInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private String itemId = "Pillow_Blank";

    public static final BuilderCodec<DropItemInteraction> CODEC =
            BuilderCodec.builder(DropItemInteraction.class, DropItemInteraction::new,
                            SimpleInteraction.CODEC)
                    .append(new KeyedCodec<>("ItemId", Codec.STRING),
                            (config, value) -> config.itemId = value,
                            (config) -> config.itemId)
                    .documentation("Item id to drop.")
                    .add()
                    .build();

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        try {
            CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
            if (commandBuffer != null) {
                ItemUtils.throwItem(context.getEntity(), new ItemStack(itemId, 1), 0.0F, commandBuffer);
            }

            context.getState().state = InteractionState.Finished;
            super.tick0(firstRun, time, type, context, cooldownHandler);
        } catch (Exception e) {
            LOGGER.atSevere().log("[pillows n plushies] drop item failed: %s", e.getMessage());
            context.getState().state = InteractionState.Failed;
        }
    }
}
