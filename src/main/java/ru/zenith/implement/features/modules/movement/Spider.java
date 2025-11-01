package ru.zenith.implement.features.modules.movement;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.item.ItemStack;
import java.util.Timer;
import java.util.TimerTask;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Spider extends Module {

    private static Spider instance;

    // Настройки для режима SpookyTime (WaterBucket)
    final SelectSetting mode = new SelectSetting("Mode", "Spider mode")
            .value("SpookyTime")
            .selected("SpookyTime");

    final ValueSetting delay = new ValueSetting("Delay", "Usage delay")
            .range(0.1f, 1.0f)
            .setValue(0.31f)
            .visible(() -> mode.isSelected("SpookyTime"));

    final BooleanSetting holdShift = new BooleanSetting("Hold Shift", "Automatically hold shift")
            .setValue(true)
            .visible(() -> mode.isSelected("SpookyTime"));

    final BooleanSetting silentUse = new BooleanSetting("Silent Use", "Use bucket silently")
            .setValue(true)
            .visible(() -> mode.isSelected("SpookyTime"));

    final BooleanSetting holdSpace = new BooleanSetting("Jump Start", "Jump at start")
            .setValue(false)
            .visible(() -> mode.isSelected("SpookyTime"));

    // Таймер и состояния
    Timer timer = new Timer();
    boolean canUse = true;
    long lastWallJumpMs = 0L;
    static final long WALL_JUMP_COOLDOWN_MS = 250L;
    int originalSlot = -1;

    public Spider() {
        super("Spider", "Spider", ModuleCategory.MOVEMENT);
        setup(mode, delay, holdShift, silentUse, holdSpace);
        instance = this;
    }

    public static Spider getInstance() {
        return instance;
    }

    @Override
    public void activate() {
        super.activate();
        originalSlot = mc.player.getInventory().selectedSlot;
        canUse = true;
        lastWallJumpMs = 0L;
    }

    @Override
    public void deactivate() {
        super.deactivate();
        timer.cancel();
        timer = new Timer();
        canUse = true;

        // Отжимаем клавиши
        if (mc.options != null) {
            mc.options.sneakKey.setPressed(false);
            mc.options.jumpKey.setPressed(false);
        }

        // Возвращаем исходный слот
        if (mode.isSelected("SpookyTime") && originalSlot != -1 && mc.player.getInventory().selectedSlot != originalSlot) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
        originalSlot = -1;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (mode.isSelected("SpookyTime")) {
            handleSpookyTime();
        }
    }

    private void handleSpookyTime() {
        if (mc.player.isTouchingWater()) {
            mc.player.setVelocity(mc.player.getVelocity().x, 0.30f, mc.player.getVelocity().z);
        } else {
            int waterSlot = -1;
            // Ищем ведро с водой в хотбаре
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                    waterSlot = i;
                    break;
                }
            }

            boolean hasWaterBucket;
            if (silentUse.isValue()) {
                // В режиме silent use проверяем весь инвентарь
                hasWaterBucket = waterSlot != -1 || hasWaterBucketInInventory();
            } else {
                // Только хотбар и рука
                hasWaterBucket = waterSlot != -1 ||
                        (!mc.player.getMainHandStack().isEmpty() &&
                                mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET);
            }

            if (hasWaterBucket) {
                if (mc.player.horizontalCollision) {
                    // Обработка прыжка в начале
                    if (holdSpace.isValue()) {
                        if (mc.player.isOnGround()) {
                            long now = System.currentTimeMillis();
                            if (now - lastWallJumpMs > WALL_JUMP_COOLDOWN_MS) {
                                mc.player.jump();
                                lastWallJumpMs = now;
                            }
                        }
                        mc.options.jumpKey.setPressed(true);
                    }

                    // Основная логика использования ведра
                    if (canUse) {
                        // Устанавливаем угол взгляда
                        mc.player.setPitch(75.0f);

                        if (silentUse.isValue()) {
                            useWaterBucketSilently(waterSlot);
                        } else {
                            useWaterBucketNormal(waterSlot);
                        }

                        // Подбрасываем игрока вверх
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.43f, mc.player.getVelocity().z);
                        canUse = false;

                        // Таймер задержки
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                canUse = true;
                            }
                        }, getAppliedDelayMs());
                    }

                    // Зажимаем шифт если нужно
                    if (holdShift.isValue()) {
                        mc.options.sneakKey.setPressed(true);
                    }
                }

                // Отжимаем прыжок когда не на стене
                if (!mc.player.horizontalCollision) {
                    if (mc.options.jumpKey.isPressed() && holdSpace.isValue()) {
                        mc.options.jumpKey.setPressed(false);
                    }
                }
            }
        }
    }

    private boolean hasWaterBucketInInventory() {
        // Проверяем весь инвентарь на наличие ведра с водой
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return true;
            }
        }
        return false;
    }

    private void useWaterBucketSilently(int waterSlot) {
        if (waterSlot == -1) {
            // Если ведра нет в хотбаре, находим и переключаемся
            int slot = findWaterBucketSlot();
            if (slot != -1) {
                int currentSlot = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = slot;
                // Исправленный конструктор с pitch и yaw
                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getPitch(), mc.player.getYaw()));
                mc.player.getInventory().selectedSlot = currentSlot;
            }
        } else {
            // Вебро в хотбаре - используем с переключением
            int currentSlot = mc.player.getInventory().selectedSlot;
            if (waterSlot != currentSlot) {
                mc.player.getInventory().selectedSlot = waterSlot;
            }
            // Исправленный конструктор с pitch и yaw
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getPitch(), mc.player.getYaw()));
            if (waterSlot != currentSlot) {
                mc.player.getInventory().selectedSlot = currentSlot;
            }
        }
        // Свинг руки
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private void useWaterBucketNormal(int waterSlot) {
        if (waterSlot != -1) {
            // Переключаемся на слот с ведром
            if (mc.player.getInventory().selectedSlot != waterSlot) {
                mc.player.getInventory().selectedSlot = waterSlot;
            }
        }
        // Используем ведро с исправленным конструктором
        mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getPitch(), mc.player.getYaw()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private int findWaterBucketSlot() {
        // Ищем ведро во всем инвентаре
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    private int getAppliedDelayMs() {
        return (int)(delay.getValue() * 1000f);
    }
}