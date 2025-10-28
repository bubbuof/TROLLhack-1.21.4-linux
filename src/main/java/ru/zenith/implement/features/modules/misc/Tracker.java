package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.player.EntitySpawnEvent;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tracker extends Module {

    final BooleanSetting only1v1 = new BooleanSetting("1v1 Only", "Track only in 1v1 duels")
            .setValue(true);

    final Set<BlockPos> placed = ConcurrentHashMap.newKeySet();
    final AtomicInteger awaitingExp = new AtomicInteger();
    final AtomicInteger crystals = new AtomicInteger();
    final AtomicInteger exp = new AtomicInteger();

    PlayerEntity trackedPlayer;
    boolean awaiting;
    int crystalStacks;
    int expStacks;

    public Tracker() {
        super("Tracker", "Tracker", ModuleCategory.MISC);
        setup(only1v1);
    }

    @Override
    public void activate() {
        super.activate();
        awaiting = false;
        trackedPlayer = null;
        awaitingExp.set(0);
        crystals.set(0);
        exp.set(0);
        crystalStacks = 0;
        expStacks = 0;
    }

    public String getDisplayInfo() {
        return trackedPlayer == null ? null : trackedPlayer.getName().getString();
    }

    @EventHandler
    public void onPacketReceive(PacketEvent event) {
        if (event.getPacket() instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket pac = (GameMessageS2CPacket) event.getPacket();
            String s = pac.content().getString();

            // Detect duel acceptance
            if (!s.contains("<") && (s.contains("has accepted your duel request") || s.contains("Accepted the duel request from"))) {
                // Используем sendMessage с двумя параметрами
                if (mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal("Дуель принята! Обновляю цель..."), false);
                }
                trackedPlayer = null;
                awaitingExp.set(0);
                crystals.set(0);
                exp.set(0);
                crystalStacks = 0;
                expStacks = 0;
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof EndCrystalEntity) {
            // Count crystals placed by opponent
            if (!placed.remove(new BlockPos((int) event.getEntity().getX(), (int) event.getEntity().getY() - 1, (int) event.getEntity().getZ()))) {
                crystals.incrementAndGet();
            }
        }
        if (event.getEntity() instanceof ExperienceBottleEntity) {
            // Count XP bottles thrown by opponent
            if (awaitingExp.get() > 0) {
                if (mc.player.squaredDistanceTo(event.getEntity()) < 16) {
                    awaitingExp.decrementAndGet();
                } else {
                    exp.incrementAndGet();
                }
            } else {
                exp.incrementAndGet();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        boolean found = false;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player.equals(mc.player)) continue;

            // If multiple players found and 1v1 mode is on - disable
            if (found && only1v1.isValue()) {
                setState(false);
                if (mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal("Ты не в дуели! Отключаю.."), false);
                }
                return;
            }

            // Set tracking target
            if (trackedPlayer == null) {
                if (mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal(
                            Formatting.LIGHT_PURPLE + "Следим за " + Formatting.DARK_PURPLE +
                                    player.getName().getString() + Formatting.LIGHT_PURPLE + "!"
                    ), false);
                }
            }
            trackedPlayer = player;
            found = true;
        }

        if (trackedPlayer == null) return;

        // Update XP bottles stacks
        int currentExpStacks = exp.get() / 64;
        if (expStacks != currentExpStacks) {
            expStacks = currentExpStacks;
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal(
                        Formatting.DARK_PURPLE + trackedPlayer.getName().getString() + Formatting.LIGHT_PURPLE +
                                " использовал " + Formatting.WHITE + expStacks + Formatting.LIGHT_PURPLE +
                                (expStacks == 1 ? " стак" : " стаков") + " Пузырьков опыта!"
                ), false);
            }
        }

        // Update crystal stacks
        int currentCrystalStacks = crystals.get() / 64;
        if (crystalStacks != currentCrystalStacks) {
            crystalStacks = currentCrystalStacks;
            if (mc.player != null) {
                mc.player.sendMessage(net.minecraft.text.Text.literal(
                        Formatting.DARK_PURPLE + trackedPlayer.getName().getString() + Formatting.LIGHT_PURPLE +
                                " использовал " + Formatting.WHITE + crystalStacks + Formatting.LIGHT_PURPLE +
                                (crystalStacks == 1 ? " стак" : " стаков") + " Кристаллов!"
                ), false);
            }
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent event) {
        // Track our own XP bottle throws
        if (event.getPacket() instanceof PlayerInteractItemC2SPacket) {
            if (mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE ||
                    mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE) {
                awaitingExp.incrementAndGet();
            }
        }

        // Track our own crystal placements
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket) {
            PlayerInteractBlockC2SPacket pac = (PlayerInteractBlockC2SPacket) event.getPacket();
            if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL ||
                    mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL) {
                placed.add(pac.getBlockHitResult().getBlockPos());
            }
        }
    }

    // Command to show current stats
    public void sendTrack() {
        if (trackedPlayer != null && mc.player != null) {
            int c = crystals.get();
            int e = exp.get();

            String message = Formatting.DARK_PURPLE + trackedPlayer.getName().getString() + Formatting.LIGHT_PURPLE +
                    " использовал " + Formatting.WHITE + c + Formatting.LIGHT_PURPLE + " (" + Formatting.WHITE +
                    (c % 64 == 0 ? c / 64 : String.format("%.1f", c / 64.0)) + Formatting.LIGHT_PURPLE +
                    ") кристаллов и " + Formatting.WHITE + e + Formatting.LIGHT_PURPLE + " (" + Formatting.WHITE +
                    (e % 64 == 0 ? e / 64 : String.format("%.1f", e / 64.0)) + Formatting.LIGHT_PURPLE +
                    ") пузырьков опыта.";

            mc.player.sendMessage(net.minecraft.text.Text.literal(message), false);
        }
    }
}