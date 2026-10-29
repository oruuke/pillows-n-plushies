package com.oruuke.pillow;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.condition.Condition;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.oruuke.pillow.event.EffectRejoinEvent;
import com.oruuke.pillow.modules.entity.condition.IsConnectedCondition;
import com.oruuke.pillow.builtin.HexcodeBuiltin;
import com.oruuke.pillow.interactions.ApplyEffectRandomModelInteraction;
import com.oruuke.pillow.interactions.DropItemInteraction;
import com.oruuke.pillow.interactions.SummonInteraction;
import com.oruuke.pillow.systems.PlushieBlockSnapSystem;
import com.oruuke.pillow.systems.PlushieHandGlowSystem;
import com.oruuke.pillow.systems.PlushieMovementSystem;
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
        //this.registerEntityComponents();
        if (isHexcodeInstalled()) {
            HexcodeBuiltin.setup(this);
        }
        this.registerInteractions();
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, EffectRejoinEvent::onPlayerReady);
        Condition.CODEC.register("Pillow:IsConnected", IsConnectedCondition.class, IsConnectedCondition.CODEC);
        this.getEntityStoreRegistry().registerSystem(new PlushieMovementSystem());
        this.getEntityStoreRegistry().registerSystem(new PlushieBlockSnapSystem());
        this.getEntityStoreRegistry().registerSystem(new PlushieHandGlowSystem());
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

    private boolean isHexcodeInstalled() {
        PluginBase hexcode = PluginManager.get()
                .getPlugin(PluginIdentifier.fromString("Riprod:Hexcode"));
        return hexcode != null && hexcode.isEnabled();
    }

    private void registerInteractions() {
        this.getCodecRegistry(Interaction.CODEC).register("Pillow:DropItem", DropItemInteraction.class, DropItemInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Pillow:Summon", SummonInteraction.class, SummonInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("Pillow:ApplyEffectRandomModel", ApplyEffectRandomModelInteraction.class, ApplyEffectRandomModelInteraction.CODEC);
    }
}
