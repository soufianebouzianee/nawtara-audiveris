package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import java.util.List;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic fragments, independent of the private page's pixels and music. */
public class TupletFragmentsTest
{
    private static Glyph block (int x, int y, int width, int height)
    {
        ByteProcessor p = new ByteProcessor(width, height);
        p.setValue(0); p.fill();
        return new Glyph(x, y, new RunTableFactory(Orientation.VERTICAL).createTable(p));
    }

    @Test public void completeDisconnectedNumberOwnsBothFragments ()
    {
        Glyph upper = block(20, 30, 6, 13), lower = block(17, 45, 5, 3);
        Glyph whole = GlyphFactory.buildGlyph(List.of(upper, lower));
        assertTrue(TupletFragments.coversExactly(whole, List.of(upper, lower)));
        assertTrue(TupletFragments.coversExactly(whole, List.of(lower, upper)));
    }

    @Test public void nearbyArticulationOutsideTheNumberAbstains ()
    {
        Glyph upper = block(20, 30, 6, 13), lower = block(17, 45, 5, 3);
        Glyph whole = GlyphFactory.buildGlyph(List.of(upper, lower));
        assertFalse(TupletFragments.coversExactly(whole, List.of(upper, block(28, 45, 3, 3))));
    }

    @Test public void incompleteOwnershipAndDuplicateWholeReadingsAbstain ()
    {
        Glyph upper = block(20, 30, 6, 13), lower = block(17, 45, 5, 3);
        Glyph whole = GlyphFactory.buildGlyph(List.of(upper, lower));
        assertFalse(TupletFragments.coversExactly(whole, List.of(upper)));
        assertFalse(TupletFragments.coversExactly(whole, List.of(upper, block(17, 45, 2, 3))));
        assertFalse(TupletFragments.coversExactly(whole, List.of(whole, upper)));
        assertFalse(TupletFragments.coversExactly(whole, List.of(upper, upper)));
    }

    @Test public void boundingBoxOverlapDoesNotProveInkOwnership ()
    {
        Glyph left = block(10, 20, 2, 10), right = block(18, 20, 2, 10);
        Glyph whole = GlyphFactory.buildGlyph(List.of(left, right));
        assertFalse(TupletFragments.coversExactly(whole, List.of(left, block(14, 20, 2, 10))));
    }
}
