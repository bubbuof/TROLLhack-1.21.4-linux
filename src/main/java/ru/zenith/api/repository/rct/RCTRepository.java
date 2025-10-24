package ru.zenith.api.repository.rct;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.EventManager;
import ru.zenith.common.QuickImports;
import ru.zenith.common.QuickLogger;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.world.ServerUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.draggables.Notifications;

public class RCTRepository implements QuickImports, QuickLogger {
    private final StopWatch stopWatch = new StopWatch();
    private boolean lobby;
    private int anarchy;

    public RCTRepository(EventManager eventManager) {eventManager.register(this);}

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (anarchy != 0 && e.getPacket() instanceof GameMessageS2CPacket message) {
            String text = message.content().getString().toLowerCase();
            if (!text.contains("хаб") && text.contains("не удалось")) {
                Notifications.getInstance().addList("[RCT] На данную анархию " + Formatting.RED + "нельзя" + Formatting.RESET + " зайти", 3000);
                anarchy = 0;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (anarchy == 0) return;

        if (!ServerUtil.isHolyWorld()) {
            anarchy = 0;
            return;
        }

        int currentAnarchy = ServerUtil.getAnarchy();
        if (lobby) {
            if (currentAnarchy == -1) lobby = false;
            else mc.player.networkHandler.sendChatCommand("hub");
            return;
        }

        if (currentAnarchy == anarchy) {
            anarchy = 0;
            return;
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen && screen.getTitle().getString().equals("Выбор Лайт анархии:"))  {
            boolean secondScreen = screen.getScreenHandler().getInventory().size() < 10;
            int[] slots = anarchy < 15 ? new int[]{0, 0} : anarchy < 33 ? new int[]{1, 14} : anarchy < 48 ? new int[]{2, 32} : new int[]{3, 47};
            if (secondScreen) PlayerInventoryUtil.clickSlot(slots[0], 0, SlotActionType.PICKUP, false);
            else PlayerInventoryUtil.clickSlot(17 + anarchy - slots[1], 0, SlotActionType.PICKUP, false);
            return;
        }

        if (stopWatch.every(500)) mc.player.networkHandler.sendChatCommand("lite");
    }

    public void reconnect(int anarchy) {
        if (anarchy > 0 && anarchy < 64) {
            this.anarchy = anarchy;
            this.lobby = true;
        } else {
            Notifications.getInstance().addList("[RCT] Не верный " + Formatting.RED + "лайт", 3000);
        }
    }
}
