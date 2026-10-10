package org.audiveris.omr.sheet.symbol;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;

/** Complete ink for a trailing header sharp consumed as two crossbar heads and a stem. */
final class MixedKeyOwnership
{
    private MixedKeyOwnership () {}

    static boolean inSlot (Rectangle box, Rectangle keyBox, int interline)
    {
        if ((box == null) || (keyBox == null) || (interline <= 0)) { return false; }
        final int right = keyBox.x + keyBox.width;
        return (box.x >= right + Math.max(2, interline / 2))
                && (box.x + box.width <= right + 5 * interline / 2)
                && (Math.abs(box.getCenterY() - keyBox.getCenterY()) <= 2 * interline);
    }

    static Glyph reassemble (List<Glyph> fragments, List<Glyph> heads, List<Glyph> stems,
                             Rectangle keyBox, int interline)
    {
        if ((heads.size() != 2) || stems.isEmpty() || (interline <= 0)
                || heads.stream().anyMatch(g -> g == null) || stems.stream().anyMatch(g -> g == null)) { return null; }
        final Rectangle first = heads.get(0).getBounds();
        final Rectangle second = heads.get(1).getBounds();
        if ((first.x >= second.x + second.width) || (second.x >= first.x + first.width)) {
            return null;
        }
        final List<Glyph> all = new ArrayList<>(fragments);
        all.addAll(heads);
        all.addAll(stems);
        if (all.stream().anyMatch(g -> (g == null) || !inSlot(g.getBounds(), keyBox, interline))) {
            return null;
        }
        final Glyph compound = GlyphFactory.buildGlyph(all);
        final Rectangle box = compound.getBounds();
        // Same proportions as the existing fragmented-sharp fallback.
        final double width = (double) box.width / interline;
        final double height = (double) box.height / interline;
        return ((width >= 0.75) && (width <= 1.5) && (height >= 3.0) && (height <= 4.5))
                ? compound : null;
    }
}
