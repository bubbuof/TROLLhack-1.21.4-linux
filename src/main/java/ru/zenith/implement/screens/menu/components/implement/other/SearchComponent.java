package ru.zenith.implement.screens.menu.components.implement.other;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.screens.menu.MenuScreen;
import ru.zenith.implement.screens.menu.components.AbstractComponent;

public class SearchComponent extends AbstractComponent {
    public static boolean typing = false;
    private boolean dragging;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private float xOffset = 0;

    @Getter
    @Setter // Добавляем сеттер
    private String text = "";

    // Остальной код без изменений...
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(16);

        updateXOffset(font, cursorPosition);

        width = 250;
        height = 35;

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(8F).thickness(2).softness(1.0F)
                .outlineColor(0xFF2A2A2A)
                .color(0xFF1A1A1A)
                .build());

        image.setTexture("textures/search.png").render(ShapeProperties.create(matrix, x + width - 25, y + 13, 10F, 10F).build());

        String displayText = text.equalsIgnoreCase("") && !typing ? "Type to search modules..." : text;

        ScissorManager scissor = Main.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), x + 1, y, width - 3, height);

        if (typing && selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int start = Math.max(0, Math.min(getStartOfSelection(), text.length()));
            int end = Math.max(0, Math.min(getEndOfSelection(), text.length()));
            if (start < end) {
                float selectionXStart = x + 12 - xOffset + font.getStringWidth(text.substring(0, start));
                float selectionXEnd = x + 12 - xOffset + font.getStringWidth(text.substring(0, end));
                float selectionWidth = selectionXEnd - selectionXStart;

                rectangle.render(ShapeProperties.create(matrix, selectionXStart, y + (height / 2) - 8, selectionWidth, 16)
                        .round(2F).color(0xFF3366CC).build());
            }
        }

        int textColor = typing ? 0xFFFFFFFF : 0xFFAAAAAA;
        font.drawString(context.getMatrices(), displayText, x + 12, y + (height / 2) - 6.0F, textColor);

        scissor.pop();

        long currentTime = System.currentTimeMillis();
        boolean focused = typing && (currentTime % 1000 < 500);

        if (focused && (selectionStart == -1 || selectionStart == selectionEnd)) {
            float cursorX = font.getStringWidth(text.substring(0, cursorPosition));
            rectangle.render(ShapeProperties.create(matrix, x + 12 - xOffset + cursorX, y + (height / 2) - 7F, 1.5F, 14)
                    .round(0.5f).color(0xFFFFFFFF).build());
        }

        if (dragging) {
            cursorPosition = getCursorIndexAt(mouseX);
            if (selectionStart == -1) {
                selectionStart = cursorPosition + 1;
            }
            selectionEnd = cursorPosition;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250) {
                selectionStart = 0;
                selectionEnd = text.length();
            } else {
                typing = true;
                dragging = true;
                lastClickTime = currentTime;
                cursorPosition = getCursorIndexAt(mouseX);
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing && Fonts.getSize(16).getStringWidth(text) < width - 50) {
            deleteSelectedText();
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            clearSelection();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing) {
            if (Screen.hasControlDown()) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_A -> {
                        selectAllText();
                        return true;
                    }
                    case GLFW.GLFW_KEY_V -> {
                        pasteFromClipboard();
                        return true;
                    }
                    case GLFW.GLFW_KEY_C -> {
                        copyToClipboard();
                        return true;
                    }
                }
            } else {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_BACKSPACE -> {
                        handleTextModification(keyCode);
                        return true;
                    }
                    case GLFW.GLFW_KEY_ENTER -> {
                        MenuScreen.INSTANCE.toggleSearch();
                        return true;
                    }
                    case GLFW.GLFW_KEY_ESCAPE -> {
                        MenuScreen.INSTANCE.toggleSearch();
                        return true;
                    }
                    case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
                        moveCursor(keyCode);
                        return true;
                    }
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void pasteFromClipboard() {
        String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
        if (clipboardText != null) {
            replaceText(cursorPosition, cursorPosition, clipboardText);
        }
    }

    private void copyToClipboard() {
        if (hasSelection()) {
            GLFW.glfwSetClipboardString(window.getHandle(), getSelectedText());
        }
    }

    private void selectAllText() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
    }

    private void handleTextModification(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                replaceText(getStartOfSelection(), getEndOfSelection(), "");
            } else if (cursorPosition > 0) {
                replaceText(cursorPosition - 1, cursorPosition, "");
            }
        }
    }

    private void moveCursor(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPosition > 0) {
            cursorPosition--;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPosition < text.length()) {
            cursorPosition++;
        }
        updateSelectionAfterCursorMove();
    }

    private void updateSelectionAfterCursorMove() {
        if (Screen.hasShiftDown()) {
            if (selectionStart == -1) selectionStart = cursorPosition;
            selectionEnd = cursorPosition;
        } else {
            clearSelection();
        }
    }

    private void replaceText(int start, int end, String replacement) {
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start > end) start = end;

        text = text.substring(0, start) + replacement + text.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private String getSelectedText() {
        return text.substring(getStartOfSelection(), getEndOfSelection());
    }

    private int getStartOfSelection() {
        return Math.min(selectionStart, selectionEnd);
    }

    private int getEndOfSelection() {
        return Math.max(selectionStart, selectionEnd);
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private int getCursorIndexAt(double mouseX) {
        FontRenderer font = Fonts.getSize(16, Fonts.Type.BOLD);
        float relativeX = (float) mouseX - x - 12 + xOffset;
        int position = 0;
        while (position < text.length()) {
            float textWidth = font.getStringWidth(text.substring(0, position + 1));
            if (textWidth > relativeX) {
                break;
            }
            position++;
        }
        return position;
    }

    private void updateXOffset(FontRenderer font, int cursorPosition) {
        float cursorX = font.getStringWidth(text.substring(0, cursorPosition));
        if (cursorX < xOffset) {
            xOffset = cursorX;
        } else if (cursorX - xOffset > width - 30) {
            xOffset = cursorX - (width - 30);
        }
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }

    // Добавляем метод для сброса курсора
    public void resetCursor() {
        cursorPosition = 0;
        clearSelection();
    }
}