package org.audiveris.omr.sheet.symbol;

import ij.process.ByteProcessor;
import org.audiveris.omr.glyph.Shape;
import org.junit.Test;
import static org.junit.Assert.*;

/** CC0: original Leland flag fixture, no corpus pixels. */
public class StackedFlagTemplateTest
{
    private static final String UP =
            "iVBORw0KGgoAAAANSUhEUgAAABUAAABSCAAAAACeIcCeAAAA2klEQVR4nK2UUQ7DIAxD493/zm8a0BJCWoFYvuDJclIHVYbN9UmY" +
            "bVLtOGinmw4ns8TizUG5VrmDcl/tzCtPybCMfsU7cN007gLybgziexd43CfDYbcLOvb5ErrJYS0mSTtmSSpmRpy3Fs9Jak5SLTOS" +
            "JIuocZdZqRGHJE1xSlXetFe5La0mOZqRTDZWTnWf6ICemZz0E7Ph8mVSl248JGkjbpMx4pCv9ddXanjX8aVaf5NRjc+B+7SQGRta" +
            "q2KilnWHf1COHWqd0LC3Xpx2+31doNOf69WhxPMFSuU3njGwz6IAAAAASUVORK5CYII=";
    private static final String DOWN =
            "iVBORw0KGgoAAAANSUhEUgAAABYAAABKCAAAAACak/trAAAA20lEQVR4nLWUURKEIAxDkw73v3J3FARaoquzbn+UZwZKWktHDsJh" +
            "ggICY/siMe5jP1fz173/ibnlbtIp3NyEErM9TFGgLOSdW/qEGatTEkTtmxJz8KaJee1S5iN74/mMp3a0hXJkEqUYR7osgyeZSQqT" +
            "FCYpbFA26rI6XvPuPsw5lQjh48hEuak9272H8QVjTWkxcL8rZpzmhyXKujrxm+u+8Oj3ZKwttM+r9qLmCQ9ag4eVuQycWDRmoYj/" +
            "jmiNL37jid81bvod8DLbY38HvNKLwSHE52olfjirPicmPYz2X3oQAAAAAElFTkSuQmCC";
    private static final String SIXTEENTH =
            "iVBORw0KGgoAAAANSUhEUgAAABUAAABDCAAAAABWqxGgAAAAqUlEQVR4nMWSwQ7DMAhDcbX//+U3bWsbQrw1UQ/LqTxZBkwVxPg2" +
            "w+Ir1YqDVrppZTItaENeq3mHKOJTK+8g76vrbjCKX1oGrA/UXtL50quPW9DhM3X8FiRxSp2Gc75UByVTlSQZuqlhzSXJTtP0al/k" +
            "6ii6JEPZl5xB60YR17vJ0limcltw7YD35Xe3LUYxd7ag3i2//1Mqtdfs/odbMzzMBNMO7yy99gnTMB+MdXmDswAAAABJRU5ErkJg" +
            "gg==";
    private static final String DAMAGED =
            "iVBORw0KGgoAAAANSUhEUgAAABQAAABPCAAAAADOq7rlAAAAx0lEQVR4nLWUSw7CQAxDbYv7X9mIQpnJB5i2IosunpzESUYljBKq" +
            "CBC4rGxCaPL1QQm2Sq6mI0uFhgoNFRqqz42SVG6oYBcqAoXq8ckX4Qu9RB7KIeYOWajyiAz7dLu6UWBenaMy1GVcnfG/4FZ97+0x" +
            "5mjLdnXZ3nw4NKG3Jm+JiQqVOrw6r786H3xg1OSE0wupWnWnUZiFuftElVOPQTbm/SvdbU1/bSQUqU+b99Pvxdl1BXpF2R0u/FjO" +
            "W7pVQ4vp2/Ja5R0dsSmaCS45YwAAAABJRU5ErkJggg==";
    private static final String SLUR =
            "iVBORw0KGgoAAAANSUhEUgAAABcAAABQCAAAAADXHLGoAAAApklEQVR4nO2VQQrFIAxEZ3r/O0+J/WoiFpKF0MXPQuVlMhERpQCA" +
            "NrRVj6uNUk9GDqyJzhFcHIdCweQICcctuONy66DXLFh8RkSuUZDTYxQk9Rbccf3mtI8eo0LfIldrcM4frcFB/zIXwC/tx+LP6+dA" +
            "QB86N9rNOufPdnGP+fN5m7J6vvur0Jf9Rczphzynn/LAt/+X2/vCnYvnAU8efs3JDfv/6wZoWh2eEUGu5AAAAABJRU5ErkJggg==";
    private static final String TIE =
            "iVBORw0KGgoAAAANSUhEUgAAABcAAABQCAAAAADXHLGoAAAAn0lEQVR4nO2VwQ7DIAxD7f3/P3uCAnXokLJVSD2MS6uH80AcEgp1" +
            "EWh/x3q1r+rWlWPaGBxBYxzyAuPwDeewIwLXZw/MNHnGmrh6QTKPXpDNoxWk80uuKrrvWXKVAzb6UQ7Y6f+aC3zUffDnP70PoSe9" +
            "J6Gdfpb+t8/P2l5ve3h06Wyerakn8z2ezI945It55BPG+WkJ3LHxMDZPPg3aNxciHJ/3P2R0AAAAAElFTkSuQmCC";
    private static final String HAIRPIN =
            "iVBORw0KGgoAAAANSUhEUgAAABcAAABQCAAAAADXHLGoAAAAyUlEQVR4nJWVSQ4EIQwD7f7/nzPqdQI4CxxDVYIEMgQMYh0Ag7pc" +
            "h2kh5KGFmIcUEh5KuHiL51LWbdnI5kIIBY9ZqHhMQsljFBxvfqPug0Ho8HBCi8df6PH4hJm3eC5l3e6N5ly8Qp/HJWzwOIUdHhbU" +
            "uXVOELANnufgPk99Tt531u3D54qbPN8X0eP5PaAWT/3e6BKn0Yc+oGqeQ56VPMf4c7lnwXkg6pxDNOe5ZG7Kc43ojBf483+tQR/3" +
            "kXjInxmhPs78/1rXD+zcK51CryO2AAAAAElFTkSuQmCC";
    private static final String LEDGERS =
            "iVBORw0KGgoAAAANSUhEUgAAABcAAABQCAAAAADXHLGoAAAAOElEQVR4nGP8zwADjAwMCA4TA3YwKo4/HKgFGEfjZUilN2oBxtF4" +
            "H5Txy8RAWzAa7xAw2OKX1uIAXIgIh6ExPFQAAAAASUVORK5CYII=";
    private static final String SHARP =
            "iVBORw0KGgoAAAANSUhEUgAAABcAAABQCAAAAADXHLGoAAAAbklEQVR4nO2SQQrAMAgEd4P///KWmECTam6FHuqcZJA1aCg4xCxm" +
            "1ZBTflB76BCGCM/7ocVWx1KL1XNtabmWJRIaOdyka1ho5Zi7xyJ5D267eiF5px6j0bpR0D1HQXpOqj/6h6y7//LuKD95az8XtoQd" +
            "yRRSrdsAAAAASUVORK5CYII=";
    private static final String NATURAL =
            "iVBORw0KGgoAAAANSUhEUgAAABcAAABQCAAAAADXHLGoAAAAXUlEQVR4nO2TUQqAMAxDE/H+V464UtAZFPc31vczFkYDaUYBIHAe" +
            "V7bunpQeVA7vOewPha1knU77nm4O7XwO7kUyvnK+uH2N1DXom5QeTNUHtfF/+qAv36T0YLUcDkNND7qWRhw1AAAAAElFTkSuQmCC";

    private static ByteProcessor ink (String png) throws Exception
    {
        return new ByteProcessor(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(
                java.util.Base64.getDecoder().decode(png))));
    }

    @Test public void intactUpAndDownStacksEarnLocalSupport () throws Exception
    {
        assertEquals(Shape.FLAG_3, StackedFlagTemplate.match(ink(UP), 21));
        assertEquals(Shape.FLAG_3_DOWN, StackedFlagTemplate.match(ink(DOWN), 21));
    }

    @Test public void incompleteOrDifferentFlagsDoNotEarnSupport () throws Exception
    {
        assertNull(StackedFlagTemplate.match(ink(SIXTEENTH), 21));
        assertNull(StackedFlagTemplate.match(ink(DAMAGED), 21));
        assertNull(StackedFlagTemplate.match(ink(UP), 10));
        assertNull(StackedFlagTemplate.match(null, 21));
    }

    @Test public void curvesLinesAndAccidentalsDoNotMatchAStack () throws Exception
    {
        for (String negative : new String[] {SLUR, TIE, HAIRPIN, LEDGERS, SHARP, NATURAL}) {
            assertNull(StackedFlagTemplate.match(ink(negative), 21));
        }
    }
}
