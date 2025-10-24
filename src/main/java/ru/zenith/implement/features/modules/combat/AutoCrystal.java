package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.entity.PlayerInventoryComponent;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.EntitySpawnEvent;
import ru.zenith.implement.events.player.TickEvent;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoCrystal extends Module {
    private final Script script = new Script();
    private BlockPos obsPosition;

    public AutoCrystal() {
        super("AutoCrystal", "Auto Crystal", ModuleCategory.COMBAT);
        setup();
    }

    @Override
    public void activate() {
        obsPosition = null;
        super.activate();
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof PlayerInteractBlockC2SPacket interact && interact.getSequence() != 0 && script.isFinished() && PlayerInventoryComponent.script.isFinished())
            script.addTickStep(0, () -> {
                BlockPos interactPos = interact.getBlockHitResult().getBlockPos();
                BlockPos spawnPos = interactPos.offset(interact.getBlockHitResult().getSide());
                BlockPos blockPos = mc.world.getBlockState(spawnPos).getBlock().equals(Blocks.OBSIDIAN) ? spawnPos : mc.world.getBlockState(interactPos).getBlock().equals(Blocks.OBSIDIAN) ? interactPos : null;
                Slot crystal = PlayerInventoryUtil.getSlot(Items.END_CRYSTAL);

                if (blockPos != null && crystal != null) PlayerInventoryComponent.addTask(() -> {
                    obsPosition = blockPos;
                    PlayerInventoryUtil.swapHand(crystal, Hand.MAIN_HAND, false);
                    PlayerIntersectionUtil.sendSequencedPacket(i -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, false), i));
                    PlayerInventoryUtil.swapHand(crystal, Hand.MAIN_HAND, false, true);
                    script.cleanup().addTickStep(6, () -> obsPosition = null);
                });
            });
    }

    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EndCrystalEntity crystal && obsPosition.equals(crystal.getBlockPos().down())) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            obsPosition = null;
            script.cleanup();
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
    }
}
