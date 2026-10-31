//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a r l i n e T i m e                                       //
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
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.VerticalSide;

import ij.process.Blitter;
import ij.process.ByteProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Conservative recovery of printed two-digit times lost to heads in symbol extraction. */
public final class BarlineTime
{
    private BarlineTime ()
    {
    }

    /** Detached evidence: finding a pair never mutates the SIG. */
    static record Pair (TimeNumberInter top, TimeNumberInter bottom)
    {
    }

    /**
     * Try only the small area immediately after an interior barline. All standard staves must
     * independently contain the same, uniquely aligned pair before anything is replaced.
     */
    public static List<Inter> recover (MeasureStack stack)
    {
        final SystemInfo system = stack.getSystem();
        final List<Pair> column = new ArrayList<>();
        Double offset = null;
        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                continue;
            }
            final var measure = stack.getMeasureAt(staff);
            if (measure == null || measure.getPartBarlineOn(HorizontalSide.LEFT) == null) {
                return List.of();
            }
            final int il = staff.getSpecificInterline();
            final int left = measure.getAbscissa(HorizontalSide.LEFT, staff);
            if (left <= staff.getHeaderStop()) {
                return List.of();
            }
            final int x = left + Math.max(1, il / 4);
            final int y = staff.getFirstLine().yAt(x);
            final Rectangle box = new Rectangle(x, y, 5 * il, 4 * il + 1);
            final var sheet = system.getSheet();
            if (!sheet.clamp(box).equals(box)) {
                return List.of();
            }
            final ByteProcessor ink = new ByteProcessor(box.width, box.height);
            ink.copyBits(sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF),
                    -box.x, -box.y, Blitter.COPY);
            final List<Pair> pairs = find(ink, box.getLocation(), il);
            if (pairs.size() != 1) {
                return List.of();
            }
            final Pair pair = pairs.get(0);
            final Rectangle top = pair.top.getBounds();
            final int right = measure.getAbscissa(HorizontalSide.RIGHT, staff);
            if (top.x + top.width >= right
                    || pair.bottom.getBounds().getMaxX() >= right
                    || top.x - left > 2.2 * il
                    || Math.abs(staff.pitchPositionOf(pair.top.getCenter()) + 2) > 0.4
                    || Math.abs(staff.pitchPositionOf(pair.bottom.getCenter()) - 2) > 0.4) {
                return List.of();
            }
            final double dx = stack.getXOffset(pair.top.getCenter(), staff) / il;
            if (!column.isEmpty() && (!pair.top.getValue().equals(column.get(0).top.getValue())
                    || !pair.bottom.getValue().equals(column.get(0).bottom.getValue())
                    || Math.abs(dx - offset) > 0.5)) {
                return List.of();
            }
            offset = dx;
            pair.top.setStaff(staff);
            pair.bottom.setStaff(staff);
            column.add(pair);
        }
        if (column.isEmpty()) {
            return List.of();
        }
        final List<Inter> result = new ArrayList<>();
        for (Pair pair : column) {
            final Rectangle box = pair.top.getBounds().union(pair.bottom.getBounds());
            box.grow(0, Math.max(1, (int) Math.rint(0.1 * pair.top.getStaff().getSpecificInterline())) + 1);
            // Only hypotheses made from the recovered digits are displaced. The all-staff ink
            // check above has completed; no partially accepted column can erase music.
            for (Inter inter : new ArrayList<>(system.getSig().vertexSet())) {
                if ((inter instanceof HeadInter || inter instanceof StemInter
                        || inter instanceof TupletInter)
                        && box.contains(inter.getCenter())) {
                    inter.remove();
                }
            }
            system.getSig().addVertex(pair.top);
            system.getSig().addVertex(pair.bottom);
            result.add(pair.top);
            result.add(pair.bottom);
        }
        return result;
    }

    /** Read a complete staff-height crop; both halves retain their own original glyph bounds. */
    static List<Pair> find (ByteProcessor ink, Point origin, int il)
    {
        final int margin = Math.max(1, (int) Math.rint(0.1 * il));
        final List<Glyph> tops = parts(ink, origin, margin, 2 * il - 2 * margin);
        final List<Glyph> bottoms = parts(ink, origin, 2 * il + margin, 2 * il - 2 * margin);
        final List<Pair> result = new ArrayList<>();
        for (TimeNumberInter top : TwoDigitTime.find(tops, il)) {
            for (Glyph glyph : bottoms) {
                final Rectangle b = glyph.getBounds();
                if (b.width < 0.7 * il || b.width > 1.8 * il || b.height < 1.6 * il
                        || b.height > 2 * il
                        || Math.abs(b.getCenterX() - top.getBounds().getCenterX()) > 0.35 * il) {
                    continue;
                }
                final Evaluation[] votes = ShapeClassifier.getInstance().evaluate(glyph, il, 1,
                        Grades.timeMinGrade / Grades.intrinsicRatio, null);
                if (votes.length == 0 || (votes[0].shape != Shape.TIME_EIGHT
                        && votes[0].shape != Shape.TIME_FOUR && votes[0].shape != Shape.TIME_TWO)) {
                    continue;
                }
                final TimeNumberInter bottom = new TimeNumberInter(glyph, votes[0].shape,
                        Grades.intrinsicRatio * votes[0].grade, VerticalSide.BOTTOM);
                // A one-pixel staff-removal speck can produce two versions of the same digit.
                // Collapse only identical values at the same position; differing readings abstain.
                final Pair existing = result.stream().filter(p ->
                        p.top.getBounds().equals(top.getBounds())
                        && p.bottom.getValue().equals(bottom.getValue())
                        && p.bottom.getCenter().distance(bottom.getCenter()) <= 0.2 * il)
                        .findFirst().orElse(null);
                if (existing == null) {
                    result.add(new Pair(top, bottom));
                } else if (bottom.getGrade() > existing.bottom.getGrade()) {
                    result.set(result.indexOf(existing), new Pair(top, bottom));
                }
            }
        }
        return result;
    }

    /** Rejoin fragments that occupy the same digit column after staff removal. */
    private static List<Glyph> parts (ByteProcessor ink, Point origin, int y, int height)
    {
        ink.setRoi(new Rectangle(0, y, ink.getWidth(), height));
        final ByteProcessor half = (ByteProcessor) ink.crop();
        ink.resetRoi();
        final List<Glyph> glyphs = GlyphFactory.buildGlyphs(
                new RunTableFactory(Orientation.VERTICAL).createTable(half),
                new Point(origin.x, origin.y + y));
        final List<List<Glyph>> columns = new ArrayList<>();
        glyphs.sort((a, b) -> Integer.compare(a.getLeft(), b.getLeft()));
        int right = -1;
        for (Glyph glyph : glyphs) {
            if (columns.isEmpty() || glyph.getLeft() > right) {
                columns.add(new ArrayList<>());
            }
            columns.get(columns.size() - 1).add(glyph);
            right = Math.max(right, glyph.getBounds().x + glyph.getBounds().width - 1);
        }
        final List<Glyph> result = new ArrayList<>(glyphs);
        for (List<Glyph> group : columns) {
            if (group.size() > 1) {
                result.add(GlyphFactory.buildGlyph(group));
            }
        }
        return result;
    }
}
