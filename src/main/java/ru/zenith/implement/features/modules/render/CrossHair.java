package ru.zenith.implement.features.modules.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.hit.EntityHitResult;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.render.Render2DUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CrossHair extends Module {
    public static CrossHair getInstance() {
        return Instance.get(CrossHair.class);
    }
    private float red = 0;

    private final ValueSetting attackSetting = new ValueSetting("Attack Indent", "Item Cooldown Indent").setValue(10).range(0, 20);
    private final ValueSetting indentSetting = new ValueSetting("Indent", "Indent from the center of the screen").setValue(0).range(0, 5);
    private final ValueSetting size1Setting = new ValueSetting("Width", "Width Cross Hair").setValue(4).range(2, 10);
    private final ValueSetting size2Setting = new ValueSetting("Height", "Height Cross Hair").setValue(1).range(1, 4);

    public CrossHair() {
        super("CrossHair", "Cross Hair", ModuleCategory.RENDER);
        setup(attackSetting, indentSetting, size1Setting, size2Setting);
    }

    public void onRenderCrossHair() {
        red = MathUtil.interpolateSmooth(2, red, mc.crosshairTarget instanceof EntityHitResult ? 5 : 1);
        int firstColor = ColorUtil.multRed(ColorUtil.WHITE, red), secondColor = ColorUtil.BLACK;
        float x = window.getScaledWidth() / 2F, y = window.getScaledHeight() / 2F;
        float cooldown = attackSetting.getInt() - (attackSetting.getInt() * mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false)));
        float size = size1Setting.getValue(), size2 = size2Setting.getValue(), offset = size2 / 2, indent = indentSetting.getInt() + cooldown;

        renderMain(x, y, size, size2, 1, indent, offset, secondColor);
        renderMain(x, y, size, size2, 0, indent, offset, firstColor);
    }

    private void renderMain(float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
        Render2DUtil.drawQuad(x - offset - padding / 2, y - size - indent - padding / 2, size2 + padding, size + padding, color);
        Render2DUtil.drawQuad(x - offset - padding / 2, y + indent - padding / 2, size2 + padding, size + padding, color);
        Render2DUtil.drawQuad(x - size - indent - padding / 2, y - offset - padding / 2, size + padding, size2 + padding, color);
        Render2DUtil.drawQuad(x + indent - padding / 2, y - offset - padding / 2, size + padding, size2 + padding, color);
    }
}
