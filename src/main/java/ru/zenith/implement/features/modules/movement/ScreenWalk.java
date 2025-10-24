package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import ru.zenith.api.event.EventHandler;
import ru.zenith.common.util.entity.MovingUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.entity.PlayerInventoryComponent;

import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.implement.events.container.CloseScreenEvent;
import ru.zenith.implement.events.item.ClickSlotEvent;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScreenWalk extends Module {
    private final List<Packet<?>> packets = new ArrayList<>();

    public ScreenWalk() {
        super("ScreenWalk", "Screen Walk", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case ClickSlotC2SPacket slot when (!packets.isEmpty() || MovingUtil.hasPlayerMovement()) && PlayerInventoryComponent.shouldSkipExecution() -> {
                packets.add(slot);
                e.cancel();
            }
            case CloseScreenS2CPacket screen when screen.getSyncId() == 0 -> e.cancel();
            default -> {
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (!PlayerInventoryUtil.isServerScreen() && PlayerInventoryComponent.shouldSkipExecution() && (!packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
            PlayerInventoryComponent.updateMoveKeys();
        }
    }

    @EventHandler
    public void onClickSlot(ClickSlotEvent e) {
        SlotActionType actionType = e.getActionType();
        if ((!packets.isEmpty() || MovingUtil.hasPlayerMovement()) && ((e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW)) || actionType.equals(SlotActionType.PICKUP_ALL))) {
            e.cancel();
        }
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent e) {
        if (!packets.isEmpty()) PlayerInventoryComponent.addTask(() -> {
            packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
            packets.clear();
            PlayerInventoryUtil.updateSlots();
        });
    }
}
