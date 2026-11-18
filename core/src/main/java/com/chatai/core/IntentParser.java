package com.chatai.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local rule-based parser (keywords, stems, number words) for Russian and English.
 * This is NOT a language model: it recognises a fixed vocabulary and phrasing patterns.
 */
public final class IntentParser {
    private IntentParser() {}

    public static final int MAX_COUNT = 2304; // 36 stacks of 64; adapters apply the configured cap on top

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern NUMBER = Pattern.compile("(\\d{1,5})");

    // stem -> id. Order matters: more specific first (LinkedHashMap).
    private static final Map<String, String> ITEMS = new LinkedHashMap<>();
    private static final Map<String, String> MOBS = new LinkedHashMap<>();
    private static final Map<String, String> EFFECTS = new LinkedHashMap<>();
    private static final Map<String, String> STRUCTS = new LinkedHashMap<>();
    private static final Map<String, Integer> NUMWORDS = new LinkedHashMap<>();

    static {
        // --- items (ru stems / en words) ---
        put(ITEMS, "золотое яблоко|золотых яблок|золотого яблока|золотые яблоки|golden apple|gapple", "golden_apple");
        put(ITEMS, "незерит|netherite ingot", "netherite_ingot");
        put(ITEMS, "алмазный меч|diamond sword", "diamond_sword");
        put(ITEMS, "алмазную кирк|алмазная кирк|алмазной кирк|diamond pickaxe", "diamond_pickaxe");
        put(ITEMS, "алмазную брон|diamond armor", "diamond_chestplate");
        put(ITEMS, "алмаз|diamond", "diamond");
        put(ITEMS, "изумруд|emerald", "emerald");
        put(ITEMS, "золот(ой|ых|ые|ого) слитк|золото|gold ingot|gold", "gold_ingot");
        put(ITEMS, "желез|iron", "iron_ingot");
        put(ITEMS, "уголь|угля|coal", "coal");
        put(ITEMS, "красн(ой|ого|ая) пыл|редстоун|redstone", "redstone");
        put(ITEMS, "яблок|apple", "apple");
        put(ITEMS, "хлеб|bread", "bread");
        put(ITEMS, "стейк|жарен(ое|ого) мяс|steak", "cooked_beef");
        put(ITEMS, "морков|carrot", "carrot");
        put(ITEMS, "факел|torch", "torch");
        put(ITEMS, "доск|planks", "oak_planks");
        put(ITEMS, "бревн|дерев|log|wood", "oak_log");
        put(ITEMS, "камн|булыжник|cobblestone|stone", "cobblestone");
        put(ITEMS, "стекл|glass", "glass");
        put(ITEMS, "тнт|динамит|tnt", "tnt");
        put(ITEMS, "лук|bow", "bow");
        put(ITEMS, "стрел|arrow", "arrow");
        put(ITEMS, "жемчуг|ender pearl|pearl", "ender_pearl");
        put(ITEMS, "элитр|elytra", "elytra");
        put(ITEMS, "фейерверк|firework", "firework_rocket");
        put(ITEMS, "кровать|bed", "red_bed");
        put(ITEMS, "сундук|chest", "chest");
        put(ITEMS, "верстак|crafting table", "crafting_table");
        put(ITEMS, "печь|печку|furnace", "furnace");
        put(ITEMS, "книг|book", "book");
        put(ITEMS, "ведр|bucket", "bucket");
        put(ITEMS, "воды|water bucket", "water_bucket");
        put(ITEMS, "лаву|lava bucket", "lava_bucket");

        // --- mobs ---
        put(MOBS, "овец|овц|овечк|sheep", "sheep");
        put(MOBS, "коров|cow", "cow");
        put(MOBS, "свин|pig", "pig");
        put(MOBS, "куриц|кур|chicken", "chicken");
        put(MOBS, "лошад|конь|коня|horse", "horse");
        put(MOBS, "волк|wolf", "wolf");
        put(MOBS, "кот|кошк|cat", "cat");
        put(MOBS, "попугай|parrot", "parrot");
        put(MOBS, "лис|fox", "fox");
        put(MOBS, "кролик|rabbit", "rabbit");
        put(MOBS, "панд|panda", "panda");
        put(MOBS, "голем|golem", "iron_golem");
        put(MOBS, "жител|villager", "villager");
        put(MOBS, "пчел|bee", "bee");
        put(MOBS, "лягуш|frog", "frog");
        put(MOBS, "осьминог|squid", "squid");

        // --- effects ---
        put(EFFECTS, "скорост|speed|swift", "speed");
        put(EFFECTS, "прыгуч|прыжк|jump", "jump_boost");
        put(EFFECTS, "регенерац|regen", "regeneration");
        put(EFFECTS, "сил[ауы]|strength", "strength");
        put(EFFECTS, "сопротивлен|resistance", "resistance");
        put(EFFECTS, "огнеупорн|огн(я|ю) защит|fire resist", "fire_resistance");
        put(EFFECTS, "невидим|invisib", "invisibility");
        put(EFFECTS, "ночн(ое|ого) зрен|night vision", "night_vision");
        put(EFFECTS, "подводн|water breath|дыхани", "water_breathing");
        put(EFFECTS, "насыщен|saturation", "saturation");
        put(EFFECTS, "поглощен|absorption", "absorption");
        put(EFFECTS, "левитац|levitation|полет|полёт", "levitation");
        put(EFFECTS, "медленное падение|slow fall", "slow_falling");
        put(EFFECTS, "спешк|haste|ускорен(ие|ия) добыч", "haste");
        put(EFFECTS, "удач|luck", "luck");

        // --- structures ---
        put(STRUCTS, "замок|замк|castle|крепост", "castle");
        put(STRUCTS, "башн|tower", "tower");
        put(STRUCTS, "мост|bridge", "bridge");
        put(STRUCTS, "пирамид|pyramid", "pyramid");
        put(STRUCTS, "платформ|площадк|platform", "platform");
        put(STRUCTS, "дом|домик|хижин|house|home|cabin|hut", "house");

        String[] w = {"ноль:0", "один:1", "одну:1", "одно:1", "одного:1", "одной:1", "два:2", "две:2", "двух:2", "три:3", "трех:3", "трёх:3",
                "четыре:4", "пять:5", "шесть:6", "семь:7", "восемь:8", "девять:9", "десять:10", "двадцать:20",
                "тридцать:30", "пятьдесят:50", "сто:100", "сотню:100", "стак:64", "stack:64", "dozen:12", "дюжину:12",
                "one:1", "two:2", "three:3", "four:4", "five:5", "six:6", "seven:7", "eight:8", "nine:9", "ten:10",
                "twenty:20", "fifty:50", "hundred:100", "a:1", "an:1"};
        for (String s : w) { String[] kv = s.split(":"); NUMWORDS.put(kv[0], Integer.parseInt(kv[1])); }
    }

    private static void put(Map<String, Integer> m, String k, int v) { m.put(k, v); }

    private static void put(Map<String, String> m, String keys, String id) {
        for (String k : keys.split("\\|(?![^(]*\\))")) m.put(k, id);
    }

    public static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("[\\p{Punct}&&[^_]]", " ").replaceAll("\\s+", " ").trim();
    }

    /** Returns text after a leading "AI " (case-insensitive) or null if the message isn't an AI request. */
    public static String stripPrefix(String message) {
        if (message == null) return null;
        String m = message.trim();
        if (m.length() < 3) return null;
        String head = m.substring(0, 2);
        if (!head.equalsIgnoreCase("ai") && !head.equalsIgnoreCase("аи") && !head.equalsIgnoreCase("ии")) return null;
        char c = m.charAt(2);
        if (!Character.isWhitespace(c) && c != ',' && c != ':') return null;
        String rest = m.substring(3).trim();
        return rest.isEmpty() ? null : rest;
    }

    public static Intent parse(String original, boolean russian) {
        String text = normalize(original);
        String target = findTarget(original);

        // --- time ---
        if (has(text, "ночь|ночью|night|midnight|полноч")) {
            if (has(text, "сдела|сделай|make|set|change|хочу|пусть|включи|нужна|let")) return time(18000L, target, russian ? "ночь" : "night");
        }
        if (has(text, "день|днем|day|noon|полден|рассвет|утро|morning|sunrise")) {
            if (has(text, "сдела|сделай|make|set|change|хочу|пусть|включи|нужен|let|day time|daytime")) {
                long t = has(text, "полден|noon") ? 6000 : 1000;
                return time(t, target, russian ? "день" : "day");
            }
        }
        if (has(text, "закат|sunset|вечер|evening")) {
            return time(13000L, target, russian ? "закат" : "sunset");
        }

        // --- weather ---
        if (has(text, "гроз|thunder|storm|шторм")) return weather(Intent.Weather.THUNDER, russian ? "гроза" : "thunderstorm");
        if (has(text, "дожд|rain|ливень")) {
            if (has(text, "останов|прекрат|убери|выключ|stop|end|clear|no rain|хватит")) return weather(Intent.Weather.CLEAR, russian ? "ясная погода" : "clear weather");
            return weather(Intent.Weather.RAIN, russian ? "дождь" : "rain");
        }
        if (has(text, "ясн|солнечн|солнц|sunny|clear weather|clear sky|sun|погод.* (хорош|чист)")) {
            return weather(Intent.Weather.CLEAR, russian ? "ясная погода" : "clear weather");
        }

        // --- teleport ---
        if (has(text, "телепорт|tp|teleport|перенес|перенеси|отправь меня|take me to")) {
            String dest = findDestinationPlayer(original);
            if (dest != null) return Intent.of(Intent.Type.TELEPORT).target(dest).label(dest).build();
            // "teleport X to me" => caller handled via target + "ко мне"
            if (target != null && has(text, "ко мне|to me|сюда|here")) {
                return Intent.of(Intent.Type.TELEPORT).target(target).id("to_requester").label(target).build();
            }
        }

        // --- heal / feed ---
        if (has(text, "вылечи|исцели|heal|restore health|накорми|feed|восстанови здоров")) {
            return Intent.of(Intent.Type.HEAL).target(target).label(russian ? "лечение" : "heal").build();
        }

        // --- firework ---
        if (has(text, "салют|фейерверк|firework")) {
            int n = Math.min(Math.max(number(text, 5), 1), 20);
            if (has(text, "дай|выдай|give|мне фейерверк") && has(text, "ракет|rocket|штук|шт|stack")) {
                // fall through to GIVE below
            } else {
                return Intent.of(Intent.Type.FIREWORK).target(target).count(n).label(russian ? "фейерверк" : "fireworks").build();
            }
        }

        // --- effect ---
        String effect = lookup(EFFECTS, text);
        if (effect != null && has(text, "эффект|effect|дай|give|добавь|apply|хочу|хочу быть|хочу иметь|заряди|potion|зелье|buff")) {
            int dur = duration(text, 60);
            int amp = 0;
            Matcher lv = Pattern.compile("(уровен[ья]?|level|lvl)\\s*(\\d)|(\\d)\\s*(уровен|level|lvl)").matcher(text);
            if (lv.find()) { String d = lv.group(2) != null ? lv.group(2) : lv.group(3); amp = Math.max(0, Math.min(Integer.parseInt(d) - 1, 4)); }
            if (has(text, "сильн|strong|максимал|max")) amp = Math.max(amp, 2);
            return Intent.of(Intent.Type.EFFECT).target(target).id(effect).duration(dur).amplifier(amp).label(effect).build();
        }

        // --- spawn mobs ---
        String mob = lookup(MOBS, text);
        if (mob != null && has(text, "создай|создать|спавн|призови|добавь|появ|заспавни|spawn|summon|create|add|make|сделай|дай мне|хочу")) {
            int n = quantity(text, 3);
            return Intent.of(Intent.Type.SPAWN).target(target).id(mob).count(clamp(n)).label(mob).build();
        }

        // --- build ---
        String st = lookup(STRUCTS, text);
        if (st != null && has(text, "постро|строй|создай|создать|возведи|сдела|build|create|construct|make|generate|поставь|хочу")) {
            return Intent.of(Intent.Type.BUILD).target(target).id(st).label(st).build();
        }

        // --- give items ---
        String item = lookup(ITEMS, text);
        if (item != null && has(text, "дай|выдай|получ|give|get|хочу|нужн|want|need|добавь|создай|spawn|пусть|подар|gift|выдать|отправь|кинь|please|пожалуйста|bring|принеси")) {
            int n = quantity(text, 1);
            if (has(text, "стак|stack") && number(text, -1) > 0) n = number(text, 1) * 64;
            return Intent.of(Intent.Type.GIVE).target(target).id(item).count(clamp(n)).label(item).build();
        }

        return Intent.of(Intent.Type.UNKNOWN).build();
    }

    private static int clamp(int n) { return Math.max(1, Math.min(n, MAX_COUNT)); }

    private static Intent time(long ticks, String target, String label) {
        return Intent.of(Intent.Type.TIME).target(target).time(ticks).label(label).build();
    }

    private static Intent weather(Intent.Weather w, String label) {
        return Intent.of(Intent.Type.WEATHER).weather(w).label(label).build();
    }

    private static boolean has(String text, String regex) {
        return Pattern.compile(regex).matcher(text).find();
    }

    private static String lookup(Map<String, String> m, String text) {
        for (Map.Entry<String, String> e : m.entrySet()) {
            // match at a word start so "кот" does not hit "который" and "лук" does not hit "клуб"
            if (Pattern.compile("(^|\\s)" + e.getKey()).matcher(text).find()) return e.getValue();
        }
        return null;
    }

    /** Explicit number in text, or a number word, else fallback. */
    static int number(String text, int fallback) {
        Matcher m = NUMBER.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        for (String tok : text.split(" ")) {
            Integer v = NUMWORDS.get(tok);
            if (v != null) return v;
        }
        return fallback;
    }

    /** Quantity with vague words: "много" -> 16, "немного"/"пару" -> 3 / 2. */
    static int quantity(String text, int fallback) {
        int n = number(text, -1);
        if (n > 0) return n;
        if (has(text, "очень много|огромн|тонн|a ton|huge|loads")) return 64;
        if (has(text, "много|lots|many|bunch|a lot")) return 16;
        if (has(text, "пару|парочку|couple")) return 2;
        if (has(text, "несколько|немного|few|several|some")) return 5;
        return fallback;
    }

    /** Duration in seconds: "2 минуты", "30 секунд", "1 hour"; default seconds. */
    static int duration(String text, int fallback) {
        Matcher m = Pattern.compile("(\\d{1,5})\\s*(секунд|сек|с\\b|second|sec|минут|мин|minute|min|час|hour|hr)").matcher(text);
        if (m.find()) {
            int v = Integer.parseInt(m.group(1));
            String u = m.group(2);
            int s;
            if (u.startsWith("мин") || u.startsWith("min")) s = v * 60;
            else if (u.startsWith("час") || u.startsWith("hou") || u.startsWith("hr")) s = v * 3600;
            else s = v;
            return Math.max(1, Math.min(s, 3600));
        }
        // number words: "две минуты"
        for (String tok : new String[]{"две минуты", "две минуту", "минуту", "пять минут", "десять минут", "two minutes", "a minute", "five minutes"}) {
            if (text.contains(tok)) {
                if (tok.contains("две") || tok.contains("two")) return 120;
                if (tok.contains("пять") || tok.contains("five")) return 300;
                if (tok.contains("десять")) return 600;
                return 60;
            }
        }
        return fallback;
    }

    private static final Pattern PLAYER_RU = Pattern.compile("(?iu)(?:игроку|игрока|игрок|игроком|вокруг|около|возле|рядом с|для|player|to|for|around|near|next to|with)\\s+(?:игрок[ауом]?\\s+|player\\s+)?([A-Za-z0-9_]{3,16})");
    private static final Pattern PLAYER_NAMED = Pattern.compile("(?iu)\\bигрок[ауом]?\\s+([A-Za-z0-9_]{3,16})|\\bplayer\\s+([A-Za-z0-9_]{3,16})");
    private static final Pattern PLAYER_USER_CREATE = Pattern.compile("(?iu)пусть\\s+(?:игрок\\s+)?([A-Za-z0-9_]{3,16})");

    private static final Pattern PLAYER_GIVE_NUM = Pattern.compile("(?iu)\\b(?:give|gift|send|дай|выдай|подари)\\s+([A-Za-z_][A-Za-z0-9_]{2,15})\\s+\\d");

    private static final java.util.Set<String> STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "me", "my", "the", "and", "you", "him", "her", "them", "all", "ai", "to", "for", "near", "with"));

    /** Detects a target player name in the original (case-preserving) text. */
    static String findTarget(String original) {
        Matcher m = PLAYER_USER_CREATE.matcher(original);
        if (m.find()) return validName(m.group(1));
        m = PLAYER_NAMED.matcher(original);
        if (m.find()) return validName(m.group(1) != null ? m.group(1) : m.group(2));
        m = PLAYER_RU.matcher(original);
        while (m.find()) {
            String n = validName(m.group(1));
            if (n != null) return n;
        }
        m = PLAYER_GIVE_NUM.matcher(original);
        if (m.find()) return validName(m.group(1));
        return null;
    }

    /** Destination of a teleport: "телепортируй меня к игроку Steve" / "tp me to Steve". */
    static String findDestinationPlayer(String original) {
        Matcher m = Pattern.compile("(?iu)(?:к игроку|к|to player|to)\\s+(?:игроку\\s+|player\\s+)?([A-Za-z0-9_]{3,16})").matcher(original);
        while (m.find()) {
            String n = validName(m.group(1));
            if (n != null && !n.equalsIgnoreCase("me")) return n;
        }
        return null;
    }

    private static String validName(String n) {
        if (n == null) return null;
        if (!NAME.matcher(n).matches()) return null;
        if (STOP.contains(n.toLowerCase(Locale.ROOT))) return null;
        return n;
    }
}
