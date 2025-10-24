package ru.zenith.implement.features.modules.render;

import ru.zenith.api.event.EventHandler;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.feature.module.ModuleCategory;
import ru.zenith.api.feature.module.setting.implement.BooleanSetting;
import ru.zenith.api.feature.module.setting.implement.ColorSetting;
import ru.zenith.api.feature.module.setting.implement.ValueSetting;
import net.minecraft.util.math.Box;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.Render3DUtil;
import ru.zenith.core.Main;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import ru.zenith.implement.events.render.WorldRenderEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Firefly extends Module {
    private final List<FireflyParticle> fireflies = new ArrayList<>();
    private final List<SparkleParticle> sparkleList = new ArrayList<>();
    private final Random random = new Random();
    private long lastTime = System.currentTimeMillis();
    private float animationTime = 0;
    
    // Settings
    public final ValueSetting count = new ValueSetting("Count", "Number of fireflies").setValue(30).range(5, 100).setInteger(true);
    public final ValueSetting speed = new ValueSetting("Speed", "Movement speed").setValue(0.08f).range(0.01f, 0.3f);
    public final ValueSetting range = new ValueSetting("Range", "Spawn range").setValue(20.0f).range(5.0f, 50.0f);
    public final ValueSetting size = new ValueSetting("Size", "Firefly size").setValue(0.12f).range(0.05f, 0.5f);
    public final BooleanSetting trails = new BooleanSetting("Trails", "Show trails").setValue(true);
    public final ValueSetting trailLength = new ValueSetting("Trail Length", "Trail length").setValue(80).range(20, 150).setInteger(true);
    public final ValueSetting trailFadeTime = new ValueSetting("Trail Fade Time", "Trail fade time").setValue(3.0f).range(1.0f, 6.0f);
    public final BooleanSetting glow = new BooleanSetting("Glow", "Glow effect").setValue(true);
    public final ValueSetting glowLayers = new ValueSetting("Glow Layers", "Glow layers").setValue(4).range(2, 8).setInteger(true);
    public final ValueSetting glowIntensity = new ValueSetting("Glow Intensity", "Glow intensity").setValue(1.5f).range(0.5f, 3.0f);
    public final ColorSetting color = new ColorSetting("Color", "Firefly color").value(0xFFFFC832);
    public final BooleanSetting rainbow = new BooleanSetting("Rainbow", "Rainbow colors").setValue(true);
    public final ValueSetting rainbowSpeed = new ValueSetting("Rainbow Speed", "Rainbow speed").setValue(1.2f).range(0.1f, 5.0f);
    public final BooleanSetting pulse = new BooleanSetting("Pulse", "Pulse effect").setValue(true);
    public final ValueSetting pulseSpeed = new ValueSetting("Pulse Speed", "Pulse speed").setValue(2.0f).range(0.5f, 5.0f);
    public final BooleanSetting sparkles = new BooleanSetting("Sparkles", "Sparkle effects").setValue(true);
    public final ValueSetting sparkleChance = new ValueSetting("Sparkle Chance", "Sparkle chance").setValue(0.1f).range(0.01f, 0.5f);

    public Firefly() {
        super("Firefly", "Adds beautiful firefly particles to your world", ModuleCategory.RENDER);
    }

    @Override
    public void activate() {
        fireflies.clear();
        sparkleList.clear();
        spawnFireflies();
    }

    @Override
    public void deactivate() {
        fireflies.clear();
        sparkleList.clear();
    }

    @EventHandler
    public void onUpdate(ru.zenith.implement.events.player.TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 16.67f;
        lastTime = currentTime;
        
        updateFireflies(deltaTime);
        updateSparkles();
    }

    private void spawnFireflies() {
        if (mc.player == null) return;
        
        Vec3d playerPos = mc.player.getPos();
        
        for (int i = 0; i < count.getInt(); i++) {
            Vec3d pos = new Vec3d(
                playerPos.x + (random.nextDouble() - 0.5) * range.getValue(),
                playerPos.y + random.nextDouble() * 12 - 3,
                playerPos.z + (random.nextDouble() - 0.5) * range.getValue()
            );
            
            Vec3d velocity = new Vec3d(
                (random.nextDouble() - 0.5) * speed.getValue() * 2.5,
                (random.nextDouble() - 0.5) * speed.getValue() * 1.2,
                (random.nextDouble() - 0.5) * speed.getValue() * 2.5
            );
            
            fireflies.add(new FireflyParticle(pos, velocity, i));
        }
    }
    
    private void updateFireflies(float deltaTime) {
        if (mc.player == null) return;
        
        Vec3d playerPos = mc.player.getPos();
        
        // Update existing fireflies
        Iterator<FireflyParticle> iterator = fireflies.iterator();
        while (iterator.hasNext()) {
            FireflyParticle firefly = iterator.next();
            firefly.update(deltaTime, pulse.isValue(), pulseSpeed.getValue());
            
            // Remove fireflies that are too far away
            if (firefly.position.distanceTo(playerPos) > range.getValue() + 15) {
                iterator.remove();
            }
        }
        
        // Spawn new fireflies if needed
        while (fireflies.size() < count.getInt()) {
            Vec3d pos = new Vec3d(
                playerPos.x + (random.nextDouble() - 0.5) * range.getValue(),
                playerPos.y + random.nextDouble() * 12 - 3,
                playerPos.z + (random.nextDouble() - 0.5) * range.getValue()
            );
            
            Vec3d velocity = new Vec3d(
                (random.nextDouble() - 0.5) * speed.getValue() * 2.5,
                (random.nextDouble() - 0.5) * speed.getValue() * 1.2,
                (random.nextDouble() - 0.5) * speed.getValue() * 2.5
            );
            
            fireflies.add(new FireflyParticle(pos, velocity, fireflies.size()));
        }
    }
    
    private void updateSparkles() {
        // Update existing sparkles
        Iterator<SparkleParticle> iterator = sparkleList.iterator();
        while (iterator.hasNext()) {
            SparkleParticle sparkle = iterator.next();
            sparkle.update();
            if (!sparkle.isAlive()) {
                iterator.remove();
            }
        }
        
        // Generate new sparkles randomly
        if (sparkles.isValue() && Math.random() < sparkleChance.getValue()) {
            for (FireflyParticle firefly : fireflies) {
                if (Math.random() < 0.3f) {
                    Vec3d sparklePos = firefly.position.add(
                        (Math.random() - 0.5) * 0.5,
                        (Math.random() - 0.5) * 0.5,
                        (Math.random() - 0.5) * 0.5
                    );
                    Color sparkleColor = getFireflyColor(firefly);
                    sparkleList.add(new SparkleParticle(sparklePos, sparkleColor));
                }
            }
        }
    }
    
    private Color getFireflyColor(FireflyParticle firefly) {
        if (rainbow.isValue()) {
            float hue = (System.currentTimeMillis() * rainbowSpeed.getValue() / 1000.0f + firefly.id * 0.15f) % 1.0f;
            return Color.getHSBColor(hue, 0.85f, 1.0f);
        } else {
            return new Color(color.getColor());
        }
    }
    
    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (fireflies.isEmpty()) return;
        
        for (FireflyParticle firefly : fireflies) {
            Color color = getFireflyColor(firefly);
            
            // Render trails first (behind firefly)
            if (trails.isValue() && firefly.trail.size() > 1) {
                renderTrail(event.getStack(), firefly, color);
            }
            
            // Render main firefly cube
            renderFireflyCube(event.getStack(), firefly.position, color, firefly.brightness);
            
            // Render enhanced glow effect
            if (glow.isValue()) {
                renderEnhancedGlow(event.getStack(), firefly.position, color, firefly.brightness);
            }
        }
        
        // Render sparkles on top
        if (sparkles.isValue()) {
            renderSparkles(event.getStack());
        }
    }
    
    private void renderFireflyCube(MatrixStack matrices, Vec3d pos, Color color, float brightness) {
        float cubeSize = size.getValue();
        int finalAlpha = (int) (color.getAlpha() * brightness);
        Color finalColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), finalAlpha);
        
        Box box = new Box(pos.subtract(cubeSize/2, cubeSize/2, cubeSize/2), pos.add(cubeSize/2, cubeSize/2, cubeSize/2));
        Render3DUtil.drawBox(box, finalColor.getRGB(), 1.0f);
    }
    
    private void renderTrail(MatrixStack matrices, FireflyParticle firefly, Color baseColor) {
        if (firefly.trail.size() < 2) return;
        
        long currentTime = System.currentTimeMillis();
        float fadeTimeMs = trailFadeTime.getValue() * 1000.0f;
        int maxTrailPoints = Math.min(firefly.trail.size(), trailLength.getInt());
        
        for (int i = Math.max(0, firefly.trail.size() - maxTrailPoints); i < firefly.trail.size(); i++) {
            TrailPoint point = firefly.trail.get(i);
            
            float age = currentTime - point.timestamp;
            if (age > fadeTimeMs) continue;
            
            float ageFade = 1.0f - (age / fadeTimeMs);
            float positionFade = (float) (i - (firefly.trail.size() - maxTrailPoints)) / maxTrailPoints;
            float combinedFade = ageFade * positionFade * point.brightness;
            
            if (combinedFade <= 0.05f) continue;
            
            int trailAlpha = (int)(combinedFade * baseColor.getAlpha());
            Color trailColor = new Color(baseColor.getRed(), baseColor.getGreen(), 
                                       baseColor.getBlue(), Math.max(0, Math.min(255, trailAlpha)));
            
            float trailSize = size.getValue() * combinedFade * 0.8f;
            Box trailBox = new Box(point.position.subtract(trailSize/2, trailSize/2, trailSize/2),
                point.position.add(trailSize/2, trailSize/2, trailSize/2));
            Render3DUtil.drawBox(trailBox, trailColor.getRGB(), 1.0f);
        }
    }
    
    private void renderEnhancedGlow(MatrixStack matrices, Vec3d pos, Color color, float brightness) {
        float baseGlowSize = size.getValue() * glowIntensity.getValue();
        int layers = glowLayers.getInt();
        
        for (int i = 0; i < layers; i++) {
            float layerSize = baseGlowSize * (1.0f + i * 0.6f);
            int layerAlpha = Math.max(5, (int) (color.getAlpha() * brightness / (i + 1.5f)));
            Color layerColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), layerAlpha);
            
            Box glowBox = new Box(pos.subtract(layerSize/2, layerSize/2, layerSize/2),
                pos.add(layerSize/2, layerSize/2, layerSize/2));
            Render3DUtil.drawBox(glowBox, layerColor.getRGB(), 1.0f);
        }
    }
    
    private void renderSparkles(MatrixStack matrices) {
        for (SparkleParticle sparkle : sparkleList) {
            float alpha = sparkle.getAlpha();
            Color sparkleColor = new Color(
                sparkle.color.getRed(), 
                sparkle.color.getGreen(), 
                sparkle.color.getBlue(), 
                (int) (alpha * 255)
            );
            
            Box sparkleBox = new Box(sparkle.position.subtract(sparkle.size/2, sparkle.size/2, sparkle.size/2),
                sparkle.position.add(sparkle.size/2, sparkle.size/2, sparkle.size/2));
            Render3DUtil.drawBox(sparkleBox, sparkleColor.getRGB(), 1.0f);
        }
    }
    
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
        public final Color color;
        public final float size;
        
        public SparkleParticle(Vec3d position, Color color) {
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
        
        public FireflyParticle(Vec3d position, Vec3d velocity, int id) {
            this.position = position;
            this.velocity = velocity;
            this.originalVelocity = velocity;
            this.id = id;
            this.targetDirection = velocity.normalize();
            trail.add(new TrailPoint(position, System.currentTimeMillis(), brightness));
        }
        
        public void update(float deltaTime, boolean pulse, float pulseSpeed) {
            time += deltaTime * 0.05f;
            directionChangeTimer += deltaTime;
            
            // Change direction randomly every 1-4 seconds
            if (directionChangeTimer > 60 + (id % 180)) {
                Random rand = new Random(id + (long)(time * 1000));
                targetDirection = new Vec3d(
                    (rand.nextDouble() - 0.5) * 2.0,
                    (rand.nextDouble() - 0.5) * 0.8,
                    (rand.nextDouble() - 0.5) * 2.0
                ).normalize();
                directionChangeTimer = 0;
            }
            
            // Smoothly interpolate towards target direction
            Vec3d currentDir = velocity.normalize();
            Vec3d newDir = currentDir.multiply(0.92).add(targetDirection.multiply(0.08)).normalize();
            
            // Add beautiful floating motion with multiple sine waves
            Vec3d floatMotion = new Vec3d(
                Math.sin(time * 1.2) * 0.003 + Math.sin(time * 0.7) * 0.001,
                Math.cos(time * 0.9) * 0.002 + Math.sin(time * 1.5) * 0.0015,
                Math.sin(time * 1.1) * 0.003 + Math.cos(time * 0.6) * 0.001
            );
            
            velocity = newDir.multiply(originalVelocity.length()).add(floatMotion);
            position = position.add(velocity.multiply(deltaTime));
            
            // Update brightness with pulse effect
            if (pulse) {
                brightness = 0.7f + (float) Math.sin(time * pulseSpeed) * 0.3f;
            } else {
                brightness = 0.8f + (float) Math.sin(time * 0.5f) * 0.2f;
            }
            brightness = Math.max(0.3f, Math.min(1.0f, brightness));
            
            // Add to trail with brightness info
            if (trail.isEmpty() || position.distanceTo(trail.get(trail.size() - 1).position) > 0.04) {
                trail.add(new TrailPoint(position, System.currentTimeMillis(), brightness));
            }
            
            // Remove old trail points
            long currentTime = System.currentTimeMillis();
            trail.removeIf(point -> currentTime - point.timestamp > 4000);
            
            // Limit trail length
            while (trail.size() > 150) {
                trail.remove(0);
            }
        }
    }
}
