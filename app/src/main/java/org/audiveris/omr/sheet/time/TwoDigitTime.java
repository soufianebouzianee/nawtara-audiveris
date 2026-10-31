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
