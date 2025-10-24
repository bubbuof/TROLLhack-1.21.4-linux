package ru.zenith.implement.screens.listgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.core.Main;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ListGuiScreen extends Screen implements QuickImports {
    public static ListGuiScreen INSTANCE = new ListGuiScreen();
    
    private int x = 100;
    private int y = 80;
    private int width = 720;
    private int height = 480;
    
    private int scrollOffset = 0;
    private int maxScroll = 0;
    
    private final int sidebarWidth = 200;
    private final int contentWidth = 300;
    private final int itemHeight = 32;
    private final int headerHeight = 60;
    private final int padding = 20;
    private final int cornerRadius = 12;
    
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private Module selectedModule = null;
    
    public ListGuiScreen() {
        super(Text.of("ListGUI"));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Setup blur effect for background
        blur.setup();
        
        // Main background with blur and rounded corners
        blur.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
            .round(cornerRadius)
            .color(new Color(15, 15, 25, 200).getRGB())
            .build());
        
        // Main container with modern dark theme
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, height)
            .round(cornerRadius)
            .color(new Color(20, 20, 30, 240).getRGB())
            .build());
        
        // Header section with gradient
        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y, width, headerHeight)
            .round(cornerRadius, cornerRadius, 0, 0)
            .color(new Color(25, 25, 35, 255).getRGB())
            .build());
        
        renderHeader(context, mouseX, mouseY);
        renderSidebar(context, mouseX, mouseY);
        renderContent(context, mouseX, mouseY);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        // Client name with modern typography
        String clientName = "NEVERLOSE";
        int nameX = x + padding;
        int nameY = y + (headerHeight - mc.textRenderer.fontHeight) / 2;
        context.drawText(mc.textRenderer, clientName, nameX, nameY, 
            Color.WHITE.getRGB(), false);
        
        // User info in top right
        String userInfo = "Login: zenith | Version: v4.0";
        int userWidth = mc.textRenderer.getWidth(userInfo);
        context.drawText(mc.textRenderer, userInfo, x + width - userWidth - padding, nameY, 
            new Color(160, 160, 160).getRGB(), false);
        
        // Subtitle
        String subtitle = "Sub up to: User";
        context.drawText(mc.textRenderer, subtitle, nameX, nameY + 12, 
            new Color(120, 120, 120).getRGB(), false);
    }
    
    private void renderSidebar(DrawContext context, int mouseX, int mouseY) {
        int sidebarX = x;
        int sidebarY = y + headerHeight;
        int sidebarHeight = height - headerHeight;
        
        // Sidebar background
        rectangle.render(ShapeProperties.create(context.getMatrices(), sidebarX, sidebarY, sidebarWidth, sidebarHeight)
            .round(0, 0, 0, cornerRadius)
            .color(new Color(18, 18, 28, 255).getRGB())
            .build());
        
        // Categories
        ModuleCategory[] categories = ModuleCategory.values();
        int currentY = sidebarY + padding;
        
        for (ModuleCategory category : categories) {
            boolean isSelected = category == selectedCategory;
            boolean isHovered = mouseX >= sidebarX && mouseX <= sidebarX + sidebarWidth &&
                              mouseY >= currentY && mouseY <= currentY + itemHeight;
            
            // Category background
            if (isSelected || isHovered) {
                Color bgColor = isSelected ? new Color(45, 45, 65, 200) : new Color(35, 35, 50, 150);
                rectangle.render(ShapeProperties.create(context.getMatrices(), sidebarX + 8, currentY, sidebarWidth - 16, itemHeight)
                    .round(6)
                    .color(bgColor.getRGB())
                    .build());
            }
            
            // Category icon (using first letter)
            String icon = String.valueOf(category.getReadableName().charAt(0));
            context.drawText(mc.textRenderer, icon, sidebarX + padding, currentY + 8, 
                isSelected ? Color.WHITE.getRGB() : new Color(180, 180, 180).getRGB(), false);
            
            // Category name
            context.drawText(mc.textRenderer, category.getReadableName(), 
                sidebarX + padding + 20, currentY + 8, 
                isSelected ? Color.WHITE.getRGB() : new Color(160, 160, 160).getRGB(), false);
            
            currentY += itemHeight + 4;
        }
    }
    
    private void renderContent(DrawContext context, int mouseX, int mouseY) {
        int contentX = x + sidebarWidth;
        int contentY = y + headerHeight;
        int contentHeight = height - headerHeight;
        
        // Main content area background
        rectangle.render(ShapeProperties.create(context.getMatrices(), contentX, contentY, width - sidebarWidth, contentHeight)
            .round(0, cornerRadius, cornerRadius, 0)
            .color(new Color(22, 22, 32, 255).getRGB())
            .build());
        
        // Content sections
        renderModuleList(context, mouseX, mouseY, contentX, contentY, contentWidth, contentHeight);
        
        if (selectedModule != null) {
            renderModuleSettings(context, mouseX, mouseY, contentX + contentWidth, contentY, 
                width - sidebarWidth - contentWidth, contentHeight);
        }
    }
    
    private void renderModuleList(DrawContext context, int mouseX, int mouseY, int startX, int startY, int listWidth, int listHeight) {
        // Module list background
        rectangle.render(ShapeProperties.create(context.getMatrices(), startX + 10, startY + 10, listWidth - 20, listHeight - 20)
            .round(8)
            .color(new Color(28, 28, 38, 200).getRGB())
            .build());
        
        // Category title
        String categoryTitle = selectedCategory.getReadableName();
        context.drawText(mc.textRenderer, categoryTitle, startX + padding, startY + padding, 
            Color.WHITE.getRGB(), false);
        
        List<Module> modules = getModulesForCategory(selectedCategory);
        int currentY = startY + padding + 25;
        
        // Calculate scrolling
        int totalHeight = modules.size() * (itemHeight + 2);
        maxScroll = Math.max(0, totalHeight - (listHeight - 60));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        int visibleStart = scrollOffset / (itemHeight + 2);
        int visibleEnd = Math.min(modules.size(), visibleStart + (listHeight - 60) / (itemHeight + 2) + 2);
        
        for (int i = visibleStart; i < visibleEnd; i++) {
            Module module = modules.get(i);
            int moduleY = currentY + i * (itemHeight + 2) - scrollOffset;
            
            if (moduleY < startY + 30 || moduleY > startY + listHeight - 30) continue;
            
            boolean isSelected = module == selectedModule;
            boolean isHovered = mouseX >= startX + 15 && mouseX <= startX + listWidth - 15 &&
                              mouseY >= moduleY && mouseY <= moduleY + itemHeight;
            
            // Module background
            if (isSelected || isHovered) {
                Color bgColor = isSelected ? new Color(50, 50, 70, 200) : new Color(40, 40, 55, 150);
                rectangle.render(ShapeProperties.create(context.getMatrices(), startX + 15, moduleY, listWidth - 30, itemHeight)
                    .round(6)
                    .color(bgColor.getRGB())
                    .build());
            }
            
            // Module state indicator
            Color stateColor = module.isState() ? new Color(100, 200, 100, 255) : new Color(60, 60, 80, 255);
            rectangle.render(ShapeProperties.create(context.getMatrices(), startX + listWidth - 35, moduleY + 8, 16, 16)
                .round(8)
                .color(stateColor.getRGB())
                .build());
            
            // Module name
            String moduleName = module.getVisibleName() != null ? module.getVisibleName() : module.getName();
            Color textColor = module.isState() ? Color.WHITE : new Color(180, 180, 180);
            context.drawText(mc.textRenderer, moduleName, startX + 25, moduleY + 8, textColor.getRGB(), false);
        }
    }
    
    private void renderModuleSettings(DrawContext context, int mouseX, int mouseY, int startX, int startY, int settingsWidth, int settingsHeight) {
        // Settings panel background
        rectangle.render(ShapeProperties.create(context.getMatrices(), startX + 10, startY + 10, settingsWidth - 20, settingsHeight - 20)
            .round(8)
            .color(new Color(25, 25, 35, 200).getRGB())
            .build());
        
        // Module name header
        String moduleName = selectedModule.getVisibleName() != null ? selectedModule.getVisibleName() : selectedModule.getName();
        context.drawText(mc.textRenderer, moduleName, startX + padding, startY + padding, 
            Color.WHITE.getRGB(), false);
        
        int currentY = startY + padding + 25;
        
        // Toggle button with modern design
        int buttonWidth = 120;
        int buttonHeight = 32;
        boolean isHoveringButton = mouseX >= startX + padding && mouseX <= startX + padding + buttonWidth &&
                                  mouseY >= currentY && mouseY <= currentY + buttonHeight;
        
        Color buttonColor = selectedModule.isState() ? 
            new Color(70, 150, 70, 255) : new Color(60, 60, 80, 255);
        
        if (isHoveringButton) {
            buttonColor = selectedModule.isState() ? 
                new Color(90, 170, 90, 255) : new Color(80, 80, 100, 255);
        }
        
        rectangle.render(ShapeProperties.create(context.getMatrices(), startX + padding, currentY, buttonWidth, buttonHeight)
            .round(8)
            .color(buttonColor.getRGB())
            .build());
        
        String buttonText = selectedModule.isState() ? "ENABLED" : "DISABLED";
        int textX = startX + padding + (buttonWidth - mc.textRenderer.getWidth(buttonText)) / 2;
        context.drawText(mc.textRenderer, buttonText, textX, currentY + 10, 
            Color.WHITE.getRGB(), false);
        
        currentY += buttonHeight + 30;
        
        // Module information with modern styling
        context.drawText(mc.textRenderer, "Module Information", 
            startX + padding, currentY, new Color(200, 200, 200).getRGB(), false);
        currentY += 20;
        
        // Info items with better spacing
        String[] infoItems = {
            "Name: " + (selectedModule.getVisibleName() != null ? selectedModule.getVisibleName() : selectedModule.getName()),
            "Category: " + selectedModule.getCategory().getReadableName(),
            "Keybind: " + getKeyName(selectedModule.getKey()),
            "State: " + (selectedModule.isState() ? "Active" : "Inactive")
        };
        
        for (String info : infoItems) {
            context.drawText(mc.textRenderer, info, 
                startX + padding + 10, currentY, new Color(160, 160, 160).getRGB(), false);
            currentY += 18;
        }
    }
    
    private String getKeyName(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "None";
        String keyName = GLFW.glfwGetKeyName(keyCode, 0);
        return keyName != null ? keyName.toUpperCase() : "Unknown";
    }
    
    private List<Module> getModulesForCategory(ModuleCategory category) {
        List<Module> modules = new ArrayList<>();
        for (Module module : Main.getInstance().getModuleRepository().modules()) {
            if (module.getCategory() == category) {
                modules.add(module);
            }
        }
        return modules;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            // Check category clicks in sidebar
            int sidebarX = x;
            int sidebarY = y + headerHeight;
            
            if (mouseX >= sidebarX && mouseX <= sidebarX + sidebarWidth) {
                ModuleCategory[] categories = ModuleCategory.values();
                int currentY = sidebarY + padding;
                
                for (ModuleCategory category : categories) {
                    if (mouseY >= currentY && mouseY <= currentY + itemHeight) {
                        selectedCategory = category;
                        selectedModule = null;
                        scrollOffset = 0;
                        return true;
                    }
                    currentY += itemHeight + 4;
                }
            }
            
            // Check module clicks in content area
            int contentX = x + sidebarWidth;
            int contentY = y + headerHeight;
            
            if (mouseX >= contentX + 15 && mouseX <= contentX + contentWidth - 15) {
                List<Module> modules = getModulesForCategory(selectedCategory);
                int currentY = contentY + padding + 25;
                
                int visibleStart = scrollOffset / (itemHeight + 2);
                int visibleEnd = Math.min(modules.size(), visibleStart + (height - headerHeight - 60) / (itemHeight + 2) + 2);
                
                for (int i = visibleStart; i < visibleEnd; i++) {
                    int moduleY = currentY + i * (itemHeight + 2) - scrollOffset;
                    if (mouseY >= moduleY && mouseY <= moduleY + itemHeight) {
                        Module clickedModule = modules.get(i);
                        
                        // Check if clicking on state indicator
                        if (mouseX >= contentX + contentWidth - 35 && mouseX <= contentX + contentWidth - 19) {
                            clickedModule.switchState();
                        } else {
                            selectedModule = clickedModule;
                        }
                        return true;
                    }
                }
            }
            
            // Check toggle button click in settings panel
            if (selectedModule != null) {
                int settingsX = contentX + contentWidth;
                int buttonY = contentY + padding + 25;
                int buttonWidth = 120;
                int buttonHeight = 32;
                
                if (mouseX >= settingsX + padding && mouseX <= settingsX + padding + buttonWidth &&
                    mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    selectedModule.switchState();
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Check if scrolling in module list area
        int contentX = x + sidebarWidth;
        int contentY = y + headerHeight;
        
        if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
            mouseY >= contentY && mouseY <= contentY + (height - headerHeight)) {
            
            scrollOffset -= (int)(verticalAmount * 30);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_P) {
            close();
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
