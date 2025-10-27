package ru.zenith.implement.features.modules.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.feature.module.setting.implement.SelectSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.core.Main;
import ru.zenith.implement.events.render.DrawEvent;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LegacyHud extends Module {
    
    public static LegacyHud getInstance() {
        return Instance.get(LegacyHud.class);
    }

    private static final ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);

    // Font selection
    private final SelectSetting customFont = new SelectSetting("Font", "Select font type")
            .value("Minecraft", "Comfortaa", "Monsterrat", "SF").selected("Minecraft");
    
    // Color setting
    private final ColorSetting colorSetting = new ColorSetting("Color", "HUD text color")
            .setColor(0x0077FF);
    
    // Rendering direction
    private final BooleanSetting renderingUp = new BooleanSetting("RenderingUp", "Render modules from top to bottom")
            .setValue(false);
    
    // HUD Components
    private final BooleanSetting waterMark = new BooleanSetting("Watermark", "Show client watermark")
            .setValue(false);
    private final BooleanSetting arrayList = new BooleanSetting("ActiveModules", "Show active modules list")
            .setValue(false);
    private final BooleanSetting coords = new BooleanSetting("Coords", "Show coordinates")
            .setValue(false);
    private final BooleanSetting direction = new BooleanSetting("Direction", "Show facing direction")
            .setValue(false);
    private final BooleanSetting armor = new BooleanSetting("Armor", "Show armor durability")
            .setValue(false);
    private final BooleanSetting totems = new BooleanSetting("Totems", "Show totem count")
            .setValue(false);
    private final BooleanSetting greeter = new BooleanSetting("Welcomer", "Show welcome message")
            .setValue(false);
    private final BooleanSetting speed = new BooleanSetting("Speed", "Show movement speed")
            .setValue(false);
    private final BooleanSetting bps = new BooleanSetting("BPS", "Show blocks per second instead of km/h")
            .setValue(false).visible(() -> speed.isValue());
    private final BooleanSetting potions = new BooleanSetting("Potions", "Show active potion effects")
            .setValue(false);
    private final BooleanSetting ping = new BooleanSetting("Ping", "Show network ping")
            .setValue(false);
    private final BooleanSetting tps = new BooleanSetting("TPS", "Show server TPS")
            .setValue(false);
    private final BooleanSetting extraTps = new BooleanSetting("ExtraTPS", "Show additional TPS info")
            .setValue(true).visible(() -> tps.isValue());
    private final BooleanSetting offhandDurability = new BooleanSetting("OffhandDurability", "Show offhand item durability")
            .setValue(false);
    private final BooleanSetting mainhandDurability = new BooleanSetting("MainhandDurability", "Show mainhand item durability")
            .setValue(false);
    private final BooleanSetting fps = new BooleanSetting("FPS", "Show FPS counter")
            .setValue(false);
    private final BooleanSetting chests = new BooleanSetting("Chests", "Show chest counter")
            .setValue(false);
    private final BooleanSetting worldTime = new BooleanSetting("WorldTime", "Show world time")
            .setValue(false);
    private final BooleanSetting biome = new BooleanSetting("Biome", "Show current biome")
            .setValue(false);
    private final BooleanSetting time = new BooleanSetting("Time", "Show real time")
            .setValue(false);

    private final ValueSetting waterMarkY = new ValueSetting("WatermarkPosY", "Watermark Y position")
            .setValue(2).range(0, 20).visible(() -> waterMark.isValue());

    private int color;

    public LegacyHud() {
        super("LegacyHud", "Legacy HUD", ModuleCategory.RENDER);
        setup(customFont, colorSetting, renderingUp, waterMark, arrayList, coords, direction, 
              armor, totems, greeter, speed, bps, potions, ping, tps, extraTps, 
              offhandDurability, mainhandDurability, fps, chests, worldTime, biome, time, waterMarkY);
    }

    @EventHandler
    public void onRender2D(DrawEvent event) {
        if (mc.player == null || mc.world == null || event.getDrawContext() == null) return;

        try {
            DrawContext context = event.getDrawContext();
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();
            int offset;

        // Font offset calculation
        switch (customFont.getSelected()) {
            case "Minecraft" -> offset = 10;
            case "Monsterrat" -> offset = 9;
            default -> offset = 8;
        }

        color = colorSetting.getColor();

        // Watermark
        if (waterMark.isValue()) {
            drawText(context, "vega33hack v1.12", 2, waterMarkY.getInt());
        }

        int j = (mc.currentScreen instanceof ChatScreen && !renderingUp.isValue()) ? 14 : 0;

        // Active modules list
        if (arrayList.isValue()) {
            try {
                List<ru.zenith.api.feature.module.Module> enabledModules = Main.getInstance().getModuleRepository().modules()
                        .stream()
                        .filter(module -> module != null && module.isState())
                        .filter(module -> module.getName() != null)
                        .sorted(Comparator.comparing(module -> {
                            String name = module.getVisibleName() != null ? module.getVisibleName() : module.getName();
                            return name != null ? getStringWidth(name) * -1 : 0;
                        }))
                        .collect(Collectors.toList());

                for (ru.zenith.api.feature.module.Module module : enabledModules) {
                    if (module == null) continue;
                    String str = (module.getVisibleName() != null ? module.getVisibleName() : module.getName());
                    if (str == null) continue;
                    
                    if (renderingUp.isValue()) {
                        drawText(context, str, (width - 2 - getStringWidth(str)), (2 + j * offset));
                        j++;
                    } else {
                        j += offset;
                        drawText(context, str, (width - 2 - getStringWidth(str)), (height - j));
                    }
                }
            } catch (Exception e) {
                // Skip module list rendering if there's an error
            }
        }

        int i = (mc.currentScreen instanceof ChatScreen && renderingUp.isValue()) ? 13 : (renderingUp.isValue() ? -2 : 0);

        // Potions
        if (potions.isValue() && mc.player != null) {
            try {
                List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getStatusEffects());
                for (StatusEffectInstance potionEffect : effects) {
                    if (potionEffect == null || potionEffect.getEffectType() == null) continue;
                    
                    StatusEffect potion = potionEffect.getEffectType().value();
                    if (potion == null) continue;
                    
                    String power = "";
                    switch (potionEffect.getAmplifier()) {
                        case 0 -> power = "I";
                        case 1 -> power = "II";
                        case 2 -> power = "III";
                        case 3 -> power = "IV";
                        case 4 -> power = "V";
                    }
                    String s = potion.getName().getString() + " " + power;
                    String s2 = getDuration(potionEffect) + "";
                    Color c = new Color(potionEffect.getEffectType().value().getColor());

                    if (renderingUp.isValue()) {
                        i += offset;
                        drawText(context, s + " " + s2, (width - getStringWidth(s + " " + s2) - 2), (height - 2 - i), c.getRGB());
                    } else {
                        drawText(context, s + " " + s2, (width - getStringWidth(s + " " + s2) - 2), (2 + i++ * offset), c.getRGB());
                    }
                }
            } catch (Exception e) {
                // Skip potions rendering if there's an error
            }
        }

        // World Time
        if (worldTime.isValue()) {
            String str2 = "WorldTime: " + Formatting.WHITE + mc.world.getTimeOfDay() % 24000;
            drawText(context, str2, width - getStringWidth(str2) - 2, renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Mainhand Durability
        if (mainhandDurability.isValue()) {
            String str = "MainHand" + Formatting.WHITE + " [" + (mc.player.getMainHandStack().getMaxDamage() - mc.player.getMainHandStack().getDamage()) + "]";
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // TPS
        if (tps.isValue()) {
            String str = "TPS " + Formatting.WHITE + "20.0" + (extraTps.isValue() ? " [20.0]" : "");
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Speed
        if (speed.isValue()) {
            double playerSpeed = Math.sqrt(Math.pow(mc.player.getX() - mc.player.prevX, 2) + Math.pow(mc.player.getZ() - mc.player.prevZ, 2)) * 20;
            String str = "Speed " + Formatting.WHITE + MathUtil.round(playerSpeed * (bps.isValue() ? 1f : 3.6f), 1) + (bps.isValue() ? " b/s" : " km/h");
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Biome
        if (biome.isValue()) {
            String str3 = "Biome: " + Formatting.WHITE + getBiome();
            drawText(context, str3, width - getStringWidth(str3) - 2, renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Time
        if (time.isValue()) {
            String str = "Time " + Formatting.WHITE + (new SimpleDateFormat("h:mm a")).format(new Date());
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Offhand Durability
        if (offhandDurability.isValue()) {
            String str = "OffHand" + Formatting.WHITE + " [" + (mc.player.getOffHandStack().getMaxDamage() - mc.player.getOffHandStack().getDamage()) + "]";
            drawText(context, str, (width - getStringWidth(str) - 2), renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Ping
        if (ping.isValue()) {
            String str1 = "Ping " + Formatting.WHITE + getPing();
            drawText(context, str1, width - getStringWidth(str1) - 2, renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // FPS
        if (fps.isValue()) {
            String fpsText = "FPS " + Formatting.WHITE + mc.getCurrentFps();
            drawText(context, fpsText, width - getStringWidth(fpsText) - 2, renderingUp.isValue() ? (height - 2 - (i += offset)) : (2 + i++ * offset));
        }

        // Coordinates and Direction (bottom left)
        boolean inHell = Objects.equals(mc.world.getRegistryKey().getValue().getPath(), "the_nether");
        int posX = (int) mc.player.getX();
        int posY = (int) mc.player.getY();
        int posZ = (int) mc.player.getZ();
        float nether = !inHell ? 0.125F : 8.0F;
        int hposX = (int) (mc.player.getX() * nether);
        int hposZ = (int) (mc.player.getZ() * nether);
        i = (mc.currentScreen instanceof ChatScreen) ? 14 : 0;
        String coordinates = Formatting.WHITE + "XYZ " + Formatting.RESET + (inHell ? 
            (posX + ", " + posY + ", " + posZ + Formatting.WHITE + " [" + Formatting.RESET + hposX + ", " + hposZ + Formatting.WHITE + "]" + Formatting.RESET) : 
            (posX + ", " + posY + ", " + posZ + Formatting.WHITE + " [" + Formatting.RESET + hposX + ", " + hposZ + Formatting.WHITE + "]"));
        String direction1 = "";

        i += offset;

        if (direction.isValue()) {
            switch (mc.player.getHorizontalFacing()) {
                case EAST -> direction1 = "East" + Formatting.WHITE + " [+X]";
                case WEST -> direction1 = "West" + Formatting.WHITE + " [-X]";
                case NORTH -> direction1 = "North" + Formatting.WHITE + " [-Z]";
                case SOUTH -> direction1 = "South" + Formatting.WHITE + " [+Z]";
                case UP, DOWN -> direction1 = "Unknown";
            }
            drawText(context, direction1, 2, (height - i - 11));
        }

        if (coords.isValue()) drawText(context, coordinates, 2, (height - i));
        if (armor.isValue()) renderArmorHUD(true, context);
        if (totems.isValue()) renderTotemHUD(context);
        if (greeter.isValue()) renderGreeter(context);
        
        } catch (Exception e) {
            // Silently handle any rendering errors to prevent crashes
        }
    }

    private void drawText(DrawContext context, String str, int x, int y, int color) {
        if (context == null || str == null || mc.textRenderer == null) return;
        try {
            context.drawText(mc.textRenderer, str, x, y, color, true);
        } catch (Exception e) {
            // Silently handle text rendering errors
        }
    }

    private void drawText(DrawContext context, String str, int x, int y) {
        if (context == null || str == null || mc.textRenderer == null) return;
        try {
            context.drawText(mc.textRenderer, str, x, y, color, true);
        } catch (Exception e) {
            // Silently handle text rendering errors
        }
    }

    private int getStringWidth(String str) {
        if (str == null || mc.textRenderer == null) return 0;
        try {
            return mc.textRenderer.getWidth(str);
        } catch (Exception e) {
            return 0;
        }
    }

    public void renderGreeter(DrawContext context) {
        if (mc.player == null || mc.player.getName() == null) return;
        try {
            String text = "Good " + getTimeOfDay() + mc.player.getName().getString();
            drawText(context, text, (int) (mc.getWindow().getScaledWidth() / 2.0F - getStringWidth(text) / 2.0F + 2.0F), 2);
        } catch (Exception e) {
            // Silently handle greeter rendering errors
        }
    }

    public static String getTimeOfDay() {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (timeOfDay < 12) return "Morning ";
        if (timeOfDay < 16) return "Afternoon ";
        if (timeOfDay < 21) return "Evening ";
        return "Night ";
    }

    public void renderTotemHUD(DrawContext context) {
        if (mc.player == null || context == null) return;
        try {
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();
            int totems = mc.player.getInventory().main.stream()
                    .filter(itemStack -> itemStack != null && itemStack.getItem() == Items.TOTEM_OF_UNDYING)
                    .mapToInt(ItemStack::getCount).sum();
            int u = mc.player.getMaxAir();
            int v = Math.min(mc.player.getAir(), u);
            if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)
                totems += mc.player.getOffHandStack().getCount();
            if (totems > 0) {
                int i = width / 2;
                int y = height - 55 - (mc.player.isSubmergedInWater() || v < u ? 10 : 0);
                int x = i - 189 + 180 + 2;
                context.drawItem(totem, x, y);
                drawText(context, totems + "", 8 + (int) (x - (float) getStringWidth(totems + "") / 2f), (y - 7), 16777215);
            }
        } catch (Exception e) {
            // Silently handle totem rendering errors
        }
    }

    private String getBiome() {
        if (mc.player == null || mc.world == null) return "Unknown";
        return "Plains"; // Simplified for now
    }

    private int getPing() {
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            return mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        }
        return 0;
    }

    private String getDuration(StatusEffectInstance effect) {
        int duration = effect.getDuration();
        int minutes = duration / 1200;
        int seconds = (duration % 1200) / 20;
        return String.format("%d:%02d", minutes, seconds);
    }

    public void renderArmorHUD(boolean percent, DrawContext context) {
        if (mc.player == null || context == null) return;
        try {
            int i = 0;
            int u = mc.player.getMaxAir();
            int v = Math.min(mc.player.getAir(), u);

            int y = mc.getWindow().getScaledHeight() - 55 - (mc.player.isSubmergedInWater() || v < u ? 10 : 0);
            for (ItemStack is : mc.player.getInventory().armor) {
                if (is == null) continue;
                i++;
                if (is.isEmpty())
                    continue;
                int x = (mc.getWindow().getScaledWidth() / 2) - 90 + (9 - i) * 20 + 2;
                context.drawItem(is, x, y);
                String s = (is.getCount() > 1) ? (is.getCount() + "") : "";
                drawText(context, s, (x + 19 - 2 - getStringWidth(s)), (y + 9), 16777215);
                if (percent && is.getMaxDamage() > 0) {
                    float green = (float) (is.getMaxDamage() - is.getDamage()) / (float) is.getMaxDamage();
                    float red = 1.0F - green;
                    int dmg = 100 - (int) (red * 100.0F);

                    drawText(context, dmg + "", (x + 8 - getStringWidth(dmg + "") / 2), (y - 11), 
                        new Color((int) Math.max(0, Math.min(255, red * 255.0F)), (int) Math.max(0, Math.min(255, green * 255.0F)), 0).getRGB());
                }
            }
        } catch (Exception e) {
            // Silently handle armor rendering errors
        }
    }
}
