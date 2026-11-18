package com.chatai.core;

/** All player-facing strings in Russian and English. Language is a config value ("ru" / "en"). */
public final class Messages {
    private Messages() {}

    public static boolean isRu(String lang) { return !"en".equalsIgnoreCase(lang); }

    private static String t(String lang, String ru, String en) { return isRu(lang) ? ru : en; }

    public static String prefix() { return "[ChatAI] "; }

    public static String accepted(String lang, Intent i) {
        switch (i.type) {
            case BUILD: return t(lang, "Запрос принят! Строю: " + structName(i.id, true) + "...", "Request accepted! Building: " + structName(i.id, false) + "...");
            case GIVE: return t(lang, "Запрос принят! Выдаю предметы...", "Request accepted! Giving items...");
            case SPAWN: return t(lang, "Запрос принят! Создаю существ...", "Request accepted! Spawning creatures...");
            default: return t(lang, "Запрос принят!", "Request accepted!");
        }
    }

    public static String done(String lang, Intent i, String detail) {
        switch (i.type) {
            case BUILD: return t(lang, capitalize(structName(i.id, true)) + " успешно построен(а)!", capitalize(structName(i.id, false)) + " built successfully!");
            case GIVE: return t(lang, "Выдано: " + detail, "Given: " + detail);
            case SPAWN: return t(lang, "Создано существ: " + detail, "Spawned: " + detail);
            case TIME: return t(lang, "Время изменено: " + i.label, "Time changed: " + i.label);
            case WEATHER: return t(lang, "Погода изменена: " + i.label, "Weather changed: " + i.label);
            case EFFECT: return t(lang, "Эффект выдан: " + detail, "Effect applied: " + detail);
            case TELEPORT: return t(lang, "Телепортация выполнена.", "Teleported.");
            case FIREWORK: return t(lang, "Фейерверк запущен!", "Fireworks launched!");
            case HEAL: return t(lang, "Здоровье и голод восстановлены.", "Health and hunger restored.");
            default: return t(lang, "Готово.", "Done.");
        }
    }

    public static String notUnderstood(String lang) {
        return t(lang, "Я не совсем понял. Попробуй описать желание по-другому.", "I didn't quite understand. Try describing your wish differently.");
    }

    public static String disabled(String lang) { return t(lang, "ChatAI выключен.", "ChatAI is disabled."); }
    public static String multiplayerOff(String lang) { return t(lang, "Мультиплеер отключён в настройках ChatAI.", "Multiplayer is disabled in ChatAI settings."); }
    public static String noPermission(String lang) { return t(lang, "У тебя нет прав на это действие.", "You don't have permission for this action."); }
    public static String creativeOnly(String lang) { return t(lang, "Это действие доступно только в творческом режиме.", "This action is only available in creative mode."); }
    public static String cooldown(String lang, long ms) { return t(lang, "Подожди ещё " + (ms / 1000 + 1) + " с.", "Please wait " + (ms / 1000 + 1) + " s."); }
    public static String playerNotFound(String lang, String n) { return t(lang, "Игрок " + n + " не найден на сервере.", "Player " + n + " was not found on the server."); }
    public static String tooBig(String lang, int cost, int max) { return t(lang, "Слишком много действий (" + cost + "). Максимум за один запрос: " + max + ".", "Too many actions (" + cost + "). Maximum per request: " + max + "."); }
    public static String confirmNeeded(String lang, String what) {
        return t(lang, "Это крупное изменение мира (" + what + "). Напиши «AI подтверди» (или «AI confirm») в течение 30 секунд, либо «AI отмена».",
                "This is a large world change (" + what + "). Type \"AI confirm\" within 30 seconds, or \"AI cancel\".");
    }
    public static String nothingToConfirm(String lang) { return t(lang, "Нечего подтверждать.", "Nothing to confirm."); }
    public static String cancelled(String lang) { return t(lang, "Отменено.", "Cancelled."); }
    public static String errorInternal(String lang) { return t(lang, "Произошла ошибка при выполнении. Мир не изменён сверх уже сделанного.", "An error occurred while executing. The world was not changed beyond what was already done."); }
    public static String apiNotConfigured(String lang) {
        return t(lang, "Внешний AI API включён, но не настроен (нет ключа) — использую встроенный разбор команд.", "External AI API is enabled but not configured (no key) - using the built-in command parser.");
    }
    public static String apiFailed(String lang) {
        return t(lang, "Внешний AI API недоступен — использую встроенный разбор команд.", "External AI API unavailable - using the built-in command parser.");
    }
    public static String noSpace(String lang) { return t(lang, "Не нашёл подходящего места рядом.", "Could not find a suitable spot nearby."); }
    public static String unknownItem(String lang) { return t(lang, "Такого предмета нет в этой версии игры.", "That item doesn't exist in this game version."); }

    public static String structName(String id, boolean ru) {
        if (id == null) return "?";
        switch (id) {
            case "house": return ru ? "дом" : "house";
            case "tower": return ru ? "башня" : "tower";
            case "castle": return ru ? "замок" : "castle";
            case "bridge": return ru ? "мост" : "bridge";
            case "pyramid": return ru ? "пирамида" : "pyramid";
            case "platform": return ru ? "платформа" : "platform";
            default: return id;
        }
    }

    private static String capitalize(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}
