package org.audiveris.omr.sheet.symbol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/** A clef change has music before it; a time signature read as a clef has none. */
public class ClefBeforeMusicTest
{
    @Test
    public void noHeadYetBeforeTheGlyph ()
    {
        assertTrue(SymbolsBuilder.noHeadBefore(100, List.of(150, 200)));
    }

    @Test
    public void aRealChangeFollowsNotes ()
    {
        assertFalse(SymbolsBuilder.noHeadBefore(180, List.of(150, 200)));
    }

    @Test
    public void anEmptyStaffHasNoMusicBefore ()
    {
        assertTrue(SymbolsBuilder.noHeadBefore(100, List.of()));
    }
}
