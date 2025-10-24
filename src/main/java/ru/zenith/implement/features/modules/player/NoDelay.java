package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.modules.combat.Aura;

import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoDelay extends Module {
    public static NoDelay getInstance() {
        return Instance.get(NoDelay.class);
    }

    public MultiSelectSetting ignoreSetting = new MultiSelectSetting("Type", "Allows the actions you choose")
            .value("Jump", "Right Click", "Break CoolDown");

    public NoDelay() {
        super("NoDelay", "No Delay", ModuleCategory.PLAYER);
        setup(ignoreSetting);
    }

    @Compile
    @EventHandler
    public void onTick(TickEvent e) {
        if (ignoreSetting.isSelected("Break CoolDown")) mc.interactionManager.blockBreakingCooldown = 0;
        if (ignoreSetting.isSelected("Jump")) mc.player.jumpingCooldown = 0;
        if (ignoreSetting.isSelected("Right Click")) mc.itemUseCooldown = 0;
    }
}