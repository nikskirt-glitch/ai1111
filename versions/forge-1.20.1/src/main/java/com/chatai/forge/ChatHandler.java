package com.chatai.forge;

import com.chatai.core.Intent;
import com.chatai.core.IntentParser;
import com.chatai.core.Messages;
import com.chatai.core.RateLimiter;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side entry point: "AI <wish>" in chat. All checks and changes happen on the server. */
@Mod.EventBusSubscriber(modid = ChatAIMod.MODID)
public final class ChatHandler {
    private static final Logger LOG = LogUtils.getLogger();
    private static final RateLimiter LIMITER = new RateLimiter();
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static final long CONFIRM_MS = 30_000L;

    private record Pending(Intent intent, long expires) {}

    private ChatHandler() {}

    @SubscribeEvent
    public static void onChat(ServerChatEvent e) {
        String body = IntentParser.stripPrefix(e.getRawText());
        if (body == null) return;
        e.setCanceled(true); // the request is handled by ChatAI, not broadcast as a normal chat line
        ServerPlayer p = e.getPlayer();
        try {
            handle(p, body);
        } catch (Exception ex) {
            LOG.error("ChatAI failed handling a request", ex);
            reply(p, Messages.errorInternal(ChatAIConfig.LANGUAGE.get()));
        }
    }

    private static void handle(ServerPlayer p, String body) {
        String lang = ChatAIConfig.LANGUAGE.get();
        MinecraftServer server = p.server;
        if (!ChatAIConfig.ENABLED.get()) { reply(p, Messages.disabled(lang)); return; }
        boolean multi = server.isDedicatedServer() || server.getPlayerCount() > 1;
        if (multi && !ChatAIConfig.MULTIPLAYER.get()) { reply(p, Messages.multiplayerOff(lang)); return; }

        String norm = IntentParser.normalize(body);
        if (norm.equals("confirm") || norm.equals("подтверди") || norm.equals("подтвердить") || norm.equals("да")) { confirm(p, lang); return; }
        if (norm.equals("cancel") || norm.equals("отмена") || norm.equals("отмени")) { PENDING.remove(p.getUUID()); reply(p, Messages.cancelled(lang)); return; }

        long wait = LIMITER.tryAcquire(p.getUUID(), System.currentTimeMillis(), ChatAIConfig.COOLDOWN.get() * 1000L);
        if (wait > 0) { reply(p, Messages.cooldown(lang, wait)); return; }
        if (!permitted(p, 0, lang, null)) return;

        boolean useApi = ChatAIConfig.API_ENABLED.get();
        String key = ChatAIConfig.API_KEY.get();
        if (useApi && key.isBlank()) { reply(p, Messages.apiNotConfigured(lang)); useApi = false; }
        if (useApi) {
            final String model = ChatAIConfig.API_MODEL.get();
            Thread t = new Thread(() -> {
                Optional<Intent> r = ApiClient.translate(body, key, model);
                server.execute(() -> {
                    try {
                        if (r.isPresent()) dispatch(p, r.get());
                        else { reply(p, Messages.apiFailed(lang)); dispatch(p, IntentParser.parse(body, Messages.isRu(lang))); }
                    } catch (Exception ex) { LOG.error("ChatAI error", ex); reply(p, Messages.errorInternal(lang)); }
                });
            }, "ChatAI-API");
            t.setDaemon(true);
            t.start();
            return;
        }
        dispatch(p, IntentParser.parse(body, Messages.isRu(lang)));
    }

    /** Checks the basic gate (enabled/creative). Type-specific permission is checked in dispatch(). */
    private static boolean permitted(ServerPlayer p, int level, String lang, Intent i) {
        if (ChatAIConfig.CREATIVE_ONLY.get() && !p.isCreative()) { reply(p, Messages.creativeOnly(lang)); return false; }
        if (!p.hasPermissions(level)) { reply(p, Messages.noPermission(lang)); return false; }
        return true;
    }

    private static void dispatch(ServerPlayer p, Intent i) {
        String lang = ChatAIConfig.LANGUAGE.get();
        if (i.type == Intent.Type.UNKNOWN) { reply(p, Messages.notUnderstood(lang)); return; }
        int need;
        switch (i.type) {
            case TIME: case WEATHER: need = ChatAIConfig.PERM_WORLD.get(); break;
            case BUILD: need = ChatAIConfig.PERM_BUILD.get(); break;
            default: need = ChatAIConfig.PERM_SELF.get();
        }
        ServerPlayer subject = p;
        if (i.targetPlayer != null && i.type != Intent.Type.TIME && i.type != Intent.Type.WEATHER) {
            subject = p.server.getPlayerList().getPlayerByName(i.targetPlayer);
            if (subject == null) { reply(p, Messages.playerNotFound(lang, i.targetPlayer)); return; }
        }
        // Teleporting yourself TO someone does not touch them; everything else aimed at another player does.
        boolean touchesOther = subject != p && !(i.type == Intent.Type.TELEPORT && !"to_requester".equals(i.id));
        if (touchesOther) need = Math.max(need, ChatAIConfig.PERM_OTHERS.get());
        if (!permitted(p, need, lang, i)) return;

        int cost = i.cost();
        int max = ChatAIConfig.MAX_ACTIONS.get();
        if (cost > max) { reply(p, Messages.tooBig(lang, cost, max)); return; }

        if (ChatAIConfig.CONFIRM_DANGEROUS.get() && i.isLargeWorldChange()) {
            PENDING.put(p.getUUID(), new Pending(i, System.currentTimeMillis() + CONFIRM_MS));
            reply(p, Messages.confirmNeeded(lang, i.label));
            return;
        }
        execute(p, subject, i, lang);
    }

    private static void confirm(ServerPlayer p, String lang) {
        Pending pe = PENDING.remove(p.getUUID());
        if (pe == null || pe.expires() < System.currentTimeMillis()) { reply(p, Messages.nothingToConfirm(lang)); return; }
        Intent i = pe.intent();
        ServerPlayer subject = p;
        if (i.targetPlayer != null) {
            subject = p.server.getPlayerList().getPlayerByName(i.targetPlayer);
            if (subject == null) { reply(p, Messages.playerNotFound(lang, i.targetPlayer)); return; }
        }
        // permissions are re-checked at confirm time (they may have changed)
        int need = i.type == Intent.Type.BUILD ? ChatAIConfig.PERM_BUILD.get() : ChatAIConfig.PERM_SELF.get();
        if (subject != p) need = Math.max(need, ChatAIConfig.PERM_OTHERS.get());
        if (!permitted(p, need, lang, i)) return;
        execute(p, subject, i, lang);
    }

    private static void execute(ServerPlayer p, ServerPlayer subject, Intent i, String lang) {
        reply(p, Messages.accepted(lang, i));
        try {
            String detail = Executor.run(p, subject, i, lang);
            String done = Messages.done(lang, i, detail);
            boolean worldWide = i.type == Intent.Type.TIME || i.type == Intent.Type.WEATHER || i.type == Intent.Type.BUILD;
            if (worldWide || subject != p) {
                broadcast(p.server, p.getGameProfile().getName() + ": " + done);
            } else {
                reply(p, done);
            }
        } catch (ActionException ex) {
            reply(p, ex.getMessage());
        } catch (Exception ex) {
            LOG.error("ChatAI execution failed", ex);
            reply(p, Messages.errorInternal(lang));
        }
    }

    static void reply(ServerPlayer p, String msg) {
        if (!ChatAIConfig.RESPONSES.get()) return;
        p.sendSystemMessage(Component.literal(Messages.prefix() + msg));
    }

    private static void broadcast(MinecraftServer s, String msg) {
        if (!ChatAIConfig.RESPONSES.get()) return;
        s.getPlayerList().broadcastSystemMessage(Component.literal(Messages.prefix() + msg), false);
    }
}
