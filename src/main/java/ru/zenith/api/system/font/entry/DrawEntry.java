package ru.zenith.api.system.font.entry;

import ru.zenith.api.system.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
