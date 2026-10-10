package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.junit.Test;
import static org.junit.Assert.*;

public class StemFlagFragmentsTest
{
    private static Glyph ink (int x, int y, int width, int height)
    {
        ByteProcessor p = new ByteProcessor(width, height);
        p.setValue(0); p.fill();
        return new Glyph(x, y, new RunTableFactory(Orientation.VERTICAL).createTable(p));
    }

    private static java.awt.geom.Line2D stem ()
    {
        return new java.awt.geom.Line2D.Double(22, 30, 22, 89);
    }

    @Test public void leftoverStemStripIncludesPixelRoundedBorder ()
    {
        assertTrue(StemFlagFragments.isStemInk(stem(), 4.0, ink(19, 65, 1, 15)));
    }

    @Test public void fractionalWidthRoundsToRasterBeforeTestingSlantedBorder ()
    {
        java.awt.geom.Line2D diagonal = new java.awt.geom.Line2D.Double(22, 30, 20, 90);
        assertTrue(StemFlagFragments.isStemInk(diagonal, 4.6, ink(17, 64, 1, 15)));
        assertFalse(StemFlagFragments.isStemInk(diagonal, 4.6, ink(16, 64, 1, 15)));
    }

    @Test public void flagWithIndependentInkMustRemain ()
    {
        assertFalse(StemFlagFragments.isStemInk(stem(), 4.0, ink(22, 65, 12, 15)));
        assertFalse(StemFlagFragments.isStemInk(stem(), 4.0, ink(26, 65, 1, 15)));
    }

    @Test public void slantedStemAndTailExtentAreRespected ()
    {
        java.awt.geom.Line2D diagonal = new java.awt.geom.Line2D.Double(20, 30, 30, 89);
        assertTrue(StemFlagFragments.isStemInk(diagonal, 4.0, ink(26, 65, 1, 8)));
        assertFalse(StemFlagFragments.isStemInk(diagonal, 4.0, ink(20, 65, 1, 8)));
        assertFalse(StemFlagFragments.isStemInk(stem(), 4.0, ink(22, 85, 1, 15)));
    }

    @Test public void missingStemMeasurementOrInkAbstains ()
    {
        assertFalse(StemFlagFragments.isStemInk(null, 4.0, ink(20, 65, 1, 15)));
        assertFalse(StemFlagFragments.isStemInk(stem(), null, ink(20, 65, 1, 15)));
        assertFalse(StemFlagFragments.isStemInk(stem(), 4.0, null));
    }
}
