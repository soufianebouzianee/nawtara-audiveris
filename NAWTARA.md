# Audiveris for Arabic music: the Nawtara version

This is the source of the modified Audiveris that the Nawtara service
(https://nawtara.com) runs to read photographed and scanned sheet music.
It is published under the same licence as Audiveris itself, the GNU Affero
General Public License v3.0 (see `LICENSE`), as that licence requires of a
modified version offered to users over a network.

- **Based on:** Audiveris, https://github.com/Audiveris/audiveris, commit
  `7a36078e7ba0c006052c1f661b949cf9b729f505`. Audiveris is © Hervé Bitteur
  and the Audiveris contributors.
- **Modified by:** Soufiane Bouziane (Nawtara), 2026.
- **Contact:** legal@nawtara.com

## What was changed, and when

The changes teach Audiveris the accidentals of Arabic music notation:
quarter-tone flats and sharps in the body of a piece and in key signatures,
as they are printed, handwritten, scanned and photographed.

| Date | Change |
| --- | --- |
| 2026-09-04 | Quarter-tone accidentals (half-flat, half-sharp) as shapes, read on notes and exported to MusicXML |
| 2026-09-05 | Key signatures mixing flats and sharps, as Arabic scores print them; sharps broken into pieces recognised |
| 2026-09-06 | Refinement of the symbol recognition added on 2026-09-05 |
| 2026-09-22 | A quarter-flat reading competes with the classifier's reading instead of overruling it |
| 2026-09-23 | A scanned or photographed half-flat recognised by the slash through its stem |
| 2026-09-24 | Half-flats read in key signatures; a barline no longer mistaken for a key sign |
| 2026-09-25 | Flat key signatures refused because of a half-flat's bowl are retried; a promoted key sign keeps its pitch; headless start without GTK on Linux |
| 2026-09-28 | Octave treble clefs no longer read (a bar number beside the clef was taken for the 8); beams accepted up to 1.8 times the typical beam height (blurred scans); a system that restates no key keeps the key in force; a doubtful system clef that disagrees with the page's clear treble clef is replaced by it |
| 2026-10-04 | Ledgers one interline from the staff are no longer discarded when note heads skew the page's ledger statistics (scans); short slanted beams with a ragged border accepted (blurred scans); a page no longer fails when a beamed head has no stem on one side |
| 2026-10-07 | A key signature whose signs were all lost (a flat joined to the clef, a half-flat not recognised) is read again with each sign cropped to the pitch the clef gives it, kept only when its first sign reads clearly and at least two signs are found; a half-flat there is recognised by its slash on the page; a weak rest that matches the page's confident rests in size and height on the staff is kept |
| 2026-10-07 (2) | A half-flat in a bar is linked to the note at its own pitch, looked for from its bowl as well as from half its height (a step off only beside a ledger note); a glyph the classifier reads confidently as a rest or a natural is not taken for a half-flat |

Files modified or added (all under `app/`):

- `app/src/main/java/org/audiveris/omr/WellKnowns.java`
- `app/src/main/java/org/audiveris/omr/glyph/Shape.java`
- `app/src/main/java/org/audiveris/omr/glyph/ShapeSet.java`
- `app/src/main/java/org/audiveris/omr/score/MusicXML.java`
- `app/src/main/java/org/audiveris/omr/score/PartwiseBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/beam/BeamsBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/clef/ClefBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/header/HeadersStep.java`
- `app/src/main/java/org/audiveris/omr/sheet/SheetStub.java`
- `app/src/main/java/org/audiveris/omr/sheet/key/KeyBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/key/KeyExtractor.java`
- `app/src/main/java/org/audiveris/omr/sheet/ledger/LedgersPostAnalysis.java`
- `app/src/main/java/org/audiveris/omr/sheet/stem/BeamLinker.java`
- `app/src/main/java/org/audiveris/omr/sheet/symbol/InterFactory.java`
- `app/src/main/java/org/audiveris/omr/sheet/symbol/SymbolsBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/symbol/SymbolsStep.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/AbstractPitchedInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/AlterInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/HeadInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/KeyAlterInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/KeyInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/SmallChordInter.java`
- `app/src/main/java/org/audiveris/omr/text/tesseract/TesseractOrder.java`
- `app/src/test/java/org/audiveris/omr/score/MusicXMLTest.java`
- `app/src/test/java/org/audiveris/omr/sheet/symbol/SymbolsBuilderTest.java`
- `app/src/test/java/org/audiveris/omr/sheet/symbol/SymbolsStepTest.java`
- `app/src/test/java/org/audiveris/omr/sig/inter/AlterLinkPointTest.java`
- `app/src/test/java/org/audiveris/omr/sig/inter/ArabicKeyShapeTest.java`

Everything else is Audiveris as published upstream at the commit above.

## Building

Exactly as Audiveris: with a Java 25 JDK,

    ./gradlew :app:installDist

The runnable distribution is then in `app/build/install/app/`. For text
recognition Audiveris uses Tesseract language data, which Nawtara takes
from https://github.com/tesseract-ocr/tessdata (Apache 2.0), commit
`ced78752cc61322fb554c280d13360b35b8684e4`: `ara.traineddata` and
`eng.traineddata`.
