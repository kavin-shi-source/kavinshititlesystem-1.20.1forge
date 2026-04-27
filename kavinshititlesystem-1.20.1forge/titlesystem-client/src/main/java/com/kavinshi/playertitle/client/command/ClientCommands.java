package com.kavinshi.playertitle.client.command;

import com.kavinshi.playertitle.client.config.ClientTabConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = "playertitleclient", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientCommands {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("titleclient")
            .then(Commands.literal("setTabRenderer")
                .then(Commands.argument("renderer", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("velocityTab");
                        builder.suggest("default");
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        String renderer = StringArgumentType.getString(context, "renderer");
                        if ("velocityTab".equalsIgnoreCase(renderer)) {
                            ClientTabConfig.get().setUseCustomTab(false);
                            ClientTabConfig.save();
                            context.getSource().sendSuccess(() -> Component.literal("§a[PlayerTitle] Client Tab renderer set to: velocityTab. Custom tab overlay disabled."), false);
                        } else if ("default".equalsIgnoreCase(renderer)) {
                            ClientTabConfig.get().setUseCustomTab(true);
                            ClientTabConfig.save();
                            context.getSource().sendSuccess(() -> Component.literal("§a[PlayerTitle] Client Tab renderer set to: default. Custom tab overlay enabled."), false);
                        } else {
                            context.getSource().sendFailure(Component.literal("§c[PlayerTitle] Unknown renderer: " + renderer + ". Use 'velocityTab' or 'default'."));
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
