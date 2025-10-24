package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BindSetting;
import ru.zenith.api.repository.friend.FriendUtils;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.entity.PlayerInventoryUtil;
import ru.zenith.common.util.other.BooleanSettable;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.keyboard.HotBarScrollEvent;
import ru.zenith.implement.events.keyboard.KeyEvent;
import ru.zenith.implement.events.player.HotBarUpdateEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationConfig;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationController;
import ru.zenith.implement.features.modules.render.ProjectilePrediction;

import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class ClickAction extends Module {
    BindSetting expBind = new BindSetting("EXP Bottle","Throw Experience Bottle");
    BindSetting friendBind = new BindSetting("Friend Add","add/remove Friend");
    List<KeyBind> keyBindings = new ArrayList<>();
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();

    public ClickAction() {
        super("ClickAction","Click Action", ModuleCategory.MISC);
        keyBindings.add(new KeyBind(Items.ENDER_PEARL, new BindSetting("Ender Pearl", "Throw Ender Pearl"), new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.WIND_CHARGE, new BindSetting("Wind Charge", "Throw Wind Charge"), new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Any Buff", "Throw Any Buff"), new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.SPLASH_POTION, new BindSetting("Any Debuff", "Throw Any Debuff"), new BooleanSettable()));
      //  keyBindings.add(new KeyBind(Items.CROSSBOW, new BindSetting("Crossbow", "Shoots from a crossbow"), new BooleanSettable()));
        keyBindings.forEach(bind -> setup(bind.setting));
        setup(expBind, friendBind);
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (!script.isFinished()) e.cancel();
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (!script.isFinished()) e.cancel();
    }
    
    @EventHandler
    public void onKey(KeyEvent e) {
        if (e.isKeyDown(friendBind.getKey()) && mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof PlayerEntity player) {
            if (FriendUtils.isFriend(player)) FriendUtils.removeFriend(player);
            else FriendUtils.addFriend(player);
        }
        keyBindings.stream().filter(bind -> e.isKeyReleased(bind.setting.getKey())).forEach(this::swapAndUse);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        List<ItemStack> stacks = keyBindings.stream().filter(bind -> PlayerIntersectionUtil.isKey(bind.setting) && PlayerInventoryUtil.getSlot(bind.item) != null).map(s -> s.item.getDefaultStack()).toList();
        ProjectilePrediction.getInstance().drawPredictionInHand(e.getStack(), stacks, AngleUtil.cameraAngle());
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (PlayerIntersectionUtil.isKey(expBind)) {
            Slot slot = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE);
            if (slot == null) return;

            RotationController.INSTANCE.rotateTo(AngleUtil.pitch(75), new RotationConfig(true, false), TaskPriority.HIGH_IMPORTANCE_2, this);
            if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
                if (stopWatch.every(250)) {
                    PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true);
                    if (script.isFinished()) script.cleanup().addTickStep(0, () -> PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true));
                }
            } else {
                PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, AngleUtil.pitch(75));
                stopWatch.reset();
            }
        } else if (!script.isFinished() && stopWatch.every(250)) {
            script.update();
        }
    }

    @Compile
    public void swapAndUse(KeyBind bind) {
        switch (bind.setting.getName()) {
            case "Any Buff" -> {
                Slot slot = PlayerInventoryUtil.getPotionFromCategory(StatusEffectCategory.BENEFICIAL);
                PlayerInventoryUtil.swapAndUse(slot, "Бафф", true);
            }
            case "Any Debuff" -> {
                Slot slot = PlayerInventoryUtil.getPotionFromCategory(StatusEffectCategory.HARMFUL);
                PlayerInventoryUtil.swapAndUse(slot, "Дебафф", true);
            }
            default -> PlayerInventoryUtil.swapAndUse(bind.item);
        }
    }

    public record KeyBind(Item item, BindSetting setting, BooleanSettable draw) {}
}