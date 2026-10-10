package org.audiveris.omr.sheet.grid;
import ij.process.ByteProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;

/** Propose a short, original-ink accidental sharing a projected bar stroke. */
final class BarAccidentalInk
{
    private BarAccidentalInk () {}
    static Glyph candidate (ByteProcessor page, int start, int stop, int top, int bottom, int il)
    {
        if(page==null || il<=0 || start<0 || stop>=page.getWidth() || top<0
                || bottom>=page.getHeight() || start>stop || bottom-top<3*il
                || stop-start+1>.35*il)return null;
        int mid=(start+stop)/2;
        // A genuine dark bar has ink above the proposed short sign. Never veto that evidence.
        for(int y=top+il/3;y<top+2*il/3;y++)for(int x=start;x<=stop;x++)if(page.get(x,y)<128)return null;
        Rectangle crop=new Rectangle(mid-il,top-il/2,2*il+1,bottom-top+il+1);
        if(!new Rectangle(0,0,page.getWidth(),page.getHeight()).contains(crop))return null;
        ByteProcessor ink=new ByteProcessor(crop.width,crop.height);
        for(int y=0;y<crop.height;y++)for(int x=0;x<crop.width;x++)ink.set(x,y,page.get(crop.x+x,crop.y+y)<128?0:255);
        for(Glyph g:GlyphFactory.buildGlyphs(new RunTableFactory(Orientation.VERTICAL).createTable(ink),new Point(crop.x,crop.y))){
            Rectangle b=g.getBounds();
            if(b.x<=crop.x || b.y<=crop.y || b.getMaxX()>=crop.getMaxX() || b.getMaxY()>=crop.getMaxY()
                    || b.width<.7*il || b.width>1.6*il || b.height<1.8*il || b.height>3*il
                    || b.y<top+.7*il || b.getMaxY()>bottom+.5*il || mid<b.x || mid>=b.getMaxX())continue;
            int ownedRows=0;
            for(int y=b.y;y<b.getMaxY();y++){
                boolean found=false;for(int x=start;x<=stop;x++)if(g.contains(new Point(x,y)))found=true;
                if(found)ownedRows++;
            }
            if(ownedRows>=.6*b.height)return g;
        }
        return null;
    }
}
