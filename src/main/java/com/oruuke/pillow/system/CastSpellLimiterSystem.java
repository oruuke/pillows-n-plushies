package com.oruuke.pillow.system;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.execution.context.HexContext;
import com.riprod.hexcode.core.common.execution.cast.component.VolatilityComponent;
import com.riprod.hexcode.core.common.execution.root.PlayerHexRoot;

public class CastSpellLimiterSystem extends WorldEventSystem<EntityStore, HexCastEvent> {

    public CastSpellLimiterSystem() {
        super(HexCastEvent.class);
    }
    HytaleLogger L = HytaleLogger.get("");

    @Override
    public void handle(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
                       @Nonnull HexCastEvent event) {
        if (event.isCancelled())
            return;
        HexContext context = event.getContext();
        if (!(context.getHexRoot() instanceof PlayerHexRoot playerRoot))
            return;
        VolatilityComponent tracker = context.volatility();
        if (tracker == null)
            return;

        var current = tracker.getCurrent();
        var set = current - current * resolveInsanity(buffer, playerRoot) / 100;

        L.atInfo().log("stability: %s", set);
        tracker.setCurrent(set);
    }

    public float resolveInsanity(ComponentAccessor<EntityStore> accessor, PlayerHexRoot playerRoot) {
        Ref<EntityStore> ref = playerRoot.getSourceRef(accessor);
        var statMap = ref != null ? accessor.getComponent(ref, EntityStatMap.getComponentType()) : null;

        if (statMap == null)
            return 1.0f;

        int idx = EntityStatType.getAssetMap().getIndex("Insanity");

        if (idx == Integer.MIN_VALUE)
            return 1.0f;

        EntityStatValue stat = statMap.get(idx);
        return stat != null ? stat.getMax() : 1.0f;
    }
}