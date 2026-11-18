package com.chatai.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Blocking HTTP client for Claude and OpenAI-compatible providers. Call it OFF the server thread. */
public final class ApiClient {
    /** Exactly one of text/error is set. error is a short machine-ish code like "HTTP 401" or "timeout". */
    public static final class Result {
        public final String text, error;
        Result(String text, String error) { this.text = text; this.error = error; }
    }

    private ApiClient() {}

    public static String baseUrl(String provider, String custom) {
        switch (provider) {
            case "groq": return "https://api.groq.com/openai/v1";
            case "openrouter": return "https://openrouter.ai/api/v1";
            case "gemini": return "https://generativelanguage.googleapis.com/v1beta/openai";
            case "custom": return custom == null || custom.isBlank() ? null : custom.trim();
            default: return null;
        }
    }

    public static String defaultModel(String provider) {
        switch (provider) {
            case "claude": return "claude-sonnet-5-5";
            case "groq": return "llama-3.3-70b-versatile";
            case "openrouter": return "meta-llama/llama-3.3-70b-instruct:free";
            case "gemini": return "gemini-flash-latest";
            default: return "";
        }
    }

    public static Result call(String provider, String customBase, String key, String model, String system, String user) {
        try {
            String m = model == null || model.isBlank() ? defaultModel(provider) : model.trim();
            if (provider.equals("claude")) return claude(key, m, system, user);
            String base = baseUrl(provider, customBase);
            if (base == null) return new Result(null, "no URL");
            if (m.isEmpty()) return new Result(null, "no model");
            return openAi(base, key, m, system, user);
        } catch (SocketTimeoutException e) {
            return new Result(null, "timeout");
        } catch (Exception e) {
            return new Result(null, "network (" + e.getClass().getSimpleName() + ")");
        }
    }

    private static Result claude(String key, String model, String system, String user) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", 700);
        body.addProperty("system", system);
        JsonArray msgs = new JsonArray();
        msgs.add(msg("user", user));
        body.add("messages", msgs);
        HttpURLConnection c = open("https://api.anthropic.com/v1/messages");
        c.setRequestProperty("x-api-key", key);
        c.setRequestProperty("anthropic-version", "2023-06-01");
        String resp = send(c, body);
        if (resp == null) return new Result(null, "HTTP " + c.getResponseCode());
        JsonElement t = JsonParser.parseString(resp).getAsJsonObject().getAsJsonArray("content").get(0).getAsJsonObject().get("text");
        return t == null || t.isJsonNull() ? new Result(null, "empty reply") : new Result(t.getAsString(), null);
    }

    private static Result openAi(String base, String key, String model, String system, String user) throws Exception {
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", 700);
        body.addProperty("temperature", 0.2);
        JsonArray msgs = new JsonArray();
        msgs.add(msg("system", system));
        msgs.add(msg("user", user));
        body.add("messages", msgs);
        HttpURLConnection c = open(b + "/chat/completions");
        c.setRequestProperty("Authorization", "Bearer " + key);
        String resp = send(c, body);
        if (resp == null) return new Result(null, "HTTP " + c.getResponseCode());
        JsonElement t = JsonParser.parseString(resp).getAsJsonObject().getAsJsonArray("choices").get(0).getAsJsonObject()
                .getAsJsonObject("message").get("content");
        return t == null || !t.isJsonPrimitive() ? new Result(null, "empty reply") : new Result(t.getAsString(), null);
    }

    private static JsonObject msg(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(25000);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("content-type", "application/json");
        return c;
    }

    /** @return response body on HTTP 2xx, or null for any other status. */
    private static String send(HttpURLConnection c, JsonObject body) throws Exception {
        try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        if (code / 100 != 2) return null;
        try (InputStream is = c.getInputStream()) { return new String(is.readAllBytes(), StandardCharsets.UTF_8); }
    }
}
