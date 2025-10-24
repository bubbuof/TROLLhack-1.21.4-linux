package ru.zenith.common.util.item;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.Hand;
import ru.zenith.api.event.EventHandler;
import ru.zenith.common.QuickImports;
import ru.zenith.core.Main;
import ru.zenith.implement.events.packet.PacketEvent;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemUsage implements QuickImports {
    public final static ItemUsage INSTANCE = new ItemUsage();
    public boolean useItem, releaseItem = true;

    public ItemUsage() {
        Main.getInstance().getEventManager().register(this);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case PlayerActionC2SPacket player when player.getAction().equals(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) -> releaseItem = true;
            case ClientStatusC2SPacket status when status.getMode().equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) -> releaseItem = true;
            case PlayerRespawnS2CPacket respawn -> releaseItem = true;
            case GameJoinS2CPacket join -> releaseItem = true;
            default -> {}
        }
    }

    public void useHand(Hand hand) {
        if (releaseItem) {
            mc.interactionManager.interactItem(mc.player, hand);
            releaseItem = false;
        }
        useItem = true;
    }
}
