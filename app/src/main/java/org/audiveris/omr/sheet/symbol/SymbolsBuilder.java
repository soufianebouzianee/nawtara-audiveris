//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S y m b o l s B u i l d e r                                  //
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

import static org.audiveris.omr.image.PixelSource.BACKGROUND;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphCluster;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.glyph.GlyphLink;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.KeyAltersRelation;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.StopWatch;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class <code>SymbolsBuilder</code> is in charge, at system level, of retrieving all
 * possible symbols interpretations.
 *
 * @author Hervé Bitteur
 */
public class SymbolsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SymbolsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Shape classifier to use. */
    private final Classifier classifier = ShapeClassifier.getInstance();

    /** Companion factory for symbols inters. */
    private final InterFactory factory;

    /** Areas where fine glyphs may be needed. */
    private final List<Rectangle> fineBoxes = new ArrayList<>();

    /** Scale-dependent global constants. */
    private final Parameters params;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SymbolsBuilder object.
     *
     * @param system  the dedicated system
     * @param factory the dedicated symbol factory
     */
    public SymbolsBuilder (SystemInfo system,
                           InterFactory factory)
    {
        this.system = system;
        this.factory = factory;

        sheet = system.getSheet();

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // buildSymbols //
    //--------------//
    /**
     * Find all possible interpretations of symbols composed from available system glyphs.
     * <p>
     * <b>Synopsis:</b>
     * <ol>
     * <li>retrieveFineBoxes() // Retrieve areas around small chords
     * <li>getSymbolsGlyphs() // Retrieve all glyphs usable for symbols
     * <li>buildLinks() // Build graph with distances
     * <li>processClusters(): // Group connected glyphs into clusters
     * <ol>
     * <li>FOREACH cluster of connected glyphs:
     * <ol>
     * <li>cluster.decompose() // Decompose cluster into all subsets
     * <li>FOREACH subset process(subset):
     * <ol>
     * <li>build compound glyph // Build one compound glyph per subset
     * <li>evaluateGlyph(compound) // Run shape classifier on compound
     * <li>FOREACH acceptable evaluation:
     * <ol>
     * <li>interFactory.create(eval, glyph) // Create inter(s) related to evaluation
     * </ol>
     * </ol>
     * </ol>
     * </ol>
     * </ol>
     *
     * @param optionalsMap the optional (weak) glyphs per system
     */
    public void buildSymbols (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        final StopWatch watch = new StopWatch("buildSymbols system #" + system.getId());
        logger.debug("System#{} buildSymbols", system.getId());

        // Header processing runs before the generic symbol step and can classify an Arabic
        // quarter-tone sign as its closest Western accidental. Correct the shape while the
        // original glyph raster is still available, before note pitch export consumes the key.
        correctArabicKeyAccidentals();

        // Identify areas for fine glyphs
        watch.start("retrieveFineBoxes");
        retrieveFineBoxes();

        // Retrieve all candidate glyphs
        watch.start("getSymbolsGlyphs");

        final List<Glyph> glyphs = getSymbolsGlyphs(optionalsMap);

        // Formalize glyphs relationships in a system-level graph
        watch.start("buildLinks");

        final SimpleGraph<Glyph, GlyphLink> systemGraph = Glyphs.buildLinks(glyphs, params.maxGap);

        // Process all sets of connected glyphs
        watch.start("processClusters");
        processClusters(systemGraph);

        // The traditional header parser intentionally stops when a flat sequence changes to
        // sharps (or vice versa). Arabic scores commonly use such mixed signatures. At this
        // stage the trailing sign has been classified by the general symbol classifier, or can
        // be reconstructed from its staff-line-split SYMBOL fragments.
        augmentMixedHeaderKeys();

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //---------------//
    // evaluateGlyph //
    //---------------//
    /**
     * Evaluate a provided glyph and create all acceptable inter instances.
     *
     * @param glyph the glyph to evaluate
     */
    private void evaluateGlyph (Glyph glyph)
    {
        if (glyph.getId() == 0) {
            glyph = sheet.getGlyphIndex().registerOriginal(glyph);
        }

        logger.debug("evaluateGlyph on {}", glyph);

        if (glyph.isVip()) {
            logger.info("VIP evaluateGlyph on {}", glyph);
        }

        final Point2D center = glyph.getCenter2D();
        final Staff closestStaff = system.getClosestStaff(center); // Just an indication!

        if (closestStaff == null) {
            return;
        }

        final Evaluation[] evals = classifier.evaluate(
                glyph,
                system,
                constants.maxEvaluationCount.getValue(),
                Grades.symbolMinGrade,
                EnumSet.of(Classifier.Condition.CHECKED));

        final SIGraph sig = system.getSig();
        final List<Inter> createdInters = new ArrayList<>();

        // Some Arabic music fonts encode quarter-tone accidentals as private-use glyphs that
        // are not present in the trainable Audiveris shape set. Detect these isolated glyphs
        // from scale-normalized geometry and their note-head context, but preserve a confident
        // standard accidental classification.
        // The Arabic reading is one candidate among the classifier's, not a verdict: the
        // evaluations below are still created, each in exclusion with it, so that a wrong
        // promotion can lose to a better-supported interpretation during SIG reduction.
        final Shape arabicAccidental = getArabicAccidental(glyph, closestStaff, evals);

        if (arabicAccidental != null) {
            final AlterInter alter = AlterInter.create(
                    glyph,
                    arabicAccidental,
                    0.95,
                    closestStaff);
            sig.addVertex(alter);
            final List<Inter> systemHeads = sig.inters(HeadInter.class);
            Collections.sort(systemHeads, org.audiveris.omr.sig.inter.Inters.byAbscissa);
            alter.setLinks(systemHeads);
            createdInters.add(alter);
            logger.debug("Detected Arabic {} glyph#{} as {}", arabicAccidental, glyph.getId(), alter);
        }

        // Create one interpretation for each acceptable evaluation

        for (Evaluation eval : evals) {
            try {
                final Inter created = factory.create(eval, glyph, closestStaff);

                if (created != null) {
                    // Set exclusion with all competitors already created for the same glyph
                    for (Inter other : createdInters) {
                        sig.insertExclusion(other, created, Exclusion.ExclusionCause.OVERLAP);
                    }

                    createdInters.add(created);
                }
            } catch (Exception ex) {
                logger.warn("Error in glyph evaluation " + ex, ex);
            }
        }
    }

    //-----------------//
    // getArabicAccidental //
    //-----------------//
    /**
     * Check for the quarter-tone accidental glyphs used by Arabic music fonts in the local
     * corpus. The note-head proximity check prevents similarly sized non-accidental symbols from
     * being promoted to accidentals.
     *
     * @param glyph candidate glyph
     * @param staff nearby staff
     * @return the detected accidental shape, or null if this is not an accidental
     */
    private Shape getArabicAccidental (Glyph glyph,
                                      Staff staff,
                                      Evaluation[] evals)
    {
        final boolean classifiedFlat = (evals.length > 0) && (evals[0].shape == Shape.FLAT);
        final Shape candidate = getArabicAccidentalShape(glyph, staff, classifiedFlat);
        if (classifiedFlat) {
            dumpGlyph("body", staff, staff.pitchPositionOf(glyph.getCenter2D()), glyph,
                    Shape.FLAT, candidate, null, false);
        }

        if (candidate == null) {
            return null;
        }

        if (candidate == Shape.QUARTER_FLAT) {
            if (hasConfidentStandardSharp(evals)) {
                return null;
            }
        }

        final Rectangle box = glyph.getBounds();
        final Scale scale = sheet.getScale();
        final int profile = system.getProfile();
        final int xGapMax = scale.toPixels(AlterHeadRelation.getXOutGapMaximum(profile));
        final int yGapMax = scale.toPixels(AlterHeadRelation.getYGapMaximum(profile));
        final int accidentalY = (candidate == Shape.QUARTER_FLAT)
                ? box.y + ((3 * box.height) / 4)
                : box.y + (box.height / 2);
        final Point accidentalPoint = new Point(box.x + box.width, accidentalY);

        for (Inter inter : system.getSig().inters(HeadInter.class)) {
            final HeadInter head = (HeadInter) inter;

            if (head.getStaff() != staff) {
                continue;
            }

            final Point notePoint = head.getCenterLeft();
            final int xGap = notePoint.x - accidentalPoint.x;
            final int yGap = Math.abs(notePoint.y - accidentalPoint.y);

            if ((xGap >= 0) && (xGap <= xGapMax) && (yGap <= yGapMax)) {
                return candidate;
            }
        }

        return null;
    }

    //------------------------//
    // augmentMixedHeaderKeys //
    //------------------------//
    /** Attach a close trailing opposite-sign accidental to a traditional header key. */
    private void augmentMixedHeaderKeys ()
    {
        final SIGraph sig = system.getSig();

        for (Staff staff : system.getStaves()) {
            if ((staff.getHeader() == null) || (staff.getHeader().key == null)
                    || (staff.getHeader().clef == null)) {
                continue;
            }

            final KeyInter key = staff.getHeader().key;
            final List<Inter> members = key.getMembers();
            if (members.isEmpty()) {
                continue;
            }

            final Shape baseShape = members.get(0).getShape();
            if ((baseShape != Shape.FLAT) && (baseShape != Shape.SHARP)) {
                continue;
            }

            final Rectangle keyBox = new Rectangle(key.getBounds());
            final int interline = staff.getSpecificInterline();
            final int searchLeft = keyBox.x + keyBox.width + Math.max(2, interline / 2);
            final int searchRight = keyBox.x + keyBox.width + ((5 * interline) / 2);
            AlterInter candidate = null;
            boolean reconstructedCandidate = false;

            for (Inter inter : sig.inters(AlterInter.class)) {
                if ((inter instanceof KeyAlterInter) || (inter.getStaff() != staff)) {
                    continue;
                }

                final AlterInter alter = (AlterInter) inter;
                final Rectangle box = alter.getBounds();
                if ((box.x >= searchLeft) && (box.x + box.width <= searchRight)
                        && (Math.abs(box.getCenterY() - keyBox.getCenterY()) <= (2 * interline))
                        && (alter.getShape() != baseShape)
                        && ((alter.getShape() == Shape.FLAT) || (alter.getShape() == Shape.SHARP))
                        && (alter.getAlteredHead() == null)
                        && ((candidate == null) || (alter.getBestGrade() > candidate.getBestGrade()))) {
                    candidate = alter;
                }
            }

            if (candidate == null) {
                final List<Glyph> fragments = new ArrayList<>();
                for (Glyph glyph : system.getGroupedGlyphs(GlyphGroup.SYMBOL)) {
                    final Rectangle box = glyph.getBounds();
                    if ((box.x >= searchLeft) && (box.x + box.width <= searchRight)
                            && (Math.abs(box.getCenterY() - keyBox.getCenterY())
                                    <= (2 * interline))) {
                        fragments.add(glyph);
                    }
                }

                if (!fragments.isEmpty()) {
                    final Glyph compound = GlyphFactory.buildGlyph(fragments);
                    // GlyphFactory creates an in-memory compound. Register it before
                    // attaching an interpretation: mixed-header repair later promotes
                    // this candidate to KeyAlterInter, which requires a registered glyph.
                    final Glyph registeredCompound = sheet.getGlyphIndex().registerOriginal(compound);
                    final Evaluation[] evals = classifier.evaluate(
                            registeredCompound,
                            system,
                            constants.maxEvaluationCount.getValue(),
                            Grades.symbolMinGrade,
                            EnumSet.of(Classifier.Condition.CHECKED));
                    for (Evaluation eval : evals) {
                        if (((eval.shape == Shape.FLAT) || (eval.shape == Shape.SHARP))
                                && (eval.shape != baseShape)) {
                            candidate = AlterInter.create(registeredCompound, eval.shape, eval.grade, staff);
                            sig.addVertex(candidate);
                            reconstructedCandidate = true;
                            break;
                        }
                    }

                    if ((candidate == null) && (baseShape == Shape.FLAT)) {
                        final List<Rectangle> seedBoxes = new ArrayList<>();
                        for (Glyph seed : system.getGroupedGlyphs(GlyphGroup.VERTICAL_SEED)) {
                            final Rectangle box = seed.getBounds();
                            if ((box.x >= searchLeft) && (box.x + box.width <= searchRight)
                                    && (Math.abs(box.getCenterY() - keyBox.getCenterY())
                                            <= (2 * interline))) {
                                seedBoxes.add(box);
                            }
                        }

                        final boolean fragmentedSharp = hasFragmentedSharpGeometry(
                                compound.getBounds(), seedBoxes, interline);
                        if (fragmentedSharp) {
                            final Glyph registered = sheet.getGlyphIndex().registerOriginal(compound);
                            candidate = AlterInter.create(registered, Shape.SHARP, 0.80, staff);
                            sig.addVertex(candidate);
                            reconstructedCandidate = true;
                        }
                    }
                }
            }

            if (candidate == null) {
                continue;
            }

            int sameShapeCount = 0;
            for (Inter member : members) {
                if (member.getShape() == candidate.getShape()) {
                    sameShapeCount++;
                }
            }
            final int signedIndex = (candidate.getShape() == Shape.SHARP)
                    ? sameShapeCount + 1 : -(sameShapeCount + 1);
            final int expectedPitch = KeyInter.getItemPitch(
                    signedIndex,
                    staff.getHeader().clef.getKind());

            // Staff-line removal can shift a sharp centroid by one diatonic position. Reject
            // anything farther away so a nearby note accidental or time digit cannot become key.
            final double pitchTolerance = reconstructedCandidate ? 2.25 : 1.25;
            if (Math.abs(candidate.getPitch() - expectedPitch) > pitchTolerance) {
                if (reconstructedCandidate && sig.containsVertex(candidate)) {
                    candidate.remove();
                }
                continue;
            }

            final KeyAlterInter keyAlter = new KeyAlterInter(candidate);
            keyAlter.setPitch((double) expectedPitch);
            candidate.remove();
            sig.addVertex(keyAlter);
            for (Inter sibling : members) {
                sig.addEdge(keyAlter, sibling, new KeyAltersRelation());
            }
            key.addMember(keyAlter);
            sig.computeContextualGrade(keyAlter);
            key.invalidateCache();
            logger.info("Extended mixed header key with {} at pitch {}", keyAlter, expectedPitch);
        }
    }

    //----------------------------------//
    // hasFragmentedSharpGeometry      //
    //----------------------------------//
    /**
     * Recognize a sharp split into components by staff-line removal.
     *
     * @param compoundBox bounds of the combined residual symbol fragments
     * @param seedBoxes   vertical stem seed bounds in the same header slot
     * @param interline   staff interline
     * @return true for sharp-like proportions with exactly two stem clusters
     */
    static boolean hasFragmentedSharpGeometry (Rectangle compoundBox,
                                               List<Rectangle> seedBoxes,
                                               double interline)
    {
        if ((compoundBox == null) || (interline <= 0)) {
            return false;
        }

        final double widthRatio = compoundBox.width / interline;
        final double heightRatio = compoundBox.height / interline;
        if ((widthRatio < 0.75) || (widthRatio > 1.5)
                || (heightRatio < 3.0) || (heightRatio > 4.5)) {
            return false;
        }

        final List<Rectangle> longSeeds = new ArrayList<>();
        for (Rectangle seed : seedBoxes) {
            if ((seed.height >= (1.5 * interline)) && compoundBox.intersects(seed)) {
                longSeeds.add(seed);
            }
        }
        longSeeds.sort((left, right) -> Integer.compare(left.x, right.x));

        int clusters = 0;
        int clusterRight = Integer.MIN_VALUE;
        for (Rectangle seed : longSeeds) {
            if (seed.x > (clusterRight + Math.max(1, (int) Math.rint(interline / 12)))) {
                clusters++;
            }
            clusterRight = Math.max(clusterRight, seed.x + seed.width - 1);
        }

        return clusters == 2;
    }

    //------------------------------//
    // getArabicAccidentalShape     //
    //------------------------------//
    /**
     * Classify Arabic quarter-tone geometry without applying note/key context.
     *
     * @param glyph candidate glyph
     * @param staff related staff
     * @return Arabic accidental shape, or null
     */
    static Shape getArabicAccidentalShape (Glyph glyph,
                                           Staff staff,
                                           boolean classifiedFlat)
    {
        final Rectangle box = glyph.getBounds();
        final double interline = staff.getSpecificInterline();

        if ((box == null) || (interline <= 0)) {
            return null;
        }

        return getArabicAccidentalShape(glyph.getBuffer(), interline, classifiedFlat);
    }

    //----------------------------------//
    // getArabicAccidentalShape         //
    //----------------------------------//
    /**
     * Classify the safe geometry-only Arabic accidental fallback.
     *
     * @param buffer candidate raster
     * @param interline staff interline in pixels
     * @return quarter-flat when its topology is present, otherwise null
     */
    static Shape getArabicAccidentalShape (ByteProcessor buffer,
                                           double interline)
    {
        return getArabicAccidentalShape(buffer, interline, false);
    }

    /**
     * Classify the Arabic accidental fallback, knowing whether the classifier read a flat.
     * <p>
     * On a scan or a photograph the half-flat's bowl fills or its slash thins, and the
     * topology tests stop matching: the classifier reads a flat, and the note sounds a quarter
     * tone low. For a glyph the classifier already calls a flat, the only question left is
     * whether its stem is slashed, and {@link #hasSlashRightOfStem} answers that one.
     *
     * @param buffer         candidate raster
     * @param interline      staff interline in pixels
     * @param classifiedFlat true when the classifier's own reading is a flat
     * @return quarter-flat when its topology or its slash is present, otherwise null
     */
    static Shape getArabicAccidentalShape (ByteProcessor buffer,
                                           double interline,
                                           boolean classifiedFlat)
    {
        if ((buffer == null) || (interline <= 0)) {
            return null;
        }

        final double widthRatio = buffer.getWidth() / interline;
        final double heightRatio = buffer.getHeight() / interline;

        // Do not infer QUARTER_SHARP from bounding-box geometry. In the validated corpus, the
        // candidate raster is the ordinary Western natural sign (two staggered stems), including
        // the signs that cancel B-flat in "Ah ya Hilu". Automatic promotion made those notes 50
        // cents sharp. QUARTER_SHARP remains supported by the model, data structures and MusicXML
        // exporter, but it must arrive from an explicit classifier result rather than this local
        // fallback.

        if ((widthRatio >= 0.65) && (widthRatio <= 1.8) && (heightRatio >= 2.2)
                && (heightRatio <= 3.6) && hasQuarterFlatTopology(buffer)) {
            return Shape.QUARTER_FLAT;
        }

        if (classifiedFlat && (widthRatio >= 0.5) && (widthRatio <= 1.8) && (heightRatio >= 2.2)
                && (heightRatio <= 3.6) && hasSlashRightOfStem(buffer)) {
            return Shape.QUARTER_FLAT;
        }

        return null;
    }

    //--------------------------------//
    // correctArabicKeyAccidentals    //
    //--------------------------------//
    /** Correct Arabic accidental glyphs already retained as members of a staff key signature. */
    private void correctArabicKeyAccidentals ()
    {
        for (Inter inter : system.getSig().inters(KeyAlterInter.class)) {
            final KeyAlterInter keyAlter = (KeyAlterInter) inter;
            final Glyph glyph = keyAlter.getGlyph();

            if ((glyph == null) || (keyAlter.getStaff() == null)) {
                continue;
            }

            final boolean classifiedFlat = keyAlter.getShape() == Shape.FLAT;
            Shape candidate = getArabicAccidentalShape(glyph, keyAlter.getStaff(), classifiedFlat);
            StemInk ink = null;
            final boolean first = isFirstOfKey(keyAlter);

            if (classifiedFlat) {
                // The glyph alone cannot tell: staff removal cuts off a slash that touches a
                // line, and damage beside a stem can look like one. The page can. It is read in
                // grey at a fixed level, not from the binary source: on a blurred scan that
                // source keeps a staff line's grey halo, five or six pixels thick, and a flat's
                // stem halo, and both then read as ink beside the stem.
                final ByteProcessor page = sheet.getPicture().getSource(Picture.SourceKey.GRAY);

                if (page != null) {
                    ink = inkBesideStem(
                            page,
                            glyph.getBounds(),
                            glyph.getBuffer(),
                            keyAlter.getStaff().getSpecificInterline());
                    candidate = crossingVerdict(candidate, ink, first);
                }
            }

            dumpGlyph("key", keyAlter.getStaff(), keyAlter.getIntegerPitch(), glyph,
                    keyAlter.getShape(), candidate, ink, first);

            if (isBarlineNotSign(ink, first)) {
                logger.info(
                        "Removed key item glyph#{}: a barline stands beside its stem",
                        glyph.getId());

                if (keyAlter.getEnsemble() instanceof KeyInter key) {
                    key.removeMember(keyAlter);
                    key.invalidateCache();
                }

                keyAlter.remove();
                continue;
            }

            if ((candidate != null) && (candidate != keyAlter.getShape())) {
                logger.info(
                        "Corrected Arabic key accidental glyph#{} from {} to {}",
                        glyph.getId(),
                        keyAlter.getShape(),
                        candidate);
                // The key's order placed this sign (KeyBuilder.adjustPitches); a new shape
                // re-measures the glyph, and a slash lifting its centre then moved yaslam's E
                // half-flat to an F. The sign stands where the key put it.
                final Double pitch = keyAlter.getPitch();
                keyAlter.setAccidentalShape(candidate);
                keyAlter.setPitch(pitch);
            }
        }
    }

    //--------------//
    // dumpKeyGlyph //
    //--------------//
    /**
     * Save a key accidental's raster and the detector's verdict, for measuring the detector
     * against a known answer. Does nothing unless the AUDIVERIS_ARABIC_DUMP environment variable
     * names a directory: each glyph becomes a PNG there, and one line of glyphs.csv.
     */
    private void dumpGlyph (String kind,
                            Staff staff,
                            double pitch,
                            Glyph glyph,
                            Shape shape,
                            Shape candidate,
                            StemInk ink,
                            boolean first)
    {
        final String dump = System.getenv("AUDIVERIS_ARABIC_DUMP");

        if ((dump == null) || dump.isBlank()) {
            return;
        }

        try {
            final java.nio.file.Path folder = java.nio.file.Paths.get(dump);
            java.nio.file.Files.createDirectories(folder);
            final Rectangle box = glyph.getBounds();
            final String name = String.format(
                    "%s-%s-s%d-p%d-x%d-y%d.png",
                    sheet.getId(),
                    kind,
                    staff.getId(),
                    (int) Math.rint(pitch),
                    box.x,
                    box.y);
            javax.imageio.ImageIO.write(
                    glyph.getBuffer().getBufferedImage(),
                    "png",
                    folder.resolve(name).toFile());
            final String line = String.format(
                    java.util.Locale.ROOT,
                    "%s,%s,%d,%d,%d,%d,%d,%d,%.3f,%s,%s,%s,%.3f,%s,%s%n",
                    name,
                    sheet.getId(),
                    staff.getId(),
                    (int) Math.rint(pitch),
                    box.x,
                    box.y,
                    box.width,
                    box.height,
                    (double) staff.getSpecificInterline(),
                    shape,
                    candidate,
                    kind,
                    (ink == null) ? -1.0 : ink.leftShare(),
                    (ink != null) && ink.neighbourStem(),
                    first);
            synchronized (SymbolsBuilder.class) {
                java.nio.file.Files.writeString(
                        folder.resolve("glyphs.csv"),
                        line,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            }
        } catch (Exception ex) {
            logger.warn("Could not dump key glyph#{}: {}", glyph.getId(), ex.toString());
        }
    }

    //------------------------------//
    // hasConfidentStandardSharp    //
    //------------------------------//
    /**
     * Report whether the normal classifier has recognized the glyph as a sharp. This protects
     * ordinary sharp signs in fonts whose raster shape also contains the flood-fill holes used
     * by the quarter-flat topology check.
     *
     * @param evals checked classifier evaluations, sorted by descending grade
     * @return true when sharp is the best acceptable interpretation
     */
    private static boolean hasConfidentStandardSharp (Evaluation[] evals)
    {
        return (evals.length > 0) && (evals[0].shape == Shape.SHARP);
    }

    //-------------------------//
    // hasQuarterFlatTopology //
    //-------------------------//
    /**
     * Check the two visual features that distinguish the Arabic quarter-flat from the ordinary
     * flat and sharp glyphs found in the corpus: a closed lower loop and a broad upper diagonal
     * stroke. Bounding-box dimensions alone are not sufficient because some ordinary sharp signs
     * use nearly the same dimensions as the quarter-sharp glyph.
     *
     * @param glyph glyph to inspect
     * @return true when the raster has the characteristic quarter-flat topology
     */
    private static boolean hasQuarterFlatTopology (Glyph glyph)
    {
        return hasQuarterFlatTopology(glyph.getBuffer());
    }

    //-------------------------//
    // hasQuarterFlatTopology //
    //-------------------------//
    /**
     * Check the raster topology of a potential Arabic quarter-flat.
     *
     * @param buffer glyph raster, with {@link org.audiveris.omr.image.PixelSource#BACKGROUND}
     *               pixels for the white background
     * @return true when the raster has the characteristic quarter-flat topology
     */
    static boolean hasQuarterFlatTopology (ByteProcessor buffer)
    {
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();

        if ((width < 8) || (height < 20)) {
            return false;
        }

        // In older engraved Arabic sheets, staff-line removal can open or fill the flat bowl or
        // disconnect the left half of the slash. The surviving diagonal remains reliable.
        if (hasCrossedFlatTopology(buffer) || hasRightSlashedFlatTopology(buffer)) {
            return true;
        }

        // The lower loop encloses white pixels. Flood-fill the background from the border so
        // that only enclosed white regions remain as holes.
        final boolean[] exterior = new boolean[width * height];
        final Deque<Integer> pending = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            enqueueBackground(buffer, x, 0, exterior, pending);
            enqueueBackground(buffer, x, height - 1, exterior, pending);
        }

        for (int y = 1; y < (height - 1); y++) {
            enqueueBackground(buffer, 0, y, exterior, pending);
            enqueueBackground(buffer, width - 1, y, exterior, pending);
        }

        while (!pending.isEmpty()) {
            final int offset = pending.removeFirst();
            final int x = offset % width;
            final int y = offset / width;

            if (x > 0) {
                enqueueBackground(buffer, x - 1, y, exterior, pending);
            }
            if (x < (width - 1)) {
                enqueueBackground(buffer, x + 1, y, exterior, pending);
            }
            if (y > 0) {
                enqueueBackground(buffer, x, y - 1, exterior, pending);
            }
            if (y < (height - 1)) {
                enqueueBackground(buffer, x, y + 1, exterior, pending);
            }
        }

        int lowerHolePixels = 0;
        final int lowerStart = height / 2;

        for (int y = lowerStart; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int offset = (y * width) + x;

                if ((buffer.get(x, y) == BACKGROUND) && !exterior[offset]) {
                    lowerHolePixels++;
                }
            }
        }

        if (lowerHolePixels < Math.max(8, height / 3)) {
            return false;
        }

        // A quarter-flat has a broad upper diagonal stroke. An ordinary flat only has its
        // narrow vertical stem in this region; ordinary sharps are filtered by their stems.
        int upperSpan = 0;
        // Restrict this to the top third. Looking as far down as the lower bowl makes a normal
        // flat appear broad even though it has no upper slash.
        final int upperEnd = Math.max(1, height / 3);

        for (int y = 0; y < upperEnd; y++) {
            int first = width;
            int last = -1;

            for (int x = 0; x < width; x++) {
                if (buffer.get(x, y) == 0) {
                    first = Math.min(first, x);
                    last = Math.max(last, x);
                }
            }

            if (last >= first) {
                upperSpan = Math.max(upperSpan, last - first + 1);
            }
        }

        int longColumns = 0;

        for (int x = 0; x < width; x++) {
            int columnPixels = 0;

            for (int y = 0; y < height; y++) {
                if (buffer.get(x, y) == 0) {
                    columnPixels++;
                }
            }

            if (columnPixels >= ((height * 2) / 3)) {
                longColumns++;
            }
        }

        // The sharp has two stems that span nearly the complete glyph height. The quarter-flat
        // has one stem, so allowing only a few thick columns rejects the sharp even when its two
        // bars create flood-fill holes of their own.
        return (upperSpan >= Math.max(5, (width * 2) / 5)) && (longColumns <= 3);
    }

    //------------------//
    // isHalfFlatOnPage //
    //------------------//
    /**
     * Whether a key sign the classifier did not read is a half-flat, by its shape and by the
     * page around it: a flat's size, a slash right of its stem's top (hasSlashRightOfStem), and
     * ink crossing to the stem's left on the grey page (inkBesideStem, SLASH_PRESENT).
     * <p>
     * Looser than isSlashedFlat, which demands the closed bowl and the slash across the stem in
     * the glyph itself: saalouni-elnas's half-flats, bold and grey on a phone capture of a
     * screen, have neither intact, and cropped to their pitch they keep only stem and slash.
     * A note's flag has that shape too, but nothing left of its stem; the page tells them apart.
     * It is only asked of a sign whose key's first sign read clearly (KeyBuilder.rescueByPitch).
     *
     * @param page      the grey page, or null
     * @param box       the glyph's bounds on the page
     * @param buffer    the glyph raster
     * @param interline staff interline, in pixels
     * @return true when the glyph is a half-flat
     */
    public static boolean isHalfFlatOnPage (ByteProcessor page,
                                            Rectangle box,
                                            ByteProcessor buffer,
                                            int interline)
    {
        if ((page == null) || (buffer == null) || (interline <= 0)) {
            return false;
        }

        final double widthRatio = buffer.getWidth() / (double) interline;
        final double heightRatio = buffer.getHeight() / (double) interline;

        if ((widthRatio < 0.5) || (widthRatio > 1.8) || (heightRatio < 2.2) || (heightRatio > 3.6)
                || !hasSlashRightOfStem(buffer)) {
            return false;
        }

        final StemInk ink = inkBesideStem(page, box, buffer, interline);

        return (ink != null) && (ink.leftShare() >= SLASH_PRESENT);
    }

    //---------------------//
    // hasSlashRightOfStem //
    //---------------------//
    /**
     * Report whether ink stands to the right of a flat's stem in its upper third.
     * <p>
     * The stem is the leftmost run of columns that rise to the glyph's top and run most of its
     * height; a flat's bowl never reaches the top, however thick staff removal and ink spread
     * make it. A plain flat has nothing to the right of its stem up there. A half-flat has its
     * slash: across chunk-1's key signatures, rendered clean, as a 100 DPI scan and as a phone
     * photo, the slash filled 0.15-0.25 of a half-flat's height in that zone, and ink there
     * reached at most 0.08 of a scanned or photographed flat's. Any threshold from 0.08 to 0.12
     * gave the same result; 0.10 is the middle.
     *
     * @param buffer glyph raster
     * @return true when a slash crosses the stem's upper third
     */
    static boolean hasSlashRightOfStem (ByteProcessor buffer)
    {
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();

        if ((width < 6) || (height < 20)) {
            return false;
        }

        final int[] stem = stemColumns(buffer);

        if (stem == null) {
            return false;
        }

        final int stemRight = stem[1];
        final int longThreshold = (height * 2) / 3;
        final int[] columnInk = new int[width];

        // The slash count below must know which columns right of the stem are tall.
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (buffer.get(x, y) != BACKGROUND) {
                    columnInk[x]++;
                }
            }
        }

        final int margin = Math.max(2, width / 8);
        int slashRows = 0;

        for (int y = 0; y < (height / 3); y++) {
            for (int x = stemRight + margin; x < width; x++) {
                // A slash is a short stroke. A tall column right of the stem is the side of a
                // bowl ink has spread up, or another stem merged into the glyph: neither is a
                // slash. Ignoring them changed nothing on chunk-1's key glyphs; it is there so
                // that a merge cannot promote a flat.
                if (columnInk[x] >= longThreshold) {
                    continue;
                }

                if (buffer.get(x, y) != BACKGROUND) {
                    slashRows++;
                    break;
                }
            }
        }

        return slashRows >= (SLASH_ROWS_MIN * height);
    }

    /** Share of a flat's height its slash must fill beside the stem (see hasSlashRightOfStem). */
    private static final double SLASH_ROWS_MIN = 0.10;

    //---------------//
    // isSlashedFlat //
    //---------------//
    /**
     * Whether a glyph is a flat with a stroke through its stem, whatever the classifier called
     * it: one stem, a closed bowl, and a stroke crossing the stem.
     * <p>
     * A half-flat whose slash reaches far left of the stem, as on a scan of Adel Samuel's
     * sheets, is read by the classifier as a cut-time or a time-signature digit, never a flat,
     * and a flat key signature keeps only glyphs read as flats: the key signature then lost its
     * first sign, or failed altogether.
     *
     * @param buffer glyph raster
     * @return true for a slashed flat
     */
    public static boolean isSlashedFlat (ByteProcessor buffer)
    {
        return (buffer != null) && hasQuarterFlatTopology(buffer) && hasCrossedFlatTopology(buffer);
    }

    //--------------//
    // stemColumns  //
    //--------------//
    /**
     * Where a flat's stem stands in its glyph: the first run of columns that reach the glyph's
     * top and hold ink over two thirds of its height.
     *
     * @param buffer the glyph raster
     * @return {left, right} columns of the stem, or null when there is none
     */
    static int[] stemColumns (ByteProcessor buffer)
    {
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();
        final int top = Math.max(1, (int) (height * 0.15));
        final int longThreshold = (height * 2) / 3;
        int stemLeft = -1;
        int stemRight = -1;

        for (int x = 0; x < width; x++) {
            int ink = 0;
            boolean reachesTop = false;

            for (int y = 0; y < height; y++) {
                if (buffer.get(x, y) != BACKGROUND) {
                    ink++;
                    reachesTop |= y < top;
                }
            }

            final boolean tall = reachesTop && (ink >= longThreshold);

            if (tall && (stemLeft < 0)) {
                stemLeft = x;
                stemRight = x;
            } else if (tall && (stemRight == (x - 1))) {
                stemRight = x;
            } else if (stemLeft >= 0) {
                break;
            }
        }

        return (stemLeft < 0) ? null : new int[]{stemLeft, stemRight};
    }

    //----------//
    // StemInk  //
    //----------//
    /**
     * What the page shows beside a key flat's stem.
     *
     * @param leftShare     share of the glyph's height, in its upper part, with ink just left of
     *                      the stem once staff lines are out
     * @param neighbourStem whether a tall stroke stands about a space to the left: another sign's
     *                      stem, whose bowl would reach the stem
     */
    record StemInk (double leftShare, boolean neighbourStem)
    {
    }

    //----------------//
    // inkBesideStem  //
    //----------------//
    /**
     * Measure a key flat's surroundings on the page.
     * <p>
     * A half-flat's slash crosses its stem and a flat has nothing left of its stem, so ink there
     * is the slash, wherever along the stem it was drawn. It is measured on the page rather than
     * on the glyph, because a slash that touches a staff line is cut off the glyph by staff
     * removal: walnba-yagmil's B half-flat, a hand-placed slash low on the stem, was missed on four
     * systems out of eight that way. Staff lines are taken out here, keeping any ink that runs
     * past a line, which is a stroke crossing it.
     *
     * @param page      the grey page
     * @param box       the glyph's bounds on the page
     * @param glyph     the glyph raster
     * @param interline the staff interline, in pixels
     * @return the measure, or null when the glyph has no stem to measure from
     */
    static StemInk inkBesideStem (ByteProcessor page,
                                  Rectangle box,
                                  ByteProcessor glyph,
                                  int interline)
    {
        final int[] stem = stemColumns(glyph);

        if ((stem == null) || (interline <= 0)) {
            return null;
        }

        final int stemColumn = 3 * interline;
        final int x0 = (box.x + stem[0]) - stemColumn;
        final boolean[][] band = inkWithoutStaffLines(
                page,
                x0,
                box.y,
                6 * interline,
                box.height,
                interline);

        // The slash: ink just left of the stem, in the glyph's upper part.
        final int upper = (int) (box.height * SLASH_UPPER_SHARE);
        final int from = (int) (stemColumn - (SLASH_REACH * interline));
        int rows = 0;

        for (int y = 0; y < upper; y++) {
            for (int x = from; x < (stemColumn - 1); x++) {
                if (band[y][x]) {
                    rows++;
                    break;
                }
            }
        }

        // A neighbour: a stroke two thirds of the glyph tall, a space or so to the left.
        final int n0 = Math.max(0, (int) (stemColumn - (1.3 * interline)));
        final int n1 = Math.max(0, (int) (stemColumn - (0.3 * interline)));
        boolean neighbour = false;

        for (int x = n0; (x < n1) && !neighbour; x++) {
            int ink = 0;

            for (int y = 0; y < box.height; y++) {
                if (band[y][x]) {
                    ink++;
                }
            }

            neighbour = ink >= (0.6 * box.height);
        }

        return new StemInk(rows / (double) box.height, neighbour);
    }

    //------------------------//
    // inkWithoutStaffLines   //
    //------------------------//
    /**
     * The page's ink in a band, with staff lines taken out: grey darker than {@link #INK_LEVEL}.
     * <p>
     * A staff line is a run of rows dark across most of the band and no thicker than a line; its
     * pixels are cleared unless ink continues both above and below them, which is a stroke
     * crossing the line. Outside the page is background.
     */
    static boolean[][] inkWithoutStaffLines (ByteProcessor page,
                                             int x0,
                                             int y0,
                                             int width,
                                             int height,
                                             int interline)
    {
        final boolean[][] dark = new boolean[height][width];
        final boolean[] lineRow = new boolean[height];

        for (int y = 0; y < height; y++) {
            int count = 0;

            for (int x = 0; x < width; x++) {
                final int px = x0 + x;
                final int py = y0 + y;
                final boolean ink = (px >= 0) && (py >= 0) && (px < page.getWidth())
                                            && (py < page.getHeight())
                                            && (page.get(px, py) < INK_LEVEL);
                dark[y][x] = ink;

                if (ink) {
                    count++;
                }
            }

            lineRow[y] = count > (0.55 * width);
        }

        final boolean[][] out = new boolean[height][];

        for (int y = 0; y < height; y++) {
            out[y] = dark[y].clone();
        }

        // Round half to even, as the measurements that set the thresholds did.
        final int thick = Math.max(2, (int) Math.rint(interline / 8.0) + 1);
        int y = 0;

        while (y < height) {
            if (!lineRow[y]) {
                y++;
                continue;
            }

            int y1 = y;

            while (((y1 + 1) < height) && lineRow[y1 + 1]) {
                y1++;
            }

            if (((y1 - y) + 1) <= (thick + 1)) {
                for (int x = 0; x < width; x++) {
                    final boolean above = (y > 0) && dark[y - 1][x];
                    final boolean below = ((y1 + 1) < height) && dark[y1 + 1][x];

                    if (!(above && below)) {
                        for (int yy = y; yy <= y1; yy++) {
                            out[yy][x] = false;
                        }
                    }
                }
            }

            y = y1 + 1;
        }

        return out;
    }

    //-----------------//
    // crossingVerdict //
    //-----------------//
    /**
     * Settle a key flat by the ink beside its stem.
     * <p>
     * A promotion with no ink left of the stem is withdrawn: every half-flat measured has some,
     * and 2alo-tra's two false half-flats, JPEG damage at a stem's top, have none. A flat is
     * promoted only as the first sign of its key, where no other sign's bowl can stand beside the
     * stem, and only with no other stem a space to its left, in case the real first sign was
     * missed. Later signs are left as read: a three-flat key packs the E flat's bowl against the
     * A flat's stem.
     *
     * @param candidate  the verdict so far
     * @param ink        the measure beside the stem, or null
     * @param firstOfKey whether the sign is its key's first
     * @return the verdict
     */
    static Shape crossingVerdict (Shape candidate,
                                  StemInk ink,
                                  boolean firstOfKey)
    {
        if (ink == null) {
            return candidate;
        }

        if ((candidate == Shape.QUARTER_FLAT) && (ink.leftShare() < SLASH_ABSENT)) {
            return null;
        }

        if ((candidate == null) && firstOfKey && !ink.neighbourStem()
                && (ink.leftShare() >= SLASH_PRESENT)) {
            return Shape.QUARTER_FLAT;
        }

        return candidate;
    }

    //------------------//
    // isBarlineNotSign //
    //------------------//
    /**
     * Whether a later key "flat" is really part of a barline that follows the key.
     * <p>
     * aghnia-3shriin prints B flat and E half-flat, then a repeat sign: the key builder took the
     * thin barline and dots for a third flat, the massive ink beside it made it a half-flat, and
     * every A of the song sounded a quarter tone low. A slash leaves at most a third of a flat's
     * height inked beside the stem (0.32 over every key sign measured); a barline fills it, and
     * brings its own stroke a space to the left. Over some 3,600 key signs, only two measured
     * above 0.35, and both were barline fragments.
     *
     * @param ink        the measure beside the stem, or null
     * @param firstOfKey whether the sign is its key's first
     * @return true to remove the item from the key
     */
    static boolean isBarlineNotSign (StemInk ink,
                                     boolean firstOfKey)
    {
        return (ink != null) && !firstOfKey && ink.neighbourStem()
                       && (ink.leftShare() > NOT_A_SIGN);
    }

    /** Above this share of ink beside its stem, a later key sign is a barline's (see above). */
    private static final double NOT_A_SIGN = 0.45;

    //--------------//
    // isFirstOfKey //
    //--------------//
    private static boolean isFirstOfKey (KeyAlterInter keyAlter)
    {
        if (keyAlter.getEnsemble() instanceof KeyInter key) {
            final List<Inter> members = key.getMembers();

            return !members.isEmpty() && (members.get(0) == keyAlter);
        }

        return false;
    }

    /**
     * Below this share of a flat's height with ink left of its stem, there is no slash. Over the
     * chunk-1 renders and the real pages checked by eye, half-flats measure 0.064 and more; the
     * false ones, 0.023 and less.
     */
    private static final double SLASH_ABSENT = 0.035;

    /**
     * At or above this share, a key's first flat is slashed. Measured by this code on the grey
     * page, first signs read 0.154 and more as half-flats and 0.113 and less as flats, the
     * highest being el-hloua-di's B flat, a 7-pixel-interline JPEG enlarged before reading.
     */
    private static final double SLASH_PRESENT = 0.135;

    /** Grey level below which a page pixel is ink, for the measure beside a stem. */
    static final int INK_LEVEL = 128;

    /** How far left of the stem the slash is looked for, in interlines. */
    private static final double SLASH_REACH = 0.45;

    /** The part of the glyph, from its top, where a slash crosses the stem. */
    private static final double SLASH_UPPER_SHARE = 0.62;

    //-------------------------//
    // hasCrossedFlatTopology //
    //-------------------------//
    /** Detect a slashed flat even when staff-line removal has damaged its enclosed bowl. */
    static boolean hasCrossedFlatTopology (ByteProcessor buffer)
    {
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();

        if ((width < 12) || (height < 24)) {
            return false;
        }

        final boolean[] longColumn = new boolean[width];
        final int longThreshold = (height * 2) / 3;

        for (int x = 0; x < width; x++) {
            int foreground = 0;

            for (int y = 0; y < height; y++) {
                if (buffer.get(x, y) != BACKGROUND) {
                    foreground++;
                }
            }

            longColumn[x] = foreground >= longThreshold;
        }

        int clusterCount = 0;
        int stemLeft = -1;
        int stemRight = -1;

        for (int x = 0; x < width; x++) {
            if (!longColumn[x] || ((x > 0) && longColumn[x - 1])) {
                continue;
            }

            clusterCount++;
            int end = x;
            while (((end + 1) < width) && longColumn[end + 1]) {
                end++;
            }
            stemLeft = x;
            stemRight = end;
            x = end;
        }

        if ((clusterCount != 1) || (stemLeft < (width / 5))
                || (stemRight > ((width * 4) / 5))) {
            return false;
        }

        final int margin = Math.max(2, width / 12);
        int upperRightRows = 0;
        int middleLeftRows = 0;
        int lowerRightRows = 0;

        for (int y = 0; y < height; y++) {
            boolean left = false;
            boolean right = false;

            for (int x = 0; x < width; x++) {
                if (buffer.get(x, y) == BACKGROUND) {
                    continue;
                }

                left |= x <= (stemLeft - margin);
                right |= x >= (stemRight + margin);
            }

            if ((y < (height / 2)) && right) {
                upperRightRows++;
            }
            if ((y >= (height / 3)) && (y < ((height * 2) / 3)) && left) {
                middleLeftRows++;
            }
            if ((y >= (height / 2)) && right) {
                lowerRightRows++;
            }
        }

        return (upperRightRows >= (height / 8))
                && (middleLeftRows >= (height / 8))
                && (lowerRightRows >= (height / 6));
    }

    //--------------------------------//
    // hasRightSlashedFlatTopology   //
    //--------------------------------//
    /**
     * Detect the narrow Arabic quarter-flat used in Bayati guides when staff-line removal has
     * disconnected the portion of its slash to the left of the stem.
     * <p>
     * Unlike an ordinary flat, this residual glyph already reaches well to the right of its lone
     * stem in the top third, before the lower bowl begins.
     *
     * @param buffer glyph raster
     * @return true when a single left stem has a surviving upper-right slash and lower-right bowl
     */
    static boolean hasRightSlashedFlatTopology (ByteProcessor buffer)
    {
        final int width = buffer.getWidth();
        final int height = buffer.getHeight();

        if ((width < 10) || (height < 24)) {
            return false;
        }

        final int longThreshold = (height * 2) / 3;
        int clusterCount = 0;
        int stemLeft = -1;
        int stemRight = -1;

        for (int x = 0; x < width; x++) {
            int foreground = 0;
            for (int y = 0; y < height; y++) {
                if (buffer.get(x, y) != BACKGROUND) {
                    foreground++;
                }
            }

            if (foreground < longThreshold) {
                continue;
            }

            if ((x == 0) || (stemRight != (x - 1))) {
                clusterCount++;
                if (clusterCount > 1) {
                    return false;
                }
                stemLeft = x;
            }
            stemRight = x;
        }

        if ((clusterCount != 1) || (stemLeft > (width / 3))) {
            return false;
        }

        final int margin = Math.max(2, width / 8);
        int upperRightRows = 0;
        int lowerRightRows = 0;

        for (int y = 0; y < height; y++) {
            boolean right = false;
            for (int x = stemRight + margin; x < width; x++) {
                if (buffer.get(x, y) != BACKGROUND) {
                    right = true;
                    break;
                }
            }

            if ((y < (height / 3)) && right) {
                upperRightRows++;
            }
            if ((y >= (height / 2)) && right) {
                lowerRightRows++;
            }
        }

        return (upperRightRows >= Math.max(3, height / 10))
                && (lowerRightRows >= (height / 6));
    }

    //-------------------------//
    // enqueueBackground      //
    //-------------------------//
    private static void enqueueBackground (ByteProcessor buffer,
                                           int x,
                                           int y,
                                           boolean[] exterior,
                                           Deque<Integer> pending)
    {
        final int offset = (y * buffer.getWidth()) + x;

        if ((buffer.get(x, y) != BACKGROUND) || exterior[offset]) {
            return;
        }

        exterior[offset] = true;
        pending.addLast(offset);
    }

    //------------------//
    // getSymbolsGlyphs //
    //------------------//
    /**
     * Report the collection of glyphs as symbols candidates.
     * <p>
     * Using a too low weight threshold would result in explosion of symbols (and inter-symbol
     * relations), which would be a disaster in terms of resources. However, we need really fine
     * glyphs to detect small flags (slashed or not).
     * <p>
     * So, we define a list of "fine boxes" at system level, which represents the boxes of system
     * small head-chords. And we use a different threshold depending on whether a glyph candidate
     * intersects or not a fine box.
     *
     * @return the candidates (ordered by abscissa)
     */
    private List<Glyph> getSymbolsGlyphs (Map<SystemInfo, List<Glyph>> optionalsMap)
    {
        // Sorted by abscissa, ordinate, id
        List<Glyph> glyphs = new ArrayList<>();

        for (Glyph glyph : system.getGroupedGlyphs(GlyphGroup.SYMBOL)) {
            final int weight = glyph.getWeight();

            if (weight >= params.minWeight) {
                glyphs.add(glyph);
            } else if ((weight >= params.minFineWeight) && hitFineBox(glyph)) {
                glyphs.add(glyph);
            }
        }

        // Include optional glyphs as well
        List<Glyph> optionals = optionalsMap.get(system);

        if ((optionals != null) && !optionals.isEmpty()) {
            for (Glyph glyph : optionals) {
                final int weight = glyph.getWeight();

                if (weight >= params.minWeight) {
                    glyphs.add(glyph);
                } else if ((weight >= params.minFineWeight) && hitFineBox(glyph)) {
                    glyphs.add(glyph);
                }
            }
        }

        return glyphs;
    }

    //------------//
    // hitFineBox //
    //------------//
    /**
     * Check whether the provided glyph intersects a fine box.
     *
     * @param glyph the glyph to check
     * @return true if in fine area
     */
    private boolean hitFineBox (Glyph glyph)
    {
        final Rectangle glyphBounds = glyph.getBounds();

        for (Rectangle box : fineBoxes) {
            if (box.intersects(glyphBounds)) {
                return true;
            }
        }

        return false;
    }

    //-----------------//
    // processClusters //
    //-----------------//
    /**
     * Process all clusters of connected glyphs, based on the glyphs graph.
     *
     * @param systemGraph the graph of candidate glyphs, with their mutual distances
     */
    private void processClusters (SimpleGraph<Glyph, GlyphLink> systemGraph)
    {
        // Retrieve all the clusters of glyphs (sets of connected glyphs)
        final ConnectivityInspector<Glyph, GlyphLink> inspector = new ConnectivityInspector<>(
                systemGraph);
        final List<Set<Glyph>> sets = inspector.connectedSets();
        logger.debug("symbols sets: {}", sets.size());

        final int interline = sheet.getInterline();
        final int maxPartCount = constants.maxPartCount.getValue();

        for (Set<Glyph> set : sets) {
            final int setSize = set.size();
            logger.debug("set size: {}", setSize);

            if (setSize > 1) {
                final Set<Glyph> subSet; // Use an upper limit for set size

                if (setSize <= maxPartCount) {
                    subSet = set;
                } else {
                    List<Glyph> list = new ArrayList<>(set);
                    Collections.sort(list, Glyphs.byReverseWeight);
                    list = list.subList(0, Math.min(list.size(), maxPartCount));
                    subSet = new LinkedHashSet<>(list);
                    logger.debug("Symbol parts shrunk from {} to {}", setSize, maxPartCount);
                }

                // Use just the subgraph for this (sub)set
                final SimpleGraph<Glyph, GlyphLink> subGraph;
                subGraph = GlyphCluster.getSubGraph(subSet, systemGraph, true);
                new GlyphCluster(new SymbolAdapter(subGraph), GlyphGroup.SYMBOL).decompose();
            } else {
                // The set is just an isolated glyph, to be evaluated directly
                final Glyph glyph = set.iterator().next();

                if (classifier.isBigEnough(glyph, interline)) {
                    evaluateGlyph(glyph);
                }
            }
        }
    }

    //-------------------//
    // retrieveFineBoxes //
    //-------------------//
    /**
     * Define a fine box on the right side of every small chord, specifically meant for
     * detecting a potential small flag there.
     */
    private void retrieveFineBoxes ()
    {
        final List<Inter> smallChords = system.getSig().inters(SmallChordInter.class);

        for (Inter inter : smallChords) {
            final Rectangle box = inter.getBounds();
            final Rectangle fineBox = new Rectangle(
                    box.x + box.width,
                    box.y,
                    params.smallChordMargin,
                    box.height);
            fineBoxes.add(fineBox);
        }
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

        private final Constant.Integer maxPartCount = new Constant.Integer(
                "Glyphs",
                7,
                "Maximum number of parts considered for a symbol");

        private final Constant.Integer maxEvaluationCount = new Constant.Integer(
                "Evaluations",
                2,
                "Maximum number of evaluations kept for a symbol");

        private final Scale.Fraction maxGap = new Scale.Fraction(
                0.8,
                "Maximum distance between two compound parts");

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.03,
                "Minimum weight for glyph consideration");

        private final Scale.AreaFraction minFineWeight = new Scale.AreaFraction(
                0.006,
                "Minimum weight for glyph consideration in a fine area");

        private final Scale.Fraction smallChordMargin = new Scale.Fraction(
                1,
                "Margin on right side of small chords to extend fine boxes");

        private final Scale.Fraction maxSymbolWidth = new Scale.Fraction(
                4.0,
                "Maximum width for a symbol");

        private final Scale.Fraction maxSymbolHeight = new Scale.Fraction(
                10.0,
                "Maximum height for a symbol (when found within staff abscissa range)");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class <code>Parameters</code> gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        final double maxGap;

        final int maxSymbolWidth;

        final int maxSymbolHeight;

        final int smallChordMargin;

        final int minWeight;

        final int minFineWeight;

        Parameters (Scale scale)
        {
            maxGap = scale.toPixelsDouble(constants.maxGap);
            maxSymbolWidth = scale.toPixels(constants.maxSymbolWidth);
            maxSymbolHeight = scale.toPixels(constants.maxSymbolHeight);
            smallChordMargin = scale.toPixels(constants.smallChordMargin);
            minWeight = scale.toPixels(constants.minWeight);
            minFineWeight = scale.toPixels(constants.minFineWeight);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }

    //---------------//
    // SymbolAdapter //
    //---------------//
    private class SymbolAdapter
            extends GlyphCluster.AbstractAdapter
    {
        private final Scale scale = sheet.getScale();

        SymbolAdapter (SimpleGraph<Glyph, GlyphLink> graph)
        {
            super(graph);
        }

        @Override
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            SymbolsBuilder.this.evaluateGlyph(glyph);
        }

        @Override
        public boolean isTooLarge (Rectangle symBox)
        {
            // Check width
            if (symBox.width > params.maxSymbolWidth) {
                return true;
            }

            // Check height (not limited if on left of system: braces / brackets)
            if (GeoUtil.center2D(symBox).getX() < system.getLeft()) {
                return false;
            } else {
                return symBox.height > params.maxSymbolHeight;
            }
        }

        @Override
        public boolean isTooLight (int weight)
        {
            double normed = scale.pixelsToAreaFrac(weight);

            return !classifier.isBigEnough(normed);
        }
    }
}
