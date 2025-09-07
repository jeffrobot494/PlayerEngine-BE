package adris.altoclef.commandsystem;

import adris.altoclef.util.Dimension;
import java.util.ArrayList;
import java.util.List;

public class GotoTarget {
   private final int x;
   private final int y;
   private final int z;
   private final Dimension dimension;
   private final GotoTarget.GotoTargetCoordType type;

   public GotoTarget(int x, int y, int z, Dimension dimension, GotoTarget.GotoTargetCoordType type) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.dimension = dimension;
      this.type = type;
   }

   public static GotoTarget parseRemainder(String line) throws CommandException {
      line = line.trim();
      if (line.startsWith("(") && line.endsWith(")")) {
         line = line.substring(1, line.length() - 1);
      }

      String[] parts = line.split(" ");
      List<Integer> numbers = new ArrayList<>();
      Dimension dimension = null;

      for (String part : parts) {
         try {
            int num = Integer.parseInt(part);
            numbers.add(num);
         } catch (NumberFormatException var9) {
            dimension = (Dimension)Arg.parseEnum(part, Dimension.class);
            break;
         }
      }

      int x = 0;
      int y = 0;
      int z = 0;
      GotoTarget.GotoTargetCoordType type;

      switch (numbers.size()) {
         case 0:
            type = GotoTarget.GotoTargetCoordType.NONE;
            break;
         case 1:
            y = numbers.get(0);
            type = GotoTarget.GotoTargetCoordType.Y;
            break;
         case 2:
            x = numbers.get(0);
            z = numbers.get(1);
            type = GotoTarget.GotoTargetCoordType.XZ;
            break;
         case 3:
            x = numbers.get(0);
            y = numbers.get(1);
            z = numbers.get(2);
            type = GotoTarget.GotoTargetCoordType.XYZ;
            break;
         default:
            throw new CommandException("Unexpected number of integers passed to coordinate: " + numbers.size());
      }

      return new GotoTarget(x, y, z, dimension, type);
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public Dimension getDimension() {
      return this.dimension;
   }

   public boolean hasDimension() {
      return this.dimension != null;
   }

   public GotoTarget.GotoTargetCoordType getType() {
      return this.type;
   }

   public static enum GotoTargetCoordType {
      XYZ,
      XZ,
      Y,
      NONE;
   }
}
