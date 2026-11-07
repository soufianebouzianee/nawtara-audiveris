//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e s t I n t e r                                       //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>RestInter</code> represents a rest note.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "rest")
public class RestInter
        extends AbstractNoteInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RestInter.class);

    /** Rests drawn as a filled bar, which the flat middle of a tie can imitate. */
    private static final java.util.EnumSet<Shape> BLOCK_RESTS = java.util.EnumSet.of(
            Shape.WHOLE_REST, Shape.HALF_REST, Shape.BREVE_REST);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB (and dummy measure).
     */
    protected RestInter ()
    {
    }

    /**
     * Creates a new RestInter object.
     *
     * @param glyph underlying glyph
     * @param shape rest shape
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch the rest pitch
     */
    public RestInter (Glyph glyph,
                      Shape shape,
                      Double grade,
                      Staff staff,
                      Double pitch)
    {
        super(glyph, null, shape, grade, staff, pitch);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // getChord //
    //----------//
    /**
     * Report the containing rest chord.
     *
     * @return containing rest chord
     */
    @Override
    public RestChordInter getChord ()
    {
        return (RestChordInter) getEnsemble();
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        // Standard addition task for this rest
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel, toPublish));

        // Wrap this rest within a rest chord
        final RestChordInter restChord = new RestChordInter(null);
        restChord.setManual(true);
        restChord.setStaff(staff);

        tasks.add(
                new AdditionTask(
                        staff.getSystem().getSig(),
                        restChord,
                        getBounds(),
                        Arrays.asList(new Link(this, new Containment(), true))));

        return tasks;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // createValid //
    //-------------//
    /**
     * Try to create a Rest inter.
     * <p>
     * A rest cannot be too close abscissa-wise to a head-chord.
     *
     * @param glyph            underlying glyph
     * @param shape            precise shape
     * @param grade            evaluation value
     * @param system           the related system
     * @param systemHeadChords abscissa-ordered list of head-chords in this system
     * @return the created instance or null if failed
     */
    public static RestInter createValid (Glyph glyph,
                                         Shape shape,
                                         double grade,
                                         SystemInfo system,
                                         List<Inter> systemHeadChords)
    {
        final Point centroid = glyph.getCentroid();

        // Pick up the closest staff
        final Staff restStaff = system.getClosestStaff(centroid);

        if ((restStaff == null) || restStaff.isTablature()) {
            return null;
        }

        final Measure measure = restStaff.getPart().getMeasureAt(centroid);

        if (measure == null) {
            return null;
        }

        // Check horizontal position WRT head-chords
        final int minDx = system.getSheet().getScale().toPixels(constants.minInterChordDx);
        final Rectangle glyphBox = glyph.getBounds();
        final Point2D restCenter = GeoUtil.center2D(glyphBox);
        final Rectangle fatBox = new Rectangle(glyphBox);
        fatBox.grow(minDx, 0);

        // All head-chords in staff measure
        final int left = measure.getAbscissa(HorizontalSide.LEFT, restStaff);
        final int right = measure.getAbscissa(HorizontalSide.RIGHT, restStaff);
        final List<Inter> measureChords = Inters.inters(systemHeadChords, (Inter inter) -> {
            if (inter.getStaff() != restStaff) {
                return false;
            }

            final Point center = inter.getCenter();

            return (center.x > left) && (center.x <= right); // Away from left, include right
        });

        for (Inter chord : measureChords) {
            if (fatBox.intersects(chord.getBounds())) {
                double dx = chord.getCenter2D().getX() - restCenter.getX();

                if (Math.abs(dx) < minDx) {
                    if (glyph.isVip() || logger.isDebugEnabled()) {
                        logger.info("Discarded stuck rest candidate glyph#{}", glyph.getId());
                    }

                    return null; // Failure
                }
            }
        }

        // A block rest (whole, half, breve) is a filled bar. The flat bottom of a tie or slur
        // that runs under (or over) two heads, cut by a staff line, looks like one: Aho Da Elli
        // Sar bar 5 got a phantom whole rest from the tie under its two G's. A rest never
        // reaches under a head on both of its ends.
        if (BLOCK_RESTS.contains(shape) && bridgesTwoHeads(glyphBox, measureChords,
                system.getSheet().getScale())) {
            if (glyph.isVip() || logger.isDebugEnabled()) {
                logger.info("Discarded rest candidate glyph#{} bridging two heads", glyph.getId());
            }

            return null;
        }

        // Pitch value WRT staff
        final double restPitch = restStaff.pitchPositionOf(centroid);

        RestInter restInter = new RestInter(glyph, shape, grade, restStaff, restPitch);
        restStaff.addNote(restInter);

        return restInter;
    }

    //-----------------//
    // bridgesTwoHeads //
    //-----------------//
    /**
     * Report whether a glyph box runs from under (or over) one head to under (or over) another
     * head, as the middle of a tie or slur does.
     * <p>
     * The left end must lie within the horizontal extent of a head (a little margin allowed)
     * and the right end within that of a later head, and the box must stay vertically close to
     * both heads.
     *
     * @param box    the rest candidate box
     * @param chords head-chords of the rest's staff measure
     * @param scale  sheet scale
     * @return true when the box bridges two heads
     */
    static boolean bridgesTwoHeads (Rectangle box,
                                    List<Inter> chords,
                                    Scale scale)
    {
        final int margin = scale.toPixels(constants.bridgeHeadMargin);
        final int maxDy = scale.toPixels(constants.bridgeMaxDy);
        final List<Rectangle> heads = new ArrayList<>();

        for (Inter chord : chords) {
            if (chord instanceof HeadChordInter headChord) {
                for (Inter note : headChord.getNotes()) {
                    heads.add(note.getBounds());
                }
            }
        }

        return bridgesTwoHeads(box, heads, margin, maxDy);
    }

    /**
     * Pixel version of {@link #bridgesTwoHeads(Rectangle, List, Scale)}.
     *
     * @param box    the rest candidate box
     * @param heads  head boxes
     * @param margin horizontal margin around a head's extent, in pixels
     * @param maxDy  maximum vertical gap between the box and a head, in pixels
     * @return true when the box bridges two heads
     */
    static boolean bridgesTwoHeads (Rectangle box,
                                    List<Rectangle> heads,
                                    int margin,
                                    int maxDy)
    {
        final int left = box.x;
        final int right = (box.x + box.width) - 1;
        boolean leftHead = false;
        boolean rightHead = false;

        for (Rectangle head : heads) {
            final int dy = Math.max(0, Math.max(head.y - (box.y + box.height),
                    box.y - (head.y + head.height)));

            if (dy > maxDy) {
                continue;
            }

            final int headLeft = head.x - margin;
            final int headRight = (head.x + head.width) - 1 + margin;
            final boolean coversLeft = (left >= headLeft) && (left <= headRight);
            final boolean coversRight = (right >= headLeft) && (right <= headRight);

            // One head under both ends is not a bridge (a rest beside a chord is ruled out
            // by the stuck check already)
            if (coversLeft && !coversRight && (head.x + (head.width / 2) < box.x + (box.width / 2))) {
                leftHead = true;
            } else if (coversRight && !coversLeft
                    && (head.x + (head.width / 2) > box.x + (box.width / 2))) {
                rightHead = true;
            }
        }

        return leftHead && rightHead;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Double suspiciousPitchPosition = new Constant.Double(
                "PitchPosition",
                2.0,
                "Maximum absolute pitch position for a rest to avoid additional checks");

        private final Scale.Fraction minInterChordDx = new Scale.Fraction(
                0.5,
                "Minimum horizontal delta between two chords");

        private final Scale.Fraction bridgeHeadMargin = new Scale.Fraction(
                0.25,
                "Horizontal margin around a head for a block rest end bridging two heads");

        private final Scale.Fraction bridgeMaxDy = new Scale.Fraction(
                1.0,
                "Maximum vertical gap between a block rest bridging two heads and each head");
    }
}
