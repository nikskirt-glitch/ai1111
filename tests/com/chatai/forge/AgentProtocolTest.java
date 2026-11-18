package com.chatai.forge;

import java.util.Arrays;
import java.util.List;

/** Dependency-free test of the AI reply parser and the command filter (needs only gson on the classpath). */
public final class AgentProtocolTest {
    static int ok = 0, bad = 0;
    static void check(String name, boolean c) { if (c) ok++; else { bad++; System.out.println("FAIL: " + name); } }

    public static void main(String[] a) {
        List<String> blocked = Arrays.asList("stop", "op", "deop", "ban", "whitelist", "reload", "forge");

        AgentProtocol.Plan p = AgentProtocol.parse("{\"mode\":\"commands\",\"commands\":[\"/give @s diamond 64\",\"time set day\"],\"say\":\"Готово\"}");
        check("commands kind", p.kind == AgentProtocol.Kind.COMMANDS);
        check("commands count", p.commands.size() == 2);
        check("slash stripped", p.commands.get(0).equals("give @s diamond 64"));

        p = AgentProtocol.parse("Вот ответ:\n```json\n{\"mode\":\"intent\",\"intent\":{\"type\":\"GIVE\",\"id\":\"diamond\",\"count\":64}}\n```");
        check("intent in markdown", p.kind == AgentProtocol.Kind.INTENT && p.intent.id.equals("diamond") && p.intent.count == 64);

        p = AgentProtocol.parse("{\"mode\":\"chat\",\"say\":\"Привет\"}");
        check("chat", p.kind == AgentProtocol.Kind.CHAT && p.say.equals("Привет"));

        p = AgentProtocol.parse("{\"type\":\"WEATHER\",\"weather\":\"RAIN\"}");
        check("legacy flat intent", p.kind == AgentProtocol.Kind.INTENT && p.intent.weather.name().equals("RAIN"));

        check("garbage", AgentProtocol.parse("не json").kind == AgentProtocol.Kind.INVALID);
        check("null", AgentProtocol.parse(null).kind == AgentProtocol.Kind.INVALID);
        check("bad build id", AgentProtocol.parse("{\"mode\":\"intent\",\"intent\":{\"type\":\"BUILD\",\"id\":\"skyscraper\"}}").kind == AgentProtocol.Kind.INVALID);
        check("bad id chars", AgentProtocol.parse("{\"mode\":\"intent\",\"intent\":{\"type\":\"GIVE\",\"id\":\"diamond; rm -rf\"}}").kind == AgentProtocol.Kind.INVALID);
        check("empty commands", AgentProtocol.parse("{\"mode\":\"commands\",\"commands\":[]}").kind == AgentProtocol.Kind.INVALID);
        check("too long cmd", AgentProtocol.parse("{\"mode\":\"commands\",\"commands\":[\"" + "x".repeat(300) + "\"]}").kind == AgentProtocol.Kind.INVALID);
        check("nested object command", AgentProtocol.parse("{\"mode\":\"commands\",\"commands\":[{\"a\":1}]}").kind == AgentProtocol.Kind.INVALID);
        check("count clamped", AgentProtocol.parse("{\"mode\":\"intent\",\"intent\":{\"type\":\"GIVE\",\"id\":\"apple\",\"count\":999999}}").intent.count == 2304);
        check("tp needs target", AgentProtocol.parse("{\"mode\":\"intent\",\"intent\":{\"type\":\"TELEPORT\"}}").kind == AgentProtocol.Kind.INVALID);

        check("allow give", AgentProtocol.checkCommand("give @s diamond 1", blocked) == null);
        check("allow mod cmd", AgentProtocol.checkCommand("mymod spawnboss zombie", blocked) == null);
        check("block op", AgentProtocol.checkCommand("op Steve", blocked) != null);
        check("block /OP caps", AgentProtocol.checkCommand("/OP Steve", blocked) != null);
        check("block minecraft:stop", AgentProtocol.checkCommand("minecraft:stop", blocked) != null);
        check("block via execute run", AgentProtocol.checkCommand("execute as @a run op @s", blocked) != null);
        check("block via nested run", AgentProtocol.checkCommand("execute as @a run execute at @s run stop", blocked) != null);
        check("block run /slash", AgentProtocol.checkCommand("execute run /ban Steve", blocked) != null);
        check("allow word 'op' as argument", AgentProtocol.checkCommand("say please op me", blocked) == null);
        check("newline rejected", AgentProtocol.checkCommand("say hi\nstop", blocked) != null);
        check("prompt mentions limit", AgentProtocol.systemPrompt("ctx", 7).contains("At most 7 commands"));

        System.out.println("passed=" + ok + " failed=" + bad);
        if (bad > 0) System.exit(1);
    }
}
