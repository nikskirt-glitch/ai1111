package com.chatai.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Builds the "game info" block for the model. Runs on the server thread. Only lists commands the player may use. */
public final class AgentContext {
    private static final Set<String> VANILLA = new HashSet<>(Arrays.asList(
            "advancement", "attribute", "ban", "ban-ip", "banlist", "bossbar", "clear", "clone", "damage", "data", "datapack", "debug",
            "defaultgamemode", "deop", "difficulty", "effect", "enchant", "execute", "experience", "xp", "fill", "fillbiome", "forceload",
            "function", "gamemode", "gamerule", "give", "help", "item", "jfr", "kick", "kill", "list", "locate", "loot", "me", "msg", "op",
            "pardon", "pardon-ip", "particle", "perf", "place", "playsound", "publish", "random", "recipe", "reload", "return", "ride",
            "save-all", "save-off", "save-on", "say", "schedule", "scoreboard", "seed", "setblock", "setidletimeout", "setworldspawn",
            "spawnpoint", "spectate", "spreadplayers", "stop", "stopsound", "summon", "tag", "team", "teammsg", "teleport", "tell", "tellraw",
            "time", "title", "tm", "tp", "trigger", "w", "weather", "whitelist", "worldborder", "forge", "chatai"));
    private static final int MAX_MOD_COMMANDS = 25, MAX_USAGE_LEN = 160;

    private AgentContext() {}

    public static String build(ServerPlayer p) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("Player: ").append(p.getGameProfile().getName())
                    .append(", dimension ").append(p.level().dimension().location())
                    .append(", pos ").append(p.getBlockX()).append(' ').append(p.getBlockY()).append(' ').append(p.getBlockZ())
                    .append(", gamemode ").append(p.gameMode.getGameModeForPlayer().getName())
                    .append(", health ").append((int) p.getHealth())
                    .append(", holding ").append(ForgeRegistries.ITEMS.getKey(p.getMainHandItem().getItem()))
                    .append(", operator ").append(p.hasPermissions(2)).append('\n');
            String[] names = p.server.getPlayerNames();
            sb.append("Online players: ").append(String.join(", ", Arrays.copyOf(names, Math.min(names.length, 30)))).append('\n');

            CommandSourceStack src = p.createCommandSourceStack();
            CommandDispatcher<CommandSourceStack> d = p.server.getCommands().getDispatcher();
            Set<String> vanilla = new TreeSet<>();
            Set<CommandNode<CommandSourceStack>> mods = new HashSet<>();
            for (CommandNode<CommandSourceStack> n : d.getRoot().getChildren()) {
                if (!n.canUse(src)) continue;
                if (VANILLA.contains(n.getName())) vanilla.add(n.getName());
                else mods.add(n);
            }
            sb.append("Vanilla commands the player may use: ").append(String.join(", ", vanilla)).append('\n');
            sb.append("Other (mod/server) commands the player may use, with exact usage:\n");
            int count = 0;
            for (CommandNode<CommandSourceStack> n : new TreeSet<>(mods)) {
                if (count++ >= MAX_MOD_COMMANDS) break;
                String usage;
                try {
                    Map<CommandNode<CommandSourceStack>, String> m = d.getSmartUsage(n, src);
                    usage = String.join(" | ", m.values());
                } catch (Exception e) {
                    usage = "";
                }
                if (usage.length() > MAX_USAGE_LEN) usage = usage.substring(0, MAX_USAGE_LEN) + "...";
                sb.append("  /").append(n.getName()).append(' ').append(usage).append('\n');
            }
            if (mods.isEmpty()) sb.append("  (none)\n");
        } catch (Exception e) {
            sb.append("(game info unavailable)\n");
        }
        return sb.toString();
    }
}
