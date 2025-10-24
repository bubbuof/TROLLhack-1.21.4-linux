package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.task.scripts.Script;
import ru.zenith.implement.events.container.HandledScreenEvent;
import ru.zenith.common.util.auction.AuctionPriceParser;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.TickEvent;

import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuctionHelper extends Module {
    AuctionPriceParser auctionPriceParser = new AuctionPriceParser();
    Script script = new Script();
    @NonFinal
    Slot cheapestSlot, costEffectiveSlot;
    int[] RED_GREEN_COLORS = {0xFF4BFF4B, 0xFFFF4B4B};

    ColorSetting cheapestItemColorSetting = new ColorSetting("Cheapest Item", "Highlight color for the lowest priced item.")
            .setColor(0xFF4BFF4B).presets(RED_GREEN_COLORS);

    ColorSetting costEffectiveItemColorSetting = new ColorSetting("Cost Effective Item", "Highlight color for the best item.")
            .setColor(0xFFFF4B4B).presets(RED_GREEN_COLORS);

    public AuctionHelper() {
        super("AuctionHelper", "Auction Helper", ModuleCategory.RENDER);
        setup(cheapestItemColorSetting, costEffectiveItemColorSetting);
    }

    
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket) script.cleanup().addTickStep(0, () -> {
            if (mc.currentScreen instanceof GenericContainerScreen screen) {
                cheapestSlot = findSlotWithLowestPrice(screen.getScreenHandler().slots);
                costEffectiveSlot = findSlotWithBestPricePerItem(screen.getScreenHandler().slots);
            }
        });
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
    }

    
    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        DrawContext context = e.getDrawContext();
        MatrixStack matrix = context.getMatrices();

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
            int offsetY = (screen.height - e.getBackgroundHeight()) / 2;

            int cheapItemColor = getBlinkingColor(cheapestItemColorSetting.getColor());
            int cheapestQuantityColor = getBlinkingColor(costEffectiveItemColorSetting.getColor());

            matrix.push();
            matrix.translate(offsetX, offsetY, 0);
            if (cheapestSlot != costEffectiveSlot) highlightSlot(context, cheapestSlot, cheapItemColor);
            highlightSlot(context, costEffectiveSlot, cheapestQuantityColor);
            matrix.pop();
        }
    }

    
    private int getBlinkingColor(int color) {
        float alpha = (float) Math.abs(Math.sin((double) System.currentTimeMillis() / 10 * Math.PI / 180));
        return ColorUtil.multAlpha(color, alpha);
    }

    
    private Slot findSlotWithLowestPrice(List<Slot> slots) {
        return slots.stream().filter(this::hasValidPrice).min(Comparator.comparingInt(slot -> auctionPriceParser.getPrice(slot.getStack()))).orElse(null);
    }

    
    private Slot findSlotWithBestPricePerItem(List<Slot> slots) {
        return slots.stream().filter(this::isValidMultiItemSlot).min(Comparator.comparingInt(slot -> auctionPriceParser.getPrice(slot.getStack()) / slot.getStack().getCount())).orElse(null);
    }

    
    private boolean hasValidPrice(Slot slot) {
        return auctionPriceParser.getPrice(slot.getStack()) >= 0;
    }

    
    private boolean isValidMultiItemSlot(Slot slot) {
        return hasValidPrice(slot) && slot.getStack().getCount() > 1;
    }

    
    private void highlightSlot(DrawContext context, Slot slot, int color) {
        if (slot != null) rectangle.render(ShapeProperties.create(context.getMatrices(), slot.x, slot.y, 16, 16).color(color).build());
    }
}
