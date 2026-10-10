package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;

/** Split residual curve ink from a dot already classified and linked beside a tied head. */
final class TiedDotRecovery
{
    private TiedDotRecovery () {}

    static Glyph body (Glyph seed, Rectangle curve, boolean above, int interline)
    {
        if (seed == null || curve == null || interline <= 0) return null;
        Rectangle b = seed.getBounds();
        if (b.width < .3 * interline || b.width > .65 * interline
                || b.height < .65 * interline || b.height > 1.2 * interline
                || b.height < 1.4 * b.width || !b.intersects(curve)
                || b.x < curve.x || b.x > curve.x + .7 * interline) return null;
        Rectangle lobe = new Rectangle(b);
        if (above) {
            int cut = curve.y + curve.height;
            lobe.y = cut;
            lobe.height = b.y + b.height - cut;
        } else {
            lobe.height = curve.y - b.y;
        }
        if (!b.contains(lobe) || lobe.height < .3 * interline
                || lobe.height > .65 * interline
                || lobe.height < .7 * b.width || lobe.height > 1.3 * b.width) return null;
        ByteProcessor ink = new ByteProcessor(lobe.width, lobe.height);
        ink.setValue(255); ink.fill();
        for (int y=0; y<lobe.height; y++) for (int x=0; x<lobe.width; x++) {
            if (seed.contains(new Point(lobe.x+x, lobe.y+y))) ink.set(x,y,0);
        }
        var components = GlyphFactory.buildGlyphs(
                new RunTableFactory(Orientation.VERTICAL).createTable(ink),
                new Point(lobe.x,lobe.y));
        if (components.size()!=1) return null;
        Glyph result = components.get(0);
        Rectangle rb = result.getBounds();
        if (rb.width < .3*interline || rb.height < .3*interline
                || result.getWeight() < .55*rb.width*rb.height) return null;
        return result;
    }
}
