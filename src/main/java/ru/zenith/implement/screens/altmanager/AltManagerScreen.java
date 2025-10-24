package ru.zenith.implement.screens.altmanager;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;

import java.util.ArrayList;
import java.util.List;

public class AltManagerScreen extends Screen implements QuickImports {
    private final Screen parent;
    private final AltManager altManager = AltManager.getInstance();
    private final List<AltButton> altButtons = new ArrayList<>();
    
    private String inputText = "";
    private boolean typing = false;
    private int scroll = 0;
    private int selectedIndex = -1;
    
    public AltManagerScreen(Screen parent) {
        super(Text.of("Alt Manager"));
        this.parent = parent;
        updateButtons();
    }
    
    private void updateButtons() {
        altButtons.clear();
        List<Alt> alts = altManager.getAlts();
        for (int i = 0; i < alts.size(); i++) {
            altButtons.add(new AltButton(alts.get(i), i));
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = window.getScaledWidth() / 2;
        int centerY = window.getScaledHeight() / 2;
        int width = 400;
        int height = 300;
        int x = centerX - width / 2;
        int y = centerY - height / 2;
        
        MatrixStack matrix = context.getMatrices();
        
        // Background overlay
        rectangle.render(ShapeProperties.create(matrix, 0, 0, window.getScaledWidth(), window.getScaledHeight())
                .color(MathUtil.applyOpacity(0xFF000000, 150)).build());
        
        // Main panel
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(2).softness(0.5f).thickness(1)
                .outlineColor(ColorUtil.getSkeetBorder()).color(ColorUtil.getSkeetBackground()).build());
        
        // Top accent bar
        rectangle.render(ShapeProperties.create(matrix, x, y, width, 3)
                .color(ColorUtil.getSkeetAccent()).build());
        
        // Title
        Fonts.getSize(18, Fonts.Type.BOLD).drawString(matrix, "Alt Manager", x + 15, y + 15, ColorUtil.getSkeetText());
        
        // Current account info
        String currentUser = altManager.getCurrentUsername();
        Fonts.getSize(14).drawString(matrix, "§7Current: §f" + currentUser, x + 15, y + 35, ColorUtil.getSkeetText());
        
        // Input field
        drawInputField(context, x + 15, y + 55, width - 30, 25, mouseX, mouseY);
        
        // Add button
        drawButton(context, x + width - 80, y + 55, 65, 25, "Add", mouseX, mouseY, () -> {
            if (!inputText.isEmpty()) {
                altManager.addAlt(new Alt(inputText));
                inputText = "";
                updateButtons();
            }
        });
        
        // Alt list
        drawAltList(context, x + 15, y + 90, width - 30, height - 140, mouseX, mouseY);
        
        // Bottom buttons
        drawButton(context, x + 15, y + height - 35, 80, 25, "Login", mouseX, mouseY, () -> {
            if (selectedIndex >= 0 && selectedIndex < altManager.getAlts().size()) {
                altManager.login(altManager.getAlts().get(selectedIndex));
            }
        });
        
        drawButton(context, x + 105, y + height - 35, 80, 25, "Remove", mouseX, mouseY, () -> {
            if (selectedIndex >= 0 && selectedIndex < altManager.getAlts().size()) {
                altManager.removeAlt(altManager.getAlts().get(selectedIndex));
                selectedIndex = -1;
                updateButtons();
            }
        });
        
        drawButton(context, x + width - 95, y + height - 35, 80, 25, "Back", mouseX, mouseY, () -> {
            mc.setScreen(parent);
        });
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawInputField(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
        
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(2).thickness(1)
                .outlineColor(typing ? ColorUtil.getSkeetAccent() : hovered ? ColorUtil.getSkeetAccent(0.5f) : ColorUtil.getSkeetBorder())
                .color(ColorUtil.getSkeetSecondary()).build());
        
        String displayText = inputText.isEmpty() && !typing ? "Enter username..." : inputText;
        int textColor = inputText.isEmpty() && !typing ? ColorUtil.getSkeetTextSecondary() : ColorUtil.getSkeetText();
        
        Fonts.getSize(14).drawString(matrix, displayText, x + 8, y + height / 2 - 3, textColor);
        
        // Cursor
        if (typing && System.currentTimeMillis() % 1000 < 500) {
            float cursorX = x + 8 + Fonts.getSize(14).getStringWidth(inputText);
            rectangle.render(ShapeProperties.create(matrix, cursorX, y + 6, 1, height - 12)
                    .color(ColorUtil.getSkeetAccent()).build());
        }
    }
    
    private void drawAltList(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();
        
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(2).thickness(1)
                .outlineColor(ColorUtil.getSkeetBorder()).color(ColorUtil.getSkeetSecondary()).build());
        
        int itemHeight = 30;
        int visibleItems = height / itemHeight;
        int startIndex = Math.max(0, -scroll / itemHeight);
        
        for (int i = startIndex; i < Math.min(altButtons.size(), startIndex + visibleItems + 1); i++) {
            AltButton button = altButtons.get(i);
            int itemY = y + 5 + (i * itemHeight) + scroll;
            
            if (itemY + itemHeight < y || itemY > y + height) continue;
            
            boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + 5, itemY, width - 10, itemHeight - 5);
            boolean selected = i == selectedIndex;
            
            if (selected) {
                rectangle.render(ShapeProperties.create(matrix, x + 5, itemY, width - 10, itemHeight - 5).round(2)
                        .color(ColorUtil.getSkeetAccent(0.3f)).build());
            } else if (hovered) {
                rectangle.render(ShapeProperties.create(matrix, x + 5, itemY, width - 10, itemHeight - 5).round(2)
                        .color(ColorUtil.getSkeetHover(0.2f)).build());
            }
            
            Fonts.getSize(14, Fonts.Type.BOLD).drawString(matrix, button.alt.getUsername(), 
                    x + 15, itemY + 8, selected ? ColorUtil.getSkeetAccent() : ColorUtil.getSkeetText());
            
            String status = button.alt.isPremium() ? "Premium" : "Cracked";
            int statusColor = button.alt.isPremium() ? 0xFF55FF55 : ColorUtil.getSkeetTextSecondary();
            Fonts.getSize(12).drawString(matrix, status, x + 15, itemY + 20, statusColor);
        }
    }
    
    private void drawButton(DrawContext context, int x, int y, int width, int height, String text, 
                           int mouseX, int mouseY, Runnable action) {
        MatrixStack matrix = context.getMatrices();
        boolean hovered = MathUtil.isHovered(mouseX, mouseY, x, y, width, height);
        
        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(2).thickness(1)
                .outlineColor(hovered ? ColorUtil.getSkeetAccent() : ColorUtil.getSkeetBorder())
                .color(hovered ? ColorUtil.getSkeetAccent(0.2f) : ColorUtil.getSkeetSecondary()).build());
        
        float textWidth = Fonts.getSize(14, Fonts.Type.BOLD).getStringWidth(text);
        Fonts.getSize(14, Fonts.Type.BOLD).drawString(matrix, text, 
                x + width / 2 - textWidth / 2, y + height / 2 - 4, 
                hovered ? ColorUtil.getSkeetAccent() : ColorUtil.getSkeetText());
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = window.getScaledWidth() / 2;
        int centerY = window.getScaledHeight() / 2;
        int width = 400;
        int height = 300;
        int x = centerX - width / 2;
        int y = centerY - height / 2;
        
        // Input field click
        if (MathUtil.isHovered(mouseX, mouseY, x + 15, y + 55, width - 30, 25)) {
            typing = true;
            return true;
        } else {
            typing = false;
        }
        
        // Add button
        if (MathUtil.isHovered(mouseX, mouseY, x + width - 80, y + 55, 65, 25)) {
            if (!inputText.isEmpty()) {
                altManager.addAlt(new Alt(inputText));
                inputText = "";
                updateButtons();
            }
            return true;
        }
        
        // Alt list clicks
        int listY = y + 90;
        int listHeight = height - 140;
        int itemHeight = 30;
        
        if (MathUtil.isHovered(mouseX, mouseY, x + 15, listY, width - 30, listHeight)) {
            int clickedIndex = ((int)mouseY - listY - 5 - scroll) / itemHeight;
            if (clickedIndex >= 0 && clickedIndex < altButtons.size()) {
                selectedIndex = clickedIndex;
            }
            return true;
        }
        
        // Login button
        if (MathUtil.isHovered(mouseX, mouseY, x + 15, y + height - 35, 80, 25)) {
            if (selectedIndex >= 0 && selectedIndex < altManager.getAlts().size()) {
                altManager.login(altManager.getAlts().get(selectedIndex));
            }
            return true;
        }
        
        // Remove button
        if (MathUtil.isHovered(mouseX, mouseY, x + 105, y + height - 35, 80, 25)) {
            if (selectedIndex >= 0 && selectedIndex < altManager.getAlts().size()) {
                altManager.removeAlt(altManager.getAlts().get(selectedIndex));
                selectedIndex = -1;
                updateButtons();
            }
            return true;
        }
        
        // Back button
        if (MathUtil.isHovered(mouseX, mouseY, x + width - 95, y + height - 35, 80, 25)) {
            mc.setScreen(parent);
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int centerX = window.getScaledWidth() / 2;
        int centerY = window.getScaledHeight() / 2;
        int width = 400;
        int height = 300;
        int x = centerX - width / 2;
        int y = centerY - height / 2;
        int listY = y + 90;
        int listHeight = height - 140;
        
        if (MathUtil.isHovered(mouseX, mouseY, x + 15, listY, width - 30, listHeight)) {
            scroll += (int)(verticalAmount * 20);
            int maxScroll = Math.max(0, (altButtons.size() * 30) - listHeight);
            scroll = Math.max(-maxScroll, Math.min(0, scroll));
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing) {
            inputText += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing) {
            if (keyCode == 259) { // Backspace
                if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) { // Enter
                if (!inputText.isEmpty()) {
                    altManager.addAlt(new Alt(inputText));
                    inputText = "";
                    updateButtons();
                }
                typing = false;
                return true;
            }
        }
        
        if (keyCode == 256) { // ESC
            mc.setScreen(parent);
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private static class AltButton {
        final Alt alt;
        final int index;
        
        AltButton(Alt alt, int index) {
            this.alt = alt;
            this.index = index;
        }
    }
}
