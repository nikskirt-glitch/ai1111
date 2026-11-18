package com.chatai.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ChatAIMod.MODID)
public final class ChatAIMod {
    public static final String MODID = "chatai";

    public ChatAIMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ChatAIConfig.SPEC, "chatai-common.toml");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.chatai.forge.client.ClientSetup.init();
        }
    }
}
