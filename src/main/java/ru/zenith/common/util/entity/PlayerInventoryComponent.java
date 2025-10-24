package ru.zenith.common.util.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.common.util.world.ServerUtil;
import ru.zenith.implement.events.player.InputEvent;
import ru.zenith.implement.events.player.PostMotionEvent;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationConfig;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationController;
import ru.zenith.implement.screens.menu.MenuScreen;

import java.util.List;

@UtilityClass
public class PlayerInventoryComponent implements QuickImports {
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public static final Script script = new Script(), postScript = new Script();
    public boolean canMove = true;

    public void tick() {
        script.update();
    }

    public void postMotion() {
        postScript.update();
    }

    public void input(InputEvent e) {
        if (!canMove) e.inputNone();
    }

    public void addTask(Runnable task) {
        if (script.isFinished() && MovingUtil.hasPlayerMovement()) {
            switch (ServerUtil.server) {
                case "FunTime" -> {
                    script.cleanup().addTickStep(0, () -> {
                        PlayerInventoryComponent.disableMoveKeys();
                        PlayerInventoryComponent.rotateToCamera();
                    }).addTickStep(1, () -> {
                        task.run();
                        enableMoveKeys();
                    });
                    return;
                }
                case "ReallyWorld" -> {
                    if (mc.player.isOnGround()) {
                        script.cleanup().addTickStep(0, PlayerInventoryComponent::disableMoveKeys).addTickStep(2, PlayerInventoryComponent::rotateToCamera).addTickStep(3, task::run)
                                .addTickStep(4, PlayerInventoryComponent::enableMoveKeys);
                        return;
                    }
                }
                case "SpookyTime", "CopyTime" -> {
                    script.cleanup().addTickStep(0, ()-> {
                                PlayerInventoryComponent.disableMoveKeys();
                                PlayerInventoryComponent.rotateToCamera();
                            }).addTickStep(1, task::run)
                            .addTickStep(2, PlayerInventoryComponent::enableMoveKeys);
                    return;
                }
            }
        }
        script.addTickStep(0, PlayerInventoryComponent::rotateToCamera);
        postScript.cleanup().addTickStep(0, () -> {
            task.run();
            PlayerInventoryUtil.closeScreen(true);
        });
    }

    private void rotateToCamera() {
        Module module = new Module("InventoryComponent","Inventory Component", ModuleCategory.PLAYER);
        module.state = true;
        RotationController.INSTANCE.rotateTo(AngleUtil.cameraAngle(), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, module);
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        PlayerInventoryUtil.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
    }

    public boolean shouldSkipExecution() {
        return mc.currentScreen != null && !PlayerIntersectionUtil.isChat(mc.currentScreen) && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !(mc.currentScreen instanceof MenuScreen);
    }
}