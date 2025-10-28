package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
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

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FTHelper extends Module {

    // Настройки биндов для каждого предмета
    BindSetting enderEyeBind = new BindSetting("Ender Eye", "Use Ender Eye");
    BindSetting netheriteScrapBind = new BindSetting("Netherite Scrap", "Swap to Netherite Scrap");
    BindSetting snowballBind = new BindSetting("Snowball", "Throw Snowball");
    BindSetting driedKelpBind = new BindSetting("Dried Kelp", "Eat Dried Kelp");
    BindSetting phantomMembraneBind = new BindSetting("Phantom Membrane", "Use Phantom Membrane");

    List<KeyBind> keyBindings = new ArrayList<>();
    StopWatch stopWatch = new StopWatch();
    Script script = new Script();

    public FTHelper() {
        super("FTHelper", "FTHelper", ModuleCategory.MISC);

        // Инициализация биндов для каждого предмета
        keyBindings.add(new KeyBind(Items.ENDER_EYE, enderEyeBind, new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, netheriteScrapBind, new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.SNOWBALL, snowballBind, new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.DRIED_KELP, driedKelpBind, new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE, phantomMembraneBind, new BooleanSettable()));

        // Регистрация настроек
        keyBindings.forEach(bind -> setup(bind.setting));
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
        // Обработка нажатий клавиш для каждого бинда
        for (KeyBind bind : keyBindings) {
            if (e.isKeyReleased(bind.setting.getKey())) {
                handleItemAction(bind);
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        // Отображение предсказания для броскаемых предметов
        List<ItemStack> stacks = keyBindings.stream()
                .filter(bind -> PlayerIntersectionUtil.isKey(bind.setting) &&
                        PlayerInventoryUtil.getSlot(bind.item) != null &&
                        isThrowableItem(bind.item))
                .map(s -> s.item.getDefaultStack())
                .toList();

        ProjectilePrediction.getInstance().drawPredictionInHand(e.getStack(), stacks, AngleUtil.cameraAngle());
    }

    @EventHandler
    public void onTick(TickEvent e) {
        // Автоматическое использование некоторых предметов при зажатой клавише
        if (PlayerIntersectionUtil.isKey(enderEyeBind)) {
            handleEnderEye();
        } else if (PlayerIntersectionUtil.isKey(driedKelpBind)) {
            handleDriedKelp();
        } else if (!script.isFinished() && stopWatch.every(250)) {
            script.update();
        }
    }

    /**
     * Обработка действий для каждого предмета
     */
    private void handleItemAction(KeyBind bind) {
        if (bind.item == Items.ENDER_EYE) {
            handleEnderEye();
        } else if (bind.item == Items.NETHERITE_SCRAP) {
            handleNetheriteScrap();
        } else if (bind.item == Items.SNOWBALL) {
            handleSnowball();
        } else if (bind.item == Items.DRIED_KELP) {
            handleDriedKelp();
        } else if (bind.item == Items.PHANTOM_MEMBRANE) {
            handlePhantomMembrane();
        }
    }

    /**
     * Око эндера - использование с текущим прицелом
     */
    private void handleEnderEye() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.ENDER_EYE);
        if (slot == null) return;

        if (mc.player.getMainHandStack().getItem() != Items.ENDER_EYE) {
            if (stopWatch.every(250)) {
                PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true);
                if (script.isFinished()) {
                    script.cleanup().addTickStep(0, () ->
                            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true));
                }
            }
        } else {
            // Используем с текущим прицелом игрока
            PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, AngleUtil.cameraAngle());
            stopWatch.reset();
        }
    }

    /**
     * Незеритовый лом - быстрая смена
     */
    private void handleNetheriteScrap() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.NETHERITE_SCRAP);
        if (slot != null) {
            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true);
        }
    }

    /**
     * Снежок - бросок с текущим прицелом
     */
    private void handleSnowball() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.SNOWBALL);
        if (slot != null) {
            PlayerInventoryUtil.swapAndUse(slot, "Снежок", true);
        }
    }

    /**
     * Сушеная ламинария - использование с текущим прицелом
     */
    private void handleDriedKelp() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.DRIED_KELP);
        if (slot == null) return;

        if (mc.player.getMainHandStack().getItem() != Items.DRIED_KELP) {
            if (stopWatch.every(250)) {
                PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true);
                if (script.isFinished()) {
                    script.cleanup().addTickStep(0, () ->
                            PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, true, true));
                }
            }
        } else {
            // Используем с текущим прицелом игрока
            PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, AngleUtil.cameraAngle());
            stopWatch.reset();
        }
    }

    /**
     * Мембрана фантома - использование с текущим прицелом
     */
    private void handlePhantomMembrane() {
        Slot slot = PlayerInventoryUtil.getSlot(Items.PHANTOM_MEMBRANE);
        if (slot != null) {
            PlayerInventoryUtil.swapAndUse(slot, "Мембрана фантома", true);
        }
    }

    /**
     * Проверка является ли предмет бросаемым
     */
    private boolean isThrowableItem(Item item) {
        return item == Items.ENDER_EYE || item == Items.SNOWBALL || item == Items.EXPERIENCE_BOTTLE;
    }

    public record KeyBind(Item item, BindSetting setting, BooleanSettable draw) {}
}