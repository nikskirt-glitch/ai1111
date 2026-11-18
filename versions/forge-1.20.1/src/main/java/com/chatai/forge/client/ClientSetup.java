package com.chatai.forge.client;

import com.chatai.forge.ChatAIMod;
import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

/** Client only: Mods-screen "Config" button and the /chatai command that opens the settings screen. */
public final class ClientSetup {
    private ClientSetup() {}

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onClientCommands);
    }

    private static void onClientCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal(ChatAIMod.MODID).executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            // open on the next tick: the chat screen closes right after a command runs
            mc.tell(() -> mc.setScreen(new SettingsScreen(null)));
            return Command.SINGLE_SUCCESS;
        }));
    }
}
