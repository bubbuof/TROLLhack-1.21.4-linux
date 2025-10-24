package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.entity.PlayerInventoryComponent;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.item.ItemUsage;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.player.TickEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoUse extends Module {
    Script script = new Script();

    MultiSelectSetting multiSetting = new MultiSelectSetting("Mode", "Choose what will be used").value("Eat", "Invisibility");

    public AutoUse() {
        super("AutoUse", "Auto Use", ModuleCategory.PLAYER);
        setup(multiSetting);
    }

    @Override
    public void deactivate() {
        script.update();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        for (String string : multiSetting.getSelected())
            switch (string) {
                case "Eat" -> {
                    Slot slot = PlayerInventoryUtil.getFoodMaxSaturationSlot();
                    if (slot != null && mc.player.getHungerManager().isNotFull() && swapAndEat(slot)) {
                        return;
                    }
                }
                case "Invisibility" -> {
                    Slot slot = PlayerInventoryUtil.getPotion(StatusEffects.INVISIBILITY);
                    if (slot != null && !PlayerIntersectionUtil.isPotionActive(StatusEffects.INVISIBILITY) && swapAndEat(slot)) {
                        return;
                    }
                }
            }
        script.update();
    }

    public boolean swapAndEat(Slot slot) {
        ItemStack stack = slot.getStack();
        if (!mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            if (!mc.player.getOffHandStack().equals(stack)) {
                if (PlayerInventoryComponent.script.isFinished()) {
                    PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true, true);
                    script.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(slot, Hand.OFF_HAND, true, true));
                }
            } else {
                ItemUsage.INSTANCE.useHand(Hand.OFF_HAND);
            }
            return true;
        }
        return false;
    }
}
