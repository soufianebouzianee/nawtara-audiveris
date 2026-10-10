package org.audiveris.omr.sheet.grid;

import java.util.function.IntUnaryOperator;

/** Measures past a search boundary when it still cuts through staff ink. */
final class StaffProjectionRange
{
    private StaffProjectionRange () {}

    static int rightLimit (int initialRight, int imageRight, int blankThreshold,
                           int blankWidth, IntUnaryOperator countAt)
    {
        if (initialRight >= imageRight || countAt.applyAsInt(initialRight) <= blankThreshold) {
            return initialRight;
        }

        int blankRun = 0;
        for (int x = initialRight + 1; x <= imageRight; x++) {
            blankRun = countAt.applyAsInt(x) <= blankThreshold ? blankRun + 1 : 0;
            if (blankRun >= Math.max(1, blankWidth)) {
                return x;
            }
        }
        return imageRight;
    }
}
