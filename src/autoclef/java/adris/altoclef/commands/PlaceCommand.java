package adris.altoclef.commands;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class PlaceCommand extends Command {
    public PlaceCommand() throws CommandException {
        super(
            "place",
            "Place a block at a specific position or nearby. Usage: `place stone 10 64 20` to place stone at coordinates, or `place stone` to place nearby.",
            new Arg<>(String.class, "block_name"),
            new Arg<>(Integer.class, "x", null, 1),
            new Arg<>(Integer.class, "y", null, 2), 
            new Arg<>(Integer.class, "z", null, 3)
        );
    }

    @Override
    protected void call(AltoClefController mod, ArgParser parser) throws CommandException {
        String blockName = parser.get(String.class);
        
        // Parse block name to Block object
        Block blockToPlace = parseBlockName(blockName);
        if (blockToPlace == null) {
            mod.log("Unknown block: " + blockName + ". Make sure to use the correct block name (e.g., 'stone', 'cobblestone', 'oak_planks')");
            finish();
            return;
        }
        
        Task placeTask;
        
        // Check if coordinates were provided (try to get them, use default if not)
        try {
            Integer x = parser.get(Integer.class);  // Will use default (null) if not provided
            Integer y = parser.get(Integer.class);
            Integer z = parser.get(Integer.class);
            
            if (x != null && y != null && z != null) {
                // Place at specific coordinates
                BlockPos targetPos = new BlockPos(x, y, z);
                placeTask = new PlaceBlockTask(targetPos, blockToPlace);
                mod.log("Placing " + blockName + " at " + x + ", " + y + ", " + z);
            } else {
                // Place nearby if any coordinate is null
                placeTask = new PlaceBlockNearbyTask(blockToPlace);
                mod.log("Placing " + blockName + " nearby");
            }
        } catch (CommandException e) {
            // If we can't get coordinates, place nearby
            placeTask = new PlaceBlockNearbyTask(blockToPlace);
            mod.log("Placing " + blockName + " nearby");
        }
        
        mod.runUserTask(placeTask, this::finish);
    }
    
    private Block parseBlockName(String blockName) {
        // Clean up the block name
        blockName = blockName.toLowerCase().trim();
        
        // Add minecraft namespace if not present
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }
        
        try {
            ResourceLocation blockId = new ResourceLocation(blockName);
            return BuiltInRegistries.BLOCK.get(blockId);
        } catch (Exception e) {
            // If parsing fails, try some common alternatives
            return tryAlternativeBlockNames(blockName.replace("minecraft:", ""));
        }
    }
    
    private Block tryAlternativeBlockNames(String originalName) {
        // Common block name aliases
        String[][] aliases = {
            {"wood", "oak_planks"},
            {"plank", "oak_planks"},
            {"planks", "oak_planks"},
            {"log", "oak_log"},
            {"dirt", "dirt"},
            {"grass", "grass_block"},
            {"cobble", "cobblestone"},
            {"smoothstone", "smooth_stone"},
            {"water", "water"},
            {"lava", "lava"}
        };
        
        for (String[] alias : aliases) {
            if (originalName.equals(alias[0])) {
                try {
                    ResourceLocation blockId = new ResourceLocation("minecraft:" + alias[1]);
                    Block block = BuiltInRegistries.BLOCK.get(blockId);
                    if (block != Blocks.AIR) {
                        return block;
                    }
                } catch (Exception ignored) {}
            }
        }
        
        return null;
    }
}