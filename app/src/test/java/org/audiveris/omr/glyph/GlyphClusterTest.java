/*
 * Copyright © Audiveris 2026. All rights reserved.
 * This software is released under the GNU General Public License.
 * Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
 */
package org.audiveris.omr.glyph;

import ij.process.ByteProcessor;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Synthetic disconnected staff-cut fragments; no corpus pixels. */
public class GlyphClusterTest
{
    private static Glyph fragment (int left, int top, boolean descending)
    {
        ByteProcessor pixels = new ByteProcessor(3, 3);
        pixels.setValue(255);
        pixels.fill();
        for (int i = 0; i < 3; i++) {
            pixels.set(i, descending ? i : 2 - i, 0);
        }
        return new Glyph(left, top,
                new RunTableFactory(Orientation.VERTICAL).createTable(pixels));
    }

    private static List<String> trials (List<Glyph> seeds, boolean reverseNeighbors)
    {
        List<String> evaluated = new ArrayList<>();
        List<Glyph> all = new ArrayList<>(seeds);
        new GlyphCluster(new GlyphCluster.AbstractAdapter(all, 100) {
            @Override public List<Glyph> getParts () { return seeds; }
            @Override public List<Glyph> getNeighbors (Glyph part) {
                List<Glyph> neighbors = new ArrayList<>(all);
                neighbors.remove(part);
                if (reverseNeighbors) Collections.reverse(neighbors);
                return neighbors;
            }
            @Override public void evaluateGlyph (Glyph glyph, Set<Glyph> parts) {
                List<Integer> positions = new ArrayList<>();
                for (Glyph part : parts) positions.add(part.getLeft());
                Collections.sort(positions);
                evaluated.add(positions.toString());
            }
        }, null, true).decompose();
        return evaluated;
    }

    @Test public void equalWeightFragmentsHaveStableTrialsAndNoLostCompounds ()
    {
        List<Glyph> original = Arrays.asList(fragment(10, 10, true),
                fragment(20, 5, false), fragment(30, 15, true));
        List<String> expected = trials(new ArrayList<>(original), false);
        assertEquals(7, expected.size());
        assertEquals(7, new HashSet<>(expected).size());
        for (int i = 0; i < 6; i++) {
            List<Glyph> shuffled = new ArrayList<>(original);
            Collections.shuffle(shuffled, new Random(i));
            List<Glyph> untouched = new ArrayList<>(shuffled);
            assertEquals(expected, trials(shuffled, true));
            assertEquals("The adapter owns its seed list", untouched, shuffled);
        }
    }

    @Test public void inkBreaksSameBoundsAndWeightTiesWithoutGlyphIds ()
    {
        Glyph first = fragment(10, 10, true);
        Glyph second = fragment(10, 10, false);
        assertNotEquals(0, GlyphCluster.byReverseWeight.compare(first, second));
        assertEquals(-GlyphCluster.byReverseWeight.compare(first, second),
                GlyphCluster.byReverseWeight.compare(second, first));
        assertEquals(0, GlyphCluster.byReverseWeight.compare(first,
                fragment(10, 10, true)));
    }
}
