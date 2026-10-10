package org.audiveris.omr.sheet.grid;
import ij.process.ByteProcessor;
import java.awt.Point;
import org.audiveris.omr.glyph.Glyph;
import org.junit.Test;
import static org.junit.Assert.*;

/** Synthetic sharp strokes and neighboring bars; no private page pixels. */
public class BarAccidentalInkTest {
 private ByteProcessor page(){var p=new ByteProcessor(240,240);p.setValue(255);p.fill();return p;}
 private void sharp(ByteProcessor p,int dx){
  for(int y=100;y<144;y++)for(int x:new int[]{104+dx,105+dx,112+dx,113+dx})p.set(x,y,0);
  for(int y:new int[]{109,110,111,130,131,132})for(int x=100+dx;x<119+dx;x++)p.set(x,y,0);
 }
 @Test public void shortSharpOwnsCandidateStrokeWithoutChangingPage(){
  var p=page();sharp(p,0);Glyph g=BarAccidentalInk.candidate(p,111,113,82,150,17);assertNotNull(g);assertTrue(g.contains(new Point(112,120)));assertFalse(g.contains(new Point(112,90)));assertEquals(255,p.get(112,90));
 }
 @Test public void fullBarEvidenceIsPreservedEvenBesideSharp(){
  var p=page();sharp(p,0);for(int y=82;y<=150;y++)p.set(112,y,0);assertNull(BarAccidentalInk.candidate(p,111,113,82,150,17));
 }
 @Test public void faintOrBrokenBarWithoutSharpCannotBeVetoed(){
  var p=page();for(int y=82;y<=150;y++)p.set(112,y,y%7==0?255:180);assertNull(BarAccidentalInk.candidate(p,111,113,82,150,17));
 }
 @Test public void nearbySignWithoutStrokeOwnershipAbstains(){
  var p=page();sharp(p,16);assertNull(BarAccidentalInk.candidate(p,111,113,82,150,17));
 }
 @Test public void clippedAndInvalidCropsAbstain(){
  var p=page();sharp(p,0);assertNull(BarAccidentalInk.candidate(p,0,2,82,150,17));assertNull(BarAccidentalInk.candidate(p,111,121,82,150,17));assertNull(BarAccidentalInk.candidate(p,111,113,82,100,17));assertNull(BarAccidentalInk.candidate(null,111,113,82,150,17));assertNull(BarAccidentalInk.candidate(p,111,113,82,150,0));assertNull(BarAccidentalInk.candidate(p,111,113,-2,66,17));
 }
 @Test public void smallDisconnectedCrossbarsAreNotJoinedIntoSign(){
  var p=page();for(int y:new int[]{110,130})for(int x=100;x<119;x++)p.set(x,y,0);assertNull(BarAccidentalInk.candidate(p,111,113,82,150,17));
 }
}
