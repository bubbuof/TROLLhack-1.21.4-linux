package ru.zenith.common.util.item;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.*;
import ru.zenith.common.QuickImports;

@UtilityClass
public class ItemUtil implements QuickImports {

    public int maxUseTick(Item item) {
        return maxUseTick(item.getDefaultStack());
    }

    public int maxUseTick(ItemStack stack) {
        return switch (stack.getUseAction()) {
            case EAT, DRINK -> 32;
            case CROSSBOW, SPEAR -> 10;
            case BOW -> 20;
            case BLOCK -> 0;
            default -> stack.getMaxUseTime(mc.player);
        };
    }

    public float getCooldownProgress(Item item) {
        ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
        ItemCooldownManager.Entry entry = cooldownManager.entries.get(item);
        if (entry == null) return 0;
        return Math.max(0, (entry.endTick - cooldownManager.tick) / 20F);
    }
}
