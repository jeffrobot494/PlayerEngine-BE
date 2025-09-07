# PlayerEngine Developer Index

## Overview

**PlayerEngine** is a server-side framework for creating AI-powered NPCs in Minecraft that can perform player-like actions. Built on Fabric 1.20.1, it integrates Automatone (Baritone pathfinding fork), Player2 API, and a robust task execution system.

**Total Java Files**: 647  
**Core Architecture**: Task-Chain system with priority-based execution  
**External Dependencies**: Fabric, Cardinal Components, Jackson JSON, Player2 API

---

## 🏗️ Core Architecture Overview

### Primary Components
1. **AltoClefController** - Central orchestrator managing all subsystems
2. **Task System** - Priority-based execution engine with chain management  
3. **Tracker System** - Game state monitoring and caching
4. **Player2API Integration** - AI service communication layer
5. **Baritone Integration** - Advanced pathfinding and navigation

### Key Patterns
- **Task-Chain Architecture**: Hierarchical task execution with interruption support
- **Tracker Pattern**: Passive state monitoring with event-driven updates
- **State Stack Management**: Behavioral configuration with push/pop operations
- **Helper/Utility Pattern**: Static utility methods for common operations
- **Entity Enhancement**: Any `LivingEntity` can be enhanced via `EntityComponentKey` system
- **Multi-Entity AI**: Multiple entities can have individual AI controllers and chat processing

---

## 📁 Directory Structure

### Core Source Directories
```
src/
├── main/java/baritone/          # Baritone pathfinding engine (fork)
└── autoclef/java/adris/altoclef/ # PlayerEngine core implementation
    ├── chains/                   # Task execution chains
    ├── commands/                 # Command implementations
    ├── control/                  # Input/interaction control systems
    ├── player2api/              # AI service integration
    ├── tasks/                   # Task implementations
    ├── tasksystem/              # Task execution framework
    ├── trackers/                # Game state tracking
    └── util/                    # Utility classes and helpers
```

### Resource Directories
```
src/
├── autoclef/resources/          # Fabric mod resources
└── main/resources/              # Baritone resources and textures
```

---

## 🎯 Entry Points & Core Controllers

### Main Entry Point
- **`AltoClefController.java`** - Central orchestrator class
  - Path: `src/autoclef/java/adris/altoclef/AltoClefController.java:1`
  - Manages all subsystems: TaskRunner, TrackerManager, Player2APIService
  - Coordinates chains, controls, and game state

### Configuration Management
- **`Settings.java`** - Bot configuration and settings
  - Path: `src/autoclef/java/adris/altoclef/Settings.java:1`
  - JSON-serializable configuration for all bot behaviors

- **`BotBehaviour.java`** - Behavioral state management
  - Path: `src/autoclef/java/adris/altoclef/BotBehaviour.java:1`
  - Stack-based state system for behavior configuration

### Command System
- **`AltoClefCommands.java`** - Command registration
  - Path: `src/autoclef/java/adris/altoclef/AltoClefCommands.java:1`
  - Central command registry for all bot commands

---

## ⚙️ Task System Core

### Framework Classes
- **`Task.java`** - Abstract base class for all tasks
  - Path: `src/autoclef/java/adris/altoclef/tasksystem/Task.java:1`
  - Lifecycle management, subtask handling, debug state tracking

- **`TaskRunner.java`** - Task execution engine
  - Path: `src/autoclef/java/adris/altoclef/tasksystem/TaskRunner.java:1`
  - Priority-based execution, chain management, interruption handling

- **`TaskChain.java`** - Task grouping and prioritization
  - Path: `src/autoclef/java/adris/altoclef/tasksystem/TaskChain.java:1`
  - Chain-based task organization with priority resolution

### Key Task Implementations
- **`AbstractDoToClosestObjectTask.java`** - Template for proximity-based tasks
  - Path: `src/autoclef/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java:1`
  - Heuristic pathfinding, object validation, goal switching

### Task Categories
```
tasks/
├── construction/    # Building and placement tasks
├── container/      # Inventory and storage management
├── entity/         # Entity interaction tasks
├── movement/       # Movement and navigation tasks
├── resources/      # Resource collection tasks
├── slot/          # Inventory slot manipulation
└── speedrun/      # Optimized gameplay tasks
```

---

## 🔗 Chain System

Task chains provide priority-based task execution with interruption capabilities:

### Core Chains
- **`UserTaskChain.java`** - User-defined tasks and commands
  - Path: `src/autoclef/java/adris/altoclef/chains/UserTaskChain.java:1`

- **`MobDefenseChain.java`** - Combat and mob defense automation
  - Path: `src/autoclef/java/adris/altoclef/chains/MobDefenseChain.java:1`

- **`FoodChain.java`** - Hunger management and food acquisition
  - Path: `src/autoclef/java/adris/altoclef/chains/FoodChain.java:1`

- **`WorldSurvivalChain.java`** - Environmental hazard management
  - Path: `src/autoclef/java/adris/altoclef/chains/WorldSurvivalChain.java:1`

- **`UnstuckChain.java`** - Movement problem resolution
  - Path: `src/autoclef/java/adris/altoclef/chains/UnstuckChain.java:1`

---

## 📊 Tracking System

### Core Tracker Classes
- **`TrackerManager.java`** - Coordinates all tracking subsystems
  - Path: `src/autoclef/java/adris/altoclef/trackers/TrackerManager.java:1`

- **`EntityTracker.java`** - Entity monitoring and caching
  - Path: `src/autoclef/java/adris/altoclef/trackers/EntityTracker.java:1`

- **`ItemStorageTracker.java`** - Container and inventory tracking
  - Path: `src/autoclef/java/adris/altoclef/trackers/storage/ItemStorageTracker.java:1`

- **`SimpleChunkTracker.java`** - Chunk loading and world state
  - Path: `src/autoclef/java/adris/altoclef/trackers/SimpleChunkTracker.java:1`

---

## 🧠 Player2 API Integration

### Core Integration Classes
- **`Player2APIService.java`** - Main AI service communication
  - Path: `src/autoclef/java/adris/altoclef/player2api/Player2APIService.java:1`
  - HTTP request handling, conversation completion, authentication

- **`EventQueueManager.java`** - Event queuing and processing
  - Path: `src/autoclef/java/adris/altoclef/player2api/EventQueueManager.java:1`

- **`Character.java`** - AI character configuration
  - Path: `src/autoclef/java/adris/altoclef/player2api/Character.java:1`

### API Utilities
- **`CharacterUtils.java`** - Character data parsing and management
  - Path: `src/autoclef/java/adris/altoclef/player2api/utils/CharacterUtils.java:1`

- **`AgentCommandUtils.java`** - Command execution utilities
  - Path: `src/autoclef/java/adris/altoclef/player2api/AgentCommandUtils.java:1`

---

## 🛠️ Essential Utility Classes

### Item Management
- **`ItemHelper.java`** - Comprehensive item utilities
  - Path: `src/autoclef/java/adris/altoclef/util/helpers/ItemHelper.java:1`
  - Item categorization, material matching, tool identification

- **`ItemTarget.java`** - Target item specification
  - Path: `src/autoclef/java/adris/altoclef/util/ItemTarget.java:1`
  - Item matching, quantity targets, catalogue integration

### World Interaction
- **`WorldHelper.java`** - World interaction and spatial utilities
  - Path: `src/autoclef/java/adris/altoclef/util/helpers/WorldHelper.java:1`
  - Block position conversions, world state queries, dimension handling

- **`StorageHelper.java`** - Inventory and container management
  - Path: `src/autoclef/java/adris/altoclef/util/helpers/StorageHelper.java:1`

### Player Control
- **`LookHelper.java`** - Player look direction and targeting
  - Path: `src/autoclef/java/adris/altoclef/util/helpers/LookHelper.java:1`

- **`InputControls.java`** - Input automation and control
  - Path: `src/autoclef/java/adris/altoclef/control/InputControls.java:1`

- **`SlotHandler.java`** - Inventory slot manipulation
  - Path: `src/autoclef/java/adris/altoclef/control/SlotHandler.java:1`

---

## 📚 Task Catalog System

### Resource Management
- **`TaskCatalogue.java`** - Registry of all available tasks and strategies
  - Path: `src/autoclef/java/adris/altoclef/TaskCatalogue.java:1`
  - Item-to-task mapping, crafting recipes, resource collection strategies

### Command Implementations
High-level commands that translate user intent into task execution:
```
commands/
├── GetCommand.java          # Item acquisition
├── GotoCommand.java         # Movement commands  
├── FollowCommand.java       # Player following
├── AttackCommand.java       # Combat commands
└── BodyLanguageCommand.java # Social behaviors
```

---

## 🔧 Control Systems

### Input Management
- **`InputControls.java`** - Keyboard and mouse automation
  - Path: `src/autoclef/java/adris/altoclef/control/InputControls.java:1`

- **`PlayerExtraController.java`** - Extended player control functionality
  - Path: `src/autoclef/java/adris/altoclef/control/PlayerExtraController.java:1`

- **`KillAura.java`** - Combat automation
  - Path: `src/autoclef/java/adris/altoclef/control/KillAura.java:1`

---

## 🎮 Key Commands for Development

### Essential Commands
- `@get <item> [count]` - Acquire specified items
- `@goto <position>` - Move to coordinates or structure
- `@follow <player>` - Follow another player
- `@stop` - Halt current task execution
- `@status` - Display current bot status
- `@spawn <name> [x] [y] [z]` - Spawn PlayerLike entities
- `@chatclef on/off` - Enable/disable AI bridge for chat processing

### Debug Commands  
- `@pause` / `@unpause` - Control bot execution
- `@reset` - Reset bot memory and state
- `@inventory` - Display current inventory

---

## 🚀 Getting Started for Developers

### 1. Understanding the Flow
1. Commands are registered in `AltoClefCommands.java:1`
2. Commands create tasks and submit them to `TaskRunner`
3. `TaskRunner` executes tasks through priority-based chain system
4. Tasks use utilities and helpers to interact with the game world

### 2. Adding a New Command
1. Create command class in `src/autoclef/java/adris/altoclef/commands/`
2. Register in `AltoClefCommands.java:1`
3. Implement command logic using existing task framework

### 3. Adding a New Task
1. Extend `Task` class in `src/autoclef/java/adris/altoclef/tasks/`
2. Implement required lifecycle methods
3. Add to appropriate task category directory
4. Register in `TaskCatalogue.java:1` if needed

### 4. Key Files to Modify
- **New Commands**: `src/autoclef/java/adris/altoclef/commands/`
- **New Tasks**: `src/autoclef/java/adris/altoclef/tasks/`
- **Configuration**: `src/autoclef/java/adris/altoclef/Settings.java:1`
- **Task Registration**: `src/autoclef/java/adris/altoclef/TaskCatalogue.java:1`

---

## 📖 Additional Resources

### Implementation Guides
- **`docs/CREATE_PLAYERLIKE_ENTITY.md`** - Complete guide for creating player-like AI entities
- **`Sir_Roderick.md`** - Example character implementation specification

### Build Configuration
- **`build.gradle`** - Project dependencies and build configuration
- **`settings.gradle`** - Plugin management and project settings
- **`gradle.properties`** - Version and configuration properties

### Documentation
- **`README.md`** - Project overview and setup instructions
- **`LICENSE`** - Project licensing information

## 🎯 Architecture Clarifications

### Entity Enhancement Reality
- **No "Bot Entity"**: PlayerEngine doesn't create bot entities - it enhances existing `LivingEntity` instances
- **Universal Enhancement**: Any `LivingEntity` can be enhanced with PlayerEngine capabilities
- **Controller Pattern**: `AltoClefController` operates on whatever entity it's attached to via `EntityContext`
- **Multi-Instance Support**: Multiple entities can have individual controllers and AI processing

### Chat Processing Architecture  
- **Single vs Multi-Entity**: Original system designed for one entity, can be extended for multiple
- **Distance-Based**: Chat messages are filtered by proximity to entities (64 block default)
- **Individual Intelligence**: Each enhanced entity can have its own conversation history and AI personality

---

## 🔧 Recent Modifications & Enhancements

This section documents significant changes made to improve PlayerEngine functionality and fix critical issues.

### **1. Enhanced Task Change Logging** 
**Files Modified**: `SingleTaskChain.java:55`, `Task.java:38,41,49`, `TaskRunner.java:36`
- **Added comprehensive task transition logging** to terminal output
- **Chain switches**: `Chain Switch: Food Chain → User Tasks`  
- **Main task changes**: `Task Change [User Tasks]: GetItemTask → PlaceBlockTask`
- **Subtask changes**: `Subtask Start [GetItemTask]: MineAndCollectTask`
- **Uses `Debug.logInternal()`** for live terminal visibility (not log files)
- **Benefit**: Real-time debugging of AI decision-making process

### **2. PlaceCommand Implementation**
**Files Added**: `PlaceCommand.java`, **Modified**: `AltoClefCommands.java:19,42`
- **New LLM command**: `place <block> [x y z]` for block placement
- **Dual modes**: Specific coordinates or nearby placement  
- **Smart block parsing**: Handles aliases like "wood" → "oak_planks"
- **Integration**: Uses existing `PlaceBlockTask` and `PlaceBlockNearbyTask`
- **Usage**: `place stone 100 64 200` or `place cobblestone` (nearby)
- **Benefit**: Enables AI to build structures through LLM commands

### **3. Entity Self-Position Awareness**
**File Modified**: `AgentStatus.java:10`
- **Added "my-position" field** to LLM prompt data
- **Format**: `"my-position": "(100, 64, -50)"`
- **Critical missing data**: AI now knows where it is in the world
- **Enables spatial reasoning**: Distance calculations, relative positioning
- **Benefit**: Dramatically improves building, navigation, and spatial tasks

### **4. JSON Parsing Resilience** 
**File Modified**: `Utils.java:69-95` *(Note: Incomplete due to build errors)*
- **Added lenient JSON parsing** for malformed LLM responses
- **Fallback system**: Strict parsing → lenient parsing → detailed error
- **Content cleaning**: Removes JSON markdown blocks and extracts JSON
- **Error context**: Better error messages for debugging
- **Benefit**: Reduces LLM response parsing failures

### **5. Critical Goto Command Bug Fix**
**File Modified**: `GotoTarget.java:42-70`
- **Fixed coordinate parsing bug** where all goto commands went to (0,0,0)
- **Root cause**: Switch expression executed after constructor call
- **Solution**: Traditional switch statement with proper execution order
- **Impact**: `goto 100 64 200` now works correctly instead of going to origin
- **Benefit**: AI can navigate to specified coordinates properly

### **6. WorldStatus Information System Documentation**
**Analysis Completed**: LLM prompt compilation system
- **Identified**: `WorldStatus.fromMod()` → `StatusUtils` methods → LLM prompt
- **Data sources**: Weather, hostiles, players, NPCs, time, difficulty
- **Key insight**: Entity position was missing from AI awareness (now fixed)
- **Scanning ranges**: 32 blocks (entities), 12 blocks (blocks)
- **Benefit**: Understanding of how AI perceives world state

### **Command Development Guidelines Added**
**Section Enhanced**: Developer Index command guidelines
- **Sequential argument parsing**: Use `parser.get(Class)` in order
- **Optional arguments**: Proper default value handling
- **Error handling**: Try-catch for missing arguments
- **Task integration**: Standard `mod.runUserTask(task, this::finish)` pattern
- **Import corrections**: Proper Minecraft mappings for this version

### **BrainLoop Design Documentation** 
**File Created**: `docs/BrainLoop_Design.md`
- **Background monitoring system**: Tick-based condition evaluation
- **Extensible architecture**: Easy addition of new monitoring conditions  
- **Emergency responses**: Auto-reactions to health, drowning, night exposure
- **Task integration**: Can trigger tasks when conditions are met
- **Status**: Design documented, not yet implemented

## 🚨 Known Issues Resolved

1. **Goto Command Failure** ✅ - Fixed coordinate parsing execution order
2. **Missing Entity Position** ✅ - Added self-position to agent status  
3. **JSON Parsing Errors** ⚠️ - Partially addressed, needs completion
4. **Limited Task Visibility** ✅ - Added comprehensive task logging
5. **Missing Place Command** ✅ - Implemented full block placement system

## 🎯 Impact Summary

These modifications significantly enhance PlayerEngine's capability for:
- **Spatial reasoning and navigation**
- **Complex building and construction tasks** 
- **Real-time debugging and monitoring**
- **Robust LLM communication**
- **Command extensibility**

The changes transform PlayerEngine from a basic task executor into a more capable, observable, and spatially-aware AI system suitable for complex world interaction tasks.

---

*This index provides a comprehensive navigation guide for the PlayerEngine codebase. For specific implementation details, refer to the individual source files using the provided file paths and line numbers.*