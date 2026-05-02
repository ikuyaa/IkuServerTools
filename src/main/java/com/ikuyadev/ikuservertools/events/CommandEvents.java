package com.ikuyadev.ikuservertools.events;

import com.ikuyadev.ikuservertools.commands.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.function.Consumer;

public class CommandEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        List<Consumer<CommandDispatcher<CommandSourceStack>>> registrations = List.of(
                // Home commands
                HomeCommand::register,
                SetHomeCommand::register,
                ListHomesCommand::register,
                DelHomeCommand::register,
                DelAllHomesCommand::register,

                // OP Commands
                GodCommand::register,
                HealCommand::register,
                FeedCommand::register,
                FlyCommand::register,
                GMCommand::register,

                // Spawn Commands
                SpawnCommand::register,
                SetSpawnCommand::register,

                // TP Commands
                BackCommand::register,
                TPACommand::register,
                TPAcceptCommand::register,
                TPDenyCommand::register,

                // Warp Commands
                WarpCommand::register,
                ListWarpsCommand::register,
                WarpCreateCommand::register,
                DelWarpCommand::register
        );

        registrations.forEach(r -> r.accept(event.getDispatcher()));
    }
}
