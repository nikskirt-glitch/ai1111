package com.chatai.forge;

import com.chatai.core.Intent;
import com.chatai.core.Structures;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pure (no Minecraft types) protocol between the mod and the AI model: builds the system prompt,
 * parses and STRICTLY validates the model's JSON reply. Nothing from the model is ever executed as code;
 * commands are plain strings that the server runs with the player's own permissions after these checks.
 */
public final class AgentProtocol {
    public enum Kind { INTENT, COMMANDS, CHAT, INVALID }

    public static final class Plan {
        public final Kind kind;
        public final Intent intent;
        public final List<String> commands;
        public final String say;

        Plan(Kind kind, Intent intent, List<String> commands, String say) {
            this.kind = kind; this.intent = intent; this.commands = commands; this.say = say == null ? "" : say;
        }

        static Plan invalid() { return new Plan(Kind.INVALID, null, new ArrayList<>(), ""); }
    }

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,40}");
    public static final int MAX_COMMAND_LEN = 256;
    public static final int HARD_MAX_COMMANDS = 20;

    private AgentProtocol() {}

    public static String systemPrompt(String context, int maxCommands) {
        return "You are the brain of ChatAI, a Minecraft 1.20.1 (Forge) mod. A player types a wish in chat (Russian or English). "
                + "Reply with ONE JSON object and NOTHING else (no markdown, no explanations).\n"
                + "Choose one mode:\n"
                + "1) {\"mode\":\"intent\",\"intent\":{\"type\":\"GIVE|TIME|WEATHER|EFFECT|SPAWN|BUILD|TELEPORT|FIREWORK|HEAL\","
                + "\"target\":null or exact player name,\"id\":minecraft id without namespace, or house|tower|castle|bridge|pyramid|platform for BUILD,"
                + "\"count\":int,\"durationSeconds\":int,\"amplifier\":int,\"weather\":\"CLEAR|RAIN|THUNDER\",\"time\":ticks 0-23999}} "
                + "for simple built-in actions (ready-made buildings, items, mobs, weather, time).\n"
                + "2) {\"mode\":\"commands\",\"commands\":[\"give @s minecraft:diamond_sword{Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5}]} 1\"],"
                + "\"say\":\"short message for the player\"} to run REAL Minecraft or mod commands as the player. At most " + maxCommands + " commands. "
                + "No leading slash. Use @s for the player. The player's own permissions apply, so commands they cannot use will simply fail. "
                + "For commands of other mods use ONLY the exact usage listed below. Use this mode for anything the built-in intents cannot do "
                + "(enchanted items, custom setblock/fill/summon, gamerules, scoreboard, mod features, etc.).\n"
                + "3) {\"mode\":\"chat\",\"say\":\"answer\"} for questions or when the wish is impossible.\n"
                + "Rules: never output server-admin commands (stop, op, ban, whitelist, reload, ...). Keep fill/clone volumes reasonable. "
                + "Write \"say\" in the language of the player's message.\n"
                + "Game info:\n" + context;
    }

    public static Plan parse(String text) {
        if (text == null) return Plan.invalid();
        int a = text.indexOf('{'), z = text.lastIndexOf('}');
        if (a < 0 || z < a) return Plan.invalid();
        try {
            JsonObject o = JsonParser.parseString(text.substring(a, z + 1)).getAsJsonObject();
            String say = clip(str(o, "say"), 300);
            String mode = str(o, "mode");
            if (mode == null) mode = o.has("type") ? "intent" : (o.has("commands") ? "commands" : "chat");
            switch (mode.toLowerCase(Locale.ROOT)) {
                case "intent": {
                    JsonObject io = o.has("intent") && o.get("intent").isJsonObject() ? o.getAsJsonObject("intent") : o;
                    Optional<Intent> i = validateIntent(io);
                    return i.isPresent() ? new Plan(Kind.INTENT, i.get(), new ArrayList<>(), say) : Plan.invalid();
                }
                case "commands": {
                    if (!o.has("commands") || !o.get("commands").isJsonArray()) return Plan.invalid();
                    JsonArray arr = o.getAsJsonArray("commands");
                    List<String> cmds = new ArrayList<>();
                    for (JsonElement e : arr) {
                        if (!e.isJsonPrimitive()) return Plan.invalid();
                        String c = normalizeCommand(e.getAsString());
                        if (c.isEmpty()) continue;
                        if (c.length() > MAX_COMMAND_LEN) return Plan.invalid();
                        cmds.add(c);
                    }
                    if (cmds.isEmpty() || cmds.size() > HARD_MAX_COMMANDS) return Plan.invalid();
                    return new Plan(Kind.COMMANDS, null, cmds, say);
                }
                case "chat":
                    return say.isEmpty() ? Plan.invalid() : new Plan(Kind.CHAT, null, new ArrayList<>(), say);
                default:
                    return Plan.invalid();
            }
        } catch (Exception e) {
            return Plan.invalid();
        }
    }

    public static String normalizeCommand(String c) {
        String s = c == null ? "" : c.trim();
        while (s.startsWith("/")) s = s.substring(1).trim();
        return s;
    }

    /** @return null if the command may be run, otherwise a short reason it was rejected. */
    public static String checkCommand(String cmd, Collection<String> blocked) {
        for (int i = 0; i < cmd.length(); i++) {
            if (cmd.charAt(i) < 32) return "control characters";
        }
        List<String> block = new ArrayList<>();
        for (String b : blocked) block.add(root(b));
        String[] t = cmd.trim().split("\\s+");
        if (t.length == 0 || t[0].isEmpty()) return "empty";
        if (block.contains(root(t[0]))) return "blocked: " + root(t[0]);
        for (int i = 0; i + 1 < t.length; i++) {
            if (t[i].equalsIgnoreCase("run") && block.contains(root(t[i + 1]))) return "blocked: " + root(t[i + 1]);
        }
        return null;
    }

    private static String root(String s) {
        String r = s.trim().toLowerCase(Locale.ROOT);
        while (r.startsWith("/")) r = r.substring(1);
        if (r.startsWith("minecraft:")) r = r.substring("minecraft:".length());
        return r;
    }

    static Optional<Intent> validateIntent(JsonObject o) {
        Intent.Type t;
        try { t = Intent.Type.valueOf(o.get("type").getAsString().toUpperCase(Locale.ROOT)); } catch (Exception e) { return Optional.empty(); }
        if (t == Intent.Type.UNKNOWN || t == Intent.Type.CLEAR_WEATHER_HELP) return Optional.empty();
        Intent.Builder b = Intent.of(t);
        if (o.has("target") && !o.get("target").isJsonNull()) {
            String n = o.get("target").getAsString();
            if (n.matches("[A-Za-z0-9_]{3,16}")) b.target(n);
        }
        String id = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsString().toLowerCase(Locale.ROOT).replace("minecraft:", "") : null;
        if (id != null && !ID.matcher(id).matches()) return Optional.empty();
        b.id(id).label(id == null ? t.name() : id);
        b.count(clamp(o, "count", 1, 1, 2304)).duration(clamp(o, "durationSeconds", 60, 1, 3600)).amplifier(clamp(o, "amplifier", 0, 0, 4));
        if (o.has("weather")) {
            try { b.weather(Intent.Weather.valueOf(o.get("weather").getAsString().toUpperCase(Locale.ROOT))); } catch (Exception ignored) {}
        }
        b.time(clamp(o, "time", 1000, 0, 23999));
        Intent built = b.build();
        if (t == Intent.Type.BUILD && !Structures.known(id)) return Optional.empty();
        if ((t == Intent.Type.GIVE || t == Intent.Type.SPAWN || t == Intent.Type.EFFECT) && id == null) return Optional.empty();
        if (t == Intent.Type.TELEPORT && built.targetPlayer == null) return Optional.empty();
        return Optional.of(built);
    }

    private static int clamp(JsonObject o, String k, int def, int lo, int hi) {
        try { return Math.max(lo, Math.min(hi, o.get(k).getAsInt())); } catch (Exception e) { return def; }
    }

    private static String str(JsonObject o, String k) {
        try { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null; } catch (Exception e) { return null; }
    }

    private static String clip(String s, int n) {
        if (s == null) return "";
        s = s.replace('\n', ' ').trim();
        return s.length() > n ? s.substring(0, n) : s;
    }
}
