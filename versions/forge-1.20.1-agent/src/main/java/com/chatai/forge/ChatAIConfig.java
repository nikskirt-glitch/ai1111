package com.chatai.forge;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

/** COMMON config (chatai-agent.toml). On a dedicated server the file on the server is authoritative. */
public final class ChatAIConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, MULTIPLAYER, RESPONSES, SOUNDS, CREATIVE_ONLY, CONFIRM_DANGEROUS,
            AGENT_COMMANDS, AGENT_CONFIRM, LOCAL_FIRST;
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE, API_PROVIDER, API_KEY, API_MODEL, API_BASE_URL;
    public static final ForgeConfigSpec.IntValue COOLDOWN, MAX_ACTIONS, PERM_SELF, PERM_OTHERS, PERM_WORLD, PERM_BUILD, AGENT_MAX_COMMANDS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_COMMANDS;

    public static final List<String> PROVIDERS = Arrays.asList("none", "claude", "groq", "openrouter", "gemini", "custom");

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        ENABLED = b.comment("Enable ChatAI").define("enabled", true);
        MULTIPLAYER = b.comment("Allow use on multiplayer (dedicated or LAN with other players)").define("multiplayer", true);
        LANGUAGE = b.comment("Reply and GUI language: ru or en").define("language", "ru", o -> o instanceof String s && (s.equals("ru") || s.equals("en")));
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
        API_PROVIDER = b.comment("none = classic built-in parser (offline, no AI). Others: claude, groq, openrouter, gemini, custom (any OpenAI-compatible URL).",
                "When a provider is used, the player's request text AND some game info (name, position, command list) are sent to that provider.")
                .define("provider", "none", o -> o instanceof String s && PROVIDERS.contains(s));
        API_KEY = b.comment("API key. Stored in plain text in this file. Never share this file or upload it anywhere.").define("key", "");
        API_MODEL = b.comment("Model name. Empty = provider default.").define("model", "");
        API_BASE_URL = b.comment("Only for provider=custom, e.g. https://example.com/v1").define("baseUrl", "");
        LOCAL_FIRST = b.comment("Try the built-in parser first and only ask the AI if it does not understand (saves free-tier quota)").define("localFirst", false);
        b.pop();
        b.push("agent");
        AGENT_COMMANDS = b.comment("Let the AI run real Minecraft / mod commands for the player. They run with the PLAYER'S OWN permissions (never more).").define("allowCommands", true);
        AGENT_CONFIRM = b.comment("Show the commands and require 'AI confirm' before running them").define("confirmCommands", true);
        AGENT_MAX_COMMANDS = b.comment("Max commands the AI may run per request").defineInRange("maxCommands", 5, 1, 20);
        BLOCKED_COMMANDS = b.comment("Commands the AI may never run (also blocked after 'execute ... run')")
                .defineList("blockedCommands", Arrays.asList("stop", "op", "deop", "ban", "ban-ip", "banlist", "pardon", "pardon-ip", "whitelist",
                        "kick", "save-all", "save-off", "save-on", "reload", "publish", "debug", "perf", "jfr", "setidletimeout", "datapack", "forge"),
                        o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    private ChatAIConfig() {}
}
