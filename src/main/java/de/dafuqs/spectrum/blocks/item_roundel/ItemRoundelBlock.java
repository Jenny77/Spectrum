package de.dafuqs.spectrum.blocks.item_roundel;

import de.dafuqs.spectrum.helpers.InventoryHelper;
import de.dafuqs.spectrum.registries.SpectrumBlockEntities;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ItemRoundelBlock extends BlockWithEntity {
	
	protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);
	
	public ItemRoundelBlock(Settings settings) {
		super(settings);
	}
	
	public static void scatterContents(World world, BlockPos pos) {
		Block block = world.getBlockState(pos).getBlock();
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof ItemRoundelBlockEntity itemBowlBlockEntity) {
			ItemScatterer.spawn(world, pos, itemBowlBlockEntity.getInventory());
			world.updateComparators(pos, block);
		}
	}
	
	@Override
	public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
		if (!world.isClient && entity instanceof ItemEntity itemEntity) {
			ItemStack remainingStack = inputItem(world, pos, itemEntity.getStack());
			if (remainingStack.isEmpty()) {
				itemEntity.remove(Entity.RemovalReason.DISCARDED);
			} else {
				itemEntity.setStack(remainingStack);
			}
		} else {
			super.onLandedUpon(world, state, pos, entity, fallDistance);
		}
	}
	
	public ItemStack inputItem(World world, BlockPos pos, ItemStack itemStack) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof ItemRoundelBlockEntity itemRoundelBlockEntity) {
			int previousCount = itemStack.getCount();
			ItemStack remainingStack = InventoryHelper.smartAddToInventory(itemStack, itemRoundelBlockEntity.getInventory(), null);
			
			if (remainingStack.getCount() != previousCount) {
				itemRoundelBlockEntity.markDirty();
				itemRoundelBlockEntity.updateInClientWorld();
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.8F, 0.8F + world.random.nextFloat() * 0.6F);
			}
			return remainingStack;
		}
		return itemStack;
	}
	
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ItemRoundelBlockEntity(pos, state);
	}
	
	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		if (world.isClient) {
			return checkType(type, SpectrumBlockEntities.ITEM_ROUNDEL, ItemRoundelBlockEntity::clientTick);
		} else {
			return null;
		}
	}
	
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}
	
	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}
	
	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		scatterContents(world, pos);
		super.onStateReplaced(state, world, pos, newState, moved);
	}
	
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) {
			return ActionResult.SUCCESS;
		} else {
			ItemStack handStack = player.getStackInHand(hand);
			
			// if the structure is valid the player can put / retrieve blocks into the shrine
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof ItemRoundelBlockEntity itemRoundelBlockEntity) {
				boolean itemsChanged = false;
				if (player.isSneaking()) {
					Inventory inventory = itemRoundelBlockEntity.getInventory();
					ItemStack retrievedStack = inventory.removeStack(0);
					if (!retrievedStack.isEmpty()) {
						player.giveItemStack(retrievedStack);
						itemsChanged = true;
					}
				} else {
					Inventory inventory = itemRoundelBlockEntity.getInventory();
					ItemStack currentStack = inventory.getStack(0);
					if (!handStack.isEmpty() && !currentStack.isEmpty()) {
						inventory.setStack(0, handStack);
						player.setStackInHand(hand, currentStack);
						itemsChanged = true;
					} else {
						if (!handStack.isEmpty()) {
							ItemStack remainingStack = InventoryHelper.smartAddToInventory(handStack, itemRoundelBlockEntity.getInventory(), null);
							player.setStackInHand(hand, remainingStack);
							itemsChanged = true;
						}
						if (!currentStack.isEmpty()) {
							player.giveItemStack(currentStack);
							itemsChanged = true;
						}
					}
				}
				
				if (itemsChanged) {
					itemRoundelBlockEntity.markDirty();
					itemRoundelBlockEntity.updateInClientWorld();
					world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.8F, 0.8F + world.random.nextFloat() * 0.6F);
				}
			}
			return ActionResult.CONSUME;
		}
	}
	
}