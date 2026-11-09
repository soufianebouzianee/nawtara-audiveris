package org.audiveris.omr.sheet.symbol;

import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.image.PixelSource.FOREGROUND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ij.process.ByteProcessor;

import org.junit.Test;

/** Sibelius's OpusSpecial half-sharp measures up to 0.68 interline wide on a photo. */
public class NarrowQuarterSharpWidthTest
{
    private static final double INTERLINE = 19;

    private static ByteProcessor sign (int width)
    {
        final ByteProcessor b = new ByteProcessor(width, 50);
        b.setValue(BACKGROUND);
        b.fill();
        final int stem = width - 7;
        for (int y = 0; y < 50; y++) {
            for (int x = stem; x < stem + 4; x++) { b.set(x, y, FOREGROUND); }
        }
        for (int y : new int[] {12, 13, 14, 15, 16, 17, 32, 33, 34, 35, 36, 37}) {
            for (int x = 0; x < stem; x++) { b.set(x, y, FOREGROUND); }
        }
        return b;
    }

    @Test
    public void readsAWiderNarrowSign ()
    {
        assertTrue(SymbolsBuilder.hasNarrowQuarterSharpTopology(sign(12), INTERLINE)); // 0.63
    }

    @Test
    public void refusesOneWiderStill ()
    {
        assertFalse(SymbolsBuilder.hasNarrowQuarterSharpTopology(sign(15), INTERLINE)); // 0.79
    }
}
