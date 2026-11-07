package org.audiveris.omr.sig.inter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.List;
import org.junit.Test;

/**
 * A block rest candidate that runs from under one head to under the next is a tie's middle
 * (Aho Da Elli Sar bar 5, b4-A-s-0 capture geometry: interline 19).
 */
public class RestBridgeTest
{
    private static final Rectangle LEFT_HEAD = new Rectangle(457, 1050, 26, 20);

    private static final Rectangle RIGHT_HEAD = new Rectangle(509, 1051, 26, 20);

    @Test
    public void tieMiddleUnderTwoHeadsIsNotARest ()
    {
        final Rectangle tie = new Rectangle(479, 1077, 34, 12);
        assertTrue(RestInter.bridgesTwoHeads(tie, List.of(LEFT_HEAD, RIGHT_HEAD), 5, 19));
    }

    @Test
    public void restBesideOneHeadIsKept ()
    {
        final Rectangle rest = new Rectangle(479, 1077, 34, 12);
        assertFalse(RestInter.bridgesTwoHeads(rest, List.of(LEFT_HEAD), 5, 19));
    }

    @Test
    public void restBetweenHeadsButFarBelowIsKept ()
    {
        final Rectangle rest = new Rectangle(479, 1120, 34, 12);
        assertFalse(RestInter.bridgesTwoHeads(rest, List.of(LEFT_HEAD, RIGHT_HEAD), 5, 19));
    }

    @Test
    public void restInTheGapBetweenHeadsIsKept ()
    {
        final Rectangle left = new Rectangle(400, 1050, 26, 20);
        final Rectangle right = new Rectangle(560, 1050, 26, 20);
        final Rectangle rest = new Rectangle(470, 1060, 34, 12);
        assertFalse(RestInter.bridgesTwoHeads(rest, List.of(left, right), 5, 19));
    }
}
