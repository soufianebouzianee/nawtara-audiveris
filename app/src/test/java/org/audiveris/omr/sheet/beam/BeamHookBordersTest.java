package org.audiveris.omr.sheet.beam;

import java.awt.geom.Line2D;
import ij.process.ByteProcessor;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.lag.BasicLag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.sheet.Scale;
import org.junit.Test;
import static org.junit.Assert.*;

public class BeamHookBordersTest
{
    private static Line2D line (double x1, double y1, double x2, double y2)
    { return new Line2D.Double(x1, y1, x2, y2); }

    private static Line2D recover (Line2D top, Line2D bottom, Line2D hook, boolean missingTop)
    { return BeamHookBorders.missingBorder(top, bottom, hook, missingTop, 10, 7, 20, 15, 3, .15, 7, 18); }

    @Test public void shortEndHookCanCompleteItsHiddenTop ()
    {
        Line2D result = recover(line(0, 0, 40, 8), line(0, 10, 40, 18),
                               line(26, 25.2, 40, 28), true);
        assertNotNull(result);
        assertEquals(26, result.getX1(), 0);
        assertEquals(15.2, result.getY1(), .0001);
        assertEquals(40, result.getX2(), 0);
        assertEquals(18, result.getY2(), .0001);
    }

    @Test public void leftHookAndHookAboveBeamAreSymmetric ()
    {
        assertNotNull(recover(line(0, 0, 40, 0), line(0, 10, 40, 10), line(0, 25, 14, 25), true));
        Line2D upper = recover(line(0, 0, 40, 0), line(0, 10, 40, 10), line(26, -15, 40, -15), false);
        assertNotNull(upper);
        assertEquals(-5, upper.getY1(), 0);
    }

    @Test public void longPartialBorderIsNotASecondFullBeam ()
    {
        assertNull(recover(line(0, 0, 40, 0), line(0, 10, 40, 10), line(5, 25, 40, 25), true));
        assertNull(recover(line(0, 0, 22, 0), line(0, 10, 22, 10), line(2, 25, 22, 25), true));
    }

    @Test public void middleDetachedAndTinyBlobsAbstain ()
    {
        Line2D top = line(0, 0, 40, 0), bottom = line(0, 10, 40, 10);
        assertNull(recover(top, bottom, line(12, 25, 26, 25), true));
        assertNull(recover(top, bottom, line(35, 25, 49, 25), true));
        assertNull(recover(top, bottom, line(36, 25, 40, 25), true));
    }

    @Test public void thickBeamAndDivergingOrDistantBordersAbstain ()
    {
        Line2D top = line(0, 0, 40, 0), bottom = line(0, 10, 40, 10);
        assertNull(recover(top, line(0, 24, 40, 24), line(26, 39, 40, 39), true));
        assertNull(recover(top, bottom, line(26, 24, 40, 30), true));
        assertNull(recover(top, bottom, line(26, 40, 40, 40), true));
        assertNull(recover(top, bottom, line(26, 14, 40, 14), true));
        assertNull(recover(null, bottom, line(26, 25, 40, 25), true));
    }

    private static BeamStructure joinedShape (boolean above, boolean fullWidth)
    {
        ByteProcessor p = new ByteProcessor(60, 26);
        p.setValue(255); p.fill();
        for (int x = 0; x < 60; x++) {
            for (int y = 0; y < 10; y++) { p.set(x, y, 0); }
        }
        for (int x = fullWidth ? 0 : 40; x < 60; x++) {
            for (int y = 10; y < 26; y++) { p.set(x, y, 0); }
        }
        if (above) { p.flipVertical(); }
        Glyph glyph = new Glyph(20, 30, new RunTableFactory(Orientation.VERTICAL).createTable(p));
        Scale scale = new Scale(new Scale.InterlineScale(9, 10, 11),
                                new Scale.LineScale(1, 1, 2), null, null, null);
        return new BeamStructure(glyph, new BasicLag(Lags.SPOT_LAG, Orientation.VERTICAL),
                                 new BeamsBuilder.ItemParameters(scale, 10, false));
    }

    @Test public void joinedEndHookProducesTwoSeparateBeamItems ()
    {
        for (boolean above : new boolean[] {false, true}) {
            BeamStructure s = joinedShape(above, false);
            assertNotNull(s.computeLines());
            assertEquals(2, s.getLines().size());
            s.adjustSides();
            double shortWidth = s.getLines().stream()
                    .mapToDouble(l -> l.median.getX2() - l.median.getX1()).min().orElseThrow();
            assertTrue("Short hook must not extend across the full beam", shortWidth <= 20);
        }
    }

    @Test public void uniformThickBlobDoesNotBecomeTwoBeams ()
    {
        BeamStructure s = joinedShape(false, true);
        assertNotNull(s.computeLines());
        assertEquals(1, s.getLines().size());
    }
}
