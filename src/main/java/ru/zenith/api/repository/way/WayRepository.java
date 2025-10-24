package ru.zenith.api.repository.way;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.EventManager;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.QuickImports;
import ru.zenith.common.QuickLogger;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.math.ProjectionUtil;
import ru.zenith.implement.events.render.DrawEvent;

import java.util.ArrayList;
import java.util.List;

public class WayRepository implements QuickImports, QuickLogger {
    public WayRepository(EventManager eventManager) {
        eventManager.register(this);
    }

    public List<Way> wayList = new ArrayList<>();

    public boolean isEmpty() {
        return wayList.isEmpty();
    }

    public void addWay(String name, BlockPos pos, String server) {
        wayList.add(new Way(name, pos, server));
    }

    public boolean hasWay(String text) {
        return wayList.stream().anyMatch(s -> s.name().equalsIgnoreCase(text));
    }

    public void deleteWay(String name) {
        wayList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
    }

    public void clearList() {
        if (!isEmpty()) wayList.clear();
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        if (isEmpty() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null) return;

        MatrixStack matrix = e.getDrawContext().getMatrices();

        wayList.forEach(way -> {
            Vec3d wayVec = way.pos().toCenterPos();
            Vec3d vec = ProjectionUtil.worldSpaceToScreenSpace(wayVec);

            if (ProjectionUtil.canSee(wayVec) && way.server().equalsIgnoreCase(mc.getNetworkHandler().getServerInfo().address)) {
                String text = way.name() + " - " + MathUtil.round(mc.getEntityRenderDispatcher().camera.getPos().distanceTo(wayVec),0.1F) + "m";
                FontRenderer font = Fonts.getSize(14);
                float height = font.getStringHeight(text) / 4;
                float width = font.getStringWidth(text);
                float padding = 3;
                double x = vec.getX() - width / 2;
                double y = vec.getY() - height / 2;

                blur.render(ShapeProperties.create(matrix, x - padding,y - padding,width + padding * 2,height + padding * 2)
                        .round(2).softness(1).color(ColorUtil.HALF_BLACK).build());
                font.drawString(matrix,text,x,y,ColorUtil.getText());
            }
        });
    }
}