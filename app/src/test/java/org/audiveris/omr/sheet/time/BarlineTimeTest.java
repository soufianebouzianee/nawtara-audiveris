//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a r l i n e T i m e T e s t                                     //
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
package org.audiveris.omr.sheet.time;

import ij.process.ByteProcessor;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original CC0 single/two-staff time changes and chord/tuplet negatives, on five paths. */
public class BarlineTimeTest
{
    @Test
    public void readAlignedPairsAndDeclineMusic () throws Exception
    {
        final StringBuilder failures = new StringBuilder();
        for (String[] sample : SAMPLES) {
            final ByteProcessor ink = new ByteProcessor(ImageIO.read(new ByteArrayInputStream(
                    Base64.getDecoder().decode(sample[3]))));
            final var pairs = BarlineTime.find(ink, new Point(107, 209),
                    Integer.parseInt(sample[1]));
            final int expected = Integer.parseInt(sample[2]);
            if (expected < 0) {
                if (!pairs.isEmpty()) failures.append(sample[0] + " invented " + pairs + "\n");
            } else {
                if (pairs.size() != 1 || pairs.get(0).top().getValue() != expected
                        || pairs.get(0).bottom().getValue() != 8)
                    failures.append(sample[0] + " expected " + expected + "/8 got " + pairs + "\n");
            }
        }
        assertEquals("", failures.toString());
    }

    @Test
    public void rejectMissingAndMisalignedHalves () throws Exception
    {
        final ByteProcessor source = new ByteProcessor(ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(SAMPLES[0][3]))));
        final int il = Integer.parseInt(SAMPLES[0][1]);
        assertEquals(1, BarlineTime.find(source, new Point(), il).size());
        final ByteProcessor missing = (ByteProcessor) source.duplicate();
        missing.setValue(255);
        missing.setRoi(0, 2 * il, missing.getWidth(), missing.getHeight() - 2 * il);
        missing.fill();
        missing.resetRoi();
        assertTrue(BarlineTime.find(missing, new Point(), il).isEmpty());

        final ByteProcessor shifted = (ByteProcessor) missing.duplicate();
        for (int y = 2 * il; y < source.getHeight(); y++) {
            for (int x = 0; x + il < source.getWidth(); x++) {
                shifted.set(x + il, y, source.get(x, y));
            }
        }
        assertTrue(BarlineTime.find(shifted, new Point(), il).isEmpty());
    }

    private static final String[][] SAMPLES = {
        {"cc0-gs-pdfwrite-input-s1-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHUlEQVR4Xu2R4W7DQAiD9/4v3alJ4MAYJyflMlXr96eHDfa6/rxm" +
            "+HHQeUWTLlRFIYI+tUkFuWmP4qMgsCyWE/P5Cs4KayExh8enQ8ujYtSUFPwabAlPBFZSQ8Zv1MybFgeJXdeM+h3Y1i1NRWRrl5u2" +
            "O09Y2LTfeUL+vxSR7c008YSbm44rlsBEIs00HR+YwEQiXWyyG5bARCItbwqavwR+QQKoOKTJpvelfWIAFYc022RrJICKQ/o2vZ5s" +
            "OiABVBzSt2lAAqg4pOeawtp4XoAlMJFIn9U01Ko81xS3wvscT8hnRWRrtzSVKpw3LQ6nWMLfNZnRjLuWpjOsZ2WTNQDok8E38thh" +
            "yUhdSa8Izg2WjIidaG02ChxIcfql5GwuCpwUcglM+M9NDMw+iTqxe7BmA5ci0hTE9PFWacoTlODzKmEJWCzTIr0j4F+AaYHeEdCi" +
            "lU14S0WnNQRWhLdUdFpD4E1TP1RrKJoqIgVaQzGa4nlVEq2hCE0hWRfd0GThaSD0jiIV7fn+6BCWItUEcC+gPAVW7OBWRJoKLNnA" +
            "pYg0O7AggctGaygwPIHLRmsoMDyBy0ZrKGJkrnG50hqKHJmL2sDWENTItU1EIsag0xU0cEVTE7isCdUVTWdVqB90uqIJfLwJZaM1" +
            "BDzyUFF2WkNBM6kY6B1F/ftN6fN6R+LB2/2YRJywJCE8gmsB5Umw4w3uJLQrmer5NgGTTb+XJFwSaRPgywAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s1-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACWElEQVR4Xu2R227jQAxD8/8/3YUzF1EcimO3doAWe16qISUySV9f" +
            "l3hNaqfY4PeGMuf3NpmcocJK3uELS52SNL10YxM9eetKUwTkjGnw+7mm9efDLxrjDrhPESJ4EQ4txh1xniNub3rfqYj0EViBrRg9" +
            "7U5GICzBWoyediYjEJZgLUZLP1MRN/96/UpFYBMLuAWzYVzJjEXldxNhNowrmbGo/G4izDXzSGakjT6vS3xRMK50SCZ2vtM09nRI" +
            "olqpLzKnmsIVK/JC8BebOlVMw3m3Nq0OvtSFwTQp+ZEmJSaJzQ1lk5CeaVqVrsIjxjMUTVKk+luaQkWD1u5vGh6M7R3jGSiNxQXc" +
            "gvkEOqMJ4QW4BfMJdEZ/hjnBLZhPoDPGM9wOLN3RBM+wG7F0oYlDGtORW3B+vilHBNMTe3B+OPlZggnI9NbFOG4GvSsiIDO8ZTOE" +
            "obNQAOElaTOfHzoLBSmyIG3m80NnoSBFFqTNfH7oLJwntcjwxMau4Zo3vIRY04DpMbs05zk4eV9lLIfIFVKidhzyGwgJqR2HKnq0" +
            "6VJVaThG0QebrvyjSsMRTel8VZDScEAT3i9CojQsWDUTfNEdTSMdZ0HtOHJRKxh/K4zlyDUB7wHOc3BFg7cQazq45A0vIdYs4QaE" +
            "dwelYeF0hHcHpWHhdIR3B6VhgcxcM1RBaTgo8ummRfhIU+rKxqTSLSrwkSad97mmWcV6p9ItOvAPN7E8KA2HjOxFLE9Kw6IyN0Xf" +
            "bFpju/BYU0+Ol4kzlgXCEV4DnOfhkpft+UnT0sU2sfMd/5v+Ac3Kn85tk3jiAAAAAElFTkSuQmCC"},
        {"cc0-gs-pdfwrite-input-s1-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8ElEQVR4Xu2TW27DQAwDff9Lu6jh9UqUSK2dKgGKzE8SkeYgfWz7" +
            "czYLhoG6wfl3JqdZcVU5wwgWVUXMcOtrKp0ycHtFJUMGij5gUmsqo8TheAmIiHLNvs+UnPgeTzjJao9priY/Pb7HE8pczUx0kAaC" +
            "uDovX1NFnDUXOkgDAe7az32mCLYHNBDgtgfbAxoIcNuD7QENSty6ecvgyQJzu9s06TT5B9tMYbXLFGc/YDItQESca3c8HQ4JIqLM" +
            "3WByPY/KGJ8w4cW2EBkSgmlF9MiE/05Lomemc9t9qEQPTfaPYoAVpCwQ0FPv1A3CTc8Lpt3IMEhZa2WYb7Tkq3IOiqqlKuegqFqq" +
            "cg6KqqUqZ6DmAEsOnVLs+nwvx2TIweUFlco4yW5y8oiIk36D5OQQEScTtZruqXjCGaI3mm79ongiyFXx4uCJYJrs4+Hg4YnAmOZ0" +
            "IfoD01i37zNEJPCmQzBeKSoTeM8F1iwyFKDjAEsOnQrQ8gt2HDoloMGC3QueCHDdgt0Lnghw3YLdC54I7Kb3dJrQZXsOnnDiZq8p" +
            "OcW7gQaCdLHDRBbbTHjtMBUqPA9oICCLbSZ8Mr9OeMLJN/PrhCeCbDS7OUQkiLPnRcyJSDB2z2X4mKMywdx2YM0iQwVKNu15wRRc" +
            "GCNlgfM17T/q3rbTWGVZhAAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s2-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB3klEQVR4Xu2T0Y7CQAwD+f+fvpO2pZt4bSctcCdVzBOJsx6BxOPn" +
            "Ko8FvMgUsQE9NzAd7V2XDTW5uaVymQFqOyoTGbD386b159N9OjGsxctiRSeG2fuHpv09zgSdaGbt0YwzQSeaWXs040zQiWH2/qEJ" +
            "F/kwYiIDNuPMMJEBm3FmmMgTazui66bAFLk2lzVpeV41TcnXdJ6WYqd1pFg9xiqDmkWz7fJiIoMS4kl/Z0QGFbSULndkUMA76XJH" +
            "J5bx0y296yagE8dTlG3EHdCJI5me9eEjQyeGrEng6cREmmdplgzwdGIizVGJns+ZFlW+S7hMETpB5NpcpoidbdElk1SlI8SnglTb" +
            "FF035an0vGQ6vktPVeWUO5o2vqYXTCdF7zNhvFBfKP7LhOlK44RzVvQmE4aM1hHjrOgtpm2GfKE8EATRqHgKNVWuWEWVqoglRFRU" +
            "FbGCiYqqIlYwUVFVxAIqKqqKWBDLj89FVRFTZvcDBkcRU1J5GhxFTEnlaXAUMSUV38OUez9ogtrPmwz4YkOsNVjLwDcDvjVgKwPf" +
            "DPjWgbUEfDLg2xNUgoMirvmaXuFWpqAA8HJDrGuwPoK3A75tgO0RvB3wbQNsj+DtgG8bYHsEbwd82wDbI3g7+AVljbnQm+hOjwAA" +
            "AABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s2-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAABsElEQVR4Xu2T0W4CMQwE+f+fpqppL87a3nW4SysQ80S89o5A4nY/" +
            "wc2DISIXGG9nmjQNl4hrfsvbLhoSfG9PxTLCXNtSkYiAtdtNye/nt2ZIRAjFDVWdEGJvnATqhBB74yRQJ4zQu90UBqSORITQu830" +
            "qJ5fu0ye4WFtLFM4gxadMaGGi640YQ6onIAmA5cGJFKgxMClAYkUKDFwaUCiNn9n6v2j6mQFZyoLy0DiLzeasHWbKbRuNx3Xr2+K" +
            "tWNSFpYBJdRq0UlTGLy8Cd9U9KTJqueXEj1r8t9qeGgZDRmu/wB3JnhKWdHcT5m+aXtOmdz36chUTviYXsKEItWk8ppF0XUmjAN6" +
            "o+K/TJhGGis5q6KLTBhmtJYyVkWXmH7esICovMKJrOIwloi4JBGJKhGXJCJRJeKKTCSqRFyRiUSViAtSkagScYEvPz6LKhGnjO6P" +
            "aZ25+C1M0LvdBM+NJgIePCjGBOxNwBMjnxKwNQNvjHzKwNoEPDHy6QpCcCDiBh/TGd7KNBQALj4oxhqs9+CukU8bYLsHd4182gDb" +
            "Pbhr5NMG2O7BXSOfNsB2D+4aXyWb3ayeqEW+AAAAAElFTkSuQmCC"},
        {"cc0-gs-pdfwrite-input-s2-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACEUlEQVR4Xu2S0W4bSQwE9f8/rSCKdoZssourrNcGgtRLJNaoC8bd" +
            "43mBR0Z1ZtDMP1iS0JBiS8TAmRRKJK/fWNI/Y0yRI/oQpUAROnx8hzlQhA6vkt/zhtDdHfJ73gBldx/8njdA3a2XgjdA3S2HCihP" +
            "s1svCijLCqUf63cFpaEvTXz0+E0pnUqeeaPEUqlaxgcNYX1//PMdQGmQ+Yw+Xnjj0fGEPl5449HxhD5eeONphsuh4o1nz65f10vB" +
            "G8+ebUp20Aqgm21OghXAXv2+Et8yVgDdanfLWEE0s81JsIJoZutF8YYou+vg97xBdPcHSvFRBhQiy3Por0tp+/iMYyiRvX4qdKEU" +
            "/zc4EbpUSil1hfkFcjbzvFKKf9CZ3OQ9GpqWJu/R0LQ0eY+GpqXJOzTzQh8l2Fri+v6MYyg9unwiRc7T7DanDChP+xc0pwQoTxe6" +
            "tfRZyhvPEfrG0kf/obwB+lS9JLwBdin+vBwy3gChtKeH0BeUjvX4uQMUkEuvwPGvhRyQOwt9FkEJaOOFPkqwBbTyG32TYGvQQkTf" +
            "LrwBdD2ibxfeALoe0bcLb4C4mTt3lrQV3yW88dTNe0vNqd4DVgDt4h0ls3hbSa93lIaUng+sAMzibSX9ZX/deOPpN/vrxhugG+1u" +
            "CVBAnX1fYA4UcOy+l+VrDzlgbyf0WQQloZEHdy6USku1Mj7w/C89fwFgRMfCVpZvFwAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s3-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHUlEQVR4Xu2R4W7DQAiD9/4v3alJ4MAYJyflMlXr96eHDfa6/rxm" +
            "+HHQeUWTLlRFIYI+tUkFuWmP4qMgsCyWE/P5Cs4KayExh8enQ8ujYtSUFPwabAlPBFZSQ8Zv1MybFgeJXdeM+h3Y1i1NRWRrl5u2" +
            "O09Y2LTfeUL+vxSR7c008YSbm44rlsBEIs00HR+YwEQiXWyyG5bARCItbwqavwR+QQKoOKTJpvelfWIAFYc022RrJICKQ/o2vZ5s" +
            "OiABVBzSt2lAAqg4pOeawtp4XoAlMJFIn9U01Ko81xS3wvscT8hnRWRrtzSVKpw3LQ6nWMLfNZnRjLuWpjOsZ2WTNQDok8E38thh" +
            "yUhdSa8Izg2WjIidaG02ChxIcfql5GwuCpwUcglM+M9NDMw+iTqxe7BmA5ci0hTE9PFWacoTlODzKmEJWCzTIr0j4F+AaYHeEdCi" +
            "lU14S0WnNQRWhLdUdFpD4E1TP1RrKJoqIgVaQzGa4nlVEq2hCE0hWRfd0GThaSD0jiIV7fn+6BCWItUEcC+gPAVW7OBWRJoKLNnA" +
            "pYg0O7AggctGaygwPIHLRmsoMDyBy0ZrKGJkrnG50hqKHJmL2sDWENTItU1EIsag0xU0cEVTE7isCdUVTWdVqB90uqIJfLwJZaM1" +
            "BDzyUFF2WkNBM6kY6B1F/ftN6fN6R+LB2/2YRJywJCE8gmsB5Umw4w3uJLQrmer5NgGTTb+XJFwSaRPgywAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s3-sys1-m2-st2", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHUlEQVR4Xu2R4W7DQAiD9/4v3alJ4MAYJyflMlXr96eHDfa6/rxm" +
            "+HHQeUWTLlRFIYI+tUkFuWmP4qMgsCyWE/P5Cs4KayExh8enQ8ujYtSUFPwabAlPBFZSQ8Zv1MybFgeJXdeM+h3Y1i1NRWRrl5u2" +
            "O09Y2LTfeUL+vxSR7c008YSbm44rlsBEIs00HR+YwEQiXWyyG5bARCItbwqavwR+QQKoOKTJpvelfWIAFYc022RrJICKQ/o2vZ5s" +
            "OiABVBzSt2lAAqg4pOeawtp4XoAlMJFIn9U01Ko81xS3wvscT8hnRWRrtzSVKpw3LQ6nWMLfNZnRjLuWpjOsZ2WTNQDok8E38thh" +
            "yUhdSa8Izg2WjIidaG02ChxIcfql5GwuCpwUcglM+M9NDMw+iTqxe7BmA5ci0hTE9PFWacoTlODzKmEJWCzTIr0j4F+AaYHeEdCi" +
            "lU14S0WnNQRWhLdUdFpD4E1TP1RrKJoqIgVaQzGa4nlVEq2hCE0hWRfd0GThaSD0jiIV7fn+6BCWItUEcC+gPAVW7OBWRJoKLNnA" +
            "pYg0O7AggctGaygwPIHLRmsoMDyBy0ZrKGJkrnG50hqKHJmL2sDWENTItU1EIsag0xU0cEVTE7isCdUVTWdVqB90uqIJfLwJZaM1" +
            "BDzyUFF2WkNBM6kY6B1F/ftN6fN6R+LB2/2YRJywJCE8gmsB5Umw4w3uJLQrmer5NgGTTb+XJFwSaRPgywAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s3-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACWElEQVR4Xu2R227jQAxD8/8/3YUzF1EcimO3doAWe16qISUySV9f" +
            "l3hNaqfY4PeGMuf3NpmcocJK3uELS52SNL10YxM9eetKUwTkjGnw+7mm9efDLxrjDrhPESJ4EQ4txh1xniNub3rfqYj0EViBrRg9" +
            "7U5GICzBWoyediYjEJZgLUZLP1MRN/96/UpFYBMLuAWzYVzJjEXldxNhNowrmbGo/G4izDXzSGakjT6vS3xRMK50SCZ2vtM09nRI" +
            "olqpLzKnmsIVK/JC8BebOlVMw3m3Nq0OvtSFwTQp+ZEmJSaJzQ1lk5CeaVqVrsIjxjMUTVKk+luaQkWD1u5vGh6M7R3jGSiNxQXc" +
            "gvkEOqMJ4QW4BfMJdEZ/hjnBLZhPoDPGM9wOLN3RBM+wG7F0oYlDGtORW3B+vilHBNMTe3B+OPlZggnI9NbFOG4GvSsiIDO8ZTOE" +
            "obNQAOElaTOfHzoLBSmyIG3m80NnoSBFFqTNfH7oLJwntcjwxMau4Zo3vIRY04DpMbs05zk4eV9lLIfIFVKidhzyGwgJqR2HKnq0" +
            "6VJVaThG0QebrvyjSsMRTel8VZDScEAT3i9CojQsWDUTfNEdTSMdZ0HtOHJRKxh/K4zlyDUB7wHOc3BFg7cQazq45A0vIdYs4QaE" +
            "dwelYeF0hHcHpWHhdIR3B6VhgcxcM1RBaTgo8ummRfhIU+rKxqTSLSrwkSad97mmWcV6p9ItOvAPN7E8KA2HjOxFLE9Kw6IyN0Xf" +
            "bFpju/BYU0+Ol4kzlgXCEV4DnOfhkpft+UnT0sU2sfMd/5v+Ac3Kn85tk3jiAAAAAElFTkSuQmCC"},
        {"cc0-gs-pdfwrite-input-s3-sys1-m3-st2", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACWElEQVR4Xu2R227jQAxD8/8/3YUzF1EcimO3doAWe16qISUySV9f" +
            "l3hNaqfY4PeGMuf3NpmcocJK3uELS52SNL10YxM9eetKUwTkjGnw+7mm9efDLxrjDrhPESJ4EQ4txh1xniNub3rfqYj0EViBrRg9" +
            "7U5GICzBWoyediYjEJZgLUZLP1MRN/96/UpFYBMLuAWzYVzJjEXldxNhNowrmbGo/G4izDXzSGakjT6vS3xRMK50SCZ2vtM09nRI" +
            "olqpLzKnmsIVK/JC8BebOlVMw3m3Nq0OvtSFwTQp+ZEmJSaJzQ1lk5CeaVqVrsIjxjMUTVKk+luaQkWD1u5vGh6M7R3jGSiNxQXc" +
            "gvkEOqMJ4QW4BfMJdEZ/hjnBLZhPoDPGM9wOLN3RBM+wG7F0oYlDGtORW3B+vilHBNMTe3B+OPlZggnI9NbFOG4GvSsiIDO8ZTOE" +
            "obNQAOElaTOfHzoLBSmyIG3m80NnoSBFFqTNfH7oLJwntcjwxMau4Zo3vIRY04DpMbs05zk4eV9lLIfIFVKidhzyGwgJqR2HKnq0" +
            "6VJVaThG0QebrvyjSsMRTel8VZDScEAT3i9CojQsWDUTfNEdTSMdZ0HtOHJRKxh/K4zlyDUB7wHOc3BFg7cQazq45A0vIdYs4QaE" +
            "dwelYeF0hHcHpWHhdIR3B6VhgcxcM1RBaTgo8ummRfhIU+rKxqTSLSrwkSad97mmWcV6p9ItOvAPN7E8KA2HjOxFLE9Kw6IyN0Xf" +
            "bFpju/BYU0+Ol4kzlgXCEV4DnOfhkpft+UnT0sU2sfMd/5v+Ac3Kn85tk3jiAAAAAElFTkSuQmCC"},
        {"cc0-gs-pdfwrite-input-s3-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8ElEQVR4Xu2TW27DQAwDff9Lu6jh9UqUSK2dKgGKzE8SkeYgfWz7" +
            "czYLhoG6wfl3JqdZcVU5wwgWVUXMcOtrKp0ycHtFJUMGij5gUmsqo8TheAmIiHLNvs+UnPgeTzjJao9priY/Pb7HE8pczUx0kAaC" +
            "uDovX1NFnDUXOkgDAe7az32mCLYHNBDgtgfbAxoIcNuD7QENSty6ecvgyQJzu9s06TT5B9tMYbXLFGc/YDItQESca3c8HQ4JIqLM" +
            "3WByPY/KGJ8w4cW2EBkSgmlF9MiE/05Lomemc9t9qEQPTfaPYoAVpCwQ0FPv1A3CTc8Lpt3IMEhZa2WYb7Tkq3IOiqqlKuegqFqq" +
            "cg6KqqUqZ6DmAEsOnVLs+nwvx2TIweUFlco4yW5y8oiIk36D5OQQEScTtZruqXjCGaI3mm79ongiyFXx4uCJYJrs4+Hg4YnAmOZ0" +
            "IfoD01i37zNEJPCmQzBeKSoTeM8F1iwyFKDjAEsOnQrQ8gt2HDoloMGC3QueCHDdgt0Lnghw3YLdC54I7Kb3dJrQZXsOnnDiZq8p" +
            "OcW7gQaCdLHDRBbbTHjtMBUqPA9oICCLbSZ8Mr9OeMLJN/PrhCeCbDS7OUQkiLPnRcyJSDB2z2X4mKMywdx2YM0iQwVKNu15wRRc" +
            "GCNlgfM17T/q3rbTWGVZhAAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s3-sys1-m4-st2", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8ElEQVR4Xu2TW27DQAwDff9Lu6jh9UqUSK2dKgGKzE8SkeYgfWz7" +
            "czYLhoG6wfl3JqdZcVU5wwgWVUXMcOtrKp0ycHtFJUMGij5gUmsqo8TheAmIiHLNvs+UnPgeTzjJao9priY/Pb7HE8pczUx0kAaC" +
            "uDovX1NFnDUXOkgDAe7az32mCLYHNBDgtgfbAxoIcNuD7QENSty6ecvgyQJzu9s06TT5B9tMYbXLFGc/YDItQESca3c8HQ4JIqLM" +
            "3WByPY/KGJ8w4cW2EBkSgmlF9MiE/05Lomemc9t9qEQPTfaPYoAVpCwQ0FPv1A3CTc8Lpt3IMEhZa2WYb7Tkq3IOiqqlKuegqFqq" +
            "cg6KqqUqZ6DmAEsOnVLs+nwvx2TIweUFlco4yW5y8oiIk36D5OQQEScTtZruqXjCGaI3mm79ongiyFXx4uCJYJrs4+Hg4YnAmOZ0" +
            "IfoD01i37zNEJPCmQzBeKSoTeM8F1iwyFKDjAEsOnQrQ8gt2HDoloMGC3QueCHDdgt0Lnghw3YLdC54I7Kb3dJrQZXsOnnDiZq8p" +
            "OcW7gQaCdLHDRBbbTHjtMBUqPA9oICCLbSZ8Mr9OeMLJN/PrhCeCbDS7OUQkiLPnRcyJSDB2z2X4mKMywdx2YM0iQwVKNu15wRRc" +
            "GCNlgfM17T/q3rbTWGVZhAAAAABJRU5ErkJggg=="},
        {"cc0-gs-pdfwrite-input-s4-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACK0lEQVR4Xu3N22rFMBBD0fz/T7f4Ejva0SiHQgqFrocELDE6vn7s" +
            "uGFD5DTizt9f4krDjshpjSMdSyKnJW4MbImcVjgxsSZyWuDCiT2RU48DC4sipx4HFhZFTi3e39gUObV4f2NT5NTi/Y1NkVOH5y9Y" +
            "FTl1eP6CVZFTi/c3NkVOLd4f2jObIqceR5r+yqLIqceVMTF/pZwW3NDvLJ3YEzktcOHEnshpgQsn9kROI+68tcSVhh2R0xpHOpZE" +
            "TkvcGNgSOa1wYmJN5LTAhRN7IqceBxYWRU49DiwsipxavL+xKXJq8f7GpsipxfsbmyKnDs9fsCpy6vD8BasipxbvD+2ZTZFTixtd" +
            "f2ZT5NTjytE3xreWU48z48YbS9xab1qCnFZ0aWFN5LTCiYk1kdMKJybWRE4j7ry1xJWGHZHTGkc6lkROS9wY2BI5rXBiYk3ktMCF" +
            "E3sipx4HFhZFTj0OLCyKnFq8v7Epcmrx/samyKnF+xubIqcOz1+wKnLq8PwFqyKnFu8P7ZlNkVOLG11/ZlPk1OPK0TfGt5ZTjzPj" +
            "xhtL3FpvWoKcVnRpYU3ktMKJiTWR0wonJtZEThPOHC8tcaRjSeS0xI2BLZHTCicm1kROC1w4sSdy6nFgYVHk1OPAwqLIqcX7G5si" +
            "pxbvLyyqh9jhwMKieogN3t/YVA+xwfsLi/CUG1yYWKPHwh0nBrZunht3HGnYufugcsOV45OhHy3dpphbn7Xgf2n6BsmTL03W7fNB" +
            "AAAAAElFTkSuQmCC"},
        {"cc0-gs-pdfwrite-input-s4-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACN0lEQVR4XtXNUa7bMBQDUe9/0y1iORI9T5wYRVOg58MGROLy+PXH" +
            "jp9YSRo6zhz//RJHTiwlDQU3BraShh0nLqwlDTtOXFhLGlZceGMvadhwYGIxadhwYGIxaVjw/sJm0rDg/YXNpGHB+wubScM9ng+s" +
            "Jg33eD6wmjQseH94PbOZNCy4cTqf2UwaFhx5Gc9sJg0bzoyJ69do2Ox2/s3SxGLSsOHAxGLSsOHAxGLS0HHm+NISR04sJQ0FNwa2" +
            "koYdJy6sJQ07TlxYSxpWXHhjL2nYcGBiMWnYcGBiMWlY8P7CZtKw4P2FzaRhwfsLm0nDPZ4PrCYN93g+sJo0LHh/eD2zmTQsuHE6" +
            "n9lMGjZcOc6N8a00bDgzjnxjCVvr7dYBDbvb0sJa0rDjxIW1pGHHiQtrSUPHmeNLSxw5sZQ0FNwY2Eoadpy4sJY07DhxYS1pWHHh" +
            "jb2kYcOBicWkYcOBicWkYcH7C5tJw4L3FzaThgXvL2wmDfd4PrCaNNzj+cBq0rDg/eH1zGbSsODG6XxmM2nYcOU4N8a30rDhzDjy" +
            "jSVsrbdbBzTsbksLa0nDjhMX1pKGHScurCUNFVde2EkaGo6cWEoaCm4MbCUNO05cWEsaVlx4Yy9p2HBgYjFp2HBgYjFpWPD+wmbS" +
            "sOD9icUbT/c4MLF44+kW7y9s3ni6xfsTi3cf4i0uXFiDT/kOJwa26GNhgxsnln743NjgyvFg6C8tsbDzqEQcenTkUYk49OjIoxJx" +
            "6NGR35qqHGBQHpIYAAAAAElFTkSuQmCC"},
        {"cc0-gs-pdfwrite-input-s4-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACM0lEQVR4XtXNQW7FIBRD0ex/060CfIhv/BxUNZV6Bgyw9Xx8/dhx" +
            "w4bIacSd/7/ElRM7Iqc1jjQsiZyWuNGxJXJa4cTAmshpgQsf7ImcehyYWBQ59TgwsShyavH+wqbIqcX7C5sipxbvL2yKnDo8f8Gq" +
            "yKnD8xesipxavN+d32yKnFrcaNo3myKnFkeaV5bM1PhlUeTUczt/szSxKHLqcWBiUeTU48DEoshpxJ23lrhyYkfktMaRhiWR0xI3" +
            "OrZETiucGFgTOS1w4YM9kVOPAxOLIqceByYWRU4t3l/YFDm1eH9hU+TU4v2FTZFTh+cvWBU5dXj+glWRU4v3u/ObTZFTixtN+2ZT" +
            "5NTjytE2+lvLqceZfuONJWytP+lQTiuytLAmclrhxMCayGmFEwNrIqcRd95a4sqJHZHTGkcalkROS9zo2BI5rXBiYE3ktMCFD/ZE" +
            "Tj0OTCyKnHocmFgUObV4f2FT5NTi/YVNkVOL9xc2RU4dnr9gVeTU4fkLVkVOLd7vzm82RU4tbjTtm02RU48rR9voby2nHmf6jTeW" +
            "sLX+pEM5rcjSwprIaYUTA2sipxVODKyJnCacOV5a4kjDkshpiRsdWyKnFU4MrImcFrjwwZ7IqceBiUWRU48DE4sipxbvL2yKnFq8" +
            "P7GoHmKHAxOL6iE2eH9hUz3EBu9PLMJTbnBhYI0eC3ec6Ni6eW7ccKNh6W6jcsOVY2fod5ZYsPZaikN7N/ZaikN7N/ZaikN7N74B" +
            "ZnsqUovwKX4AAAAASUVORK5CYII="},
        {"cc0-gs-ps2write-input-s1-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHUlEQVR4Xu2R4W7DQAiD9/4v3alJ4MAYJyflMlXr96eHDfa6/rxm" +
            "+HHQeUWTLlRFIYI+tUkFuWmP4qMgsCyWE/P5Cs4KayExh8enQ8ujYtSUFPwabAlPBFZSQ8Zv1MybFgeJXdeM+h3Y1i1NRWRrl5u2" +
            "O09Y2LTfeUL+vxSR7c008YSbm44rlsBEIs00HR+YwEQiXWyyG5bARCItbwqavwR+QQKoOKTJpvelfWIAFYc022RrJICKQ/o2vZ5s" +
            "OiABVBzSt2lAAqg4pOeawtp4XoAlMJFIn9U01Ko81xS3wvscT8hnRWRrtzSVKpw3LQ6nWMLfNZnRjLuWpjOsZ2WTNQDok8E38thh" +
            "yUhdSa8Izg2WjIidaG02ChxIcfql5GwuCpwUcglM+M9NDMw+iTqxe7BmA5ci0hTE9PFWacoTlODzKmEJWCzTIr0j4F+AaYHeEdCi" +
            "lU14S0WnNQRWhLdUdFpD4E1TP1RrKJoqIgVaQzGa4nlVEq2hCE0hWRfd0GThaSD0jiIV7fn+6BCWItUEcC+gPAVW7OBWRJoKLNnA" +
            "pYg0O7AggctGaygwPIHLRmsoMDyBy0ZrKGJkrnG50hqKHJmL2sDWENTItU1EIsag0xU0cEVTE7isCdUVTWdVqB90uqIJfLwJZaM1" +
            "BDzyUFF2WkNBM6kY6B1F/ftN6fN6R+LB2/2YRJywJCE8gmsB5Umw4w3uJLQrmer5NgGTTb+XJFwSaRPgywAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s1-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACWElEQVR4Xu2R227jQAxD8/8/3YUzF1EcimO3doAWe16qISUySV9f" +
            "l3hNaqfY4PeGMuf3NpmcocJK3uELS52SNL10YxM9eetKUwTkjGnw+7mm9efDLxrjDrhPESJ4EQ4txh1xniNub3rfqYj0EViBrRg9" +
            "7U5GICzBWoyediYjEJZgLUZLP1MRN/96/UpFYBMLuAWzYVzJjEXldxNhNowrmbGo/G4izDXzSGakjT6vS3xRMK50SCZ2vtM09nRI" +
            "olqpLzKnmsIVK/JC8BebOlVMw3m3Nq0OvtSFwTQp+ZEmJSaJzQ1lk5CeaVqVrsIjxjMUTVKk+luaQkWD1u5vGh6M7R3jGSiNxQXc" +
            "gvkEOqMJ4QW4BfMJdEZ/hjnBLZhPoDPGM9wOLN3RBM+wG7F0oYlDGtORW3B+vilHBNMTe3B+OPlZggnI9NbFOG4GvSsiIDO8ZTOE" +
            "obNQAOElaTOfHzoLBSmyIG3m80NnoSBFFqTNfH7oLJwntcjwxMau4Zo3vIRY04DpMbs05zk4eV9lLIfIFVKidhzyGwgJqR2HKnq0" +
            "6VJVaThG0QebrvyjSsMRTel8VZDScEAT3i9CojQsWDUTfNEdTSMdZ0HtOHJRKxh/K4zlyDUB7wHOc3BFg7cQazq45A0vIdYs4QaE" +
            "dwelYeF0hHcHpWHhdIR3B6VhgcxcM1RBaTgo8ummRfhIU+rKxqTSLSrwkSad97mmWcV6p9ItOvAPN7E8KA2HjOxFLE9Kw6IyN0Xf" +
            "bFpju/BYU0+Ol4kzlgXCEV4DnOfhkpft+UnT0sU2sfMd/5v+Ac3Kn85tk3jiAAAAAElFTkSuQmCC"},
        {"cc0-gs-ps2write-input-s1-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8ElEQVR4Xu2TW27DQAwDff9Lu6jh9UqUSK2dKgGKzE8SkeYgfWz7" +
            "czYLhoG6wfl3JqdZcVU5wwgWVUXMcOtrKp0ycHtFJUMGij5gUmsqo8TheAmIiHLNvs+UnPgeTzjJao9priY/Pb7HE8pczUx0kAaC" +
            "uDovX1NFnDUXOkgDAe7az32mCLYHNBDgtgfbAxoIcNuD7QENSty6ecvgyQJzu9s06TT5B9tMYbXLFGc/YDItQESca3c8HQ4JIqLM" +
            "3WByPY/KGJ8w4cW2EBkSgmlF9MiE/05Lomemc9t9qEQPTfaPYoAVpCwQ0FPv1A3CTc8Lpt3IMEhZa2WYb7Tkq3IOiqqlKuegqFqq" +
            "cg6KqqUqZ6DmAEsOnVLs+nwvx2TIweUFlco4yW5y8oiIk36D5OQQEScTtZruqXjCGaI3mm79ongiyFXx4uCJYJrs4+Hg4YnAmOZ0" +
            "IfoD01i37zNEJPCmQzBeKSoTeM8F1iwyFKDjAEsOnQrQ8gt2HDoloMGC3QueCHDdgt0Lnghw3YLdC54I7Kb3dJrQZXsOnnDiZq8p" +
            "OcW7gQaCdLHDRBbbTHjtMBUqPA9oICCLbSZ8Mr9OeMLJN/PrhCeCbDS7OUQkiLPnRcyJSDB2z2X4mKMywdx2YM0iQwVKNu15wRRc" +
            "GCNlgfM17T/q3rbTWGVZhAAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s2-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB3klEQVR4Xu2T0Y7CQAwD+f+fvpO2pZt4bSctcCdVzBOJsx6BxOPn" +
            "Ko8FvMgUsQE9NzAd7V2XDTW5uaVymQFqOyoTGbD386b159N9OjGsxctiRSeG2fuHpv09zgSdaGbt0YwzQSeaWXs040zQiWH2/qEJ" +
            "F/kwYiIDNuPMMJEBm3FmmMgTazui66bAFLk2lzVpeV41TcnXdJ6WYqd1pFg9xiqDmkWz7fJiIoMS4kl/Z0QGFbSULndkUMA76XJH" +
            "J5bx0y296yagE8dTlG3EHdCJI5me9eEjQyeGrEng6cREmmdplgzwdGIizVGJns+ZFlW+S7hMETpB5NpcpoidbdElk1SlI8SnglTb" +
            "FF035an0vGQ6vktPVeWUO5o2vqYXTCdF7zNhvFBfKP7LhOlK44RzVvQmE4aM1hHjrOgtpm2GfKE8EATRqHgKNVWuWEWVqoglRFRU" +
            "FbGCiYqqIlYwUVFVxAIqKqqKWBDLj89FVRFTZvcDBkcRU1J5GhxFTEnlaXAUMSUV38OUez9ogtrPmwz4YkOsNVjLwDcDvjVgKwPf" +
            "DPjWgbUEfDLg2xNUgoMirvmaXuFWpqAA8HJDrGuwPoK3A75tgO0RvB3wbQNsj+DtgG8bYHsEbwd82wDbI3g7+AVljbnQm+hOjwAA" +
            "AABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s2-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAABsElEQVR4Xu2T0W4CMQwE+f+fpqppL87a3nW4SysQ80S89o5A4nY/" +
            "wc2DISIXGG9nmjQNl4hrfsvbLhoSfG9PxTLCXNtSkYiAtdtNye/nt2ZIRAjFDVWdEGJvnATqhBB74yRQJ4zQu90UBqSORITQu830" +
            "qJ5fu0ye4WFtLFM4gxadMaGGi640YQ6onIAmA5cGJFKgxMClAYkUKDFwaUCiNn9n6v2j6mQFZyoLy0DiLzeasHWbKbRuNx3Xr2+K" +
            "tWNSFpYBJdRq0UlTGLy8Cd9U9KTJqueXEj1r8t9qeGgZDRmu/wB3JnhKWdHcT5m+aXtOmdz36chUTviYXsKEItWk8ppF0XUmjAN6" +
            "o+K/TJhGGis5q6KLTBhmtJYyVkWXmH7esICovMKJrOIwloi4JBGJKhGXJCJRJeKKTCSqRFyRiUSViAtSkagScYEvPz6LKhGnjO6P" +
            "aZ25+C1M0LvdBM+NJgIePCjGBOxNwBMjnxKwNQNvjHzKwNoEPDHy6QpCcCDiBh/TGd7KNBQALj4oxhqs9+CukU8bYLsHd4182gDb" +
            "Pbhr5NMG2O7BXSOfNsB2D+4aXyWb3ayeqEW+AAAAAElFTkSuQmCC"},
        {"cc0-gs-ps2write-input-s2-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACEUlEQVR4Xu2S0W4bSQwE9f8/rSCKdoZssourrNcGgtRLJNaoC8bd" +
            "43mBR0Z1ZtDMP1iS0JBiS8TAmRRKJK/fWNI/Y0yRI/oQpUAROnx8hzlQhA6vkt/zhtDdHfJ73gBldx/8njdA3a2XgjdA3S2HCihP" +
            "s1svCijLCqUf63cFpaEvTXz0+E0pnUqeeaPEUqlaxgcNYX1//PMdQGmQ+Yw+Xnjj0fGEPl5449HxhD5eeONphsuh4o1nz65f10vB" +
            "G8+ebUp20Aqgm21OghXAXv2+Et8yVgDdanfLWEE0s81JsIJoZutF8YYou+vg97xBdPcHSvFRBhQiy3Por0tp+/iMYyiRvX4qdKEU" +
            "/zc4EbpUSil1hfkFcjbzvFKKf9CZ3OQ9GpqWJu/R0LQ0eY+GpqXJOzTzQh8l2Fri+v6MYyg9unwiRc7T7DanDChP+xc0pwQoTxe6" +
            "tfRZyhvPEfrG0kf/obwB+lS9JLwBdin+vBwy3gChtKeH0BeUjvX4uQMUkEuvwPGvhRyQOwt9FkEJaOOFPkqwBbTyG32TYGvQQkTf" +
            "LrwBdD2ibxfeALoe0bcLb4C4mTt3lrQV3yW88dTNe0vNqd4DVgDt4h0ls3hbSa93lIaUng+sAMzibSX9ZX/deOPpN/vrxhugG+1u" +
            "CVBAnX1fYA4UcOy+l+VrDzlgbyf0WQQloZEHdy6USku1Mj7w/C89fwFgRMfCVpZvFwAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s3-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHUlEQVR4Xu2R4W7DQAiD9/4v3alJ4MAYJyflMlXr96eHDfa6/rxm" +
            "+HHQeUWTLlRFIYI+tUkFuWmP4qMgsCyWE/P5Cs4KayExh8enQ8ujYtSUFPwabAlPBFZSQ8Zv1MybFgeJXdeM+h3Y1i1NRWRrl5u2" +
            "O09Y2LTfeUL+vxSR7c008YSbm44rlsBEIs00HR+YwEQiXWyyG5bARCItbwqavwR+QQKoOKTJpvelfWIAFYc022RrJICKQ/o2vZ5s" +
            "OiABVBzSt2lAAqg4pOeawtp4XoAlMJFIn9U01Ko81xS3wvscT8hnRWRrtzSVKpw3LQ6nWMLfNZnRjLuWpjOsZ2WTNQDok8E38thh" +
            "yUhdSa8Izg2WjIidaG02ChxIcfql5GwuCpwUcglM+M9NDMw+iTqxe7BmA5ci0hTE9PFWacoTlODzKmEJWCzTIr0j4F+AaYHeEdCi" +
            "lU14S0WnNQRWhLdUdFpD4E1TP1RrKJoqIgVaQzGa4nlVEq2hCE0hWRfd0GThaSD0jiIV7fn+6BCWItUEcC+gPAVW7OBWRJoKLNnA" +
            "pYg0O7AggctGaygwPIHLRmsoMDyBy0ZrKGJkrnG50hqKHJmL2sDWENTItU1EIsag0xU0cEVTE7isCdUVTWdVqB90uqIJfLwJZaM1" +
            "BDzyUFF2WkNBM6kY6B1F/ftN6fN6R+LB2/2YRJywJCE8gmsB5Umw4w3uJLQrmer5NgGTTb+XJFwSaRPgywAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s3-sys1-m2-st2", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHUlEQVR4Xu2R4W7DQAiD9/4v3alJ4MAYJyflMlXr96eHDfa6/rxm" +
            "+HHQeUWTLlRFIYI+tUkFuWmP4qMgsCyWE/P5Cs4KayExh8enQ8ujYtSUFPwabAlPBFZSQ8Zv1MybFgeJXdeM+h3Y1i1NRWRrl5u2" +
            "O09Y2LTfeUL+vxSR7c008YSbm44rlsBEIs00HR+YwEQiXWyyG5bARCItbwqavwR+QQKoOKTJpvelfWIAFYc022RrJICKQ/o2vZ5s" +
            "OiABVBzSt2lAAqg4pOeawtp4XoAlMJFIn9U01Ko81xS3wvscT8hnRWRrtzSVKpw3LQ6nWMLfNZnRjLuWpjOsZ2WTNQDok8E38thh" +
            "yUhdSa8Izg2WjIidaG02ChxIcfql5GwuCpwUcglM+M9NDMw+iTqxe7BmA5ci0hTE9PFWacoTlODzKmEJWCzTIr0j4F+AaYHeEdCi" +
            "lU14S0WnNQRWhLdUdFpD4E1TP1RrKJoqIgVaQzGa4nlVEq2hCE0hWRfd0GThaSD0jiIV7fn+6BCWItUEcC+gPAVW7OBWRJoKLNnA" +
            "pYg0O7AggctGaygwPIHLRmsoMDyBy0ZrKGJkrnG50hqKHJmL2sDWENTItU1EIsag0xU0cEVTE7isCdUVTWdVqB90uqIJfLwJZaM1" +
            "BDzyUFF2WkNBM6kY6B1F/ftN6fN6R+LB2/2YRJywJCE8gmsB5Umw4w3uJLQrmer5NgGTTb+XJFwSaRPgywAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s3-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACWElEQVR4Xu2R227jQAxD8/8/3YUzF1EcimO3doAWe16qISUySV9f" +
            "l3hNaqfY4PeGMuf3NpmcocJK3uELS52SNL10YxM9eetKUwTkjGnw+7mm9efDLxrjDrhPESJ4EQ4txh1xniNub3rfqYj0EViBrRg9" +
            "7U5GICzBWoyediYjEJZgLUZLP1MRN/96/UpFYBMLuAWzYVzJjEXldxNhNowrmbGo/G4izDXzSGakjT6vS3xRMK50SCZ2vtM09nRI" +
            "olqpLzKnmsIVK/JC8BebOlVMw3m3Nq0OvtSFwTQp+ZEmJSaJzQ1lk5CeaVqVrsIjxjMUTVKk+luaQkWD1u5vGh6M7R3jGSiNxQXc" +
            "gvkEOqMJ4QW4BfMJdEZ/hjnBLZhPoDPGM9wOLN3RBM+wG7F0oYlDGtORW3B+vilHBNMTe3B+OPlZggnI9NbFOG4GvSsiIDO8ZTOE" +
            "obNQAOElaTOfHzoLBSmyIG3m80NnoSBFFqTNfH7oLJwntcjwxMau4Zo3vIRY04DpMbs05zk4eV9lLIfIFVKidhzyGwgJqR2HKnq0" +
            "6VJVaThG0QebrvyjSsMRTel8VZDScEAT3i9CojQsWDUTfNEdTSMdZ0HtOHJRKxh/K4zlyDUB7wHOc3BFg7cQazq45A0vIdYs4QaE" +
            "dwelYeF0hHcHpWHhdIR3B6VhgcxcM1RBaTgo8ummRfhIU+rKxqTSLSrwkSad97mmWcV6p9ItOvAPN7E8KA2HjOxFLE9Kw6IyN0Xf" +
            "bFpju/BYU0+Ol4kzlgXCEV4DnOfhkpft+UnT0sU2sfMd/5v+Ac3Kn85tk3jiAAAAAElFTkSuQmCC"},
        {"cc0-gs-ps2write-input-s3-sys1-m3-st2", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACWElEQVR4Xu2R227jQAxD8/8/3YUzF1EcimO3doAWe16qISUySV9f" +
            "l3hNaqfY4PeGMuf3NpmcocJK3uELS52SNL10YxM9eetKUwTkjGnw+7mm9efDLxrjDrhPESJ4EQ4txh1xniNub3rfqYj0EViBrRg9" +
            "7U5GICzBWoyediYjEJZgLUZLP1MRN/96/UpFYBMLuAWzYVzJjEXldxNhNowrmbGo/G4izDXzSGakjT6vS3xRMK50SCZ2vtM09nRI" +
            "olqpLzKnmsIVK/JC8BebOlVMw3m3Nq0OvtSFwTQp+ZEmJSaJzQ1lk5CeaVqVrsIjxjMUTVKk+luaQkWD1u5vGh6M7R3jGSiNxQXc" +
            "gvkEOqMJ4QW4BfMJdEZ/hjnBLZhPoDPGM9wOLN3RBM+wG7F0oYlDGtORW3B+vilHBNMTe3B+OPlZggnI9NbFOG4GvSsiIDO8ZTOE" +
            "obNQAOElaTOfHzoLBSmyIG3m80NnoSBFFqTNfH7oLJwntcjwxMau4Zo3vIRY04DpMbs05zk4eV9lLIfIFVKidhzyGwgJqR2HKnq0" +
            "6VJVaThG0QebrvyjSsMRTel8VZDScEAT3i9CojQsWDUTfNEdTSMdZ0HtOHJRKxh/K4zlyDUB7wHOc3BFg7cQazq45A0vIdYs4QaE" +
            "dwelYeF0hHcHpWHhdIR3B6VhgcxcM1RBaTgo8ummRfhIU+rKxqTSLSrwkSad97mmWcV6p9ItOvAPN7E8KA2HjOxFLE9Kw6IyN0Xf" +
            "bFpju/BYU0+Ol4kzlgXCEV4DnOfhkpft+UnT0sU2sfMd/5v+Ac3Kn85tk3jiAAAAAElFTkSuQmCC"},
        {"cc0-gs-ps2write-input-s3-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8ElEQVR4Xu2TW27DQAwDff9Lu6jh9UqUSK2dKgGKzE8SkeYgfWz7" +
            "czYLhoG6wfl3JqdZcVU5wwgWVUXMcOtrKp0ycHtFJUMGij5gUmsqo8TheAmIiHLNvs+UnPgeTzjJao9priY/Pb7HE8pczUx0kAaC" +
            "uDovX1NFnDUXOkgDAe7az32mCLYHNBDgtgfbAxoIcNuD7QENSty6ecvgyQJzu9s06TT5B9tMYbXLFGc/YDItQESca3c8HQ4JIqLM" +
            "3WByPY/KGJ8w4cW2EBkSgmlF9MiE/05Lomemc9t9qEQPTfaPYoAVpCwQ0FPv1A3CTc8Lpt3IMEhZa2WYb7Tkq3IOiqqlKuegqFqq" +
            "cg6KqqUqZ6DmAEsOnVLs+nwvx2TIweUFlco4yW5y8oiIk36D5OQQEScTtZruqXjCGaI3mm79ongiyFXx4uCJYJrs4+Hg4YnAmOZ0" +
            "IfoD01i37zNEJPCmQzBeKSoTeM8F1iwyFKDjAEsOnQrQ8gt2HDoloMGC3QueCHDdgt0Lnghw3YLdC54I7Kb3dJrQZXsOnnDiZq8p" +
            "OcW7gQaCdLHDRBbbTHjtMBUqPA9oICCLbSZ8Mr9OeMLJN/PrhCeCbDS7OUQkiLPnRcyJSDB2z2X4mKMywdx2YM0iQwVKNu15wRRc" +
            "GCNlgfM17T/q3rbTWGVZhAAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s3-sys1-m4-st2", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8ElEQVR4Xu2TW27DQAwDff9Lu6jh9UqUSK2dKgGKzE8SkeYgfWz7" +
            "czYLhoG6wfl3JqdZcVU5wwgWVUXMcOtrKp0ycHtFJUMGij5gUmsqo8TheAmIiHLNvs+UnPgeTzjJao9priY/Pb7HE8pczUx0kAaC" +
            "uDovX1NFnDUXOkgDAe7az32mCLYHNBDgtgfbAxoIcNuD7QENSty6ecvgyQJzu9s06TT5B9tMYbXLFGc/YDItQESca3c8HQ4JIqLM" +
            "3WByPY/KGJ8w4cW2EBkSgmlF9MiE/05Lomemc9t9qEQPTfaPYoAVpCwQ0FPv1A3CTc8Lpt3IMEhZa2WYb7Tkq3IOiqqlKuegqFqq" +
            "cg6KqqUqZ6DmAEsOnVLs+nwvx2TIweUFlco4yW5y8oiIk36D5OQQEScTtZruqXjCGaI3mm79ongiyFXx4uCJYJrs4+Hg4YnAmOZ0" +
            "IfoD01i37zNEJPCmQzBeKSoTeM8F1iwyFKDjAEsOnQrQ8gt2HDoloMGC3QueCHDdgt0Lnghw3YLdC54I7Kb3dJrQZXsOnnDiZq8p" +
            "OcW7gQaCdLHDRBbbTHjtMBUqPA9oICCLbSZ8Mr9OeMLJN/PrhCeCbDS7OUQkiLPnRcyJSDB2z2X4mKMywdx2YM0iQwVKNu15wRRc" +
            "GCNlgfM17T/q3rbTWGVZhAAAAABJRU5ErkJggg=="},
        {"cc0-gs-ps2write-input-s4-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACK0lEQVR4Xu3N22rFMBBD0fz/T7f4Ejva0SiHQgqFrocELDE6vn7s" +
            "uGFD5DTizt9f4krDjshpjSMdSyKnJW4MbImcVjgxsSZyWuDCiT2RU48DC4sipx4HFhZFTi3e39gUObV4f2NT5NTi/Y1NkVOH5y9Y" +
            "FTl1eP6CVZFTi/c3NkVOLd4f2jObIqceR5r+yqLIqceVMTF/pZwW3NDvLJ3YEzktcOHEnshpgQsn9kROI+68tcSVhh2R0xpHOpZE" +
            "TkvcGNgSOa1wYmJN5LTAhRN7IqceBxYWRU49DiwsipxavL+xKXJq8f7GpsipxfsbmyKnDs9fsCpy6vD8BasipxbvD+2ZTZFTixtd" +
            "f2ZT5NTjytE3xreWU48z48YbS9xab1qCnFZ0aWFN5LTCiYk1kdMKJybWRE4j7ry1xJWGHZHTGkc6lkROS9wY2BI5rXBiYk3ktMCF" +
            "E3sipx4HFhZFTj0OLCyKnFq8v7Epcmrx/samyKnF+xubIqcOz1+wKnLq8PwFqyKnFu8P7ZlNkVOLG11/ZlPk1OPK0TfGt5ZTjzPj" +
            "xhtL3FpvWoKcVnRpYU3ktMKJiTWR0wonJtZEThPOHC8tcaRjSeS0xI2BLZHTCicm1kROC1w4sSdy6nFgYVHk1OPAwqLIqcX7G5si" +
            "pxbvLyyqh9jhwMKieogN3t/YVA+xwfsLi/CUG1yYWKPHwh0nBrZunht3HGnYufugcsOV45OhHy3dpphbn7Xgf2n6BsmTL03W7fNB" +
            "AAAAAElFTkSuQmCC"},
        {"cc0-gs-ps2write-input-s4-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACN0lEQVR4XtXNUa7bMBQDUe9/0y1iORI9T5wYRVOg58MGROLy+PXH" +
            "jp9YSRo6zhz//RJHTiwlDQU3BraShh0nLqwlDTtOXFhLGlZceGMvadhwYGIxadhwYGIxaVjw/sJm0rDg/YXNpGHB+wubScM9ng+s" +
            "Jg33eD6wmjQseH94PbOZNCy4cTqf2UwaFhx5Gc9sJg0bzoyJ69do2Ox2/s3SxGLSsOHAxGLSsOHAxGLS0HHm+NISR04sJQ0FNwa2" +
            "koYdJy6sJQ07TlxYSxpWXHhjL2nYcGBiMWnYcGBiMWlY8P7CZtKw4P2FzaRhwfsLm0nDPZ4PrCYN93g+sJo0LHh/eD2zmTQsuHE6" +
            "n9lMGjZcOc6N8a00bDgzjnxjCVvr7dYBDbvb0sJa0rDjxIW1pGHHiQtrSUPHmeNLSxw5sZQ0FNwY2Eoadpy4sJY07DhxYS1pWHHh" +
            "jb2kYcOBicWkYcOBicWkYcH7C5tJw4L3FzaThgXvL2wmDfd4PrCaNNzj+cBq0rDg/eH1zGbSsODG6XxmM2nYcOU4N8a30rDhzDjy" +
            "jSVsrbdbBzTsbksLa0nDjhMX1pKGHScurCUNFVde2EkaGo6cWEoaCm4MbCUNO05cWEsaVlx4Yy9p2HBgYjFp2HBgYjFpWPD+wmbS" +
            "sOD9icUbT/c4MLF44+kW7y9s3ni6xfsTi3cf4i0uXFiDT/kOJwa26GNhgxsnln743NjgyvFg6C8tsbDzqEQcenTkUYk49OjIoxJx" +
            "6NGR35qqHGBQHpIYAAAAAElFTkSuQmCC"},
        {"cc0-gs-ps2write-input-s4-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACM0lEQVR4XtXNQW7FIBRD0ex/060CfIhv/BxUNZV6Bgyw9Xx8/dhx" +
            "w4bIacSd/7/ElRM7Iqc1jjQsiZyWuNGxJXJa4cTAmshpgQsf7ImcehyYWBQ59TgwsShyavH+wqbIqcX7C5sipxbvL2yKnDo8f8Gq" +
            "yKnD8xesipxavN+d32yKnFrcaNo3myKnFkeaV5bM1PhlUeTUczt/szSxKHLqcWBiUeTU48DEoshpxJ23lrhyYkfktMaRhiWR0xI3" +
            "OrZETiucGFgTOS1w4YM9kVOPAxOLIqceByYWRU4t3l/YFDm1eH9hU+TU4v2FTZFTh+cvWBU5dXj+glWRU4v3u/ObTZFTixtN+2ZT" +
            "5NTjytE2+lvLqceZfuONJWytP+lQTiuytLAmclrhxMCayGmFEwNrIqcRd95a4sqJHZHTGkcalkROS9zo2BI5rXBiYE3ktMCFD/ZE" +
            "Tj0OTCyKnHocmFgUObV4f2FT5NTi/YVNkVOL9xc2RU4dnr9gVeTU4fkLVkVOLd7vzm82RU4tbjTtm02RU48rR9voby2nHmf6jTeW" +
            "sLX+pEM5rcjSwprIaYUTA2sipxVODKyJnCacOV5a4kjDkshpiRsdWyKnFU4MrImcFrjwwZ7IqceBiUWRU48DE4sipxbvL2yKnFq8" +
            "P7GoHmKHAxOL6iE2eH9hUz3EBu9PLMJTbnBhYI0eC3ec6Ni6eW7ccKNh6W6jcsOVY2fod5ZYsPZaikN7N/ZaikN7N/ZaikN7N74B" +
            "ZnsqUovwKX4AAAAASUVORK5CYII="},
        {"cc0-jpeg-q60-prepared-1-s1-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACUklEQVR4Xu2S0VIbUQxD+f+fbodubMuypd2kLE1nOA+Za8mWIPDx" +
            "6zofiPMe0EYfLSbmp+kMG7PSV9pkcSlN3pe+pIlVng8R3p4654yyeH6pqa5nxhCH8KnB21JxM2OIU7neVLcjYsmdyuWmOl0iprhI" +
            "X9u0SM82wWWwmYv0ZFMebQlb06Y90ZSPnrCmAqmla8iTJeCGpnz0gLOm0vJlyIstYWvatJ+mCvCpm/ZmTUkF+NRt8U2b4H5N3bSb" +
            "m0q6++9U0ou/0ytNpeXrClvCuzTBWj3PqQCMKHkq/21T6kNoPw+8T8mAnjGC162nmvb/CNB5re3gcEYmUAhHr0sXm/K2aLcliu/u" +
            "b5r+wAu4m95jg2ZBXA8iIEbYxftPmWZBXC80F3c5gmZBZQ2ai7scQbOgsgbNxV2OoFlQWYPm4i5H0CzArPhIoYbYfXw0eL5Khs8m" +
            "gTU1VTS/PYHzNBRdo4kzlmYLngqhHcNSdF4lDcNadG/TWtU0QBqGatq+PpQQaRj+SROeT6UhDQM21f0QCGkYsKiy+zSRhgNrIrwN" +
            "G9JwQMmAdxPtGDgd4NXCWA4uCHgPcJ6E8xu8HEjDweENXg6k4eDwBi8H0nBweIOXA2k4KhUaAl4OpGHomdhS6kQaBo5sRbJK6Y6Z" +
            "eFPTlohNIlLIjjXv+5qwi50DITtE4Pc1Sf2B0g2RqP5SXU6U7lgjs0glKt2xhW5aRxqGSo1knjek4cBghncT7Tg4vuDNwlgerjjg" +
            "LcB5Fu444C3AeRbuOOAtwHkW7jjgLcB5Fu444C3gN0nEFUtoVcjDAAAAAElFTkSuQmCC"},
        {"cc0-jpeg-q60-prepared-1-s1-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAChElEQVR4Xu2T0W7bUAxD+/8/3aGJLZGUSDttNmDDztMVKZFJgHx8" +
            "3ucD8U7DOzRlQsoO79CUCSk7vENTJqTs8A5NmZBSIq7w0gtNNoQlXKIteGcoITTRIm7BO4MBlDG0Fr7VBOeSMUQQcK2fGTyniKkO" +
            "4an2M4LXHDHVITzVfkbwmiOmOpWHWq8IHUvCkHU+9voZOK+2CNBm0Vubhggzpt9qqqs1Q0WYX23qK5NBYk+HcG7B27GlLAuH1ju8" +
            "dafpcVyPmcH0jnwcmgx1ZUKI3vn4xL1w0lQ0hEzGzj/WdMwvNxV3m0R6vvt5g9C0iSQNN+KbNpE1dTO2aRNF05OMawpij2BdUj0S" +
            "uor6g76jaZWn0M9r1kiSy6LhqfTzBhKnYpnw7LV+3qAT+g60Cd7C+5otA7QJ3sL7kjWjRnRP+vadTUtVLX25OFyxhsCIfqvnHg4e" +
            "jXiA1rrW918eTRaJaLiJ9yjhx03131n2JIJHByVY/jftUILlzU06A7Lc5w+Vx9tgQaFLRHYdHI9vTzQdGt1ziAuWZcudihIsy1JU" +
            "IkpMsCypyed5x7M2XVZZw1NFr30pa3igCa8XibCGB5vgfCqMNQLY1Pc6K97xUNOZzdOCdzxYU+n4XvGOB0sU3W2C5dF4QFebYCW0" +
            "4EDXkGgaNB8Ied7xaDyhy4V3PBpO6HLhHY+GE7pceMfTqdBwosuFdyySiTUgD6zh0UTqQYNxemAJ/D1Na+Cfa8IqNgqne0zgX910" +
            "WSX6idMDe+JV0U+a+HRXAWt4KhNvV5GwRqBTn8E673gnQNGMrjbBCmh+oYtA8gLacKBrSDQ9WnGga0g0PVpxoGtIND1acaBrSDQ9" +
            "WnGga8gvkBz6V3d6mLsAAAAASUVORK5CYII="},
        {"cc0-jpeg-q60-prepared-1-s1-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACJUlEQVR4Xu3TW27cQBBDUe1/0w5iPZrFqksNBhjFBnK+MsUW+eNs" +
            "X+/ZOn9i7nLiM9v/pdf5yjd/VN3EwDcO/qzIKbDy+ovEEKzm42v/PUoZacXrEOpChFrvOoS6EJGhdzg1nKChdjg1nKChVk5YiAGa" +
            "WqebwwBNrdPNYcCGVjlhIQZsaJUTTtE9GFr1RI10T3qrXB5cKk8FBkGvlQsWYhD0WrlgIQZBr5ULFmJw47mlv55bEi9M0T1rlfdD" +
            "7y310g8t9dZ1+dTS8BdRXipOWO/tl44TJLX/cqm8LEKEWm87TFJGvPelobeW7P/TGoplMSTSLDN56L2l2n/xV9VNTHxku9v5nUvb" +
            "zVhOkW/s/FWRU+QbO39V5BT5xs5fFTlFvrHzV0VOmY9880dFTpFvnPydiCHy5lemQsSG3uFkOGFz63RTnLAfsoSFGATXUvl4uikM" +
            "2Boap/SkMGC6JJ/3S4UB0yEpbgeDQaBDq7v8GHDCyszZrv8ecRLoivGnS4iY1y/+UqQs8IWDP1MxBN4vvngNg8DrK399wiDw7spf" +
            "nzAIvLvy1ycMglUqAxd/fcIgqJ26sq4dBswrdUbvhu7BUPjcUpkqwUJ3Nhc+tyRTFpzozqjwc0v+Jd1PdA/mxmuIGukezJXzVWAQ" +
            "TKXXDQsxCFbr3mw/ASdBqTb+9sJJ4vUXfyhSFvjCwZ+pGDKfOPgzFUPmEwd/pmLIfOLgz1QMmU8c/Jn6A6htj+xwTp36AAAAAElF" +
            "TkSuQmCC"},
        {"cc0-jpeg-q60-prepared-2-s1-sys1-m1-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACJUlEQVR4Xu3VwW4bMRADUP//T6ewvLviUCRHaycoWuSd4qFMIumh" +
            "j6/3PDR+BlKW8MSBn4GUJTxx4GcgZQlPHPgZSFnACxd+OIUoqL245Pt8EnAtDPk+nwRL7zz4KRsEa+96WdkgwN7X95eDYIMAe3+X" +
            "7sLe8f3loNggWIqXg2KDhHvhs+/zSUK9MOT7fBLdH3p3qdoZ+vYl3+eTDeufL/T5ZMPfWTo++z6fbICl63T+sPBJ6/xNutvB3Xuv" +
            "f5f+dnL31ijlWnG6uHvHD7lGd28cQ2qpniYbROdQLV4OhQ0SGJrl5YNggwRXjnb8WbNBMFsFfnzxicflBT+++MS7KuvGwG+nEFmz" +
            "s66cVy1EFnTWmTT06RJP1YdFypxSujv0wZL6peq7KoZGKS/4JYqhwf0Tv0QxNLh/4pcohgb3T/wSxdB7qDl+VDVxwDtdU5d7PNQ1" +
            "dbnHQ11Tl3s3h95fujv0bUscrzaeaHeH/rklDpWtRwoPtXtNbOHQs+Mc9JrYUUPNVE4tOZS7cmrJodyVU0sO5a6cOjg0f85dOZWg" +
            "+lH/R+SXRU4l7P4Pl+qn3JVTqRbDUO7KqVR7f3CJemEod+VUoFrYed1soQ2cWq3xdwZ9DbhV4e8M+upxqcbfepLHhEsl/tKTPCZc" +
            "qvB3Bn29oV045XTD79Infn7p6MahC78d9HUDtyN+O+jrBm5H/HbQ1w3cjvjtoK8buB3x20FfN3A74rfDHx0Mza4HfmqLAAAAAElF" +
            "TkSuQmCC"},
        {"cc0-jpeg-q60-prepared-2-s1-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACFElEQVR4Xu3TS3IbQQwDUN3/0rEj2moSjU/bmUmqUn67aVDASo9f" +
            "3/Kg8GrwqYQbBa8Gn0q4UfBq8KmEGwWvBp8qOPEJ7zobKjiw4GXjMgV625CbMpGEvev79UToRNpq28NtS2Rq3jY6kUjv/rLTidRq" +
            "//rS/kLoRNtq18PFS+4PNQ87E2l6yNSZyHmsXx4OfXepORy6dAmTKcQH+pJrc9mZtfQck1x25mfpz5xNmcganUdTOnGg9L4lbL1t" +
            "aa9dL7pPJ9peu170lAw0UkueNjLQSC152shA22v3F0InGtau79cToRMNa9f364nQiYatbcfUmUgbtX3HtblMG+0N3nU21HCi4NXg" +
            "Uwk3Cl4NPpVwo+DV4FMJNwpeDT6VcKPg1eBTCTcKXg0+NXDlEYauXMILkHIJd2JRPFBwKBbFA+WrQ5ctYbw7OKFg6KDm4IT68tBF" +
            "S5gyR0fEP1qqbzwBKRfIUJoKscKGQlWIBToUqkIs0KFQFWKBDoWqEAt0KFSFmFnV7+XjwwoxM8rHhxViZpSPDyvEzKheH3cujY+P" +
            "by3ExOy9cQl621CoCvEOetvQ76fX+04GwmgW8DdFPEvYSuGPnvirhqUU/uiJv2pYSuGPnvirga0E/qSI52MHEx98mv3vS5hNIY76" +
            "ku/yqTEmJjwt4jnD+gZPi3jOsL7B0yKeM6xv8LSI5wzrGzwt4jnD+gZPyxsyLNyfUjKeGwAAAABJRU5ErkJggg=="},
        {"cc0-jpeg-q60-prepared-2-s1-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACKUlEQVR4Xu3S4UoDURBD4b7/SyusuzuZzJxQhRYUv192cpsg+vj4" +
            "scfkT1QMM595/C89z1cO/kikLPKNkz8rIUq8vX3YcRJV81nwqqUx9MQvhUE0euuAhRgko1cOWIhBMGvlgoUYBLNWLliIQbDUzsvA" +
            "CavauaTvuhCRZaiOdXEhIuvSV9A/dzHcyVL9R7QXqyeeOF06v953dzld8ZJcJgyY1v4vfZfWnsXjsMCAae0bl5Zbe1zoHmjr/fXl" +
            "ZOgeSGl9e7t1dA+k9M8sra3braN7Iq3Lf4S+VBgkVfv+pXGYOElG8ThMnETW/MTQT5dad/2c2lIWSX3xRyqGka888tAvWPKRgz9q" +
            "csp85OCPmpwyHzn4oyanzEcO/qjJKfORgz9qcsp85OCPmpwSn7j4O5VTMKrHYREitvUup44Tdg+1Ly+nhhO2Dr1i6R5al7AQA1ZL" +
            "61S7CQyYLH3nD4UB0yX5+rx0GDAdkuJxMBgEOlTd7cOCE9Zmrnb9ecVJoCvGn5YQMa8v/lKkLPCFkz9TMQTeLz54DYPA6zt/fcEg" +
            "8O7OX18wCLy789cXDIIqlYGbv75gEPROXanrhAHzSp3Ru6F7sBS+b6lNtaDQne2F71uSKQsudGdU+Lol/ybdL3QP9sZ7iBrpHuyV" +
            "+1VgEGyl9w0LMQiq9avZPgJOglZt/O2Nk8Trb/5QpCzwhZM/UzFkPnHyZyqGzCdO/kzFkPnEyZ+pGDKfOPkz9QnAHYH6MXhXKwAA" +
            "AABJRU5ErkJggg=="},
        {"cc0-jpeg-q60-prepared-3-s1-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACUklEQVR4Xu2S0VIbUQxD+f+fbodubMuypd2kLE1nOA+Za8mWIPDx" +
            "6zofiPMe0EYfLSbmp+kMG7PSV9pkcSlN3pe+pIlVng8R3p4654yyeH6pqa5nxhCH8KnB21JxM2OIU7neVLcjYsmdyuWmOl0iprhI" +
            "X9u0SM82wWWwmYv0ZFMebQlb06Y90ZSPnrCmAqmla8iTJeCGpnz0gLOm0vJlyIstYWvatJ+mCvCpm/ZmTUkF+NRt8U2b4H5N3bSb" +
            "m0q6++9U0ou/0ytNpeXrClvCuzTBWj3PqQCMKHkq/21T6kNoPw+8T8mAnjGC162nmvb/CNB5re3gcEYmUAhHr0sXm/K2aLcliu/u" +
            "b5r+wAu4m95jg2ZBXA8iIEbYxftPmWZBXC80F3c5gmZBZQ2ai7scQbOgsgbNxV2OoFlQWYPm4i5H0CzArPhIoYbYfXw0eL5Khs8m" +
            "gTU1VTS/PYHzNBRdo4kzlmYLngqhHcNSdF4lDcNadG/TWtU0QBqGatq+PpQQaRj+SROeT6UhDQM21f0QCGkYsKiy+zSRhgNrIrwN" +
            "G9JwQMmAdxPtGDgd4NXCWA4uCHgPcJ6E8xu8HEjDweENXg6k4eDwBi8H0nBweIOXA2k4KhUaAl4OpGHomdhS6kQaBo5sRbJK6Y6Z" +
            "eFPTlohNIlLIjjXv+5qwi50DITtE4Pc1Sf2B0g2RqP5SXU6U7lgjs0glKt2xhW5aRxqGSo1knjek4cBghncT7Tg4vuDNwlgerjjg" +
            "LcB5Fu444C3AeRbuOOAtwHkW7jjgLcB5Fu444C3gN0nEFUtoVcjDAAAAAElFTkSuQmCC"},
        {"cc0-jpeg-q60-prepared-3-s1-sys1-m2-st2", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACO0lEQVR4Xu2QUW7kQBBCc/9LZ5W1u5qiADsrJ6OR9n25gIZJPj6/" +
            "wQcQrBNO0B1JPRNO0B1JPRNO0B1JPRNO0B1IPaDvz555ZqnLOvTwkjxPEY9IPRclJMrU7aX9enSMpSl8afCdqLeqY4hTeWRJaEL6" +
            "8SXQ6kuybHjKDW+3dLIeqYaLpRJvLdUTUfDo0n4iCh5dOh7WRy94fqk+qEAuqeD/JVHwrkuFKHjx0pb+cUkVvPvS7f+eCr770pZA" +
            "q68bQMFLlrY8FfxB+/MGu+E1SyUPof0e+L6kCnrHLB7Clwbfl1RB7xh/lAw9u3SqMnRzqd5upN+zPdIuy35NUAKzZZ0Bug3r9aQn" +
            "MLtfHwG6Dev1FZjlCroN2JbALFfQbcC2BGa5gm4DtiUwyxV0G7DtbwkJBz3bG+4uDXCg4FAjuxbeWHAOSJ6Hq/lWBMujeoXU8U5A" +
            "tkoRsEZClkoRsEZgdeqppgHWCOyl9lpIiDUCL1nC51NpWCOAS/B+CB1rBNpSdfdrYo0EzlR5OwTWSOAKw9nCOwmu33ByE6wEDyw4" +
            "ByTPwv0NDi+skeDyBocX1khweYPDC2skuLzB4YU1ElWKCwsOL6wR6J24stWJNTyjEndQ7zg9IBpxyDU6PSAbr6eMHDB9v7cEU+wc" +
            "GDlhCl+wRPrC6QHT+ANLeqqGXKPTE6pUaR1rBHbrWTwEhTUSrZngbOGdCPcXHNwEK8ILB5xCohngjQNOIdEM8MYBp5BoBnjjgFNI" +
            "NAO8ccAp5A9/7AhYFtng2AAAAABJRU5ErkJggg=="},
        {"cc0-jpeg-q60-prepared-3-s1-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAChElEQVR4Xu2T0W7bUAxD+/8/3aGJLZGUSDttNmDDztMVKZFJgHx8" +
            "3ucD8U7DOzRlQsoO79CUCSk7vENTJqTs8A5NmZBSIq7w0gtNNoQlXKIteGcoITTRIm7BO4MBlDG0Fr7VBOeSMUQQcK2fGTyniKkO" +
            "4an2M4LXHDHVITzVfkbwmiOmOpWHWq8IHUvCkHU+9voZOK+2CNBm0Vubhggzpt9qqqs1Q0WYX23qK5NBYk+HcG7B27GlLAuH1ju8" +
            "dafpcVyPmcH0jnwcmgx1ZUKI3vn4xL1w0lQ0hEzGzj/WdMwvNxV3m0R6vvt5g9C0iSQNN+KbNpE1dTO2aRNF05OMawpij2BdUj0S" +
            "uor6g76jaZWn0M9r1kiSy6LhqfTzBhKnYpnw7LV+3qAT+g60Cd7C+5otA7QJ3sL7kjWjRnRP+vadTUtVLX25OFyxhsCIfqvnHg4e" +
            "jXiA1rrW918eTRaJaLiJ9yjhx03131n2JIJHByVY/jftUILlzU06A7Lc5w+Vx9tgQaFLRHYdHI9vTzQdGt1ziAuWZcudihIsy1JU" +
            "IkpMsCypyed5x7M2XVZZw1NFr30pa3igCa8XibCGB5vgfCqMNQLY1Pc6K97xUNOZzdOCdzxYU+n4XvGOB0sU3W2C5dF4QFebYCW0" +
            "4EDXkGgaNB8Ied7xaDyhy4V3PBpO6HLhHY+GE7pceMfTqdBwosuFdyySiTUgD6zh0UTqQYNxemAJ/D1Na+Cfa8IqNgqne0zgX910" +
            "WSX6idMDe+JV0U+a+HRXAWt4KhNvV5GwRqBTn8E673gnQNGMrjbBCmh+oYtA8gLacKBrSDQ9WnGga0g0PVpxoGtIND1acaBrSDQ9" +
            "WnGga8gvkBz6V3d6mLsAAAAASUVORK5CYII="},
        {"cc0-jpeg-q60-prepared-3-s1-sys1-m3-st2", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACc0lEQVR4Xu2S4U7DQAyDef+XHqK7S2wnTjtRhoT4fl1tx9YQH4/r" +
            "fCDeCSjykM8RX0NOQJH/JVdDBkIh/JixJSxhiFLwHqEC6WDJpeA9gvfcUbQU7l2qYlUONZ8zeE4VVS3CU83nCF5zRVWrcqjxGqFj" +
            "aShqVQ41XiN0LA1FLcJTzefAvuoqQPvhpSLCN7ZfWtpXroPF/Hx5Ka5cB4n5tYSdgrejbamB+os4hB+OpkYSQSY0ZU+QOOo7kExo" +
            "zJ4gcdNWbA4xExrrTgpx01Zsfmfp+f36UnB1KSS04X3OsLRE/hn4VU8GcqhMbU2GIKcXI36p035kqdNk6J6lTis/846lVq1CPs+B" +
            "ytrJFr5XLJ/nSFtVKxDL5wW6hnEJY/A+p68YwBi8T2k74hPdTd7etVTsJxH6cvHjjLYEPtFPdefww6MVB2i1sbz/8ujLIhUJL/W/" +
            "elnybcAGZls1xw3fX0L+l3qwwXPzUhESDsfxUuX7KrSw0RAxuw6ux7dnNC1SnVND3WBZut6qKINlaYYuTHnH0y6Z/7jEO5YYeuPS" +
            "S38+a3hgCa8bibCGB5fgvCqMNQZwKe/1W/GOh5aim78q3vHQzm7Hd4t3PDiiaDYZLI/WJ5oEJs+jAxvNIaNp0H5Cw4F3PFpOaDjw" +
            "jkfLCQ0H3vFoOaHhwDseLSc0HHjHw520k3LBGh6txBmQFWt4aiMtuUane9rC9y3hFBuB0z2m8E8s6aXTN04f6BtjyDU6faCv7FXA" +
            "Gp7oxNtWJKzhydLVW4QW7wxQNaPRZLAGtD/QIDB5A7qw0Bgymh6dWGgMGU2PTiw0hoymRycWGkNG06MTC40hn5EI5WzY5fHsAAAA" +
            "AElFTkSuQmCC"},
        {"cc0-jpeg-q60-prepared-3-s1-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACJUlEQVR4Xu3TW27cQBBDUe1/0w5iPZrFqksNBhjFBnK+MsUW+eNs" +
            "X+/ZOn9i7nLiM9v/pdf5yjd/VN3EwDcO/qzIKbDy+ovEEKzm42v/PUoZacXrEOpChFrvOoS6EJGhdzg1nKChdjg1nKChVk5YiAGa" +
            "WqebwwBNrdPNYcCGVjlhIQZsaJUTTtE9GFr1RI10T3qrXB5cKk8FBkGvlQsWYhD0WrlgIQZBr5ULFmJw47mlv55bEi9M0T1rlfdD" +
            "7y310g8t9dZ1+dTS8BdRXipOWO/tl44TJLX/cqm8LEKEWm87TFJGvPelobeW7P/TGoplMSTSLDN56L2l2n/xV9VNTHxku9v5nUvb" +
            "zVhOkW/s/FWRU+QbO39V5BT5xs5fFTlFvrHzV0VOmY9880dFTpFvnPydiCHy5lemQsSG3uFkOGFz63RTnLAfsoSFGATXUvl4uikM" +
            "2Boap/SkMGC6JJ/3S4UB0yEpbgeDQaBDq7v8GHDCyszZrv8ecRLoivGnS4iY1y/+UqQs8IWDP1MxBN4vvngNg8DrK399wiDw7spf" +
            "nzAIvLvy1ycMglUqAxd/fcIgqJ26sq4dBswrdUbvhu7BUPjcUpkqwUJ3Nhc+tyRTFpzozqjwc0v+Jd1PdA/mxmuIGukezJXzVWAQ" +
            "TKXXDQsxCFbr3mw/ASdBqTb+9sJJ4vUXfyhSFvjCwZ+pGDKfOPgzFUPmEwd/pmLIfOLgz1QMmU8c/Jn6A6htj+xwTp36AAAAAElF" +
            "TkSuQmCC"},
        {"cc0-jpeg-q60-prepared-3-s1-sys1-m4-st2", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACIklEQVR4Xu3SQY7jQAxD0dz/0mnAcUoSpU+njdirvNVELJPAzDye" +
            "Jz0afSGOcqQ7v6XP6cqLvip8Ckp7+WHYEGh3/UVsCKJ5/1p/j1xGovj9dTsMTESidn3cL52JSNQOS9zHCRpq0wkLMWBDazphIQZs" +
            "aE0nLMSADa3phIUYsKE1nbAQAza0phNO0d0YStPpoqV37XBq6G4Mtf3SYcBy7W/plPuWNvctPVfvB1N091rl8dC5pV560VJvjctV" +
            "S8M/U3mZccJ6b790nKBU25fKy8JEJGqvXuq97TBxGdHej4a+sRRDtsyGJDWnGT90bqn2L/qqOoiRrhztfHPpaOsgRrqy0UeFT5mO" +
            "bPRR4VOmIxt9VPiU6chGHxU+ZTqy0UeFT5mObPRR4VNS6/OfmQ2RVMeUqTMRm3qHU8UJW0Pl4+FUcMLGofuXsBADYy1Nf3tYiIER" +
            "S//5L4GBkZbS5/1SYWDkpfi+HQQGRh6K6vqr44SVnXd7/vOIE5ZHlL4NJmJaH/Rl4jJDF3b6LLMh0P5CHy+cMC0v9PHCCdPyQh8v" +
            "nDAtL/TxwgnT8kIfL5yw2ll24txgwLQyz+S7oLsxFF6zNBbet5SnarDQnUHhfUsxpcGO7owa6f5GdwMq4bzQ3Zgr52uCgTGVrhsW" +
            "YmBE66s3/+Y+ToxSXenTYCJD+xd9mLjM0IWdPstsyHRip88yGzKd2OmzzIZMJ3b6LLMh04mdPsv+ALn0lOcSn9cVAAAAAElFTkSu" +
            "QmCC"},
        {"cc0-jpeg-q60-prepared-4-s1-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACPElEQVR4XtXQUYojMRRD0ex/0zPETmzpWlZBwzTM+SmwxBPU68/P" +
            "vCK2VA0LbkxsqRoW3JjYUjUsuDGwZHp6xY2JLdPTG058sadqeMOBhUVVwwve39hUNcx4XrCqapjxvGBV1TDidcGq6WnC84JV09OE" +
            "5wWrpqcJzwtWTU8j3hesqhpmPC9YVTXMeH4az6yqGmbcGMY7m6anGVc+E7+29Plc9TTjzsKi6WnGgYVF09OKO//9EkcGlkxPr7gx" +
            "sWV6esOJL/ZUDW84sLCoanjB+xubqoYXvL+xqWqY8fzGpulpxPsbm6anCc8LVk1PE54XrJqeJjwvWDU9TXhesGp6mvC8YNX0NOL9" +
            "6f3OpulpxI23zzurqoYZV4b5zKqqYcaRjU3T04j3NzZNTyPe39g0PY14f2PT9PSOIwNLpqcFV97YMT294sbElunpDSe+2FM1vOHA" +
            "wqKq4QXvb2yqGl7w/samqmHG8xubpqcR729smp5GvL+xaXqa8Lxg1fQ04XnBqulpwvOCVdPThOcFq6anEe9P451VVcOMG8N4Z9P0" +
            "NOPK58avLa3Fi55mHFpYND3NOLCwaHqarb9F6LmeNpx5/aMljgwsmZ5ecWNiy/T0ggsLi6qGNxxYWFQ1vOD9jU1Vw4znBauqhhnP" +
            "C1ZVDTOe39g0PY14f2PT9DThecGq6WnC8xub7iEOeH9hEZ7yEwe+2KPHwokTE1uH58aBGxNbh+fGgRsTW4fnxoEbE1uH58aBGxNb" +
            "h+fGgRsTW4e/flevvroEl88AAAAASUVORK5CYII="},
        {"cc0-jpeg-q60-prepared-4-s1-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACQElEQVR4Xr3N0W4DIRBD0fz/T7eCJYAvZhxVSs/LStg7fv38zcth" +
            "SdTpHUc6lkSd3nGkY0nU6R1HGnZUiC+48WBLhdjjxBt7ok49Dkwsijr1ODCxKOrU4v2FTVGnDs9vWBV16vD8hlVRpw7PL2yqEJ94" +
            "fsOqCvGJ5zesqhCfeH7DqgqxwfsLmyrEBu8vbKoQOxzo2jOLKsQOR8bEvy2Nz12IHe5MLKoQOxyYWFQhdsYv3PnC0sCdby1xpWFH" +
            "hfiGIw+2RJ3ecGJgTdTpBRfe2BN16nFgYlHUqcX7C5uiTh2e37Aq6tTh+Q2rok4dnl/YVCE2eH9hU4XY4P2FTRVig/cXNlWIDd7f" +
            "sCrq1OH5R3tnU4XY4UjTn1lUIXa40vRXFlWIHa5MLKoQOxyYWFQhdjgwsahC7HBgYlGF+IorDTsqxFdcadhRIb7hSMeSCrHHiTf2" +
            "RJ16HJhYFHVq8f7CpqhTh+c3rIo6dXh+YVOF2OD9hU0VYoP3FzZViE88v2FVhfjE8xtWVYhPPL9hVYXY4P2FTRVig/cf/ZlVUacW" +
            "R5r+zKIKscOVMfFvS+NzF2KHOxOLKsQOByYWVYid8Qt3vrD04MzrS0sc6VhSIb7gxsCaqNMLLryxJ+rU48DEoqhTjwMTi6JOLd5f" +
            "2BR16vD8hlVRpw7Pb1gVderw/IZVUacGr29YVSE+8fzCJqT8wPsTixQLxIGJRYqFAxcG1g65ceBGw47xSQe40rBjfNIBrjTsGJ90" +
            "gCsNO8YnHeBKw47xC6B7zKFuRgYpAAAAAElFTkSuQmCC"},
        {"cc0-jpeg-q60-prepared-4-s1-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACNklEQVR4XtXNQW4DMQxD0dz/0i3s6YzMb1oMugiQt/DCJMTXz/+8" +
            "HJZEn55xZGJJ9OkZRyaWRJ+ecWRgR4X4hCMXtkSfHnDhxp7oU48DDxZFn3ocKGyu2tDj+cKm6FOL9wubok8dnl+wKvrU4fnCpgqx" +
            "wfuFTRXiHc8vWFUh3vH8glUVYoP3C5sqxAbvFzZViB0OTOObRRViiyvXxseWrrcRYos7N/ZUiC0u3NhTIW5w5/uXuDKwo0J8wpGJ" +
            "JRVijxM39kSfehx4sCj61ONAYXPVhge8X9hctaHH84VN0acOzy9YFX3q8HxhU4XY4P3CpgqxwfuFTRVig/cLmyrEBu8vWBV96vD8" +
            "ZfyzqULscGSY3yyqEDtcGeYviyrEDlceLKoQOxx4sKhC7HDgwaIKscOBB4sqxEdcGdhRIT7iysCOCvEJRyaWVMotTtzYE33qcaCw" +
            "uWrDA94vbK7a0OP5wqboU4v3C5uiTx2eX7Aq+tTh+cKmCrHB+4VNFWKD9wubKsQG7xc2VYgN3i9sqhAbvH+Z/6yKPrU4MsxvFlWI" +
            "Lc5cNz62dA8ehdji0I09FWKLCzf2VIjPOPP6/iWuDOyoEJ9w5MKW6NMTTvxhTfTpARceLK7a0OP5BaurNvR4fsHqqg09ni9sij51" +
            "eH7BquhTh+cLmyrEO55fsKpCvOP5wiakfMP7hU1I+Y4DN/YoFjZc+MPaJjc23JhY2r1RIY5MLO3eqBBHJpZ2b1SIIxNLuzcqxJGJ" +
            "pd0vPQ3Smxoo3AEAAAAASUVORK5CYII="},
        {"cc0-raster-150-input-s1-sys1-m2-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACLUlEQVR4Xu2SUW7tQAxCu/9Nt9JVHTA2ZH6a+/rU82UDg6UoH5/n" +
            "fDSmM1Mtw8sN3PF36aXzEuGG3tIVzrQUxht6wzsuTW0qLxXjDfyeO6Y2lZeKMcKve8dUdf/OYYzw694xVd2/cxgD/LbYPVWoA2OA" +
            "3xa7pwp1YAzw22L3VKEOjAF+W+yeKtSB0YJXWwdrmvnXL2EaHaRhLuVKYbTgjekgDXMpVwqjBW9MR+RK4YEFL7aGv0sZvNgafucl" +
            "sDV4ve+/69KmQb12Nm9B57svsT5SGA/YGp+7xNqE3mI8YOtgbUJvMR6wdbA2obcYD9g6WJvQW4wHbB3Y2IV65Xi5ZWvBxi7UK8eL" +
            "RRvA/35Jc9LRV0NvYJ67lJCOvhq04wzp6KtBO86Qjr4atOMM6ejrIdpZaI6JpkUvFJpjomnQ9r45gmXh5qcuZa1jjcDWumkda1jQ" +
            "2Vs3jbGG5T2Xtu8HpWMNC9958hKadVesYeEr3Ky7Yo0A36l2nnesEUAr2nnesUYArRPNgmBZtJ3RLAiWRds7mi6sEdDujqYLawS0" +
            "u6PpwhoB7e5ourBGQLs7mi6sEdDujqYLa1i0EzurE2tYtBM7qxNrWLZGvuRuGTmw9fGdn73Ub3WnMHJg73vu0vxTFCMHXN/PXXL/" +
            "BKuMNSx7564y1gigtXqnMrFGYPZOZWKNAPcqmgXBsmg7o1kQLIu2M5oFwbJoO6NZECyLtjOaBcGyaDujWRAsi7YzmgVfLXqGvQgq" +
            "4AIAAAAASUVORK5CYII="},
        {"cc0-raster-150-input-s1-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACGklEQVR4XtXS0WpDQQiE4b7/S7ccFlF/nVkoJCHfVdfxjFDy8/s/" +
            "PwvudJdY4pUHd7pLLPHKgzvdJZZ45cGd7hILvBG4V9lQ4oXAvcqGArv53plIYjPfOxMJW++cTCYSsvWzl/QtGUhb6zYjGUhb6zYj" +
            "GUhb6zYjGUhb6zYjGUhb6zYjGUi19fT2ibolxsZs7ZNXXdrwi0OMDfZO/OIQY4O9E784xNhg78QvDjE22Dvxi0OMDfZO/OIQ4ws2" +
            "99dOBlZtfuWl2ZqT778UvXMyycCYvXMyycCYvXMyycCYvXMyycBgL987E0ls5ntnIqn35svd+Z5LHfe6S7zihcC97hILvPHgDl0X" +
            "Vrzy4A5dF1a8ErhX2VDihcC9yoYSLwTuVTaUeCFwr7KhxAuBe5UNJV4I3KtsKLC9vxQTSbX5XZf8rJOBsbVus04Gxt66zSoZGJ+4" +
            "tP3/ctLJwPjMpWzmm2Rg1DvvvHTa6987GRjZmu31750MjGyduJtMJLG94m4ykcT2jttBBga7O24HGRjs7rgdZGCwu+N2kIHB7o7b" +
            "QQYGuztuBxlI7Mx3nU4ykNiZ7zqdZCBtjfWSuiXGxtZX77z2Ur/VkyDGxt73vkvzl0JibKi+111Sv4k6rWQg7Z37tJKBka3ROyeT" +
            "DIzZOyeTDKza3HEzmchgf+JmMpHB/sTNZCKD/YmbyUQG+xM3k4kM9iduJhMZ7E/cTH9c15nUzUnFRgAAAABJRU5ErkJggg=="},
        {"cc0-raster-150-input-s2-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB7klEQVR4Xu3N0W4jMQxD0f7/T++iXjumrkXOtCiySJHzNKY01Mef" +
            "b/mY9Hu9jTQL3peGNLNq836tpOUnQe3dr5W0/CRgs77tLZdH7NX3T146e8+k4fLg7D2ThsuDs/dMGi4Pzt4zabg8OHvPpOHy4Ow9" +
            "k4bLI/bq+3dc0pe981KXTtyeXB6xu+L25PKI3RW3J5dH7K64Pbk8YnfF7cnlEbsrbk8uv9R1v/al88pONREuv/CsS/uONvMNLo/0" +
            "zjMvEbcnl0fsrrg9uTxgc8XtBz+x2F1x+8FPrN2pFxZuP/iJtTv1wsLtBz+xtFNvrMxIM0Nb9crKjDQztFWvrMxIM0Nb9crKjDQz" +
            "2K64K9LMYLvirkgzg+2KuyLNDLYr7oo0i3jjE3eKPA145RN3ijwNeOXizvtSwTvPu8T54XrDeNalL995uUvrza0iT63/dWnhVpGn" +
            "Fm/8w60iTw1eWLhX5KnBCwv3ijxtsX/jZpGnLfZv3CzytMX+jZtFnrbYv3GzyNNWbd6v173E3v3+DZc8/jP0acDWDv8Z+jRga4f/" +
            "DH0asLXDf4Y+Ddja4T9DnwZs7fCfoU+/4PLCkqc3vC99h14g7g59egPbFXeHPr2B7Yq7Q5/ewHbF3aFPb2C74u7QpzewXXF36NMb" +
            "2K64O/wFxUub0hoRinIAAAAASUVORK5CYII="},
        {"cc0-raster-150-input-s2-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAAB8klEQVR4Xu3NW24DMQxD0ex/0y2MgSDpWqSnRR9IkPM3Jod6fHzH" +
            "Q2CvsqHEC4G9yoYSLwT2KhtKvBDYq2wocL9iN5lI4nrFbjKRxN38foVL13L91rdkYHC3fv/kpWl3f9nJQKqrv3tp363fz3+pf8XL" +
            "zERS382veJmZyOr7ib1kIosXAnvJRAb3K3aDDCyuV+wGGVhcr9gNMrC4XrEbZGBxvWI3yMDiesVukIHVN1/hElfzO992MjC4mt/5" +
            "tpOBNO3uLzsZSHX1femr6uq1u79MZCDtu/vLRAYGV/Nb33meSzt2k4kkrlfsJhMZ3A/sVTaUeCGwV9lQ4oXAXmVDiRcCe5UNJV4I" +
            "7FU2lHghsFfZ0OCNhZ3uEEu8srDTHWKJVxZ2ukMs8crCTneIJV453XnGS8x3Nyqj/7rEdHKrNPirS/1OYKs7xAJvXNjqDrHAGxe2" +
            "ukMs8MaFre4QC7xxYas7xAJvXNjqDvGA+4nN7hAPuJ/Y7A7xgPuJze4QD7if2OwO8aAv59fzXuJufr/CJYd/LeOjwc0Z/1rGR4Ob" +
            "M/61jI8GN2f8axkfDW7O+NcyPhrcnPGvZXz8gtN+OsRH70vfUS/s2F7Gxxu43bG9jI83cLtjexkfb+B2x/YyPt7A7Y7tZXy8gdsd" +
            "28v4eAO3O7aXT/oOldiaOKUOAAAAAElFTkSuQmCC"},
        {"cc0-raster-150-input-s2-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACI0lEQVR4XsXS0WpjUQxD0f7/T7eUYGxvWbrDQNL1dI9lZAj5+v5f" +
            "XydutRA94I0XbrUQPeCNF261ED3gjRdutRBFvFC410IU8ULhXgtRwG6+LzaI2Mz3xQaB9upE2SDQXp0oGwTaqxNlg0B7daJsEGiv" +
            "TpQNAu3VibKBNVv10t6dQmR0azfzfQmR0a2Ku1MMT2yfuDvF8MR24n6xgcVm4n6xgcVm4n6xgcVm4n6xgcVm4n6xgcVm4n6xgXX1" +
            "6kTZwJqtn7/0L7fMOJid7710t16zzYyjq/WabWYcXa3XbDPj6Gq9ZpsZR9qqE2WDQHt1omwQsZnviw0iNvN9scGD2T1xr4Uo4oXC" +
            "vRaiB7zxiztTDCNe+cWdKYYWLxTuTTG0eKFwb4qhxQuFe1MMLV4o3JtiaPFC4d4UQ4sXCvemGBps3y8nRNZs/tSlPNtsEFyt12yz" +
            "QXC3XrPJBsFfXLp+v55sNgj+5lI38002COadT156tc/vmw2Cbu32+X2zQdCtirstRBbbJ+62EFls37hdbBCwe+N2sUHA7o3bxQYB" +
            "uzduFxsE7N64XWwQsHvjdrGBxc5+z6mygcXOfs+psoF1Nc5L7pYZB1ffvPPeS/vWTooZB3ff5y7pP4XMOHB977vk/hNzOtnAujvv" +
            "6WSDoFurVyfKBoH26kTZIJrNGzdbiAL2N262EAXsb9xsIQrY37jZQhSwv3GzhShgf+NmC1HA/sbN9gM8vJHcpNXiOAAAAABJRU5E" +
            "rkJggg=="},
        {"cc0-raster-150-input-s3-sys1-m2-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACLUlEQVR4Xu2SUW7tQAxCu/9Nt9JVHTA2ZH6a+/rU82UDg6UoH5/n" +
            "fDSmM1Mtw8sN3PF36aXzEuGG3tIVzrQUxht6wzsuTW0qLxXjDfyeO6Y2lZeKMcKve8dUdf/OYYzw694xVd2/cxgD/LbYPVWoA2OA" +
            "3xa7pwp1YAzw22L3VKEOjAF+W+yeKtSB0YJXWwdrmvnXL2EaHaRhLuVKYbTgjekgDXMpVwqjBW9MR+RK4YEFL7aGv0sZvNgafucl" +
            "sDV4ve+/69KmQb12Nm9B57svsT5SGA/YGp+7xNqE3mI8YOtgbUJvMR6wdbA2obcYD9g6WJvQW4wHbB3Y2IV65Xi5ZWvBxi7UK8eL" +
            "RRvA/35Jc9LRV0NvYJ67lJCOvhq04wzp6KtBO86Qjr4atOMM6ejrIdpZaI6JpkUvFJpjomnQ9r45gmXh5qcuZa1jjcDWumkda1jQ" +
            "2Vs3jbGG5T2Xtu8HpWMNC9958hKadVesYeEr3Ky7Yo0A36l2nnesEUAr2nnesUYArRPNgmBZtJ3RLAiWRds7mi6sEdDujqYLawS0" +
            "u6PpwhoB7e5ourBGQLs7mi6sEdDujqYLa1i0EzurE2tYtBM7qxNrWLZGvuRuGTmw9fGdn73Ub3WnMHJg73vu0vxTFCMHXN/PXXL/" +
            "BKuMNSx7564y1gigtXqnMrFGYPZOZWKNAPcqmgXBsmg7o1kQLIu2M5oFwbJoO6NZECyLtjOaBcGyaDujWRAsi7YzmgVfLXqGvQgq" +
            "4AIAAAAASUVORK5CYII="},
        {"cc0-raster-150-input-s3-sys1-m2-st2", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACLUlEQVR4Xu2SUW7tQAxCu/9Nt9JVHTA2ZH6a+/rU82UDg6UoH5/n" +
            "fDSmM1Mtw8sN3PF36aXzEuGG3tIVzrQUxht6wzsuTW0qLxXjDfyeO6Y2lZeKMcKve8dUdf/OYYzw694xVd2/cxgD/LbYPVWoA2OA" +
            "3xa7pwp1YAzw22L3VKEOjAF+W+yeKtSB0YJXWwdrmvnXL2EaHaRhLuVKYbTgjekgDXMpVwqjBW9MR+RK4YEFL7aGv0sZvNgafucl" +
            "sDV4ve+/69KmQb12Nm9B57svsT5SGA/YGp+7xNqE3mI8YOtgbUJvMR6wdbA2obcYD9g6WJvQW4wHbB3Y2IV65Xi5ZWvBxi7UK8eL" +
            "RRvA/35Jc9LRV0NvYJ67lJCOvhq04wzp6KtBO86Qjr4atOMM6ejrIdpZaI6JpkUvFJpjomnQ9r45gmXh5qcuZa1jjcDWumkda1jQ" +
            "2Vs3jbGG5T2Xtu8HpWMNC9958hKadVesYeEr3Ky7Yo0A36l2nnesEUAr2nnesUYArRPNgmBZtJ3RLAiWRds7mi6sEdDujqYLawS0" +
            "u6PpwhoB7e5ourBGQLs7mi6sEdDujqYLa1i0EzurE2tYtBM7qxNrWLZGvuRuGTmw9fGdn73Ub3WnMHJg73vu0vxTFCMHXN/PXXL/" +
            "BKuMNSx7564y1gigtXqnMrFGYPZOZWKNAPcqmgXBsmg7o1kQLIu2M5oFwbJoO6NZECyLtjOaBcGyaDujWRAsi7YzmgVfLXqGvQgq" +
            "4AIAAAAASUVORK5CYII="},
        {"cc0-raster-150-input-s3-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACGklEQVR4XtXS0WpDQQiE4b7/S7ccFlF/nVkoJCHfVdfxjFDy8/s/" +
            "PwvudJdY4pUHd7pLLPHKgzvdJZZ45cGd7hILvBG4V9lQ4oXAvcqGArv53plIYjPfOxMJW++cTCYSsvWzl/QtGUhb6zYjGUhb6zYj" +
            "GUhb6zYjGUhb6zYjGUhb6zYjGUi19fT2ibolxsZs7ZNXXdrwi0OMDfZO/OIQY4O9E784xNhg78QvDjE22Dvxi0OMDfZO/OIQ4ws2" +
            "99dOBlZtfuWl2ZqT778UvXMyycCYvXMyycCYvXMyycCYvXMyycBgL987E0ls5ntnIqn35svd+Z5LHfe6S7zihcC97hILvPHgDl0X" +
            "Vrzy4A5dF1a8ErhX2VDihcC9yoYSLwTuVTaUeCFwr7KhxAuBe5UNJV4I3KtsKLC9vxQTSbX5XZf8rJOBsbVus04Gxt66zSoZGJ+4" +
            "tP3/ctLJwPjMpWzmm2Rg1DvvvHTa6987GRjZmu31750MjGyduJtMJLG94m4ykcT2jttBBga7O24HGRjs7rgdZGCwu+N2kIHB7o7b" +
            "QQYGuztuBxlI7Mx3nU4ykNiZ7zqdZCBtjfWSuiXGxtZX77z2Ur/VkyDGxt73vkvzl0JibKi+111Sv4k6rWQg7Z37tJKBka3ROyeT" +
            "DIzZOyeTDKza3HEzmchgf+JmMpHB/sTNZCKD/YmbyUQG+xM3k4kM9iduJhMZ7E/cTH9c15nUzUnFRgAAAABJRU5ErkJggg=="},
        {"cc0-raster-150-input-s3-sys1-m3-st2", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACGklEQVR4XtXS0WpDQQiE4b7/S7ccFlF/nVkoJCHfVdfxjFDy8/s/" +
            "PwvudJdY4pUHd7pLLPHKgzvdJZZ45cGd7hILvBG4V9lQ4oXAvcqGArv53plIYjPfOxMJW++cTCYSsvWzl/QtGUhb6zYjGUhb6zYj" +
            "GUhb6zYjGUhb6zYjGUhb6zYjGUi19fT2ibolxsZs7ZNXXdrwi0OMDfZO/OIQY4O9E784xNhg78QvDjE22Dvxi0OMDfZO/OIQ4ws2" +
            "99dOBlZtfuWl2ZqT778UvXMyycCYvXMyycCYvXMyycCYvXMyycBgL987E0ls5ntnIqn35svd+Z5LHfe6S7zihcC97hILvPHgDl0X" +
            "Vrzy4A5dF1a8ErhX2VDihcC9yoYSLwTuVTaUeCFwr7KhxAuBe5UNJV4I3KtsKLC9vxQTSbX5XZf8rJOBsbVus04Gxt66zSoZGJ+4" +
            "tP3/ctLJwPjMpWzmm2Rg1DvvvHTa6987GRjZmu31750MjGyduJtMJLG94m4ykcT2jttBBga7O24HGRjs7rgdZGCwu+N2kIHB7o7b" +
            "QQYGuztuBxlI7Mx3nU4ykNiZ7zqdZCBtjfWSuiXGxtZX77z2Ur/VkyDGxt73vkvzl0JibKi+111Sv4k6rWQg7Z37tJKBka3ROyeT" +
            "DIzZOyeTDKza3HEzmchgf+JmMpHB/sTNZCKD/YmbyUQG+xM3k4kM9iduJhMZ7E/cTH9c15nUzUnFRgAAAABJRU5ErkJggg=="},
        {"cc0-raster-150-input-s4-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACIklEQVR4Xp3NMW4EAQzDwPz/00lxOJji2log04qwf37/5WflKvT1" +
            "5B8frkJfT/7x4Sr09eQfH65CXw/+MFxC206+P1xC2w6+Tm6hbQdfJ7fQtoOvk1to28HXyS20beXb5Db0deHr5Db0deHr5Db0deHr" +
            "5Db0deHr5Db0deHr5Db0deHr5Db0deHr5Db0deHryTW0beXbyTW0beXbyTW0beXbyTW0beXbyTW0beXbyTW0rfCHL3fQtsIfvtxB" +
            "206+P1xC206+P1xC2w6+Tm6hbQdfJ7fQtoOvk1to28HXyS20beXb5Db0deHr5Db0deHr5Db0deHr5Db0deHr5Db0deHr5Db0deX7" +
            "w2Xo68r3h8vQ15Xvk1to28HXyS207eDr5BbadvB1cgttK/zhyx20rfCHL3fQtpPvD5fQtpPvD5fQtoOvk1to28HXyS207eDr5Bba" +
            "dvB1cgttW/k2uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6uQ19Xfg6" +
            "uQ19PfnHh6vQ15N/fLgKfT34w3AJbTv5/nAJbTv4OrmFth18ndxC2w6+Tm6hbQdfJ7fQtpVvJ9fQtpVvJ9fQtpVvk9vQ14Wvk9vQ" +
            "14Wvk9vQ14Wvk9vQ14WvD5fytj/4/nApb/uD7w+X8rY/+P5wKW/7g+8Pl/K2P/j+cClv+4PvD5fyB0CJvqExUpr0AAAAAElFTkSu" +
            "QmCC"},
        {"cc0-raster-150-input-s4-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHElEQVR4Xp3NQW5gMQzD0N7/0p1FEXyakRVg3laE/fP7P34W7qiO" +
            "K3843FEdV/5wuKM6rvzhcEd1XPj+xyXVceH7H5dUx8jXyS3VMfJ1ckt1jHyd3FIdI18nt1THyNfJLdUx8nVyS3UMfHtyTXUMfHty" +
            "TXUMfHtyTXUMfHtyTXWMfJ3cUh0jXye3VMfI18kt1THydXJLdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1ckt1XPnD4Y7quPKHwx3VceH7" +
            "H5dUx4Xvf1xSHSNfJ7dUx8jXyS3VMfJ1ckt1jHyd3FIdA9+eXFMdA9+eXFMdA9+eXFMdA9+eXFMdA9+eXFMdA9+eXFMdI18nt1TH" +
            "yNfJLdUx8nVyS3WMfJ3cUh0jXye3VMfI18kt1XHlD4c7quPKHw53VMeF739cUh0Xvv9xSXWMfJ3cUh0jXye3VMfI18kt1THydXJL" +
            "dQx8e3JNdQx8e3JNdQx8e3JNdQx8e3JNdQx8e3JNdQx8e3JNdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1ckt1jHyd3FIdI18nt1THwj/+" +
            "uKI6Fv7xxxXVceUPhzuq48ofDndUx4Xvf1xSHRe+/3FJdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1ckt1DHyb3E6P+eLr5HZ6zBdfJ7fT" +
            "Y774OrmdHnPg+4c7ewYXfzjc2TO4+MPhzp7BxR8Od/YMLv5wuLNncPGHw509g4s/HO7sHyNpuKdhH9k4AAAAAElFTkSuQmCC"},
        {"cc0-raster-150-input-s4-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACGElEQVR4Xp3NQY4DQQzDwPz/09lDYDSHI6uBrasI+/P9j8/CHdVx" +
            "5Q/DHdVx5Q/DHdVx5Q/DHdVx4fuHS6rjwvcPl1THhe8fLqmOC98/XFIdI18nt1THyNfJLdUx8nVyS3WMfJ3cUh0jXye3VMfI18kt" +
            "1THydXJLdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1ckt1jHyd3FIdI18nt1THyNfJLdUx8nVyS3WMfJ3cUh1X/jDcUR1X/jDcUR0Xvn+4" +
            "pDoufP9wSXWMfJ3cUh0jXye3VMfI18kt1THydXJLdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1ckt1jHyd3FIdI18nt1THyNfJLdUx8nVy" +
            "S3WMfJ3cUh0jXye3VMfI18kt1THydXJLdVz5w3BHdVz5w3BHdVz4/uGS6rjw/cMl1THydXJLdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1" +
            "ckt1jHyd3FIdI18nt1THyNfJLdUx8nVyS3WMfJ3cUh0jXye3VMfI18kt1THydXJLdYx8ndxSHSNfJ7dUx8jXyS3VsfCPH1dUx8I/" +
            "flxRHVf+MNxRHVf+MNxRHRe+f7ikOi58/3BJdYx8ndxSHSNfJ7dUx8jXyS3VMfJ1ckt1jHz9cPl0mQPfP1w+XebA9w+XT5c58P3D" +
            "5dNlDnx/uLNr8OIPw51dgxd/GO7sGrz4w3Bn1+DFH4Y7uwYv/jDc2TV48Yfhzv4Apzbke3AjjkUAAAAASUVORK5CYII="},
        {"cc0-raster-300-input-s1-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACT0lEQVR4Xu2SW25jQRBCs/9Nz8iPrqYo4LY1iRNFc776AgWy5I8/" +
            "L/ABJE8HWEjEImYEWEjEImYEWAikouY9oUT/jKQalHXmhaUqEC1dlKHzpTqfJbw0vu8afkTWtSphcQg3Dd6ROp4d8yeo1GcsTW0q" +
            "50t1qzqGpmKHS/tUVExNSK8t4SB7TdsSaPVKrCPV8L6lY61egbqRDadavQJ1IxuyVuLJ0r4QBVfaS0v3w3pQwZX2f6kORMGV9lOX" +
            "ClFwpf3Gpa3V6wTZcKrV6wTZcKrV6wTVAKKQfuMSxPbzgN2Ad1NTsS9ewhS8r6mG1jGLh3DT4H1NNRwutRB+XFKVvYSbZehzl56q" +
            "zrQvRzUNOIAfUHBT+6ehigYhgQW3BH1ruGTjM9t5+ixIuOWE0cGChFtOGB0sSLjlhNHBgoRbThgdLJzCxXc4hETTwxMLzgHJ81B5" +
            "/zIEKzCLp8J4J6BqhdTxTkC2ShGwRmB10q0UN9YI1JL8UU0DrBH4jqV2LSTEGgFYwuIhdKwRaEtVwN+MNQK4s7v718QaCZxZ9ftl" +
            "8E4ANxjOFt5JcP2Gk5tgJXhgwTkgeRbub3B4YY0Elzc4vLBGgssbHF5YI8HlDQ4vrBHATtzYqsIagV7ZZkBnnB6YjbjzBUtKm8bG" +
            "6QFZ+L4lmCJj4fSAKXzfkvinNJwecI1Of+L0gGlcMusLpydk5dXQPy1hq9I61kjs2uf5EATeSUAzw9HCOxHuX3AOSF6CJx5wColm" +
            "gDcecAqJZoA3HnAKiWaANx5wColmgDcecAr5C6TLIz1JZPZUAAAAAElFTkSuQmCC"},
        {"cc0-raster-300-input-s1-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACbUlEQVR4Xu2QUW4cQRBCff9LJ3K2uwuogh0rM44s5X1NAQ3a/fj1" +
            "BT4A7xwkwmcm1ExIhM9MqJmQCJ8RX0NOwRm6Ir6FJBvCI4MVVCKKS8F3Bguoo2klPLYU/7/LS/icKrrahJdanxF8zRVdbcJLrc8I" +
            "vuaKrjbhpdZnYj2bKkD7UUv71VQB2ncvNWHl6tNzHs0VIsKN7VeW6pHpILGuJewUfDvGlh7ov4hDeBjGGslsICEh9wKpR6ZEPaR8" +
            "yDrqxdSwAQ8pH7KOejE1bMBDyoeso15MDRvwkPIh+56pIXiU6y8SQ1tzRKurjAtUn7wbZRbuWRrFJ5YGaatw1ucFagjfzerDS/hv" +
            "SeyGJRDBoeOPUJ8XaH0iNip1wxJoHXwL3++ZOs4J5qGe3rWk7uKEPl083jKV1Anu4oQ+XTzeMpXACXaJJ0eXQysKWtIcd9DlkAYg" +
            "LUkHnwZuQHiJgtTw90vE/6URLAjcsNTByoOGiOxadGOhMSSaFi7ny5E8Sy/uSiNYlrF20gjveMalrZKGeMczDj2xtIe0dRQLa3j+" +
            "xRLVDhJhDQ8uwfOuMNbw4BA0y9nwjoeGdjkdE97x4Mquh0+Ddzww0dBsESyP1heaBJLn0YGN5pBoGrSf0PDBOx4tJzR88I5HywkN" +
            "H7zj0XJCwwfveKAUNxaa3ljDI5W4AnLDGp7WSEO20BqWqRGXXKPTPWPf9y3hljoLp3tM33NL7eFPXnJTRj5Yw7Mr+emsAtYITKWT" +
            "xnjHc1pPb1c63glAsaLRIlgB7T9oEEheQBcWGkOi6dGJhcaQaHp0YqExJJoenVhoDImmRycWGkN+A2Qs2HnTXxyNAAAAAElFTkSu" +
            "QmCC"},
        {"cc0-raster-300-input-s1-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACRUlEQVR4XtXSS27DUAxD0ex/0y1iv49E6fIVaT3oGcWiTMFoX18f" +
            "eildUMcFooeORccFooeORccFoHfedCc7xEBvDLqW+JTk8p+dsiHRan1uuYzs4nLJ1JkI1d46qUxEdm1zifs4QU1tMyo4QU3rHnEf" +
            "J6hp3SM+hQFrSvfooUvd3wkLMWBNaxhhIQastobJHiqaG7U1Tv7yUvmo8LxmFc0daVa6PtDc0Wqh6wPNHa0Wuj7Q3NFqoesDzU+k" +
            "PP4GGByl7v2AhRh42vnUpdr67y81tXVScMJ27Xy7TipOWO2tk4oTVnvLoGEitHvH22XQMREqxfrcchnRYn3u2ZB8cuh3l94v7ztP" +
            "XEr1m25lh5jokdfpzseX6i3Ni/NGTw9ddCnxKdMjF11KfMr0yEWXEp8yPXLRpcSnTI9cdCnxKdIbg65FNkS5PD8Rl6FaXCeFiVhX" +
            "280STtgsze92s4gTZi9hIQZGf+n0URgY61L7UWkWYGDsS93/RBxFGBjhUni9TjIMjHhpN8tjwQlLh2Z5euhwwuKVVR9+9jgx4hGh" +
            "q5uJmNZvuhm4jOmBSfciGxI9EOnuwomh7ZHuLpwY2h7p7sKJoe2R7i6csFAabwy6PWHApDJeCeMCA1Ya0yE8RXOjKXzmUlv45CUY" +
            "N8lAcwZ9z10qL/79peNHaTDQ3IBGGC8YsFmZX+2nAQZGV9rNMk6MVTuLy6DBiRGKla5uJjK0f9HFwGWOnrjpVmRDQ2/cdCuyoaE3" +
            "broV2dDQGzfdimxo6I2bbkXfmj+P7ESq+yoAAAAASUVORK5CYII="},
        {"cc0-raster-300-input-s2-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACHElEQVR4Xu2S0W7DMBRC+/8/vUmu63AxYCdtNk3aefOFcNRpj6+L" +
            "PBRcQmKYYEmDS0gMEyxpcAmJYYAdHa4BKQuU4T1ViAK0u6XySWDaPQ5+zyeBaRgOdtAGiWl4OghsEMDdnzbNf88ZGwRg9sd/03QQ" +
            "2CCAu/eapmF820EbJHgX3n7PJ4k6fLz6QeKTCM7uia6agCIKcyFaUh0DrnXcfQdWdLjWcfcdWNHhWsfdd2BFh2sdd99BrN9imjX9" +
            "CE/E3Zcoz/OIB8DdVyjPLSbpiaKLpi7i4emA2CACJpimJ2GDRBGN8fKYsUGiaAjuDnzi4fEClwc+8YzJ6njC5YFPPMciWV5nSYgs" +
            "x2a1vK6aEFlgs2qS6F0Tq2qxkDIHjm6L3jYVF5YmcqqB7QoXCznVsGDAxUJONSwYcLGQUw0LBlws5DTBmoXob5q4QKxyD4tWS6vc" +
            "c1J03XRW9DETxzMbFc1vmTgV7HQUp0WfMfU3dyqL2CJEC1VOLSh6jCe3Cjm1SFHeyqlFivJWTh1alLdy6tCivJVTDWw/+J/Qk1NN" +
            "GS+PRE41Zbw8EjnVlGUQ5a2cSuowiPJWTiV190YTzd5voucNJlx28DcNfQ3wqoK/aehrgFcV/E1DXwO8quBvGvqa4FkBf9LQ1xMs" +
            "DS9yusG/6R3uN6GB4W5DXzfgdYS7DX3dgNcR7jb0dQNeR7jb0NcNeB3hbkNfN+B1hLuNbyeDn9xvXq1bAAAAAElFTkSuQmCC"},
        {"cc0-raster-300-input-s2-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACAklEQVR4Xu3PS27DQAwDUN//0i0ydWKJ4sdJ6i6Cvk2RoUyi29dr" +
            "NgJvuhBLuHKDN12IJVy5wZsuxAJu3OFdZUMJFx7wsHCZ1GqvXMLesmTqTCSN3uPB1JlIGsXlQffpRCK982XSiVRqP2Zp9pYH3acT" +
            "bfSWB92nEwNqy46pM5Ehh0ydiay9tK3YoZeXxsaCR5UNHRy5wZvGpwau7PDsYCIPJ3Z4djCRhxM7PDuYyMOJHZ4dTHTW3y2dnNKJ" +
            "1UuvW8LaMqT7dKLN2uNF9+lEm73lRRbKwJi15UUWykAjteRpkIFGaufLpBOp1O5fzxdCJ9roLQ+6Tyca1h6/zdB7S/j7eCJMpLXW" +
            "uuPaXKa19gLvKhtqOPEDrxqfajiyhZ3/pQpntg9YIlt4AFIu4c5HLuEBigfKs0O/toTxdOKEgqETNSdOqKeHfmkJU+bUEfH00KtL" +
            "bejWkQdDrMDQ/a8TYoUNhaoQC3QoVIVYoEOhKsQCHQpVIeaO8q3/e06ImdLd4WEXYgYHHvCwCzGDAw942IWYac1lKFSFmGnFVy71" +
            "3guXoLcMhaoQT1B73VJtlvCjhb9qWErhRwt/1bCUwo8W/mpgK4PfLPzVwdoJv/ghnk/LC3chjv6X3nH9Ul0Y8HjhrxmWN3i88NcM" +
            "yxs8XvhrhuUNHi/8NcPyBo8X/ppheYPHyzfxIKbVs9y1/wAAAABJRU5ErkJggg=="},
        {"cc0-raster-300-input-s2-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACMElEQVR4Xt3S0W7jQAxD0fz/T+8CdsZDUbp0ka4LdM9TM1RIBOjr" +
            "z+dejV+oGN7wnd+/5CsHPxIpi3zjzc+2EEVe/hNL9rmeCU4iHOI+ThIv3p95CoOk9coDFmKQtNr98FuXeq28YCEGwVDbXxpO2K59" +
            "eGm3ypfbQ5MyIEvnt8+/8s73l8qH6CaeaLnxUxXDmdcbP18wYF5t/HzBgHm18fMFA+bVxs8XDNhQ3B4GGDCtXV/vLw0GTFp/cGl6" +
            "lFNF74GU/jdLY+v0VtF7MrUOTwaDYLc+vTTU7hcsxCBqve2h4yRpxe2h4ySy5i8MfbpUu78y9PGS/g9sfqRiGPnKKw99Y6lveV7d" +
            "xMhXDn5U5JT5yMGPipwyHzn4UZFT5iMHPypyynzk4EdFTpFvvPmZiiGq5fUTSRnqxf2lCRGbaqe3ghO2Sut3pzfFCYtLWIhBMC/d" +
            "/SgMgmtp/FHlTWAQ7KXpf0KfFAaBLMnX+0uFQaBLu9k+NpywMrTKy4cJJ0xXrnr5c8ZJoCPGT7cQMa/f/FKkjPnA4ncqhsQHlN9e" +
            "OAm8XfnthZPA25XfXjgJvF357YUTJqW68ebXCwbMKnVFnhsMWGssQzhF78FQ+MzSWPjkEjwPyRu9M+h7bql98d8v3f4oD97oPYBG" +
            "eL5gwFZl/er8KjAIptLpreIkuGpXcXsYcBJIsfPTLUSB91/8UKQs8YmTX6kYBr5x8isVw8A3Tn6lYhj4xsmvVAwD3zj5lfoLwQyH" +
            "9KOfOQYAAAAASUVORK5CYII="},
        {"cc0-raster-300-input-s3-sys1-m2-st1", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACT0lEQVR4Xu2SW25jQRBCs/9Nz8iPrqYo4LY1iRNFc776AgWy5I8/" +
            "L/ABJE8HWEjEImYEWEjEImYEWAikouY9oUT/jKQalHXmhaUqEC1dlKHzpTqfJbw0vu8afkTWtSphcQg3Dd6ROp4d8yeo1GcsTW0q" +
            "50t1qzqGpmKHS/tUVExNSK8t4SB7TdsSaPVKrCPV8L6lY61egbqRDadavQJ1IxuyVuLJ0r4QBVfaS0v3w3pQwZX2f6kORMGV9lOX" +
            "ClFwpf3Gpa3V6wTZcKrV6wTZcKrV6wTVAKKQfuMSxPbzgN2Ad1NTsS9ewhS8r6mG1jGLh3DT4H1NNRwutRB+XFKVvYSbZehzl56q" +
            "zrQvRzUNOIAfUHBT+6ehigYhgQW3BH1ruGTjM9t5+ixIuOWE0cGChFtOGB0sSLjlhNHBgoRbThgdLJzCxXc4hETTwxMLzgHJ81B5" +
            "/zIEKzCLp8J4J6BqhdTxTkC2ShGwRmB10q0UN9YI1JL8UU0DrBH4jqV2LSTEGgFYwuIhdKwRaEtVwN+MNQK4s7v718QaCZxZ9ftl" +
            "8E4ANxjOFt5JcP2Gk5tgJXhgwTkgeRbub3B4YY0Elzc4vLBGgssbHF5YI8HlDQ4vrBHATtzYqsIagV7ZZkBnnB6YjbjzBUtKm8bG" +
            "6QFZ+L4lmCJj4fSAKXzfkvinNJwecI1Of+L0gGlcMusLpydk5dXQPy1hq9I61kjs2uf5EATeSUAzw9HCOxHuX3AOSF6CJx5wColm" +
            "gDcecAqJZoA3HnAKiWaANx5wColmgDcecAr5C6TLIz1JZPZUAAAAAElFTkSuQmCC"},
        {"cc0-raster-300-input-s3-sys1-m2-st2", "21", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACT0lEQVR4Xu2SW25jQRBCs/9Nz8iPrqYo4LY1iRNFc776AgWy5I8/" +
            "L/ABJE8HWEjEImYEWEjEImYEWAikouY9oUT/jKQalHXmhaUqEC1dlKHzpTqfJbw0vu8afkTWtSphcQg3Dd6ROp4d8yeo1GcsTW0q" +
            "50t1qzqGpmKHS/tUVExNSK8t4SB7TdsSaPVKrCPV8L6lY61egbqRDadavQJ1IxuyVuLJ0r4QBVfaS0v3w3pQwZX2f6kORMGV9lOX" +
            "ClFwpf3Gpa3V6wTZcKrV6wTZcKrV6wTVAKKQfuMSxPbzgN2Ad1NTsS9ewhS8r6mG1jGLh3DT4H1NNRwutRB+XFKVvYSbZehzl56q" +
            "zrQvRzUNOIAfUHBT+6ehigYhgQW3BH1ruGTjM9t5+ixIuOWE0cGChFtOGB0sSLjlhNHBgoRbThgdLJzCxXc4hETTwxMLzgHJ81B5" +
            "/zIEKzCLp8J4J6BqhdTxTkC2ShGwRmB10q0UN9YI1JL8UU0DrBH4jqV2LSTEGgFYwuIhdKwRaEtVwN+MNQK4s7v718QaCZxZ9ftl" +
            "8E4ANxjOFt5JcP2Gk5tgJXhgwTkgeRbub3B4YY0Elzc4vLBGgssbHF5YI8HlDQ4vrBHATtzYqsIagV7ZZkBnnB6YjbjzBUtKm8bG" +
            "6QFZ+L4lmCJj4fSAKXzfkvinNJwecI1Of+L0gGlcMusLpydk5dXQPy1hq9I61kjs2uf5EATeSUAzw9HCOxHuX3AOSF6CJx5wColm" +
            "gDcecAqJZoA3HnAKiWaANx5wColmgDcecAr5C6TLIz1JZPZUAAAAAElFTkSuQmCC"},
        {"cc0-raster-300-input-s3-sys1-m3-st1", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACbUlEQVR4Xu2QUW4cQRBCff9LJ3K2uwuogh0rM44s5X1NAQ3a/fj1" +
            "BT4A7xwkwmcm1ExIhM9MqJmQCJ8RX0NOwRm6Ir6FJBvCI4MVVCKKS8F3Bguoo2klPLYU/7/LS/icKrrahJdanxF8zRVdbcJLrc8I" +
            "vuaKrjbhpdZnYj2bKkD7UUv71VQB2ncvNWHl6tNzHs0VIsKN7VeW6pHpILGuJewUfDvGlh7ov4hDeBjGGslsICEh9wKpR6ZEPaR8" +
            "yDrqxdSwAQ8pH7KOejE1bMBDyoeso15MDRvwkPIh+56pIXiU6y8SQ1tzRKurjAtUn7wbZRbuWRrFJ5YGaatw1ucFagjfzerDS/hv" +
            "SeyGJRDBoeOPUJ8XaH0iNip1wxJoHXwL3++ZOs4J5qGe3rWk7uKEPl083jKV1Anu4oQ+XTzeMpXACXaJJ0eXQysKWtIcd9DlkAYg" +
            "LUkHnwZuQHiJgtTw90vE/6URLAjcsNTByoOGiOxadGOhMSSaFi7ny5E8Sy/uSiNYlrF20gjveMalrZKGeMczDj2xtIe0dRQLa3j+" +
            "xRLVDhJhDQ8uwfOuMNbw4BA0y9nwjoeGdjkdE97x4Mquh0+Ddzww0dBsESyP1heaBJLn0YGN5pBoGrSf0PDBOx4tJzR88I5HywkN" +
            "H7zj0XJCwwfveKAUNxaa3ljDI5W4AnLDGp7WSEO20BqWqRGXXKPTPWPf9y3hljoLp3tM33NL7eFPXnJTRj5Yw7Mr+emsAtYITKWT" +
            "xnjHc1pPb1c63glAsaLRIlgB7T9oEEheQBcWGkOi6dGJhcaQaHp0YqExJJoenVhoDImmRycWGkN+A2Qs2HnTXxyNAAAAAElFTkSu" +
            "QmCC"},
        {"cc0-raster-300-input-s3-sys1-m3-st2", "21", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACbUlEQVR4Xu2QUW4cQRBCff9LJ3K2uwuogh0rM44s5X1NAQ3a/fj1" +
            "BT4A7xwkwmcm1ExIhM9MqJmQCJ8RX0NOwRm6Ir6FJBvCI4MVVCKKS8F3Bguoo2klPLYU/7/LS/icKrrahJdanxF8zRVdbcJLrc8I" +
            "vuaKrjbhpdZnYj2bKkD7UUv71VQB2ncvNWHl6tNzHs0VIsKN7VeW6pHpILGuJewUfDvGlh7ov4hDeBjGGslsICEh9wKpR6ZEPaR8" +
            "yDrqxdSwAQ8pH7KOejE1bMBDyoeso15MDRvwkPIh+56pIXiU6y8SQ1tzRKurjAtUn7wbZRbuWRrFJ5YGaatw1ucFagjfzerDS/hv" +
            "SeyGJRDBoeOPUJ8XaH0iNip1wxJoHXwL3++ZOs4J5qGe3rWk7uKEPl083jKV1Anu4oQ+XTzeMpXACXaJJ0eXQysKWtIcd9DlkAYg" +
            "LUkHnwZuQHiJgtTw90vE/6URLAjcsNTByoOGiOxadGOhMSSaFi7ny5E8Sy/uSiNYlrF20gjveMalrZKGeMczDj2xtIe0dRQLa3j+" +
            "xRLVDhJhDQ8uwfOuMNbw4BA0y9nwjoeGdjkdE97x4Mquh0+Ddzww0dBsESyP1heaBJLn0YGN5pBoGrSf0PDBOx4tJzR88I5HywkN" +
            "H7zj0XJCwwfveKAUNxaa3ljDI5W4AnLDGp7WSEO20BqWqRGXXKPTPWPf9y3hljoLp3tM33NL7eFPXnJTRj5Yw7Mr+emsAtYITKWT" +
            "xnjHc1pPb1c63glAsaLRIlgB7T9oEEheQBcWGkOi6dGJhcaQaHp0YqExJJoenVhoDImmRycWGkN+A2Qs2HnTXxyNAAAAAElFTkSu" +
            "QmCC"},
        {"cc0-raster-300-input-s3-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACRUlEQVR4XtXSS27DUAxD0ex/0y1iv49E6fIVaT3oGcWiTMFoX18f" +
            "eildUMcFooeORccFooeORccFoHfedCc7xEBvDLqW+JTk8p+dsiHRan1uuYzs4nLJ1JkI1d46qUxEdm1zifs4QU1tMyo4QU3rHnEf" +
            "J6hp3SM+hQFrSvfooUvd3wkLMWBNaxhhIQastobJHiqaG7U1Tv7yUvmo8LxmFc0daVa6PtDc0Wqh6wPNHa0Wuj7Q3NFqoesDzU+k" +
            "PP4GGByl7v2AhRh42vnUpdr67y81tXVScMJ27Xy7TipOWO2tk4oTVnvLoGEitHvH22XQMREqxfrcchnRYn3u2ZB8cuh3l94v7ztP" +
            "XEr1m25lh5jokdfpzseX6i3Ni/NGTw9ddCnxKdMjF11KfMr0yEWXEp8yPXLRpcSnTI9cdCnxKdIbg65FNkS5PD8Rl6FaXCeFiVhX" +
            "280STtgsze92s4gTZi9hIQZGf+n0URgY61L7UWkWYGDsS93/RBxFGBjhUni9TjIMjHhpN8tjwQlLh2Z5euhwwuKVVR9+9jgx4hGh" +
            "q5uJmNZvuhm4jOmBSfciGxI9EOnuwomh7ZHuLpwY2h7p7sKJoe2R7i6csFAabwy6PWHApDJeCeMCA1Ya0yE8RXOjKXzmUlv45CUY" +
            "N8lAcwZ9z10qL/79peNHaTDQ3IBGGC8YsFmZX+2nAQZGV9rNMk6MVTuLy6DBiRGKla5uJjK0f9HFwGWOnrjpVmRDQ2/cdCuyoaE3" +
            "broV2dDQGzfdimxo6I2bbkXfmj+P7ESq+yoAAAAASUVORK5CYII="},
        {"cc0-raster-300-input-s3-sys1-m4-st2", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACRUlEQVR4XtXSS27DUAxD0ex/0y1iv49E6fIVaT3oGcWiTMFoX18f" +
            "eildUMcFooeORccFooeORccFoHfedCc7xEBvDLqW+JTk8p+dsiHRan1uuYzs4nLJ1JkI1d46qUxEdm1zifs4QU1tMyo4QU3rHnEf" +
            "J6hp3SM+hQFrSvfooUvd3wkLMWBNaxhhIQastobJHiqaG7U1Tv7yUvmo8LxmFc0daVa6PtDc0Wqh6wPNHa0Wuj7Q3NFqoesDzU+k" +
            "PP4GGByl7v2AhRh42vnUpdr67y81tXVScMJ27Xy7TipOWO2tk4oTVnvLoGEitHvH22XQMREqxfrcchnRYn3u2ZB8cuh3l94v7ztP" +
            "XEr1m25lh5jokdfpzseX6i3Ni/NGTw9ddCnxKdMjF11KfMr0yEWXEp8yPXLRpcSnTI9cdCnxKdIbg65FNkS5PD8Rl6FaXCeFiVhX" +
            "280STtgsze92s4gTZi9hIQZGf+n0URgY61L7UWkWYGDsS93/RBxFGBjhUni9TjIMjHhpN8tjwQlLh2Z5euhwwuKVVR9+9jgx4hGh" +
            "q5uJmNZvuhm4jOmBSfciGxI9EOnuwomh7ZHuLpwY2h7p7sKJoe2R7i6csFAabwy6PWHApDJeCeMCA1Ya0yE8RXOjKXzmUlv45CUY" +
            "N8lAcwZ9z10qL/79peNHaTDQ3IBGGC8YsFmZX+2nAQZGV9rNMk6MVTuLy6DBiRGKla5uJjK0f9HFwGWOnrjpVmRDQ2/cdCuyoaE3" +
            "broV2dDQGzfdimxo6I2bbkXfmj+P7ESq+yoAAAAASUVORK5CYII="},
        {"cc0-raster-300-input-s4-sys1-m2-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACSUlEQVR4XrXN0Q4bMRBC0fz/T7daZ2PDXYZElXpeLBk0vP78o1fA" +
            "julpwZULO6anBVcu7JiezjiysGR6OuLGjTVVwxEXPthTNZxwYGNR1XDA+webqoYZzwtWVQ0znhesqhpmPH+waXqa8Lxg1fQ04XnB" +
            "qulpwvOCVdPThOcFq6anCc8LVk1PE56/Xf+smp5G3FjWN5umpxFHLu9vNk1PI64cbJqeRrx/sGl6GvH+wabpacT7B5umpwVXLuyY" +
            "ns44srBkejrixo01VcMRFz7YUzWccGBjUdVwwPsHm6qGGc8LVlUNM54XrKoaZjx/sGl6mvC8YNX0NOF5warpacLzglXT04TnBaum" +
            "pwnPC1ZNTxOev13/rJqeRtxY1jebpqcZV94T9zPqacadjUXT04wDG4umpxkHNhZNTzMObCyanhZcubBjejrixo01VcMRFz7YUzUc" +
            "ceGDPVXDCQc2FlUNB7x/sKlqmPG8YFXVMON5waqqYcTrglXT04TnBaumpwnPC1ZNTxOeF6yaniY8L1g1PU14XrBqeprw/O36Z9X0" +
            "NOLGsr7ZND3NuPKeuJ9RTzPubCyanmYc2Fg0Pc04sLFoetpw5vWfljiysGR6OuLGjTVVwxEXPthTNZxwYGNR1XDA+webqoYD3j/Y" +
            "VDXMeF6wqmqY8bxgVdUw4nXBqulpwvOCVdPThOcPNt2XOOD9g033JQ54f2MRvuVPHPhgj74WHrhwY+3he+OBGwtLTz9UiCMLS08/" +
            "VIgjC0tPP1SIIwtLTz9UiCMLS09/AYkSjt8G0d3SAAAAAElFTkSuQmCC"},
        {"cc0-raster-300-input-s4-sys1-m3-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACRElEQVR4XrXNQW7EMAxD0dz/0i3GzkTiD8UEBfo2BkyCOn7+6DDY" +
            "ETkNeOWDHZHTgFc+2BE5HfHGxpbI6YQnTqyJnA544Ys9kVOPBy4sipxa3C9sipxa3C9sipw6nG9YFTl1ON+wKnLqcL5hVeTU4HrH" +
            "rsipwfWOXZFTg+sduyKnBtc7dkVOHc43rIqcOpzf1jerIqcWjxzrxPnMcmrxTGFT5NTifmFT5NTifmFT5NTifmFT5HTGIwtLIqcj" +
            "3tjYEjmd8MSJNZHTAS98sSdy6vHAhUWRU4v7hU2RU4v7hU2RU4fzDasipw7nG1ZFTg2ud+yKnBpc79gVOTW43rErcmpwvWNX5NTg" +
            "eseuyKnD+YZVkVOH89v6ZlXk1OKRY504n1lOLZ4pbIqcWtwvbIqcWtwvbIqcWtwvbIqcznhkYUnkdMQbG1sipxOeOLEmcjrghS/2" +
            "RE49HriwKHJqcb+wKXJqcb+wKXLqcL5hVeTU4XzDqsipwfWOXZFTg+sduyKnBtc7dkVODa537IqcOpxvWBU5dTi/rW9WRU4d3lj2" +
            "N6sipxavHOvE+cxyavFMYVPk1OJ+YVPk1OJ+YVPkNOGZ458u8cjCksjpiDc2tkROJzxxYk3kdMALX+yJnHo8cGFR5NTifmFT5NTi" +
            "fmFT5NThfMOqyKnD+YZVkVOD6w2r6iG+43zDqnqI7zhf2ISn/Ib7FxbpsXDDCyfWbp4bxBMbW3cvKsAbC0vGm47ikYUl401H8cjC" +
            "kvGmo3hkYcl401E8srBk/AKTCXT5il3G6wAAAABJRU5ErkJggg=="},
        {"cc0-raster-300-input-s4-sys1-m4-st1", "21", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAGkAAABVCAAAAABvUZUwAAACQUlEQVR4XrXNW2rgQBBDUe9/0wl+xC1dq2QzMOenoSVU288/2gJ2" +
            "TE8LXtmxY3pa8MqOHdPTEW+c2DI9nfDEhTXT0wEv3FhUNRxwf2FT1XDA/YVNVcOM84JVVcOM8wubpqcJ5wWrpqcJ5wWrpqcJ5wWr" +
            "pqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ50/HN6umpxGPbMeJ65n1NOKZhU3T04j7C5umpxH3FzZNTyPuL2yans545MCS6emI" +
            "N05smZ5OeOLCmunpgBduLKoaDri/sKlqmHFesKpqmHFesKpqGHFdsGp6mnBesGp6mnBesGp6mnBesGp6mnBesGp6mnBesGp6mnBe" +
            "sGp6mnBesGp6mnD+dHyzanoa8ch2nLieWU8jnlnYND2NuL+waXoacX9h0/Q04v7CpunpjEcOLJmejnjjxJbp6YQnLqyZng544cai" +
            "quGA+wubqoYZ5wWrqoYZ5wWrqoYR1wWrpqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ5wWrpqcJ5w/nN6um" +
            "pxGvbMeJ65n1NOKZhU3T04j7C5umpxH3FzZNTxue2f7TJR45sGR6OuKNE1umpxOeuLBmejrghRuLqoYD7i9sqhpmnBesqhpmnBes" +
            "qhpGXBesmp4mnBesmp4mnF/YdC9xwP2FTfcSB9y/sQhv+RMP/GGPXgsPvHBh7eG9QTxxYuvpQ4V4ZMdO8KUDvLJjJ/jSAV7ZsRN8" +
            "6QCv7NgJvnSAV3bsBL+VHXvyqOo82AAAAABJRU5ErkJggg=="},
    };
}
