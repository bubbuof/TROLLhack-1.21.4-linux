package ru.zenith.implement.features.modules.misc;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.implement.events.player.TickEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.common.util.render.Render3DUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AncientXray extends Module {

    private static AncientXray instance;

    public static AncientXray getInstance() {
        if (instance == null) {
            instance = new AncientXray();
        }
        return instance;
    }

    final ValueSetting searchRadius = new ValueSetting("Search Radius", "Радиус поиска обломков")
            .range(4.0f, 64.0f)
            .setValue(20.0f);

    final BooleanSetting autoClick = new BooleanSetting("Auto Click", "Автоматически кликать по обломкам")
            .setValue(true);

    final ValueSetting clickDelayMs = new ValueSetting("Click Delay", "Задержка между кликами")
            .range(50.0f, 2000.0f)
            .setValue(500.0f);

    final List<BlockPos> highlightedDebris = new ArrayList<>();
    final Set<BlockPos> clicked = new HashSet<>();
    long lastClickAt = 0L;
    int scanTicker = 0;

    public AncientXray() {
        super("AncientXray", "AncientXray", ModuleCategory.MISC);
        setup(searchRadius, autoClick, clickDelayMs);
        instance = this;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (scanTicker++ % 5 == 0) {
            scanForDebris();
        }

        if (autoClick.isValue() && !highlightedDebris.isEmpty() && mc.getNetworkHandler() != null) {
            long now = System.currentTimeMillis();
            if (now - lastClickAt >= clickDelayMs.getValue()) {
                for (BlockPos pos : highlightedDebris) {
                    if (!clicked.contains(pos)) {
                        try {
                            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
                                    pos,
                                    Direction.UP
                            ));
                            clicked.add(pos);
                            lastClickAt = now;
                        } catch (Throwable ignored) {
                        }
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!highlightedDebris.isEmpty()) {
            int color = 0xFFFFFFC8; // RGBA: 255, 255, 255, 200

            for (BlockPos pos : highlightedDebris) {
                Box box = new Box(pos);
                Render3DUtil.drawBox(box, color, 1.0F);
            }
        }
    }

    private void scanForDebris() {
        highlightedDebris.clear();
        if (mc.player == null || mc.world == null) return;

        BlockPos playerPos = new BlockPos(
                (int) mc.player.getX(),
                (int) mc.player.getY(),
                (int) mc.player.getZ()
        );
        int r = Math.max(1, (int) searchRadius.getValue());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (isTargetBlock(pos, block)) {
                        highlightedDebris.add(pos);
                    }
                }
            }
        }

        clicked.removeIf(pos -> !highlightedDebris.contains(pos));
    }

    private boolean isTargetBlock(BlockPos pos, Block block) {
        return block == Blocks.ANCIENT_DEBRIS &&
                hasAtLeastTwoAirBlocksAround(pos) &&
                !hasTwoQuartzOrGoldNearby(pos) &&
                hasAtLeastFiveAirInCube(pos) &&
                !hasTooManyAncientDebrisNearby(pos);
    }

    private boolean hasAtLeastTwoAirBlocksAround(BlockPos pos) {
        int airBlockCount = 0;

        for (Direction direction : Direction.values()) {
            Block surroundingBlock = mc.world.getBlockState(pos.offset(direction)).getBlock();
            if (surroundingBlock == Blocks.AIR || surroundingBlock == Blocks.CAVE_AIR) {
                airBlockCount++;
                if (airBlockCount >= 2) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasTwoQuartzOrGoldNearby(BlockPos pos) {
        int quartzOrGoldCount = 0;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearbyBlock = mc.world.getBlockState(pos.add(x, y, z)).getBlock();
                    if (nearbyBlock == Blocks.NETHER_QUARTZ_ORE || nearbyBlock == Blocks.NETHER_GOLD_ORE) {
                        quartzOrGoldCount++;
                        if (quartzOrGoldCount >= 4) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean hasAtLeastFiveAirInCube(BlockPos pos) {
        int airBlockCount = 0;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearbyBlock = mc.world.getBlockState(pos.add(x, y, z)).getBlock();
                    if (nearbyBlock == Blocks.AIR || nearbyBlock == Blocks.CAVE_AIR) {
                        airBlockCount++;
                        if (airBlockCount >= 4) {
                            return true;
                        }
                    }
                }
            }
        }

        return airBlockCount >= 4;
    }

    private boolean hasTooManyAncientDebrisNearby(BlockPos pos) {
        int ancientDebrisCount = 0;

        for (int x = -3; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 3; z++) {
                    Block nearbyBlock = mc.world.getBlockState(pos.add(x, y, z)).getBlock();
                    if (nearbyBlock == Blocks.ANCIENT_DEBRIS) {
                        ancientDebrisCount++;
                        if (ancientDebrisCount > 3) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}