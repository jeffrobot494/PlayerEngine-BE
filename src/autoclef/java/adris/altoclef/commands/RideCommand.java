package adris.altoclef.commands;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.EnterBoatWithPlayerTask;

public class RideCommand extends Command {
   public RideCommand() throws CommandException {
      super(
         "ride",
         "Enter and ride in the same boat as the specified player (or your owner if omitted).",
         new Arg<>(String.class, "username", null, 0)
      );
   }

   @Override
   protected void call(AltoClefController mod, ArgParser parser) throws CommandException {
      String username = parser.get(String.class);
      if (username == null) {
         if (mod.getOwner() == null) {
            mod.logWarning("No owner set; specify a username: @ride <username>");
            this.finish();
            return;
         }
         username = mod.getOwner().getName().getString();
      }

      EnterBoatWithPlayerTask task = new EnterBoatWithPlayerTask(username);
      mod.runUserTask(task, this::finish);
   }
}

