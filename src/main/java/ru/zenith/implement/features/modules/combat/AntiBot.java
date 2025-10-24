package ru.zenith.implement.features.modules.combat;

import com.mojang.authlib.GameProfile;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AntiBot extends Module {
    public static AntiBot getInstance() {
        return Instance.get(AntiBot.class);
    }

    Set<UUID> suspectSet = new HashSet<>();
    static Set<UUID> botSet = new HashSet<>();

    public AntiBot() {
        super("AntiBot", ModuleCategory.COMBAT);
    }


    @EventHandler
    public void onPacket(PacketEvent e) {
        switch (e.getPacket()) {
            case PlayerListS2CPacket list -> checkPlayerAfterSpawn(list);
            case PlayerRemoveS2CPacket remove -> removePlayerBecauseLeftServer(remove);
            default -> {
            }
        }
    }


    @EventHandler
    public void onTick(TickEvent e) {
        if (!suspectSet.isEmpty()) mc.world.getPlayers().stream()
                .filter(p -> suspectSet.contains(p.getUuid())).forEach(this::evaluateSuspectPlayer);
    }


    private void checkPlayerAfterSpawn(PlayerListS2CPacket listS2CPacket) {
        listS2CPacket.getPlayerAdditionEntries().forEach(entry -> {
            GameProfile profile = entry.profile();
            if (profile == null || isRealPlayer(entry, profile)) {
                return;
            }

            if (isDuplicateProfile(profile)) {
                botSet.add(profile.getId());
            } else {
                suspectSet.add(profile.getId());
            }
        });
    }


    private void removePlayerBecauseLeftServer(PlayerRemoveS2CPacket removeS2CPacket) {
        removeS2CPacket.profileIds().forEach(uuid -> {
            suspectSet.remove(uuid);
            botSet.remove(uuid);
        });
    }


    private boolean isRealPlayer(PlayerListS2CPacket.Entry entry, GameProfile profile) {
        return entry.latency() < 2 || (profile.getProperties() != null && !profile.getProperties().isEmpty());
    }


    private void evaluateSuspectPlayer(PlayerEntity player) {
        Iterable<ItemStack> armor = null;

        if (!isFullyEquipped(player)) {
            armor = player.getArmorItems();
        }
        if ((isFullyEquipped(player) || hasArmorChanged(player, armor))) {
            botSet.add(player.getUuid());
        }
        suspectSet.remove(player.getUuid());
    }


    public boolean isDuplicateProfile(GameProfile profile) {
        return Objects.requireNonNull(mc.getNetworkHandler()).getPlayerList().stream()
                .filter(player -> player.getProfile().getName().equals(profile.getName()) && !player.getProfile().getId().equals(profile.getId()))
                .count() == 1;
    }


    public boolean isFullyEquipped(PlayerEntity entity) {
        return IntStream.rangeClosed(0, 3)
                .mapToObj(entity.getInventory()::getArmorStack)
                .allMatch(stack -> stack.getItem() instanceof ArmorItem && !stack.hasEnchantments());
    }


    public boolean hasArmorChanged(PlayerEntity entity, Iterable<ItemStack> prevArmor) {
        if (prevArmor == null) {
            return true;
        }

        List<ItemStack> currentArmorList = StreamSupport.stream(entity.getArmorItems().spliterator(), false).toList();
        List<ItemStack> prevArmorList = StreamSupport.stream(prevArmor.spliterator(), false).toList();

        return !IntStream.range(0, Math.min(currentArmorList.size(), prevArmorList.size()))
                .allMatch(i -> currentArmorList.get(i).equals(prevArmorList.get(i))) || currentArmorList.size() != prevArmorList.size();
    }

    public boolean isBot(PlayerEntity entity) {
        return botSet.contains(entity.getUuid());
    }

    public void reset() {
        suspectSet.clear();
        botSet.clear();
    }

    @Override
    public void deactivate() {
        reset();
        super.deactivate();
    }
}