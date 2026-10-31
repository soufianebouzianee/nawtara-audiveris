package org.audiveris.omr.sheet.beam;

import ij.process.ByteProcessor;
import java.awt.geom.Line2D;
import org.junit.Test;
import static org.junit.Assert.*;

/** CC0: original ledger-note fixture, embedded as text. */
public class MultipleRestSerifsTest
{
    private static final String LEDGER_BEAM =
            "iVBORw0KGgoAAAANSUhEUgAAAYsAAACqCAAAAAB5DDxwAAACmklEQVR4nO3d7U7iUBRA0XbC+7/yncTESRxbAUPpbrvWv1GHC2yO" +
            "9ANwHhMRf/a+AvyjRYcWHVp0aNGhRYcWHVp0aAEAwNHNzl9k2L/o0KJDiw4tOrTo0KJDiw4tOrTo0KJDiw4tOrTo0KJDCwA4Fue7" +
            "O2xHdWjRoUWHFh1adGjRoUWHFh1adGjRoUXHbfnL87uvx8UsvuvFXHRo0aEFAACnfB2Ive6t2e8+5vGolXIbmndbdZ9busB+d4cW" +
            "HVp0aNGhRYcWHVp0aNGhRYcWHVp0aNGhBQCc5Hy383pbWb13bUd1aNGhRYcWHVp0aNGhRYcWHVp0aNGhRYcWHVp0aNGhBQAci/Pd" +
            "U+a9s7ajOrTo0KJDiw4tOrTo0CL4OTnX+cyoubqmuYjNxVK1+e37o1eaicV711x03DxTzJv+BnhiEs1Fx83205aeunfNRcfdcGOX" +
            "x8g48JrPrPplTXPRoUWHFh1adGjRsfqUf67PPZh/+N52t3R9Va8DOebn/J/xGG3nlq6s6fkiPhfnm4qx8rt7jyO0q2uai44fD52M" +
            "U/1dmPm/f2+x1r0jUT+uaS5irzP/5XHF116RadvL32aVR+bg82furmou4u+/+PjGQ//91Y+wMbVteq+Yi8P9fb15x22T/Tz6PPqi" +
            "W20uDjAX98ovP2a+f/VIkzI/+HMb3SZzcYDtqKcvaHqlEbpWb5tsc3HCuVi46OmYxl4Lm4tLzMWdhXdYc0xp5uJw+xc7v0LiFHsv" +
            "d5mLi8/Fb58rxnRq5qJDiw4tLv18sc9rWw/AXHRo0aHFdPX97mVjujRz0RGaizFdnLm4+PmLpT2MscP1iDEX09XP632dDDPxwVx0" +
            "7Ha+m2/MRYcWHVp0aNGhRYcWHVp0aNGhxZTxFzc7TCanBhpsAAAAAElFTkSuQmCC";
    private static final String CENTERED_REST =
            "iVBORw0KGgoAAAANSUhEUgAAAPAAAACMCAAAAABn3nv0AAABDElEQVR4nOXaMQqAQAwF0dlh739lxQuoICjrvCJNUvw2JGOjRWIk" +
            "RmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkRmIkZp41x1EW+y++" +
            "yiwxEiMxEiMxEiMxEiMxEiMxEiMxEiMxEjPvbdT/ITESIzESIzESIzESIzESIzESMy8nVrwenpAYiZEYiZEYiZEYiZEYiZEYiZEY" +
            "iZEYiRmL7fePSYzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzESIzE" +
            "+HWAt+07yQYcjTAyawAAAABJRU5ErkJggg==";

    private static ByteProcessor ink (String png) throws Exception
    {
        return new ByteProcessor(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(
                java.util.Base64.getDecoder().decode(png))));
    }

    @Test public void ledgerBeamEndStemsAreNotMultipleRestSerifs () throws Exception
    {
        final ByteProcessor p = ink(LEDGER_BEAM);
        final Line2D line = new Line2D.Double(20, 54, 374, 54);
        assertTrue(MultipleRestSerifs.hasEndStems(p, line, 12, 21, 5));
        p.flipVertical();
        assertTrue(MultipleRestSerifs.hasEndStems(p,
                new Line2D.Double(20, 115, 374, 115), 12, 21, 5));
    }

    @Test public void genuineCenteredSerifsRemainMultipleRestCandidates () throws Exception
    {
        final ByteProcessor p = ink(CENTERED_REST);
        final Line2D line = new Line2D.Double(30, 70, 210, 70);
        assertFalse(MultipleRestSerifs.hasEndStems(p, line, 12, 21, 5));
        assertFalse(MultipleRestSerifs.hasEndStems(null, line, 12, 21, 5));
        assertFalse(MultipleRestSerifs.hasEndStems(p, line, 12, 0, 5));
        // Only one long end stroke is insufficient evidence to keep a beam.
        for (int y = 77; y < 130; y++) {
            for (int x = 28; x <= 32; x++) { p.set(x, y, 0); }
        }
        assertFalse(MultipleRestSerifs.hasEndStems(p, line, 12, 21, 5));
    }
}
