package com.chatai.forge;

import net.minecraftforge.common.ForgeConfigSpec;

/** COMMON config (chatai-common.toml). On a dedicated server this file on the server is authoritative. */
public final class ChatAIConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, MULTIPLAYER, RESPONSES, SOUNDS, API_ENABLED, CREATIVE_ONLY, CONFIRM_DANGEROUS;
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE, API_PROVIDER, API_KEY, API_MODEL;
    public static final ForgeConfigSpec.IntValue COOLDOWN, MAX_ACTIONS, PERM_SELF, PERM_OTHERS, PERM_WORLD, PERM_BUILD;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("Enable ChatAI").define("enabled", true);
        MULTIPLAYER = b.comment("Allow use on multiplayer (dedicated or LAN with other players)").define("multiplayer", true);
        LANGUAGE = b.comment("Reply language: ru or en").define("language", "ru", o -> o instanceof String s && (s.equals("ru") || s.equals("en")));
        RESPONSES = b.comment("Show ChatAI responses in chat").define("responses", true);
        SOUNDS = b.comment("Play a sound when a command completes").define("sounds", true);
        b.pop();
        b.push("limits");
        COOLDOWN = b.comment("Seconds between requests per player").defineInRange("cooldownSeconds", 3, 0, 120);
        MAX_ACTIONS = b.comment("Max actions (blocks / items / entities) per request").defineInRange("maxActionsPerRequest", 2000, 1, 5000);
        b.pop();
        b.push("safety");
        CREATIVE_ONLY = b.comment("Only players in creative mode may use ChatAI").define("creativeOnly", false);
        CONFIRM_DANGEROUS = b.comment("Require 'AI confirm' for large world changes (buildings, 20+ mobs)").define("confirmDangerous", true);
        b.comment("Minimum vanilla permission level (0 = everyone, 2 = operators, 4 = owner)").push("permissions");
        PERM_SELF = b.defineInRange("self", 0, 0, 4);
        PERM_OTHERS = b.comment("Needed when the action targets another player").defineInRange("others", 2, 0, 4);
        PERM_WORLD = b.comment("Needed for weather and time").defineInRange("world", 2, 0, 4);
        PERM_BUILD = b.comment("Needed for building").defineInRange("build", 0, 0, 4);
        b.pop();
        b.pop();
        b.push("api");
        API_ENABLED = b.comment("Send requests to an external AI API. Your text leaves the game when enabled. Falls back to the local parser on any failure.").define("enabled", false);
        API_PROVIDER = b.comment("Provider: claude").define("provider", "claude", o -> o instanceof String s && s.equals("claude"));
        API_KEY = b.comment("API key (stored in plain text in this file; never share it)").define("key", "");
        API_MODEL = b.define("model", "claude-sonnet-5-5");
        b.pop();
        SPEC = b.build();
    }

    private ChatAIConfig() {}
}
