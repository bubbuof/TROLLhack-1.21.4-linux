package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.world.ServerUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.draggables.Notifications;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationConfig;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationController;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClanUpgrade extends Module {
    StopWatch stopWatch = new StopWatch();

    public ClanUpgrade() {
        super("ClanUpgrade","Clan Upgrade", ModuleCategory.MISC);
        setup();
    }

    
    @EventHandler
    public void onTick(TickEvent e) {
        if (ServerUtil.getWorldType().equals("lobby") && stopWatch.every(5000)) {
            Notifications.getInstance().addList("В этом мире нельзя" + Formatting.RED + " прокачивать " + Formatting.RESET + "клан", 2500);
            return;
        }

        int slotId = PlayerInventoryUtil.getHotbarSlotId(s -> mc.player.getInventory().getStack(s).getItem().equals(Items.TORCH) || mc.player.getInventory().getStack(s).getItem().equals(Items.REDSTONE));
        if (slotId == -1) {
            if (stopWatch.every(5000)) {
                Notifications.getInstance().addList("Нужен" + Formatting.RED + "факел/редстоун " + Formatting.RESET + "в хотбаре", 2500);
            }
            return;
        }

        if (mc.player.getInventory().selectedSlot != slotId) {
            mc.player.getInventory().selectedSlot = slotId;
            return;
        }

        BlockPos pos = mc.player.getBlockPos().down();
        if (mc.world.getBlockState(pos).isSolid()) {
            RotationController controller = RotationController.INSTANCE;
            controller.rotateTo(AngleUtil.pitch(90), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, this);
            if (controller.getServerAngle().getPitch() >= 89) {
                PlayerIntersectionUtil.sendSequencedPacket(in -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false), in));
                PlayerIntersectionUtil.sendSequencedPacket(in -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos.up(), Direction.UP, in));
            }
        }
    }
}
