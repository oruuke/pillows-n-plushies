package com.oruuke.pillow.builtin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.oruuke.pillow.builtin.system.CastSpellLimiterSystem;
import com.riprod.hexcode.api.event.CastEvent;

public class HexcodeBuiltin {
    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static void setup(JavaPlugin plugin) {
        CastEvent.register(plugin.getEventRegistry(), new CastSpellLimiterSystem());
        LOGGER.atInfo().log("registered spell limiter");
    }
}
