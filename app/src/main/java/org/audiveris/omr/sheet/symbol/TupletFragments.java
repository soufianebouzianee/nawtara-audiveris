package org.audiveris.omr.sheet.symbol;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.Relation;

/** Resolve a complete printed triplet against articulations made from its own fragments. */
final class TupletFragments
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TupletFragments.class);

    private TupletFragments () {}

    static void resolve (SIGraph sig)
    {
        for (Inter inter : sig.inters(TupletInter.class)) {
            final TupletInter tuplet = (TupletInter) inter;
            if (tuplet.isManual() || tuplet.getShape() != Shape.TUPLET_THREE
                    || tuplet.getGlyph() == null || tuplet.getGrade() < 0.65) {
                continue;
            }
            final List<AbstractChordInter> chords = new ArrayList<>();
            for (Relation rel : sig.getRelations(tuplet, ChordTupletRelation.class)) {
                final Inter other = sig.getOppositeInter(tuplet, rel);
                if (other instanceof AbstractChordInter chord) { chords.add(chord); }
            }
            if (chords.size() != 3) { continue; }
            final BeamGroupInter beam = chords.get(0).getBeamGroup();
            if (beam == null || chords.stream().anyMatch(c -> c.isRest()
                    || c.getBeamGroup() != beam
                    || !c.getDurationSansTuplet().equals(chords.get(0).getDurationSansTuplet()))) {
                continue;
            }
            final List<Inter> fragments = new ArrayList<>();
            for (Inter articulation : sig.inters(ArticulationInter.class)) {
                if (articulation.getGlyph() != null
                        && SplitRestRecovery.containsInk(tuplet.getGlyph(), articulation.getGlyph())) {
                    fragments.add(articulation);
                }
            }
            if (fragments.stream().anyMatch(Inter::isManual)
                    || !coversExactly(tuplet.getGlyph(),
                            fragments.stream().map(Inter::getGlyph).toList())) {
                continue;
            }
            // Every removed pixel belongs to the classified number, and all of that number
            // is accounted for. A separate articulation near the triplet is never consumed.
            logger.info("Retained split triplet {} over {} owned articulation fragments", tuplet, fragments.size());
            for (Inter fragment : fragments) { fragment.remove(); }
        }
    }

    static boolean coversExactly (Glyph whole, List<Glyph> fragments)
    {
        if (whole == null || fragments.size() < 2) { return false; }
        for (Glyph fragment : fragments) {
            if (fragment == null || !SplitRestRecovery.containsInk(whole, fragment)) { return false; }
        }
        final Rectangle box = whole.getBounds();
        for (int y = box.y; y < box.y + box.height; y++) {
            for (int x = box.x; x < box.x + box.width; x++) {
                final Point p = new Point(x, y);
                if (whole.contains(p) && fragments.stream().noneMatch(g -> g.contains(p))) {
                    return false;
                }
            }
        }
        // Duplicate interpretations of one complete glyph are not a fragmentation.
        return fragments.stream().noneMatch(g -> SplitRestRecovery.containsInk(g, whole));
    }
}
