package ru.zenith.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.api.event.EventManager;
import ru.zenith.implement.events.container.HandledScreenEvent;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    public int backgroundWidth;
    @Shadow
    public int backgroundHeight;

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "render", at = @At("RETURN"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EventManager.callEvent(new HandledScreenEvent(context, focusedSlot, backgroundWidth, backgroundHeight));
    }
}
