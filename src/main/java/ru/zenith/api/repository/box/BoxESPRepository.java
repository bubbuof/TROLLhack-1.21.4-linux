package ru.zenith.api.repository.box;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.EventManager;
import ru.zenith.common.QuickImports;
import ru.zenith.common.QuickLogger;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.math.ProjectionUtil;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.implement.events.block.BlockUpdateEvent;
import ru.zenith.implement.events.render.WorldLoadEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.util.HashMap;
import java.util.Map;

public class BoxESPRepository implements QuickImports, QuickLogger {
    private final Map<BlockPos, Pair<VoxelShape, Integer>> boxes = new HashMap<>();
    public final Map<EntityType<?>, Integer> entities = new HashMap<>();
    public final Map<Block, Integer> blocks = new HashMap<>();
    public boolean drawFill = true;

    public BoxESPRepository(EventManager eventManager) {
        eventManager.register(this);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        boxes.forEach((pos, pair) -> {
            if (drawFill) Render3DUtil.drawShape(pos, pair.getLeft(), pair.getRight(), 1);
            else Render3DUtil.drawShapeAlternative(pos, pair.getLeft(), pair.getRight(), 1, false, false);
        });
        PlayerIntersectionUtil.streamEntities().filter(ent -> entities.containsKey(ent.getType()) && ent != mc.player).forEach(ent -> {
            int entityColor = entities.get(ent.getType());
            int color = entityColor == 0 ? ColorUtil.getClientColor() : entityColor;
            Box box = ent.getBoundingBox().offset(MathUtil.interpolate(ent).subtract(ent.getPos()));
            if (ProjectionUtil.canSee(box)) Render3DUtil.drawBox(box, color, 1);
        });
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        boxes.clear();
    }

    @EventHandler
    public void onBlockUpdate(BlockUpdateEvent e) {
        BlockPos pos = e.pos();
        BlockState state = e.state();
        Block block = state.getBlock();
        switch (e.type()) {
            case LOAD -> {
                if (blocks.containsKey(block)) putBox(pos, state, block);
            }
            case UPDATE -> {
                if (blocks.containsKey(block) && !boxes.containsKey(pos)) putBox(pos, state, block);
                if (boxes.containsKey(pos) && (state.isAir() || !boxes.get(pos).getLeft().equals(state.getOutlineShape(mc.world, pos)))) boxes.remove(pos);
            }
            case UNLOAD -> boxes.remove(pos);
        }
    }

    private void putBox(BlockPos pos, BlockState state, Block block) {
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        int blockColor = blocks.get(block);
        int color = blockColor == 0 ? ColorUtil.replAlpha(state.getMapColor(mc.world, pos).color, 1F) : blockColor;
        boxes.put(pos, new Pair<>(shape, color));
    }
}
