package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic dot/curve remnants; no private score pixels. */
public class TiedDotRecoveryTest
{
    private static Glyph seed (boolean above)
    {
        ByteProcessor ink = new ByteProcessor(10, 19);
        ink.setValue(255); ink.fill();
        // Round lobe, with a vertical remnant left by erasing a touching curve.
        for (int y=0; y<19; y++) for (int x=0; x<10; x++) {
            int yy = above ? y : 18-y;
            if ((x-4)*(x-4)+(yy-13)*(yy-13)<=25
                    || (yy<=12 && x>=3 && x<=5)) ink.set(x,y,0);
        }
        return new Glyph(40,50,new RunTableFactory(Orientation.VERTICAL).createTable(ink));
    }

    @Test public void separatesUpperCurveFromLowerDotWithoutMutatingSeed ()
    {
        Glyph original = seed(true);
        int weight = original.getWeight();
        Glyph body = TiedDotRecovery.body(original,new Rectangle(38,40,80,19),true,20);
        assertNotNull(body);
        assertEquals(new Rectangle(40,59,10,10),body.getBounds());
        assertTrue(body.contains(new Point(44,63)));
        assertFalse(body.contains(new Point(44,52)));
        assertEquals(weight,original.getWeight());
        assertTrue(original.contains(new Point(44,52)));
    }

    @Test public void mirroredLowerCurveLeavesUpperDot ()
    {
        Glyph body = TiedDotRecovery.body(seed(false),new Rectangle(38,60,80,19),false,20);
        assertNotNull(body);
        assertEquals(new Rectangle(40,50,10,10),body.getBounds());
        assertTrue(body.contains(new Point(44,55)));
        assertFalse(body.contains(new Point(44,66)));
    }

    @Test public void ordinaryRoundDotIsNotTrimmed ()
    {
        Glyph body = TiedDotRecovery.body(seed(true),new Rectangle(38,40,80,19),true,20);
        assertNull(TiedDotRecovery.body(body,new Rectangle(38,55,80,5),true,20));
    }

    @Test public void disconnectedCurveAndMiddleOfCurveAbstain ()
    {
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(38,20,80,8),true,20));
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(5,40,80,19),true,20));
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(45,40,80,19),true,20));
    }

    @Test public void tooMuchOrTooLittleCutAbstains ()
    {
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(38,40,80,25),true,20));
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(38,40,80,14),true,20));
    }

    @Test public void sparseAndDisconnectedInkCannotBecomeDot ()
    {
        ByteProcessor ink = new ByteProcessor(10,19); ink.setValue(255); ink.fill();
        for(int y=0;y<19;y++) ink.set(4,y,0);
        Glyph sparse = new Glyph(40,50,new RunTableFactory(Orientation.VERTICAL).createTable(ink));
        assertNull(TiedDotRecovery.body(sparse,new Rectangle(38,40,80,19),true,20));
        ink.set(9,18,0);
        Glyph disconnected = new Glyph(40,50,new RunTableFactory(Orientation.VERTICAL).createTable(ink));
        assertNull(TiedDotRecovery.body(disconnected,new Rectangle(38,40,80,19),true,20));
    }

    @Test public void invalidGeometryAbstains ()
    {
        assertNull(TiedDotRecovery.body(null,new Rectangle(38,40,80,19),true,20));
        assertNull(TiedDotRecovery.body(seed(true),null,true,20));
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(38,40,80,19),true,0));
        assertNull(TiedDotRecovery.body(seed(true),new Rectangle(38,40,80,19),true,10));
    }
}
