package com.chatai.forge;

import com.chatai.core.Intent;
import com.chatai.core.Structures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Optional Claude API translation of free text into ONE Intent. The model only returns a small JSON
 * description; it is strictly validated and never executed as code. Blocking: call off the server thread.
 */
public final class ApiClient {
    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,40}");
    private static final String SYSTEM = "You convert a Minecraft player's request into ONE JSON object and output nothing else. "
            + "Schema: {\"type\":\"GIVE|TIME|WEATHER|EFFECT|SPAWN|BUILD|TELEPORT|FIREWORK|HEAL|UNKNOWN\","
            + "\"target\":null or exact player name,\"id\":minecraft id without namespace (item/entity/effect) or one of "
            + "house|tower|castle|bridge|pyramid|platform for BUILD,\"count\":int,\"durationSeconds\":int,\"amplifier\":int,"
            + "\"weather\":\"CLEAR|RAIN|THUNDER\",\"time\":ticks 0-23999}. For TELEPORT, target is the destination player. "
            + "If the request is not one of these, use UNKNOWN.";

    private ApiClient() {}

    public static Optional<Intent> translate(String text, String key, String model) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", 200);
            body.addProperty("system", SYSTEM);
            JsonArray msgs = new JsonArray();
            JsonObject m = new JsonObject();
            m.addProperty("role", "user");
            m.addProperty("content", text);
            msgs.add(m);
            body.add("messages", msgs);
            HttpURLConnection c = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(15000);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("content-type", "application/json");
            c.setRequestProperty("x-api-key", key);
            c.setRequestProperty("anthropic-version", "2023-06-01");
            try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            if (c.getResponseCode() != 200) return Optional.empty();
            String resp;
            try (InputStream is = c.getInputStream()) { resp = new String(is.readAllBytes(), StandardCharsets.UTF_8); }
            String out = JsonParser.parseString(resp).getAsJsonObject()
                    .getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
            int a = out.indexOf('{'), z = out.lastIndexOf('}');
            if (a < 0 || z < a) return Optional.empty();
            return validate(JsonParser.parseString(out.substring(a, z + 1)).getAsJsonObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    static Optional<Intent> validate(JsonObject o) {
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
        if (t == Intent.Type.BUILD && !Structures.known(id)) return Optional.empty();
        if ((t == Intent.Type.GIVE || t == Intent.Type.SPAWN || t == Intent.Type.EFFECT) && id == null) return Optional.empty();
        if (t == Intent.Type.TELEPORT && b.build().targetPlayer == null) return Optional.empty();
        return Optional.of(b.build());
    }

    private static int clamp(JsonObject o, String k, int def, int lo, int hi) {
        try { return Math.max(lo, Math.min(hi, o.get(k).getAsInt())); } catch (Exception e) { return def; }
    }
}
