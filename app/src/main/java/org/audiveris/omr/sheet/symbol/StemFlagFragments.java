package org.audiveris.omr.sheet.symbol;

import java.util.ArrayList;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Relation;

/** A leftover slice of an already beamed stem is not additional flag ink. */
final class StemFlagFragments
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StemFlagFragments.class);

    private StemFlagFragments () {}

    static void resolve (SIGraph sig)
    {
        for (Inter flag : new ArrayList<>(sig.inters(FlagInter.class))) {
            if (flag.isManual()) { continue; }
            for (Relation rel : sig.getRelations(flag, FlagStemRelation.class)) {
                if (rel.isManual()) { continue; }
                final Inter other = sig.getOppositeInter(flag, rel);
                if (other instanceof StemInter stem && !stem.getBeams().isEmpty()
                        && isStemInk(stem.getMedian(), stem.getWidth(), flag.getGlyph())) {
                    logger.info("Removed beamed-stem fragment {} from {}", flag, stem);
                    flag.remove();
                    break;
                }
            }
        }
    }

    static boolean isStemInk (Line2D median, Double width, Glyph flag)
    {
        if (median == null || width == null || width <= 0 || flag == null) { return false; }
        final Rectangle box = flag.getBounds();
        final double radius = Math.ceil(width) / 2 + 1; // Raster width rounds upward.
        if (box.width > width + 2 || box.y < Math.min(median.getY1(), median.getY2())
                || box.y + box.height - 1 > Math.max(median.getY1(), median.getY2())) {
            return false;
        }
        for (int y = box.y; y < box.y + box.height; y++) {
            for (int x = box.x; x < box.x + box.width; x++) {
                if (flag.contains(new Point(x, y)) && median.ptSegDist(x + 0.5, y + 0.5) > radius) {
                    return false;
                }
            }
        }
        return true;
    }
}
