package ru.zenith.implement.features.modules.player.scaffold;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.ItemStack;

import java.util.Set;

public class ScaffoldBlockItemSelection {

    private static ScaffoldBlockItemSelection instance;

    public static ScaffoldBlockItemSelection getInstance() {
        if (instance == null) {
            instance = new ScaffoldBlockItemSelection();
        }
        return instance;
    }

    private final Set<Block> disallowedBlocks = Set.of(
            Blocks.TNT,
            Blocks.COBWEB,
            Blocks.NETHER_PORTAL
    );

    private final Set<Block> unfavorableBlocks = Set.of(
            Blocks.CRAFTING_TABLE,
            Blocks.JIGSAW,
            Blocks.SMITHING_TABLE,
            Blocks.FLETCHING_TABLE,
            Blocks.ENCHANTING_TABLE,
            Blocks.CAULDRON,
            Blocks.MAGMA_BLOCK
    );

    public boolean isValidBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (!(stack.getItem() instanceof net.minecraft.item.BlockItem)) {
            return false;
        }

        net.minecraft.item.BlockItem blockItem = (net.minecraft.item.BlockItem) stack.getItem();
        Block block = blockItem.getBlock();

        if (block == null) return false;

        // Don't use falling blocks
        if (block instanceof FallingBlock) {
            return false;
        }

        // Check disallowed blocks
        return !disallowedBlocks.contains(block);
    }

    public boolean isBlockUnfavorable(ItemStack stack) {
        if (!(stack.getItem() instanceof net.minecraft.item.BlockItem)) {
            return true;
        }

        net.minecraft.item.BlockItem blockItem = (net.minecraft.item.BlockItem) stack.getItem();
        Block block = blockItem.getBlock();
        if (block == null) return true;

        // Check various unfavorable conditions
        if (block.getSlipperiness() > 0.6F) return true;
        if (block.getVelocityMultiplier() < 1.0F) return true;
        if (block.getJumpVelocityMultiplier() < 1.0F) return true;
        if (block instanceof BlockWithEntity) return true;

        return unfavorableBlocks.contains(block);
    }
}