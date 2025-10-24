package ru.zenith.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.zenith.api.event.EventManager;
import ru.zenith.implement.events.block.BlockCollisionEvent;

@Mixin(BlockCollisionSpliterator.class)
public abstract class BlockCollisionSpliteratorMixin {
    @Redirect(method = "computeNext", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/BlockView;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private BlockState computeNext(BlockView instance, BlockPos blockPos) {
        BlockCollisionEvent event = new BlockCollisionEvent(blockPos, instance.getBlockState(blockPos));
        EventManager.callEvent(event);
        return event.state();
    }
}