package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TridentAim extends Module {

    private static TridentAim instance;

    public static TridentAim getInstance() {
        if (instance == null) {
            instance = new TridentAim();
        }
        return instance;
    }

    final BooleanSetting smooth = new BooleanSetting("Smooth Aim", "Плавная наводка")
            .setValue(false);

    final ValueSetting distance = new ValueSetting("Distance", "Максимальная дистанция")
            .range(4.0f, 140.0f)
            .setValue(16.0f);

    final ValueSetting yCorrection = new ValueSetting("Y Correction", "Коррекция по высоте")
            .range(0.0f, 5.0f)
            .setValue(1.5f);

    public TridentAim() {
        super("TridentAim", "TridentAim", ModuleCategory.COMBAT);
        setup(smooth, distance, yCorrection);
        instance = this;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isUsingItem()) {
            ItemStack active = mc.player.getActiveItem();
            if (active != null && active.getItem() == Items.TRIDENT) {
                PlayerEntity target = findNearestPlayer(distance.getValue());
                if (target != null) {
                    if (canSee(mc.player.getEyePos(), target.getEyePos())) {
                        // Используем фиксированное значение для tickDelta
                        float tickDelta = 1.0f; // Для TickEvent это нормально
                        float[] desired = calcAimToEntity(target, yCorrection.getValue());
                        float[] toSend;

                        if (smooth.isValue()) {
                            float curYaw = mc.player.getYaw();
                            float curPitch = mc.player.getPitch();
                            float lerp = 0.3F;
                            float yawDelta = wrapDegrees(desired[0] - curYaw);
                            float newYaw = curYaw + yawDelta * lerp;
                            float newPitch = lerp(curPitch, desired[1], lerp);
                            toSend = new float[]{newYaw, newPitch};
                        } else {
                            toSend = desired;
                        }

                        // Устанавливаем поворот
                        mc.player.setYaw(toSend[0]);
                        mc.player.setPitch(toSend[1]);
                    }
                }
            }
        }
    }

    private PlayerEntity findNearestPlayer(float maxDist) {
        double best = Double.MAX_VALUE;
        PlayerEntity bestPlayer = null;
        double maxSq = maxDist * maxDist;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) {
                if (player != mc.player && player.isAlive() && !isFriend(player)) {
                    double d = mc.player.squaredDistanceTo(player);
                    if (d <= maxSq && d < best) {
                        best = d;
                        bestPlayer = player;
                    }
                }
            }
        }

        return bestPlayer;
    }

    private boolean canSee(Vec3d from, Vec3d to) {
        HitResult hitResult = mc.world.raycast(new RaycastContext(
                from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        ));
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    private float[] calcAimToEntity(PlayerEntity target, float yCorr) {
        Vec3d eyes = mc.player.getEyePos();
        // Используем текущую позицию цели (без интерполяции)
        Vec3d targetPos = target.getPos().add(0.0, target.getStandingEyeHeight() + yCorr, 0.0);
        Vec3d diff = targetPos.subtract(eyes);
        double diffXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float)(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float pitch = (float)(-Math.toDegrees(Math.atan2(diff.y, diffXZ)));
        return new float[]{yaw, pitch};
    }

    // Вспомогательные методы
    private float wrapDegrees(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }
        if (angle < -180.0F) {
            angle += 360.0F;
        }
        return angle;
    }

    private float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    private boolean isFriend(PlayerEntity player) {
        // Заглушка для проверки друзей - реализуйте свою логику
        // return Main.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
        return false;
    }
}