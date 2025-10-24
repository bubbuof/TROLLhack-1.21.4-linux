package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.util.entity.PlayerInventoryComponent;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.features.draggables.Notifications;
import ru.zenith.implement.features.modules.render.Hud;
import ru.zenith.interfaces.IArmorItem;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoArmor extends Module {

    public AutoArmor() {
        super("AutoArmor", "Auto Armor", ModuleCategory.PLAYER);
        setup();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (PlayerInventoryUtil.isServerScreen()) return;
        if (!PlayerInventoryComponent.script.isFinished()) return;
        List<Runnable> list = new ArrayList<>();
        for (EquipmentSlot equipment : EquipmentSlot.values()) {
            ItemStack equipStack = mc.player.getInventory().getArmorStack(equipment.getEntitySlotId());
            if (equipment.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (equipment.equals(EquipmentSlot.CHEST) && equipStack.getItem().equals(Items.ELYTRA)) continue;

            int armorSlot = 8 - equipment.getEntitySlotId();

            Slot slot = PlayerInventoryUtil.getSlot(s -> {
                ItemStack stack = s.getStack();
                return s.id != armorSlot && !isBroken(stack) && !hasCurseOfBinding(stack) && stack.getItem() instanceof ArmorItem armorItem && ((IArmorItem) armorItem).zov_pidarok$getType().getEquipmentSlot().equals(equipment);
            }, Comparator.comparingDouble(s -> calculateArmorValue(s.getStack(), (IArmorItem) s.getStack().getItem())));

            if (slot != null && isBetter(slot.getStack(), equipStack)) {
                list.add(() -> PlayerInventoryUtil.moveItem(slot, armorSlot));
                continue;
            }

            if (isBroken(equipStack)) {
                Hud hud = Hud.getInstance();
                if (slot != null) {
                    list.add(() -> PlayerInventoryUtil.moveItem(slot, armorSlot));
                    if (hud.state && hud.notificationSettings.isSelected("Auto Armor"))
                        Notifications.getInstance().addList(Text.literal("Заменил - " + Formatting.GREEN + equipName(equipment) + Formatting.RESET + " на ").append(equipStack.getName()), 3000);
                } else if (PlayerInventoryUtil.getSlot(Items.AIR, s -> s.id >= 9) != null) {
                    list.add(() -> PlayerInventoryUtil.clickSlot(armorSlot, 0, SlotActionType.QUICK_MOVE,false));
                    if (hud.state && hud.notificationSettings.isSelected("Auto Armor"))
                        Notifications.getInstance().addList(Text.literal("Засейвил - ").append(equipStack.getName()), 3000);
                }
            }
        }
        if (!list.isEmpty()) PlayerInventoryComponent.addTask(() -> {
            list.forEach(Runnable::run);
            PlayerInventoryUtil.updateSlots();
        });
    }

    private float calculateArmorValue(ItemStack stack, IArmorItem armorItem) {
        Registry<Enchantment> enchantments = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        ArmorMaterial material = armorItem.zov_pidarok$getMaterial();

        float protection = material.defense().getOrDefault(armorItem.zov_pidarok$getType(), 0) + material.toughness() + EnchantmentHelper.getLevel(enchantments.getEntry(Enchantments.PROTECTION.getValue()).orElseThrow(), stack);
        float unbreaking = EnchantmentHelper.getLevel(enchantments.getEntry(Enchantments.UNBREAKING.getValue()).orElseThrow(), stack);
        float mending = EnchantmentHelper.getLevel(enchantments.getEntry(Enchantments.MENDING.getValue()).orElseThrow(), stack);

        return protection + unbreaking * 0.1f + mending * 0.2f;
    }

    private boolean hasCurseOfBinding(ItemStack stack) {
        RegistryEntry<Enchantment> curseEntry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.BINDING_CURSE.getValue()).orElse(null);
        if (curseEntry == null) return false;
        return EnchantmentHelper.getLevel(curseEntry, stack) > 0;
    }

    private boolean isBroken(ItemStack stack) {
        return (double) stack.getDamage() / stack.getMaxDamage() > 0.98;
    }

    private boolean isBetter(ItemStack newArmor, ItemStack currentArmor) {
        if (currentArmor.isEmpty()) return true;
        if (!(newArmor.getItem() instanceof ArmorItem newItem) || !(currentArmor.getItem() instanceof ArmorItem currentItem))
            return false;

        return calculateArmorValue(newArmor, (IArmorItem) newItem) > calculateArmorValue(currentArmor, (IArmorItem) currentItem);
    }

    private String equipName(EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case FEET -> "Ботинки";
            case LEGS -> "Поножи";
            case CHEST -> "Нагрудник";
            case HEAD -> "Шлем";
            default -> "ДЕЗЗ СВИНЬЯ СУКААА!";
        };
    }
}
