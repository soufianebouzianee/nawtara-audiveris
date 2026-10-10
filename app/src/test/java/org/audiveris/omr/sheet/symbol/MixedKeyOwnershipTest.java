package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import java.awt.Rectangle;
import java.util.List;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.junit.Test;
import static org.junit.Assert.*;

public class MixedKeyOwnershipTest
{
    private static final Rectangle KEY = new Rectangle(100, 100, 40, 80);
    private static Glyph ink (int x, int y, int width, int height)
    {
        // Each fragment is fully inked. Overlapping ownership must retain the union once.
        return new Glyph(x, y, new RunTableFactory(Orientation.VERTICAL)
                .createTable(new ByteProcessor(width, height)));
    }
    private static List<Glyph> heads ()
    {
        return List.of(ink(160, 116, 20, 12), ink(160, 140, 20, 12));
    }
    private static List<Glyph> stems () { return List.of(ink(174, 100, 3, 68)); }

    @Test public void reconstructsEveryOwnedInkPixel ()
    {
        var fragments = List.of(ink(164, 106, 3, 62));
        var compound = MixedKeyOwnership.reassemble(fragments, heads(), stems(), KEY, 20);
        assertNotNull(compound);
        assertEquals(new Rectangle(160, 100, 20, 68), compound.getBounds());
        var b = compound.getBuffer();
        for (var group : List.of(fragments, heads(), stems())) {
            for (var glyph : group) {
                var box = glyph.getBounds();
                for (int x = box.x; x < box.x + box.width; x++) {
                    for (int y = box.y; y < box.y + box.height; y++) {
                        assertEquals(0, b.get(x - 160, y - 100));
                    }
                }
            }
        }
        assertEquals(255, b.get(0, 0)); // Do not fill missing ink between pieces.
    }
    @Test public void refusesSingleHeadOrMissingStem ()
    {
        assertNull(MixedKeyOwnership.reassemble(List.of(), heads().subList(0, 1), stems(), KEY, 20));
        assertNull(MixedKeyOwnership.reassemble(List.of(), heads(), List.of(), KEY, 20));
    }
    @Test public void refusesSideBySideMusicalNotes ()
    {
        assertNull(MixedKeyOwnership.reassemble(List.of(),
                List.of(ink(151, 116, 15, 12), ink(167, 140, 15, 12)), stems(), KEY, 20));
    }
    @Test public void refusesOutsideHeaderInkAndInvalidScale ()
    {
        assertNull(MixedKeyOwnership.reassemble(List.of(ink(190, 116, 3, 20)), heads(), stems(), KEY, 20));
        assertNull(MixedKeyOwnership.reassemble(List.of(), heads(), stems(), KEY, 0));
        assertFalse(MixedKeyOwnership.inSlot(new Rectangle(141, 100, 3, 20), KEY, 20));
        assertFalse(MixedKeyOwnership.inSlot(new Rectangle(160, 200, 20, 10), KEY, 20));
    }
    @Test public void refusesShortNoteAndStemCompound ()
    {
        assertNull(MixedKeyOwnership.reassemble(List.of(), heads(),
                List.of(ink(174, 110, 3, 45)), KEY, 20));
    }
    @Test public void duplicateFragmentOwnershipDoesNotDuplicateInk ()
    {
        var a = MixedKeyOwnership.reassemble(List.of(), heads(), stems(), KEY, 20);
        var h = heads();
        var b = MixedKeyOwnership.reassemble(h, h, stems(), KEY, 20);
        assertNotNull(a); assertNotNull(b);
        assertEquals(a.getWeight(), b.getWeight());
    }
}
