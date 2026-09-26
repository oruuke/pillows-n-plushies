package com.oruuke.pillow;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.oruuke.pillow.interactions.DropItemInteraction;
import com.riprod.patchly.PatchManager;

import java.util.logging.Level;

public class PillowsNPlushies extends JavaPlugin {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final PatchManager patchManager;
    private static PillowsNPlushies instance;

    public PillowsNPlushies(JavaPluginInit init) {
        super(init);
        patchManager = new PatchManager(this);
        instance = this;
        //LOGGER.atInfo().log("welcome to " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        patchManager.install();
        this.registerEntityComponents();
        this.registerInteractions();
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("starting pillows n' plushies!");
    }

    public static PillowsNPlushies get() {
        return instance;
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("shutting down pillows n' plushies!");
        patchManager.shutdown();
    }

    private void registerEntityComponents() {
        ComponentRegistryProxy<EntityStore> entityStoreRegistry = this.getEntityStoreRegistry();
    }

    private void registerInteractions() {
        this.getCodecRegistry(Interaction.CODEC).register("Pillows:DropItem", DropItemInteraction.class, DropItemInteraction.CODEC);
    }
}
