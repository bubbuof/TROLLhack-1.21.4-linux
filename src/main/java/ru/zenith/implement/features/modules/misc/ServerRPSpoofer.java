package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.event.EventHandler;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.common.util.math.Counter;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServerRPSpoofer extends Module {
    private ResourcePackAction currentAction = ResourcePackAction.WAIT;
    private final Counter counter = Counter.create();

    public ServerRPSpoofer() {
        super("ServerRPSpoof", "Server RP Spoof", ModuleCategory.MISC);
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof ResourcePackSendS2CPacket) {
            currentAction = ResourcePackAction.ACCEPT;
            e.cancel();
        }
    }

    
    @EventHandler
    
    public void onTick(TickEvent e) {
        ClientPlayNetworkHandler networkHandler = mc.getNetworkHandler();
        if (networkHandler != null) {
            if (currentAction == ResourcePackAction.ACCEPT) {
                networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.ACCEPTED));
                currentAction = ResourcePackAction.SEND;
                counter.resetCounter();
            } else if (currentAction == ResourcePackAction.SEND && counter.isReached(300L)) {
                networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
                currentAction = ResourcePackAction.WAIT;
            }
        }
    }

    @Override
    public void deactivate() {
        currentAction = ResourcePackAction.WAIT;
        super.deactivate();
    }

    public enum ResourcePackAction {
        ACCEPT, SEND, WAIT;
    }
}
