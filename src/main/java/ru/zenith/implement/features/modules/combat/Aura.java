package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.types.EventType;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.core.Main;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.RotationUpdateEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.implement.features.draggables.Notifications;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackHandler;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackPerpetrator;
import ru.zenith.implement.features.modules.combat.killaura.rotation.*;
import ru.zenith.implement.features.modules.combat.killaura.rotation.angle.*;
import ru.zenith.implement.features.modules.combat.killaura.target.TargetSelector;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.Objects;


@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Aura extends Module {
    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    Animation esp_anim = new DecelerateAnimation().setMs(400).setValue(1);
    TargetSelector targetSelector = new TargetSelector();
    PointFinder pointFinder = new PointFinder();
    @NonFinal
    LivingEntity target, lastTarget;
    ValueSetting distance = new ValueSetting("Distance", "Maximum attack distance")
            .setValue(3.3f).range(1.0f, 6.0f);

    MultiSelectSetting targetType = new MultiSelectSetting("Target Type", "Filters the entire list of targets by type")
            .value("Players", "Mobs", "Animals", "Friends");

    MultiSelectSetting attackSetting = new MultiSelectSetting("Attack Setting", "Allows you to customize the attack")
            .value("Only Critical", "Dynamic Cooldown", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls");

    SelectSetting correctionType = new SelectSetting("Correction Type", "Selects the type of correction")
            .value("Free", "Focused").selected("Free");

    GroupSetting correctionGroup = new GroupSetting("Move correction", "Prevents detection by movement sensitive anti-cheats")
            .settings(correctionType).setValue(true);

    SelectSetting aimMode = new SelectSetting("Rotation Type", "Allows you to select the rotation type")
            .value("FunTime", "Snap", "Matrix").selected("Snap");

    SelectSetting targetEspType = new SelectSetting("Target Esp Type", "Selects the type of target esp")
            .value("Cube", "Circle", "Ghosts").selected("Circle");

    ValueSetting ghostSpeed = new ValueSetting("Ghost Speed", "Speed of ghost flying around the target")
            .setValue(1).range(1F, 2F).visible(()-> targetEspType.isSelected("Ghosts"));

    GroupSetting targetEspGroup = new GroupSetting("Target Esp", "Displays the player in the world")
            .settings(targetEspType, ghostSpeed).setValue(true);

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(distance, targetType, attackSetting, correctionGroup, aimMode, targetEspGroup);
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        super.deactivate();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        esp_anim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = esp_anim.getOutput().floatValue();
        if (targetEspGroup.isValue() && lastTarget != null && !esp_anim.isFinished(Direction.BACKWARDS)) {
            float red = MathHelper.clamp((lastTarget.hurtTime - tickCounter.getTickDelta(false)) / 10, 0, 1);
            switch (targetEspType.getSelected()) {
                case "Cube" -> Render3DUtil.drawCube(lastTarget, anim, red);
                case "Circle" -> Render3DUtil.drawCircle(e.getStack(), lastTarget, anim, red);
                case "Ghosts" -> Render3DUtil.drawGhosts(lastTarget, anim, red, ghostSpeed.getValue());
            }
        }
    }

    
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 3000);
            }
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                if (target != null) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                Render3DUtil.updateTargetEsp();
                if (target != null) Main.getInstance().getAttackPerpetrator().performAttack(getConfig());
            }
        }
    }

    private LivingEntity updateTarget() {
        TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(targetType.getSelected());
        targetSelector.searchTargets(mc.world.getEntities(), distance.getValue(), 360, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    
    private void rotateToTarget(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        AttackHandler attackHandler = Main.getInstance().getAttackPerpetrator().getAttackHandler();
        RotationController controller = RotationController.INSTANCE;
        Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
        RotationConfig rotationConfig = getRotationConfig();
        switch (aimMode.getSelected()) {
            case "Snap" -> {
                if (attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100)) {
                    controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }
            case "FunTime" -> {
                if (attackHandler.canAttack(config, 3)) {
                    controller.clear();
                    controller.rotateTo(rotation, target, 40, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }
            case "Matrix" -> controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }
    }

    public AttackPerpetrator.AttackPerpetratorConfigurable getConfig() {
        Pair<Vec3d, Box> point = pointFinder.computeVector(target, distance.getValue(), RotationController.INSTANCE.getRotation(), getSmoothMode().randomValue(), attackSetting.isSelected("Ignore The Walls"));
        Angle angle = AngleUtil.fromVec3d(point.getLeft().subtract(Objects.requireNonNull(mc.player).getEyePos()));
        Box box = point.getRight();
        return new AttackPerpetrator.AttackPerpetratorConfigurable(target, angle, distance.getValue(), attackSetting.getSelected(), aimMode, box);
    }

    public RotationConfig getRotationConfig() {
        return new RotationConfig(getSmoothMode(), correctionGroup.isValue(), correctionType.isSelected("Free"));
    }

    
    public AngleSmoothMode getSmoothMode() {
        return switch (aimMode.getSelected()) {
            case "FunTime" -> new FunTimeSmoothMode();
            case "Matrix" -> new MatrixSmoothMode();
            case "Snap" -> new SnapSmoothMode();
            default -> new LinearSmoothMode();
        };
    }
}