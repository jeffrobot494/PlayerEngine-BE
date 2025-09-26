
package adris.altoclef.player2api;

import java.util.function.Consumer;

import adris.altoclef.player2api.manager.ConversationManager;
import adris.altoclef.player2api.manager.TTSManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.tasks.LookAtOwnerTask;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AgentSideEffects {
    private static final Logger LOGGER = LogManager.getLogger();

    public sealed interface CommandExecutionStopReason
            permits CommandExecutionStopReason.Cancelled,
            CommandExecutionStopReason.Finished,
            CommandExecutionStopReason.Error {
        String commandName();

        record Cancelled(String commandName) implements CommandExecutionStopReason {
        }

        record Finished(String commandName) implements CommandExecutionStopReason {
        }

        record Error(String commandName, String errMsg) implements CommandExecutionStopReason {
        }
    }

    public static void onEntityMessage(MinecraftServer server, Event.CharacterMessage characterMessage) {
        // message part:
        if (characterMessage.message() != null && !characterMessage.message().isBlank()) {
            AgentConversationData sendingCharacterData = characterMessage.sendingCharacterData();
            String message = String.format("<%s> %s", sendingCharacterData.getName(), characterMessage.message());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                // if you are an owner, or close, send to player.
                // if(sendingCharacterData.isOwner(player.getUUID()) ||
                // isClose(sendingCharacterData, player) ){
                broadcastChatToPlayer(server, message, player);
                // }
            }
            TTSManager.TTS(characterMessage.message(), sendingCharacterData.getCharacter(),
                    sendingCharacterData.getPlayer2apiService());
            ConversationManager.onAICharacterMessage(characterMessage,
                    characterMessage.sendingCharacterData().getUUID());
        }

        // command part:
        if (characterMessage.command() != null && !characterMessage.command().isBlank()) {
            onCommandListGenerated(characterMessage.sendingCharacterData().getMod(), characterMessage.command(),
                    characterMessage.sendingCharacterData()::onCommandFinish);
        }
    }

    public static void onError(MinecraftServer server, String errMsg) {
        LOGGER.error(errMsg);
    }

    public static void onCommandListGenerated(AltoClefController mod, String command,
            Consumer<CommandExecutionStopReason> onStop) {
        CommandExecutor cmdExecutor = mod.getCommandExecutor();
        String commandWithPrefix = cmdExecutor.isClientCommand(command) ? command
                : (cmdExecutor.getCommandPrefix() + command);
        if (commandWithPrefix.equals("@stop")) {
            mod.isStopping = true;
        } else {
            mod.isStopping = false;
        }
        if (commandWithPrefix.contains("idle")) {
            mod.runUserTask(new LookAtOwnerTask());
            return;
        }

        // add quotes to build_structure so it gets proccessed as one arg:
        String processedCommandWithPrefix = commandWithPrefix.replaceFirst(
                "^(@build_structure)\\s+(?![\"'])(.+)$",
                "$1 \"$2\"");

        cmdExecutor.execute(processedCommandWithPrefix, () -> {
            if (mod.isStopping) {
                LOGGER.info(
                        "[AgentSideEffects/AgentSideEffects]: (%s) was cancelled. Not adding finish event to queue.",
                        processedCommandWithPrefix);
                // Other canceled logic here
                onStop.accept(new CommandExecutionStopReason.Cancelled(commandWithPrefix));
                LOGGER.info("after cancel, not running look at owner");
            } else {
                if (!commandWithPrefix.equals("@bodylang greeting")) {
                    LOGGER.info("Running on stop after finish cmd={}", commandWithPrefix);
                    onStop.accept(new CommandExecutionStopReason.Finished(commandWithPrefix));
                } else {
                    LOGGER.info("Ignore onStop for bodylang greeting");
                }
                LOGGER.info("Running look at owner task after finish cmd={}", commandWithPrefix);
                mod.runUserTask(new LookAtOwnerTask());
            }
        }, (err) -> {
            onStop.accept(new CommandExecutionStopReason.Error(commandWithPrefix, err.getMessage()));
            LOGGER.info("Running look at owner aftr error in cmd={}", commandWithPrefix);
            mod.runUserTask(new LookAtOwnerTask());
        });
    }

    private static void broadcastChatToPlayer(MinecraftServer server, String message, ServerPlayer player) {
        player.displayClientMessage(Component.literal(message), false);
    }

}