//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      H e a d e r s S t e p                                     //
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.ClefInter.ClefKind;
import org.audiveris.omr.step.AbstractSystemStep;
import org.audiveris.omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class <code>HeadersStep</code> implements <b>HEADERS</b> step, which handles the beginning
 * of every staff in a system.
 *
 * @author Hervé Bitteur
 */
public class HeadersStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeadersStep.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>HeadersStep</code> object.
     */
    public HeadersStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
        throws StepException
    {
        new HeaderBuilder(system).processHeader(); // -> Staff clef + key + time
    }

    //----------//
    // doEpilog //
    //----------//
    /**
     * Arabic fork: a system's clef that disagrees with a clearly read treble clef on the same
     * staff of the page, and is itself doubtful, is replaced by that clef.
     * <p>
     * On a photo, a curled staff's lines are found from well right of its start, so its header
     * begins past the clef, and a key sign or a note there was read as a bass or C clef
     * (foq-el-nakhl, hena-esekran): the whole system then played a sixth and an octave off.
     * The treble clefs of the page were read at 0.65 to 0.78, those false clefs at 0.41 to 0.61.
     */
    @Override
    protected void doEpilog (Sheet sheet,
                             Void context)
        throws StepException
    {
        if (sheet.getSystems().isEmpty()) {
            return;
        }

        final int staffCount = sheet.getSystems().get(0).getStaves().size();

        for (SystemInfo system : sheet.getSystems()) {
            if (system.getStaves().size() != staffCount) {
                return; // Staves cannot be matched from one system to the next
            }
        }

        for (int index = 0; index < staffCount; index++) {
            ClefInter reference = null;

            for (SystemInfo system : sheet.getSystems()) {
                final StaffHeader header = system.getStaves().get(index).getHeader();
                final ClefInter clef = (header != null) ? header.clef : null;

                if ((clef != null) && ((reference == null) || (clef.getGrade() > reference
                        .getGrade()))) {
                    reference = clef;
                }
            }

            // Only towards a treble clef, the clef of this repertoire: a page of treble clefs
            // (a songbook scan, holdout book "2") had a false bass clef read at 0.6 and more.
            if ((reference == null) || (reference.getKind() != ClefKind.TREBLE) || (reference
                    .getGrade() < constants.minReferenceGrade.getValue())) {
                continue;
            }

            for (SystemInfo system : sheet.getSystems()) {
                final Staff staff = system.getStaves().get(index);
                final StaffHeader header = staff.getHeader();
                final ClefInter clef = (header != null) ? header.clef : null;

                if ((clef == null) || (clef.getKind() == reference.getKind()) || (clef
                        .getGrade() >= constants.maxReplacedGrade.getValue())) {
                    continue;
                }

                final ClefInter replacement = reference.replicate(staff);
                replacement.setBounds(clef.getBounds());
                logger.info(
                        "Staff#{} header clef {} replaced by {} as read elsewhere on the page",
                        staff.getId(),
                        clef,
                        reference.getShape());
                clef.remove();
                system.getSig().addVertex(replacement);
                replacement.freeze();
                header.clef = replacement;
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio minReferenceGrade = new Constant.Ratio(
                0.6,
                "Minimum grade of a header clef that other staves' clefs can be set to");

        private final Constant.Ratio maxReplacedGrade = new Constant.Ratio(
                0.65,
                "Maximum grade of a disagreeing header clef that is replaced");
    }
}
