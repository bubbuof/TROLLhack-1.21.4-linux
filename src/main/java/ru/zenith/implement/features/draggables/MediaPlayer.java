package ru.zenith.implement.features.draggables;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.BufferUtil;
import ru.zenith.common.util.other.Instance;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.render.Hud;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayer extends AbstractDraggable {
    public static MediaPlayer getInstance() {
        return Instance.getDraggable(MediaPlayer.class);
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MediaInfo mediaInfo = new MediaInfo("Название Трека", "Артист", new byte[0], 43, 150, false);
    private final Identifier artwork = Identifier.of("textures/xyu.png");
    private final StopWatch lastMedia = new StopWatch();
    public IMediaSession session;
    private float widthDuration;

    public MediaPlayer() {
        super("Media Player", 10, 400, 100, 40,true);
    }

    @Override
    public boolean visible() {
        return !lastMedia.finished(2000) || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Media Player") && mc.player.age % 5 == 0) executorService.submit(() -> {
            IMediaSession currentSession = session = MediaPlayerInfo.Instance.getMediaSessions().stream().max(Comparator.comparing(s -> s.getMedia().getPlaying())).orElse(null);
            if (currentSession != null) {
                MediaInfo info = currentSession.getMedia();
                if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty()) {
                    if (mediaInfo.getTitle().equals("Название Трека") || !Arrays.toString(mediaInfo.getArtworkPng()).equals(Arrays.toString(info.getArtworkPng()))) {
                        BufferUtil.registerTexture(artwork, info.getArtworkPng());
                    }
                    mediaInfo = info;
                    lastMedia.reset();
                }
            }
        });
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        ScissorManager scissor = Main.getInstance().getScissorManager();
        FontRenderer big = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer mini = Fonts.getSize(12, Fonts.Type.DEFAULT);
        int sizeArtwork = 28;
        int maxDurationWidth = getWidth() - (sizeArtwork + 10);
        int duration = (int) mediaInfo.getDuration();
        int position = MathHelper.clamp((int) mediaInfo.getPosition(), 0, duration);
        String timeDuration = StringUtil.getDuration(duration);
        widthDuration = MathHelper.clamp(MathUtil.interpolateSmooth(1, widthDuration, Math.round((float) position / duration * maxDurationWidth)), 1, maxDurationWidth);

        // GameSense style background
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());
        
        // Top accent line
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        // Text area with scissor
        scissor.push(matrix.peek().getPositionMatrix(), getX() + sizeArtwork + 6, getY(), getWidth() - sizeArtwork - 8, getHeight());
        big.drawStringWithScroll(matrix, mediaInfo.getTitle(), getX() + sizeArtwork + 6, getY() + 5, 50, ColorUtil.WHITE);
        mini.drawStringWithScroll(matrix, mediaInfo.getArtist(), getX() + sizeArtwork + 6, getY() + 14, 50, ColorUtil.getColor(180, 180, 180));
        scissor.pop();

        // Album artwork with border
        rectangle.render(ShapeProperties.create(matrix, getX() + 3, getY() + 3, sizeArtwork, sizeArtwork)
                .color(ColorUtil.getColor(40, 40, 40)).build());
        Render2DUtil.drawTexture(context, artwork, getX() + 4, getY() + 4, sizeArtwork - 2, 3, sizeArtwork - 2, sizeArtwork - 2, sizeArtwork - 2, ColorUtil.WHITE);

        // Time display
        mini.drawString(matrix, StringUtil.getDuration(position), getX() + 6 + sizeArtwork, getY() + 25, ColorUtil.getColor(160, 160, 160));
        mini.drawString(matrix, timeDuration, getX() + getWidth() - 4 - mini.getStringWidth(timeDuration), getY() + 25, ColorUtil.getColor(160, 160, 160));

        // Progress bar background
        rectangle.render(ShapeProperties.create(matrix, getX() + 6 + sizeArtwork, getY() + getHeight() - 6, maxDurationWidth, 2)
                .color(ColorUtil.getColor(40, 40, 40)).build());

        // Progress bar fill
        rectangle.render(ShapeProperties.create(matrix, getX() + 6 + sizeArtwork, getY() + getHeight() - 6, widthDuration, 2)
                .color(ColorUtil.getClientColor()).build());

        // Play/pause indicator
        String playState = mediaInfo.getPlaying() ? "▐▐" : "▶";
        mini.drawString(matrix, playState, getX() + getWidth() - 15, getY() + 5, ColorUtil.getClientColor());
    }
}
