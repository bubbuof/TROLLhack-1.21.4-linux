package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.implement.events.packet.PacketEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoolDowns extends AbstractDraggable {
    public static CoolDowns getInstance() {
        return Instance.getDraggable(CoolDowns.class);
    }
    public final List<CoolDown> list = new ArrayList<>();

    public CoolDowns() {
        super("Cool Downs", 120, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(c -> c.anim.isFinished(Direction.BACKWARDS));
        list.stream().filter(c -> !Objects.requireNonNull(mc.player).getItemCooldownManager().isCoolingDown(c.item.getDefaultStack())).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
    }

   @Override
    public void packet(PacketEvent e) {
        if (PlayerIntersectionUtil.nullCheck()) return;
        switch (e.getPacket()) {
            case CooldownUpdateS2CPacket c -> {
                Item item = Registries.ITEM.get(c.cooldownGroup());
                list.stream().filter(coolDown -> coolDown.item.equals(item)).forEach(coolDown -> coolDown.anim.setDirection(Direction.BACKWARDS));
                if (c.cooldown() != 0) {
                    list.add(new CoolDown(item, new StopWatch().setMs(-c.cooldown() * 50L), new DecelerateAnimation().setMs(150).setValue(1.0F)));
                }
            }
            case PlayerRespawnS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();

        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer fontCoolDown = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // GameSense style background
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());
        
        // Top accent line
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        float centerX = getX() + getWidth() / 2.0F;
        font.drawString(matrix, getName().toLowerCase(), (int) (centerX - font.getStringWidth(getName().toLowerCase()) / 2.0F), getY() + 4, ColorUtil.WHITE);

        int offset = 16;
        int maxWidth = 80;
        for (CoolDown coolDown : list) {
            float animation = coolDown.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            int time = -coolDown.time.elapsedTime() / 1000;
            String name = coolDown.item.getDefaultStack().getName().getString();
            String duration = StringUtil.getDuration(time);

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                // Item icon - smaller
                Render2DUtil.defaultDrawStack(context, coolDown.item.getDefaultStack(), getX() + 3, centerY - 2, false, false, 0.4F);
                
                // Item name
                fontCoolDown.drawString(matrix, name, getX() + 14, centerY, ColorUtil.getColor(200, 200, 200));
                
                // Duration with warning color for low time
                int durationColor = time <= 5 ? ColorUtil.getColor(255, 150, 150) : ColorUtil.getClientColor();
                fontCoolDown.drawString(matrix, duration, getX() + getWidth() - 4 - fontCoolDown.getStringWidth(duration), centerY, durationColor);
            });

            int width = (int) fontCoolDown.getStringWidth(name + duration) + 25;
            maxWidth = Math.max(width, maxWidth);
            offset += (int) (10 * animation);
        }

        setWidth(maxWidth);
        setHeight(offset + 2);
    }

    public record CoolDown(Item item, StopWatch time, Animation anim) {}
}
