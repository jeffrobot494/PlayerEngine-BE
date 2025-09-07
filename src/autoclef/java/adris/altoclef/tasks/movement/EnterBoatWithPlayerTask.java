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
