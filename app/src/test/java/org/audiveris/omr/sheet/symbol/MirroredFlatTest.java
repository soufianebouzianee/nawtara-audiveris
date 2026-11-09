package org.audiveris.omr.sheet.symbol;

import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.image.PixelSource.FOREGROUND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.process.ByteProcessor;

import org.junit.Test;

/** The mirrored flat that Sibelius's OpusSpecial font uses for its half-flat. */
public class MirroredFlatTest
{
    private static final double INTERLINE = 17;

    private static ByteProcessor blank (int w, int h)
    {
        final ByteProcessor b = new ByteProcessor(w, h);
        b.setValue(BACKGROUND);
        b.fill();
        return b;
    }

    private static void fill (ByteProcessor b, int x0, int y0, int x1, int y1)
    {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                b.set(x, y, FOREGROUND);
            }
        }
    }

    private static ByteProcessor mirroredFlat ()
    {
        final ByteProcessor b = blank(16, 40);
        fill(b, 13, 0, 15, 39); // stem at the right edge
        fill(b, 3, 20, 12, 23); // top of the bowl
        fill(b, 1, 23, 5, 31); // left side of the bowl
        fill(b, 4, 31, 12, 37); // foot of the bowl
        return b;
    }

    @Test
    public void readsTheMirroredFlat ()
    {
        assertTrue(SymbolsBuilder.isMirroredFlat(mirroredFlat(), INTERLINE));
    }

    @Test
    public void aNoteWithItsStemUpHasASolidBowl ()
    {
        final ByteProcessor b = blank(18, 40);
        fill(b, 15, 0, 17, 39); // stem at the right edge
        fill(b, 1, 25, 14, 37); // solid head
        assertFalse(SymbolsBuilder.isMirroredFlat(b, INTERLINE));
    }

    @Test
    public void aGlyphWiderThanABowlIsNotAMirroredFlat ()
    {
        final ByteProcessor b = mirroredFlat();
        assertFalse(SymbolsBuilder.isMirroredFlat(b, 12));
    }

    @Test
    public void aPlainFlatHasItsStemOnTheLeft ()
    {
        final ByteProcessor b = blank(16, 40);
        fill(b, 0, 0, 2, 39);
        fill(b, 3, 20, 12, 23);
        fill(b, 10, 23, 15, 31);
        fill(b, 3, 31, 12, 37);
        assertFalse(SymbolsBuilder.isMirroredFlat(b, INTERLINE));
    }

    @Test
    public void aDigitFourHasACrossbarPastItsStem ()
    {
        final ByteProcessor b = blank(18, 40);
        fill(b, 12, 0, 14, 39); // stem
        fill(b, 0, 26, 17, 30); // crossbar passes the stem
        fill(b, 5, 5, 11, 25); // diagonal, in the upper left
        assertFalse(SymbolsBuilder.isMirroredFlat(b, INTERLINE));
    }

    @Test
    public void aStemAloneHasNoBowl ()
    {
        final ByteProcessor b = blank(16, 40);
        fill(b, 13, 0, 15, 39);
        assertFalse(SymbolsBuilder.isMirroredFlat(b, INTERLINE));
    }

    @Test
    public void inkInTheUpperLeftIsNotAMirroredFlat ()
    {
        final ByteProcessor b = mirroredFlat();
        fill(b, 1, 0, 9, 9);
        assertFalse(SymbolsBuilder.isMirroredFlat(b, INTERLINE));
    }
}
