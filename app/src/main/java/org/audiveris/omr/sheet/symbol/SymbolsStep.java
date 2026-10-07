//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y m b o l s S t e p                                      //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.note.ChordsBuilder;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class <code>SymbolsStep</code> retrieves fixed-shape symbols in a system.
 * <p>
 * This accounts for rests, flags, dots, tuplets, alterations, ...
 *
 * @author Hervé Bitteur
 */
public class SymbolsStep
        extends AbstractSystemStep<SymbolsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsStep.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SymbolsStep object.
     */
    public SymbolsStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (OmrStep step,
                           Sheet sheet)
    {
        sheet.getSheetEditor().refresh();

        // Update glyph board if needed (to see OCR'd data)
        final SelectionService service = sheet.getGlyphIndex().getEntityService();

        @SuppressWarnings("unchecked")
        final EntityListEvent<Glyph> listEvent = (EntityListEvent<Glyph>) service.getLastEvent(
                EntityListEvent.class);

        if (listEvent != null) {
            service.publish(listEvent);
        }
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Sheet sheet,
                             Context context)
        throws StepException
    {
        promoteConsistentRests(sheet);
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
        throws StepException
    {
        /**
         * Prepare image without staff lines, with all good and weak inters erased and
         * with all weak inters saved as optional symbol parts.
         */
        final Context context = new Context();
        new SymbolsFilter(sheet).process(context.optionalsMap);

        return context;
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
        throws StepException
    {
        final StopWatch watch = new StopWatch("SymbolsStep doSystem #" + system.getId());
        watch.start("factory");

        final InterFactory factory = new InterFactory(system);

        // Retrieve symbols inters
        watch.start("buildSymbols");
        new SymbolsBuilder(system, factory).buildSymbols(context.optionalsMap);

        // Allocate rest-based chords
        watch.start("buildRestChords");
        new ChordsBuilder(system).buildRestChords();

        // Some checks that need presence of other symbols
        watch.start("lateChecks");
        factory.lateChecks();

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //------------------------//
    // promoteConsistentRests //
    //------------------------//
    /**
     * Keep a weak rest that looks like the page's confident rests of its shape.
     * <p>
     * A rest has no stem or head to support it, so its grade is the classifier's alone, and
     * under the minimum contextual grade it is dropped at reduction. On a phone capture of a
     * screen (in-rah-mennak-ya-ein, 2026-10-07) the eighth rests of bars 3 and 9 graded 0.22
     * and 0.38 while seven others alike graded 0.56 to 0.77: same 17x33 glyph, same height on
     * the staff. Dropped, bar 3's rhythm was rebuilt around the hole and played wrong with no
     * doubt. One edition draws all its rests alike, so a weak candidate of the same size
     * (within maxRestSizeRatio) at the same pitch (within maxRestPitchGap) as the median of at
     * least minConfidentRests confident ones is the same sign; saalouni-elnas, from a screen
     * too, has two, at 0.67 and 0.75, and bar 7's at 0.47. On in-rah the weak candidates the
     * rule refuses stood seven or more steps off the rests' line.
     *
     * @param sheet the sheet processed
     */
    private static void promoteConsistentRests (Sheet sheet)
    {
        final double minGrade = Grades.minContextualGrade;
        final Map<Shape, List<RestInter>> confident = new EnumMap<>(Shape.class);
        final Map<Shape, List<RestInter>> weak = new EnumMap<>(Shape.class);

        for (SystemInfo system : sheet.getSystems()) {
            for (Inter inter : system.getSig().inters(RestInter.class)) {
                final RestInter rest = (RestInter) inter;

                if ((rest.getStaff() == null) || (rest.getPitch() == null)) {
                    continue;
                }

                if (rest.getGrade() >= minGrade) {
                    confident.computeIfAbsent(rest.getShape(), k -> new ArrayList<>()).add(rest);
                } else if (rest.getGrade() >= constants.minWeakRestGrade.getValue()) {
                    weak.computeIfAbsent(rest.getShape(), k -> new ArrayList<>()).add(rest);
                }
            }
        }

        for (Map.Entry<Shape, List<RestInter>> entry : weak.entrySet()) {
            final List<RestInter> models = confident.get(entry.getKey());

            if ((models == null) || (models.size() < constants.minConfidentRests.getValue())) {
                continue;
            }

            final double width = median(models, 0);
            final double height = median(models, 1);
            final double pitch = median(models, 2);

            for (RestInter rest : entry.getValue()) {
                final double interline = rest.getStaff().getSpecificInterline();
                final Rectangle box = rest.getBounds();

                if (looksLike(
                        new double[]{box.width / interline, box.height / interline, rest.getPitch()},
                        new double[]{width, height, pitch})) {
                    logger.info("Promoted {} grade {} like {} confident ones", rest,
                            String.format("%.3f", rest.getGrade()), models.size());
                    rest.setGrade(minGrade + constants.promotionMargin.getValue());

                    // Its rest chord was built per system, at the old grade, and would take
                    // the rest down with it at reduction.
                    if (rest.getEnsemble() instanceof Inter chord) {
                        chord.setGrade(rest.getGrade());
                    }
                }
            }
        }
    }

    //-----------//
    // looksLike //
    //-----------//
    /**
     * Whether a rest's width and height (interlines) and pitch match the confident rests'.
     *
     * @param rest   {width, height, pitch} of the weak rest
     * @param models median {width, height, pitch} of the confident rests
     * @return true when within maxRestSizeRatio in size and maxRestPitchGap in pitch
     */
    static boolean looksLike (double[] rest,
                              double[] models)
    {
        final double sizeRatio = constants.maxRestSizeRatio.getValue();

        return (Math.abs(rest[0] - models[0]) <= (sizeRatio * models[0]))
                && (Math.abs(rest[1] - models[1]) <= (sizeRatio * models[1]))
                && (Math.abs(rest[2] - models[2]) <= constants.maxRestPitchGap.getValue());
    }

    /** Median width (0) or height (1), in interlines, or pitch (2) of the rests. */
    private static double median (List<RestInter> rests,
                                  int what)
    {
        final List<Double> values = new ArrayList<>();

        for (RestInter rest : rests) {
            final double interline = rest.getStaff().getSpecificInterline();
            final Rectangle box = rest.getBounds();
            values.add(switch (what) {
                case 0 -> box.width / interline;
                case 1 -> box.height / interline;
                default -> rest.getPitch();
            });
        }

        Collections.sort(values);

        return values.get(values.size() / 2);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Integer minConfidentRests = new Constant.Integer(
                "rests",
                2,
                "Confident rests of a shape needed to judge weak ones by them");

        private final Constant.Ratio minWeakRestGrade = new Constant.Ratio(
                0.15,
                "Lowest grade of a rest that may be kept for looking like the confident ones");

        private final Constant.Ratio maxRestSizeRatio = new Constant.Ratio(
                0.12,
                "Largest relative difference in width or height from the confident rests");

        private final Constant.Ratio promotionMargin = new Constant.Ratio(
                0.05,
                "How far above the minimum contextual grade a promoted rest is set");

        private final Constant.Double maxRestPitchGap = new Constant.Double(
                "pitch",
                0.5,
                "Largest pitch difference from the confident rests");
    }

    //---------//
    // Context //
    //---------//
    /**
     * Context for step processing.
     */
    protected static class Context
    {
        /** Map of optional (weak) glyphs per system. */
        public final Map<SystemInfo, List<Glyph>> optionalsMap = new TreeMap<>();
    }
}
