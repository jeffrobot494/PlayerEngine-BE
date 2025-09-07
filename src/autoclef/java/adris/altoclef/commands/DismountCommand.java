package adris.altoclef.commands;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.DismountBoatTask;

public class DismountCommand extends Command {
   public DismountCommand() {
      super("dismount", "Immediately dismount if currently riding a boat.");
   }

   @Override
   protected void call(AltoClefController mod, ArgParser parser) {
      mod.runUserTask(new DismountBoatTask(), this::finish);
   }
}

