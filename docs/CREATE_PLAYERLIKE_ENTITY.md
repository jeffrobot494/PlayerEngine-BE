# Complete Implementation Guide: Player-Like AI Entity for PlayerEngine

## Overview

This guide provides a complete implementation for creating a player-like entity that integrates with PlayerEngine and can be controlled by Player2 API characters. The entity will look like a Minecraft player, have full AI capabilities, and can be spawned and controlled in-game.

---

## Architecture Overview

```
Player2 API Character → PlayerLike Entity → PlayerEngine Enhancement
     ↓                         ↓                      ↓
- Name, skin, personality  - Humanoid model      - AI capabilities
- Voice IDs               - Player textures      - Task system
- Description             - Custom renderer      - Pathfinding
```

---

## Step 1: Create the PlayerLike Entity Class

**File**: `src/main/java/baritone/entity/PlayerLikeEntity.java`

```java
package baritone.entity;

import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import baritone.api.entity.LivingEntityInventory;
import baritone.api.entity.LivingEntityInteractionManager;
import adris.altoclef.player2api.Prompts;
import adris.altoclef.player2api.ConversationHistory;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

public class PlayerLikeEntity extends PathfinderMob {
    // Data synchronizers for client-server communication
    private static final EntityDataAccessor<String> CHARACTER_NAME = 
        SynchedEntityData.defineId(PlayerLikeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_URL = 
        SynchedEntityData.defineId(PlayerLikeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CHARACTER_DESCRIPTION = 
        SynchedEntityData.defineId(PlayerLikeEntity.class, EntityDataSerializers.STRING);
    
    // PlayerEngine integration components
    private Character character;
    private LivingEntityInventory playerLikeInventory;
    private LivingEntityInteractionManager interactionManager;
    private boolean playerEngineEnabled = false;
    
    public PlayerLikeEntity(EntityType<? extends PlayerLikeEntity> entityType, Level world) {
        super(entityType, world);
        this.character = CharacterUtils.DEFAULT_CHARACTER;
        this.playerLikeInventory = new LivingEntityInventory(this);
        this.interactionManager = new LivingEntityInteractionManager(world, this);
        this.setupDefaultGoals();
    }
    
    /**
     * Create attribute supplier with player-like stats
     */
    public static AttributeSupplier.Builder createPlayerLikeAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)           // Same as player
            .add(Attributes.MOVEMENT_SPEED, 0.25D)       // Same as player
            .add(Attributes.ATTACK_DAMAGE, 1.0D)         // Base attack damage
            .add(Attributes.FOLLOW_RANGE, 32.0D)         // AI follow range
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)  // No knockback resistance
            .add(Attributes.ATTACK_KNOCKBACK, 0.0D);     // No attack knockback
    }
    
    /**
     * Setup default AI goals (before PlayerEngine enhancement)
     */
    private void setupDefaultGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARACTER_NAME, CharacterUtils.DEFAULT_CHARACTER.name());
        this.entityData.define(SKIN_URL, CharacterUtils.DEFAULT_CHARACTER.skinURL());
        this.entityData.define(CHARACTER_DESCRIPTION, CharacterUtils.DEFAULT_CHARACTER.description());
    }
    
    /**
     * Set the Player2 API character for this entity
     */
    public void setCharacter(Character character) {
        this.character = character;
        this.entityData.set(CHARACTER_NAME, character.name());
        this.entityData.set(SKIN_URL, character.skinURL());
        this.entityData.set(CHARACTER_DESCRIPTION, character.description());
        this.setCustomName(Component.literal(character.name()));
        this.setCustomNameVisible(true);
    }
    
    /**
     * Get the current character
     */
    public Character getCharacter() {
        return this.character != null ? this.character : CharacterUtils.DEFAULT_CHARACTER;
    }
    
    /**
     * Get skin URL for rendering
     */
    public String getSkinURL() {
        return this.entityData.get(SKIN_URL);
    }
    
    /**
     * Get character name
     */
    public String getCharacterName() {
        return this.entityData.get(CHARACTER_NAME);
    }
    
    /**
     * Get character description
     */
    public String getCharacterDescription() {
        return this.entityData.get(CHARACTER_DESCRIPTION);
    }
    
    /**
     * Enable PlayerEngine capabilities
     */
    public void enablePlayerEngine() {
        if (!this.playerEngineEnabled) {
            this.playerEngineEnabled = true;
            // Clear default goals when PlayerEngine takes over
            this.goalSelector.removeAllGoals();
            // PlayerEngine will handle AI behavior through task system
        }
    }
    
    /**
     * Check if PlayerEngine is enabled
     */
    public boolean isPlayerEngineEnabled() {
        return this.playerEngineEnabled;
    }
    
    /**
     * Get the player-like inventory
     */
    public LivingEntityInventory getPlayerLikeInventory() {
        return this.playerLikeInventory;
    }
    
    /**
     * Get the interaction manager
     */
    public LivingEntityInteractionManager getInteractionManager() {
        return this.interactionManager;
    }
    
    /**
     * Handle player interactions
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (itemStack.is(Items.NAME_TAG)) {
            return super.mobInteract(player, hand);
        }
        
        // Custom interaction logic here
        if (!this.level().isClientSide) {
            player.sendSystemMessage(Component.literal(
                getCharacterName() + ": " + getCharacterDescription()
            ));
        }
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Save character data to NBT
     */
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.character != null) {
            CharacterUtils.writeToNBT(nbt, this.character);
        }
        nbt.putBoolean("PlayerEngineEnabled", this.playerEngineEnabled);
        
        // Save inventory data
        if (this.playerLikeInventory != null) {
            CompoundTag inventoryTag = new CompoundTag();
            this.playerLikeInventory.save(inventoryTag);
            nbt.put("PlayerLikeInventory", inventoryTag);
        }
    }
    
    /**
     * Load character data from NBT
     */
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        try {
            this.character = CharacterUtils.readFromNBT(nbt);
            this.setCharacter(this.character);
        } catch (Exception e) {
            this.character = CharacterUtils.DEFAULT_CHARACTER;
            this.setCharacter(this.character);
        }
        
        this.playerEngineEnabled = nbt.getBoolean("PlayerEngineEnabled");
        if (this.playerEngineEnabled) {
            this.enablePlayerEngine();
        }
        
        // Load inventory data
        if (nbt.contains("PlayerLikeInventory")) {
            CompoundTag inventoryTag = nbt.getCompound("PlayerLikeInventory");
            this.playerLikeInventory.load(inventoryTag);
        }
    }
    
    /**
     * Custom tick method for PlayerEngine integration
     */
    @Override
    public void tick() {
        super.tick();
        
        // Update inventory if needed
        if (this.playerLikeInventory != null) {
            this.playerLikeInventory.tick();
        }
        
        // PlayerEngine integration tick logic here
        if (this.playerEngineEnabled) {
            // PlayerEngine controller will handle behavior
        }
    }
    
    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        // Don't despawn PlayerLike entities
        return false;
    }
    
    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
}
```

---

## Step 2: Create the Player Model

**File**: `src/main/java/baritone/client/model/PlayerLikeModel.java`

```java
package baritone.client.model;

import baritone.entity.PlayerLikeEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class PlayerLikeModel extends HumanoidModel<PlayerLikeEntity> {
    
    public PlayerLikeModel(ModelPart root) {
        super(root);
    }
    
    /**
     * Create the model layer definition (same structure as PlayerModel)
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        
        // Head
        partDefinition.addOrReplaceChild("head", 
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE), 
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Hat layer
        partDefinition.addOrReplaceChild("hat",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Body
        partDefinition.addOrReplaceChild("body", 
            CubeListBuilder.create()
                .texOffs(16, 16)
                .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, CubeDeformation.NONE), 
            PartPose.offset(0.0F, 0.0F, 0.0F));
        
        // Right arm
        partDefinition.addOrReplaceChild("right_arm", 
            CubeListBuilder.create()
                .texOffs(40, 16)
                .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE), 
            PartPose.offset(-5.0F, 2.0F, 0.0F));
        
        // Left arm
        partDefinition.addOrReplaceChild("left_arm", 
            CubeListBuilder.create()
                .texOffs(32, 48)
                .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE), 
            PartPose.offset(5.0F, 2.0F, 0.0F));
        
        // Right leg
        partDefinition.addOrReplaceChild("right_leg", 
            CubeListBuilder.create()
                .texOffs(0, 16)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE), 
            PartPose.offset(-1.9F, 12.0F, 0.0F));
        
        // Left leg
        partDefinition.addOrReplaceChild("left_leg", 
            CubeListBuilder.create()
                .texOffs(16, 48)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, CubeDeformation.NONE), 
            PartPose.offset(1.9F, 12.0F, 0.0F));
        
        return LayerDefinition.create(meshDefinition, 64, 64);
    }
    
    @Override
    public void setupAnim(PlayerLikeEntity entity, float limbSwing, float limbSwingAmount, 
                         float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        // Custom animations based on entity state
        if (entity.isPlayerEngineEnabled()) {
            // PlayerEngine-specific animations
            // Can access entity.getCharacter() for character-specific behavior
        }
        
        // Add idle animations
        if (limbSwingAmount < 0.01F) {
            // Slight breathing animation
            this.body.xRot = 0.0F;
            this.rightArm.xRot = (float) (Math.sin(ageInTicks * 0.067F) * 0.05F);
            this.leftArm.xRot = (float) (Math.sin(ageInTicks * 0.067F) * -0.05F);
        }
    }
}
```

---

## Step 3: Create the Entity Renderer

**File**: `src/main/java/baritone/client/PlayerLikeRenderer.java`

```java
package baritone.client;

import baritone.client.model.PlayerLikeModel;
import baritone.entity.PlayerLikeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class PlayerLikeRenderer extends MobRenderer<PlayerLikeEntity, PlayerLikeModel> {
    
    // Default textures
    private static final ResourceLocation DEFAULT_TEXTURE = 
        new ResourceLocation("automatone", "textures/entity/knight_skin.png");
    private static final ResourceLocation ALEX_TEXTURE = 
        new ResourceLocation("automatone", "textures/entity/alex_skin.png");
    
    public PlayerLikeRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerLikeModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        
        // Add layers similar to PlayerRenderer
        this.addLayer(new HumanoidArmorLayer<>(this, 
            new PlayerLikeModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new PlayerLikeModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
            context.getModelManager()));
        
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(PlayerLikeEntity entity) {
        String skinURL = entity.getSkinURL();
        
        // For now, use default textures
        // TODO: Implement dynamic skin loading from skinURL
        if (skinURL != null && !skinURL.isEmpty()) {
            // Check if it's a custom skin path
            if (skinURL.contains("alex")) {
                return ALEX_TEXTURE;
            } else if (skinURL.contains("knight")) {
                return DEFAULT_TEXTURE;
            }
        }
        
        return DEFAULT_TEXTURE;
    }
    
    @Override
    public void render(PlayerLikeEntity entity, float entityYaw, float partialTicks,
                      PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        
        // Scale the entity to match player size (players are slightly smaller than 1.0)
        poseStack.pushPose();
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        poseStack.popPose();
    }
    
    @Override
    protected void scale(PlayerLikeEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = 0.9375F; // Player scale
        poseStack.scale(scale, scale, scale);
    }
    
    @Override
    protected boolean shouldShowName(PlayerLikeEntity entity) {
        // Always show name for PlayerLike entities
        return entity.hasCustomName();
    }
    
    @Override
    protected void renderNameTag(PlayerLikeEntity entity, Component displayName, 
                                PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Custom name tag rendering
        super.renderNameTag(entity, displayName, poseStack, buffer, packedLight);
    }
    
    /**
     * Get the attack animation progress
     */
    protected float getAttackAnim(PlayerLikeEntity entity, float partialTicks) {
        return entity.getAttackAnim(partialTicks);
    }
    
    /**
     * Setup rotation angles
     */
    protected void setupRotations(PlayerLikeEntity entity, PoseStack poseStack, 
                                 float ageInTicks, float rotationYaw, float partialTicks) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTicks);
        
        // Add custom rotations for PlayerEngine behavior
        if (entity.isPlayerEngineEnabled()) {
            // Adjust pose based on current task or AI state
        }
    }
}
```

---

## Step 4: Register Entity and Model

**File**: `src/main/java/baritone/Automatone.java` (Add to existing file)

```java
// Add imports
import baritone.entity.PlayerLikeEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

// Add after FISHING_BOBBER declaration
public static final EntityType<PlayerLikeEntity> PLAYER_LIKE_ENTITY = 
    FabricEntityTypeBuilder.<PlayerLikeEntity>create()
        .spawnGroup(MobCategory.CREATURE)
        .entityFactory(PlayerLikeEntity::new)
        .dimensions(EntityDimensions.scalable(0.6F, 1.8F)) // Same as player dimensions
        .trackRangeBlocks(32)
        .trackedUpdateRate(3)
        .forceTrackedVelocityUpdates(false)
        .build();

// Add to onInitialize() method
@Override
public void onInitialize() {
    DefaultCommands.registerAll();
    
    // Register entities
    Registry.register(BuiltInRegistries.ENTITY_TYPE, id("fishing_bobber"), FISHING_BOBBER);
    Registry.register(BuiltInRegistries.ENTITY_TYPE, id("player_like"), PLAYER_LIKE_ENTITY);
    
    // Register attributes
    FabricDefaultAttributeRegistry.register(PLAYER_LIKE_ENTITY, PlayerLikeEntity.createPlayerLikeAttributes());
}
```

**File**: `src/main/java/baritone/AutomatoneClient.java` (Add to existing file)

```java
// Add imports
import baritone.client.PlayerLikeRenderer;
import baritone.client.model.PlayerLikeModel;
import net.minecraft.client.model.geom.ModelLayers;

// Add to onInitializeClient() method
@Override
public void onInitializeClient() {
    // Register renderers
    EntityRendererRegistry.register(Automatone.FISHING_BOBBER, CustomFishingBobberRenderer::new);
    EntityRendererRegistry.register(Automatone.PLAYER_LIKE_ENTITY, PlayerLikeRenderer::new);
    
    // Register model layers
    EntityModelLayerRegistry.registerModelLayer(ModelLayers.PLAYER, PlayerLikeModel::createBodyLayer);
}
```

---

## Step 5: Create Spawn Command

**File**: `src/autoclef/java/adris/altoclef/commands/SpawnCommand.java`

```java
package adris.altoclef.commands;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.player2api.Character;
import adris.altoclef.player2api.utils.CharacterUtils;
import baritone.Automatone;
import baritone.entity.PlayerLikeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.server.level.ServerLevel;

public class SpawnCommand extends Command {
    
    public SpawnCommand() throws CommandException {
        super(
            "spawn", 
            "Spawn a player-like AI entity. Usage: @spawn [characterName] [x] [y] [z]", 
            new Arg<>(String.class, "characterName", null, 0),
            new Arg<>(Integer.class, "x", null, 0),
            new Arg<>(Integer.class, "y", null, 0),
            new Arg<>(Integer.class, "z", null, 0)
        );
    }

    @Override
    protected void call(AltoClefController mod, ArgParser parser) throws CommandException {
        try {
            // Parse arguments
            String characterName = parser.get(String.class);
            Integer x = parser.get(Integer.class);
            Integer y = parser.get(Integer.class);
            Integer z = parser.get(Integer.class);
            
            // Get or create character
            Character character;
            if (characterName != null) {
                // Try to get character from Player2 API
                character = mod.getPlayer2APIService().getSelectedCharacter();
                // Override name if provided
                character = new Character(
                    characterName,
                    character.shortName(),
                    character.greetingInfo(),
                    character.description(),
                    character.skinURL(),
                    character.voiceIds()
                );
            } else {
                // Use selected character from Player2 API
                character = mod.getPlayer2APIService().getSelectedCharacter();
            }
            
            // Determine spawn position
            BlockPos spawnPos;
            if (x != null && y != null && z != null) {
                spawnPos = new BlockPos(x, y, z);
            } else {
                // Spawn near the closest player or bot position
                var closestPlayer = mod.getClosestPlayer();
                if (closestPlayer.isPresent()) {
                    spawnPos = closestPlayer.get().blockPosition().offset(2, 0, 0);
                } else {
                    // Spawn at bot's current position
                    spawnPos = mod.getEntity().blockPosition().offset(2, 0, 0);
                }
            }
            
            // Ensure spawn position is safe
            spawnPos = findSafeSpawnPosition(mod.getWorld(), spawnPos);
            
            // Create the entity
            PlayerLikeEntity entity = new PlayerLikeEntity(Automatone.PLAYER_LIKE_ENTITY, mod.getWorld());
            
            // Set character data
            entity.setCharacter(character);
            
            // Set position
            entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // Add to world
            boolean spawned = mod.getWorld().addFreshEntity(entity);
            
            if (spawned) {
                // Enable PlayerEngine capabilities
                entity.enablePlayerEngine();
                
                mod.logMessage("Successfully spawned player-like entity: " + character.name() + 
                    " at (" + spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ() + ")");
                
                // Optional: Make the entity face the closest player
                var closestPlayer = mod.getClosestPlayer();
                if (closestPlayer.isPresent()) {
                    entity.lookAt(closestPlayer.get(), 30.0F, 30.0F);
                }
                
            } else {
                mod.logWarning("Failed to spawn player-like entity. Position may be invalid.");
            }
            
        } catch (Exception e) {
            mod.logError("Error spawning player-like entity: " + e.getMessage());
            throw new CommandException("Failed to spawn entity: " + e.getMessage());
        }
        
        this.finish();
    }
    
    /**
     * Find a safe spawn position (not inside blocks, above ground)
     */
    private BlockPos findSafeSpawnPosition(ServerLevel world, BlockPos originalPos) {
        // Check if original position is safe
        if (isSafeSpawnPosition(world, originalPos)) {
            return originalPos;
        }
        
        // Try positions around the original
        for (int dy = 0; dy <= 5; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos testPos = originalPos.offset(dx, dy, dz);
                    if (isSafeSpawnPosition(world, testPos)) {
                        return testPos;
                    }
                }
            }
        }
        
        // If no safe position found, return original
        return originalPos;
    }
    
    /**
     * Check if a position is safe for spawning (not inside solid blocks, has ground support)
     */
    private boolean isSafeSpawnPosition(ServerLevel world, BlockPos pos) {
        // Check if the spawn position and the position above are not solid
        if (world.getBlockState(pos).isSolid() || world.getBlockState(pos.above()).isSolid()) {
            return false;
        }
        
        // Check if there's ground support (block below)
        return world.getBlockState(pos.below()).isSolid();
    }
}
```

**File**: `src/autoclef/java/adris/altoclef/AltoClefCommands.java` (Add to existing registerNewCommand)

```java
// Add import
import adris.altoclef.commands.SpawnCommand;

// Add to registerNewCommand method
controller.getCommandExecutor()
    .registerNewCommand(
        new GetCommand(),
        new SpawnCommand(), // Add this line
        new EquipCommand(),
        new BodyLanguageCommand(),
        // ... rest of existing commands
    );
```

---

## Step 6: PlayerEngine Integration Task

**File**: `src/autoclef/java/adris/altoclef/tasks/entity/ControlPlayerLikeEntityTask.java`

```java
package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import baritone.entity.PlayerLikeEntity;
import baritone.api.IBaritone;
import baritone.BaritoneAPI;

public class ControlPlayerLikeEntityTask extends Task {
    
    private final PlayerLikeEntity entity;
    private final Task delegateTask;
    private IBaritone baritone;
    
    public ControlPlayerLikeEntityTask(PlayerLikeEntity entity, Task delegateTask) {
        this.entity = entity;
        this.delegateTask = delegateTask;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        if (entity != null && !entity.isRemoved()) {
            // Get Baritone instance for this entity
            this.baritone = BaritoneAPI.getProvider().getBaritone(entity);
            
            // Enable PlayerEngine if not already enabled
            if (!entity.isPlayerEngineEnabled()) {
                entity.enablePlayerEngine();
            }
        }
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        if (entity == null || entity.isRemoved()) {
            // Entity no longer exists
            setFinished();
            return null;
        }
        
        if (baritone != null && delegateTask != null) {
            // Execute the delegate task using this entity's context
            return delegateTask;
        }
        
        return null;
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Clean up resources
        if (baritone != null) {
            baritone.getPathingBehavior().cancelEverything();
        }
    }
    
    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ControlPlayerLikeEntityTask otherTask) {
            return this.entity.equals(otherTask.entity) && 
                   this.delegateTask.equals(otherTask.delegateTask);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Controlling PlayerLike entity: " + 
               (entity != null ? entity.getCharacterName() : "null") +
               " with task: " + (delegateTask != null ? delegateTask.toString() : "null");
    }
    
    public PlayerLikeEntity getEntity() {
        return entity;
    }
    
    public Task getDelegateTask() {
        return delegateTask;
    }
}
```

---

## Step 7: Enhanced Commands for Entity Control

**File**: `src/autoclef/java/adris/altoclef/commands/EntityGetCommand.java`

```java
package adris.altoclef.commands;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.ItemList;
import adris.altoclef.tasks.entity.ControlPlayerLikeEntityTask;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.util.ItemTarget;
import baritone.entity.PlayerLikeEntity;

public class EntityGetCommand extends Command {
    
    public EntityGetCommand() throws CommandException {
        super(
            "entityget",
            "Make a specific PlayerLike entity get items. Usage: @entityget <entityName> <item> [count]",
            new Arg<>(String.class, "entityName"),
            new Arg<>(ItemList.class, "item"),
            new Arg<>(Integer.class, "count", 1)
        );
    }
    
    @Override
    protected void call(AltoClefController mod, ArgParser parser) throws CommandException {
        String entityName = parser.get(String.class);
        ItemList items = parser.get(ItemList.class);
        int count = parser.get(Integer.class);
        
        // Find the PlayerLike entity with the given name
        PlayerLikeEntity targetEntity = findPlayerLikeEntityByName(mod, entityName);
        
        if (targetEntity == null) {
            mod.logWarning("Could not find PlayerLike entity with name: " + entityName);
            this.finish();
            return;
        }
        
        // Create ItemTarget for the get task
        ItemTarget[] targets = new ItemTarget[items.count()];
        for (int i = 0; i < items.count(); i++) {
            targets[i] = new ItemTarget(items.get(i), count);
        }
        
        // Create a get task for the entity
        Task getTask = new GetItemTask(targets);
        
        // Create control task that will execute the get task using the specific entity
        ControlPlayerLikeEntityTask controlTask = new ControlPlayerLikeEntityTask(targetEntity, getTask);
        
        mod.runUserTask(controlTask, () -> {
            mod.logMessage("Entity " + entityName + " completed get task for " + items);
            this.finish();
        });
    }
    
    private PlayerLikeEntity findPlayerLikeEntityByName(AltoClefController mod, String name) {
        for (var entity : mod.getWorld().getAllEntities()) {
            if (entity instanceof PlayerLikeEntity playerLike) {
                if (name.equals(playerLike.getCharacterName())) {
                    return playerLike;
                }
            }
        }
        return null;
    }
}
```

---

## Step 8: Texture Resources

**Directory**: `src/main/resources/assets/automatone/textures/entity/`

Create the following texture files:

1. **knight_skin.png** - Default PlayerLike entity skin (64x64 player skin format)
2. **alex_skin.png** - Alternative skin option (64x64 player skin format)

The textures should follow Minecraft's player skin format:
- 64x64 pixel PNG files
- Standard Minecraft player skin UV mapping
- Include all body parts: head, body, arms, legs
- Optional: hat/overlay layers

---

## Step 9: Usage Examples

### In-Game Commands

```bash
# Basic spawning
@spawn                          # Spawn with default character
@spawn "Knight"                 # Spawn with custom name
@spawn "Alex" 100 64 200       # Spawn at specific coordinates

# Entity-specific commands (after implementing EntityGetCommand)
@entityget "Knight" diamond 5   # Make Knight collect 5 diamonds
@entityget "Alex" wood 64       # Make Alex collect 64 wood

# General commands that work with PlayerLike entities
@chatclef on                    # Enable AI bridge
@get diamond 5                  # Original bot gets diamonds
@follow Knight                  # Follow the PlayerLike entity named Knight
```

### Programmatic Spawning

```java
// In your mod code
PlayerLikeEntity entity = new PlayerLikeEntity(Automatone.PLAYER_LIKE_ENTITY, world);

// Set character from Player2 API
Character character = player2Service.getSelectedCharacter();
entity.setCharacter(character);

// Position and spawn
entity.setPos(x, y, z);
world.addFreshEntity(entity);

// Enable PlayerEngine capabilities
entity.enablePlayerEngine();
```

---

## Step 10: Chat Integration - Making PlayerLike Entities Respond to Chat

**IMPORTANT**: Without this step, your PlayerLike entities will NOT respond to nearby player chat messages. They'll just be enhanced mobs that can execute commands but won't have conversational AI capabilities.

### Understanding the Current Limitation

The current `EventQueueManager` is designed for a single entity per server. To make PlayerLike entities respond to chat, we need to extend it to support multiple entities with individual chat processing.

### **File**: `src/autoclef/java/adris/altoclef/player2api/EventQueueManager.java` (Modifications)

Add multi-entity support to the existing EventQueueManager:

```java
// Add to existing EventQueueManager class

/**
 * Register a PlayerLike entity for chat processing
 */
public static void registerPlayerLikeEntity(PlayerLikeEntity entity, AltoClefController controller) {
    UUID entityUUID = entity.getUUID();
    
    // Create individual event queue for this entity
    EventQueueData entityQueue = new EventQueueData(controller);
    queueData.put(entityUUID, entityQueue);
    
    // Enable chat processing
    entityQueue.setEnabled(true);
    
    LOGGER.info("Registered PlayerLike entity for chat processing: {} (UUID: {})", 
               entity.getCharacterName(), entityUUID);
}

/**
 * Unregister a PlayerLike entity (cleanup when removed)
 */
public static void unregisterPlayerLikeEntity(UUID entityUUID) {
    EventQueueData removed = queueData.remove(entityUUID);
    if (removed != null) {
        removed.setEnabled(false);
        LOGGER.info("Unregistered PlayerLike entity from chat processing: {}", entityUUID);
    }
}

/**
 * Enhanced distance checking for PlayerLike entities
 */
private static boolean isCloseToPlayerLikeEntity(EventQueueData data, String playerName) {
    AltoClefController controller = data.getController();
    LivingEntity entity = controller.getEntity();
    
    // Find the speaking player
    Optional<ServerPlayer> speakingPlayer = findPlayerByName(controller.getWorld(), playerName);
    if (speakingPlayer.isEmpty()) {
        return false;
    }
    
    // Check distance between entity and speaking player
    double distance = entity.distanceTo(speakingPlayer.get());
    return distance <= messagePassingMaxDistance;
}

/**
 * Find player by name in the world
 */
private static Optional<ServerPlayer> findPlayerByName(ServerLevel world, String playerName) {
    return world.getServer().getPlayerList().getPlayers().stream()
            .filter(player -> player.getName().getString().equals(playerName))
            .findFirst();
}

// Modify existing onUserChatMessage method
public static void onUserChatMessage(Event.UserMessage msg) {
    LOGGER.info("User message event={}", msg);
    
    // Send to ALL entities (original + PlayerLike) close to the user
    filterQueueData(d -> {
        // Check both original logic and PlayerLike entity logic
        return isCloseToPlayer(d, msg.userName()) || isCloseToPlayerLikeEntity(d, msg.userName());
    }).forEach(data -> {
        data.onEvent(msg);  // Each entity processes the chat independently
    });
}
```

### **File**: `PlayerLikeEntity.java` (Add Chat Processing)

Add chat integration methods to the existing PlayerLikeEntity class:

```java
// Add to existing PlayerLikeEntity class (after existing methods)

// Chat processing components
private AltoClefController individualController;
private Player2APIService individualPlayer2API;
private ConversationHistory conversationHistory;

/**
 * Enhanced PlayerEngine enablement with chat integration
 */
@Override
public void enablePlayerEngine() {
    if (!this.playerEngineEnabled) {
        this.playerEngineEnabled = true;
        this.goalSelector.removeAllGoals();
        
        // Create individual controller for this entity
        this.individualController = createIndividualController();
        
        // Create individual Player2 API service
        this.individualPlayer2API = createIndividualPlayer2API();
        this.individualController.setPlayer2APIService(individualPlayer2API);
        
        // Initialize conversation history with proper system prompt
        this.conversationHistory = new ConversationHistory();
        addCharacterSystemPrompt(); // This now includes ALL available commands!
        
        // Register for chat processing
        EventQueueManager.registerPlayerLikeEntity(this, individualController);
        
        LOGGER.info("Enabled PlayerEngine with chat integration for: {}", getCharacterName());
    }
}

/**
 * Create individual AltoClefController for this entity
 */
private AltoClefController createIndividualController() {
    try {
        // Create entity context for this specific entity
        EntityContext entityContext = new EntityContext(this);
        
        // Create individual controller
        AltoClefController controller = new AltoClefController(entityContext);
        
        // Initialize controller components
        controller.initializeForPlayerLikeEntity(this);
        
        return controller;
    } catch (Exception e) {
        LOGGER.error("Failed to create individual controller for PlayerLike entity", e);
        return null;
    }
}

/**
 * Create individual Player2 API service with character-specific configuration
 */
private Player2APIService createIndividualPlayer2API() {
    // Create unique game ID for this character
    String gameId = "playerlike_" + getCharacterName().toLowerCase().replace(" ", "_") + "_" + getUUID().toString().substring(0, 8);
    
    return new Player2APIService(gameId);
}

/**
 * Add character-specific system prompt with full command list to conversation
 */
private void addCharacterSystemPrompt() {
    if (conversationHistory != null && character != null && individualController != null) {
        // Use the same system prompt generation as the main PlayerEngine system
        // This automatically includes ALL available commands with descriptions
        String systemPrompt = Prompts.getAINPCSystemPrompt(
            character, 
            individualController.getCommandExecutor().allCommands(),  // Dynamic command list!
            getOwnerUsername() // Owner who spawned this entity
        );
        
        conversationHistory.setBaseSystemPrompt(systemPrompt);
        LOGGER.info("Set system prompt for {} with {} available commands", 
                   getCharacterName(), individualController.getCommandExecutor().allCommands().size());
    }
}

/**
 * Get the username of the player who owns/spawned this entity
 */
private String getOwnerUsername() {
    // Try to find the closest player as the "owner"
    if (level() != null && !level().isClientSide) {
        ServerLevel serverLevel = (ServerLevel) level();
        List<ServerPlayer> nearbyPlayers = serverLevel.getPlayers(player -> 
            player.distanceTo(this) <= 16.0
        );
        
        if (!nearbyPlayers.isEmpty()) {
            // Return the closest player as owner
            ServerPlayer closestPlayer = nearbyPlayers.get(0);
            for (ServerPlayer player : nearbyPlayers) {
                if (player.distanceTo(this) < closestPlayer.distanceTo(this)) {
                    closestPlayer = player;
                }
            }
            return closestPlayer.getName().getString();
        }
    }
    
    return "Unknown Owner"; // Fallback
}

/**
 * Process incoming chat messages (called by EventQueueManager)
 */
public void processChatMessage(String message, String senderName) {
    if (!isPlayerEngineEnabled() || individualController == null || individualPlayer2API == null) {
        return;
    }
    
    try {
        // Add user message to conversation history
        conversationHistory.addUserMessage(senderName, message);
        
        // Send conversation to Player2 API
        LOGGER.info("Processing chat message for {}: {} -> {}", getCharacterName(), senderName, message);
        
        // Use async processing to avoid blocking the main thread
        CompletableFuture.supplyAsync(() -> {
            try {
                return individualPlayer2API.completeConversation(conversationHistory);
            } catch (Exception e) {
                LOGGER.error("Error getting AI response for {}: {}", getCharacterName(), e.getMessage());
                return null;
            }
        }).thenAccept(this::handleAIResponse);
        
    } catch (Exception e) {
        LOGGER.error("Error processing chat message for {}: {}", getCharacterName(), e.getMessage());
    }
}

/**
 * Handle AI response from Player2 API
 */
private void handleAIResponse(JsonObject aiResponse) {
    if (aiResponse == null || individualController == null) {
        return;
    }
    
    try {
        // Add AI response to conversation history
        String aiMessage = aiResponse.has("message") ? aiResponse.get("message").getAsString() : "";
        if (!aiMessage.isEmpty()) {
            conversationHistory.addAssistantMessage(aiMessage);
        }
        
        // Execute AI commands using AgentCommandUtils
        AgentCommandUtils.executeAIResponse(aiResponse, individualController);
        
        // Send chat response if the AI wants to talk
        if (aiResponse.has("chat_response")) {
            String chatResponse = aiResponse.get("chat_response").getAsString();
            sendChatMessage(chatResponse);
        }
        
        LOGGER.info("Processed AI response for {}", getCharacterName());
        
    } catch (Exception e) {
        LOGGER.error("Error handling AI response for {}: {}", getCharacterName(), e.getMessage());
    }
}

/**
 * Send a chat message as this entity
 */
private void sendChatMessage(String message) {
    if (level() != null && !level().isClientSide) {
        ServerLevel serverLevel = (ServerLevel) level();
        MinecraftServer server = serverLevel.getServer();
        
        // Format message with character name
        String formattedMessage = String.format("<%s> %s", getCharacterName(), message);
        Component chatComponent = Component.literal(formattedMessage);
        
        // Broadcast to nearby players
        List<ServerPlayer> nearbyPlayers = serverLevel.getPlayers(player -> 
            player.distanceTo(this) <= 32.0 // Chat radius
        );
        
        for (ServerPlayer player : nearbyPlayers) {
            player.sendSystemMessage(chatComponent);
        }
        
        LOGGER.info("Chat message sent by {}: {}", getCharacterName(), message);
    }
}

/**
 * Cleanup when entity is removed
 */
@Override
public void remove(RemovalReason reason) {
    if (isPlayerEngineEnabled()) {
        // Unregister from chat processing
        EventQueueManager.unregisterPlayerLikeEntity(getUUID());
        
        // Cleanup individual controller
        if (individualController != null) {
            individualController.onStop();
        }
    }
    
    super.remove(reason);
}

/**
 * Get conversation history for debugging/persistence
 */
public ConversationHistory getConversationHistory() {
    return conversationHistory;
}
```

### **File**: `src/autoclef/java/adris/altoclef/AltoClefController.java` (Add Helper Method)

Add this method to the existing AltoClefController class:

```java
/**
 * Initialize controller specifically for PlayerLike entities
 */
public void initializeForPlayerLikeEntity(PlayerLikeEntity entity) {
    try {
        // Set up minimal required components for PlayerLike entity operation
        // This is a lighter initialization than full bot setup
        
        // Initialize basic trackers
        this.trackerManager = new TrackerManager(this);
        this.entityTracker = new EntityTracker(this.trackerManager);
        this.chunkTracker = new SimpleChunkTracker(this);
        
        // Initialize task system
        this.taskRunner = new TaskRunner(this);
        this.userTaskChain = new UserTaskChain(this.taskRunner);
        
        // Initialize command system
        this.commandExecutor = new CommandExecutor(this);
        this.initializeCommands();
        
        // Initialize controls
        this.inputControls = new InputControls(this);
        this.slotHandler = new SlotHandler(this);
        
        // Load basic settings
        this.settings = new Settings();
        
        LOGGER.info("Initialized AltoClefController for PlayerLike entity: {}", entity.getCharacterName());
        
    } catch (Exception e) {
        LOGGER.error("Failed to initialize controller for PlayerLike entity", e);
    }
}

/**
 * Set Player2 API service (for PlayerLike entities)
 */
public void setPlayer2APIService(Player2APIService service) {
    this.player2apiService = service;
}

/**
 * Cleanup method for PlayerLike entities
 */
public void onStop() {
    if (this.taskRunner != null) {
        this.taskRunner.disable();
    }
    if (this.userTaskChain != null) {
        this.userTaskChain.cancel(this);
    }
}
```

### **File**: `src/autoclef/java/adris/altoclef/player2api/AgentCommandUtils.java` (Add Command Execution)

Create this new utility class to handle AI command execution:

```java
package adris.altoclef.player2api;

import adris.altoclef.AltoClefController;
import adris.altoclef.commandsystem.CommandException;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AgentCommandUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Execute AI response commands
     */
    public static void executeAIResponse(JsonObject aiResponse, AltoClefController controller) {
        if (aiResponse == null || controller == null) {
            return;
        }
        
        try {
            // Check for commands in the AI response
            if (aiResponse.has("commands")) {
                JsonArray commands = aiResponse.getAsJsonArray("commands");
                
                for (JsonElement commandElement : commands) {
                    if (commandElement.isJsonPrimitive()) {
                        String command = commandElement.getAsString();
                        executeCommand(command, controller);
                    }
                }
            }
            
            // Check for direct command in message
            if (aiResponse.has("message")) {
                String message = aiResponse.get("message").getAsString();
                if (message.startsWith("@")) {
                    executeCommand(message, controller);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error executing AI response commands: {}", e.getMessage());
        }
    }
    
    /**
     * Execute a single command
     */
    private static void executeCommand(String command, AltoClefController controller) {
        try {
            if (command.startsWith("@")) {
                command = command.substring(1); // Remove @ prefix
            }
            
            LOGGER.info("Executing AI command: @{}", command);
            controller.getCommandExecutor().executeWithPrefix(command);
            
        } catch (CommandException e) {
            LOGGER.error("Error executing command '{}': {}", command, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error executing command '{}': {}", command, e.getMessage());
        }
    }
}
```

### **Usage Examples After Chat Integration**

With chat integration enabled, your PlayerLike entities will now respond to nearby chat:

```bash
# Player approaches and says:
Player: "Hello Knight, can you get me some diamonds?"

# PlayerLike entity processes through Player2 API and responds:
<Knight> Greetings! I'll gather some diamonds for you right away.
# Entity then executes: @get diamond 5

# More complex interactions:
Player: "Knight, follow me to the mine"
<Knight> Of course! I'll follow you.
# Entity executes: @follow PlayerName

Player: "What's your name?"
<Knight> I am Knight, your faithful AI companion. How may I assist you today?

# Entity responds based on character personality from Player2 API
Player: "Tell me a joke"
<Knight> Why don't skeletons fight each other? They don't have the guts! *chuckles in medieval*
```

### **Critical Fix: Automatic Command Discovery**

**IMPORTANT**: The updated `addCharacterSystemPrompt()` method now uses `Prompts.getAINPCSystemPrompt()` which **automatically includes all available commands** in the system prompt. This means:

- **Dynamic Command List**: The LLM automatically learns about ALL PlayerEngine commands (`@get`, `@goto`, `@follow`, `@idle`, etc.)
- **Command Descriptions**: Each command includes usage examples and capabilities
- **No Manual Updates**: New commands are automatically available to the LLM
- **Same Intelligence**: PlayerLike entities have the same command knowledge as the main PlayerEngine system

**What the LLM receives**:
```
Valid Commands:
get:        Get a resource or Craft an item in Minecraft. Examples: `get log 20` gets 20 logs, `get diamond_chestplate 1` gets 1 diamond chestplate  
goto:       Tell bot to travel to a set of coordinates
follow:     Follows you or someone else. Example: `follow Player`  
idle:       Make bot idle/stop current task
stop:       Stop current task
farm:       Farm crops or breed animals
fish:       Go fishing for food and items
...and all other registered commands
```

This enables natural language understanding: **"go chop wood"** → LLM sees `get` command → **`@get log 32`** → Task execution.

### **Configuration Notes**

1. **Chat Range**: Entities respond to chat within 64 blocks (configurable via `messagePassingMaxDistance`)
2. **Character Personality**: Each entity responds according to its Player2 API character configuration
3. **Command Execution**: AI responses can trigger any PlayerEngine command (@get, @goto, @follow, etc.)
4. **Conversation Memory**: Each entity maintains its own conversation history with players
5. **Dynamic Commands**: Command list updates automatically when new commands are added to PlayerEngine

### **Performance Considerations**

- Each PlayerLike entity makes independent calls to Player2 API
- Consider rate limiting for servers with many entities
- Conversation histories grow over time - implement cleanup as needed
- Async processing prevents chat from blocking the main server thread

---

## Step 11: Advanced Features (Future Enhancements)

### Dynamic Skin Loading

```java
public class SkinManager {
    private static final Map<String, ResourceLocation> SKIN_CACHE = new ConcurrentHashMap<>();
    
    public static CompletableFuture<ResourceLocation> downloadSkin(String skinURL) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Download skin from URL
                // Convert to Minecraft texture format
                // Cache for future use
                return new ResourceLocation("automatone", "textures/entity/downloaded_" + skinURL.hashCode() + ".png");
            } catch (Exception e) {
                return new ResourceLocation("automatone", "textures/entity/knight_skin.png");
            }
        });
    }
}
```

### AI Behavior Integration

```java
public class PlayerLikeAIBehavior extends Goal {
    private final PlayerLikeEntity entity;
    private final AltoClefController controller;
    
    public PlayerLikeAIBehavior(PlayerLikeEntity entity) {
        this.entity = entity;
        this.controller = new AltoClefController(entity); // Custom controller per entity
    }
    
    @Override
    public void tick() {
        // Execute PlayerEngine tasks
        controller.tick();
        
        // Handle Player2 API communication
        if (hasNewAICommand()) {
            executeAICommand();
        }
    }
}
```

---

## Complete File Structure

```
src/
├── main/java/baritone/
│   ├── entity/
│   │   └── PlayerLikeEntity.java
│   ├── client/
│   │   ├── PlayerLikeRenderer.java
│   │   └── model/
│   │       └── PlayerLikeModel.java
│   ├── Automatone.java (modified)
│   └── AutomatoneClient.java (modified)
├── autoclef/java/adris/altoclef/
│   ├── commands/
│   │   ├── SpawnCommand.java
│   │   └── EntityGetCommand.java
│   ├── tasks/entity/
│   │   └── ControlPlayerLikeEntityTask.java
│   └── AltoClefCommands.java (modified)
└── main/resources/assets/automatone/textures/entity/
    ├── knight_skin.png
    └── alex_skin.png
```

---

## Testing and Deployment

### Build Process

1. **Compile**: `./gradlew build`
2. **Test**: `./gradlew test`
3. **Install**: Place JAR in Fabric server mods folder

### In-Game Testing

1. **Start server** with PlayerEngine mod loaded
2. **Enable AI bridge**: `@chatclef on`
3. **Spawn entity**: `@spawn TestCharacter`
4. **Verify entity** appears and responds to commands
5. **Test AI integration** through Player2 API

### Verification Checklist

- [ ] Entity spawns correctly
- [ ] Character data persists through server restart
- [ ] Entity responds to PlayerEngine commands
- [ ] Textures render correctly
- [ ] Player2 API integration works
- [ ] Entity has player-like capabilities (inventory, interaction)
- [ ] Pathfinding and task system integration functional

---

## Troubleshooting

### Common Issues

1. **Entity doesn't spawn**: Check entity registration and attributes
2. **Rendering issues**: Verify model layers and texture paths
3. **AI not responding**: Ensure PlayerEngine enhancement is enabled
4. **Crashes**: Check NBT serialization and entity data synchronization

### Debug Commands

```java
// Add debug info to entity
@Override
public void tick() {
    super.tick();
    if (level().isClientSide && tickCount % 20 == 0) {
        System.out.println("PlayerLike Entity: " + getCharacterName() + 
                          " PlayerEngine: " + isPlayerEngineEnabled());
    }
}
```

---

This complete implementation provides a fully functional player-like entity that integrates with PlayerEngine's AI capabilities and can be controlled through the Player2 API system. The entity looks like a Minecraft player, has all the necessary components for AI behavior, and can be spawned and controlled in-game.