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

