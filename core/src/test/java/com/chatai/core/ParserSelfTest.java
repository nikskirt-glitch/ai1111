package com.chatai.core;

/** Dependency-free self test (runs with plain `java`), so CI needs no test framework download. */
public final class ParserSelfTest {
    static int failed = 0, passed = 0;

    static void check(String msg, boolean ru, Intent.Type type, String target, String id, int count) {
        String body = IntentParser.stripPrefix(msg);
        Intent i = body == null ? Intent.of(Intent.Type.UNKNOWN).build() : IntentParser.parse(body, ru);
        boolean ok = i.type == type
                && (target == null ? i.targetPlayer == null : target.equals(i.targetPlayer))
                && (id == null || id.equals(i.id))
                && (count < 0 || count == i.count);
        if (ok) passed++; else { failed++; System.out.println("FAIL: \"" + msg + "\" -> " + i + " (expected " + type + ", target=" + target + ", id=" + id + ", count=" + count + ")"); }
    }

    public static void main(String[] a) {
        check("AI дай мне 64 алмаза", true, Intent.Type.GIVE, null, "diamond", 64);
        check("ai give me 10 apples", false, Intent.Type.GIVE, null, "apple", 10);
        check("AI дай игроку Steve 20 золотых яблок", true, Intent.Type.GIVE, "Steve", "golden_apple", 20);
        check("AI пусть игрок Steve получит 10 яблок", true, Intent.Type.GIVE, "Steve", "apple", 10);
        check("AI give Steve 5 emeralds", false, Intent.Type.GIVE, "Steve", "emerald", 5);
        check("AI дай мне два стака алмазов", true, Intent.Type.GIVE, null, "diamond", 128);
        check("AI создай рядом со мной дом", true, Intent.Type.BUILD, null, "house", -1);
        check("AI построй мне красивую башню", true, Intent.Type.BUILD, null, "tower", -1);
        check("AI построй рядом со мной небольшой замок", true, Intent.Type.BUILD, null, "castle", -1);
        check("AI создай вокруг Steve небольшой дом", true, Intent.Type.BUILD, "Steve", "house", -1);
        check("AI build me a pyramid", false, Intent.Type.BUILD, null, "pyramid", -1);
        check("AI сделай ночь", true, Intent.Type.TIME, null, null, -1);
        check("AI make it day", false, Intent.Type.TIME, null, null, -1);
        check("AI начни дождь", true, Intent.Type.WEATHER, null, null, -1);
        check("AI stop the rain", false, Intent.Type.WEATHER, null, null, -1);
        check("AI дай мне эффект скорости на 2 минуты", true, Intent.Type.EFFECT, null, "speed", -1);
        check("AI create many sheep near me", false, Intent.Type.SPAWN, null, "sheep", 16);
        check("AI создай рядом со мной много овец", true, Intent.Type.SPAWN, null, "sheep", 16);
        check("AI телепортируй меня к игроку Steve", true, Intent.Type.TELEPORT, "Steve", null, -1);
        check("AI tp me to Alex", false, Intent.Type.TELEPORT, "Alex", null, -1);
        check("AI создай вокруг меня праздничный фейерверк", true, Intent.Type.FIREWORK, null, null, -1);
        check("AI вылечи меня", true, Intent.Type.HEAL, null, null, -1);
        check("AI абракадабра", true, Intent.Type.UNKNOWN, null, null, -1);
        check("hello AI", true, Intent.Type.UNKNOWN, null, null, -1);

        Intent e = IntentParser.parse("дай мне эффект скорости на 2 минуты", true);
        if (e.durationSeconds != 120) { failed++; System.out.println("FAIL duration " + e.durationSeconds); } else passed++;

        if (IntentParser.stripPrefix("AIR drop") != null) { failed++; System.out.println("FAIL prefix AIR"); } else passed++;
        if (IntentParser.stripPrefix("ai  hello") == null) { failed++; System.out.println("FAIL prefix ai"); } else passed++;

        for (String k : Structures.KINDS) {
            int n = Structures.blockCount(k);
            if (n <= 0 || n > 4000) { failed++; System.out.println("FAIL structure size " + k + "=" + n); } else passed++;
        }

        RateLimiter rl = new RateLimiter();
        java.util.UUID u = java.util.UUID.randomUUID();
        if (rl.tryAcquire(u, 1000, 3000) != 0) { failed++; System.out.println("FAIL rl1"); } else passed++;
        if (rl.tryAcquire(u, 2000, 3000) <= 0) { failed++; System.out.println("FAIL rl2"); } else passed++;
        if (rl.tryAcquire(u, 4500, 3000) != 0) { failed++; System.out.println("FAIL rl3"); } else passed++;

        System.out.println("passed=" + passed + " failed=" + failed);
        if (failed > 0) System.exit(1);
    }
}
