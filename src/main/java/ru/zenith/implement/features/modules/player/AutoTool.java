package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.block.BlockBreakingEvent;
import ru.zenith.implement.events.keyboard.HotBarScrollEvent;
import ru.zenith.implement.events.player.HotBarUpdateEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.ItemRendererEvent;

import java.util.Comparator;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTool extends Module {
    private final StopWatch swap = new StopWatch(), breaking = new StopWatch();
    private final Script script = new Script(), swapBackScript = new Script();
    private ItemStack renderStack;
    private BlockPos lastBreakPos;

    public AutoTool() {
        super("AutoTool", "Auto Tool", ModuleCategory.PLAYER);
    }

    @EventHandler
    public void onItemRenderer(ItemRendererEvent e) {
        if (renderStack != null && e.getHand().equals(Hand.MAIN_HAND) && Objects.equals(mc.player, e.getPlayer())) {
            e.setStack(renderStack);
        }
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (!swapBackScript.isFinished()) e.cancel();
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (!swapBackScript.isFinished()) e.cancel();
    }

    @EventHandler
    public void onBlockBreaking(BlockBreakingEvent e) {
        breaking.reset();
        lastBreakPos = e.blockPos();
        if (!mc.player.isCreative() && swapBackScript.isFinished() && swap.finished(350)) {
            Slot currentBestSlot = findBestTool(lastBreakPos);
            if (currentBestSlot != null && currentBestSlot != PlayerInventoryUtil.mainHandSlot()) {
                renderStack = mc.player.getMainHandStack();
                PlayerInventoryUtil.swapHand(currentBestSlot, Hand.MAIN_HAND, true);
                swapBackScript.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(currentBestSlot, Hand.MAIN_HAND, true, true));
                swap.reset();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
        if (!swapBackScript.isFinished() && swap.finished(350)) {
            Slot currentBestSlot = findBestTool(lastBreakPos);
            if (currentBestSlot != PlayerInventoryUtil.mainHandSlot() || breaking.finished(100)) {
                script.cleanup().addTickStep(4, () -> renderStack = null);
                swapBackScript.update();
                swap.reset();
            }
        }
    }

    @Compile
    private Slot findBestTool(BlockPos blockPos) {
        BlockState state = mc.world.getBlockState(blockPos);
        if (PlayerIntersectionUtil.isAir(state)) return PlayerInventoryUtil.mainHandSlot();
        return PlayerInventoryUtil.slots().sorted(Comparator.comparing(slot -> slot.equals(PlayerInventoryUtil.mainHandSlot())))
                .filter(s -> s.getStack().getMiningSpeedMultiplier(state) != 1).max(Comparator.comparingDouble(s -> s.getStack().getMiningSpeedMultiplier(state))).orElse(null);
    }
}
