//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 S y m b o l s S t e p T e s t                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.sheet.symbol;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests for keeping a weak rest that looks like the page's confident ones. */
public class SymbolsStepTest
{
    /** in-rah-mennak-ya-ein from a screen: confident eighth rests, 17x33 px at interline 17. */
    private static final double[] MODELS = {1.0, 1.94, -0.2};

    @Test
    public void aWeakRestLikeTheOthersIsKept ()
    {
        // Bar 3's rest, graded 0.22: same glyph, same height on the staff.
        assertTrue(SymbolsStep.looksLike(new double[]{1.0, 1.94, -0.1}, MODELS));
    }

    @Test
    public void aCandidateOffTheRestsLineIsNot ()
    {
        // Graded 0.16 as an eighth rest, seven steps below the rests: a note's flag.
        assertFalse(SymbolsStep.looksLike(new double[]{0.88, 1.76, 7.7}, MODELS));
    }

    @Test
    public void aCandidateOfAnotherSizeIsNot ()
    {
        // Graded 0.28 as an eighth rest, 19x18 px: half a rest's height.
        assertFalse(SymbolsStep.looksLike(new double[]{1.12, 1.06, -0.2}, MODELS));
    }
}
