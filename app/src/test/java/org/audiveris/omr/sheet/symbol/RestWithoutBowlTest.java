package org.audiveris.omr.sheet.symbol;

import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.image.PixelSource.FOREGROUND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.process.ByteProcessor;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.glyph.Shape;
import org.junit.Test;

/**
 * An eighth rest that passes the half-flat topology is told by its foot: a flat's bowl is in
 * its lower third, a rest's tail is thin (Aho Da Elli Sar bar 2, interline 20).
 */
public class RestWithoutBowlTest
{
    private static final double INTERLINE = 20;

    private static ByteProcessor blank (int w,
                                        int h)
    {
        final ByteProcessor b = new ByteProcessor(w, h);
        b.setValue(BACKGROUND);
        b.fill();
        return b;
    }

    private static void fill (ByteProcessor b,
                              int x0,
                              int y0,
                              int x1,
                              int y1)
    {
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                b.set(x, y, FOREGROUND);
            }
        }
    }

    /** Blob at the top, a five-pixel tail down to the foot. */
    private static ByteProcessor eighthRest ()
    {
        final ByteProcessor b = blank(21, 46);
        fill(b, 0, 4, 21, 18);
        fill(b, 10, 18, 16, 46);
        return b;
    }

    /** A stem on the left and a bowl at the foot. */
    private static ByteProcessor flat ()
    {
        final ByteProcessor b = blank(18, 50);
        fill(b, 0, 0, 4, 50);
        fill(b, 0, 32, 16, 48);
        return b;
    }

    /** A slashed flat cut from a note's stem: thin left edge, bowl 0.45 interline wide. */
    private static ByteProcessor narrowFlat ()
    {
        final ByteProcessor b = blank(10, 50);
        fill(b, 0, 0, 4, 50);
        fill(b, 7, 34, 9, 46);
        fill(b, 0, 44, 9, 48);
        return b;
    }

    /** The "(" of a bracketed flat: a thin curved stroke, neither blob nor bowl. */
    private static ByteProcessor parenthesis ()
    {
        final ByteProcessor b = blank(5, 34);
        fill(b, 3, 0, 5, 6);
        fill(b, 0, 5, 3, 29);
        fill(b, 3, 28, 5, 34);
        return b;
    }

    private static Evaluation[] votes (Object... shapeGrades)
    {
        final Evaluation[] evals = new Evaluation[shapeGrades.length / 2];

        for (int i = 0; i < evals.length; i++) {
            evals[i] = new Evaluation((Shape) shapeGrades[2 * i], (Double) shapeGrades[(2 * i) + 1]);
        }

        return evals;
    }

    @Test
    public void weakRestWithoutBowlIsNoHalfFlat ()
    {
        assertTrue(SymbolsBuilder.isRestWithoutBowl(
                votes(Shape.EIGHTH_REST, 0.43), eighthRest(), INTERLINE));
        assertTrue(SymbolsBuilder.isRestWithoutBowl(
                votes(Shape.ONE_16TH_REST, 0.32, Shape.EIGHTH_REST, 0.23), eighthRest(), INTERLINE));
    }

    @Test
    public void halfFlatReadAsWeakRestKeepsItsBowl ()
    {
        assertFalse(SymbolsBuilder.isRestWithoutBowl(
                votes(Shape.ONE_32ND_REST, 0.32), flat(), INTERLINE));
        assertFalse(SymbolsBuilder.isRestWithoutBowl(
                votes(Shape.QUARTER_REST, 0.20), narrowFlat(), INTERLINE));
    }

    @Test
    public void bracketReadAsRestKeepsTheCandidate ()
    {
        assertFalse(SymbolsBuilder.isRestWithoutBowl(
                votes(Shape.QUARTER_REST, 0.595), parenthesis(), INTERLINE));
    }

    @Test
    public void anyNonRestVoteKeepsTheCandidate ()
    {
        assertFalse(SymbolsBuilder.isRestWithoutBowl(
                votes(Shape.EIGHTH_REST, 0.43, Shape.FLAT, 0.2), eighthRest(), INTERLINE));
        assertFalse(SymbolsBuilder.isRestWithoutBowl(votes(), eighthRest(), INTERLINE));
    }
}
