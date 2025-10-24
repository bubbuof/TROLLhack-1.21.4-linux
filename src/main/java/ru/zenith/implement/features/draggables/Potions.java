package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.implement.events.packet.PacketEvent;

import java.util.*;

public class Potions extends AbstractDraggable {
    private final List<Potion> list = new ArrayList<>();
    public Potions() {
        super("Potions", 210, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        list.removeIf(p -> p.anim.isFinished(Direction.BACKWARDS));
        list.forEach(p -> p.effect.update(mc.player,null));
    }

    @Override
    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case EntityStatusEffectS2CPacket effect -> {
                if (!PlayerIntersectionUtil.nullCheck() && effect.getEntityId() == Objects.requireNonNull(mc.player).getId()) {
                    RegistryEntry<StatusEffect> effectId = effect.getEffectId();
                    list.stream().filter(p -> p.effect.getEffectType().getIdAsString().equals(effectId.getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
                    list.add(new Potion(new StatusEffectInstance(effectId, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon()), new DecelerateAnimation().setMs(150).setValue(1.0F)));
                }
            }
            case RemoveEntityStatusEffectS2CPacket effect -> list.stream().filter(s -> s.effect.getEffectType().getIdAsString().equals(effect.effect().getIdAsString())).forEach(s -> s.anim.setDirection(Direction.BACKWARDS));
            case PlayerRespawnS2CPacket p -> list.clear();
            case GameJoinS2CPacket p -> list.clear();
            default -> {}
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT); // Larger readable font
        FontRenderer fontPotion = Fonts.getSize(13, Fonts.Type.DEFAULT);

        // ScoreBoard style background with blur and rounded corners
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(4).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

        float centerX = getX() + getWidth() / 2.0F;
        int offset = 16, maxWidth = 80;

        // ScoreBoard style title
        font.drawText(matrix, Text.of(getName().toLowerCase()), (int) (centerX - font.getStringWidth(getName().toLowerCase()) / 2.0F), getY() + 4);
        
        for (Potion potion : list) {
            StatusEffectInstance effect = potion.effect;
            float animation = potion.anim.getOutput().floatValue();
            float centerY = getY() + offset;
            int amplifier = effect.getAmplifier();

            String name = effect.getEffectType().value().getName().getString();
            String duration = getDuration(effect);
            String lvl = amplifier > 0 ? " " + (amplifier + 1) : "";

            MathUtil.scale(matrix, centerX, centerY, 1, animation, () -> {
                // Simple warning color for low duration
                int durationColor = effect.getDuration() != -1 && effect.getDuration() <= 120 ? 
                        ColorUtil.getColor(255, 150, 150) : ColorUtil.getClientColor();
                
                // Simple potion icon
                Render2DUtil.drawSprite(matrix, mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType()), getX() + 3, (int) centerY - 1, 6, 6);
                
                // Simple potion name with level
                fontPotion.drawString(matrix, name + lvl, getX() + 12, centerY, ColorUtil.getColor(200, 200, 200));
                
                // Simple duration
                fontPotion.drawString(matrix, duration, getX() + getWidth() - 4 - fontPotion.getStringWidth(duration), centerY, durationColor);
            });

            int width = (int) fontPotion.getStringWidth(name + lvl + duration) + 25;
            maxWidth = Math.max(width, maxWidth);
            offset += (int) (10 * animation);
        }

        setWidth(maxWidth);
        setHeight(offset + 2);
    }

    private String getDuration(StatusEffectInstance pe) {
        int var1 = pe.getDuration();
        int mins = var1 / 1200;
        return pe.isInfinite() || mins > 60 ? "**:**": mins + ":" + String.format("%02d", (var1 % 1200) / 20);
    }

    private record Potion(StatusEffectInstance effect, Animation anim) {}
}
