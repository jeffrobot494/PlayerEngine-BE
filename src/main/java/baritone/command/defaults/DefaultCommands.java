package baritone.command.defaults;

import baritone.PlayerEngine;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.command.ICommand;
import baritone.api.command.argument.ICommandArgument;
import baritone.api.command.exception.CommandException;
import baritone.api.command.exception.CommandNotEnoughArgumentsException;
import baritone.api.command.manager.ICommandManager;
import baritone.api.entity.IAutomatone;
import baritone.command.argument.ArgConsumer;
import baritone.command.manager.BaritoneArgumentType;
import baritone.command.manager.BaritoneCommandManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class DefaultCommands {
   public static final ExecutionControlCommands controlCommands = new ExecutionControlCommands();
   public static final SelCommand selCommand = new SelCommand();
   public static final DynamicCommandExceptionType BARITONE_COMMAND_FAILED_EXCEPTION = new DynamicCommandExceptionType(Message.class::cast);

   public static void registerAll() {
      for (ICommand command : new ArrayList<>(
         Arrays.asList(
            new HelpCommand(),
            new SetCommand(),
            new CommandAlias(Arrays.asList("modified", "mod", "baritone", "modifiedsettings"), "List modified settings", "set modified"),
            new CommandAlias("reset", "Reset all settings or just one", "set reset"),
            new GoalCommand(),
            new GotoCommand(),
            new PathCommand(),
            new ProcCommand(),
            new ETACommand(),
            new VersionCommand(),
            new RepackCommand(),
            new BuildCommand(),
            new SchematicaCommand(),
            new ComeCommand(),
            new AxisCommand(),
            new ForceCancelCommand(),
            new GcCommand(),
            new InvertCommand(),
            new TunnelCommand(),
            new RenderCommand(),
            new FarmCommand(),
            new ChestsCommand(),
            new FollowCommand(),
            new ExploreFilterCommand(),
            new ReloadAllCommand(),
            new SaveAllCommand(),
            new ExploreCommand(),
            new BlacklistCommand(),
            new FindCommand(),
            new MineCommand(),
            new ClickCommand(),
            new SurfaceCommand(),
            new ThisWayCommand(),
            new WaypointsCommand(),
            new FishCommand(),
            new CommandAlias("sethome", "Sets your home waypoint", "waypoints save home"),
            new CommandAlias("home", "Path to your home waypoint", "waypoints goto home"),
            selCommand
         )
      )) {
         ICommandManager.registry.register(command);
      }

      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, ctx, dedicated) -> register(dispatcher));
   }

   private static void logRanCommand(CommandSourceStack source, String command, String rest) {
      if (BaritoneAPI.getGlobalSettings().echoCommands.get()) {
         String msg = command + rest;
         String toDisplay = BaritoneAPI.getGlobalSettings().censorRanCommands.get() ? command + " ..." : msg;
         source.sendSuccess(
            () -> {
               MutableComponent component = Component.literal(String.format("> %s", toDisplay));
               component.setStyle(
                  component.getStyle()
                     .applyFormat(ChatFormatting.WHITE)
                     .withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Component.literal("Click to rerun command")))
                     .withClickEvent(new ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/automatone " + msg))
               );
               return component;
            },
            false
         );
      }
   }

   public static boolean runCommand(CommandSourceStack source, String msg, IBaritone baritone) throws CommandException {
      if (msg.trim().equalsIgnoreCase("damn")) {
         source.sendSuccess(() -> Component.literal("daniel"), false);
         return false;
      } else if (msg.trim().equalsIgnoreCase("orderpizza")) {
         PlayerEngine.LOGGER.fatal("No pizza :(");
         return false;
      } else if (msg.isEmpty()) {
         return runCommand(source, "help", baritone);
      } else {
         Tuple<String, List<ICommandArgument>> pair = BaritoneCommandManager.expand(msg);
         String command = (String)pair.getA();
         String rest = msg.substring(((String)pair.getA()).length());
         ArgConsumer argc = new ArgConsumer(baritone.getCommandManager(), (List<ICommandArgument>)pair.getB(), baritone);
         if (!argc.hasAny()) {
            Settings.Setting<?> setting = BaritoneAPI.getGlobalSettings().byLowerName.get(command.toLowerCase(Locale.ROOT));
            if (setting != null) {
               logRanCommand(source, command, rest);
               if (setting.getValueClass() == Boolean.class) {
                  baritone.getCommandManager().execute(source, String.format("set toggle %s", setting.getName()));
               } else {
                  baritone.getCommandManager().execute(source, String.format("set %s", setting.getName()));
               }

               return true;
            }
         } else if (argc.hasExactlyOne()) {
            for (Settings.Setting<?> setting : BaritoneAPI.getGlobalSettings().allSettings) {
               if (!setting.getName().equals("logger") && setting.getName().equalsIgnoreCase((String)pair.getA())) {
                  logRanCommand(source, command, rest);

                  try {
                     baritone.getCommandManager().execute(source, String.format("set %s %s", setting.getName(), argc.getString()));
                  } catch (CommandNotEnoughArgumentsException var10) {
                  }

                  return true;
               }
            }
         }

         if (ICommandManager.getCommand((String)pair.getA()) != null) {
            logRanCommand(source, command, rest);
         }

         return baritone.getCommandManager().execute(source, pair);
      }
   }

   private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("automatone").requires(s -> s.hasPermission(2)))
            .then(
               Commands.argument("command", StringArgumentType.greedyString())
                  .executes(
                     command -> runCommand(
                        (CommandSourceStack)command.getSource(),
                        ((CommandSourceStack)command.getSource()).getEntityOrException(),
                        BaritoneArgumentType.getCommand(command, "command")
                     )
                  )
            )
      );
   }

   private static int runCommand(CommandSourceStack source, Entity target, String command) throws CommandSyntaxException {
      if (!(target instanceof LivingEntity)) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         try {
            for (LivingEntity entity : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(100.0, 100.0, 100.0), e -> true)) {
               if (entity instanceof IAutomatone) {
                  runCommand(source, command, BaritoneAPI.getProvider().getBaritone(entity));
               }
            }

            return 1;
         } catch (CommandException var6) {
            throw BARITONE_COMMAND_FAILED_EXCEPTION.create(var6.handle());
         }
      }
   }
}
