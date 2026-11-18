package com.chatai.forge.client;

import com.chatai.forge.ChatAIConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** "ChatAI Settings": dark/purple, category tabs, Save / Reset / Back. Edits the local COMMON config. */
public final class SettingsScreen extends Screen {
    private static final int BG_TOP = 0xFF120A1F, BG_BOTTOM = 0xFF241047, ACCENT = 0xFFA855F7, DIM = 0xFFB9A7D6;
    private static final String[] TABS = {"General", "Limits", "Safety", "AI API"};

    private final Screen parent;
    private int tab = 0;
    // working copy (applied only on Save)
    private boolean enabled, multiplayer, responses, sounds, apiEnabled, creativeOnly, confirm;
    private String language, provider, apiKey;
    private int cooldown, maxActions, permSelf, permOthers, permWorld, permBuild;
    private EditBox keyBox;
    private final List<Runnable> refreshers = new ArrayList<>();

    public SettingsScreen(Screen parent) {
        super(Component.literal("ChatAI Settings"));
        this.parent = parent;
        load();
    }

    private void load() {
        enabled = ChatAIConfig.ENABLED.get(); multiplayer = ChatAIConfig.MULTIPLAYER.get();
        responses = ChatAIConfig.RESPONSES.get(); sounds = ChatAIConfig.SOUNDS.get();
        apiEnabled = ChatAIConfig.API_ENABLED.get(); creativeOnly = ChatAIConfig.CREATIVE_ONLY.get();
        confirm = ChatAIConfig.CONFIRM_DANGEROUS.get(); language = ChatAIConfig.LANGUAGE.get();
        provider = ChatAIConfig.API_PROVIDER.get(); apiKey = ChatAIConfig.API_KEY.get();
        cooldown = ChatAIConfig.COOLDOWN.get(); maxActions = ChatAIConfig.MAX_ACTIONS.get();
        permSelf = ChatAIConfig.PERM_SELF.get(); permOthers = ChatAIConfig.PERM_OTHERS.get();
        permWorld = ChatAIConfig.PERM_WORLD.get(); permBuild = ChatAIConfig.PERM_BUILD.get();
    }

    private void defaults() {
        enabled = true; multiplayer = true; responses = true; sounds = true; apiEnabled = false;
        creativeOnly = false; confirm = true; language = "ru"; provider = "claude"; apiKey = "";
        cooldown = 3; maxActions = 2000; permSelf = 0; permOthers = 2; permWorld = 2; permBuild = 0;
    }

    private void save() {
        set(ChatAIConfig.ENABLED, enabled); set(ChatAIConfig.MULTIPLAYER, multiplayer);
        set(ChatAIConfig.RESPONSES, responses); set(ChatAIConfig.SOUNDS, sounds);
        set(ChatAIConfig.API_ENABLED, apiEnabled); set(ChatAIConfig.CREATIVE_ONLY, creativeOnly);
        set(ChatAIConfig.CONFIRM_DANGEROUS, confirm); set(ChatAIConfig.LANGUAGE, language);
        set(ChatAIConfig.API_PROVIDER, provider);
        if (keyBox != null) apiKey = keyBox.getValue().trim();
        set(ChatAIConfig.API_KEY, apiKey);
        set(ChatAIConfig.COOLDOWN, cooldown); set(ChatAIConfig.MAX_ACTIONS, maxActions);
        set(ChatAIConfig.PERM_SELF, permSelf); set(ChatAIConfig.PERM_OTHERS, permOthers);
        set(ChatAIConfig.PERM_WORLD, permWorld); set(ChatAIConfig.PERM_BUILD, permBuild);
        ChatAIConfig.SPEC.save();
    }

    private static <T> void set(ForgeConfigSpec.ConfigValue<T> v, T x) { v.set(x); }

    @Override
    protected void init() {
        refreshers.clear();
        keyBox = null;
        int cx = width / 2, left = cx - 150, y = 62;
        int tw = 74;
        for (int t = 0; t < TABS.length; t++) {
            final int ti = t;
            Button b = Button.builder(Component.literal((t == tab ? "> " : "") + TABS[t]), x -> { if (tab != ti) { remember(); tab = ti; rebuildWidgets(); } })
                    .bounds(left + t * (tw + 1) + 2, 34, tw, 20).build();
            addRenderableWidget(b);
        }
        switch (tab) {
            case 0:
                y = toggle(left, y, "Enable ChatAI", () -> enabled, v -> enabled = v);
                y = toggle(left, y, "Enable Multiplayer", () -> multiplayer, v -> multiplayer = v);
                y = cycle(left, y, "AI Language", () -> language.equals("ru") ? "Русский" : "English", () -> language = language.equals("ru") ? "en" : "ru");
                y = toggle(left, y, "Response Messages", () -> responses, v -> responses = v);
                y = toggle(left, y, "Sound Effects", () -> sounds, v -> sounds = v);
                break;
            case 1:
                y = stepper(left, y, "Command Cooldown (s)", () -> cooldown, v -> cooldown = v, 0, 120, 1);
                y = stepper(left, y, "Max Actions Per Request", () -> maxActions, v -> maxActions = v, 1, 5000, 100);
                break;
            case 2:
                y = toggle(left, y, "Creative Mode Only", () -> creativeOnly, v -> creativeOnly = v);
                y = toggle(left, y, "Confirm Dangerous Changes", () -> confirm, v -> confirm = v);
                y = stepper(left, y, "Permission: own actions", () -> permSelf, v -> permSelf = v, 0, 4, 1);
                y = stepper(left, y, "Permission: other players", () -> permOthers, v -> permOthers = v, 0, 4, 1);
                y = stepper(left, y, "Permission: weather/time", () -> permWorld, v -> permWorld = v, 0, 4, 1);
                y = stepper(left, y, "Permission: building", () -> permBuild, v -> permBuild = v, 0, 4, 1);
                break;
            default:
                y = toggle(left, y, "Enable AI API", () -> apiEnabled, v -> apiEnabled = v);
                y = cycle(left, y, "API Provider", () -> "Claude", () -> provider = "claude");
                keyBox = new EditBox(font, left + 150, y, 150, 20, Component.literal("API Key"));
                keyBox.setMaxLength(256);
                keyBox.setValue(apiKey);
                keyBox.setFormatter((s, i) -> net.minecraft.util.FormattedCharSequence.forward("*".repeat(s.length()), net.minecraft.network.chat.Style.EMPTY));
                addRenderableWidget(keyBox);
                break;
        }
        int by = height - 28;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> { save(); onClose(); }).bounds(cx - 155, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> { defaults(); rebuildWidgets(); }).bounds(cx - 50, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(cx + 55, by, 100, 20).build());
    }

    private void remember() { if (keyBox != null) apiKey = keyBox.getValue().trim(); }

    private int toggle(int left, int y, String label, Supplier<Boolean> get, java.util.function.Consumer<Boolean> put) {
        Button[] self = new Button[1];
        self[0] = Button.builder(Component.literal(get.get() ? "ON" : "OFF"), b -> { put.accept(!get.get()); b.setMessage(Component.literal(get.get() ? "ON" : "OFF")); })
                .bounds(left + 200, y, 100, 20).build();
        addRenderableWidget(self[0]);
        labels.add(new Object[]{label, left, y});
        return y + 26;
    }

    private int cycle(int left, int y, String label, Supplier<String> text, Runnable click) {
        addRenderableWidget(Button.builder(Component.literal(text.get()), b -> { click.run(); b.setMessage(Component.literal(text.get())); })
                .bounds(left + 200, y, 100, 20).build());
        labels.add(new Object[]{label, left, y});
        return y + 26;
    }

    private int stepper(int left, int y, String label, Supplier<Integer> get, java.util.function.Consumer<Integer> put, int min, int max, int step) {
        Button mid = Button.builder(Component.literal(String.valueOf(get.get())), b -> {}).bounds(left + 224, y, 52, 20).build();
        mid.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> { put.accept(Math.max(min, get.get() - step)); mid.setMessage(Component.literal(String.valueOf(get.get()))); }).bounds(left + 200, y, 22, 20).build());
        addRenderableWidget(mid);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> { put.accept(Math.min(max, get.get() + step)); mid.setMessage(Component.literal(String.valueOf(get.get()))); }).bounds(left + 278, y, 22, 20).build());
        labels.add(new Object[]{label, left, y});
        return y + 26;
    }

    private final List<Object[]> labels = new ArrayList<>();

    @Override
    protected void rebuildWidgets() { labels.clear(); super.rebuildWidgets(); }

    @Override
    protected void clearWidgets() { labels.clear(); super.clearWidgets(); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);
        g.fill(width / 2 - 160, 28, width / 2 + 160, 30, ACCENT);
        g.drawCenteredString(font, "ChatAI", width / 2, 10, ACCENT);
        g.drawCenteredString(font, "ChatAI Settings", width / 2, 20, DIM);
        for (Object[] l : labels) g.drawString(font, (String) l[0], (Integer) l[1] + 2, (Integer) l[2] + 6, 0xFFEADCFF, false);
        if (tab == 3) {
            g.drawString(font, "API Key", width / 2 - 148, 62 + 26 * 2 + 6, 0xFFEADCFF, false);
            g.drawCenteredString(font, "Your text is sent to the provider when enabled. Falls back to the local parser.", width / 2, height - 48, DIM);
        } else if (tab == 0) {
            g.drawCenteredString(font, "Built-in parser works offline. It is rule-based, not an AI model.", width / 2, height - 48, DIM);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
