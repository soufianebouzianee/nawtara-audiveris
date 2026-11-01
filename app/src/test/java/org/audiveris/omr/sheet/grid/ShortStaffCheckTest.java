//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h o r t S t a f f C h e c k T e s t                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.grid;

import org.audiveris.omr.sheet.StaffLine;
import ij.process.ByteProcessor;
import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/** Original synthetic staff drawings dedicated to CC0; no corpus pixels. */
public class ShortStaffCheckTest
{
    private static final int IL = 20;
    private static final int LEFT = 40;
    private static final int TOP = 80;
    private static final int RIGHT = 320;

    private static ByteProcessor page ()
    {
        ByteProcessor b = new ByteProcessor(800, 600);
        b.setValue(255);
        b.fill();
        b.setValue(0);
        return b;
    }

    private static List<LineInfo> lines (ByteProcessor b, int count, int spacing, int thickness)
    {
        List<LineInfo> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int y = TOP + i * spacing;
            b.setRoi(LEFT, y, RIGHT - LEFT + 1, thickness);
            b.fill();
            lines.add(new StaffLine(List.of(new Point2D.Double(LEFT, y),
                    new Point2D.Double(RIGHT, y)), thickness));
        }
        b.resetRoi();
        return lines;
    }

    private static void close (ByteProcessor b)
    {
        b.setLineWidth(2);
        b.drawLine(RIGHT, TOP, RIGHT, TOP + 4 * IL);
    }

    private static void note (ByteProcessor b)
    {
        b.fillOval(LEFT + 5 * IL, TOP + 4 * IL + 4, 24, 13);
        b.drawLine(LEFT + 5 * IL + 23, TOP + 4 * IL + 10,
                LEFT + 5 * IL + 23, TOP + IL);
    }

    private static boolean check (ByteProcessor b, List<LineInfo> lines)
    {
        return ShortStaffCheck.hasStaffEvidence(b, IL, 2, lines);
    }

    @Test public void completeFinalBarWithClosingPeakIsSupported ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        note(b);
        assertTrue(check(b, l));
        assertTrue(ShortStaffCheck.isLastIsolated(new Rectangle(LEFT, 400, 280, 82),
                List.of(new Rectangle(LEFT, TOP, 700, 82)), IL));
        assertTrue(ShortStaffCheck.isLastIsolated(new Rectangle(LEFT, TOP, 280, 82),
                List.of(), IL)); // lone staff on a continuation page
    }

    @Test public void underlineAndPartialStaffKeepTheirLengthFilters ()
    {
        for (int count : new int[] {1, 2, 3, 4, 6}) {
            ByteProcessor b = page();
            List<LineInfo> l = lines(b, count, IL, 2);
            close(b);
            note(b);
            assertFalse(check(b, l));
        }
    }

    @Test public void emptyFiveLineBoxAndTableAreNotMusic ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        b.drawLine(LEFT, TOP, LEFT, TOP + 4 * IL);
        assertFalse(check(b, l));
        note(b);
        b.drawLine(LEFT + 7 * IL, TOP, LEFT + 7 * IL, TOP + 4 * IL);
        assertFalse(check(b, l));
    }

    @Test public void frameWithProtrudingSidesIsRejected ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        note(b);
        b.setLineWidth(2);
        b.drawLine(RIGHT, TOP - IL, RIGHT, TOP + 5 * IL);
        assertFalse(check(b, l));
    }

    @Test public void dottedOrnamentAndMissingBarlineAreRejected ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        note(b);
        assertFalse(check(b, l));
        close(b);
        b.setValue(255);
        for (int x = LEFT + 4; x < RIGHT - 4; x += 10) {
            b.setRoi(x, TOP, 7, 4 * IL + 2);
            b.fill();
        }
        b.resetRoi();
        assertFalse(check(b, l));
    }

    @Test public void wrongSpacingAndThicknessAreRejected ()
    {
        for (int[] pair : new int[][] {{15, 2}, {IL, 5}, {IL, 1}}) {
            ByteProcessor b = page();
            List<LineInfo> l = lines(b, 5, pair[0], pair[1]);
            close(b);
            note(b);
            // 1px is permitted at a 2px scale; inconsistent thickness is not.
            if (pair[1] == 1) {
                l.set(2, new StaffLine(List.of(new Point2D.Double(LEFT, TOP + 2 * IL),
                        new Point2D.Double(RIGHT, TOP + 2 * IL)), 5));
            }
            assertFalse(check(b, l));
        }
    }

    @Test public void ossiaCueSplitStaffAndNonfinalInsertAreRejected ()
    {
        Rectangle candidate = new Rectangle(LEFT, 400, 280, 82);
        for (Rectangle other : List.of(new Rectangle(LEFT, 450, 700, 82),
                new Rectangle(400, 400, 350, 82), new Rectangle(LEFT, 300, 700, 82),
                new Rectangle(LEFT + 4 * IL, TOP, 700, 82))) {
            assertFalse(ShortStaffCheck.isLastIsolated(candidate, List.of(other), IL));
        }
    }

    @Test public void fullWidthLastLineNeedsNoException ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        note(b);
        // The helper is gated by the original thresholds at both callers. A normal final
        // staff must not turn a short staff ABOVE it into an accepted ossia.
        assertFalse(ShortStaffCheck.isLastIsolated(new Rectangle(LEFT, TOP, 280, 82),
                List.of(new Rectangle(LEFT, 400, 700, 82)), IL));
    }

    @Test public void cc0EngravedLastBarsKeepTheirMusicInk () throws Exception
    {
        // Crops of our original CC0 last-bar-alone{,-busy} feature pages, raster 300.
        // Include the busy bar: its long stems must not be mistaken for table dividers.
        String[] pngs = {PLAIN_BAR, BUSY_BAR};
        int[] ends = {303, 536};
        for (int k = 0; k < pngs.length; k++) {
            ByteProcessor b = new ByteProcessor(javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(pngs[k]))));
            List<LineInfo> l = new ArrayList<>();
            for (int y : new int[] {53, 74, 94, 115, 136}) {
                l.add(new StaffLine(List.of(new Point2D.Double(22, y),
                        new Point2D.Double(ends[k], y)), 3));
            }
            assertTrue(ShortStaffCheck.hasStaffEvidence(b, 21, 3, l));
        }
    }

    private static final String PLAIN_BAR =
            "iVBORw0KGgoAAAANSUhEUgAAAjUAAAC0CAAAAABJDYIQAAAE5ElEQVR4nO2dWVLDMBAFLRf3v7IoCAleZOGRxliZ1/0DhJAySdfT" +
            "Pk55qpGmafmE9PhS/xsIz4fp2d+6pCmhjTazJWqWj4MwdWsA+qwhX+CUNQtRUkIbOJs19HvBq1+DS9rMZlXo30DNmnSUL0SNOEcT" +
            "dmk/t/f99etxpFHn9Nxwfg6icAZYHAA7zA2DHawBO1gDdrAG7GAN2MEasIM1YAdrwA7WgB2sgRutSeyg0GH2cwZtZPCx5iEM2qjg" +
            "Ys1TF7QRwcMaZFHDwZrl+Zf+VwMJa1amoI0EzNfADdaQLoJ4Zw0SKdBrDZYoQr8G7rQmPw5KET4CdFqTls5wvk4Fp6xBGClmT2me" +
            "B8IhOH3WYIgmLllD+yQGI2+4xxqiRg3frMEfDRysQRU5uqxhCCXKBb1hXApPvzWFOwFBcC7IGvo54WG+BuxgDdxsDd0a7XrDfP5V" +
            "8qQNLRTYwRpwu4/CuQhe34ojScR3uvsCYmWNhjTwBWcv4Z+tYTFBFK8W6veIi9MLgkBdPp+XAb26fKCCa10+GigRqMsHd9floy+s" +
            "gXNdPpCgz5rtMJthtwasXsLN1hA1IlBTAu49RUfUqNBjDdKo0mENQ25ZPvyMwCIZGHmDHayBf9ttvm2NdIZP6e4LeN+s0ZUGmq1B" +
            "GnFarCGk1WmwBmnksVuDNGC2BmnAbA3SwOF8zVkyHikyeyxzE0Bi2KxhbwSYrUEa6F29ZBlBlrk5apBGl+aseUmDPXrMjVGzdoVB" +
            "lBaNWUPASEOV6hbypA07QMEO1sCF+4bL9bCSXHqnuy8g5jlv3tb4nF3zLkXNgR8petyAPWtOKEHcBOeac95oExuzNX+1Tyd+CSLW" +
            "7C3AC2Gumq9BqsjMjQ3UzorMwEkHh6x5+pJX4hA2gWk8o/DrxCpiMrJIYMyafL5ONWETl84WKu/UoXsjgFtFfG5GJ4TbfS/3j9JE" +
            "hYX9NTCGNXRtotNjDXaowm7zFvKkDf0asIM1cP1u82XFms1szW7yJmSOp7sv4N3PXh6+gbyzwbmwhQoZNWCyZlNMbRM2GCJFXyWS" +
            "VVNEuyTDeWuKlRsrphA/cTFkTS79mAreEDrR6aiI/7Ph88ARoiYwFmt2bVQ+WzEAhPcN57RZuzoOFKImMrYWql7L/Mep7muCmLvN" +
            "/zziTdSEpvEUXWno9PjF8lkQFHt1gKo3SKO8K6v+N6/vyoctQydNCv8fXnT28veE5estLFftg6C0ndhdncyt1DyHmHy43oQOZ6St" +
            "aZx0Ya5GA/YNgx2sgf8ZeR+9lEvHZj3jQ82BIRksa37mDvfLo/SYRmIwa3agTXBrHNq6WqKQNuMwetYwBTQiQ1lTjxPCJqo1fLIS" +
            "DJU18CaMbw1rW4GsSfttWb4ThhBtzfu1r5wgkKQlaxYpc7h/uIm6gxj6ztacPundAU1U8N6w5wdcixOiJsQ570shbIKPvD2L2BRu" +
            "+bIuCAhvOYb6Q4veD3dz/sHrZWGI3ebXUbgzGc4MxnjWbM8/oMx4jGgNqiitQzHuUWH81UuIeWdmj+uA2JVILj7ZAhJZw20u9bBb" +
            "Uyw7TF0XKXqzhqRRpKtW1vaIG70aEZrOea+0EamsBt3VAYrzeUgjQ2tNCVYYlemoRMIKoyyO9WtABtahwA7WgB2sATtYA3awBuxg" +
            "DdjBGrCDNWAHa2Ay8wl5uLqQTD9uigAAAABJRU5ErkJggg==";

    private static final String BUSY_BAR =
            "iVBORw0KGgoAAAANSUhEUgAAAjUAAAC0CAAAAABJDYIQAAAGfUlEQVR4nO2d0ZarKgxApav//8vcdabTO7VFaiRAQvZ+mXNqx0Hd" +
            "BoSAKW810ra9fiE9ftR/B5bnLvr2jy5pS2gTm5sk1Lx+DoGpWwPQZg3xBU5Z8yJKSmgDZ2MN7V7QatfgUmxuYlVo30DNmnQUXwg1" +
            "wTnqsEuffXs/P/99jjTROd03nJ8PUTgDDA6AHPqGQQ7WgBysATlYA3KwBuRgDcjBGpCDNSAHa0AO1sBEaxIZFHG46TmDNmHQseYh" +
            "DNpEQcWapy5oEwQNa5AlGgrWvM5/ad8bhLBmZwrahID+GphgDdElINqxBoki0GoNlkSEdg3MtCY/JkoRfALQaE16dYb5dVFQijUI" +
            "E4qbpjTPCeGwOG3WYEhMVGIN9VMwePKGOdYQaqIhW9v8G5mGzizSyJtYwRpCTTh/mqwhsgT1R7eG+oHlt5b3p92awpuAwC5/Y0C2" +
            "nrxp57igZdoj/TUgB2tgsjU0a4Jw8MDD9YdKG5UaCuRgDaj115x7et6/iiO5evJ+r4N9lFr6Du0qX1/KPaBv2JU0afUe7dy1idps" +
            "jcuzHamxnzsc9V0rMcLThUjL6D/Jn7ul4Y1BeBK8D7nxJGity7f5wVNZA6zL5wNPZQ2xLp+HCgppFLhbvhDJmonmCuTXmo/X8erw" +
            "t9ukuNtjw/OcAgW1pk+k2e/VwGUyVyC/7Zr3U5e75Zl1b45kawWyjMnRyzR+z9fHZCKiak3u2XW7zcNcgWZjcE2JGZej6ntkP/rP" +
            "otMJNROuEdXTwFiDNFFpiDWLVE9fIiTVUwGyzaEC2eawdn8NrJlt3itXW68X7nCeVzZVIDWkR/b1+0k91gxP8J/1bExfsp41xmaF" +
            "mOu6Tac/DGSNsadj+pI99NcMv0rmum5zdetKQUUt1iBNtnV+PMSaFL3rNtc3R5BGbM3gk2IgY26/hrKBAvnPAG2djlXcpWDGeEMa" +
            "sMUCpfNf9WRNeZhbebKruZM2pkDp5V/mTkGLNV1yI7pgrmh5pWx20TMU0nRrQ6cv53qZ0UvTd4Mv0uaL2+VjsyyNubLltUYg7s3n" +
            "wd7bfZw5s5k7gZqxptJvYeq4kcZqrDF3ZeyWLH/ZbuqOOwl5w1CBvGFQg7xh6Jg3XF4PSzs/Vj37dfD3twt/QK09ZD1vuLZnj607" +
            "g+R+7wQb9wxVCjUHBTc+huKDXN88+daUx5qmdAFQYfYJvnUp+uyjck+ubp1+esXWfKufTmyEL2Tj0py1pvCOE/2ygIc2Tc/+GgOH" +
            "tmag2ZLDcah8VPa/7FpoIEdZpfrpyz7zfP0X50zJZh+SX9/LmlQuKwHnEu5usFvj8R2vU02Fta56N42iZs/3zSJklyvie3oZHcy0" +
            "Jsses6AfY+9V8mtW0CMPDvA9rKGOGszwE95iDXbM4PMxdvx1INscKpBtDmrQGob+2ebPFWvevpRKnTcX6tuI2eZ5G4I0O71Dtnmu" +
            "7peeGnPoZqd3rKF4xLKD8n182prXqukz2GCIabSD/+VY81lHUS9ZRf3KnLfmLdh8LQ/hxwr6t7Mg1uTSf0utLIKOKZKpFfF/e7IP" +
            "CkWoMUKPe1hizUcdlc+uGGAJ48VzgShv+HcJvr+xq+OAYjjUxHryS4db8qhs8w9tdjw+dnQvx/Jn3hyFujY/mx7f23wRMH81j5xF" +
            "96vN0R/1KU3A8JOHzod6Tnkqe+NdmoI/TAdUmUX3XJZ6P1z771OVF9UErCwmkIfPvfybYfl/wkR51b420EeD4tLzecqM3d3M3O7v" +
            "agvT1hhFnrU6QPkldL2vKv4oBBuFi0S2OVQg2xzUINsc5Cj2RrxlnF/eh5Duf9JWOvsqa2U5hxV35GCNdFFcULXG3qsM54SftN/F" +
            "ihiLNcXk5IlcvvipfReGMWbNgm2NtKA/ytaoDxCv1NZIy/hjM9YsHn427wOzbqxZ05+0xbKm0D01/CHK6Z3qn2vWPNP5uG4xuWLN" +
            "LreG+z0gF0YvT8/0hlVpH/NGm3jIaygsqfGWRL0od+WePeaBbEv1TCpZ8+VM0DhetGfJey+fR/Ja/mDNYPIK/mDNNPLmFs1sc8c3" +
            "D4hgjgJMscZxpIVB1tTXO8KgELTHGl5zGQ+5NcVlh0Nk5oNWrCHSROTKuNFn1qvKIlnghkujjTtt1sm8h7NcG6Mu9uchTRiuZjZ8" +
            "eIMzgWjIh+mxgiO4gCwqkMM4FMjBGpCDNSAHa0AO1oAcrAE5WANysAbkYA1sYv4DCbpK8pH6IKIAAAAASUVORK5CYII=";

    @Test public void letteringInRuledBoxIsNotHeadAndStemEvidence ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        b.drawLine(LEFT, TOP, LEFT, TOP + 4 * IL);
        b.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14));
        b.drawString("Title / table text", LEFT + 2 * IL, TOP + 2 * IL - 3);
        assertFalse(check(b, l));
    }

    @Test public void stemlessWholeNoteWithoutRepeatedHeaderIsSupported ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        b.fillOval(LEFT + 5 * IL, TOP + 5 * IL - 7, 28, 15);
        b.setValue(255);
        b.fillOval(LEFT + 5 * IL + 6, TOP + 5 * IL - 4, 15, 8);
        assertTrue(check(b, l));
    }

    @Test public void fittedLineInterruptedByClefNeedsActualInkAtBothEnds ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        note(b);
        l.set(3, new StaffLine(List.of(new Point2D.Double(LEFT + 3 * IL, TOP + 3 * IL),
                new Point2D.Double(RIGHT, TOP + 3 * IL)), 2));
        assertTrue(check(b, l));
        b.setValue(255);
        b.setRoi(LEFT, TOP + 3 * IL - 2, 2 * IL, 6);
        b.fill();
        assertFalse(check(b, l));
    }

    @Test public void beamedStemAcrossAllFourSpacesIsNotATableDivider ()
    {
        ByteProcessor b = page();
        List<LineInfo> l = lines(b, 5, IL, 2);
        close(b);
        b.setLineWidth(2);
        int x = LEFT + 7 * IL;
        b.drawLine(x, TOP, x, TOP + 4 * IL);
        b.fillOval(x - 22, TOP + 4 * IL - 6, 24, 13);
        b.setRoi(x, TOP - 2, 2 * IL, 7);
        b.fill();
        b.resetRoi();
        assertTrue(check(b, l));
    }
}
