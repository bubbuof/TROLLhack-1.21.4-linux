package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.repository.friend.FriendUtils;
import ru.zenith.common.util.other.Instance;
import ru.zenith.core.Main;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackPerpetrator;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RaytracingUtil;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerBot extends Module {
    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }

    private LivingEntity target = null;

    private final MultiSelectSetting attackSetting = new MultiSelectSetting("Attack setting", "Allows you to customize the attack")
            .value("Only Critical", "Dynamic Cooldown", "Break Shield", "UnPress Shield", "No Attack When Eat");

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        setup(attackSetting);
    }

    @Compile
    @Override
    public void deactivate() {
        target = null;
        super.deactivate();
    }

    @Compile
    @EventHandler
    public void onTick(TickEvent e) {
        EntityHitResult result = RaytracingUtil.raytraceEntity(3, AngleUtil.cameraAngle(), s -> !FriendUtils.isFriend(s.getName().getString()));
        if (result instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity entity) {
            AttackPerpetrator.AttackPerpetratorConfigurable config = new AttackPerpetrator.AttackPerpetratorConfigurable(target = entity, AngleUtil.cameraAngle(),
                    3.3F, attackSetting.getSelected(), new SelectSetting("Mode", "").value("Default"), target.getBoundingBox());
            Main.getInstance().getAttackPerpetrator().performAttack(config);
        } else target = null;
    }
}
