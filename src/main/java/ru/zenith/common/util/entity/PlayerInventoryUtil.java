package ru.zenith.common.util.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import lombok.experimental.UtilityClass;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PickItemFromEntityC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.item.ItemUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.features.draggables.Notifications;
import ru.zenith.implement.features.modules.combat.killaura.rotation.Angle;
import ru.zenith.implement.features.modules.combat.killaura.rotation.AngleUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class PlayerInventoryUtil implements QuickImports {

    public void moveItem(Slot from, int to) {
        if (from != null) moveItem(from.id, to, false, false);
    }

    public void moveItem(Slot from, int to, boolean task) {
        moveItem(from, to, task, false);
    }

    public void moveItem(Slot from, int to, boolean task, boolean updateInventory) {
        if (from != null) moveItem(from.id, to, task, updateInventory);
    }

    public void moveItem(int from, int to, boolean task, boolean updateInventory) {
        if (from == to || from == -1) return;

        int count = Math.toIntExact(slots().count()) - 10;
        if (from >= count && count == 36) {
            if (task) PlayerInventoryComponent.addTask(() -> clickSlot(to, from - count, SlotActionType.SWAP, false));
            else clickSlot(to, from - count, SlotActionType.SWAP, false);
            return;
        }

        if (task) PlayerInventoryComponent.addTask(() -> moveItem(from, to, updateInventory));
        else moveItem(from, to, updateInventory);
    }

    public void moveItem(int from, int to, boolean updateInventory) {
        clickSlot(from, 0, SlotActionType.SWAP, false);
        clickSlot(to, 0, SlotActionType.SWAP, false);
        clickSlot(from, 0, SlotActionType.SWAP, false);
        if (updateInventory) updateSlots();
    }

    public void swapHand(Slot slot, Hand hand, boolean task) {
        swapHand(slot, hand, task,false);
    }

    public void swapHand(Slot slot, Hand hand, boolean task, boolean updateInventory) {
        if (slot == null || slot.id == -1 || (hand.equals(Hand.OFF_HAND) && !(slot.inventory instanceof PlayerInventory || slot.inventory instanceof EnderChestInventory))) return;
        int button = hand.equals(Hand.MAIN_HAND) ? mc.player.getInventory().selectedSlot : 40;
        if (task) PlayerInventoryComponent.addTask(() -> swap(slot, button, updateInventory));
        else swap(slot, button, updateInventory);
    }

    public void swap(Slot slot, int button, boolean updateInventory) {
        clickSlot(slot, button, SlotActionType.SWAP, false);
        if (updateInventory) PlayerInventoryUtil.updateSlots();
    }

    public void swapAndUse(Slot slot, String text, boolean task) {
        if (slot == null) {
            Notifications.getInstance().addList(Formatting.RED + text + Formatting.RESET + " - не найден!", 3000);
            return;
        }
        if (task) PlayerInventoryComponent.addTask(() -> swapAndUse(slot, AngleUtil.cameraAngle()));
        else swapAndUse(slot, AngleUtil.cameraAngle());
    }

    public void swapAndUse(Item item) {
        swapAndUse(item, AngleUtil.cameraAngle(), true);
    }

    public void swapAndUse(Item item, Angle angle, boolean task) {
        float cooldownProgress = ItemUtil.getCooldownProgress(item);

        if (cooldownProgress > 0) {
            String time = MathUtil.round(cooldownProgress, 0.1) + "с";
            Notifications.getInstance().addList(Formatting.RED + item.getName().getString() + Formatting.RESET + " - в кд еще " + time, 2000);
            return;
        }

        Slot slot = getSlot(item);
        if (slot == null) {
            Notifications.getInstance().addList(Formatting.RED + item.getName().getString() + Formatting.RESET + " - не найден!", 2000);
            return;
        }

        if (task) PlayerInventoryComponent.addTask(() -> swapAndUse(slot, angle));
        else swapAndUse(slot, angle);
    }

    public void swapAndUse(Slot slot, Angle angle) {
        swapHand(slot, Hand.MAIN_HAND, false);
        PlayerIntersectionUtil.interactItem(Hand.MAIN_HAND, angle);
        swapHand(slot, Hand.MAIN_HAND, false,true);
    }

    public void updateSlots() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        ItemStack stack = Registries.ITEM.get(MathUtil.getRandom(0, 100)).getDefaultStack();
        mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(screenHandler.syncId, screenHandler.getRevision(), 0, 0, SlotActionType.PICKUP_ALL, stack, Int2ObjectMaps.singleton(0, stack)));
    }

    public void closeScreen(boolean packet) {
        if (packet) mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        else mc.player.closeHandledScreen();
    }

    public void clickSlot(Slot slot, int button, SlotActionType clickType, boolean silent) {
        if (slot != null) clickSlot(slot.id, button, clickType, silent);
    }

    public void clickSlot(int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId, clickType, silent);
    }

    public void clickSlot(int windowId, int slotId, int buttonId, SlotActionType clickType, boolean silent) {
        mc.interactionManager.clickSlot(windowId, slotId, buttonId, clickType, mc.player);
        if (silent) mc.player.currentScreenHandler.onSlotClick(slotId, buttonId, clickType, mc.player);
    }

    public Slot getSlot(Item item) {
        return getSlot(item,s -> true);
    }

    public Slot getSlot(Item item, Predicate<Slot> filter) {
        return getSlot(item, Comparator.comparingInt(s -> 0), filter);
    }

    public Slot getSlot(Predicate<Slot> filter) {
        return slots().filter(filter).findFirst().orElse(null);
    }

    public Slot getSlot(Predicate<Slot> filter, Comparator<Slot> comparator) {
        return slots().filter(filter).max(comparator).orElse(null);
    }

    public Slot getSlot(Item item, Comparator<Slot> comparator, Predicate<Slot> filter) {
        return slots().filter(s -> s.getStack().getItem().equals(item)).filter(filter).max(comparator).orElse(null);
    }

    public Slot getFoodMaxSaturationSlot() {
        return slots().filter(s -> s.getStack().get(DataComponentTypes.FOOD) != null && !s.getStack().get(DataComponentTypes.FOOD).canAlwaysEat())
                .max(Comparator.comparingDouble(s -> s.getStack().get(DataComponentTypes.FOOD).saturation())).orElse(null);
    }

    public Slot getSlot(List<Item> item) {
        return slots().filter(s -> item.contains(s.getStack().getItem())).findFirst().orElse(null);
    }

    public Slot getPotion(RegistryEntry<StatusEffect> effect) {
        return slots().filter(s -> {
            PotionContentsComponent component = s.getStack().get(DataComponentTypes.POTION_CONTENTS);
            if (component == null) return false;
            return StreamSupport.stream(component.getEffects().spliterator(), false).anyMatch(e -> e.getEffectType().equals(effect));
        }).findFirst().orElse(null);
    }

    public Slot getPotionFromCategory(StatusEffectCategory category) {
        return slots().filter(s -> {
            ItemStack stack = s.getStack();
            PotionContentsComponent component = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (!stack.getItem().equals(Items.SPLASH_POTION) || component == null) return false;
            StatusEffectCategory category2 = category.equals(StatusEffectCategory.BENEFICIAL) ? StatusEffectCategory.HARMFUL : StatusEffectCategory.BENEFICIAL;
            long effects = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> e.getEffectType().value().getCategory().equals(category)).count();
            long effects2 = StreamSupport.stream(component.getEffects().spliterator(), false).filter(e -> e.getEffectType().value().getCategory().equals(category2)).count();
            return effects >= effects2;
        }).findFirst().orElse(null);
    }

    public int getInventoryCount(Item item) {
        return IntStream.range(0, 45).filter(i -> Objects.requireNonNull(mc.player).getInventory().getStack(i).getItem().equals(item)).map(i -> mc.player.getInventory().getStack(i).getCount()).sum();
    }

    public int getHotbarItems(List<Item> items) {
        return IntStream.range(0, 9).filter(i -> items.contains(mc.player.getInventory().getStack(i).getItem())).findFirst().orElse(-1);
    }

    public int getHotbarSlotId(IntPredicate filter) {
        return IntStream.range(0, 9).filter(filter).findFirst().orElse(-1);
    }

    public int getCount(Predicate<Slot> filter) {
        return slots().filter(filter).mapToInt(s -> s.getStack().getCount()).sum();
    }

    public Slot mainHandSlot() {
        long count = slots().count();
        int i = count == 46 ? 10 : 9;
        return slots().toList().get(Math.toIntExact(count - i + mc.player.getInventory().selectedSlot));
    }

    public boolean isServerScreen() {
        return slots().toList().size() != 46;
    }

    public Stream<Slot> slots(){
        return mc.player.currentScreenHandler.slots.stream();
    }
}
