# Session Changelog: Boat Riding + Dismount + Ghost AI Diagnosis

This document records all changes made in this session, including full code listings of new/modified files, and a diagnosis of a possible "ghost AI" issue (duplicate responders after despawn).

## Summary of Changes

- Added a task to enter and ride in the same boat as a specified player and finish once mounted.
- Added a command `@ride [username]` to use the task.
- Added a minimal task to immediately dismount if riding a boat.
- Added a command `@dismount` to run the immediate dismount task.
- Registered the new commands in the command registry.

## Files Added

1) `src/autoclef/java/adris/altoclef/tasks/movement/EnterBoatWithPlayerTask.java`

```java
package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

public class EnterBoatWithPlayerTask extends Task {
   private final String playerName;
   private final double approachDistance;
   private final MovementProgressChecker progress = new MovementProgressChecker();
   private boolean finished = false;

   public EnterBoatWithPlayerTask(String playerName, double approachDistance) {
      this.playerName = playerName;
      this.approachDistance = approachDistance;
   }

   public EnterBoatWithPlayerTask(String playerName) {
      this(playerName, 1.7);
   }

   @Override
   protected void onStart() {
      this.progress.reset();
      this.finished = false;
   }

   private boolean inSameBoatAsPlayer(AltoClefController mod, Player player) {
      Entity myVehicle = mod.getEntity().getVehicle();
      Entity theirVehicle = player.getVehicle();
      if (myVehicle instanceof Boat && theirVehicle != null) {
         return myVehicle == theirVehicle;
      }
      return false;
   }

   @Override
   protected Task onTick() {
      AltoClefController mod = this.controller;

      // Resolve player or move to last known position
      Optional<Player> targetPlayerOpt = mod.getEntityTracker().getPlayerEntity(this.playerName);
      if (targetPlayerOpt.isEmpty()) {
         Optional<Vec3> last = mod.getEntityTracker().getPlayerMostRecentPosition(this.playerName);
         if (last.isEmpty()) {
            this.setDebugState("Waiting for player to load: " + this.playerName);
            return null;
         }
         Vec3 pos = last.get();
         this.setDebugState("Going to last seen position for " + this.playerName);
         return new GetToBlockTask(new BlockPos((int)pos.x, (int)pos.y, (int)pos.z), false);
      }

      Player player = targetPlayerOpt.get();

      // Already riding same boat
      if (inSameBoatAsPlayer(mod, player)) {
         this.setDebugState("Riding with player " + this.playerName);
         this.finished = true;
         return null;
      }

      // If player is not yet in a boat, stay very close and wait
      if (!(player.getVehicle() instanceof Boat)) {
         this.setDebugState("Player not in boat; following until they embark");
         return new GetToEntityTask(player, 2.0);
      }

      Boat boat = (Boat) player.getVehicle();

      // If we're riding a different vehicle, dismount first
      if (mod.getEntity().getVehicle() != null && mod.getEntity().getVehicle() != boat) {
         mod.getEntity().stopRiding();
      }

      // If close enough and seat available, mount directly
      double dist = mod.getEntity().distanceTo(boat);
      boolean canMount = boat.getPassengers().size() < 2;
      if (dist <= this.approachDistance && canMount) {
         boolean mounted = mod.getEntity().startRiding(boat, true);
         if (!mounted) {
            // Retry by getting even closer
            this.setDebugState("Mount failed, getting closer");
            return new GetToEntityTask(boat, 1.0);
         }
         this.setDebugState("Mounted boat with player " + this.playerName);
         this.finished = true;
         return null;
      }

      if (!canMount) {
         this.setDebugState("Boat full; waiting for free seat");
         return new GetToEntityTask(boat, 2.0);
      }

      // Approach the boat
      this.setDebugState("Approaching player's boat");
      return new GetToEntityTask(boat, this.approachDistance);
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task other) {
      if (!(other instanceof EnterBoatWithPlayerTask t)) return false;
      return this.playerName.equals(t.playerName) && Math.abs(this.approachDistance - t.approachDistance) < 0.1;
   }

   @Override
   protected String toDebugString() {
      return "Enter boat with player " + this.playerName;
   }

   @Override
   public boolean isFinished() {
      return this.finished;
   }
}
```

2) `src/autoclef/java/adris/altoclef/commands/RideCommand.java`

```java
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
```

3) `src/autoclef/java/adris/altoclef/tasks/movement/DismountBoatTask.java`

```java
package adris.altoclef.tasks.movement;

import adris.altoclef.tasksystem.Task;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;

public class DismountBoatTask extends Task {
   private boolean finished = false;

   @Override
   protected void onStart() {
      Entity vehicle = this.controller.getEntity().getVehicle();
      if (vehicle instanceof Boat) {
         this.controller.getEntity().stopRiding();
      }
      finished = true;
      this.setDebugState("Dismounted (if in boat)");
   }

   @Override
   protected Task onTick() {
      return null;
   }

   @Override
   protected void onStop(Task interruptTask) {
   }

   @Override
   protected boolean isEqual(Task other) {
      return other instanceof DismountBoatTask;
   }

   @Override
   protected String toDebugString() {
      return "Dismount boat";
   }

   @Override
   public boolean isFinished() {
      return finished;
   }
}
```

4) `src/autoclef/java/adris/altoclef/commands/DismountCommand.java`

```java
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
```

## Files Modified

1) `src/autoclef/java/adris/altoclef/AltoClefCommands.java`

```java
package adris.altoclef;

import adris.altoclef.commands.AttackPlayerOrMobCommand;
import adris.altoclef.commands.BodyLanguageCommand;
import adris.altoclef.commands.DepositCommand;
import adris.altoclef.commands.EquipCommand;
import adris.altoclef.commands.FarmCommand;
import adris.altoclef.commands.FishCommand;
import adris.altoclef.commands.FollowCommand;
import adris.altoclef.commands.FoodCommand;
import adris.altoclef.commands.GamerCommand;
import adris.altoclef.commands.GetCommand;
import adris.altoclef.commands.GiveCommand;
import adris.altoclef.commands.GotoCommand;
import adris.altoclef.commands.HeroCommand;
import adris.altoclef.commands.IdleCommand;
import adris.altoclef.commands.LocateStructureCommand;
import adris.altoclef.commands.MeatCommand;
import adris.altoclef.commands.PlaceCommand;
import adris.altoclef.commands.ReloadSettingsCommand;
import adris.altoclef.commands.ResetMemoryCommand;
import adris.altoclef.commands.RideCommand;
import adris.altoclef.commands.DismountCommand;
import adris.altoclef.commands.SetAIBridgeEnabledCommand;
import adris.altoclef.commands.StopCommand;
import adris.altoclef.commands.random.ScanCommand;
import adris.altoclef.commandsystem.CommandException;

public class AltoClefCommands {
   public static void init(AltoClefController controller) throws CommandException {
      controller.getCommandExecutor()
         .registerNewCommand(
            new GetCommand(),
            new EquipCommand(),
            new BodyLanguageCommand(),
            new DepositCommand(),
            new GotoCommand(),
            new IdleCommand(),
            new HeroCommand(),
            new LocateStructureCommand(),
            new StopCommand(),
            new FoodCommand(),
            new MeatCommand(),
            new PlaceCommand(),
            new ReloadSettingsCommand(),
            new ResetMemoryCommand(),
            new GamerCommand(),
            new FollowCommand(),
            new RideCommand(),
            new DismountCommand(),
            new GiveCommand(),
            new ScanCommand(),
            new AttackPlayerOrMobCommand(),
            new SetAIBridgeEnabledCommand(),
            new FarmCommand(),
            new FishCommand()
         );
   }
}
```

## Usage

- Enter boat with a player (owner by default):
  - `@ride` (uses owner if set)
  - `@ride <username>`
- Dismount immediately if riding a boat:
  - `@dismount`

## Notes and Limitations

- EnterBoatWithPlayerTask completes once the entity is riding in the same boat as the player. It does not continuously maintain the state; follow-up tasks should be issued as needed.
- DismountBoatTask performs an immediate dismount if currently in a boat and finishes in one tick; it does nothing if not mounted.

## Ghost AI Diagnosis (Duplicate Responders)

### Symptom
After despawning the AI entity and spawning a new one, you may observe two AIs responding to chat: the new one and a "ghost" of the old one.

### Likely Root Cause
- The Player2 chat/LLM bridge keeps per-entity state in a static map: `EventQueueManager.queueData` (UUID → EventQueueData).
- When an AI entity is removed/despawned, the corresponding entry is not removed from `queueData`.
- `EventQueueManager.injectOnTick(server)` continues to process all entries, so the stale queue still reacts to chat.

### How to Confirm
- Look for logs indicating multiple processing for a single chat:
  - "LLMCompleter returned json=...", "onCharMsg ... running onCharMsg for = ..." twice.
- Check for creations without removals:
  - "EventQueueManager/getOrCreateEventQueueData: creating new queue data for entId=..." lines after each spawn, with no corresponding cleanup.

### Immediate Workarounds
- Before despawning an AI, disable its chat bridge: `@chatclef off` (SetAIBridgeEnabledCommand), which sets `EventQueueData.setEnabled(false)`.
- Stop the controller: `@stop` before despawn to cancel behaviors (does not clear the queue, but reduces activity).

### Recommended Fix (to implement next)
1) Provide a removal path in `EventQueueManager`:
   - Add a `remove(UUID)` method to delete the entry from `queueData`.
   - Optionally add a periodic sweep in `injectOnTick` to drop entries whose `data.getEntity()` is null, not alive, or otherwise invalid.

2) Call cleanup on lifecycle events:
   - In `AltoClefController.stop()` (when tearing down for good), call `EventQueueManager.remove(controller.getPlayer().getUUID())` and/or set `setEnabled(false)`.
   - If you have a custom AI entity class, call the same in its `discard()` / removal callback.

3) Optional safety net:
   - Use weak references in the `queueData` values and skip/clean entries when the entity/controller is gone.

### Verification Checklist
- After despawn, verify a log entry for removal and that only one queue exists.
- After respawn, only a single "creating new queue data" appears; only one AI responds to chat.

---

This changelog should help you track the exact behavior added for boat riding/dismounting and guide you through troubleshooting and fixing duplicate AI responders in your world.

