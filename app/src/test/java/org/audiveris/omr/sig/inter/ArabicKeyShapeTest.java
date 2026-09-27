package org.audiveris.omr.sig.inter;

import static org.audiveris.omr.glyph.Shape.FLAT;
import static org.audiveris.omr.glyph.Shape.QUARTER_FLAT;
import static org.audiveris.omr.glyph.Shape.QUARTER_SHARP;
import static org.audiveris.omr.glyph.Shape.SHARP;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.audiveris.omr.glyph.ShapeSet;

import java.util.List;

import org.junit.Test;

/** Regression coverage for fractional signs in and around key signatures. */
public class ArabicKeyShapeTest
{
    @Test
    public void fractionalSignsCanBelongToKnownKeysButAreNotTraditionalKeyCandidates ()
    {
        assertTrue(ShapeSet.KeyMemberShapes.contains(QUARTER_FLAT));
        assertTrue(ShapeSet.KeyMemberShapes.contains(QUARTER_SHARP));

        // The generic key lookup uses circle-of-fifths pitch maps and must never receive these.
        assertFalse(ShapeSet.KeyValidShapes.contains(QUARTER_FLAT));
        assertFalse(ShapeSet.KeyValidShapes.contains(QUARTER_SHARP));
    }

    @Test
    public void threePositionedGuideNotesRemainNonTraditional ()
    {
        // A B/E/A Arabic guide may independently mix whole-flat and quarter-flat members.
        assertTrue(KeyInter.isNonTraditionalShapes(
                List.of(FLAT, QUARTER_FLAT, QUARTER_FLAT)));

        // Mixed ordinary accidentals also require explicit key-step/key-alter export.
        assertTrue(KeyInter.isNonTraditionalShapes(List.of(FLAT, FLAT, SHARP)));

        // A conventional three-flat signature remains eligible for <fifths>-3</fifths>.
        assertFalse(KeyInter.isNonTraditionalShapes(List.of(FLAT, FLAT, FLAT)));
    }
}
