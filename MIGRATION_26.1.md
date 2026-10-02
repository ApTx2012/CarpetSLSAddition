# Yarn → Mojang official 映射表（1.21 → 26.1.2）

> 26.1.2 起 Fabric 弃用 Yarn，改用 Mojang 官方映射（jar 未混淆）。
> 本表记录本 mod 迁移用到的类名对照。⚠️ 表示结构/语义有变化，需人工核对。

## block
- block.BlockState → world.level.block.state.BlockState
- block.Blocks → world.level.block.Blocks
- block.MushroomPlantBlock → world.level.block.MushroomBlock
- block.RedstoneWireBlock → world.level.block.RedStoneWireBlock
- block.ShulkerBoxBlock → world.level.block.ShulkerBoxBlock
- block.entity.BlockEntity → world.level.block.entity.BlockEntity
- block.entity.EndGatewayBlockEntity → world.level.block.entity.TheEndGatewayBlockEntity
- block.entity.EndPortalBlockEntity → world.level.block.entity.TheEndPortalBlockEntity
- block.enums.TrialSpawnerState → world.level.block.entity.trialspawner.TrialSpawnerState
- block.pattern.BlockPattern → world.level.block.state.pattern.BlockPattern
- block.pattern.CachedBlockPosition → world.level.block.state.pattern.BlockPattern$BlockCacheLoader ⚠️
- block.spawner.TrialSpawnerConfig → world.level.block.entity.trialspawner.TrialSpawnerConfig
- block.spawner.TrialSpawnerData → world.level.block.entity.trialspawner.TrialSpawnerStateData ⚠️
- block.spawner.TrialSpawnerLogic → world.level.block.entity.trialspawner.TrialSpawner ⚠️

## command
- command.CommandRegistryAccess → commands.CommandBuildContext ⚠️
- command.argument.* → commands.arguments.*
- server.command.ServerCommandSource → commands.CommandSourceStack
- server.command.CommandManager → commands.Commands
- command.CommandSource.suggestMatching → commands.SharedSuggestionProvider 方法 ⚠️

## component / nbt / text / util
- component.DataComponentTypes → core.component.DataComponentTypes
- nbt.NbtCompound → nbt.CompoundTag
- nbt.NbtElement → nbt.Tag
- text.Text → network.chat.Component
- text.Style → network.chat.Style
- util.Formatting → ChatFormatting
- util.math.BlockPos → core.BlockPos
- util.math.Direction → core.Direction
- util.math.MathHelper → util.Mth
- util.math.Vec2f → world.phys.Vec2
- util.math.Vec3d → world.phys.Vec3
- util.UserCache → server.players.GameProfileCache ⚠️
- util.Uuids → core.UUIDUtil
- util.Nameable → world.Nameable
- registry.RegistryKey → resources.ResourceKey
- registry.tag.TagKey → tags.TagKey
- registry.tag.BlockTags → tags.BlockTags
- SharedConstants → SharedConstants

## entity
- entity.Entity → world.entity.Entity
- entity.EntityType → world.entity.EntityType
- entity.EquipmentSlot → world.entity.EquipmentSlot
- entity.FallingBlockEntity → world.entity.item.FallingBlockEntity
- entity.ItemEntity → world.entity.item.ItemEntity
- entity.Leashable → world.entity.Leashable
- entity.LivingEntity → world.entity.LivingEntity
- entity.attribute.EntityAttributes → world.entity.ai.attributes.Attributes ⚠️
- entity.boss.dragon.EnderDragonFight → world.level.dimension.end.EnderDragonFight
- entity.damage.DamageSource → world.damagesource.DamageSource
- entity.decoration.ArmorStandEntity → world.entity.decoration.ArmorStand
- entity.decoration.EndCrystalEntity → world.entity.EndCrystal ⚠️
- entity.mob.EndermanEntity → world.entity.monster.EnderMan
- entity.mob.MobEntity → world.entity.Mob
- entity.passive.AnimalEntity → world.entity.animal.Animal
- entity.passive.ArmadilloEntity → world.entity.animal.Armadillo ⚠️
- entity.player.PlayerAbilities → world.entity.player.Abilities
- entity.player.PlayerEntity → world.entity.player.Player
- entity.player.PlayerInventory → world.entity.player.Inventory
- entity.vehicle.BoatEntity → world.entity.vehicle.Boat ⚠️

## item / inventory / screen
- inventory.Inventory → world.entity.player.Inventory
- item.BlockItem → world.item.BlockItem
- item.ElytraItem → world.item.ElytraItem ⚠️
- item.Equipment → world.item.Equipment ⚠️
- item.Item → world.item.Item
- item.ItemStack → world.item.ItemStack
- item.Items → world.item.Items
- screen.AnvilScreenHandler → world.inventory.AnvilMenu
- screen.ScreenHandler → world.inventory.AbstractContainerMenu
- screen.slot.Slot → world.inventory.Slot

## network
- network.NetworkSide → network.protocol.PacketFlow
- network.packet.c2s.common.SyncedClientOptions → server.level.ClientInformation
- network.packet.s2c.play.EntityPositionS2CPacket → network.protocol.game.ClientboundMoveEntityPacket
- network.packet.s2c.play.EntitySetHeadYawS2CPacket → network.protocol.game.ClientboundRotateHeadPacket
- network.packet.s2c.play.EntityTrackerUpdateS2CPacket → network.protocol.game.ClientboundSetEntityDataPacket
- network.packet.s2c.play.PlayerListS2CPacket → network.protocol.game.ClientboundPlayerInfoUpdatePacket
- server.network.ConnectedClientData → server.network.CommonListenerCookie ⚠️
- server.network.ServerPlayNetworkHandler → server.network.ServerGamePacketListenerImpl
- server.network.ServerPlayerEntity → server.level.ServerPlayer

## server / world
- server.MinecraftServer → server.MinecraftServer
- server.PlayerManager → server.players.PlayerList
- server.world.ServerWorld → server.level.ServerLevel
- server.world.SleepManager → server.players.SleepStatus ⚠️
- world.World → world.level.Level
- world.WorldView → world.level.LevelReader
- world.GameMode → world.level.GameType
- world.SpawnHelper → world.level.NaturalSpawner
- world.chunk.WorldChunk → world.level.chunk.LevelChunk
- world.Heightmap → world.level.levelgen.Heightmap
- world.dimension.NetherPortal → world.level.block.NetherPortalBlock ⚠️
- world.gen.feature.EndPortalFeature → world.level.levelgen.feature.EndPodiumFeature
- sound.SoundCategory → sounds.SoundSource
- sound.SoundEvents → sounds.SoundEvents
- resource.ResourcePackProfile → server.packs.* ⚠️
- resource.ResourceType → server.packs.PackType
- resource.VanillaResourcePackProvider → server.packs.vanilla.VanillaPackResources ⚠️