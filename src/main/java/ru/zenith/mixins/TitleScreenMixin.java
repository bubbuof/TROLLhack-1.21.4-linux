package ru.zenith.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.zenith.implement.screens.altmanager.AltManagerScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    
    protected TitleScreenMixin(Text title) {
        super(title);
    }
    
    @Inject(method = "init", at = @At("RETURN"))
    private void addAltManagerButton(CallbackInfo ci) {
        // Add compact Alt Manager button in bottom-right corner
        int buttonWidth = 80;
        int buttonHeight = 20;
        int x = this.width - buttonWidth - 5; // 5px from right edge
        int y = this.height - buttonHeight - 5; // 5px from bottom
        
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("Alts"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new AltManagerScreen((TitleScreen)(Object)this));
                    }
                })
                .dimensions(x, y, buttonWidth, buttonHeight)
                .build());
    }
}
