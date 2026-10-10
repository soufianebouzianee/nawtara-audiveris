package org.audiveris.omr.sheet.grid;

import ij.process.ByteProcessor;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class StaffProjectionRangeTest
{
    @Test public void clippedProjectionMeasuresThroughThePrintedStaffEnd ()
    {
        ByteProcessor image = new ByteProcessor(220, 70);
        image.setValue(255); image.fill();
        // Five slanted two-pixel lines, continuing far beyond the initial search window.
        for (int x = 10; x <= 179; x++) {
            for (int line = 0; line < 5; line++) {
                int y = 8 + line * 10 + x / 50;
                image.set(x, y, 0); image.set(x, y + 1, 0);
            }
        }
        int end = StaffProjectionRange.rightLimit(100, 219, 4, 20, x -> {
            int count = 0;
            for (int y = 8 + x / 50; y <= 48 + x / 50; y++) {
                if (image.get(x, y) == 0) { count++; }
            }
            return count;
        });
        assertEquals("Include measured blank after the real end", 199, end);
    }

    @Test public void genuineBlankAtSearchLimitDoesNotExtend ()
    {
        AtomicInteger calls = new AtomicInteger();
        assertEquals(100, StaffProjectionRange.rightLimit(100, 219, 4, 20, x -> {
            calls.incrementAndGet(); return x <= 100 ? 0 : 10;
        }));
        assertEquals(1, calls.get());
    }

    @Test public void shortBreakDoesNotHideContinuedStaffButWideGapStopsBeforeNeighbor ()
    {
        AtomicInteger last = new AtomicInteger();
        int end = StaffProjectionRange.rightLimit(100, 300, 4, 20, x -> {
            last.set(x);
            return (x >= 110 && x <= 112) || (x >= 180 && x <= 209) ? 0 : 10;
        });
        assertEquals(199, end);
        assertEquals("Never sample a neighboring staff beyond a real wide blank", 199, last.get());
    }

    @Test public void inkToImageBorderIsClamped ()
    {
        assertEquals(120, StaffProjectionRange.rightLimit(100, 120, 4, 20, x -> 10));
        assertEquals(120, StaffProjectionRange.rightLimit(120, 120, 4, 20, x -> {
            fail("No out-of-image sampling"); return 0;
        }));
    }

    @Test public void thresholdAndExactBlankWidthAreRespected ()
    {
        assertEquals(100, StaffProjectionRange.rightLimit(100, 200, 4, 3, x -> 4));
        assertEquals(104, StaffProjectionRange.rightLimit(100, 200, 4, 3,
                x -> x >= 102 ? 4 : 5));
    }
}
