package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.api.system.sound.SoundManager;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.implement.events.container.SetScreenEvent;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Notifications extends AbstractDraggable {
    public static Notifications getInstance() {
        return Instance.getDraggable(Notifications.class);
    }
    private final List<Notification> list = new ArrayList<>();
    private final List<Stack> stacks = new ArrayList<>();

    public Notifications() {
        super("Notifications", 0, 50, 100, 15,true);
    }

    @Override
    public void tick() {
        list.forEach(notif -> {
            if (System.currentTimeMillis() > notif.removeTime || (notif.text.getString().contains("Пример Уведомления") && !PlayerIntersectionUtil.isChat(mc.currentScreen)))
                notif.anim.setDirection(Direction.BACKWARDS);
        });
        list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));
        while (!stacks.isEmpty()) {
            addTextIfNotEmpty(TypePickUp.INVENTORY, "Подняты предметы: ");
            addTextIfNotEmpty(TypePickUp.SHULKER_INVENTORY, "Сложены предметы в шалкер: ");
            addTextIfNotEmpty(TypePickUp.SHULKER, "Поднят шалкер с: ");
        }
    }

    @Override
    public void packet(PacketEvent e) {
        if (!PlayerIntersectionUtil.nullCheck()) switch (e.getPacket()) {
            case ItemPickupAnimationS2CPacket item when Hud.getInstance().notificationSettings.isSelected("Item Pick Up") && item.getCollectorEntityId()
                    == Objects.requireNonNull(mc.player).getId() && Objects.requireNonNull(mc.world).getEntityById(item.getEntityId()) instanceof ItemEntity entity -> {
                ItemStack itemStack = entity.getStack();
                ContainerComponent component = itemStack.get(DataComponentTypes.CONTAINER);
                if (component == null) {
                    Text itemText = itemStack.getName();
                    if (itemText.getContent().toString().equals("empty")) {
                        MutableText text = Text.empty().append(itemText);
                        if (itemStack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + itemStack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                        stacks.add(new Stack(TypePickUp.INVENTORY, text));
                    }
                } else component.stream().filter(s -> s.getName().getContent().toString().equals("empty")).forEach(stack -> {
                    MutableText text = Text.empty().append(stack.getName());
                    if (stack.getCount() > 1) text.append(Formatting.RESET + " [" + Formatting.RED + stack.getCount() + Formatting.GRAY + "x" + Formatting.RESET + "]");
                    stacks.add(new Stack(TypePickUp.SHULKER, text));
                });
            }
            case ScreenHandlerSlotUpdateS2CPacket slot when Hud.getInstance().notificationSettings.isSelected("Item Pick Up") -> {
                int slotId = slot.getSlot();
                ContainerComponent updatedContainer = slot.getStack().get(DataComponentTypes.CONTAINER);
                if (updatedContainer != null && slotId < Objects.requireNonNull(mc.player).currentScreenHandler.slots.size() && slot.getSyncId() == 0) {
                    ContainerComponent currentContainer = mc.player.currentScreenHandler.getSlot(slotId).getStack().get(DataComponentTypes.CONTAINER);
                    if (currentContainer != null) updatedContainer.stream().filter(stack -> currentContainer.stream().noneMatch(s -> Objects.equals(s.getComponents(), stack.getComponents()) && s.toString().equals(stack.toString()))).forEach(stack -> {
                        MutableText text = Text.empty().append(stack.getName());
                        stacks.add(new Stack(TypePickUp.SHULKER_INVENTORY, text));
                    });
                }
            }
            default -> {}
        }
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        if (e.getScreen() instanceof ChatScreen) {
            addList("Пример Уведомления",99999999);
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT); // Larger readable font

        setX((window.getScaledWidth() - getWidth()) / 2);

        float offsetY = 0;
        float offsetX = 4;
        for (Notification notification : list) {
            float anim = notification.anim.getOutput().floatValue();
            float width = font.getStringWidth(notification.text) + offsetX * 2;
            float startY = getY() + offsetY;
            float startX = getX() + (getWidth() - width) / 2;
            final float finalOffsetY = offsetY; // Create final copy for lambda

            MathUtil.setAlpha(anim, () -> {
                // ScoreBoard style background with blur and rounded corners
                blur.render(ShapeProperties.create(matrix, getX(), getY() + finalOffsetY, getWidth(), getHeight())
                        .round(4).thickness(2).softness(1).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());
                
                font.drawText(matrix, notification.text, (int) (startX + offsetX), startY + 4);
            });
            offsetY += (getHeight() + 2) * anim;
        }
    }

    private void addTextIfNotEmpty(TypePickUp type, String prefix) {
        MutableText text = Text.empty();
        List<Stack> list = stacks.stream().filter(stack -> stack.type.equals(type)).toList();
        for (int i = 0, size = list.size(); i < size; i++) {
            Stack stack = list.get(i);
            if (stack.type != type) continue;
            text.append(stack.text);
            stacks.remove(stack);
            if (text.getString().length() > 150) break;
            if (i + 1 != size) text.append(" ,  ");
        }
        if (!text.equals(Text.empty())) addList(Text.empty().append(prefix).append(text), 8000);
    }

    public void addList(String text, long removeTime) {
        addList(text,removeTime,null);
    }

    public void addList(Text text, long removeTime) {
        addList(text,removeTime,null);
    }

    public void addList(String text, long removeTime, SoundEvent sound) {
        addList(Text.empty().append(text), removeTime, sound);
    }

    public void addList(Text text, long removeTime, SoundEvent sound) {
        list.add(new Notification(text, new DecelerateAnimation().setMs(300).setValue(1), System.currentTimeMillis() + removeTime));
        if (list.size() > 12) list.removeFirst();
        list.sort(Comparator.comparingDouble(notif -> -notif.removeTime));
        if (sound != null) SoundManager.playSound(sound);
    }

    public record Notification(Text text, Animation anim, long removeTime) {}
    public record Stack(TypePickUp type, MutableText text) {}
    public enum TypePickUp {
        INVENTORY, SHULKER, SHULKER_INVENTORY
    }
}
