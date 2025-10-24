package ru.zenith.api.system.shape;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import org.joml.Vector4i;

@Getter
@Setter
public class ShapeProperties {
    private MatrixStack matrix;
    private float x, y, width, height;
    private float softness, thickness;
    private float start, end;
    private float quality;
    private Vector4f round;
    private int outlineColor;
    private Vector4i color;

    @Builder(toBuilder = true)
    private ShapeProperties(MatrixStack matrix,
                             float x,
                             float y,
                             float width,
                             float height,
                             float softness,
                             float thickness,
                             float start,
                             float end,
                             float quality,
                             Vector4f round,
                             int outlineColor,
                             Vector4i color) {
        this.matrix = matrix;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.softness = softness;
        this.thickness = thickness;
        this.start = start;
        this.end = end;
        this.quality = quality == 0 ? 20F : quality;
        this.round = round != null ? round : new Vector4f(0);
        this.outlineColor = outlineColor;
        this.color = color != null ? color : new Vector4i(-1);
    }

    public static class ShapePropertiesBuilder {

        public ShapePropertiesBuilder color(int color) {
            this.color = new Vector4i(color);
            return this;
        }

        public ShapePropertiesBuilder color(Vector4i color) {
            this.color = color;
            return this;
        }

        public ShapePropertiesBuilder color(int... components) {
            this.color = new Vector4i(components);
            return this;
        }

        public ShapePropertiesBuilder round(float round) {
            this.round = new Vector4f(round);
            return this;
        }

        public ShapePropertiesBuilder round(Vector4f round) {
            this.round = new Vector4f(round);
            return this;
        }

        public ShapePropertiesBuilder round(float... roundValues) {
            switch (roundValues.length) {
                case 0 -> this.round = new Vector4f(0F);
                case 1 -> this.round = new Vector4f(roundValues[0]);
                case 2 -> this.round = new Vector4f(roundValues[0], roundValues[1], roundValues[0], roundValues[1]);
                case 3 -> this.round = new Vector4f(roundValues[0], roundValues[1], roundValues[2], roundValues[1]);
                default -> this.round = new Vector4f(roundValues[0], roundValues[1], roundValues[2], roundValues[3]);
            }
            return this;
        }

        public ShapePropertiesBuilder thickness(float thickness) {
            this.thickness = thickness;
            return this;
        }

        public ShapePropertiesBuilder softness(float softness) {
            this.softness = softness;
            return this;
        }

        public ShapePropertiesBuilder outlineColor(int outline) {
            this.outlineColor = outline;
            return this;
        }
    }

    public static ShapeProperties.ShapePropertiesBuilder create(MatrixStack matrix, double x, double y, double width, double height) {
        return ShapeProperties.builder()
                .matrix(matrix)
                .x((float) x)
                .y((float) y)
                .width((float) width)
                .height((float) height)
                .quality(20F)
                .outlineColor(-1);
    }
}