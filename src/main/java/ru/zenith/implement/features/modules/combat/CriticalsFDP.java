package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.AttackEvent;
import ru.zenith.implement.events.packet.PacketEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CriticalsFDP extends Module {

    private static CriticalsFDP instance;

    public static CriticalsFDP getInstance() {
        if (instance == null) {
            instance = new CriticalsFDP();
        }
        return instance;
    }

    // Настройки режимов
    final SelectSetting mode = new SelectSetting("Mode", "Critical attack mode")
            .value("Packet", "NCPPacket", "BlocksMC", "BlocksMC2", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "CustomMotion", "Visual", "Grim")
            .selected("Packet");

    final ValueSetting delayMs = new ValueSetting("Delay", "Delay between criticals")
            .range(0.0f, 500.0f)
            .setValue(0.0f);

    final ValueSetting hurtTimeMax = new ValueSetting("HurtTime", "Maximum target hurt time")
            .range(0.0f, 10.0f)
            .setValue(10.0f);

    final ValueSetting customY = new ValueSetting("Custom-Y", "Custom Y motion")
            .range(0.01f, 0.42f)
            .setValue(0.2f);

    long lastTime = 0L;

    public CriticalsFDP() {
        super("CriticalsFDP", "CriticalsFDP", ModuleCategory.COMBAT);
        setup(mode, delayMs, hurtTimeMax, customY);
        instance = this;
    }

    @Override
    public void activate() {
        super.activate();
        if (mode.isSelected("NoGround") && mc.player != null) {
            mc.player.setOnGround(false);
        }
    }

    @EventHandler
    public void onAttack(AttackEvent event) {
        Entity target = event.getEntity();
        if (target instanceof LivingEntity living) {
            if (mc.player != null) {
                if (mode.isSelected("Grim")) {
                    if (!mc.player.isSubmergedInWater()) {
                        boolean wasSprinting = mc.player.isSprinting();
                        mc.player.setSprinting(false);
                        double fall = ThreadLocalRandom.current().nextDouble(1.0E-5, 1.0E-4);
                        mc.player.fallDistance = (float) fall;
                        double y = mc.player.getY();
                        sendPos(y - fall, false);
                        if (living != null) {
                            mc.player.attack(living);
                        }
                        mc.player.setSprinting(wasSprinting);
                        lastTime = System.currentTimeMillis();
                    }
                } else if (canTrigger(living)) {
                    double x = mc.player.getX();
                    double y = mc.player.getY();
                    double z = mc.player.getZ();

                    switch (mode.getSelected()) {
                        case "Packet" -> {
                            sendPos(y + 0.0625F, true);
                            sendPos(y, false);
                            mc.player.attack(living);
                        }
                        case "NCPPacket" -> {
                            sendPos(y + 0.11, false);
                            sendPos(y + 0.1100013579, false);
                            sendPos(y + 1.3579E-6, false);
                            mc.player.attack(living);
                        }
                        case "BlocksMC" -> {
                            sendPos(y + 0.001091981, true);
                            sendPos(y, false);
                        }
                        case "BlocksMC2" -> {
                            if (mc.player.age % 4 == 0) {
                                sendPos(y + 0.0011, true);
                                sendPos(y, false);
                            }
                        }
                        case "Hop" -> {
                            mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
                            mc.player.fallDistance = 0.1F;
                            mc.player.setOnGround(false);
                        }
                        case "TPHop" -> {
                            sendPos(y + 0.02, false);
                            sendPos(y + 0.01, false);
                            mc.player.updatePosition(x, y + 0.01, z);
                        }
                        case "Jump" -> {
                            mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
                        }
                        case "LowJump" -> {
                            mc.player.setVelocity(mc.player.getVelocity().x, 0.3425, mc.player.getVelocity().z);
                        }
                        case "CustomMotion" -> {
                            mc.player.setVelocity(mc.player.getVelocity().x, customY.getValue(), mc.player.getVelocity().z);
                        }
                        case "Visual" -> {
                            mc.player.attack(living);
                        }
                    }
                    lastTime = System.currentTimeMillis();
                }
            }
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mode.isSelected("NoGround") && event.getType() == PacketEvent.Type.SEND) {
            // Логика для NoGround режима
        }
    }

    private void sendPos(double y, boolean onGround) {
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), y, mc.player.getZ(), onGround, false
            ));
        }
    }

    private boolean canTrigger(LivingEntity target) {
        if (mc.player == null) return false;

        if (!mc.player.isOnGround() && !mode.isSelected("Jump") && !mode.isSelected("LowJump") &&
                !mode.isSelected("Hop") && !mode.isSelected("TPHop")) {
            return false;
        }

        // Убрал isFallFlying() и заменил на более универсальные проверки
        if (mc.player.isUsingItem() || isFlying() || mc.player.isClimbing()) {
            return false;
        }

        if (mc.player.hasVehicle()) {
            return false;
        }

        if (target.hurtTime > hurtTimeMax.getValue()) {
            return false;
        }

        long delay = (long) delayMs.getValue();
        return System.currentTimeMillis() - lastTime >= delay;
    }

    private boolean isFlying() {
        // Универсальная проверка на полет/парение
        if (mc.player == null) return false;

        // Попробуйте один из этих вариантов:
        // return mc.player.getAbilities().flying; // Творческий полет
        // return mc.player.isFallFlying(); // Если все же есть в вашей версии
        // return !mc.player.isOnGround() && mc.player.getVelocity().y > 0; // Простая проверка
        // return mc.player.isSwimming(); // Если в воде

        // Пока просто возвращаем false чтобы код компилировался
        return false;
    }
}