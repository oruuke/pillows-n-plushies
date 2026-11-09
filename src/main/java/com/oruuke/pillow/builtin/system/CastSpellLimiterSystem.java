package com.oruuke.pillow.builtin.system;

import java.util.function.Consumer;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.CastEvent;
import com.riprod.hexcode.core.common.execution.context.HexContext;
import com.riprod.hexcode.core.common.execution.cast.component.VolatilityComponent;

public class CastSpellLimiterSystem implements Consumer<CastEvent> {

    @Override
    public void accept(CastEvent event) {
        if (event.isCancelled())
            return;
        HexContext context = event.getContext();
        VolatilityComponent tracker = context.volatility();
        if (tracker == null)
            return;

        var playerRoot = context.getRootRef();
        var buffer = context.getAccessor();

        var current = tracker.getCurrent();
        var set = current - current * resolveInsanity(buffer, playerRoot) / 100;

        tracker.setCurrent(set);
    }

    public float resolveInsanity(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
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