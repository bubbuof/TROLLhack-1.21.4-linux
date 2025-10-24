package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.player.AttackEvent;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationController;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Criticals extends Module {
    public static Criticals getInstance() {
        return Instance.get(Criticals.class);
    }

    SelectSetting mode = new SelectSetting("Mode", "Select bypass mode").value("Grim");

    public Criticals() {
        super("Criticals", ModuleCategory.COMBAT);
        setup(mode);
    }

    @Compile
    @EventHandler
    public void onAttack(AttackEvent e) {
        if (mc.player.isTouchingWater()) return;
        if (mode.isSelected("Grim")) {
            if (!mc.player.isOnGround() && mc.player.fallDistance == 0) {
                PlayerIntersectionUtil.grimSuperBypass$$$(-(mc.player.fallDistance = MathUtil.getRandom(1e-5F, 1e-4F)), RotationController.INSTANCE.getRotation().random(1e-3F));
            }
        }
    }
}