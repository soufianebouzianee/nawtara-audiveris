package org.audiveris.omr.sig.inter;

import static org.junit.Assert.assertEquals;

import org.audiveris.omr.glyph.Shape;

import java.awt.Point;
import java.awt.Rectangle;

import org.junit.Test;

/** Where an accidental is linked to its note (emta-hataref's A half-flats, chunk-1). */
public class AlterLinkPointTest
{
    /** The half-flat glyph of emta-hataref bar 36: 33x71 px at interline 25, its A head at y 2604. */
    private static final Rectangle HALF_FLAT = new Rectangle(1218, 2554, 33, 71);

    @Test
    public void aHalfFlatMeetsItsNoteAtTheBowlLikeAFlat ()
    {
        final Point point = AlterInter.linkPoint(Shape.QUARTER_FLAT, HALF_FLAT);

        assertEquals(1251, point.x);
        assertEquals(2607, point.y); // three quarters down, level with the head (2604)
        assertEquals(AlterInter.linkPoint(Shape.FLAT, HALF_FLAT), point);
    }

    @Test
    public void otherSignsMeetTheirNoteHalfWayDown ()
    {
        assertEquals(2589, AlterInter.linkPoint(Shape.SHARP, HALF_FLAT).y);
        assertEquals(2589, AlterInter.linkPoint(Shape.NATURAL, HALF_FLAT).y);
    }
}
