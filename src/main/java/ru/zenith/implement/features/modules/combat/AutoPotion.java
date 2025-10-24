package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoPotion extends Module {
    public AutoPotion() {
        super("AutoPotion", "Auto Potion", ModuleCategory.COMBAT);
        setup();
    }

    @EventHandler
    public void onTick(TickEvent e) {

    }
}
