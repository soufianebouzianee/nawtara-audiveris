//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              M u l t i p l e R e s t S e r i f s                               //
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
package org.audiveris.omr.sheet.beam;

import ij.process.ByteProcessor;
import java.awt.geom.Line2D;

/** Distinguish a beam's one-sided end stems from centered multiple-rest serifs. */
final class MultipleRestSerifs
{
    private MultipleRestSerifs () {}

    static boolean hasEndStems (ByteProcessor page, Line2D beam, double height,
                                int interline, int stemWidth)
    {
        if (page == null || beam == null || interline <= 0 || stemWidth <= 0) {
            return false;
        }
        for (int direction : new int[] {-1, 1}) {
            if (endStem(page, beam.getX1(), beam.getY1(), height, interline, stemWidth, direction)
                    && endStem(page, beam.getX2(), beam.getY2(), height, interline, stemWidth, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean endStem (ByteProcessor page, double x, double y, double height,
                                    int il, int width, int dir)
    {
        // Beyond the beam, require a long concrete stroke on one side and no
        // corresponding serif on the other. Staff removal may interrupt a stem.
        final int radius = (int) Math.ceil(width / 2.0) + 1;
        final int near = (int) Math.ceil(height / 2) + 2;
        final int far = near + (int) Math.ceil(1.5 * il);
        if (x - radius < 0 || x + radius >= page.getWidth()
                || y - far < 0 || y + far >= page.getHeight()) {
            return false;
        }
        int ink = 0, gap = 0, maxGap = 0;
        for (int d = near; d <= far; d++) {
            if (inkRow(page, x, y + dir * d, radius)) { ink++; gap = 0; }
            else { maxGap = Math.max(maxGap, ++gap); }
        }
        if (ink < 0.75 * (far - near + 1) || maxGap > 0.35 * il) { return false; }
        int run = 0, maxRun = 0;
        for (int d = near; d <= near + (int) Math.ceil(0.65 * il); d++) {
            if (inkRow(page, x, y - dir * d, radius)) { maxRun = Math.max(maxRun, ++run); }
            else { run = 0; }
        }
        return maxRun < 0.25 * il;
    }

    private static boolean inkRow (ByteProcessor page, double x, double y, int radius)
    {
        final int yy = (int) Math.rint(y);
        for (int xx = (int) Math.rint(x) - radius; xx <= (int) Math.rint(x) + radius; xx++) {
            if (page.get(xx, yy) == 0) { return true; }
        }
        return false;
    }
}
