package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BindSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.implement.events.keyboard.KeyEvent;

import java.util.Comparator;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoSwap extends Module {
    BindSetting bind = new BindSetting("Item use key", "Uses item when pressed");

    SelectSetting firstItem = new SelectSetting("First item", "Select first swap item.")
            .value("Totem of Undying", "Player Head", "Golden Apple", "Shield");

    SelectSetting secondItem = new SelectSetting("Second item", "Select second swap item.")
            .value("Totem of Undying", "Player Head", "Golden Apple", "Shield");

    public AutoSwap() {
        super("AutoSwap", "Auto Swap", ModuleCategory.COMBAT);
        setup(firstItem, secondItem, bind);
    }

    
    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(bind.getKey())) {
            Slot first = PlayerInventoryUtil.getSlot(getItemByType(firstItem.getSelected()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
            Slot second = PlayerInventoryUtil.getSlot(getItemByType(secondItem.getSelected()), Comparator.comparing(s -> s.getStack().hasEnchantments()), s -> s.id != 46 && s.id != 45);
            Slot validSlot = first != null && mc.player.getOffHandStack().getItem() != first.getStack().getItem() ? first : second;
            PlayerInventoryUtil.swapHand(validSlot, Hand.OFF_HAND, true, true);
        }
    }

    
    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Totem of Undying" -> Items.TOTEM_OF_UNDYING;
            case "Player Head" -> Items.PLAYER_HEAD;
            case "Golden Apple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }
}
