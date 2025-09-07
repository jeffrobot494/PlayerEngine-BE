# BrainLoop Design - Entity Condition Monitoring System

## Overview

A "brain loop" system that runs every server tick to evaluate conditions and trigger actions when certain criteria are met. This operates as a background monitoring system for AI entities.

## Architecture

### Core Concept
- Extends `TaskChain` with very low priority (-1.0f)
- Runs continuously in background alongside other chains
- Evaluates list of conditions every server tick
- Triggers actions when conditions are met
- Includes cooldown system to prevent spam

### Integration Points
- Integrates with `AltoClefController` via `TaskRunner`
- Can trigger tasks through existing task system
- Uses `Debug.logInternal()` for terminal output
- Accesses world state through controller

## Implementation

```java
package adris.altoclef.chains;

import adris.altoclef.AltoClefController;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A special TaskChain that runs every server tick to evaluate conditions
 * and trigger actions when certain criteria are met.
 * 
 * This operates as a background monitoring system that can:
 * - Check world state conditions
 * - Monitor entity status
 * - Trigger emergency responses
 * - Execute periodic maintenance tasks
 */
public class BrainLoop extends TaskChain {
    
    private final List<BrainCondition> conditions = new ArrayList<>();
    
    public BrainLoop(TaskRunner runner) {
        super(runner);
        initializeDefaultConditions();
    }
    
    /**
     * Initialize default conditions that should always be monitored
     */
    private void initializeDefaultConditions() {
        // Example: Low health emergency
        addCondition(
            "Emergency Health", 
            () -> controller.getPlayer().getHealth() < 4.0f,
            () -> Debug.logInternal("Brain Loop: Emergency health detected!")
        );
        
        // Example: Drowning detection
        addCondition(
            "Drowning Alert",
            () -> controller.getPlayer().getAirSupply() < 60, // Less than 3 seconds of air
            () -> Debug.logInternal("Brain Loop: Entity is drowning!")
        );
        
        // Example: Night time safety check
        addCondition(
            "Night Safety",
            () -> controller.getWorld().isNight() && controller.getWorld().canSeeSky(controller.getPlayer().blockPosition()),
            () -> Debug.logInternal("Brain Loop: Entity exposed at night!")
        );
    }
    
    @Override
    protected void onTick() {
        // Run every server tick - evaluate all conditions
        for (BrainCondition condition : conditions) {
            try {
                if (condition.shouldTrigger()) {
                    if (!condition.hasTriggeredRecently()) {
                        condition.trigger();
                        condition.markTriggered();
                    }
                }
            } catch (Exception e) {
                Debug.logError("Brain Loop condition '" + condition.getName() + "' failed: " + e.getMessage());
            }
        }
    }
    
    @Override
    protected void onStop() {
        // Reset all condition states when stopping
        for (BrainCondition condition : conditions) {
            condition.reset();
        }
    }
    
    @Override
    public void onInterrupt(TaskChain other) {
        // Brain loop should generally not be interrupted
        // It runs in background at low priority
    }
    
    @Override
    public float getPriority() {
        // Very low priority - only runs when no other chains are active
        // or use negative priority to run alongside other chains
        return -1.0f;
    }
    
    @Override
    public boolean isActive() {
        // Always active - continuously monitoring
        return true;
    }
    
    @Override
    public String getName() {
        return "Brain Loop";
    }
    
    /**
     * Add a new condition to monitor
     */
    public void addCondition(String name, Supplier<Boolean> condition, Runnable action) {
        conditions.add(new BrainCondition(name, condition, action));
    }
    
    /**
     * Add a condition that can trigger a task
     */
    public void addTaskCondition(String name, Supplier<Boolean> condition, Supplier<Task> taskSupplier) {
        conditions.add(new BrainCondition(name, condition, () -> {
            Task task = taskSupplier.get();
            if (task != null) {
                Debug.logInternal("Brain Loop: Triggering task - " + task);
                // You could trigger this through the UserTaskChain or create a special BrainTaskChain
                controller.runUserTask(task, () -> {});
            }
        }));
    }
    
    /**
     * Remove a condition by name
     */
    public void removeCondition(String name) {
        conditions.removeIf(condition -> condition.getName().equals(name));
    }
    
    /**
     * Get all active conditions (for debugging)
     */
    public List<String> getActiveConditions() {
        List<String> active = new ArrayList<>();
        for (BrainCondition condition : conditions) {
            try {
                if (condition.shouldTrigger()) {
                    active.add(condition.getName());
                }
            } catch (Exception e) {
                active.add(condition.getName() + " (ERROR)");
            }
        }
        return active;
    }
    
    /**
     * Inner class representing a brain condition
     */
    private static class BrainCondition {
        private final String name;
        private final Supplier<Boolean> condition;
        private final Runnable action;
        private long lastTriggeredTime = 0;
        private static final long COOLDOWN_MS = 5000; // 5 second cooldown between triggers
        
        public BrainCondition(String name, Supplier<Boolean> condition, Runnable action) {
            this.name = name;
            this.condition = condition;
            this.action = action;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean shouldTrigger() {
            return condition.get();
        }
        
        public void trigger() {
            action.run();
        }
        
        public boolean hasTriggeredRecently() {
            return System.currentTimeMillis() - lastTriggeredTime < COOLDOWN_MS;
        }
        
        public void markTriggered() {
            lastTriggeredTime = System.currentTimeMillis();
        }
        
        public void reset() {
            lastTriggeredTime = 0;
        }
    }
}
```

## Usage Examples

### Adding Custom Conditions

```java
// In AltoClefController initialization
BrainLoop brainLoop = new BrainLoop(taskRunner);

// Add custom monitoring conditions
brainLoop.addCondition(
    "Low Food Warning",
    () -> controller.getPlayer().getFoodData().getFoodLevel() < 6,
    () -> Debug.logInternal("Brain Loop: Low food detected!")
);

brainLoop.addTaskCondition(
    "Auto Food Gathering",
    () -> controller.getPlayer().getFoodData().getFoodLevel() < 4,
    () -> new FoodTask() // Automatically gather food when very hungry
);
```

### Integration with AltoClefController

Add to controller initialization:
```java
// In AltoClefController constructor, after other chains
BrainLoop brainLoop = new BrainLoop(this.taskRunner);
```

## Key Features

### 1. **Background Monitoring**
- Runs at low priority alongside other chains
- Doesn't interfere with main task execution
- Continuous evaluation every server tick

### 2. **Condition System**
- Simple boolean conditions with lambda expressions
- Custom actions when conditions are met
- Built-in cooldown to prevent spam

### 3. **Task Integration**
- Can trigger tasks through existing system
- Supports both simple actions and complex task chains
- Emergency response capabilities

### 4. **Default Conditions**
- **Emergency Health**: Triggers when health < 4.0
- **Drowning Alert**: Triggers when air supply < 60 ticks
- **Night Safety**: Triggers when exposed at night

### 5. **Extensibility**
- Easy to add new conditions
- Support for both logging and task triggering
- Exception handling for robust operation

## Benefits

1. **Proactive Behavior**: Entity can respond to conditions automatically
2. **Safety Systems**: Automatic emergency responses
3. **Contextual Awareness**: React to environmental changes
4. **Debugging**: Live monitoring of entity state
5. **Flexibility**: Easy to add/remove monitoring conditions

## Potential Use Cases

- **Emergency Health Management**: Auto-retreat when low health
- **Environmental Hazard Detection**: Lava, drowning, fall damage
- **Resource Monitoring**: Auto-gather when supplies low
- **Time-Based Actions**: Shelter seeking at night
- **Combat Awareness**: Detect nearby threats
- **Maintenance Tasks**: Periodic equipment checks
- **Social Behavior**: React to nearby players/NPCs

This system would provide a foundation for more intelligent, reactive AI behavior beyond just following LLM commands.