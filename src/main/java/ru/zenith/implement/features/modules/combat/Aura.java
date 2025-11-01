package ru.zenith.implement.features.modules.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import org.joml.Vector4i;
import ru.zenith.api.event.EventHandler;
import ru.zenith.api.event.types.EventType;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.*;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.common.util.task.TaskPriority;
import ru.zenith.core.Main;
import ru.zenith.implement.events.packet.PacketEvent;
import ru.zenith.implement.events.player.RotationUpdateEvent;
import ru.zenith.implement.events.render.WorldRenderEvent;
import ru.zenith.implement.features.draggables.Notifications;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackHandler;
import ru.zenith.implement.features.modules.combat.killaura.attack.AttackPerpetrator;
import ru.zenith.implement.features.modules.combat.killaura.rotation.*;
import ru.zenith.implement.features.modules.combat.killaura.rotation.angle.*;
import ru.zenith.implement.features.modules.combat.killaura.target.TargetSelector;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.*;
import java.util.List;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aura extends Module {
    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    final Animation esp_anim = new DecelerateAnimation().setMs(400).setValue(1);
    final TargetSelector targetSelector = new TargetSelector();
    final PointFinder pointFinder = new PointFinder();
    @NonFinal
    LivingEntity target, lastTarget;

    // Firefly ESP System
    final List<FireflyParticle> targetFireflies = new ArrayList<>();
    final List<SparkleParticle> targetSparkles = new ArrayList<>();
    final Random fireflyRandom = new Random();
    @NonFinal
    long lastFireflyTime = System.currentTimeMillis();

    // Settings
    final ValueSetting distance = new ValueSetting("Distance", "Maximum attack distance")
            .setValue(3.3f).range(1.0f, 6.0f);

    final MultiSelectSetting targetType = new MultiSelectSetting("Target Type", "Filters the entire list of targets by type")
            .value("Players", "Mobs", "Animals", "Friends");

    final MultiSelectSetting attackSetting = new MultiSelectSetting("Attack Setting", "Allows you to customize the attack")
            .value("Only Critical", "Dynamic Cooldown", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls");

    final SelectSetting correctionType = new SelectSetting("Correction Type", "Selects the type of correction")
            .value("Free", "Focused").selected("Free");

    final GroupSetting correctionGroup = new GroupSetting("Move correction", "Prevents detection by movement sensitive anti-cheats")
            .settings(correctionType).setValue(true);

    final SelectSetting aimMode = new SelectSetting("Rotation Type", "Allows you to select the rotation type")
            .value("FunTime", "Snap", "Matrix", "SpookyTime").selected("Snap");

    // Updated ESP types with Fireflies
    final SelectSetting targetEspType = new SelectSetting("Target Esp Type", "Selects the type of target esp")
            .value("Cube", "Circle", "Ghosts", "Crystals", "Fireflies").selected("Circle");

    final ValueSetting ghostSpeed = new ValueSetting("Ghost Speed", "Speed of ghost flying around the target")
            .setValue(1).range(1F, 2F).visible(()-> targetEspType.isSelected("Ghosts"));

    // Crystal settings
    final ValueSetting crystalSpeed = new ValueSetting("Crystal Speed", "Speed of crystal rotation")
            .setValue(1.5f).range(0.5F, 3.0F).visible(()-> targetEspType.isSelected("Crystals"));

    final ValueSetting crystalSize = new ValueSetting("Crystal Size", "Size of crystals")
            .setValue(0.08f).range(0.03F, 0.2F).visible(()-> targetEspType.isSelected("Crystals"));

    final ValueSetting crystalOrbit = new ValueSetting("Crystal Orbit", "Orbit radius of crystals")
            .setValue(1.2f).range(0.5F, 2.5F).visible(()-> targetEspType.isSelected("Crystals"));

    final ValueSetting crystalCount = new ValueSetting("Crystal Count", "Number of crystals")
            .setValue(12).range(4, 24).visible(()-> targetEspType.isSelected("Crystals"));

    // Firefly settings
    final ValueSetting fireflyCount = new ValueSetting("Firefly Count", "Number of fireflies around target")
            .setValue(25).range(8, 50).setInteger(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting fireflySpeed = new ValueSetting("Firefly Speed", "Movement speed")
            .setValue(0.12f).range(0.05f, 0.3f).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting fireflySize = new ValueSetting("Firefly Size", "Firefly size")
            .setValue(0.08f).range(0.04f, 0.2f).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting fireflyRange = new ValueSetting("Firefly Range", "Spawn range around target")
            .setValue(2.5f).range(1.0f, 5.0f).visible(()-> targetEspType.isSelected("Fireflies"));

    final BooleanSetting fireflyTrails = new BooleanSetting("Firefly Trails", "Show trails")
            .setValue(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting trailLength = new ValueSetting("Trail Length", "Trail length")
            .setValue(60).range(20, 120).setInteger(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final BooleanSetting fireflyGlow = new BooleanSetting("Firefly Glow", "Glow effect")
            .setValue(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting glowIntensity = new ValueSetting("Glow Intensity", "Glow intensity")
            .setValue(1.8f).range(0.5f, 3.0f).visible(()-> targetEspType.isSelected("Fireflies"));

    final ColorSetting fireflyColor = new ColorSetting("Firefly Color", "Firefly color")
            .value(0xFFFFC832).visible(()-> targetEspType.isSelected("Fireflies"));

    final BooleanSetting fireflyRainbow = new BooleanSetting("Firefly Rainbow", "Rainbow colors")
            .setValue(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting rainbowSpeed = new ValueSetting("Rainbow Speed", "Rainbow speed")
            .setValue(1.5f).range(0.1f, 5.0f).visible(()-> targetEspType.isSelected("Fireflies"));

    final BooleanSetting fireflyPulse = new BooleanSetting("Firefly Pulse", "Pulse effect")
            .setValue(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting pulseSpeed = new ValueSetting("Pulse Speed", "Pulse speed")
            .setValue(2.2f).range(0.5f, 5.0f).visible(()-> targetEspType.isSelected("Fireflies"));

    final BooleanSetting sparkles = new BooleanSetting("Sparkles", "Sparkle effects")
            .setValue(true).visible(()-> targetEspType.isSelected("Fireflies"));

    final ValueSetting sparkleChance = new ValueSetting("Sparkle Chance", "Sparkle chance")
            .setValue(0.15f).range(0.01f, 0.5f).visible(()-> targetEspType.isSelected("Fireflies"));

    final GroupSetting targetEspGroup = new GroupSetting("Target Esp", "Displays the player in the world")
            .settings(targetEspType, ghostSpeed, crystalSpeed, crystalSize, crystalOrbit, crystalCount,
                    fireflyCount, fireflySpeed, fireflySize, fireflyRange, fireflyTrails, trailLength,
                    fireflyGlow, glowIntensity, fireflyColor, fireflyRainbow, rainbowSpeed,
                    fireflyPulse, pulseSpeed, sparkles, sparkleChance).setValue(true);

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(distance, targetType, attackSetting, correctionGroup, aimMode, targetEspGroup);
    }

    @Override
    public void activate() {
        targetSelector.releaseTarget();
        target = null;
        targetFireflies.clear();
        targetSparkles.clear();
        super.activate();
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        targetFireflies.clear();
        targetSparkles.clear();
        super.deactivate();
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        esp_anim.setDirection(target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = esp_anim.getOutput().floatValue();
        if (targetEspGroup.isValue() && lastTarget != null && !esp_anim.isFinished(Direction.BACKWARDS)) {
            float tickDelta = 1.0f;
            float red = MathHelper.clamp((lastTarget.hurtTime - tickDelta) / 10, 0, 1);
            switch (targetEspType.getSelected()) {
                case "Cube" -> Render3DUtil.drawCube(lastTarget, anim, red);
                case "Circle" -> Render3DUtil.drawCircle(e.getStack(), lastTarget, anim, red);
                case "Ghosts" -> Render3DUtil.drawGhosts(lastTarget, anim, red, ghostSpeed.getValue());
                case "Crystals" -> renderCrystals(e.getStack(), lastTarget, anim, red);
                case "Fireflies" -> renderFireflies(e.getStack(), lastTarget, anim, red);
            }
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();
                if (target != null) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                    updateFireflySystem();
                } else {
                    targetFireflies.clear();
                    targetSparkles.clear();
                }
            }
            case EventType.POST -> {
                Render3DUtil.updateTargetEsp();
                if (target != null) {
                    performAttack();
                }
            }
        }
    }

    // Firefly ESP System
    private void updateFireflySystem() {
        if (lastTarget == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFireflyTime) / 16.67f;
        lastFireflyTime = currentTime;

        updateTargetFireflies(deltaTime);
        updateTargetSparkles();
    }

    private void spawnTargetFireflies(LivingEntity target) {
        if (target == null) return;

        Vec3d targetPos = getTargetPosition(target);
        int fireflyCountValue = fireflyCount.getInt();

        for (int i = 0; i < fireflyCountValue; i++) {
            Vec3d pos = new Vec3d(
                    targetPos.x + (fireflyRandom.nextDouble() - 0.5) * fireflyRange.getValue(),
                    targetPos.y + fireflyRandom.nextDouble() * target.getHeight(),
                    targetPos.z + (fireflyRandom.nextDouble() - 0.5) * fireflyRange.getValue()
            );

            Vec3d velocity = new Vec3d(
                    (fireflyRandom.nextDouble() - 0.5) * fireflySpeed.getValue() * 2.0,
                    (fireflyRandom.nextDouble() - 0.5) * fireflySpeed.getValue() * 1.5,
                    (fireflyRandom.nextDouble() - 0.5) * fireflySpeed.getValue() * 2.0
            );

            targetFireflies.add(new FireflyParticle(pos, velocity, i, target));
        }
    }

    private void updateTargetFireflies(float deltaTime) {
        if (lastTarget == null) return;

        Vec3d targetPos = getTargetPosition(lastTarget);

        // Update existing fireflies
        Iterator<FireflyParticle> iterator = targetFireflies.iterator();
        while (iterator.hasNext()) {
            FireflyParticle firefly = iterator.next();
            firefly.update(deltaTime, fireflyPulse.isValue(), pulseSpeed.getValue(), targetPos);

            // Remove fireflies that are too far away
            if (firefly.position.distanceTo(targetPos) > fireflyRange.getValue() + 2) {
                iterator.remove();
            }
        }

        // Spawn new fireflies if needed
        while (targetFireflies.size() < fireflyCount.getInt()) {
            Vec3d pos = new Vec3d(
                    targetPos.x + (fireflyRandom.nextDouble() - 0.5) * fireflyRange.getValue(),
                    targetPos.y + fireflyRandom.nextDouble() * lastTarget.getHeight(),
                    targetPos.z + (fireflyRandom.nextDouble() - 0.5) * fireflyRange.getValue()
            );

            Vec3d velocity = new Vec3d(
                    (fireflyRandom.nextDouble() - 0.5) * fireflySpeed.getValue() * 2.0,
                    (fireflyRandom.nextDouble() - 0.5) * fireflySpeed.getValue() * 1.5,
                    (fireflyRandom.nextDouble() - 0.5) * fireflySpeed.getValue() * 2.0
            );

            targetFireflies.add(new FireflyParticle(pos, velocity, targetFireflies.size(), lastTarget));
        }
    }

    private void updateTargetSparkles() {
        // Update existing sparkles
        Iterator<SparkleParticle> iterator = targetSparkles.iterator();
        while (iterator.hasNext()) {
            SparkleParticle sparkle = iterator.next();
            sparkle.update();
            if (!sparkle.isAlive()) {
                iterator.remove();
            }
        }

        // Generate new sparkles randomly
        if (sparkles.isValue() && Math.random() < sparkleChance.getValue()) {
            for (FireflyParticle firefly : targetFireflies) {
                if (Math.random() < 0.2f) {
                    Vec3d sparklePos = firefly.position.add(
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3,
                            (Math.random() - 0.5) * 0.3
                    );
                    int sparkleColor = getFireflyColor(firefly);
                    targetSparkles.add(new SparkleParticle(sparklePos, sparkleColor));
                }
            }
        }
    }

    private Vec3d getTargetPosition(LivingEntity target) {
        return new Vec3d(
                MathHelper.lerp(1.0f, target.lastRenderX, target.getX()),
                MathHelper.lerp(1.0f, target.lastRenderY, target.getY()) + target.getHeight() / 2,
                MathHelper.lerp(1.0f, target.lastRenderZ, target.getZ())
        );
    }

    private int getFireflyColor(FireflyParticle firefly) {
        if (fireflyRainbow.isValue()) {
            float hue = (System.currentTimeMillis() * rainbowSpeed.getValue() / 1000.0f + firefly.id * 0.1f) % 1.0f;
            return java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
        } else {
            return fireflyColor.getColor();
        }
    }

    private void renderFireflies(MatrixStack matrixStack, LivingEntity entity, float animProgress, float hurtProgress) {
        if (entity == null || animProgress <= 0) return;
        if (targetFireflies.isEmpty()) {
            spawnTargetFireflies(entity);
            return;
        }

        for (FireflyParticle firefly : targetFireflies) {
            int color = getFireflyColor(firefly);

            // Apply hurt effect
            if (hurtProgress > 0) {
                int hurtColor = 0xFFFF5555;
                color = overlayColor(color, hurtColor, hurtProgress * 0.6f);
            }

            // Render trails first (behind firefly)
            if (fireflyTrails.isValue() && firefly.trail.size() > 1) {
                renderFireflyTrail(matrixStack, firefly, color);
            }

            // Render main firefly
            renderFireflyCube(matrixStack, firefly.position, color, firefly.brightness * animProgress);

            // Render enhanced glow effect
            if (fireflyGlow.isValue()) {
                renderFireflyGlow(matrixStack, firefly.position, color, firefly.brightness * animProgress);
            }
        }

        // Render sparkles on top
        if (sparkles.isValue()) {
            renderFireflySparkles(matrixStack);
        }
    }

    private void renderFireflyCube(MatrixStack matrices, Vec3d pos, int color, float brightness) {
        float cubeSize = fireflySize.getValue();
        int finalColor = multiplyColorAlpha(color, brightness);

        Box box = new Box(
                pos.subtract(cubeSize/2, cubeSize/2, cubeSize/2),
                pos.add(cubeSize/2, cubeSize/2, cubeSize/2)
        );
        Render3DUtil.drawBox(box, finalColor, 1.0f, true, true, false);
    }

    private void renderFireflyTrail(MatrixStack matrices, FireflyParticle firefly, int baseColor) {
        if (firefly.trail.size() < 2) return;

        long currentTime = System.currentTimeMillis();
        float fadeTimeMs = 2000.0f; // 2 seconds fade time
        int maxTrailPoints = Math.min(firefly.trail.size(), trailLength.getInt());

        for (int i = Math.max(0, firefly.trail.size() - maxTrailPoints); i < firefly.trail.size(); i++) {
            TrailPoint point = firefly.trail.get(i);

            float age = currentTime - point.timestamp;
            if (age > fadeTimeMs) continue;

            float ageFade = 1.0f - (age / fadeTimeMs);
            float positionFade = (float) (i - (firefly.trail.size() - maxTrailPoints)) / maxTrailPoints;
            float combinedFade = ageFade * positionFade * point.brightness;

            if (combinedFade <= 0.05f) continue;

            int trailColor = multiplyColorAlpha(baseColor, combinedFade * 0.7f);

            float trailSize = fireflySize.getValue() * combinedFade * 0.6f;
            Box trailBox = new Box(
                    point.position.subtract(trailSize/2, trailSize/2, trailSize/2),
                    point.position.add(trailSize/2, trailSize/2, trailSize/2)
            );
            Render3DUtil.drawBox(trailBox, trailColor, 1.0f, true, true, false);
        }
    }

    private void renderFireflyGlow(MatrixStack matrices, Vec3d pos, int color, float brightness) {
        float baseGlowSize = fireflySize.getValue() * glowIntensity.getValue();

        for (int i = 0; i < 3; i++) {
            float layerSize = baseGlowSize * (1.0f + i * 0.8f);
            int layerColor = multiplyColorAlpha(color, brightness / (i + 2.0f));

            Box glowBox = new Box(
                    pos.subtract(layerSize/2, layerSize/2, layerSize/2),
                    pos.add(layerSize/2, layerSize/2, layerSize/2)
            );
            Render3DUtil.drawBox(glowBox, layerColor, 1.0f, true, true, false);
        }
    }

    private void renderFireflySparkles(MatrixStack matrices) {
        for (SparkleParticle sparkle : targetSparkles) {
            float alpha = sparkle.getAlpha();
            int sparkleColor = multiplyColorAlpha(sparkle.color, alpha);

            Box sparkleBox = new Box(
                    sparkle.position.subtract(sparkle.size/2, sparkle.size/2, sparkle.size/2),
                    sparkle.position.add(sparkle.size/2, sparkle.size/2, sparkle.size/2)
            );
            Render3DUtil.drawBox(sparkleBox, sparkleColor, 1.0f, true, true, false);
        }
    }

    // Crystal ESP (existing)
    private void renderCrystals(MatrixStack matrixStack, LivingEntity entity, float animProgress, float hurtProgress) {
        if (entity == null || animProgress <= 0) return;

        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d targetPos = getTargetPosition(entity);
        boolean canSee = mc.player.canSee(entity);
        double time = System.currentTimeMillis() / 1000.0;
        int totalCrystals = (int) crystalCount.getValue();

        for (int i = 0; i < totalCrystals; i++) {
            double angle = time * crystalSpeed.getValue() + i * (2 * Math.PI / totalCrystals);
            double radius = crystalOrbit.getValue() * (1.0 + 0.1 * Math.sin(time * 2.0 + i));

            double crystalX = Math.cos(angle) * radius;
            double crystalZ = Math.sin(angle) * radius;
            double crystalY = 0.3 * Math.sin(time * 3.0 + i);

            renderSingleCrystal(
                    matrixStack, camera, targetPos.subtract(camera.getPos()),
                    (float)crystalX, (float)(crystalY + entity.getHeight() * 0.7), (float)crystalZ,
                    time, i, animProgress, hurtProgress, entity, canSee, 1.0f, 1.0f
            );
        }
    }

    private void renderSingleCrystal(MatrixStack matrixStack, Camera camera, Vec3d targetPos,
                                     float crystalX, float crystalY, float crystalZ,
                                     double time, int index, float animProgress,
                                     float hurtProgress, LivingEntity entity, boolean canSee,
                                     float scaleMultiplier, float alphaMultiplier) {

        MatrixStack matrices = new MatrixStack();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(
                targetPos.x + crystalX,
                targetPos.y + crystalY,
                targetPos.z + crystalZ
        );

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(time * 50 + index * 30)));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(time * 40 + index * 25)));

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        MatrixStack.Entry entry = matrices.peek().copy();

        int baseColor = getCrystalColor(entity, hurtProgress, index, time);
        float pulse = (float)(0.7 + 0.3 * Math.sin(time * 4.0 + index));
        int pulseColor = multiplyColorBrightness(baseColor, pulse);
        float alpha = animProgress * alphaMultiplier;
        int finalColor = multiplyColorAlpha(pulseColor, alpha);

        float baseSize = crystalSize.getValue() * 3.0f;
        float animatedSize = baseSize * scaleMultiplier * (0.8f + 0.2f * (float)Math.sin(time * 3.0 + index));

        Render3DUtil.drawTexture(
                entry,
                getBloomTexture(),
                -animatedSize / 2,
                -animatedSize / 2,
                animatedSize,
                animatedSize,
                new Vector4i(finalColor, finalColor, finalColor, finalColor),
                canSee
        );
    }

    private int getCrystalColor(LivingEntity entity, float hurtProgress, int index, double time) {
        int hue = (int)((System.currentTimeMillis() / 50 + index * 30) % 360);
        int baseColor = fadeColor(hue);

        if (hurtProgress > 0) {
            int hurtColor = 0xFFFF5555;
            return overlayColor(baseColor, hurtColor, hurtProgress * 0.8f);
        }

        if (entity.hasStatusEffect(StatusEffects.POISON)) {
            int poisonColor = 0xFF55FF55;
            float pulse = (float)(0.5 + 0.5 * Math.sin(time * 8.0));
            return overlayColor(baseColor, poisonColor, 0.6f * pulse);
        }

        return baseColor;
    }

    // Utility methods
    private int fadeColor(int hue) {
        float normalizedHue = (hue % 360) / 360.0f;
        int r = (int)(Math.sin(normalizedHue * Math.PI * 2 + 0) * 127 + 128);
        int g = (int)(Math.sin(normalizedHue * Math.PI * 2 + 2) * 127 + 128);
        int b = (int)(Math.sin(normalizedHue * Math.PI * 2 + 4) * 127 + 128);
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private int multiplyColorBrightness(int color, float multiplier) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * multiplier);
        int g = (int)(((color >> 8) & 0xFF) * multiplier);
        int b = (int)((color & 0xFF) * multiplier);

        r = MathHelper.clamp(r, 0, 255);
        g = MathHelper.clamp(g, 0, 255);
        b = MathHelper.clamp(b, 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int multiplyColorAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        a = MathHelper.clamp(a, 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int overlayColor(int baseColor, int overlayColor, float strength) {
        int r1 = (baseColor >> 16) & 0xFF;
        int g1 = (baseColor >> 8) & 0xFF;
        int b1 = baseColor & 0xFF;
        int a1 = (baseColor >> 24) & 0xFF;

        int r2 = (overlayColor >> 16) & 0xFF;
        int g2 = (overlayColor >> 8) & 0xFF;
        int b2 = overlayColor & 0xFF;

        int r = (int)(r1 + (r2 - r1) * strength);
        int g = (int)(g1 + (g2 - g1) * strength);
        int b = (int)(b1 + (b2 - b1) * strength);

        r = MathHelper.clamp(r, 0, 255);
        g = MathHelper.clamp(g, 0, 255);
        b = MathHelper.clamp(b, 0, 255);

        return (a1 << 24) | (r << 16) | (g << 8) | b;
    }

    private net.minecraft.util.Identifier getBloomTexture() {
        try {
            return (net.minecraft.util.Identifier) Render3DUtil.class.getDeclaredField("bloom").get(null);
        } catch (Exception e) {
            return net.minecraft.util.Identifier.of("textures/particle/glitter_7.png");
        }
    }

    // Firefly Particle Classes
    private static class TrailPoint {
        public final Vec3d position;
        public final long timestamp;
        public final float brightness;

        public TrailPoint(Vec3d position, long timestamp, float brightness) {
            this.position = position;
            this.timestamp = timestamp;
            this.brightness = brightness;
        }
    }

    private static class SparkleParticle {
        public Vec3d position;
        public float life;
        public final float maxLife;
        public final int color;
        public final float size;

        public SparkleParticle(Vec3d position, int color) {
            this.position = position;
            this.maxLife = this.life = 30 + (float) Math.random() * 20;
            this.color = color;
            this.size = 0.02f + (float) Math.random() * 0.05f;
        }

        public void update() {
            life--;
        }

        public boolean isAlive() {
            return life > 0;
        }

        public float getAlpha() {
            return Math.max(0, Math.min(1, life / maxLife));
        }
    }

    private static class FireflyParticle {
        public Vec3d position;
        public Vec3d velocity;
        public final List<TrailPoint> trail = new ArrayList<>();
        public final int id;
        private float time = 0;
        private final Vec3d originalVelocity;
        private Vec3d targetDirection;
        private float directionChangeTimer = 0;
        public float brightness = 1.0f;
        private final LivingEntity target;

        public FireflyParticle(Vec3d position, Vec3d velocity, int id, LivingEntity target) {
            this.position = position;
            this.velocity = velocity;
            this.originalVelocity = velocity;
            this.id = id;
            this.target = target;
            this.targetDirection = velocity.normalize();
            trail.add(new TrailPoint(position, System.currentTimeMillis(), brightness));
        }

        public void update(float deltaTime, boolean pulse, float pulseSpeed, Vec3d targetPos) {
            time += deltaTime * 0.05f;
            directionChangeTimer += deltaTime;

            // Change direction randomly
            if (directionChangeTimer > 80 + (id % 120)) {
                Random rand = new Random(id + (long)(time * 1000));
                targetDirection = new Vec3d(
                        (rand.nextDouble() - 0.5) * 1.5,
                        (rand.nextDouble() - 0.5) * 0.6,
                        (rand.nextDouble() - 0.5) * 1.5
                ).normalize();
                directionChangeTimer = 0;
            }

            // Move towards target center if too far
            double distanceToTarget = position.distanceTo(targetPos);
            if (distanceToTarget > 3.0) {
                Vec3d toTarget = targetPos.subtract(position).normalize().multiply(0.02);
                targetDirection = targetDirection.multiply(0.9).add(toTarget.multiply(0.1)).normalize();
            }

            Vec3d currentDir = velocity.normalize();
            Vec3d newDir = currentDir.multiply(0.94).add(targetDirection.multiply(0.06)).normalize();

            // Floating motion
            Vec3d floatMotion = new Vec3d(
                    Math.sin(time * 1.2) * 0.002,
                    Math.cos(time * 0.9) * 0.0015,
                    Math.sin(time * 1.1) * 0.002
            );

            velocity = newDir.multiply(originalVelocity.length()).add(floatMotion);
            position = position.add(velocity.multiply(deltaTime));

            // Update brightness
            if (pulse) {
                brightness = 0.7f + (float) Math.sin(time * pulseSpeed) * 0.3f;
            } else {
                brightness = 0.8f + (float) Math.sin(time * 0.5f) * 0.2f;
            }
            brightness = Math.max(0.4f, Math.min(1.0f, brightness));

            // Add to trail
            if (trail.isEmpty() || position.distanceTo(trail.get(trail.size() - 1).position) > 0.03) {
                trail.add(new TrailPoint(position, System.currentTimeMillis(), brightness));
            }

            // Remove old trail points
            long currentTime = System.currentTimeMillis();
            trail.removeIf(point -> currentTime - point.timestamp > 3000);

            while (trail.size() > 100) {
                trail.remove(0);
            }
        }
    }

    // Rest of the existing Aura methods...
    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance().addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 3000);
            }
        }
    }

    private void performAttack() {
        mc.player.setSprinting(false);
        AttackPerpetrator.AttackPerpetratorConfigurable config = getConfig();
        Main.getInstance().getAttackPerpetrator().performAttack(config);
    }

    private LivingEntity updateTarget() {
        TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(targetType.getSelected());
        targetSelector.searchTargets(mc.world.getEntities(), distance.getValue(), 360, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    public RotationConfig getRotationConfig() {
        return new RotationConfig(getSmoothMode(), correctionGroup.isValue(), correctionType.isSelected("Free"));
    }

    private void rotateToTarget(AttackPerpetrator.AttackPerpetratorConfigurable config) {
        AttackHandler attackHandler = Main.getInstance().getAttackPerpetrator().getAttackHandler();
        RotationController controller = RotationController.INSTANCE;
        Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());
        RotationConfig rotationConfig = getRotationConfig();

        switch (aimMode.getSelected()) {
            case "Snap" -> {
                if (attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(100)) {
                    controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }
            case "FunTime" -> {
                if (attackHandler.canAttack(config, 3)) {
                    controller.clear();
                    controller.rotateTo(rotation, target, 40, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }
            case "Matrix" -> controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            case "SpookyTime" -> {
                controller.rotateTo(rotation, target, 70, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }
        }
    }

    public AttackPerpetrator.AttackPerpetratorConfigurable getConfig() {
        Pair<Vec3d, Box> point = pointFinder.computeVector(target, distance.getValue(),
                RotationController.INSTANCE.getRotation(), getSmoothMode().randomValue(),
                attackSetting.isSelected("Ignore The Walls"));

        Angle angle = AngleUtil.fromVec3d(point.getLeft().subtract(Objects.requireNonNull(mc.player).getEyePos()));
        Box box = point.getRight();
        return new AttackPerpetrator.AttackPerpetratorConfigurable(target, angle, distance.getValue(), attackSetting.getSelected(), aimMode, box);
    }

    public AngleSmoothMode getSmoothMode() {
        return switch (aimMode.getSelected()) {
            case "FunTime" -> new FunTimeSmoothMode();
            case "Matrix" -> new MatrixSmoothMode();
            case "SpookyTime" -> new SpookyTimeSmoothMode();
            case "Snap" -> new SnapSmoothMode();
            default -> new LinearSmoothMode();
        };
    }
}