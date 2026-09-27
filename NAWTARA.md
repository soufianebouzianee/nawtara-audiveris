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

Files modified or added (all under `app/`):

- `app/src/main/java/org/audiveris/omr/WellKnowns.java`
- `app/src/main/java/org/audiveris/omr/glyph/Shape.java`
- `app/src/main/java/org/audiveris/omr/glyph/ShapeSet.java`
- `app/src/main/java/org/audiveris/omr/score/MusicXML.java`
- `app/src/main/java/org/audiveris/omr/score/PartwiseBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/SheetStub.java`
- `app/src/main/java/org/audiveris/omr/sheet/key/KeyBuilder.java`
- `app/src/main/java/org/audiveris/omr/sheet/key/KeyExtractor.java`
- `app/src/main/java/org/audiveris/omr/sheet/symbol/InterFactory.java`
- `app/src/main/java/org/audiveris/omr/sheet/symbol/SymbolsBuilder.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/AbstractPitchedInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/AlterInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/HeadInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/KeyAlterInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/KeyInter.java`
- `app/src/main/java/org/audiveris/omr/sig/inter/SmallChordInter.java`
- `app/src/main/java/org/audiveris/omr/text/tesseract/TesseractOrder.java`
- `app/src/test/java/org/audiveris/omr/score/MusicXMLTest.java`
- `app/src/test/java/org/audiveris/omr/sheet/symbol/SymbolsBuilderTest.java`
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
