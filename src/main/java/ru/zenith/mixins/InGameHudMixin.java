package ru.zenith.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.common.QuickImports;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.core.Main;
import ru.zenith.api.system.draw.DrawEngineImpl;
import ru.zenith.api.event.EventManager;
import ru.zenith.implement.events.render.DrawEvent;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.implement.features.modules.render.CrossHair;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.ConcurrentModificationException;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements QuickImports {
    @Unique private final Hud hud = Hud.getInstance();

    @Final @Shadow private MinecraftClient client;

    @Shadow protected abstract void renderStatusBars(DrawContext context);

    @Shadow protected abstract void renderMountHealth(DrawContext context);

    @Inject(method = "render", at = @At("RETURN"))
    public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        blur.setup();
        DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickDelta(false));
        EventManager.callEvent(event);
        Render2DUtil.onRender(context);
        if (!client.options.hudHidden) {
            Main.getInstance().getDraggableRepository().draggable().forEach(draggable -> {
                if (draggable.canDraw(hud, draggable)) draggable.startAnimation();
                else draggable.stopAnimation();

                float scale = draggable.getScaleAnimation().getOutput().floatValue();
                if (!draggable.isCloseAnimationFinished()) {
                    draggable.validPosition();
                    try {
                        MathUtil.setAlpha(scale, () -> draggable.drawDraggable(context));
                    } catch (ConcurrentModificationException ignored) {}
                }
            });
        }
    }

    @Inject(method = "renderCrosshair", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"), cancellable = true)
    public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrossHair crossHair = CrossHair.getInstance();
        if (crossHair.isState()) {
            crossHair.onRenderCrossHair();
            ci.cancel();
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
    public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Potions")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("Score Board")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOverlayMessage", at = @At(value = "HEAD"), cancellable = true)
    private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("HotBar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At(value = "HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("HotBar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMainHud", at = @At(value = "HEAD"), cancellable = true)
    private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (hud.isState() && hud.interfaceSettings.isSelected("HotBar")) {
            context.drawGuiTexture(RenderLayer::getGuiTextured, InGameHud.HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 1, 1);
            if (client.interactionManager.hasStatusBars()) renderStatusBars(context);
            this.renderMountHealth(context);
            ci.cancel();
        }
    }
}
