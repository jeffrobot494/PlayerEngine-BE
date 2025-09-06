# Creating Player-Like AI Entities with PlayerEngine

## Overview

This comprehensive guide demonstrates how to create fully integrated, intelligent player-like entities using the PlayerEngine framework. Based on the implementation patterns from the Player2NPC mod, this guide will walk you through creating AI companions that can understand natural language, perform complex tasks, and interact with the Minecraft world just like players.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Core Architecture](#core-architecture)
3. [Step-by-Step Implementation](#step-by-step-implementation)
4. [Advanced Features](#advanced-features)
5. [Integration Patterns](#integration-patterns)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

## Prerequisites

### Dependencies

Add these dependencies to your `build.gradle`:

```gradle
dependencies {
    // PlayerEngine framework (replace with actual coordinates)
    modImplementation "your.group:playerengine:1.0.0"
    
    // Cardinal Components API for modular capabilities
    modImplementation "dev.onyxstudios.cardinal-components-api:cardinal-components-base:5.2.2"
    modImplementation "dev.onyxstudios.cardinal-components-api:cardinal-components-entity:5.2.2"
    modImplementation "dev.onyxstudios.cardinal-components-api:cardinal-components-world:5.2.2"
    
    // Fabric API
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.92.2+1.20.1"
    
    // Player2 API for LLM integration (optional)
    modImplementation "your.group:player2-api:1.0.0"
}
```

### Understanding the Framework

PlayerEngine uses a **component-based architecture** where any `LivingEntity` can be enhanced with player-like capabilities through interfaces and Cardinal Components. Key concepts:

- **No Custom Entity Types**: Enhance existing entities rather than creating new ones
- **Interface-Driven**: Implement core interfaces to gain capabilities
- **Component Registration**: Use Cardinal Components to attach systems
- **Task-Based AI**: High-level goals executed through task chains

## Core Architecture

### The Four Core Interfaces

Every player-like AI entity must implement these interfaces:

```java
public class YourAIEntity extends LivingEntity 
    implements IAutomatone, IInventoryProvider, IInteractionManagerProvider, IHungerManagerProvider {
    // Implementation details below
}
```

#### Interface Breakdown:

1. **`IAutomatone`**: Marker interface that identifies this entity as AI-controllable
2. **`IInventoryProvider`**: Provides full player-like inventory with armor, offhand, and storage
3. **`IInteractionManagerProvider`**: Enables block breaking, placing, and item usage
4. **`IHungerManagerProvider`**: Adds hunger system (optional to tick)

### Component Architecture Flow

```
Player Chat → EventQueueManager → Player2 API → LLM Processing
                                      ↓
AltoClefController ← Task System ← Command Generation
       ↓
Baritone Pathfinding → World Interaction → Entity Actions
```

## Step-by-Step Implementation

### Step 1: Create Your AI Entity Class

```java
package com.yourmod.entities;

import adris.altoclef.AltoClefController;
import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.EventQueueManager;
import baritone.api.IBaritone;
import baritone.api.entity.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class IntelligentCompanion extends LivingEntity 
    implements IAutomatone, IInventoryProvider, IInteractionManagerProvider, IHungerManagerProvider {
    
    // Core PlayerEngine components
    private LivingEntityInteractionManager interactionManager;
    private LivingEntityInventory inventory;
    private LivingEntityHungerManager hungerManager;
    
    // AI Controller
    private AltoClefController controller;
    
    // Character data for personality and behavior
    private Character character;
    
    // Player2 API game identifier
    private static final String GAME_ID = "your-game-id-here";
    
    public IntelligentCompanion(EntityType<? extends IntelligentCompanion> type, World world) {
        super(type, world);
        initialize();
    }
    
    public IntelligentCompanion(World world, Character character, PlayerEntity owner) {
        super(YourMod.COMPANION_ENTITY_TYPE, world);
        this.character = character;
        initialize();
        if (controller != null) {
            controller.setOwner(owner);
        }
    }
    
    private void initialize() {
        // Basic entity setup
        this.setStepHeight(0.6f);
        this.setMovementSpeed(0.4f);
        
        // Initialize PlayerEngine components
        this.interactionManager = new LivingEntityInteractionManager(this);
        this.inventory = new LivingEntityInventory(this);
        this.hungerManager = new LivingEntityHungerManager();
        
        // Initialize AI controller (server-side only)
        if (!getWorld().isClient && character != null) {
            this.controller = new AltoClefController(
                IBaritone.KEY.get(this), 
                character, 
                GAME_ID
            );
            
            // Send initial greeting to establish character
            EventQueueManager.sendGreeting(this.controller, character);
        }
    }
    
    // Interface implementations
    @Override
    public LivingEntityInventory getLivingInventory() {
        return inventory;
    }
    
    @Override
    public LivingEntityInteractionManager getInteractionManager() {
        return interactionManager;
    }
    
    @Override
    public LivingEntityHungerManager getHungerManager() {
        return hungerManager;
    }
    
    // Core entity lifecycle methods
    @Override
    public void tick() {
        super.tick();
        
        // Update PlayerEngine components
        if (interactionManager != null) {
            interactionManager.update();
        }
        
        if (inventory != null) {
            inventory.updateItems();
        }
        
        // Optional: Enable hunger system
        // if (hungerManager != null) {
        //     hungerManager.update(this);
        // }
        
        // Tick AI controller (server-side only)
        if (!getWorld().isClient && controller != null) {
            controller.serverTick();
        }
        
        // Enable attack cooldown
        this.lastAttackedTicks++;
        this.tickHandSwing();
    }
    
    @Override
    public void tickMovement() {
        super.tickMovement();
        this.headYaw = this.getYaw();
        pickupNearbyItems();
    }
    
    // Additional implementation methods (inventory, NBT, etc.) go here...
}
```

### Step 2: Register Entity Type

```java
package com.yourmod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class YourMod implements ModInitializer {
    
    public static final EntityType<IntelligentCompanion> COMPANION_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier("yourmod", "intelligent_companion"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, IntelligentCompanion::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
            .build()
    );
    
    @Override
    public void onInitialize() {
        // Additional mod initialization
    }
}
```

### Step 3: Component Registration

Create a component initializer to attach PlayerEngine capabilities:

```java
package com.yourmod.components;

import baritone.KeepName;
import baritone.api.IBaritone;
import baritone.api.BaritoneAPI;
import com.yourmod.entities.IntelligentCompanion;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;

@KeepName
public class YourModComponents implements EntityComponentInitializer {
    
    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Register PlayerEngine components for your entity
        registry.registerFor(
            IntelligentCompanion.class, 
            IBaritone.KEY, 
            BaritoneAPI.getProvider().componentFactory()
        );
        
        // Additional component registrations as needed
        // registry.registerFor(IntelligentCompanion.class, IInteractionController.KEY, EntityInteractionController::new);
        // registry.registerFor(IntelligentCompanion.class, ISelectionManager.KEY, SelectionManager::new);
    }
}
```

Register this in your `fabric.mod.json`:

```json
{
  "entrypoints": {
    "main": ["com.yourmod.YourMod"],
    "cardinal-components": ["com.yourmod.components.YourModComponents"]
  }
}
```

### Step 4: Entity Management System

Create a management system for spawning and controlling your AI entities:

```java
package com.yourmod.companion;

import adris.altoclef.player2api.Character;
import com.yourmod.entities.IntelligentCompanion;
import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.tick.ServerTickingComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CompanionManager implements Component, ServerTickingComponent {
    
    public static final ComponentKey<CompanionManager> KEY = ComponentRegistry
        .getOrCreate(new Identifier("yourmod", "companion_manager"), CompanionManager.class);
    
    private final ServerPlayerEntity player;
    private final Map<String, UUID> companions = new ConcurrentHashMap<>();
    
    public CompanionManager(ServerPlayerEntity player) {
        this.player = player;
    }
    
    public void spawnCompanion(Character character) {
        if (player.getWorld() == null) return;
        
        // Check if companion already exists
        UUID existingId = companions.get(character.name());
        if (existingId != null) {
            ServerWorld world = player.getServerWorld();
            if (world.getEntity(existingId) instanceof IntelligentCompanion existing && existing.isAlive()) {
                // Teleport existing companion to player
                BlockPos pos = player.getBlockPos().add(
                    player.getRandom().nextInt(3) - 1, 
                    1, 
                    player.getRandom().nextInt(3) - 1
                );
                existing.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                return;
            }
        }
        
        // Spawn new companion
        BlockPos spawnPos = player.getBlockPos().add(
            player.getRandom().nextInt(3) - 1, 
            1, 
            player.getRandom().nextInt(3) - 1
        );
        
        IntelligentCompanion companion = new IntelligentCompanion(player.getWorld(), character, player);
        companion.refreshPositionAndAngles(
            spawnPos.getX() + 0.5, 
            spawnPos.getY(), 
            spawnPos.getZ() + 0.5,
            player.getYaw(), 
            0
        );
        
        player.getServerWorld().spawnEntity(companion);
        companions.put(character.name(), companion.getUuid());
    }
    
    public void dismissCompanion(String characterName) {
        UUID companionId = companions.remove(characterName);
        if (companionId != null && player.getServer() != null) {
            for (var world : player.getServer().getWorlds()) {
                var entity = world.getEntity(companionId);
                if (entity instanceof IntelligentCompanion) {
                    entity.discard();
                    return;
                }
            }
        }
    }
    
    @Override
    public void serverTick() {
        // Periodic maintenance tasks if needed
    }
    
    @Override
    public void readFromNbt(NbtCompound tag) {
        NbtCompound companionsTag = tag.getCompound("companions");
        for (String key : companionsTag.getKeys()) {
            companions.put(key, companionsTag.getUuid(key));
        }
    }
    
    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtCompound companionsTag = new NbtCompound();
        companions.forEach(companionsTag::putUuid);
        tag.put("companions", companionsTag);
    }
}
```

Register the companion manager:

```java
// In your components class
@Override
public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    // Previous registrations...
    
    registry.registerFor(ServerPlayerEntity.class, CompanionManager.KEY, CompanionManager::new);
}
```

## Advanced Features

### Custom Commands

Create custom commands for your AI entities:

```java
package com.yourmod.commands;

import adris.altoclef.commands.AbstractCommand;
import adris.altoclef.AltoClefController;
import adris.altoclef.tasks.DoNothingTask;

public class CustomFollowCommand extends AbstractCommand {
    
    public CustomFollowCommand() {
        super("customfollow", "Follow a specific player with custom behavior");
    }
    
    @Override
    protected void call(AltoClefController mod, String[] args) {
        if (args.length == 0) {
            mod.runUserTask(new DoNothingTask(), this);
            return;
        }
        
        String playerName = args[0];
        mod.runUserTask(new CustomFollowPlayerTask(playerName), this);
    }
}
```

### Network Synchronization

For proper client-server synchronization:

```java
package com.yourmod.network;

import com.yourmod.entities.IntelligentCompanion;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;

public class CompanionSpawnPacket implements FabricPacket {
    // Implementation similar to AutomatonSpawnPacket
    // Include entity data, character info, and inventory state
}
```

### Character Integration

Integrate with Player2 API for dynamic character assignment:

```java
package com.yourmod.character;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import java.util.concurrent.CompletableFuture;

public class CharacterService {
    
    public static CompletableFuture<Character[]> loadCharacters(String gameId) {
        return CompletableFuture.supplyAsync(() -> 
            CharacterUtils.requestCharacters(gameId)
        );
    }
    
    public static Character createCustomCharacter(String name, String personality) {
        return new Character(
            name,
            name.toLowerCase(),
            personality,
            "A helpful AI companion",
            "default_skin_url",
            new String[]{"helpful", "friendly", "intelligent"}
        );
    }
}
```

## Integration Patterns

### Event Handling

Handle important game events:

```java
@Override
public void tick() {
    super.tick();
    
    // Custom event handling
    if (controller != null && !getWorld().isClient) {
        // Handle combat events
        if (getAttacker() instanceof PlayerEntity attacker && !isOwner(attacker.getUuid())) {
            controller.getEventQueueManager().addEvent(
                new Event.InfoMessage("I'm being attacked by " + attacker.getName().getString())
            );
        }
        
        // Handle environmental events
        if (isInLava()) {
            controller.getEventQueueManager().addEvent(
                new Event.InfoMessage("Help! I'm in lava!")
            );
        }
    }
}
```

### Custom Task Creation

Create specialized tasks for your entities:

```java
package com.yourmod.tasks;

import adris.altoclef.AltoClefController;
import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.player.PlayerEntity;

public class CustomProtectPlayerTask extends Task {
    private final String playerName;
    
    public CustomProtectPlayerTask(String playerName) {
        this.playerName = playerName;
    }
    
    @Override
    protected void onStart(AltoClefController mod) {
        // Initialize protection behavior
    }
    
    @Override
    protected Task onTick(AltoClefController mod) {
        PlayerEntity target = mod.getPlayer().getWorld().getPlayers().stream()
            .filter(p -> p.getName().getString().equals(playerName))
            .findFirst().orElse(null);
            
        if (target == null) {
            return null; // Task complete if player not found
        }
        
        // Implement protection logic
        return this; // Continue task
    }
    
    @Override
    protected void onStop(AltoClefController mod, Task interruptTask) {
        // Cleanup
    }
    
    @Override
    protected boolean isEqual(Task other) {
        return other instanceof CustomProtectPlayerTask cp && 
               cp.playerName.equals(this.playerName);
    }
    
    @Override
    protected String toDebugString() {
        return "Protecting " + playerName;
    }
}
```

## Best Practices

### Performance Optimization

1. **Server-Side Processing**: Keep AI logic server-side only
2. **Efficient Ticking**: Minimize expensive operations in tick methods
3. **Component Lazy Loading**: Initialize components only when needed
4. **Memory Management**: Clean up resources properly

```java
@Override
public void remove(RemovalReason reason) {
    // Clean up AI controller
    if (controller != null) {
        controller.shutdown();
        controller = null;
    }
    
    super.remove(reason);
}
```

### Error Handling

```java
private void safeInitialize() {
    try {
        this.controller = new AltoClefController(
            IBaritone.KEY.get(this), 
            character, 
            GAME_ID
        );
    } catch (Exception e) {
        // Log error and create fallback behavior
        YourMod.LOGGER.error("Failed to initialize AI controller", e);
        this.controller = null;
    }
}
```

### Configuration Management

Create configurable behavior:

```java
public class CompanionConfig {
    public static final boolean ENABLE_HUNGER = false;
    public static final float FOLLOW_DISTANCE = 3.0f;
    public static final int CHAT_RADIUS = 64;
    public static final boolean AUTO_PICKUP_ITEMS = true;
    
    // Load from config file or use defaults
}
```

## Troubleshooting

### Common Issues

1. **Controller Not Initializing**
   - Ensure `IBaritone.KEY` component is registered
   - Check server-side only initialization
   - Verify character data is not null

2. **Inventory Not Syncing**
   - Implement proper NBT serialization
   - Use custom spawn packets for full state sync
   - Call `inventory.updateItems()` in tick

3. **Entity Not Responding to Commands**
   - Check Player2 API integration
   - Verify EventQueueManager setup
   - Ensure proper chat radius configuration

4. **Pathfinding Issues**
   - Confirm Baritone component registration
   - Check entity collision box size
   - Verify step height configuration

### Debug Tools

```java
public void debugInfo() {
    if (YourMod.DEBUG_MODE) {
        YourMod.LOGGER.info("Entity: {}, Controller: {}, Character: {}", 
            this.getUuid(), 
            controller != null, 
            character != null ? character.name() : "null"
        );
    }
}
```

## Conclusion

This guide provides a comprehensive foundation for creating intelligent, player-like AI entities using PlayerEngine. The key principles are:

1. **Interface Implementation**: Implement core PlayerEngine interfaces
2. **Component Registration**: Use Cardinal Components for modular capabilities
3. **Proper Initialization**: Set up AI controller server-side only
4. **Lifecycle Management**: Handle entity spawning, persistence, and cleanup
5. **Error Handling**: Implement robust error handling and fallbacks

By following these patterns, you can create sophisticated AI companions that seamlessly integrate with Minecraft's gameplay systems while leveraging the full power of PlayerEngine's task-based AI framework.

For more advanced use cases, refer to the PlayerEngine source code and the Player2NPC mod implementation for real-world examples of these concepts in action.