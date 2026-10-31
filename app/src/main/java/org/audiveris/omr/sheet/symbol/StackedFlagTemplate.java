//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             S t a c k e d F l a g T e m p l a t e                              //
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

import ij.process.ByteProcessor;
import java.awt.image.BufferedImage;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;

/** Whole-outline evidence for the distinctive Leland three-flag stack. */
final class StackedFlagTemplate
{
    private StackedFlagTemplate () {}

    static Shape match (ByteProcessor ink, int interline)
    {
        if (ink == null || interline <= 0 || ink.getWidth() < .8 * interline
                || ink.getWidth() > 1.4 * interline || ink.getHeight() < 3.5 * interline
                || ink.getHeight() > 4.4 * interline) {
            return null;
        }
        for (Shape shape : new Shape[] {Shape.FLAG_3, Shape.FLAG_3_DOWN}) {
            if (matches(ink, shape)) { return shape; }
        }
        return null;
    }

    private static boolean matches (ByteProcessor ink, Shape shape)
    {
        final BufferedImage image = MusicFont.getBaseFont(MusicFamily.Leland, 21)
                .buildImage(shape, false);
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) >= 128) {
                    left = Math.min(left, x); right = Math.max(right, x);
                    top = Math.min(top, y); bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left) { return false; }
        int actual = 0, expected = 0, shared = 0;
        for (int y = 0; y < ink.getHeight(); y++) {
            for (int x = 0; x < ink.getWidth(); x++) {
                final int xx = left + Math.min(right - left,
                        (int) ((x + .5) * (right - left + 1) / ink.getWidth()));
                final int yy = top + Math.min(bottom - top,
                        (int) ((y + .5) * (bottom - top + 1) / ink.getHeight()));
                final boolean a = ink.get(x, y) == 0;
                final boolean b = (image.getRGB(xx, yy) >>> 24) >= 128;
                if (a) { actual++; }
                if (b) { expected++; }
                if (a && b) { shared++; }
            }
        }
        // Both the three lobes and their white counters must agree. No partial
        // template, isolated arc, stem strip, or broad accidental earns support.
        return shared >= .8 * actual && shared >= .8 * expected
                && 2.0 * shared >= .84 * (actual + expected);
    }
}
