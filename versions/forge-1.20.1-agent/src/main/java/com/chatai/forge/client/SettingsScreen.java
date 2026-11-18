package com.chatai.forge.client;

import com.chatai.forge.ChatAIConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** "ChatAI Settings" (Russian / English): dark + purple, category tabs, Save / Reset / Back. Edits the local config. */
public final class SettingsScreen extends Screen {
    private static final int BG_TOP = 0xFF120A1F, BG_BOTTOM = 0xFF241047, ACCENT = 0xFFA855F7, DIM = 0xFFB9A7D6, TEXT = 0xFFEADCFF;
    private static final int LABEL_W = 168, CTRL_X = 170, CTRL_W = 130;

    private final Screen parent;
    private int tab = 0;
    // working copy (applied only on Save)
    private boolean enabled, multiplayer, responses, sounds, creativeOnly, confirm, agentCommands, agentConfirm, localFirst;
    private String language, provider, apiKey, model, baseUrl;
    private int cooldown, maxActions, permSelf, permOthers, permWorld, permBuild, maxCommands;
    private EditBox keyBox, modelBox, urlBox;
    private final List<Object[]> labels = new ArrayList<>();

    public SettingsScreen(Screen parent) {
        super(Component.literal("ChatAI Settings"));
        this.parent = parent;
        load();
    }

    private boolean ru() { return "ru".equals(language); }
    private String t(String en, String ru) { return ru() ? ru : en; }

    private void load() {
        enabled = ChatAIConfig.ENABLED.get(); multiplayer = ChatAIConfig.MULTIPLAYER.get();
        responses = ChatAIConfig.RESPONSES.get(); sounds = ChatAIConfig.SOUNDS.get();
        creativeOnly = ChatAIConfig.CREATIVE_ONLY.get(); confirm = ChatAIConfig.CONFIRM_DANGEROUS.get();
        agentCommands = ChatAIConfig.AGENT_COMMANDS.get(); agentConfirm = ChatAIConfig.AGENT_CONFIRM.get();
        localFirst = ChatAIConfig.LOCAL_FIRST.get(); language = ChatAIConfig.LANGUAGE.get();
        provider = ChatAIConfig.API_PROVIDER.get(); apiKey = ChatAIConfig.API_KEY.get();
        model = ChatAIConfig.API_MODEL.get(); baseUrl = ChatAIConfig.API_BASE_URL.get();
        cooldown = ChatAIConfig.COOLDOWN.get(); maxActions = ChatAIConfig.MAX_ACTIONS.get();
        permSelf = ChatAIConfig.PERM_SELF.get(); permOthers = ChatAIConfig.PERM_OTHERS.get();
        permWorld = ChatAIConfig.PERM_WORLD.get(); permBuild = ChatAIConfig.PERM_BUILD.get();
        maxCommands = ChatAIConfig.AGENT_MAX_COMMANDS.get();
    }

    private void defaults() {
        enabled = true; multiplayer = true; responses = true; sounds = true; creativeOnly = false; confirm = true;
        agentCommands = true; agentConfirm = true; localFirst = false; language = "ru"; provider = "none";
        apiKey = ""; model = ""; baseUrl = ""; cooldown = 3; maxActions = 2000;
        permSelf = 0; permOthers = 2; permWorld = 2; permBuild = 0; maxCommands = 5;
    }

    private void remember() {
        if (keyBox != null) apiKey = keyBox.getValue().trim();
        if (modelBox != null) model = modelBox.getValue().trim();
        if (urlBox != null) baseUrl = urlBox.getValue().trim();
    }

    private void save() {
        remember();
        set(ChatAIConfig.ENABLED, enabled); set(ChatAIConfig.MULTIPLAYER, multiplayer);
        set(ChatAIConfig.RESPONSES, responses); set(ChatAIConfig.SOUNDS, sounds);
        set(ChatAIConfig.CREATIVE_ONLY, creativeOnly); set(ChatAIConfig.CONFIRM_DANGEROUS, confirm);
        set(ChatAIConfig.AGENT_COMMANDS, agentCommands); set(ChatAIConfig.AGENT_CONFIRM, agentConfirm);
        set(ChatAIConfig.LOCAL_FIRST, localFirst); set(ChatAIConfig.LANGUAGE, language);
        set(ChatAIConfig.API_PROVIDER, provider); set(ChatAIConfig.API_KEY, apiKey);
        set(ChatAIConfig.API_MODEL, model); set(ChatAIConfig.API_BASE_URL, baseUrl);
        set(ChatAIConfig.COOLDOWN, cooldown); set(ChatAIConfig.MAX_ACTIONS, maxActions);
        set(ChatAIConfig.PERM_SELF, permSelf); set(ChatAIConfig.PERM_OTHERS, permOthers);
        set(ChatAIConfig.PERM_WORLD, permWorld); set(ChatAIConfig.PERM_BUILD, permBuild);
        set(ChatAIConfig.AGENT_MAX_COMMANDS, maxCommands);
        ChatAIConfig.SPEC.save();
    }

    private static <T> void set(ForgeConfigSpec.ConfigValue<T> v, T x) { v.set(x); }

    private String providerName(String p) {
        switch (p) {
            case "claude": return "Claude";
            case "groq": return t("Groq (free)", "Groq (бесплатно)");
            case "openrouter": return t("OpenRouter (free)", "OpenRouter (беспл.)");
            case "gemini": return t("Gemini (free)", "Gemini (бесплатно)");
            case "custom": return t("Custom (OpenAI API)", "Свой (OpenAI API)");
            default: return t("No AI (classic)", "Без ИИ (классика)");
        }
    }

    private String nextProvider() {
        List<String> all = ChatAIConfig.PROVIDERS;
        return all.get((all.indexOf(provider) + 1) % all.size());
    }

    @Override
    protected void init() {
        labels.clear();
        keyBox = null; modelBox = null; urlBox = null;
        String[] tabs = {t("General", "Общие"), t("Limits", "Лимиты"), t("Safety", "Защита"), t("AI", "ИИ"), t("Agent", "Агент")};
        int cx = width / 2, left = cx - 150, y = 62, tw = 58;
        for (int i = 0; i < tabs.length; i++) {
            final int ti = i;
            addRenderableWidget(Button.builder(Component.literal((i == tab ? "> " : "") + tabs[i]), b -> {
                if (tab != ti) { remember(); tab = ti; rebuildWidgets(); }
            }).bounds(left + i * (tw + 2), 34, tw, 20).build());
        }
        switch (tab) {
            case 0:
                y = toggle(left, y, t("Enable ChatAI", "Включить ChatAI"), () -> enabled, v -> enabled = v);
                y = toggle(left, y, t("Enable Multiplayer", "Мультиплеер"), () -> multiplayer, v -> multiplayer = v);
                y = cycle(left, y, t("AI Language", "Язык"), () -> ru() ? "Русский" : "English", () -> { remember(); language = ru() ? "en" : "ru"; rebuildWidgets(); });
                y = toggle(left, y, t("Response Messages", "Ответы в чате"), () -> responses, v -> responses = v);
                y = toggle(left, y, t("Sound Effects", "Звуки"), () -> sounds, v -> sounds = v);
                break;
            case 1:
                y = stepper(left, y, t("Command Cooldown (s)", "Задержка (сек)"), () -> cooldown, v -> cooldown = v, 0, 120, 1);
                y = stepper(left, y, t("Max Actions Per Request", "Макс. действий"), () -> maxActions, v -> maxActions = v, 1, 5000, 100);
                break;
            case 2:
                y = toggle(left, y, t("Creative Mode Only", "Только творческий"), () -> creativeOnly, v -> creativeOnly = v);
                y = toggle(left, y, t("Confirm Dangerous Changes", "Подтверждать крупное"), () -> confirm, v -> confirm = v);
                y = stepper(left, y, t("Permissions: own", "Права: свои"), () -> permSelf, v -> permSelf = v, 0, 4, 1);
                y = stepper(left, y, t("Permissions: others", "Права: на других"), () -> permOthers, v -> permOthers = v, 0, 4, 1);
                y = stepper(left, y, t("Permissions: weather/time", "Права: погода/время"), () -> permWorld, v -> permWorld = v, 0, 4, 1);
                y = stepper(left, y, t("Permissions: building", "Права: постройки"), () -> permBuild, v -> permBuild = v, 0, 4, 1);
                break;
            case 3:
                y = cycle(left, y, t("API Provider", "Провайдер"), () -> providerName(provider), () -> provider = nextProvider());
                keyBox = box(left, y, t("API Key", "Ключ API"), apiKey, 256, true); y += 26;
                modelBox = box(left, y, t("Model", "Модель"), model, 100, false); y += 26;
                if (provider.equals("custom")) { urlBox = box(left, y, t("URL", "Адрес"), baseUrl, 200, false); y += 26; }
                break;
            default:
                y = toggle(left, y, t("Agent: commands", "Агент: команды"), () -> agentCommands, v -> agentCommands = v);
                y = toggle(left, y, t("Confirm commands", "Подтверждать команды"), () -> agentConfirm, v -> agentConfirm = v);
                y = stepper(left, y, t("Max commands", "Макс. команд"), () -> maxCommands, v -> maxCommands = v, 1, 20, 1);
                y = toggle(left, y, t("Local parser first", "Сначала локальный"), () -> localFirst, v -> localFirst = v);
                break;
        }
        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal(t("Save", "Сохранить")), b -> { save(); onClose(); }).bounds(cx - 155, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(t("Reset", "Сбросить")), b -> { defaults(); rebuildWidgets(); }).bounds(cx - 50, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(t("Back", "Назад")), b -> onClose()).bounds(cx + 55, by, 100, 20).build());
    }

    private EditBox box(int left, int y, String label, String value, int maxLen, boolean secret) {
        EditBox e = new EditBox(font, left + 90, y, 210, 20, Component.literal(label));
        e.setMaxLength(maxLen);
        e.setValue(value);
        if (secret) e.setFormatter((s, i) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        addRenderableWidget(e);
        labels.add(new Object[]{label, left, y, 88});
        return e;
    }

    private int toggle(int left, int y, String label, Supplier<Boolean> get, Consumer<Boolean> put) {
        addRenderableWidget(Button.builder(Component.literal(onOff(get.get())), b -> { put.accept(!get.get()); b.setMessage(Component.literal(onOff(get.get()))); })
                .bounds(left + CTRL_X, y, CTRL_W, 20).build());
        labels.add(new Object[]{label, left, y, LABEL_W});
        return y + 26;
    }

    private String onOff(boolean v) { return v ? t("ON", "ВКЛ") : t("OFF", "ВЫКЛ"); }

    private int cycle(int left, int y, String label, Supplier<String> text, Runnable click) {
        addRenderableWidget(Button.builder(Component.literal(text.get()), b -> {
            click.run();
            b.setMessage(Component.literal(text.get()));
        }).bounds(left + CTRL_X, y, CTRL_W, 20).build());
        labels.add(new Object[]{label, left, y, LABEL_W});
        return y + 26;
    }

    private int stepper(int left, int y, String label, Supplier<Integer> get, Consumer<Integer> put, int min, int max, int step) {
        Button mid = Button.builder(Component.literal(String.valueOf(get.get())), b -> {}).bounds(left + CTRL_X + 24, y, CTRL_W - 48, 20).build();
        mid.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> { put.accept(Math.max(min, get.get() - step)); mid.setMessage(Component.literal(String.valueOf(get.get()))); })
                .bounds(left + CTRL_X, y, 22, 20).build());
        addRenderableWidget(mid);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> { put.accept(Math.min(max, get.get() + step)); mid.setMessage(Component.literal(String.valueOf(get.get()))); })
                .bounds(left + CTRL_X + CTRL_W - 22, y, 22, 20).build());
        labels.add(new Object[]{label, left, y, LABEL_W});
        return y + 26;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
        g.fill(width / 2 - 160, 28, width / 2 + 160, 30, ACCENT);
        g.drawCenteredString(font, "ChatAI", width / 2, 10, ACCENT);
        g.drawCenteredString(font, t("ChatAI Settings", "Настройки ChatAI"), width / 2, 20, DIM);
        for (Object[] l : labels) g.drawString(font, (String) l[0], (Integer) l[1] + 2, (Integer) l[2] + 6, TEXT, false);
        String hint = null;
        switch (tab) {
            case 0: hint = t("Built-in parser works offline. It is rule-based, not an AI model.", "Встроенный разбор работает без интернета. Это правила, а не нейросеть."); break;
            case 3: hint = t("With an AI provider your text and game info are sent to it. Key is stored in plain text.", "С ИИ твой текст и данные игры уходят провайдеру. Ключ хранится открытым текстом."); break;
            case 4: hint = t("AI commands run with YOUR permissions only. Blocked list is in the config file.", "Команды ИИ идут с ТВОИМИ правами. Чёрный список — в файле конфига."); break;
            default: break;
        }
        if (hint != null) g.drawCenteredString(font, hint, width / 2, height - 48, DIM);
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
