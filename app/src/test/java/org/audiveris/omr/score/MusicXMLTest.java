//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M u s i c X M L T e s t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.score;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.proxymusic.AccidentalValue;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/** Tests for MusicXML values used by the Arabic notation extensions. */
public class MusicXMLTest
{
    @Test
    public void arabicQuarterToneAccidentalsAreExportedPrecisely ()
    {
        assertEquals(AccidentalValue.QUARTER_FLAT, MusicXML.accidentalValueOf(Shape.QUARTER_FLAT));
        assertEquals(AccidentalValue.QUARTER_SHARP, MusicXML.accidentalValueOf(Shape.QUARTER_SHARP));
        assertEquals(
                "accidentalQuarterToneFlatArabic",
                MusicXML.accidentalSmuflOf(Shape.QUARTER_FLAT));
        assertEquals(
                "accidentalQuarterToneSharpArabic",
                MusicXML.accidentalSmuflOf(Shape.QUARTER_SHARP));
        assertEquals(null, MusicXML.accidentalSmuflOf(Shape.SHARP));
        assertEquals(
                BigDecimal.valueOf(-0.5),
                AlterInter.alterationValueOf(
                        new AlterInter(null, Shape.QUARTER_FLAT, 1.0, null, null, null)));
        assertEquals(
                BigDecimal.valueOf(0.5),
                AlterInter.alterationValueOf(
                        new AlterInter(null, Shape.QUARTER_SHARP, 1.0, null, null, null)));
    }
}
