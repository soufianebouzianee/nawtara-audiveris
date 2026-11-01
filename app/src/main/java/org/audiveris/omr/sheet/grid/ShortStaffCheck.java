//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h o r t S t a f f C h e c k                                   //
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

import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.ProcessingSwitch;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.util.HorizontalSide;

import ij.process.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>ShortStaffCheck</code> supplies positive evidence before either length filter
 * makes an exception for an isolated final five-line staff. It does not create new clusters.
 */
final class ShortStaffCheck
{
    private static final Logger logger = LoggerFactory.getLogger(ShortStaffCheck.class);

    private ShortStaffCheck ()
    {
    }

    static boolean isSupported (Sheet sheet,
                                LineCluster cluster,
                                Collection<LineCluster> clusters)
    {
        final Scale scale = sheet.getScale();
        if (cluster.getSize() != 5 || cluster.getInterline() != scale.getInterline()
                || sheet.getStub().getProcessingSwitches().getValue(
                        ProcessingSwitch.fourStringTablatures)
                || sheet.getStub().getProcessingSwitches().getValue(
                        ProcessingSwitch.sixStringTablatures)) {
            return false;
        }

        final List<Rectangle> others = new ArrayList<>();
        for (LineCluster other : clusters) {
            if (other != cluster) {
                others.add(other.getBounds());
            }
        }
        final boolean supported = isLastIsolated(cluster.getBounds(), others, scale.getInterline())
                && hasStaffEvidence(sheet.getPicture().getSource(Picture.SourceKey.BINARY),
                        scale.getInterline(), scale.getFore(), new ArrayList<>(cluster.getLines()));
        if (supported) {
            logger.info("Retaining short final staff {} at {}", cluster, cluster.getBounds());
        }
        return supported;
    }

    /** Decline ossias, cues, fragments alongside held staves, and indented inserts. */
    static boolean isLastIsolated (Rectangle box,
                                   Collection<Rectangle> others,
                                   int interline)
    {
        Rectangle previous = null;
        for (Rectangle other : others) {
            if (other.getMaxY() + 6 * interline > box.y) {
                return false;
            }
            if (previous == null || other.y > previous.y) {
                previous = other;
            }
        }
        return previous == null || Math.abs(box.x - previous.x) <= 2 * interline;
    }

    /**
     * Verify the original binary ink, not merely five fitted splines. The sheet interline and
     * foreground thickness must agree with every line. A closing bar and note-sized ink are
     * required as well: an empty ruled box is not music.
     */
    static boolean hasStaffEvidence (ByteProcessor binary,
                                     int interline,
                                     Integer fore,
                                     List<? extends LineInfo> lines)
    {
        if (binary == null || interline < 4 || fore == null || fore <= 0 || lines.size() != 5) {
            return false;
        }
        final List<Double> lefts = new ArrayList<>();
        final List<Double> rights = new ArrayList<>();
        for (LineInfo line : lines) {
            lefts.add(line.getEndPoint(HorizontalSide.LEFT).getX());
            rights.add(line.getEndPoint(HorizontalSide.RIGHT).getX());
            if (line.getThickness() < 0.5 * fore || line.getThickness() > 1.8 * fore) {
                return false;
            }
        }
        // As in buildStaves: a clef can interrupt one fitted filament's left end.
        // Test the actual rule ink over the median staff range, including that interruption.
        Collections.sort(lefts);
        Collections.sort(rights);
        final int x1 = (int) Math.ceil(lefts.get(2));
        final int x2 = (int) Math.floor(rights.get(2));
        if (x2 - x1 < 8 * interline || x1 < 0 || x2 >= binary.getWidth()) {
            return false;
        }
        final int radius = Math.max(1, (int) Math.ceil(fore / 2.0));
        for (int i = 0; i < 5; i++) {
            final LineInfo line = lines.get(i);
            int hits = 0;
            int leftHits = 0;
            int rightHits = 0;
            for (int x = x1; x <= x2; x++) {
                double y = line.yAt((double) x);
                if (y - radius < 0 || y + radius >= binary.getHeight()) {
                    return false;
                }
                if (i > 0 && Math.abs(y - lines.get(i - 1).yAt((double) x) - interline)
                        > 0.15 * interline) {
                    return false;
                }
                for (int dy = -radius; dy <= radius; dy++) {
                    if (ink(binary, x, (int) Math.rint(y) + dy)) {
                        hits++;
                        if (x < x1 + interline) leftHits++;
                        if (x > x2 - interline) rightHits++;
                        break;
                    }
                }
            }
            if (hits < 0.85 * (x2 - x1 + 1)
                    || leftHits < 0.7 * interline || rightHits < 0.7 * interline) {
                return false;
            }
        }

        // A true closing peak spans all four spaces but does not continue as a page frame.
        final double slope = (lines.get(0).yAt((double) x2)
                - lines.get(0).yAt((double) x1)) / (x2 - x1);
        boolean closing = false;
        int peakWidth = 0;
        for (int x = x1; x <= Math.min(binary.getWidth() - 1, x2 + interline / 2); x++) {
            final int top = (int) Math.rint(lines.get(0).yAt((double) x));
            final int bottom = (int) Math.rint(lines.get(4).yAt((double) x));
            int hits = 0;
            for (int y = top; y <= bottom; y++) {
                if (ink(binary, x - (int) Math.rint(slope * (y - top)), y)) {
                    hits++;
                }
            }
            if (hits >= 0.95 * (bottom - top + 1)) {
                if (++peakWidth > 0.65 * interline) {
                    return false;
                }
                if (x > x1 + 2 * interline && x < x2 - interline
                        && !hasAttachment(binary, lines, x, top, bottom, interline, radius)) {
                    return false; // Bare interior table divider, not an attached note stem.
                }
                if (x >= x2 - interline
                        && !ink(binary, x, top - interline / 2)
                        && !ink(binary, x, bottom + interline / 2)) {
                    closing = true;
                }
            } else {
                peakWidth = 0;
            }
        }
        return closing && hasMusicInk(binary, lines, x1, x2, interline, radius);
    }

    /** A beamed stem may reach both outer rules; its head/beam distinguishes it from a divider. */
    private static boolean hasAttachment (ByteProcessor binary,
                                          List<? extends LineInfo> lines,
                                          int x,
                                          int top,
                                          int bottom,
                                          int interline,
                                          int radius)
    {
        final int width = Math.max(2, (int) Math.ceil(0.45 * interline));
        final int height = Math.max(2, (int) Math.ceil(0.18 * interline));
        for (int side : new int[] {-1, 1}) {
            int rows = 0;
            for (int y = top - interline; y <= bottom + interline; y++) {
                boolean rule = false;
                for (LineInfo line : lines) {
                    if (Math.abs(y - line.yAt((double) x)) <= radius + 1) {
                        rule = true;
                        break;
                    }
                }
                int hits = 0;
                if (!rule) {
                    for (int dx = 1; dx <= width; dx++) {
                        if (ink(binary, x + side * dx, y)) hits++;
                    }
                }
                rows = hits >= 0.8 * width ? rows + 1 : 0;
                if (rows >= height) return true;
            }
        }
        return false;
    }

    /** Require a compact head-sized patch off the rules, rather than another line or stem. */
    private static boolean hasMusicInk (ByteProcessor binary,
                                        List<? extends LineInfo> lines,
                                        int left,
                                        int right,
                                        int interline,
                                        int radius)
    {
        final int minRun = Math.max(2, (int) Math.ceil(0.45 * interline));
        final int minRows = Math.max(2, (int) Math.ceil(0.18 * interline));
        for (int x = left + interline; x < right - interline; x++) {
            final int top = (int) Math.rint(lines.get(0).yAt((double) x));
            int rows = 0;
            for (int y = Math.max(0, top - interline); y <= Math.min(
                    binary.getHeight() - 1, top + 6 * interline); y++) {
                boolean rule = false;
                for (LineInfo line : lines) {
                    if (Math.abs(y - line.yAt((double) x)) <= radius + 1) {
                        rule = true;
                        break;
                    }
                }
                int run = 0;
                if (!rule) {
                    for (int dx = 0; dx < minRun && x + dx < right - interline / 2; dx++) {
                        if (ink(binary, x + dx, y)) {
                            run++;
                        }
                    }
                }
                rows = run == minRun ? rows + 1 : 0;
                if (rows >= minRows && hasStemBeside(binary, x, y, left, right, interline)) {
                    return true;
                }
            }
        }
        return hasWholeHead(binary, lines, left, right, interline, radius);
    }

    /** A filled head/beam needs a nearby sustained stem, beyond the height of table lettering. */
    private static boolean hasStemBeside (ByteProcessor binary,
                                          int x,
                                          int y,
                                          int left,
                                          int right,
                                          int interline)
    {
        final int height = 2 * interline;
        for (int sx = Math.max(left + interline, x - interline / 2);
                sx <= Math.min(right - interline, x + 3 * interline / 2); sx++) {
            for (int direction : new int[] {-1, 1}) {
                int hits = 0;
                for (int dy = 0; dy < height; dy++) {
                    if (ink(binary, sx, y + direction * dy)) {
                        hits++;
                    }
                }
                if (hits >= 0.95 * height) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A stemless whole note must have a wide oval ring, a clear centre and clear surround. */
    private static boolean hasWholeHead (ByteProcessor binary,
                                         List<? extends LineInfo> lines,
                                         int left,
                                         int right,
                                         int interline,
                                         int radius)
    {
        final double rx = 0.68 * interline;
        final double ry = 0.36 * interline;
        for (int x = left + 2 * interline; x < right - interline; x++) {
            for (int pitch = -2; pitch <= 12; pitch++) {
                final int cy = (int) Math.rint(lines.get(0).yAt((double) x)
                        + pitch * interline / 2.0);
                int ring = 0, ringInk = 0, centre = 0, centreWhite = 0, outer = 0, outerWhite = 0;
                for (int dy = -(int) Math.ceil(1.4 * ry); dy <= 1.4 * ry; dy++) {
                    final int y = cy + dy;
                    boolean rule = false;
                    for (LineInfo line : lines) {
                        if (Math.abs(y - line.yAt((double) x)) <= radius + 1) {
                            rule = true;
                            break;
                        }
                    }
                    if (rule || y < 0 || y >= binary.getHeight()) {
                        continue;
                    }
                    for (int dx = -(int) Math.ceil(1.3 * rx); dx <= 1.3 * rx; dx++) {
                        final double q = dx * dx / (rx * rx) + dy * dy / (ry * ry);
                        final boolean black = ink(binary, x + dx, y);
                        if (q >= 0.4 && q <= 1.1) {
                            ring++;
                            if (black) ringInk++;
                        } else if (q < 0.25) {
                            centre++;
                            if (!black) centreWhite++;
                        } else if (q >= 1.5) {
                            outer++;
                            if (!black) outerWhite++;
                        }
                    }
                }
                if (ring > 0 && centre > 0 && outer > 0 && ringInk >= 0.55 * ring
                        && centreWhite >= 0.65 * centre && outerWhite >= 0.8 * outer) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean ink (ByteProcessor binary,
                                int x,
                                int y)
    {
        return x >= 0 && x < binary.getWidth() && y >= 0 && y < binary.getHeight()
                && binary.get(x, y) == 0;
    }
}
