package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScoreBoard extends AbstractDraggable {
    private List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
    private ScoreboardObjective objective;

    public ScoreBoard() {
        super("Score Board", 10, 100, 100, 120, true);
    }

    @Override
    public boolean visible() {
        return !scoreboardEntries.isEmpty();
    }

    @Override
    public void tick() {
        objective = Objects.requireNonNull(mc.world).getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        scoreboardEntries = mc.world.getScoreboard().getScoreboardEntries(objective).stream().sorted(Comparator.comparing(ScoreboardEntry::value).reversed().thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer fontTitle = Fonts.getSize(14, Fonts.Type.DEFAULT);

        Text mainText = objective != null ? objective.getDisplayName() : Text.empty();

        int padding = 3;
        int titleHeight = 14;

        // GameSense style background like CoolDowns
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .color(ColorUtil.getColor(15, 15, 15, 200)).build());

        // Top accent line like CoolDowns
        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 1)
                .color(ColorUtil.getClientColor()).build());

        // Calculate width based on content
        int maxWidth = 100;
        for (ScoreboardEntry entry : scoreboardEntries) {
            Text entryText = Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(entry.owner()), entry.name());
            int entryWidth = (int) font.getStringWidth(entryText) + padding * 2;
            maxWidth = Math.max(maxWidth, entryWidth);
        }

        int titleWidth = (int) fontTitle.getStringWidth(mainText) + padding * 2;
        maxWidth = Math.max(maxWidth, titleWidth);

        // Draw title centered
        float centerX = getX() + getWidth() / 2.0F;
        fontTitle.drawString(matrix, mainText.getString().toLowerCase(), (int) (centerX - fontTitle.getStringWidth(mainText.getString().toLowerCase()) / 2.0F), getY() + 4, ColorUtil.WHITE);

        // Draw scoreboard entries
        int offset = titleHeight;
        for (ScoreboardEntry entry : scoreboardEntries) {
            Text entryText = Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(entry.owner()), entry.name());
            font.drawText(matrix, entryText, (double) (getX() + padding), (double) (getY() + offset));
            offset += 10;
        }

        setWidth(maxWidth);
        setHeight(offset + padding);
    }
}