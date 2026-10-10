package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic ink, never a crop or transcription from the private TRAIN score. */
public class SplitRestRecoveryTest
{
    private static ByteProcessor page ()
    {
        ByteProcessor p = new ByteProcessor(100, 100);
        p.setValue(255);
        p.fill();
        return p;
    }

    private static void hook (ByteProcessor p)
    {
        for (int y = 25; y <= 35; y++) {
            for (int x = 23; x <= 33; x++) {
                if ((x - 28) * (x - 28) + (y - 30) * (y - 30) <= 25) {
                    p.set(x, y, 0);
                }
            }
        }
        for (int x = 28; x <= 42; x++) {
            for (int y = 32; y <= 34; y++) { p.set(x, y, 0); }
        }
        for (int y = 30; y <= 64; y++) {
            int x = 42 - (y - 30) / 3;
            for (int dx = 0; dx < 3; dx++) { p.set(x + dx, y, 0); }
        }
    }

    private static Glyph crop (ByteProcessor page, Rectangle b)
    {
        ByteProcessor p = new ByteProcessor(b.width, b.height);
        for (int y = 0; y < b.height; y++) {
            for (int x = 0; x < b.width; x++) { p.set(x, y, page.get(x + b.x, y + b.y)); }
        }
        return new Glyph(b.x, b.y, new RunTableFactory(Orientation.VERTICAL).createTable(p));
    }

    private static Glyph neck (ByteProcessor p)
    {
        return crop(p, new Rectangle(31, 34, 12, 31));
    }

    @Test public void restoresConnectedHookWithoutChangingThePage ()
    {
        ByteProcessor p = page(); hook(p);
        byte[] before = ((byte[]) p.getPixels()).clone();
        Glyph seed = neck(p);
        Glyph complete = SplitRestRecovery.recover(p, seed, 20);
        assertNotNull(complete);
        assertTrue(SplitRestRecovery.containsInk(complete, seed));
        assertTrue(complete.contains(new java.awt.Point(28, 30)));
        assertArrayEquals(before, (byte[]) p.getPixels());
    }

    @Test public void detachedDotBesideANeckIsNotOwnedInk ()
    {
        ByteProcessor p = page(); hook(p);
        for (int x = 34; x <= 39; x++) {
            for (int y = 25; y <= 35; y++) { p.set(x, y, 255); }
        }
        assertNull(SplitRestRecovery.recover(p, neck(p), 20));
    }

    @Test public void missingSeedPixelsAndMissingPageAbstain ()
    {
        ByteProcessor p = page(); hook(p); Glyph seed = neck(p);
        assertNull(SplitRestRecovery.recover(null, seed, 20));
        assertNull(SplitRestRecovery.recover(page(), seed, 20));
        p.set(38, 45, 255);
        assertNull(SplitRestRecovery.recover(p, seed, 20));
    }

    @Test public void aComponentClippedByTheSearchWindowAbstains ()
    {
        ByteProcessor p = page(); hook(p);
        for (int x = 0; x < 28; x++) { p.set(x, 30, 0); }
        assertNull(SplitRestRecovery.recover(p, neck(p), 20));
    }

    @Test public void aNearbyDisconnectedMarkIsNotConsumed ()
    {
        ByteProcessor p = page(); hook(p);
        p.set(24, 60, 0);
        Glyph complete = SplitRestRecovery.recover(p, neck(p), 20);
        assertNotNull(complete);
        assertFalse(complete.contains(new java.awt.Point(24, 60)));
        assertFalse(SplitRestRecovery.containsInk(complete,
                crop(p, new Rectangle(24, 60, 1, 1))));
    }
}
