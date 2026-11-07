//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T w o D i g i t T i m e                                     //
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
package org.audiveris.omr.sheet.time;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.inter.AbstractNumberInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.util.VerticalSide;

import ij.process.ByteProcessor;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Local digit composition for time numbers 10 through 19; no global classifier changes. */
final class TwoDigitTime
{
    private TwoDigitTime ()
    {
    }

    static List<TimeNumberInter> find (List<Glyph> parts, Staff staff)
    {
        final List<TimeNumberInter> result = find(parts, staff.getSpecificInterline());
        result.removeIf(number -> !TimeNumberInter.isPitchValid(
                staff.pitchPositionOf(number.getGlyph().getCentroid())));
        for (TimeNumberInter number : result) {
            number.setStaff(staff);
            number.setSide(staff.pitchPositionOf(number.getGlyph().getCentroid()) < 0
                    ? VerticalSide.TOP : VerticalSide.BOTTOM);
        }
        return result;
    }

    static List<TimeNumberInter> find (List<Glyph> parts, int il)
    {
        final List<TimeNumberInter> result = new ArrayList<>();
        for (Glyph one : parts) {
            final Rectangle a = one.getBounds();
            // A printed 1 has a flag and a foot. A narrow stem or clipped barline is not a digit.
            if (a.width < 0.55 * il || a.width > 1.4 * il
                    || a.height < 1.5 * il || a.height > 2.2 * il) {
                continue;
            }
            final Evaluation first = vote(one, il);
            if (first == null || first.shape != Shape.TIME_ONE) {
                continue;
            }
            final List<Glyph> rights = new ArrayList<>();
            for (Glyph part : parts) {
                final Rectangle b = part.getBounds();
                if (b.x >= a.x + a.width && b.x <= a.x + a.width + 1.9 * il
                        && Math.abs(b.y - a.y) <= 0.2 * il
                        && Math.abs(b.y + b.height - a.y - a.height) <= 0.2 * il) {
                    rights.add(part);
                }
            }
            // Staff removal can split a zero into its two vertical walls. Try that compound too.
            final List<Glyph> candidates = new ArrayList<>(rights);
            for (int i = 0; i < rights.size(); i++) {
                for (int j = i + 1; j < rights.size(); j++) {
                    candidates.add(GlyphFactory.buildGlyph(List.of(rights.get(i), rights.get(j))));
                }
            }
            for (Glyph digit : candidates) {
                final Rectangle b = digit.getBounds();
                final int gap = b.x - a.x - a.width;
                if (gap < 0 || gap > 0.35 * il || b.width < 0.55 * il
                        || b.width > 1.8 * il || a.union(b).width > 3.3 * il) {
                    continue;
                }
                final Evaluation second = vote(digit, il);
                final Integer value;
                if (isZero(digit.getBuffer())) {
                    value = 0;
                } else if (second != null) {
                    final Integer classified = AbstractNumberInter.valueOf(second.shape);
                    value = (classified != null && (classified == 6 || classified == 9))
                            ? loopDigit(digit.getBuffer()) : classified;
                } else {
                    continue;
                }
                if (value == null || value < 0 || value > 9) {
                    continue;
                }
                // A zero wall can rank as THREE. A printed 3 must face right, not be
                // the left parenthesis-shaped wall of an incompletely assembled zero.
                if (value == 3 && !hasRightFacingThreeTopology(digit.getBuffer())) {
                    continue;
                }
                // Never interpret a zero based solely on a low-ranked classifier vote.
                if (value == 0 && !isZero(digit.getBuffer())) {
                    continue;
                }
                final Glyph compound = GlyphFactory.buildGlyph(List.of(one, digit));
                final double grade = Grades.intrinsicRatio * Math.min(first.grade,
                        (second != null) ? second.grade : first.grade);
                final TimeNumberInter number = new TimeNumberInter(compound, Shape.NUMBER_CUSTOM,
                        grade, VerticalSide.TOP);
                number.setValue(10 + value);
                if (result.stream().noneMatch(n -> n.getBounds().equals(number.getBounds()))) {
                    result.add(number);
                }
            }
        }
        return result;
    }

    private static Evaluation vote (Glyph glyph, int interline)
    {
        final Evaluation[] evals = ShapeClassifier.getInstance().evaluate(glyph, interline, 1,
                Grades.timeMinGrade / Grades.intrinsicRatio, null);
        return evals.length == 0 ? null : evals[0];
    }

    /** A 3 carries its continuous curved spine on the right, unlike a zero's left wall. */
    static boolean hasRightFacingThreeTopology (ByteProcessor ink)
    {
        final int w = ink.getWidth();
        final int h = ink.getHeight();
        int count = 0;
        int sumX = 0;
        for (int y = h / 5; y < 4 * h / 5; y++) {
            for (int x = 0; x < w; x++) {
                if (ink.get(x, y) == 0) {
                    count++;
                    sumX += x;
                }
            }
        }
        return count > 0 && (double) sumX / count >= 0.5 * w;
    }

    /** Resolve the 5/6/9 confusion locally, after a separately recognized printed 1. */
    private static Integer loopDigit (ByteProcessor ink)
    {
        final int w = ink.getWidth();
        final int h = ink.getHeight();
        int top = 0;
        for (int y = 0; y < Math.max(2, h / 10); y++) {
            int count = 0;
            for (int x = 0; x < w; x++) {
                if (ink.get(x, y) == 0) {
                    count++;
                }
            }
            top = Math.max(top, count);
        }
        // The flat cap of 5 spans the width; the curved caps of 6 and 9 do not.
        if (top >= 0.75 * w) {
            return 5;
        }
        int count = 0;
        int sumX = 0;
        for (int y = h / 5; y < 4 * h / 5; y++) {
            for (int x = 0; x < w; x++) {
                if (ink.get(x, y) == 0) {
                    count++;
                    sumX += x;
                }
            }
        }
        // The continuous stem is on the left of 6 and on the right of 9.
        final double center = (double) sumX / count / w;
        if (center < 0.47) {
            return 6;
        }
        if (center > 0.49) {
            return 9;
        }
        return null;
    }

    /**
     * A zero has two walls around a continuous central opening. Staff removal may open its
     * top and bottom; a six lacks the upper right wall and an eight closes the middle.
     */
    /**
     * Tell a printed 6 from a printed 9 on a glyph's ink: the closed loop of a 6 sits in the
     * lower half of the digit, the loop of a 9 in the upper half (largest enclosed hole).
     *
     * @param ink glyph buffer, foreground 0
     * @return 6, 9, or null when the ink does not say
     */
    static Integer holeSixOrNine (ByteProcessor ink)
    {
        final int w = ink.getWidth();
        final int h = ink.getHeight();
        if (w < 5 || h < 8) {
            return null;
        }
        // Background connected to the border, on a one-pixel padded grid.
        final int pw = w + 2;
        final int ph = h + 2;
        final boolean[] outside = new boolean[pw * ph];
        final java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        queue.add(new int[]{0, 0});
        outside[0] = true;
        final int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            final int[] p = queue.poll();
            for (int[] d : steps) {
                final int x = p[0] + d[0];
                final int y = p[1] + d[1];
                if (x < 0 || y < 0 || x >= pw || y >= ph || outside[y * pw + x]) {
                    continue;
                }
                final boolean inkPixel = x >= 1 && y >= 1 && x <= w && y <= h
                        && ink.get(x - 1, y - 1) == 0;
                if (!inkPixel) {
                    outside[y * pw + x] = true;
                    queue.add(new int[]{x, y});
                }
            }
        }
        // Enclosed background components: keep the largest one.
        final int[] label = new int[pw * ph];
        int bestArea = 0;
        double bestY = 0;
        int next = 0;
        for (int y0 = 1; y0 <= h; y0++) {
            for (int x0 = 1; x0 <= w; x0++) {
                if (outside[y0 * pw + x0] || ink.get(x0 - 1, y0 - 1) == 0 || label[y0 * pw + x0] != 0) {
                    continue;
                }
                next++;
                int area = 0;
                double sy = 0;
                queue.clear();
                queue.add(new int[]{x0, y0});
                label[y0 * pw + x0] = next;
                while (!queue.isEmpty()) {
                    final int[] p = queue.poll();
                    area++;
                    sy += p[1] - 1;
                    for (int[] d : steps) {
                        final int x = p[0] + d[0];
                        final int y = p[1] + d[1];
                        if (x < 1 || y < 1 || x > w || y > h || outside[y * pw + x]
                                || label[y * pw + x] != 0 || ink.get(x - 1, y - 1) == 0) {
                            continue;
                        }
                        label[y * pw + x] = next;
                        queue.add(new int[]{x, y});
                    }
                }
                if (area > bestArea) {
                    bestArea = area;
                    bestY = sy / area;
                }
            }
        }
        if (bestArea >= 0.04 * w * h) {
            final double c = (bestY + 0.5) / h;
            // Measured on the 6 and 9 strips of all five paths: 0.71-0.72 and 0.26-0.27.
            if (c < 0.45) {
                return 9;
            }
            if (c > 0.55) {
                return 6;
            }
        }
        return null;
    }

    /**
     * Tell 6 from 9 by which outer edge runs straight: the stem of a 9 keeps the right edge
     * nearly vertical while the left edge swings from bowl to tail, and a 6 is the mirror image.
     * Rows crossed by a staff line (inked from side to side) are skipped, so the cue does not
     * depend on the loop being closed.
     *
     * @param ink glyph buffer, foreground 0
     * @return 6, 9, or null when neither edge is clearly straighter
     */
    static Integer straightEdge (ByteProcessor ink)
    {
        final int w = ink.getWidth();
        final int h = ink.getHeight();
        if (w < 8 || h < 12) {
            return null;
        }
        final int y0 = (int) (0.2 * h);
        final int y1 = (int) (0.85 * h);
        int minL = Integer.MAX_VALUE;
        int maxL = -1;
        int minR = Integer.MAX_VALUE;
        int maxR = -1;
        int rows = 0;
        for (int y = y0; y < y1; y++) {
            int left = -1;
            int right = -1;
            for (int x = 0; x < w; x++) {
                if (ink.get(x, y) == 0) {
                    if (left < 0) {
                        left = x;
                    }
                    right = x;
                }
            }
            if (left < 0 || (left == 0 && right == w - 1)) {
                continue; // empty row, or a staff line through the glyph
            }
            rows++;
            minL = Math.min(minL, left);
            maxL = Math.max(maxL, left);
            minR = Math.min(minR, right);
            maxR = Math.max(maxR, right);
        }
        if (rows < 0.4 * (y1 - y0)) {
            return null;
        }
        final int spreadL = maxL - minL;
        final int spreadR = maxR - minR;
        final int margin = Math.max(2, (int) Math.rint(0.1 * w));
        if (spreadR + margin <= spreadL) {
            return 9;
        }
        if (spreadL + margin <= spreadR) {
            return 6;
        }
        return null;
    }

    /**
     * Tell a printed 6 from a printed 9 on a glyph's ink: the straight outer edge first, then
     * the largest enclosed hole when the edges do not say.
     *
     * @param ink glyph buffer, foreground 0
     * @return 6, 9, or null when the ink does not say
     */
    static Integer sixOrNine (ByteProcessor ink)
    {
        final Integer edge = straightEdge(ink);
        return (edge != null) ? edge : holeSixOrNine(ink);
    }

    /**
     * The top number of a whole time glyph (a numerator stacked on a denominator),
     * cut at the emptiest row near the middle, judged 6 or 9 by its ink.
     *
     * @param ink whole time glyph buffer, foreground 0
     * @return 6, 9 or null
     */
    static Integer topSixOrNine (ByteProcessor ink)
    {
        final int w = ink.getWidth();
        final int h = ink.getHeight();
        int cut = h / 2;
        int least = Integer.MAX_VALUE;
        for (int y = (int) (0.4 * h); y <= (int) (0.6 * h); y++) {
            int count = 0;
            for (int x = 0; x < w; x++) {
                if (ink.get(x, y) == 0) {
                    count++;
                }
            }
            if (count < least) {
                least = count;
                cut = y;
            }
        }
        if (cut < 8) {
            return null;
        }
        final ByteProcessor top = new ByteProcessor(w, cut);
        top.setValue(255);
        top.fill();
        for (int y = 0; y < cut; y++) {
            for (int x = 0; x < w; x++) {
                top.set(x, y, ink.get(x, y));
            }
        }
        return sixOrNine(top);
    }

    static boolean isZero (ByteProcessor ink)
    {
        final int w = ink.getWidth();
        final int h = ink.getHeight();
        int walls = 0;
        int rows = 0;
        for (int y = h / 5; y < 4 * h / 5; y++) {
            boolean left = false;
            boolean right = false;
            for (int x = 0; x < w / 2; x++) {
                left |= ink.get(x, y) == 0;
                right |= ink.get(w - 1 - x, y) == 0;
            }
            for (int x = w / 2 - Math.max(1, w / 20); x <= w / 2 + Math.max(1, w / 20); x++) {
                if (ink.get(x, y) == 0) {
                    return false;
                }
            }
            rows++;
            if (left && right) {
                walls++;
            }
        }
        return rows > 0 && walls >= 0.85 * rows;
    }
}
