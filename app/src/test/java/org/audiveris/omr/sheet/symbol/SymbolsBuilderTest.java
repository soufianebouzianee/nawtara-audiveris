//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S y m b o l s B u i l d e r T e s t                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.omr.sheet.symbol;

import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import static org.audiveris.omr.image.PixelSource.FOREGROUND;

import ij.process.ByteProcessor;
import java.awt.Rectangle;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests for Arabic accidental raster discrimination. */
public class SymbolsBuilderTest
{
    //--------------------------------------------------------------------------------------------//
    // Ink beside a key flat's stem, measured on the page (walnba-yagmil, 2alo-tra)                //
    //--------------------------------------------------------------------------------------------//
    private static final int IL = 20;

    private static final Rectangle FLAT_BOX = new Rectangle(100, 30, 13, 66);

    /** A staff of 2-pixel lines, a space of 20 pixels, with a flat whose stem is at x=100. */
    private static ByteProcessor staffPage (boolean slashed,
                                            boolean neighbour)
    {
        final ByteProcessor page = blank(220, 170);

        for (int line = 0; line < 5; line++) {
            horizontal(page, 0, 219, 40 + (line * IL));
            horizontal(page, 0, 219, 41 + (line * IL));
        }

        drawFlat(page, 100, 30);

        if (slashed) {
            // A slash drawn low, rising through the stem and across a staff line.
            thickDiagonal(page, 88, 66, 106, 50, 1);
        }

        if (neighbour) {
            drawFlat(page, 79, 20); // the sign before, a space to the left
        }

        return page;
    }

    private static void drawFlat (ByteProcessor buffer,
                                  int x,
                                  int y)
    {
        for (int dx = 0; dx <= 2; dx++) {
            vertical(buffer, x + dx, y, y + 65);
        }

        for (int row = y + 48; row <= (y + 65); row++) {
            horizontal(buffer, x + 3, x + 12 - ((row - y - 48) / 3), row);
        }
    }

    /** The flat as its glyph comes out of staff removal: no line, and no slash. */
    private static ByteProcessor flatGlyph ()
    {
        final ByteProcessor glyph = blank(13, 66);
        drawFlat(glyph, 0, 0);
        return glyph;
    }

    @Test
    public void aPlainFlatHasNoInkLeftOfItsStem ()
    {
        final SymbolsBuilder.StemInk ink = SymbolsBuilder.inkBesideStem(
                staffPage(false, false), FLAT_BOX, flatGlyph(), IL);

        assertTrue(ink.leftShare() < 0.035);
        assertTrue(SymbolsBuilder.crossingVerdict(null, ink, true) == null);
    }

    @Test
    public void aSlashCutOffTheGlyphIsFoundOnThePage ()
    {
        // walnba-yagmil: the glyph lost the slash to staff removal; the page still has it.
        final SymbolsBuilder.StemInk ink = SymbolsBuilder.inkBesideStem(
                staffPage(true, false), FLAT_BOX, flatGlyph(), IL);

        assertTrue(ink.leftShare() >= 0.135);
        assertFalse(ink.neighbourStem());
        assertTrue(SymbolsBuilder.crossingVerdict(null, ink, true)
                == org.audiveris.omr.glyph.Shape.QUARTER_FLAT);
    }

    @Test
    public void onlyAKeysFirstSignIsPromoted ()
    {
        // A later sign has another sign's bowl beside its stem.
        final SymbolsBuilder.StemInk ink = SymbolsBuilder.inkBesideStem(
                staffPage(true, false), FLAT_BOX, flatGlyph(), IL);

        assertTrue(SymbolsBuilder.crossingVerdict(null, ink, false) == null);
    }

    @Test
    public void aStemASpaceToTheLeftBlocksThePromotion ()
    {
        // The real first sign was missed, and its bowl stands beside this stem.
        final SymbolsBuilder.StemInk ink = SymbolsBuilder.inkBesideStem(
                staffPage(true, true), FLAT_BOX, flatGlyph(), IL);

        assertTrue(ink.neighbourStem());
        assertTrue(SymbolsBuilder.crossingVerdict(null, ink, true) == null);
    }

    @Test
    public void aPromotionWithNothingBesideTheStemIsWithdrawn ()
    {
        // 2alo-tra: JPEG damage at a flat's top read as a slash, with no ink left of the stem.
        final SymbolsBuilder.StemInk ink = SymbolsBuilder.inkBesideStem(
                staffPage(false, false), FLAT_BOX, flatGlyph(), IL);

        assertTrue(SymbolsBuilder.crossingVerdict(
                org.audiveris.omr.glyph.Shape.QUARTER_FLAT, ink, false) == null);
    }

    @Test
    public void aLaterKeySignWithABarlineBesideItIsNoSign ()
    {
        // aghnia-3shriin: a repeat sign after the key, read as a third flat.
        final SymbolsBuilder.StemInk barline = new SymbolsBuilder.StemInk(0.6, true);

        assertTrue(SymbolsBuilder.isBarlineNotSign(barline, false));
        assertFalse(SymbolsBuilder.isBarlineNotSign(barline, true));
        assertFalse(SymbolsBuilder.isBarlineNotSign(new SymbolsBuilder.StemInk(0.3, true), false));
        assertFalse(SymbolsBuilder.isBarlineNotSign(new SymbolsBuilder.StemInk(0.6, false), false));
        assertFalse(SymbolsBuilder.isBarlineNotSign(null, false));
    }

    @Test
    public void noStemMeansNoVerdictChange ()
    {
        assertTrue(SymbolsBuilder.inkBesideStem(
                staffPage(false, false), FLAT_BOX, blank(13, 66), IL) == null);
        assertTrue(SymbolsBuilder.crossingVerdict(
                org.audiveris.omr.glyph.Shape.QUARTER_FLAT, null, true)
                == org.audiveris.omr.glyph.Shape.QUARTER_FLAT);
    }

    @Test
    public void aSlashedFlatIsAFlatOfTheKeyWhateverItWasClassifiedAs ()
    {
        // aa-dm3a's B half-flat on a scan: the slash reaches a space left of the stem, and the
        // classifier reads the whole as a cut-time.
        final ByteProcessor halfFlat = blank(39, 68);

        for (int x = 17; x <= 22; x++) {
            vertical(halfFlat, x, 0, 67);
        }

        thickDiagonal(halfFlat, 37, 10, 0, 41, 2);

        for (int y = 27; y <= 57; y++) {
            horizontal(halfFlat, 20, Math.min(38, 31 + ((57 - y) / 3)), y);
        }

        assertTrue(SymbolsBuilder.isSlashedFlat(halfFlat));
    }

    @Test
    public void aPlainFlatOrACutTimeIsNotASlashedFlat ()
    {
        assertFalse(SymbolsBuilder.isSlashedFlat(thickFlat(false)));

        // A cut-time: a C open to the right, and a line through it.
        final ByteProcessor cutTime = blank(30, 60);

        for (int y = 10; y <= 49; y++) {
            horizontal(cutTime, 4, 7, y);
        }

        horizontal(cutTime, 4, 24, 10);
        horizontal(cutTime, 4, 24, 11);
        horizontal(cutTime, 4, 24, 48);
        horizontal(cutTime, 4, 24, 49);

        for (int x = 13; x <= 15; x++) {
            vertical(cutTime, x, 0, 59);
        }

        assertFalse(SymbolsBuilder.isSlashedFlat(cutTime));
    }

    @Test
    public void narrowWesternNaturalHasNoArabicFallback ()
    {
        final ByteProcessor natural = blank(7, 26);

        vertical(natural, 1, 0, 17);
        vertical(natural, 2, 0, 17);
        vertical(natural, 5, 8, 25);
        vertical(natural, 6, 8, 25);
        horizontal(natural, 1, 6, 8);
        horizontal(natural, 1, 6, 17);

        // width/interline=0.7 and height/interline=2.6 previously entered the unsafe
        // quarter-sharp bounding-box branch.
        assertTrue(SymbolsBuilder.getArabicAccidentalShape(natural, 10) == null);
    }

    @Test
    public void scannedHalfFlatWithAThickStemIsFoundByItsSlash ()
    {
        // alayk-salat-allah-wa-salamuh's key half-flat on a 100 DPI scan: the stem is seven
        // pixels thick and the bowl's right side stands tall enough to look like a second stem,
        // so the one-stem topology tests gave up and the note sounded a quarter tone low.
        final ByteProcessor halfFlat = thickFlat(true);

        assertTrue(SymbolsBuilder.hasSlashRightOfStem(halfFlat));
        assertTrue(SymbolsBuilder.getArabicAccidentalShape(halfFlat, 28, true)
                == org.audiveris.omr.glyph.Shape.QUARTER_FLAT);
    }

    @Test
    public void scannedFlatWithAThickStemStaysAFlat ()
    {
        final ByteProcessor flat = thickFlat(false);

        assertFalse(SymbolsBuilder.hasSlashRightOfStem(flat));
        assertTrue(SymbolsBuilder.getArabicAccidentalShape(flat, 28, true) == null);
    }

    @Test
    public void aFlatMergedWithTheNextStemIsNotSlashed ()
    {
        // A flat packed against the next note's stem can come out as one glyph; the stem,
        // rising at its right, must not read as a slash.
        final ByteProcessor merged = thickFlat(false);

        for (int x = 20; x <= 22; x++) {
            vertical(merged, x, 0, 71);
        }

        assertFalse(SymbolsBuilder.hasSlashRightOfStem(merged));
    }

    @Test
    public void slashedQuarterFlatUsesCorpusWidth ()
    {
        final ByteProcessor quarterFlat = blank(10, 30);

        vertical(quarterFlat, 5, 0, 29);
        horizontal(quarterFlat, 1, 8, 5);
        horizontal(quarterFlat, 3, 9, 17);
        horizontal(quarterFlat, 3, 9, 27);
        vertical(quarterFlat, 3, 17, 27);
        vertical(quarterFlat, 9, 17, 27);

        // width/interline=0.83, matching the E half-flat key glyph in the validated sheet.
        assertTrue(SymbolsBuilder.getArabicAccidentalShape(quarterFlat, 12)
                == org.audiveris.omr.glyph.Shape.QUARTER_FLAT);
    }

    @Test
    public void quarterFlatRequiresAnEnclosedLowerLoop ()
    {
        final ByteProcessor quarterFlat = blank(12, 24);

        // One full-height stem.
        vertical(quarterFlat, 6, 0, 23);
        // Broad upper stroke connected to the stem. A single-pixel stroke is enough for this
        // topology fixture; real anti-aliased glyphs contain a diagonal band here.
        horizontal(quarterFlat, 0, 6, 5);
        // Closed lower loop.
        horizontal(quarterFlat, 4, 10, 13);
        horizontal(quarterFlat, 4, 10, 22);
        vertical(quarterFlat, 4, 13, 22);
        vertical(quarterFlat, 10, 13, 22);

        assertTrue(SymbolsBuilder.hasQuarterFlatTopology(quarterFlat));

        // Opening the loop connects its white center to the exterior.
        quarterFlat.set(4, 18, BACKGROUND);
        quarterFlat.set(10, 18, BACKGROUND);
        assertFalse(SymbolsBuilder.hasQuarterFlatTopology(quarterFlat));
    }

    @Test
    public void crossedQuarterFlatSurvivesDamagedBowl ()
    {
        final ByteProcessor quarterFlat = blank(39, 68);

        // Thick single stem, as measured in the bekram-el-loulou corpus glyphs.
        for (int x = 17; x <= 22; x++) {
            vertical(quarterFlat, x, 0, 67);
        }
        // Descending slash from upper-right through the stem to middle-left.
        thickDiagonal(quarterFlat, 37, 10, 0, 41, 2);
        // A filled lower bowl models the damage caused when a staff line is removed.
        for (int y = 27; y <= 57; y++) {
            horizontal(quarterFlat, 20, Math.min(38, 31 + ((57 - y) / 3)), y);
        }

        assertTrue(SymbolsBuilder.hasCrossedFlatTopology(quarterFlat));
        assertTrue(SymbolsBuilder.getArabicAccidentalShape(quarterFlat, 24)
                == org.audiveris.omr.glyph.Shape.QUARTER_FLAT);
    }

    @Test
    public void ordinarySharpIsNotAQuarterFlat ()
    {
        final ByteProcessor sharp = blank(12, 24);
        vertical(sharp, 3, 0, 23);
        vertical(sharp, 4, 0, 23);
        vertical(sharp, 8, 0, 23);
        vertical(sharp, 9, 0, 23);
        horizontal(sharp, 1, 10, 8);
        horizontal(sharp, 1, 10, 15);

        assertFalse(SymbolsBuilder.hasQuarterFlatTopology(sharp));
        assertFalse(SymbolsBuilder.hasCrossedFlatTopology(sharp));
    }

    @Test
    public void ordinaryFlatWithClosedBowlIsNotAQuarterFlat ()
    {
        final ByteProcessor flat = blank(12, 24);

        vertical(flat, 5, 0, 23);
        horizontal(flat, 5, 10, 13);
        horizontal(flat, 5, 10, 22);
        vertical(flat, 10, 13, 22);

        assertFalse(SymbolsBuilder.hasQuarterFlatTopology(flat));
    }

    @Test
    public void narrowQuarterFlatSurvivesLeftSlashDisconnection ()
    {
        final ByteProcessor quarterFlat = blank(19, 66);

        // Thick stem at the left edge, matching the E half-flat in ah-ya-helw2.
        for (int x = 0; x <= 3; x++) {
            vertical(quarterFlat, x, 0, 65);
        }
        // Only the upper-right portion of the diagonal remains connected to the main glyph.
        thickDiagonal(quarterFlat, 2, 24, 18, 8, 1);
        // Lower bowl.
        horizontal(quarterFlat, 3, 14, 35);
        horizontal(quarterFlat, 3, 10, 58);
        vertical(quarterFlat, 14, 35, 47);
        thickDiagonal(quarterFlat, 14, 47, 10, 58, 1);

        assertTrue(SymbolsBuilder.hasRightSlashedFlatTopology(quarterFlat));
        assertTrue(SymbolsBuilder.getArabicAccidentalShape(quarterFlat, 24)
                == org.audiveris.omr.glyph.Shape.QUARTER_FLAT);
    }

    @Test
    public void fragmentedHeaderSharpRequiresTwoStemClusters ()
    {
        final Rectangle compound = new Rectangle(383, 1168, 26, 96);
        final List<Rectangle> twoStems = List.of(
                new Rectangle(390, 1184, 2, 81),
                new Rectangle(393, 1183, 1, 71),
                new Rectangle(398, 1173, 2, 85),
                new Rectangle(400, 1170, 2, 86));

        assertTrue(SymbolsBuilder.hasFragmentedSharpGeometry(compound, twoStems, 24));
        assertFalse(SymbolsBuilder.hasFragmentedSharpGeometry(
                compound,
                twoStems.subList(0, 2),
                24));
        assertFalse(SymbolsBuilder.hasFragmentedSharpGeometry(
                new Rectangle(383, 1168, 50, 45),
                twoStems,
                24));
    }

    //--------------------------------------------------------------------------------------------//
    // A key's later half-flat, cropped to its pitch (saalouni-elnas, a phone capture of a screen) //
    //--------------------------------------------------------------------------------------------//
    /** Staff 1's E half-flat as the pitch crop leaves it: stem and slash, no bowl. */
    private static final String[] CROPPED_HALF_FLAT = {
            "###............",
            "###............",
            "###............",
            "###............",
            "###.......####.",
            "###......######",
            "###.....#######",
            "###....########",
            "###...#########",
            "###..########..",
            "############...",
            "###########....",
            "##########.....",
            "#########......",
            "########.......",
            "######.........",
            "######.........",
            "####...........",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "###............",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            "####...........",
            ".#.............",
    };

    private static ByteProcessor raster (String[] rows)
    {
        final ByteProcessor buffer = blank(rows[0].length(), rows.length);

        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                if (rows[y].charAt(x) == '#') {
                    buffer.set(x, y, FOREGROUND);
                }
            }
        }

        return buffer;
    }

    /** A staff page with the glyph at (100, 30); with its slash crossing left of the stem. */
    private static ByteProcessor pageWith (ByteProcessor glyph,
                                           boolean crossing)
    {
        final ByteProcessor page = blank(220, 170);

        for (int line = 0; line < 5; line++) {
            horizontal(page, 0, 219, 40 + (line * IL));
            horizontal(page, 0, 219, 41 + (line * IL));
        }

        for (int y = 0; y < glyph.getHeight(); y++) {
            for (int x = 0; x < glyph.getWidth(); x++) {
                if (glyph.get(x, y) != BACKGROUND) {
                    page.set(100 + x, 30 + y, FOREGROUND);
                }
            }
        }

        if (crossing) {
            thickDiagonal(page, 92, 48, 101, 41, 2); // the slash's left end, cut off the crop
        }

        return page;
    }

    @Test
    public void aCroppedHalfFlatIsKnownByItsSlashOnThePage ()
    {
        final ByteProcessor glyph = raster(CROPPED_HALF_FLAT);
        final Rectangle box = new Rectangle(100, 30, glyph.getWidth(), glyph.getHeight());

        assertFalse(SymbolsBuilder.isSlashedFlat(glyph));
        assertTrue(SymbolsBuilder.isHalfFlatOnPage(pageWith(glyph, true), box, glyph, IL));
    }

    @Test
    public void aFlagIsNoHalfFlat ()
    {
        // The same stem and stroke with nothing left of the stem is a note's flag.
        final ByteProcessor glyph = raster(CROPPED_HALF_FLAT);
        final Rectangle box = new Rectangle(100, 30, glyph.getWidth(), glyph.getHeight());

        assertFalse(SymbolsBuilder.isHalfFlatOnPage(pageWith(glyph, false), box, glyph, IL));
        assertFalse(SymbolsBuilder.isHalfFlatOnPage(null, box, glyph, IL));
    }

    private static ByteProcessor blank (int width,
                                        int height)
    {
        final ByteProcessor buffer = new ByteProcessor(width, height);
        buffer.setValue(BACKGROUND);
        buffer.fill();
        return buffer;
    }

    /** A flat as a scan leaves it: thick stem, a filled bowl whose right side stands tall. */
    private static ByteProcessor thickFlat (boolean slashed)
    {
        final ByteProcessor glyph = blank(24, 72);

        for (int x = 0; x <= 6; x++) {
            vertical(glyph, x, 0, 71);
        }

        for (int x = 7; x <= 16; x++) {
            vertical(glyph, x, 44, 71);
        }

        for (int x = 14; x <= 16; x++) {
            vertical(glyph, x, 26, 71);
        }

        for (int x = 9; x <= 12; x++) {
            vertical(glyph, x, 52, 62);
            for (int y = 52; y <= 62; y++) {
                glyph.set(x, y, BACKGROUND);
            }
        }

        if (slashed) {
            thickDiagonal(glyph, 3, 22, 21, 6, 2);
        }

        return glyph;
    }

    private static void horizontal (ByteProcessor buffer,
                                    int x1,
                                    int x2,
                                    int y)
    {
        line(buffer, x1, x2, y);
    }

    private static void line (ByteProcessor buffer,
                              int x1,
                              int x2,
                              int y)
    {
        for (int x = x1; x <= x2; x++) {
            buffer.set(x, y, FOREGROUND);
        }
    }

    private static void thickDiagonal (ByteProcessor buffer,
                                       int x1,
                                       int y1,
                                       int x2,
                                       int y2,
                                       int radius)
    {
        final int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));

        for (int i = 0; i <= steps; i++) {
            final int x = x1 + (((x2 - x1) * i) / steps);
            final int y = y1 + (((y2 - y1) * i) / steps);

            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    final int px = x + dx;
                    final int py = y + dy;
                    if ((px >= 0) && (px < buffer.getWidth())
                            && (py >= 0) && (py < buffer.getHeight())) {
                        buffer.set(px, py, FOREGROUND);
                    }
                }
            }
        }
    }

    private static void vertical (ByteProcessor buffer,
                                  int x,
                                  int y1,
                                  int y2)
    {
        for (int y = y1; y <= y2; y++) {
            buffer.set(x, y, FOREGROUND);
        }
    }
}
