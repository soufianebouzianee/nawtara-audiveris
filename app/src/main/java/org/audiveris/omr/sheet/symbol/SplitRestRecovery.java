package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;

/** Recover intact ink around a rest neck after curve extraction has taken its hook. */
final class SplitRestRecovery
{
    private SplitRestRecovery () {}

    /**
     * Return a complete, unclipped component containing every pixel of the seed.
     * This only proposes ink: the caller must independently classify the complete
     * component as an eighth rest and apply the normal rest/chord placement checks.
     */
    static Glyph recover (ByteProcessor noStaff, Glyph seed, int interline)
    {
        if ((noStaff == null) || (interline <= 0)) {
            return null;
        }
        final Rectangle s = seed.getBounds();
        if ((s.width < 0.3 * interline) || (s.width > 0.8 * interline)
                || (s.height < 1.2 * interline) || (s.height > 2.2 * interline)) {
            return null;
        }
        final Rectangle crop = new Rectangle(s);
        crop.grow((int) Math.ceil(0.7 * interline), (int) Math.ceil(0.5 * interline));
        if (!new Rectangle(0, 0, noStaff.getWidth(), noStaff.getHeight()).contains(crop)) {
            return null;
        }
        final ByteProcessor buffer = new ByteProcessor(crop.width, crop.height);
        for (int y = 0; y < crop.height; y++) {
            for (int x = 0; x < crop.width; x++) {
                buffer.set(x, y, noStaff.get(crop.x + x, crop.y + y));
            }
        }
        for (Glyph component : GlyphFactory.buildGlyphs(
                new RunTableFactory(Orientation.VERTICAL).createTable(buffer),
                new Point(crop.x, crop.y))) {
            final Rectangle b = component.getBounds();
            if (!b.contains(s) || (b.x <= crop.x) || (b.y <= crop.y)
                    || (b.getMaxX() >= crop.getMaxX()) || (b.getMaxY() >= crop.getMaxY())
                    || (b.width < 0.7 * interline) || (b.width > 1.5 * interline)
                    || (b.height < 1.5 * interline) || (b.height > 2.5 * interline)
                    || (s.x - b.x < 0.25 * interline)
                    || (s.y - b.y < 0.1 * interline)) {
                continue;
            }
            if (containsInk(component, seed)) {
                return component;
            }
        }
        return null;
    }

    /** Bounds alone cannot establish ownership of neighboring ink. */
    static boolean containsInk (Glyph whole, Glyph part)
    {
        final Rectangle b = part.getBounds();
        if (!whole.getBounds().contains(b)) {
            return false;
        }
        for (int y = b.y; y < b.y + b.height; y++) {
            for (int x = b.x; x < b.x + b.width; x++) {
                final Point point = new Point(x, y);
                if (part.contains(point) && !whole.contains(point)) {
                    return false;
                }
            }
        }
        return true;
    }
}
