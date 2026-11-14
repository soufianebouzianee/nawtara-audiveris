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

    static int leftLimit (int initialLeft, int imageLeft, int blankThreshold,
                          int blankWidth, IntUnaryOperator countAt)
    {
        if (initialLeft <= imageLeft || countAt.applyAsInt(initialLeft) <= blankThreshold) {
            return initialLeft;
        }

        int blankRun = 0;
        for (int x = initialLeft - 1; x >= imageLeft; x--) {
            blankRun = countAt.applyAsInt(x) <= blankThreshold ? blankRun + 1 : 0;
            if (blankRun >= Math.max(1, blankWidth)) {
                return x;
            }
        }
        return imageLeft;
    }
}
