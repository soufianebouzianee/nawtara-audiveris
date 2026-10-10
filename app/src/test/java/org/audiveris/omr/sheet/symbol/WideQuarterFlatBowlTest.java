package org.audiveris.omr.sheet.symbol;
import ij.process.ByteProcessor;
import org.junit.Test;
import static org.junit.Assert.*;
public class WideQuarterFlatBowlTest {
    private ByteProcessor sign(boolean slash, boolean closed) {
        ByteProcessor b=new ByteProcessor(44,60);b.setValue(255);b.fill();
        for(int x=21;x<=24;x++) for(int y=0;y<60;y++)b.set(x,y,0);
        if(slash)for(int x=0;x<=38;x++){int y=38-x*3/4;for(int d=-2;d<=2;d++)b.set(x,y+d,0);}
        for(int y=33;y<=55;y++)for(int x=24;x<=39;x++)
            if(y==33||y==55||x==39)b.set(x,y,0);
        if(!closed)b.set(39,44,255);
        return b;
    }
    @Test public void pitchComesFromLowerBowlNotTallSlash(){
        Double y=SymbolsBuilder.wideQuarterFlatBowlY(sign(true,true),20);
        assertNotNull(y);assertEquals(44,y,0.01);
    }
    @Test public void ordinaryFlatDoesNotQualify(){assertNull(SymbolsBuilder.wideQuarterFlatBowlY(sign(false,true),20));}
    @Test public void openBowlHasNoIndependentPitch(){assertNull(SymbolsBuilder.wideQuarterFlatBowlY(sign(true,false),20));}
    @Test public void secondStemIsRefused(){ByteProcessor b=sign(true,true);for(int y=0;y<60;y++)b.set(40,y,0);assertNull(SymbolsBuilder.wideQuarterFlatBowlY(b,20));}
    @Test public void existingNarrowSignsAndInvalidScaleStayOut(){assertNull(SymbolsBuilder.wideQuarterFlatBowlY(sign(true,true),25));assertNull(SymbolsBuilder.wideQuarterFlatBowlY(sign(true,true),0));assertNull(SymbolsBuilder.wideQuarterFlatBowlY(null,20));}
    @Test public void originalInkMustConfirmStemAndSlash(){
        ByteProcessor sign=sign(true,true);
        assertTrue(SymbolsBuilder.wideQuarterFlatOnPage(sign,new java.awt.Rectangle(0,0,44,60),sign,20));
        ByteProcessor blank=new ByteProcessor(44,60);blank.setValue(255);blank.fill();
        assertFalse(SymbolsBuilder.wideQuarterFlatOnPage(blank,new java.awt.Rectangle(0,0,44,60),sign,20));
        assertFalse(SymbolsBuilder.wideQuarterFlatOnPage(sign(false,true),new java.awt.Rectangle(0,0,44,60),sign,20));
    }
    @Test public void originalFrameAndBoundsMustBeValid(){
        ByteProcessor sign=sign(true,true);
        assertFalse(SymbolsBuilder.wideQuarterFlatOnPage(null,new java.awt.Rectangle(0,0,44,60),sign,20));
        assertFalse(SymbolsBuilder.wideQuarterFlatOnPage(sign,new java.awt.Rectangle(-1,0,44,60),sign,20));
        assertFalse(SymbolsBuilder.wideQuarterFlatOnPage(sign,new java.awt.Rectangle(0,0,45,60),sign,20));
    }
}
