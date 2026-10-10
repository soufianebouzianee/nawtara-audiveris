package org.audiveris.omr.sheet.beam;

import java.awt.geom.Line2D;
import org.audiveris.omr.math.LineUtil;

/** Complete only a short end hook beside an independently observed full beam. */
final class BeamHookBorders
{
    private BeamHookBorders () {}

    static Line2D missingBorder (Line2D top, Line2D bottom, Line2D hook,
                                boolean missingTop, double height, double minHookWidth,
                                double maxHookWidth, double minBeamWidth, double corner, double slopeGap,
                                double minHeight, double maxHeight)
    {
        if (top == null || bottom == null || hook == null || height <= 0) { return null; }
        final double left = Math.max(top.getX1(), bottom.getX1());
        final double right = Math.min(top.getX2(), bottom.getX2());
        final double width = right - left;
        final double hookWidth = hook.getX2() - hook.getX1();
        if (width <= maxHookWidth || hookWidth < minHookWidth || hookWidth > maxHookWidth
                || hookWidth > 0.75 * width || hook.getX1() < left - corner
                || hook.getX2() > right + corner
                || (Math.abs(hook.getX1() - left) > corner
                    && Math.abs(hook.getX2() - right) > corner)) { return null; }
        // The later side adjustment must not extend the short hook across the full beam.
        final double leftGap = hook.getX1() - left, rightGap = right - hook.getX2();
        if ((leftGap > corner && leftGap < minBeamWidth)
                || (rightGap > corner && rightGap < minBeamWidth)) { return null; }
        final double slope = LineUtil.getSlope(top);
        if (Math.abs(LineUtil.getSlope(bottom) - slope) > slopeGap
                || Math.abs(LineUtil.getSlope(hook) - slope) > slopeGap) { return null; }
        for (double x : new double[] {hook.getX1(), hook.getX2()}) {
            final double topY = LineUtil.yAtX(top, x);
            final double bottomY = LineUtil.yAtX(bottom, x);
            final double beamHeight = bottomY - topY;
            final double separation = missingTop ? LineUtil.yAtX(hook, x) - bottomY
                    : topY - LineUtil.yAtX(hook, x);
            if (beamHeight < minHeight || beamHeight > maxHeight
                    || separation < height - corner || separation > 2 * height) { return null; }
        }
        final double dy = missingTop ? -height : height;
        return new Line2D.Double(hook.getX1(), hook.getY1() + dy,
                                hook.getX2(), hook.getY2() + dy);
    }
}
