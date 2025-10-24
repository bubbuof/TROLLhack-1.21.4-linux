package ru.zenith.api.feature.draggable;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import ru.zenith.implement.events.container.SetScreenEvent;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.features.modules.render.Hud;

public interface Draggable {
    boolean visible();

    void tick();

    void render(DrawContext context, int mouseX, int mouseY, float delta);

    void packet(PacketEvent e);

    void setScreen(SetScreenEvent screen);

    boolean mouseClicked(double mouseX, double mouseY, int button);

    boolean mouseReleased(double mouseX, double mouseY, int button);
}
