package ru.zenith.implement.features.modules.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.types.EventType;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.implement.events.player.RotationUpdateEvent;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationConfig;
import ru.zenith.implement.features.modules.combat.killaura.rotation.RotationController;

import java.util.Comparator;
import java.util.Objects;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Nuker extends Module {
    public BlockPos pos;
    private VoxelShape shape;

    private final BooleanSetting rotateSetting = new BooleanSetting("Rotate", "Break blocks lower than the player").setValue(true);
    private final BooleanSetting downSetting = new BooleanSetting("Down", "Break blocks lower than the player").setValue(true);
    private final ValueSetting radiusSetting = new ValueSetting("Radius", "Breaks blocks in a radius around you").setValue(3).range(1, 6);

    public Nuker() {
        super("Nuker", ModuleCategory.PLAYER);
        setup(rotateSetting, downSetting, radiusSetting);
    }

    
    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (pos != null && shape != null && !shape.isEmpty())
            Render3DUtil.drawShape(pos, shape, ColorUtil.getClientColor(), 2);
    }

    
    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            pos = PlayerIntersectionUtil.getCube(mc.player.getBlockPos(), radiusSetting.getInt(), radiusSetting.getInt(), downSetting.isValue())
                    .stream().filter(this::validBlock).min(Comparator.comparingDouble(this::blockPriority)).orElse(null);

            if (pos != null) {
                if (rotateSetting.isValue()) RotationController.INSTANCE.rotateTo(AngleUtil.calculateAngle(pos.toCenterPos()), RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
                shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
                mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    
    private double blockPriority(BlockPos pos) {
        return switch (mc.world.getBlockState(pos).getBlock().getTranslationKey().replace("block.minecraft.", "")) {
            case "ancient_debris" -> 0;
            case "diamond_ore" -> 1;
            case "emerald_ore" -> 2;
            case "gold_ore" -> 3;
            case "iron_ore" -> 4;
            case "lapis_ore" -> 5;
            case "redstone_ore" -> 6;
            default -> mc.player.squaredDistanceTo(pos.toCenterPos());
        };
    }

    
    private boolean validBlock(BlockPos pos) {
        BlockState state = Objects.requireNonNull(mc.world).getBlockState(pos);
        return !PlayerIntersectionUtil.isAir(state) && state.getBlock() != Blocks.WATER && state.getBlock() != Blocks.LAVA && state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.BARRIER;
    }
}
