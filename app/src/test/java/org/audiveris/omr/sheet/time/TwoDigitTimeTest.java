package org.audiveris.omr.sheet.time;

import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTableFactory;
import ij.process.ByteProcessor;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** CC0 original Leland digit strips, through all five roundtrip conversions.
 * Cropped at the half-staff borders, as in the time builder. No corpus content.
 * Eight noisy/touching low-resolution strips deliberately have no accepted reading;
 * these are pinned as abstentions, not silently asserted to read 10..19. Existing
 * whole-number TIME_TWELVE/TIME_SIXTEEN recognition remains available to the builder.
 */
public class TwoDigitTimeTest
{
    @Test
    public void readTwoDigitsAndRejectSingleDigitsAndStems () throws Exception
    {
        final StringBuilder failures = new StringBuilder();
        for (String[] sample : SAMPLES) {
            final ByteProcessor ink = new ByteProcessor(ImageIO.read(new ByteArrayInputStream(
                    Base64.getDecoder().decode(sample[2]))));
            final var parts = GlyphFactory.buildGlyphs(
                    new RunTableFactory(Orientation.VERTICAL).createTable(ink), new Point());
            final var numbers = TwoDigitTime.find(parts, 21);
            final int expected = Integer.parseInt(sample[1]);
            if (expected < 0) {
                if (!numbers.isEmpty()) failures.append(sample[0] + " invented " + numbers + "\n");
            } else {
                if (numbers.size() != 1 || numbers.get(0).getValue() != expected)
                    failures.append(sample[0] + " expected " + expected + " got " + numbers + "\n");
            }
        }
        assertEquals("", failures.toString());
    }

    /** CC0 drawing: the left wall of a clipped zero is not a right-facing 3. */
    @Test
    public void rejectZeroWallAsThree ()
    {
        final ByteProcessor wall = new ByteProcessor(17, 37);
        wall.setValue(255);
        wall.fill();
        for (int y = 0; y < 37; y++) {
            final int x = Math.abs(y - 18) / 4;
            for (int dx = 0; dx < 5; dx++) {
                wall.set(x + dx, y, 0);
            }
        }
        assertFalse(TwoDigitTime.hasRightFacingThreeTopology(wall));
        wall.flipHorizontal();
        assertTrue(TwoDigitTime.hasRightFacingThreeTopology(wall));
    }

    private static final String[][] SAMPLES = {
        {"path-gs-pdfwrite-0-10", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAuUlEQVR4nO2W0QrDMAhFVfr/v+wY7YaxJrlOYQzmUxKTk6OFtqw0" +
            "BJuxS5HNBTsOgoPt8XPCliW7i2MSeeM7Khs8Re2lFsuSJCm5AXcVaEMwKSS8VfBk0CYKtP0Tq7xUhOI5aR3sUdXyaCxQW1Bcotys" +
            "tAelRQ7Z99WChVUvZRl63dSB0gQK66OUfIb4o7p7pUjyp9uuQOoLBer2c5224rrVrFtmGbe6Dl1a49/WufSeTNX9juc8INEDDykh" +
            "Sg/CQQwAAAAASUVORK5CYII="},
        {"path-gs-pdfwrite-1-11", "11",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAkklEQVR4nO2UwQ7AIAhDC9n//zLLdjIsFqvuZk8m1GchBgskWXOW" +
            "io5tcvYupKIrZi6pQVNQC6GgpDIFVc88lFTEDFr0PR8ho6oOwIs+ajZGyailmeOTar49NKi1mSOn6r4bdagGFdQcdapr1B0l6s/V" +
            "Ny9fuJt0UONywVvooMblgrfQQSWRJW/NduzaHg8tvroBix8XR/rz5loAAAAASUVORK5CYII="},
        {"path-gs-pdfwrite-2-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA6klEQVR4nKWWSxLAIAhDjeP9r5yu2iJGBWXRn/LIMBoLli7wP85H" +
            "9JxWggGT+lNhWFUlbEiFVAk1RoqEUzULdDfbI2jUXhRwq2pVAwoV6hTCqtxiykTt612QSr1eCBSqMqKwUrUUZUrMq9WkKPq6HFCb" +
            "TvHdfYs5LSqKWyd6VfGs51Sq4isKmhS3vr3NtmMS/VA7I1GMtVsQvrcMCkLQ1NvTJBstR6L6mFUFRTpShffa0Tq3DKJgn77k3gMS" +
            "bR9OMOcmMVXegqQlJVRRNn20vgCpTGAp1GIPM4nSqQlVrreJv75xqtx0nB4oDxT/NWXxVPG8AAAAAElFTkSuQmCC"},
        {"path-gs-pdfwrite-3-13", "13",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA7ElEQVR4nK2W2xKDMAhEIZP//+XtTG0tMZu4UHnwAuGAGFCHjeLh" +
            "GksLW9BNFOdKrFHU49RjWuQ/VrOC4FoVhtonZacZxKNJJGlB9gF9rWmTFhIAQla0opI0qRASvyWdsA7btLcDwdTuax4dsAnb9eDO" +
            "9mVw6fLutLkBx+A9U3Nf4UcUCl2D4a4/sTnzPYirwr00RW8nX2n0IfgHbGmKGmXVUMZYCZTf7Ba17H4czmwwc5MP6BubiHJhzf+f" +
            "iSRKSUpElWa7PTLbi4IiCp8zK56IOhDsD2b3U8Tf1tvjawotFJuRfkZYYGqOab4AQ201Wm7oF3QAAAAASUVORK5CYII="},
        {"path-gs-pdfwrite-4-14", "14",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAwElEQVR4nNWVUQ7EIAhEZ0jvf2U22ZrVtqIgZpP61ZbxOSBVKprB" +
            "+nj5/oj2NDLUhoYk5qqNCppSmKgkCYEEOYnLNlNYLbvCRtEQuEm4uBqSOBPKeneSXVQ8PfK+fpPgmMQb6amQeHpqTKiuNEmCt69o" +
            "vtxQGjLFfuT4CbIkOBNkJZkj9g8q0qhialwG2UZC5mxfQPlMwe9KZ4LSVw5TtEPnMn+u1T6UOlEHwiwakdfW6t0olk2wjsZ2j+x7" +
            "6av6AG9WJUtAJQE8AAAAAElFTkSuQmCC"},
        {"path-gs-pdfwrite-5-15", "15",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA3klEQVR4nM2W4Q6FIAiFOc33f+Vzt7wrRFSkfsRWWeDngdQJSmtQ" +
            "bfqfxQ855DU7zPto9IHpnJ6pooxRe6IoY9QTkpStzrfZH29Rpyhio7vMyr6ID6I2J8IEVdOTtJX2dUnCOLRspYe2PZtXc1Gwo8FH" +
            "pWoOeChGa86R464VESDxf11qVCdVqyDJxCK3yXDqze5X7GXtoDh3lyyG3eyJ71dYCQsnCHV/hoJ55lFwWllVAfsCCu+hLuMXEqTJ" +
            "z91kAgDpSHlVHUmf5tYnuxpRnWdbxek1qVCLs4Lnblb3D6oYL05mJESFAAAAAElFTkSuQmCC"},
        {"path-gs-pdfwrite-6-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA5UlEQVR4nM2WgQ6DMAhEucb//+VbsmUTKK1Xm5iRaNTSx1EpClow" +
            "nJdpJI32Pu3CNwyiQ3j/w9aNn+ONAQeq5qIsKspTUoILJGSHporCiIQKpZAmlhMsKkC1tiiKsqoNUdZWVqMMylrVSJQU5vDeS+kx" +
            "Tznupfe2FNnvQVUUan9pO6O+QYR9UfR7XKVagLXNkkKF0o3kXpPJnW/aGTSjOwdbRrF7go21GiBvo+weSquTJnlJ1KcTtFML/kiV" +
            "URh7XpXx+nMmq2JGdDH0BCOreJOudw7e88/BpdT/swXUsGImwcIKvgCPhjVM+DDzOgAAAABJRU5ErkJggg=="},
        {"path-gs-pdfwrite-7-17", "17",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAzUlEQVR4nMWW4Q7DIAiEucb3f+VbsmyZyuEUTeSXBfp5IKYFrTFU" +
            "618I7wf4EKr1Y9OGYG0apVI+EfxLXFBlnoUYFYvyhl5oWpV996VErYjy9vSkbjgWrHTPIxLHust0edQ0Rm2fKI+kzi2ZnlN6n9me" +
            "03n6N5oC86dXo9ZHChGKOVFUbR9yRNCXkb2D8K6N62yd2iQKdlrVARTUaaRQkN7LBUJP211VOIZCFNgokLsohJFVFOLQvRPEOdRw" +
            "m+bXTll9UG1KOO2R9ukPyAvQuiJXPyVwowAAAABJRU5ErkJggg=="},
        {"path-gs-pdfwrite-8-18", "18",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA/klEQVR4nK2W2RLDIAhFuUz//5fpTJtYRVZTHrIIOQIiBkKL4Peo" +
            "NDSpTBvOzbVKREz7l/+tSZJx/Q5AHK9QI33k8g1egJ7AGlQsLmbKXolFuEOi0II781JozL1CQKDkplPYX2RDRXmYZvDNuJ0pIEPl" +
            "y7fB1vgGCq3lA4zJ5z0YkATu5u5uHJJ8/EaJJOFJOspndX7LHHc1QEJaGlUUck2xi4JU9zSkHOBI59zao+3siKprDeugNrFgfOCU" +
            "0ywOvSKjDs9RJP9Dkaz3EkofnrY88eoBCvF7DXVlww5xP3HafkEpi6h75l9PGU+jJqb/I2eFwuDs3u7Z7n9WNoneYrE7UIEANj0A" +
            "AAAASUVORK5CYII="},
        {"path-gs-pdfwrite-9-19", "19",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA40lEQVR4nLWWSxKEMAgFedTc/8pvVuaDkEAyw0JjYdoGLUpQpkBf" +
            "mowMKfeGz+b2d4ptiRmm8d6QJKT3ZE1KYSAFG5JWMNd0EpqSagn7KsbQXM9f4WjZAleP3YRWer5O6s+kRI865YbeS8GicFmeNFS1" +
            "PMQoWUn1BBJW91LyoMhVp6wW1lbJngNRmckCaWAXKEk4p9tOxlfubF/C5GmSr1hAScBgFYVYp9grDMc7FMz5xioO1lBdBn+04jGK" +
            "uXl1Eqyi2gYrxWMrGtIUtSHTfqwcKcF2bs8UV9JaRV9MOA3soPkCVVgyUJjd1yUAAAAASUVORK5CYII="},
        {"path-gs-pdfwrite-10-2", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA00lEQVR4nM2W2w7DIAxDccT//7In0XVjqLm/zA/VLvWJqUIoOFRh" +
            "Xfn9uKQaRPvjbSdPP/KoXwv1ki4KB5BuLGOBtjGOgsFCMVVc4t8SXaJ0s/wNimUUg09McpUtqamohqLimGoRLjvjDYHQSnaSaphJ" +
            "EnvNECONmSHRvM9FPQysIuomcf+JFdR2UtxS+2ImQYYs1HXkjKgkS0IBhUcnms3A3rzay+PzxdrWoT24EPTmgzYZDBcLqc53GEfu" +
            "ZGj3FQpsMcrk9k388HJDhabo7jYyvgDDASlfxfORngAAAABJRU5ErkJggg=="},
        {"path-gs-pdfwrite-11-3", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyElEQVR4nLWWSw7EMAhDMZr7X9kjdVEl00AM6aCuKvNwPrUKWlRY" +
            "vWQoNzAn8ZfKEOUKaexHHTU3UmBFKMSspqtVoYvSbXltcgPF91BPKKyLSmyxiGoUso9qMICtqRQ1Qu+K9R/L6rnXjMX+Gslq207r" +
            "oh6dyNTKtk8AHqHEI3SJdBR9CauDCuajfEVxPYW08Y2l2wLbqEaM+k4AeYzXpxv/Hn2uCnGO0m+Dy8otPLqivBa0ynaLKnQ1zYaS" +
            "DNVs7/yqrXuYnMIXuDYqRtxdttkAAAAASUVORK5CYII="},
        {"path-gs-pdfwrite-12-4", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAApUlEQVR4nN2WSw5DMQgDbdT7X5muKlVpAzbvI7UsE5hMrCzCxL64" +
            "LmTRjKg2LRLiNBIsqzmKlhR0q8RZqGw7bsrKk0Joqee64KE+SeRhlOKEArWMsj0wRCn2bQ+JJIAKK7oktBeUSdihqD9N0cogYYMS" +
            "72RY5XEUByRUVh4JsZcySWCOMk/ValZxLSpnKApzb9ll0fY7Wf0Vitav71Vfh4xPUbfzBBPEHENxrzmoAAAAAElFTkSuQmCC"},
        {"path-gs-pdfwrite-13-5", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAtUlEQVR4nM3W4Q6DIAwE4B7h/V/5lszp5oRegWazP4zB9oMYioLW" +
            "DnTG2Rk3K5YWZTCfaRQti6L3sGZBJqhnMSQSoDYh5NhfN8NvqKoSPhqIItPtQSJti0IOLLwrrOx2ngFwdlXk+yrWJaiDoJ8XoBqo" +
            "YYqKtp9PcZhFfGIItarS+FRFLQLrFC43NzivkEd5wRsdyMijjuAgxe87dBI11Zc4TL0q4pLXznv56bMz93+1VwUlewDEvStAqS7N" +
            "pQAAAABJRU5ErkJggg=="},
        {"path-gs-pdfwrite-14-6", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAw0lEQVR4nLWWWRKEMAhEIeX9r9xTY81ijCwdEv6w4NmghCjEMx2e" +
            "wI5FioS/gynUl4TOtzIaTTKt8SSlUYEGrkCqPLFRTqZyKE8DSFVZAIEaCZCqKolkHUSrIOrxDNSMsCPKfg9pN9UVVZdi1YPFbe/b" +
            "pvYPR3zBj2mp7ZeDz7GMqrM/+Ln8IdOTCuO8YwYlRGyYwSbLrG1HYR1KFqKwDvVgOrsmQK/Wlg2srIm7LDUjQ9SNFR/KqfvVE59S" +
            "JYIh0XvxC5tyKkhMM0/zAAAAAElFTkSuQmCC"},
        {"path-gs-pdfwrite-15-7", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAq0lEQVR4nMWU4QrEIAyDE9n7v3KOG7gxrrPt7Ln8EEH7mdoqBVME" +
            "tI9dOheO+VXNJp1h1hxp1DWaEygzlo9QPxSOrW1xFDpI0648rUPpJpn0XX05CnSU70q2M81U8AjmQ1eJa8pXkMMTMigW95VuV7Z4" +
            "BJ1DXnk49MoaRtHd8UKC9Nt2vSuWoTpp+CxXJ8g6FMpQLEMx+Bk2lKm5O1iG4h//dpSgxvJQ8fzwAQRiFlgGYk/aAAAAAElFTkSu" +
            "QmCC"},
        {"path-gs-pdfwrite-16-8", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA1klEQVR4nL1WWw7DIAzD3P/Onqa12qBxwBFafioedkwAU7DJQNBH" +
            "Pb17TC3svIZkmhtE0d5X9UCSC13JAkemltYpowpzM5W1UmVEt2azQJXseVODK1UIWrSovrORCjRrhYNlB7ZI93bwQ4Z8G+UdlApo" +
            "q6J9tLociTEs1SpEoUQVo+BTyaOJgqp38Da8n5A5RB2H2zaD6TvDDQmk1f2KPGh95KpaMVXoSzylqpVfnEL0JP+jJJy+f1Z1ngqL" +
            "tvPijHca4+Aca0Oef4p8b5er8b1dQfQ5fQEZjjI92YzpBwAAAABJRU5ErkJggg=="},
        {"path-gs-pdfwrite-17-9", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyElEQVR4nM2WSxLEIAhEaSv3v3LPYlLRceSri7AzwEtDlAiKbpjW" +
            "NGJFmgHCTABqqCeNwg6roAaSW5qN+iV1Fkq9ct4RRc2iZOj9MVW6FVBIocz9884CT6KwiYIbsUax8gFaPHQbhbC6tn48TAJE64Q2" +
            "PqxkZlTFJlS0V310fpfi2WX4uJg1RVSyykt3QUMxibr3QEJXs0hvOzgwUphDxaBJ1N+/lMd6xY3JMInSzVM1M5lHKbcO82KnOu9s" +
            "jsWZe19HLdpjn6IPR0UpSaXa10kAAAAASUVORK5CYII="},
        {"path-gs-pdfwrite-18-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA60lEQVR4nKWWQZLFIAhEacr7X7lnNaUiEOCzwojPhoooKLdhuwxn" +
            "3JglhfhjhiYKm6Xh0oQkhxZEqFAU7qEty4uK04sD0EwQ1nFkaUuUZDHlsn+b2g29gtZkqRn7pErmJyqJfzdAhqqlJ3HInWCBFJvW" +
            "ykHjeYdei6IYkeLOUNB1m9MZ2KgUPNKhagISThL8brNrTqKZWUMQ37k1IfH+xAEKkaA2Cg4IExRSSe1aMfu4WqJgcRj0dmzvWI5f" +
            "rwn8E+4GUUwQ8dDvVx/G/MRr64/iCxu8ZMSuHKoqWRtlZJ0vre1Hdw7NoYm66B9I3DVhl/hyHAAAAABJRU5ErkJggg=="},
        {"path-gs-pdfwrite-19-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA4UlEQVR4nL2W0RKEIAhFu47//8t3H5rSEBBod3nYiREPRytb8HgE" +
            "xqUYEaNrTdvUbkYxXXd3rjGPIwMNK1cKF+icTTlFLDBCOoy2LSoFK0feyr6vCioj5aPgNMVeSi7QqdxGyyxBDepWlpTSZi1tZSme" +
            "v9RfnNxOUeR9koqToFWHXmdoyYK79opBKcDM7m2f9i8TUFCZoMrqJRC1ZydvRSFWP2TsKJ9XXLT+bcXvoY6foOKn6JtoSRe8RYX6" +
            "BFEM1JStsHSJorgg6laCpXxjHZTo/2BpX+vxV8sydx+E+XZ8AE9HMVBSprVQAAAAAElFTkSuQmCC"},
        {"path-gs-ps2write-0-10", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAuUlEQVR4nO2W0QrDMAhFVfr/v+wY7YaxJrlOYQzmUxKTk6OFtqw0" +
            "BJuxS5HNBTsOgoPt8XPCliW7i2MSeeM7Khs8Re2lFsuSJCm5AXcVaEMwKSS8VfBk0CYKtP0Tq7xUhOI5aR3sUdXyaCxQW1Bcotys" +
            "tAelRQ7Z99WChVUvZRl63dSB0gQK66OUfIb4o7p7pUjyp9uuQOoLBer2c5224rrVrFtmGbe6Dl1a49/WufSeTNX9juc8INEDDykh" +
            "Sg/CQQwAAAAASUVORK5CYII="},
        {"path-gs-ps2write-1-11", "11",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAkklEQVR4nO2UwQ7AIAhDC9n//zLLdjIsFqvuZk8m1GchBgskWXOW" +
            "io5tcvYupKIrZi6pQVNQC6GgpDIFVc88lFTEDFr0PR8ho6oOwIs+ajZGyailmeOTar49NKi1mSOn6r4bdagGFdQcdapr1B0l6s/V" +
            "Ny9fuJt0UONywVvooMblgrfQQSWRJW/NduzaHg8tvroBix8XR/rz5loAAAAASUVORK5CYII="},
        {"path-gs-ps2write-2-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA6klEQVR4nKWWSxLAIAhDjeP9r5yu2iJGBWXRn/LIMBoLli7wP85H" +
            "9JxWggGT+lNhWFUlbEiFVAk1RoqEUzULdDfbI2jUXhRwq2pVAwoV6hTCqtxiykTt612QSr1eCBSqMqKwUrUUZUrMq9WkKPq6HFCb" +
            "TvHdfYs5LSqKWyd6VfGs51Sq4isKmhS3vr3NtmMS/VA7I1GMtVsQvrcMCkLQ1NvTJBstR6L6mFUFRTpShffa0Tq3DKJgn77k3gMS" +
            "bR9OMOcmMVXegqQlJVRRNn20vgCpTGAp1GIPM4nSqQlVrreJv75xqtx0nB4oDxT/NWXxVPG8AAAAAElFTkSuQmCC"},
        {"path-gs-ps2write-3-13", "13",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA7ElEQVR4nK2W2xKDMAhEIZP//+XtTG0tMZu4UHnwAuGAGFCHjeLh" +
            "GksLW9BNFOdKrFHU49RjWuQ/VrOC4FoVhtonZacZxKNJJGlB9gF9rWmTFhIAQla0opI0qRASvyWdsA7btLcDwdTuax4dsAnb9eDO" +
            "9mVw6fLutLkBx+A9U3Nf4UcUCl2D4a4/sTnzPYirwr00RW8nX2n0IfgHbGmKGmXVUMZYCZTf7Ba17H4czmwwc5MP6BubiHJhzf+f" +
            "iSRKSUpElWa7PTLbi4IiCp8zK56IOhDsD2b3U8Tf1tvjawotFJuRfkZYYGqOab4AQ201Wm7oF3QAAAAASUVORK5CYII="},
        {"path-gs-ps2write-4-14", "14",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAwElEQVR4nNWVUQ7EIAhEZ0jvf2U22ZrVtqIgZpP61ZbxOSBVKprB" +
            "+nj5/oj2NDLUhoYk5qqNCppSmKgkCYEEOYnLNlNYLbvCRtEQuEm4uBqSOBPKeneSXVQ8PfK+fpPgmMQb6amQeHpqTKiuNEmCt69o" +
            "vtxQGjLFfuT4CbIkOBNkJZkj9g8q0qhialwG2UZC5mxfQPlMwe9KZ4LSVw5TtEPnMn+u1T6UOlEHwiwakdfW6t0olk2wjsZ2j+x7" +
            "6av6AG9WJUtAJQE8AAAAAElFTkSuQmCC"},
        {"path-gs-ps2write-5-15", "15",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA3klEQVR4nM2W4Q6FIAiFOc33f+Vzt7wrRFSkfsRWWeDngdQJSmtQ" +
            "bfqfxQ855DU7zPto9IHpnJ6pooxRe6IoY9QTkpStzrfZH29Rpyhio7vMyr6ID6I2J8IEVdOTtJX2dUnCOLRspYe2PZtXc1Gwo8FH" +
            "pWoOeChGa86R464VESDxf11qVCdVqyDJxCK3yXDqze5X7GXtoDh3lyyG3eyJ71dYCQsnCHV/hoJ55lFwWllVAfsCCu+hLuMXEqTJ" +
            "z91kAgDpSHlVHUmf5tYnuxpRnWdbxek1qVCLs4Lnblb3D6oYL05mJESFAAAAAElFTkSuQmCC"},
        {"path-gs-ps2write-6-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA5UlEQVR4nM2WgQ6DMAhEucb//+VbsmUTKK1Xm5iRaNTSx1EpClow" +
            "nJdpJI32Pu3CNwyiQ3j/w9aNn+ONAQeq5qIsKspTUoILJGSHporCiIQKpZAmlhMsKkC1tiiKsqoNUdZWVqMMylrVSJQU5vDeS+kx" +
            "Tznupfe2FNnvQVUUan9pO6O+QYR9UfR7XKVagLXNkkKF0o3kXpPJnW/aGTSjOwdbRrF7go21GiBvo+weSquTJnlJ1KcTtFML/kiV" +
            "URh7XpXx+nMmq2JGdDH0BCOreJOudw7e88/BpdT/swXUsGImwcIKvgCPhjVM+DDzOgAAAABJRU5ErkJggg=="},
        {"path-gs-ps2write-7-17", "17",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAzUlEQVR4nMWW4Q7DIAiEucb3f+VbsmyZyuEUTeSXBfp5IKYFrTFU" +
            "618I7wf4EKr1Y9OGYG0apVI+EfxLXFBlnoUYFYvyhl5oWpV996VErYjy9vSkbjgWrHTPIxLHust0edQ0Rm2fKI+kzi2ZnlN6n9me" +
            "03n6N5oC86dXo9ZHChGKOVFUbR9yRNCXkb2D8K6N62yd2iQKdlrVARTUaaRQkN7LBUJP211VOIZCFNgokLsohJFVFOLQvRPEOdRw" +
            "m+bXTll9UG1KOO2R9ukPyAvQuiJXPyVwowAAAABJRU5ErkJggg=="},
        {"path-gs-ps2write-8-18", "18",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA/klEQVR4nK2W2RLDIAhFuUz//5fpTJtYRVZTHrIIOQIiBkKL4Peo" +
            "NDSpTBvOzbVKREz7l/+tSZJx/Q5AHK9QI33k8g1egJ7AGlQsLmbKXolFuEOi0II781JozL1CQKDkplPYX2RDRXmYZvDNuJ0pIEPl" +
            "y7fB1vgGCq3lA4zJ5z0YkATu5u5uHJJ8/EaJJOFJOspndX7LHHc1QEJaGlUUck2xi4JU9zSkHOBI59zao+3siKprDeugNrFgfOCU" +
            "0ywOvSKjDs9RJP9Dkaz3EkofnrY88eoBCvF7DXVlww5xP3HafkEpi6h75l9PGU+jJqb/I2eFwuDs3u7Z7n9WNoneYrE7UIEANj0A" +
            "AAAASUVORK5CYII="},
        {"path-gs-ps2write-9-19", "19",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA40lEQVR4nLWWSxKEMAgFedTc/8pvVuaDkEAyw0JjYdoGLUpQpkBf" +
            "mowMKfeGz+b2d4ptiRmm8d6QJKT3ZE1KYSAFG5JWMNd0EpqSagn7KsbQXM9f4WjZAleP3YRWer5O6s+kRI865YbeS8GicFmeNFS1" +
            "PMQoWUn1BBJW91LyoMhVp6wW1lbJngNRmckCaWAXKEk4p9tOxlfubF/C5GmSr1hAScBgFYVYp9grDMc7FMz5xioO1lBdBn+04jGK" +
            "uXl1Eqyi2gYrxWMrGtIUtSHTfqwcKcF2bs8UV9JaRV9MOA3soPkCVVgyUJjd1yUAAAAASUVORK5CYII="},
        {"path-gs-ps2write-10-2", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA00lEQVR4nM2W2w7DIAxDccT//7In0XVjqLm/zA/VLvWJqUIoOFRh" +
            "Xfn9uKQaRPvjbSdPP/KoXwv1ki4KB5BuLGOBtjGOgsFCMVVc4t8SXaJ0s/wNimUUg09McpUtqamohqLimGoRLjvjDYHQSnaSaphJ" +
            "EnvNECONmSHRvM9FPQysIuomcf+JFdR2UtxS+2ImQYYs1HXkjKgkS0IBhUcnms3A3rzay+PzxdrWoT24EPTmgzYZDBcLqc53GEfu" +
            "ZGj3FQpsMcrk9k388HJDhabo7jYyvgDDASlfxfORngAAAABJRU5ErkJggg=="},
        {"path-gs-ps2write-11-3", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyElEQVR4nLWWSw7EMAhDMZr7X9kjdVEl00AM6aCuKvNwPrUKWlRY" +
            "vWQoNzAn8ZfKEOUKaexHHTU3UmBFKMSspqtVoYvSbXltcgPF91BPKKyLSmyxiGoUso9qMICtqRQ1Qu+K9R/L6rnXjMX+Gslq207r" +
            "oh6dyNTKtk8AHqHEI3SJdBR9CauDCuajfEVxPYW08Y2l2wLbqEaM+k4AeYzXpxv/Hn2uCnGO0m+Dy8otPLqivBa0ynaLKnQ1zYaS" +
            "DNVs7/yqrXuYnMIXuDYqRtxdttkAAAAASUVORK5CYII="},
        {"path-gs-ps2write-12-4", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAApUlEQVR4nN2WSw5DMQgDbdT7X5muKlVpAzbvI7UsE5hMrCzCxL64" +
            "LmTRjKg2LRLiNBIsqzmKlhR0q8RZqGw7bsrKk0Joqee64KE+SeRhlOKEArWMsj0wRCn2bQ+JJIAKK7oktBeUSdihqD9N0cogYYMS" +
            "72RY5XEUByRUVh4JsZcySWCOMk/ValZxLSpnKApzb9ll0fY7Wf0Vitav71Vfh4xPUbfzBBPEHENxrzmoAAAAAElFTkSuQmCC"},
        {"path-gs-ps2write-13-5", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAtUlEQVR4nM3W4Q6DIAwE4B7h/V/5lszp5oRegWazP4zB9oMYioLW" +
            "DnTG2Rk3K5YWZTCfaRQti6L3sGZBJqhnMSQSoDYh5NhfN8NvqKoSPhqIItPtQSJti0IOLLwrrOx2ngFwdlXk+yrWJaiDoJ8XoBqo" +
            "YYqKtp9PcZhFfGIItarS+FRFLQLrFC43NzivkEd5wRsdyMijjuAgxe87dBI11Zc4TL0q4pLXznv56bMz93+1VwUlewDEvStAqS7N" +
            "pQAAAABJRU5ErkJggg=="},
        {"path-gs-ps2write-14-6", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAw0lEQVR4nLWWWRKEMAhEIeX9r9xTY81ijCwdEv6w4NmghCjEMx2e" +
            "wI5FioS/gynUl4TOtzIaTTKt8SSlUYEGrkCqPLFRTqZyKE8DSFVZAIEaCZCqKolkHUSrIOrxDNSMsCPKfg9pN9UVVZdi1YPFbe/b" +
            "pvYPR3zBj2mp7ZeDz7GMqrM/+Ln8IdOTCuO8YwYlRGyYwSbLrG1HYR1KFqKwDvVgOrsmQK/Wlg2srIm7LDUjQ9SNFR/KqfvVE59S" +
            "JYIh0XvxC5tyKkhMM0/zAAAAAElFTkSuQmCC"},
        {"path-gs-ps2write-15-7", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAq0lEQVR4nMWU4QrEIAyDE9n7v3KOG7gxrrPt7Ln8EEH7mdoqBVME" +
            "tI9dOheO+VXNJp1h1hxp1DWaEygzlo9QPxSOrW1xFDpI0648rUPpJpn0XX05CnSU70q2M81U8AjmQ1eJa8pXkMMTMigW95VuV7Z4" +
            "BJ1DXnk49MoaRtHd8UKC9Nt2vSuWoTpp+CxXJ8g6FMpQLEMx+Bk2lKm5O1iG4h//dpSgxvJQ8fzwAQRiFlgGYk/aAAAAAElFTkSu" +
            "QmCC"},
        {"path-gs-ps2write-16-8", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA1klEQVR4nL1WWw7DIAzD3P/Onqa12qBxwBFafioedkwAU7DJQNBH" +
            "Pb17TC3svIZkmhtE0d5X9UCSC13JAkemltYpowpzM5W1UmVEt2azQJXseVODK1UIWrSovrORCjRrhYNlB7ZI93bwQ4Z8G+UdlApo" +
            "q6J9tLociTEs1SpEoUQVo+BTyaOJgqp38Da8n5A5RB2H2zaD6TvDDQmk1f2KPGh95KpaMVXoSzylqpVfnEL0JP+jJJy+f1Z1ngqL" +
            "tvPijHca4+Aca0Oef4p8b5er8b1dQfQ5fQEZjjI92YzpBwAAAABJRU5ErkJggg=="},
        {"path-gs-ps2write-17-9", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyElEQVR4nM2WSxLEIAhEaSv3v3LPYlLRceSri7AzwEtDlAiKbpjW" +
            "NGJFmgHCTABqqCeNwg6roAaSW5qN+iV1Fkq9ct4RRc2iZOj9MVW6FVBIocz9884CT6KwiYIbsUax8gFaPHQbhbC6tn48TAJE64Q2" +
            "PqxkZlTFJlS0V310fpfi2WX4uJg1RVSyykt3QUMxibr3QEJXs0hvOzgwUphDxaBJ1N+/lMd6xY3JMInSzVM1M5lHKbcO82KnOu9s" +
            "jsWZe19HLdpjn6IPR0UpSaXa10kAAAAASUVORK5CYII="},
        {"path-gs-ps2write-18-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA60lEQVR4nKWWQZLFIAhEacr7X7lnNaUiEOCzwojPhoooKLdhuwxn" +
            "3JglhfhjhiYKm6Xh0oQkhxZEqFAU7qEty4uK04sD0EwQ1nFkaUuUZDHlsn+b2g29gtZkqRn7pErmJyqJfzdAhqqlJ3HInWCBFJvW" +
            "ykHjeYdei6IYkeLOUNB1m9MZ2KgUPNKhagISThL8brNrTqKZWUMQ37k1IfH+xAEKkaA2Cg4IExRSSe1aMfu4WqJgcRj0dmzvWI5f" +
            "rwn8E+4GUUwQ8dDvVx/G/MRr64/iCxu8ZMSuHKoqWRtlZJ0vre1Hdw7NoYm66B9I3DVhl/hyHAAAAABJRU5ErkJggg=="},
        {"path-gs-ps2write-19-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA4UlEQVR4nL2W0RKEIAhFu47//8t3H5rSEBBod3nYiREPRytb8HgE" +
            "xqUYEaNrTdvUbkYxXXd3rjGPIwMNK1cKF+icTTlFLDBCOoy2LSoFK0feyr6vCioj5aPgNMVeSi7QqdxGyyxBDepWlpTSZi1tZSme" +
            "v9RfnNxOUeR9koqToFWHXmdoyYK79opBKcDM7m2f9i8TUFCZoMrqJRC1ZydvRSFWP2TsKJ9XXLT+bcXvoY6foOKn6JtoSRe8RYX6" +
            "BFEM1JStsHSJorgg6laCpXxjHZTo/2BpX+vxV8sydx+E+XZ8AE9HMVBSprVQAAAAAElFTkSuQmCC"},
        {"path-raster-300-0-10", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAArUlEQVR4nN2WUQ+EIAyD1+X+/1/uPRx6Iw4tATXaF0MKHx0aHGhR" +
            "iIPasspLpnxMFeLyZYDA8sONM5IxWVGjBLF6RHlvqLbhGkmRWCAEy6eFMi3V7i7YoNB6M3NTSfJpocwVEi4rEAlqpDw749g5HMrW" +
            "+2qQY+d8V+PyCYzXo9iB4hMLvAtFxby+QArWDcfOY6M7VbnIkz/Hv9dq/lbKjLRVi2n1ro9rp7fZ5acvc4YeTMRTbsIAAAAASUVO" +
            "RK5CYII="},
        {"path-raster-300-1-11", "11",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAh0lEQVR4nO2VzQ6AMAiDS+P7vzLG21j2AxuLl303Q1NKY1QUFikf" +
            "QkMiDY72IjLEuVSyHgqMiJF1oESsds7Dqdql+eZ5hrXVVudoHLgcCoyIU2oXx57KaijWlFQeaHZ296qjxsfKu2gk1Ta8Vm7ol864" +
            "Vn4Y0E64Vr90JcU3rfsj/DTDYXaqF2mqFEdXZFKKAAAAAElFTkSuQmCC"},
        {"path-raster-300-2-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA60lEQVR4nKWW2w7DIAxDMeL/f9mTNq1NaK7Ubw3hxFAUAIcU5Ecw" +
            "ZOWsURTEXEHFzZpmvksid7uwUSlp/IlWjnaVkYZRDRbKN1VKqrl6iDEq36lYfVfIUbXSgS5U9JufQoSKSDX+DMrYopc9S+VLtmbN" +
            "FH1TNDtDUJy/+QzqLpVZMgbH7+WqeAqGS6q3vrzPrhck6qF1CuId4gEKT0MC3kFh52g1UDBAcuuOLq93rQ959ORcUUfYcwX1sd2K" +
            "LVf78r7W9mB7gX9dIL68By2t3olCkNVwxSTaXGDU1e4Fum8UNZ2F58fh5SzqfADE8y50fJXOKgAAAABJRU5ErkJggg=="},
        {"path-raster-300-3-13", "13",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA2UlEQVR4nN1W0RLDIAgjXv//l7Ob11UroOC6l+WhZxFyAZEWlDvQ" +
            "v9DbsFyKBIGl/UhGUDmBWVUnKgvHqlhU6zTgFjGr6gNDVwmJCvmULJOP3QQXVLqiP1bFANVMFEcDZlTzmvO+gLF1ROWzhut70zBQ" +
            "zWpOgcXCx5qBoqm40wikmeBOQ6EL+jZBtOV4glNQxwsu65YqRkZfmEwakE8Qi6OJf3HQnrasKBXUYlcVAj75suM5Kvl3KnqWXVXQ" +
            "ppKVQo8pNxkuSvN/rp9dbse8faabFeFaeRe52V8hDS9TgFCzFQAAAABJRU5ErkJggg=="},
        {"path-raster-300-4-14", "14",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyklEQVR4nNWU4Q6DMAiEOeL7v/ItmnVrtVCQxWT8s9CP40RB6QL9" +
            "w5C5pi8l6paOAT89oLJBE7XouoqKKpqorCiKhVqS4JNqthuo6ngiW5LQgkfz6YrCaGV/CTif6a3xOLsx2O6JOsWktwbHk/4up1Pc" +
            "WQbO/dD8eDSc1SAFS1Lbq7jhNN920CsEarK2s4r6iOLPVFGqqCaKUkXFSJL5ycA5YkhV9K+hCVV1FJ9XtUlaFozUs179Nwpv4+w9" +
            "/Drr7OpelFDlbf2eewGOzSNRUxojkgAAAABJRU5ErkJggg=="},
        {"path-raster-300-5-15", "15",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA0ElEQVR4nL2W2w7DIAxD42n//8ueVm0Tl4Q5hM0PFVA4OKFBBa0T" +
            "2g79YfPn3KwmWoSKd/+uoiuGqKwptp17gcS+26EKHOtRSK/uVP0YfBSUrRfK5gqfFmOUYgp9m36AAgnjueBg2oEQlc85ItSGMKed" +
            "BsUURy+uK8oka2diL0BODd9VguRqN+2cIxRRvBKJIzWI9zMOUnQFYU46VyiipEs/f4I4h7IforiNYhTm3wPkOADnXdoVI5JaOHwt" +
            "HkGtIPzZrSq5CV0t5+d9vQSZPQDm0StUZck0swAAAABJRU5ErkJggg=="},
        {"path-raster-300-6-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA00lEQVR4nMWVyxaDIAxEGY7//8vTTbUhD5oAp81CJcTrZFQAmwzI" +
            "wTjVxklb1L8XzyfxXF2zmyMSXXLPikJEgt9gKhjke1kUdS0sKtleGP1Ue02gMKnMiFqxXQc1aibKCe6rYpzpGVGeVTTsq9ge+CGr" +
            "8vw/aCVipL0bZNpzhKPbq/SrUwFnkVll7X2iHB67ucjQY9VRfA6H/kGeQ51cr2jM+qeqTRR/pooVVEpWWRXUuYxiYm7VK9hUFsUQ" +
            "YraJGksseCJ/X8Yb8LRCmFjwyrNe5l4dWTBKvYv9lAAAAABJRU5ErkJggg=="},
        {"path-raster-300-7-17", "17",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAtklEQVR4nM2W0Q7EIAgEWdP//+W9h8vlVBDRklqejJBxVEwLShOo" +
            "xrQm2xyqsvLL/VOhgJ7qUPGAApY9KTFqr10rUayyK5V4Vj4KaShMi8kgak4SC0aN+ko563JiFm8G9oNhM0ylBsGEZkA/UW5KVdFY" +
            "cV2K+tjptVVsiWtLSQyp3WPHc895XYpvskIaCvaln90g8lCShsLofSyjMMwc3CDSUHByxzYIL1l9vFSd+f4jVjf/GEQ+2C4dV/+j" +
            "2O0AAAAASUVORK5CYII="},
        {"path-raster-300-8-18", "18",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA50lEQVR4nK2WSxLEIAhEaSr3v3LPZuKHiIAJq2jrs6FKIihTYBzQ" +
            "VVaLdJz114uvNOGSatBja9UU24dlmgTzJDGlNCjf1Do4bUu6yoQWTSGFwqoABZYmnCRZWjQlIsCaelR2zAwaVNpUh5lUkxeHjwr1" +
            "ift4TZpifJyOS7PpBU0mwjDQx1odWRIR2FrFOyRSsijE2lUjcQOtNWTuaLkEzd7Wiye5dAfpwsqoMZ6s84ZMC3vT2/kdSj5E8RyF" +
            "7VhfvBMwy8UEsfGoxargBgy/ir9YfxQ9LmDLvp+3t+TK3qvPj9Wtm0jyA3kPMVe2bzTkAAAAAElFTkSuQmCC"},
        {"path-raster-300-9-19", "19",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA1UlEQVR4nMWW0QqDMAxFe8v+/5czKEPTmKQ37cD7IGrC2bE0Okib" +
            "An0hYcXr6Oo86rcVEbffoChSUzKIULHUnYHxWKQVZpJb6lWpiDijqDWPi+yyE+mW7pjrCIXaDiyKkspStQKBOpZqfZOExOqA0TCh" +
            "zGSZ6AIi4GVVejZ49z/uLz+1YL0Sq/N0sk/WRdpKZMW+14oQQwIqoBAiNj8TYGaQIqX5x+DIJipGvrBFk8ghCg/Sqw8o4+jtexIl" +
            "a1JlBn8QPYrz6/W6iDf0aFn+6Su8ZJb3vpIZLU+Gt4JNAAAAAElFTkSuQmCC"},
        {"path-raster-300-10-2", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA1UlEQVR4nLWVWw7FIAhEwbj/Lc/NTWrqawTU8lMjcDqoAYUQ07JA" +
            "tf4by1DqeL6PXysXSUmE1CNhxXHUq6Ipjuz4VNHMHZS3wnSmJKpKLxeo91Cyi2oOGUeonfNKZB8LUYih3nh4LzGJwRpJiHaGJshD" +
            "ij9R0LAcA8miiHxJktiosXsqQ2YnCMYvLVQ9KYrpFqqbFJblGwPCeldzkk72LFWluvbC9LTJqKslM1Vdso7z3o1awfH58Morp1lT" +
            "6NgnxWD3Bt0vVOzH0LOOuqjTHKhWBi/4B7YZKlmwiM32AAAAAElFTkSuQmCC"},
        {"path-raster-300-11-3", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAsUlEQVR4nO2W0Q7EIAgExdz///LcW3vVroCaNGmOJ1NgREMXjSLM" +
            "xHdUQnVJQGiLIlG/RTSwJMquC+5cQVRr+CECJS9kYH5VYWqNBr4Mxe1yT1U2jzoanI6E2o10KSSr0oZyfHaBypN9ZQMX2WTmUR2O" +
            "pQNeshelj0CMPmCjxOaCvYmTkNM6JmVYNb7r+pj4a3v82vsmYnZMLGv7Yb22F2HyxznfVy0nPyb2vfpUitaIL9W0KDjCHSbgAAAA" +
            "AElFTkSuQmCC"},
        {"path-raster-300-12-4", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAApElEQVR4nN2WQQ7DQAgDbdT/f9m9R1mwW5RK5ZjAaMIishTOwesD" +
            "NckorEUlUn1EVlpDCVsoDe+faTszKdhWGjNeAYk90EVhnrLySmXMq9crh3RGMSXBsfJIqDUSTifIfEGUIfUdijkJk5VPQvVSAQm9" +
            "VUJCdVIRCbxLn49fppUxSPRQ4T/rod2uD1GcCy87cOsD9fvrx1+g2DSym9WbMv/OMMYbezMcRwzruFcAAAAASUVORK5CYII="},
        {"path-raster-300-13-5", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAuUlEQVR4nMWW2wrDQAhEHdn//+UpJYGS7opXWp8kWU9Go7KgnA3G" +
            "cxErQs2ILEk0HWFaWhXHULRfrSmQeCgiXrzV0dFthl+glnsiXK0VBsnbZz1BhCdT0rUCBsuODirYW3D3FTctLKriHo4aivEs1T1x" +
            "YKGCyuxSNSF02iih6oagj8LmsIo6QAf+IHoo5IkqY6b/QOHjMoVivuV1TJQEUNcESV3V49OPUbTyDVw/vgTlr2pGyLUwjvYC9nYk" +
            "TS8fBeYAAAAASUVORK5CYII="},
        {"path-raster-300-14-6", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAs0lEQVR4nLXWwQ7EIAgEUMf0/3959rKHuiswIHpp0tQniJKCwxnY" +
            "vaTx8VQl0vUDCivEwJqaNIThJrhK2C0iUcjFNLSonFAUKpxZiYqHFPLiFJcU1ATFxqj4fRjmI22VdMYMynRhY0KCZoTnFUTjYcAJ" +
            "xbVq2c7wlqTroyfIDuofQZUyxYbrnKLY2K8KY96n2Ef5lWeK4s29QpmibTAb1Y+FQBITxEDc3pH+6yv29s00e+kPJC4pQcY53DAA" +
            "AAAASUVORK5CYII="},
        {"path-raster-300-15-7", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAmklEQVR4nNWTbQuEMAyDk+L//8uR8/AF2bHWhcnlQ2Wze0hnpNAS" +
            "P0Vb3aXzTftMNHcvvB+rIuomHmUYhY6vBTZWwKb4S5RcKKnF0gPUdkZZY0uiR+NhUGkbL4WBNhS/DxWvvRCnB67Yayhfu8ZR7Ha8" +
            "EAb2v0e4xsP8AZnJ22xXtKGY+58CNoVrPCRQWRJmDkgfCjZU3pTT1QrCchVQJCnr+QAAAABJRU5ErkJggg=="},
        {"path-raster-300-16-8", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAwElEQVR4nK2UyRIEIQhDiTX//8uZqj7MYhE0CrdmecYWAEMaMidl" +
            "+jBJkXufiDzlW8P/b7qopBI1S10w00CRu0Cl5ZyO2ULpn3uuCn2oQB8q0IcK9KECaEN9YKiTRN9VVbxRtWUKVQwJTZRm0VWlDX2o" +
            "gIvyB3ockJC7X4ts7gscdfh5Ls6PBkPVvMW5I61U9auGdyjJve0rLuJOi7IPFTxHeR0/wmDhAMUTXVgu5ATLtETMYAqtSScLWZm/" +
            "kGXkDXzXJknh5ICNAAAAAElFTkSuQmCC"},
        {"path-raster-300-17-9", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAuklEQVR4nMWWSw7EMAhDMer9r+xKs5j0MxBwM6pXUZM8DC2ooIXC" +
            "/RHj0+YtkkFCjVvkMAMBdSCdEkMbdSaVFKBusTm3lZS9acp+o7IXZVBd1eVCftBQDflkH+tQ9mcUNBSWu4KK4hWCQrRtiaEswXrn" +
            "zWslsDzaOEzOpyi7wBisS2X/lLvhbUtBLfmEtLKd8RSF7A5bKEVePYh1qKHo+3ghQXxXlCcDy78Q3vSUHYqiBLeTnoyHTJdkOwbZ" +
            "H03teCsPAAAAAElFTkSuQmCC"},
        {"path-raster-300-18-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA8klEQVR4nK2WWw7FIAhEGeL+tzw3uelDFBTa8mVFj4NaEBRr6D8Y" +
            "epwhTaz54zsPh1G4WBpOXZCkV4sAFYqCR7SutKr9DmhNlJxNR5bmVsxYOUAJF9TPRMkDVQnUS1EyqRp/pMCwQs2HHBiDDs2RuBeV" +
            "3naalps+bGZYhMf/fC4OaEwyGWHwV2/Fk5M4OdavKAJSJcBNvq6iMHNwttozEPu+I73rExLpOVqdxMnDIgouqNu8avGSbJlIMeGL" +
            "SgcI26aTTqpXtKcNsSZRSHR9UCYo9QC5LiVavOfyFnVbdNXNSysWf53V+EyzqIyqZa29pfwAkf8xbEd0Ng8AAAAASUVORK5CYII="},
        {"path-raster-300-19-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA30lEQVR4nM2W4Q6DIAyEKfH9X/mWaBBa2uxat2X3R7Tk83ooQdCU" +
            "ZL0xtaardkpvvGQnrXCDcia7NQDb006TFp2cjcU3KJOk78VDSYVUij2S7KiMKUc9nXkoskFJoa7Z9/eSEFKuGHVj6m1/MNc5OlpV" +
            "uF4wkQdjShmE+heXscoK6cUUB1VYOMu6syqzMEbPPwZ5hIJqwd1kWFLTcTg7A09qXrTlrPABFKJCfQVhw/rGzvArFJjyXzQoRRSI" +
            "Yt6VRAUWhZg4SrSryToh3lHrnhIa309S/jsIVwPhRr9sXS9KETJHMOazZgAAAABJRU5ErkJggg=="},
        {"path-raster-150-0-10", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAApUlEQVR4nO2W4Q6EIAyD6eL7v3JNPDUbMjKYl5Dc9Y9Cmw8dCoBF" +
            "C6ZF12llRPX6+ZC2cRKdpARJMKSmIXMk8mnZWqUk30Chk+rOB2rUNKlc9tq1wmsonE31vcyiTiVAxaK4WNnJ11CHkrjtusk/lqQJ" +
            "t/6ouGQg+7MoDqD6fyjXfMFZFMxFrwNBFKszQus0UqMCOw+c1L1etdxP32P6nOGyZVfj7MS6GlHRz4N9AAAAAElFTkSuQmCC"},
        {"path-raster-150-1-11", "11",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAjElEQVR4nO3VsRKAIAwD0KTn//9yHFzUk9JQN83GtTwqg1C4hJfV" +
            "rYi0GjDCtBqvSQir26A6EqIu0aB6EsrXTrxFEQ7VunOcqa6E4gdygZIySepPdSSDPEpwKC0hT9Rwi+BQSjdozm3Vk2VM1U78VDlR" +
            "b53lp+oJo3eS71EcLmZVnv5D4xdKaW001Xp2j6IWSeU9xXwAAAAASUVORK5CYII="},
        {"path-raster-150-2-12", "12",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA3ElEQVR4nNWW2w7DMAhDcbT//2VPk6oWUtxcp2k8paScgBtBQfOG" +
            "8EThz98pwa8jpF0hr9xtj1kgBDHJSpPczmcZRbE7qotkmhW1WrLyDRS2ofCjAtGBIqdJVrbVZ2UbyWKBorzKjQ6UFMptkKqCA9VS" +
            "mzon1p1BnlUHQB5yFsh2as8k168mbhQXbjseSLGLjnPg1mVPRkNZoSlqWSJhAoU2qRNVTZxqyQGtbhPH/DTM5uCAIekPA/cqGP5v" +
            "Ds4aJ7RiKpH6v8q+uwi825lV2kXPO3SxdLdd1cqd8waBkSthKZFZxwAAAABJRU5ErkJggg=="},
        {"path-raster-150-3-13", "13",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAz0lEQVR4nNWWwRLDIAhEwen///J2miYVHDci2kO4qfhcEBkVYk3d" +
            "CGS+71Nk0TSPAhjLo1gcbr5lXfaaJsmH1fO0qnhuQ05lkiTcbfkG/4xS7oTIBVbUbabMdmwOUId1tcCRsCpUkLKlRhXYmWBqWGfg" +
            "OWVmdpRtJPmhkCGJDbrmKgGSg4X5usJgfaauYKTsqna4kc6pUo+B06V4RL+6MxNPqLeHSPLc3p4vemT/DMJmSmq79rxiqhpW7A0O" +
            "713pf+58P/ysr/fGXx/tVWbhDb1tKlEZimNjAAAAAElFTkSuQmCC"},
        {"path-raster-150-4-14", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAv0lEQVR4nNWVWQ7DMAhEB5T7X3mqZmncFhywidTyFRt4YsAiQrxM" +
            "zk8AjcPwGjHaj+0bYaNmSWiqmjW9ASXpVDqoeRIKBS6DeVx1WO/qWp9cnI+qsiRaGctQz8W61MbBr6GkTCfKGEKFOvBE7brm5GGr" +
            "ihUkbAJZQML+2sMg+n0Ltf3MZidKy0jQMhK0jITMvpJ38uchta/Ev+HP/3H+DMUIhrEJ0h8jc1WFTW9CiXuIxMgqt7e7j37093vt" +
            "a38Azy8gVhuZqlQAAAAASUVORK5CYII="},
        {"path-raster-150-5-15", "15",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAuUlEQVR4nMWW4Q6DMAiE74jv/8q3ZEuMXQVBOnf/oPgJxWIpHMXB" +
            "kuM/jzEskw2W/3JPclAtEnoFCqtQgodii4StQlK4uvBj2GrhDDK00k4xlVWVxCm3nx0cKG7SJPKJrOoiz1HF8pwO5igK+l0rUNGi" +
            "9Unkv86g3rvDPbOPPSqbFa9DrEoie6jUfE2hcpPa0JeWoYQeirPrJopYhSLuor5nK5FB1f872t2569gTVzXvfhU84Qy9o/sFAh0h" +
            "VMki/8cAAAAASUVORK5CYII="},
        {"path-raster-150-6-16", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAwklEQVR4nMWWQRbDIAhEGZ73v/J00dpi0ApqU1YKkx9E5QmKMdiJ" +
            "NCG5BL1KJWqYCTSqxjRQFkhs56DLKkPqmR4jSbzsU/sFCsdQuGmBMGNfdSZQMZJocn246j/s0kQ4Z6KbkcmqE0lbMdnkSRg0mb2c" +
            "5N0Z1jmo3/75DpKnUBz4dYnEHu5ErbCHIh3r5h1k7NDpdjJS9/QEirKHgvOsouBdt99Bmj0EYqhoj4f9T/W9Bt8YT8lIsfBUm5ES" +
            "qP6ZN84HmVErSbVGF0YAAAAASUVORK5CYII="},
        {"path-raster-150-7-17", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAr0lEQVR4nNWVQQ7FIAhEZ4j3vzJ/0TRFv6hUmrSzUQEfaAxSYUS7" +
            "ALRvvpy0UQKj7hZPbONLZHetNnFV1Z5kkOY+inskPHTANBR3UWWdpJO48AHVNTRPVOfV6THwDy1ZN4UTlUCCjC/CFSeoLRLEQFzS" +
            "wKvXtCwVNE5SVxUUe8ZbKCILRWSh+EiT0TSUYg9FzxFG8bO9fU1MQxFZKCILRURQVTQT/0Eew1p3fee7+gEfYRdbcqsEDwAAAABJ" +
            "RU5ErkJggg=="},
        {"path-raster-150-8-18", "18",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA2klEQVR4nMWW2w4DIQhEZ4z//8s0Tbb1BgjWpLyt4NkBlF0KOmP/" +
            "AIjp0YJKv2rHE3vXgPrNanaDrHIUVZH8ZIVyRSVJmFklUtnI69BQIRK8sHQHaWLTHQQ/RbI6GMzPuxR1dEq4+jKTYrUSjcazsmss" +
            "+Cg5YImKimywHaVb8NLg3lFj1aDjSnXQPyHMoDZnjRlVIUvdQelFLHagSoz1/GTAm6UpO6yVXFJlKLvYwfInFO+hwIsJ0sFmO0j7" +
            "xM+qhpj28cC4NpIe93fa+P9iXpFy8yo/209YzfECy6ooYXO8QFgAAAAASUVORK5CYII="},
        {"path-raster-150-9-19", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAv0lEQVR4nM2VUQ7EIAhEB9P7X3k22TRbcQc0ta2dP0WfCIhGVLJ6" +
            "AGeCt4klBWMySXL8UVSkirXp6XgD6MfGOa+oJrcTPgGgWD0bKxwHzaNg16HaWMWRGlaZR7wAZfFccS+Msvb+95knWoXKCQMdosrg" +
            "t3pDoK9t68Sqf7Oufm+QD2WQ16Fy1p735dVO5eUNnSHXUUq89w2evaCFluUZlHo8g63Ez7jUK0Y+5V4lfRzKtGOzf5Ch1dX+S/pV" +
            "0x8+kf8iWnv21acAAAAASUVORK5CYII="},
        {"path-raster-150-10-2", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAwklEQVR4nN2VwQ7DIAxD42r//8uetEntRkliIw7TOKEKP9wADhjz" +
            "gXPGa/oeieRoSXcp5pJHT3qxkGwpuBoHt6AEQ7IrkXVoy/4fhW0oxB6UcKcKFH1SZK74Nbu9Iwd1rpdJkdfqUmikQFcJMayiP0H1" +
            "LUeaV7kn+GVPSH6K1qkMuq7yfIfp6qPnFPsprlB8pYWySaH0we6r0wchNUM9kNHdfCPb8SNtgvtQMWdxBTVVccnVTJf+dVv2Ubme" +
            "oqO2OIknbngoTdQTEG4AAAAASUVORK5CYII="},
        {"path-raster-150-11-3", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAj0lEQVR4nO1WQQrAMAgzsv9/OTsMBh0V007oKMupWA1WbRC0LmAx" +
            "2De7leFIPZjkmGUlxUpUTyZymmoKnnpAfay/TmYRFcWOej+adXPVQis8OB7PsrLTRqmiCed2c6UC6Z/HlipaWSvcp8sBzS0/oe0S" +
            "fm0v1/YWnKRSmUzsIBSfpfsVA3tEFe8uddoeS+UJV90gPMXwHiEAAAAASUVORK5CYII="},
        {"path-raster-150-12-4", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAj0lEQVR4nN2WzQ6AMAiDW+L7vzImXpzTDZgcRE77gS9r08OoGBUf" +
            "znTYDcjkLkaCpJEgaSRIGgkxrwqiNm+jHjnTJRTtwDoF2qM1bJc0fZA0Eip5xTQUm7XqK1RTbhAsVIAEmetj5xsnKOqS5+p8laOY" +
            "hwJrp33Jd9rJuWQfnxb4IxSHm74x9us76z6XKHAHrj4TSmwLneAAAAAASUVORK5CYII="},
        {"path-raster-150-13-5", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAoklEQVR4nM2U0Q5CMQhDaXP//5frm9HkdsDEKY9bOelGAxR3hfAl" +
            "c86efFUG5VlSF7VRV0+uxd0vXClVnHOlBorr61VUu3+Fr00Qc6jA4AQxhwoM5gruPNtXumnXJ65U0CSo53bSNkobaWeMsZgJMLhk" +
            "UGUxRZXfSNOOPotxfrfjpCv+JerKJSoOkb59ypV8sNRz9VrVtFdy9cbyT7eu+r/1ANRiG1SGDkp+AAAAAElFTkSuQmCC"},
        {"path-raster-150-14-6", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAoUlEQVR4nM2WQRKEMAgEGSr///Js7U23nAQQ3XBKRe00BKOgyYCY" +
            "5/W0W1uMkNPXQzneskLO6tfpFODfanUO2DIet8JhPGm8mhUbE+SSW6kV6zuIWK6eBZmsmre0VLFWUBcKZUcfynAHRQZYL3c7GWI1" +
            "ngy+JWos72g/r3ZAsfA2m7CqsDy65hYo5jOEbJsJgCmrWWvmf9UUq/LFuX5G2n4AG8IeSEGltPkAAAAASUVORK5CYII="},
        {"path-raster-150-15-7", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAjUlEQVR4nN3VUQrAIAwD0DR4/ytnn4Mp2s4wcP0s9VmlaAijiD6l" +
            "Oz9ew2E2qb+lED4KMdcqFOZ9lSj8j5KNUsXiXKr0xWVFb2lc2Na76TFUet3VSvh6GFR7GVpGSUmoH1A2StilYl1CmwTaJNAmoXrt" +
            "slHCHpU7H2iTQJsE2iQc+XkhbFTkJZx5V4W4AFgqE00Un3ybAAAAAElFTkSuQmCC"},
        {"path-raster-150-16-8", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAoUlEQVR4nM2WwQ6AIAxD6cL//3KNBz0gkxUQ7MkMfBZSJmByBKdO" +
            "7wVTSb7ANolFhZKrJ6nXVZ2ESi2yV7pMmMv3pS51hWgsTPkuhlGXL0xzhfupvv1ZpOhp75H1JGiaK4oo3xcX9CuTSfCGAv0qKusg" +
            "AV/nKr+OUlmvpWnKoVmnu6azLb2drXO56Y/zFxSUA2TzejsCN5lSHHbV0l6UF9UDxtEbTT96OFEAAAAASUVORK5CYII="},
        {"path-raster-150-17-9", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAgUlEQVR4nO2VUQrAIAxDk7L7Xzn7HDirHWh1bPksWh4hbSnURacu" +
            "pw4YhunoM6nBOInKnn4gE6iogFNebR+vfP2t4tkyzJ7BS1wxg6wnt0ehL+aK/q72/RJW5YoFAbutMi8OG5fvDWGwxM1wl7ALFQNv" +
            "MqkUusxI2gx1d1rJH0h1AgS8ElRVBSZLAAAAAElFTkSuQmCC"},
        {"path-raster-150-18-12", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAwElEQVR4nNWWWQ7DIAxEGSv3v7KrJkpqFrsDhC7+SQAzPA3CAE02" +
            "kLXUHWllSbottkStjIKhldlNBbeHkoKd72qt8QqTUjLL8utS211OJY4qP1xvqE4mbdMpiJNKeqW9ZzDSwiXquSrsqkpT7auG6Xp8" +
            "SqbXHOnbJc4rSgtF205aVUXjQNay1VS/RoWKqIzPU8H8e1s9QIX5HbwC7dtw0CuMUiHsWvBmkP+X0rB6hFK13ZEWzkHmHnzmElWU" +
            "DZ/rARjrIWGoOQxMAAAAAElFTkSuQmCC"},
        {"path-raster-150-19-16", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAy0lEQVR4nM2WbQ+DIAyEe43//y/fsugykJaXGya7LyotT6FoK2il" +
            "UD7UJqtsgZfPeHdtCFE9Ui2SzSzXSBao3mBHuJEgozAklajZnDPx8W0km87VWE+gsA2FxXnMUblHEofNnS+TkDGPPG4mJNH3nyBv" +
            "EZQ1+nWlSLIv68qVynkL3J6rQ5nEUZFZIVWCjGIzAhHFhKennRJq8j1xbQX/WpDRrNp/Ov9Tax8OH9kg9qHszYp6bA+V9WTElk+x" +
            "6VX302X007fQB+PMF6MvrussScN07ssAAAAASUVORK5CYII="},
        {"path-jpeg-q60-0-10", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA0ElEQVR4nM1W0Q6EIAxbF///l3vJ+XBDNijqedfEBNmoXSUw0Bog" +
            "jA8hi7Ekw2fZCcg0fVPWNsu4P/srWKrSmAJQUUn1VfDbmEwtEPOAa6JQbZBc1ZFpsCoCRYGfLTMDuxm/yXNb2FcLVG9RVXmS4kaV" +
            "YBTqqUGBq+a5Ikoj3WqecbkIQ576g6xDq1SoOb9k+0W4kkQp/LQqzUm3y+ASlXSKDajkU9D+x/bTVOwGfegZVci+jVLUT2zndDo2" +
            "SDCOOrvYFmUtUnvRY3ChZHY1Ul94iylK1NtoNQAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-1-11", "11",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAp0lEQVR4nN2TUQrDMAxDJdP7X9lltDCTdo7VeaNUX4Eoz5Hj0DGI" +
            "YS1tGhQxK2OZW5O1kSAFpIJKzYSA+iIelIBsQxEKiqfzEuVF1E5K7F6+VWZmRhlR80Hw9F52bBQvxYP4nQXUpC7SLixFjE+LnAW8" +
            "OliGNtlPUexD4U8oSqjHv+DjUewbBvQNg6MkFlA36BVjnKPP+Q78Wg4Oj+cj6lPFzZLvAlgBKtogSVYxB18AAAAASUVORK5CYII="},
        {"path-jpeg-q60-2-12", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA8ElEQVR4nK2W4RLDIAiDjef7v3J22+5aEbGBLb9aZV8DRTqwGeG+" +
            "3O9wDjExPSS1gBTGj6YJN4kzAgxchaYgPK3nfgAVJQjxQq8B+OBKKYgXNiiNhDB2SZA756J60pQXc2WfvMa2uzFVSI8elSJhtzi0" +
            "SvEbwVPwkE3x/LAblagSAn72DFoZA+q8EsbsD5OBy9aogTgtsYJC4CePwoaDUq1wJiVcwYOsZFd4jEh+B2GNoeAK0yWCnNPd3q4Z" +
            "saZcPYPwfyIS30Hy/BpSrnjclVBXefmXvto2u7mf3+CS/vvML33kFExRX8hPCy3LhmxuXggYMWzQYy0JAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-3-13", "13",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA5ElEQVR4nMWW4Q7DIAiEOdP3f+VbVrcMFBFt092PpgX3BU/RgWIF" +
            "9c5hxhtRJCvMBhzZH0BV8RsEVVdZIn3UmrI0QdhPepmy5geCUFkiyXeIN8X8ClrxPpTEqLN4+svTCiGqkiQWOxI3J8ggV3JFsSac" +
            "tLPbZxuBGoN5D4ZOUYD0yZBbvJGOKxg+sUWnYvNtvWuPvjkLBsZdVI9TrD2v6AUv244dFLweVqysV6iPXDvPSRJ36qpXePxA/hMK" +
            "t6Eg11Ecp9bbWUaxZFXNneXue31K9AYQ3s03qLO9Uq3eoeAPh42+ADVfMFfOUfpfAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-4-14", "14",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA60lEQVR4nLWW0RKFIAhEXcf//2XuNKlJAYJ6eSrcjitSBkpD4Llk" +
            "+c+opMmmNhR5g0Q6ao+U8rIpeicCrjAZz8dMpdVaUdJRU1OYkDoqtjySkoUJIkAo3Q5jNt0UBFctBzcJydpB2xJmpAFFJg1T0lpf" +
            "QcyWSLmT4cntClNPXlR/2rIfqxVZDRNCkTnKUcqMvnchawPqOiiMUqlQZXwS6SAcGx2Sy8oYPzJBezXQ/GwcXu9woLxvVXEACE97" +
            "3tdcVWuFwO/HHSOLif5WKzqHOtjt0GQeTd/B61hjKly3kRW3HdTm4hv8agUu+gHrrTRUndn+9AAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-5-15", "15",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA1UlEQVR4nN2WwRKDIAxEs4z//8vbaa3TAAEW0Ev3IsL6CIGooOWC" +
            "azPszeQeTyaJQn+Jak4/niHdRjJxgYqO5aDYR61zrEBhbRuDXEHwd1SnnTegsAWyMu1jEpreNHEQYAbn8u0sKmV5qO57NbihdDW4" +
            "lHMgSjshJxujHRRi4nX5wcClXDGaFrtvUdaex95XbdFnJdZMVOgPHzMcdPdZjQpji4iC4HmiBveV/hTFzU8qatJ0ObPtkWvwjOND" +
            "iv/mshptnMPT8R18l3VM8qjRT2I9nq/2BXf4Llb62EhsAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-6-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA8UlEQVR4nMWWwQ7EIAhEgfT/f3k2201aRQZRD8ulptDHFKhVIZ3p" +
            "u3Qe5x1jbBLbOXVAKEXlJGlIAcupSrj6gr6ckWVlUVkNZ6pIneA9EaokCjS4qspbkNZqojTxjag7Gnd3EhLctVlZqTmBjaG2NggZ" +
            "0rZJcCS5vFe3X/HivqRwGrk7VQXTfoUAtdC71hTn0x7sYSVUrRWXLBt6+vOO9a1PWFWVoaL6o3Rru+w4R4F6TofhAAX5syosTquV" +
            "EqZfKB3ROC0K+ZZrpTRL9cMB/Snvq5IfI/jvexTv0POMkhNEsw1OCx4ExKc+oic7UfYF/ABgcDhNTiWRdgAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-7-17", "17",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA2ElEQVR4nLVViw7EIAijxv//5V6yu5svYKKuyRaDWCgMB0oDVGvW" +
            "RjZ7bHcuZJvJ20GJcyOZZ7X0emqEqVSgzzRP6lPQO6cZprkISXZBjQqRso/IARnUfEqY7ruiQ8jfg+1a8V6wvGvkWX001kpWFxOd" +
            "L/sRKd4pMY4sz6D8ZagdZCuGLhcGS94Q12EQOEmJYzMIJe5AFbpnDmQFrRirtTqVFV66+raooJsXOgjDviOQstlByKmsYAZ9s4Pw" +
            "/RGgWmYShyo62Cno78D7O38N0/P9nFUdwdX8ASFQJ12sTfjAAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-8-18", "18",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA60lEQVR4nK2WgQ6DMAhEe83+/5dvyYxKK5SjjsSohT2OlrqCbTDc" +
            "j5OnGZcb88nDZw/NGwyst6pxVnMlYyjK9TAe6rXyGKXaKjBkdUmUZL1GgoaSDDKK3iqL1pV81oAU9YvgG1i3L2p1GG7zHqw1Arz8" +
            "4gpS8JwoJqvHYJj/2ThtrLSrHcWUJatCOq6ikHtEFIRpmL7ta+NxKd2eijqMozZsNAPNI58sCXXEMuJuqGpr1otu5z4KCauiCmu3" +
            "hEr2H+uqAnP/6LNoLOutqYL5+WPmxpNMfFaLp/yKMqgg2i3Oy3ejwrxnRHaA/AKY6TpDQzrzqAAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-9-19", "19",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA8ElEQVR4nLWWWQ7CMAxEOxb3v/IgUaHGa+wg/AHFy8tkcQp4KcPy" +
            "XITCOJskLH6EKZKSzJDXUs4wX6MaRqUGGaohiupLmQynV0SKCeb1cdZ4rXKTmQp6FzzqdvFJjpa2Cr7Ur7JaWTAFOV1k7xOb00NG" +
            "pnYw78cpqr9SoU3PFe5PFCiux2DPiszcV/Y6Cbze+M/GKYfnfvp9VdzBdONsYFep3qnqHi4eq4Kt9xs920FUQZkxkItqolqdfdaD" +
            "U1S4l8zzf24cdlAoCuFJTVXfgorUbWfGOcrdPe38lGP9c+VEPODsesmjWqt9O+eyvMe43gDmN0tNd27/AAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-10-2", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAzUlEQVR4nL2W4RKEIAiEhfH9X3lvppo7KZFFvfhRjsHnoqIJimdy" +
            "PPFrnuYGqPfBhqPfz6FuEXCHJFSlTSMHeSQkSdTDHyHrzQSLv2SzqiT0eCFBdJvDPTahCv9PsPpjN+VMLWPlEpEVVcYaEtZQQh0Q" +
            "zLST271uklSYQ4YllcqBYPowgbpIoGZuiDqiwK6BxppY04iEDSgxL0KpeKOamK9Tb/ayNYhAEo2iCpq+B2MbqkKKqauVlzlk7ix4" +
            "julrwldJoNgUGVXtP9+A+wE+niVfCbDI8AAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-11-3", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAxElEQVR4nOWWwQ7EIAhEB+P///JsGutu44KAmuxhOfTQwnNqYaoQ" +
            "Voh6l2Z+8UgkAwtcUWFEK2G/CNywVN2lCkGyqHehhFkFsaCfEkWB51A4h5JzKOyj+CWKSVTvccb3v3pyRk2beyWhpqhhhtteyS9I" +
            "nEIRx1AyfcgsgVuqIssh+IKDKe+gnrHioqI46JqLXhUfGM08F9UhCVbxEkKT/DND/iNvX0UpPcAkavhLiE+yVbWKhnhO0ORolzz1" +
            "rXm7VjUbnxeh1CdK4AtnSgAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-12-4", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAqklEQVR4nM2UwQ6EQAhDW+L//3I37m1NYIoyZjmYONO8FKhSyIuX" +
            "dxVaIDBW0dBqDKXF/UsNsmUKtivhAeoahSlXGkPpfJADqJN0gvgY9SVhVTmK3R2EQZK3zWMtkRmLMEhmRXLOZndYu7I9IUOxT4KR" +
            "K/dbPKrLjickrro/hRzFH1PaMytWOt1qT/dcuRV7UeqFoJpVMToVundnpTkU/hJFE1VtsGBsTvsHllEcR3Zm6NMAAAAASUVORK5C" +
            "YII="},
        {"path-jpeg-q60-13-5", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAqklEQVR4nNXV2w6EIAwE0E7j///y7NMmBij0FqM8GaQntUgBZT1g" +
            "zNOYF1HzTVQSbZNE2yTRNkmuLkj21Cm4toOfoy7PIrjqdqbg5RA7g+yrFfooQeMOIl12TgCYy+ofRscZcn8gixQj1VJT4SCxlBXW" +
            "CSBBmViQup29IoXp4QWtD33UbrBK4SA9USvGi6fHFRwkpq8JDikxntU9xPdnmD1xLWRvnDlu22h++WcjSjNqZnUAAAAASUVORK5C" +
            "YII="},
        {"path-jpeg-q60-14-6", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAw0lEQVR4nMWWQRaDMAhEgdf7X3m6qL5qDDAQbdklwmdAJVFIYHrd" +
            "gutsRZLM9rYngapv1MdpW6OuaiRluqxM6vXqTNIuSqskIVSxZs6+3oeSJ1G4DyVImC8eJWipanRdGFXkd285SEmxOk0UhaCvik5j" +
            "TDSApSFzIHGDxtzgYYEmKsZrF0XOUPFQdPza7yxOhX+ZV2n5P1AF4gxiVdXfoaUeun4OYmTt1rh+nFiadSoucKrL72F0v5r1J3CP" +
            "234JjBK/ASDDKj7vjc5sAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-15-7", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAtElEQVR4nM2VyRKDMAxDpQz//8vq0EMK0yxycKE+MGR7kRxDKDSD" +
            "gPZHDX0G6vs5Spv0XnQk4dSIoNjrYhjVxHOsbfNRmJjsqOocxjACBmf7rKCWciWnCCxVuxPJzePW7uZhjWr1YyjRLQZdMqif15UW" +
            "VH0Fx8PF35tjUf20xz+dYs9kHgppKE59P6CK88MwUTTm3G6QaShadXuzQaah6JEQMqiLKPvKKWkkPPFnwD+h/FThBdrIGVj/+9pL" +
            "AAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-16-8", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA0klEQVR4nL2W2w7DIAxD42j//8ueNLVaByTEgMZDpRJy6lyAghYO" +
            "DOYYL3eNZMPJyxR+5nZiM0FZVUcyTnR5mWQzVpIrKecmomwLhXMoq7O8/tV9FA6qQhH2qizC1Qc5M9w4nRtHXVsKsHPAhPSfCpLi" +
            "5vEjjb4WIA5U8Duo9dXjyMSRAPl5tCqgqWqJWFbV97otogaXCx8vkHNFTZhP7ALLE5uYMM+MUdkooAr1Kqsas5AZJ2n/jRBbFXxe" +
            "EciXlk6GZk+HHtP/q3YsnO2BS1zcNwrdLUSWVP+zAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-17-9", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAx0lEQVR4nM2W4Q7DIAiED+L7v/ItzZZMW6+AM+n80yryeRJFjJDN" +
            "LiPUkwGvkDAZ6oxqoa8X+x6XVPXOTOhSqLMMhksqlF7cyqpQZzm2NQ/s3Icq7NCxjeVrbg+H/U9RNnx+U3VALBN9lWRufVlSVTjl" +
            "4QbrrCYtQ/IE48Br1AdnaYktmpDfaJOWvJwo7O/QTA8UF5+JNAnpZ8I0O1C10PxJFLehiCKKpx+z9fJjLBUyhUx0cY50ln10ZH01" +
            "JdzeI426woIL+QIhHCNPw6H8ugAAAABJRU5ErkJggg=="},
        {"path-jpeg-q60-18-12", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA8ElEQVR4nJ2WSxLDMAhDLU/uf2V1umgHCH9WaXAeEsVxwKMC4tpN" +
            "US2Ra+7pBn6PUpZAiEJNOgY2VgVdik7x2xOFcoVBJetsOLIygy4Z4fL+P1iy7s5fgRqRkKLiiWmRzrVZwfKxjPC3Z48xib7B2B7r" +
            "xGNvC3WQz/ObYeZAofJgcVNVtiV9Vwjw82mXwaXBSvSzJtGmhigk3R71Ch7pj3+pSqYeFrM2CA8UnzhV0CGNewVUm34xDNSixijI" +
            "S39XN1FIfp2oV+Vb1AY3vaKnh5thYCG0dzonmzhBDc9VgZa9elG8Tae/+oK3aKJH0SKPH6qEOmLgWMOsAAAAAElFTkSuQmCC"},
        {"path-jpeg-q60-19-16", "16",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA5UlEQVR4nL2W2w7EIAhEGbL//8uzaZNuRRHBNsuDqUoPA14iKMZw" +
            "f3Yz3ezo9Fn5ZielQ2VJ/HVxy9I4kksi3WEtZDCvYU0VZiR4qEgUlgOFWi3SM6iUKEoChXF1aqa2OwE5gjl8qPHelyTDwcnuLTnC" +
            "wj+DO6I4V5UlwXcfEsxhDkMHu1BbBYf5b9jtrC0AAtSGss1aSXvvWaur4tk4dQhQxb0h27Xin+6rp6ZFf76HkvdQeIoK9wVrqpjw" +
            "KdcK0yhZFJfICIViirqO1rFweJxNF6J5H/mQRo3n4N6iEznxi7Id+gKTxzBWdAho5QAAAABJRU5ErkJggg=="},
        {"stem-beside-eight", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyUlEQVR4nK2W2xKEIAxDE8b//+XszM5qFxRsafsgl+AxIFQpTIIA" +
            "NHbc4m9Egzf41nvESeraVNQVL5D60pQWJWFwt7NW99vVP6YlljzninUosA4F1qHAOhTIMhRO2ID0HRyNRviw17ZcPYcTJYfkdaV3" +
            "IT1B1qEQTTJwHOhovlpoRzzzcTIotlb6Xsa3yYCr04esZM6VJvUNVBd31v6+Uh0KyY/XihVBcS2HXHHZbKGZMO/KWAaz6k91/xTN" +
            "jQVz+yIhX5FOyKZ8AF0BJlbHfBKWAAAAAElFTkSuQmCC"},
        {"barline-beside-eight", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAAyUlEQVR4nK2W2xKDIAxEs4z//8vbmbY2BQWzJHmQy8pxQYiCNgl8" +
            "Co4dffwNbxYNPPUeOoldG1Rd4QdiX7rSVJIN7nbW6jqc/WNaYslzrlCHMtShDHUoQx3KgDKUnbABGTs4HI3gZq9tubqPIIoBKeqK" +
            "z0J6gqhDmZpkLHCg1Xy10A4982Fyk7ZWfF/GtwnB1emDXiLnipP6BqqLK2t/X7EOZcmP14qloLCWJVdYNps0E+RdOcthXv2q4Z+i" +
            "uTExty8S8i/SCdmVF1oEJlZ/OVrBAAAAAElFTkSuQmCC"},
        {"brace-beside-eight", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAAEoAAAAkCAAAAAALRfOHAAAA20lEQVR4nK2WUQ7EIAhEmab3v/JsskmrIlIQ+aha5Dlai4KyMogM" +
            "Tliduh7XkmSQ/bdYqxL0Y+KtcWwzpqrF443kWDZPcIKYNKiFTK3VHM5xmKuw5DVVOIcS7KFoReKYKlmz8igBjqHkgSnkHYqlFgJj" +
            "r22psi2IYsAVVcVvR3mCOIeSbJKRwA+dzVeO785nPiw65daK/4f+mkioenSwlaip4qK+gRpsZu3vK2ZQ+qRXVjy8PFYGBd+dUgW3" +
            "eaVmgrqqxuouJPoe4e924wPOwgK53ToKPCsn5Ob5AReMK1JmNYIDAAAAAElFTkSuQmCC"},
        {"staff-removed-gs-pdfwrite", "10",
            "iVBORw0KGgoAAAANSUhEUgAAADsAAAAmCAAAAABfBrwqAAAAtElEQVR4nNWWiw7DIAhFgf//57ssvmCRwjBNN9LEqD0etKnKoBbc" +
            "S6LRomJ16hdk2+ugsFXJo8D7UQ3DG6JomIYl1LLbKDnrCjVnCWdrEBOR1x2WG5vQbkM6usvpelgeOTvodcj332eG1LUk1YWi7q1p" +
            "WcooZf4Fn0VZS0deeorFQ96/Y/GbOeMmLyIWJ170kjcdt64znAOVzrwMbxOeQmWyFYa7ya5sJ2AvD6mcx5n7ce94ARNrJ0chEDws" +
            "AAAAAElFTkSuQmCC"},
        {"staff-removed-gs-ps2write", "10",
            "iVBORw0KGgoAAAANSUhEUgAAADsAAAAmCAAAAABfBrwqAAAAtElEQVR4nNWWiw7DIAhFgf//57ssvmCRwjBNN9LEqD0etKnKoBbc" +
            "S6LRomJ16hdk2+ugsFXJo8D7UQ3DG6JomIYl1LLbKDnrCjVnCWdrEBOR1x2WG5vQbkM6usvpelgeOTvodcj332eG1LUk1YWi7q1p" +
            "WcooZf4Fn0VZS0deeorFQ96/Y/GbOeMmLyIWJ170kjcdt64znAOVzrwMbxOeQmWyFYa7ya5sJ2AvD6mcx5n7ce94ARNrJ0chEDws" +
            "AAAAAElFTkSuQmCC"},
        {"staff-removed-raster-300", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAD0AAAAmCAAAAABSGMxtAAAArElEQVR4nOWW4QrDIAyEc0ff/5VvbF3RuZgExxC2+9fEz4siaSA7" +
            "BWu6YubkXlZwuqAJ09ARUCOs7gPqvFGE26HQV55Lzp2wbO0lmMCxmOTjqviJtSXe4ca403DfV9EbK9hQ+ZK1Maw7qQv1t+Z6W2Bd" +
            "pFfFP6UP7fPmPloxrR89tzJaX/NWTmvnrSlKsOzz7JBXo3zsCs0b5ziU9H//M1eYHUyD7fvcEuOzwA09WB9SRU43YwAAAABJRU5E" +
            "rkJggg=="},
        {"staff-removed-raster-150", "10",
            "iVBORw0KGgoAAAANSUhEUgAAAD4AAAAmCAAAAAC5L3duAAAApklEQVR4nOWWSw7DIAxEPRb3v/JUCFqlKbKpJ20XnQ0K9sMfSAJo" +
            "XbCjxtzUw8TFo5skzxwQGjz2yRb2fRonV24kf3I/dhU5HuaFgZcq70LHy7SJ+0792CCwJnXBXKDNcM2ZJ4s4r4helZdJ+3e8PX9W" +
            "vxzdJZqffN+Z9sWV6Php6yjhlKIzTz7rvc+x+K9oK/I+E4bWbhdj6bbjtqhsJtZerjxv6Qa7oRtWeFLEhQAAAABJRU5ErkJggg=="},
        {"staff-removed-jpeg-q60", "10",
            "iVBORw0KGgoAAAANSUhEUgAAADYAAAAiCAAAAAAxaXWMAAAAmElEQVR4nNVV0Q7EIAyCxv//ZS63ZTqv2l76tPFkJAhsxlL4ghjQ" +
            "bT0zgzbE4Hq7RSQ6obGmLrdU5TbDkJz69M48ZVuzC3Lfyv5QOTAMyT1lBTOAgRsDXfa7N7DjTM0XKkfVDSU0d+UfGNJqslYohpd0" +
            "s5fLVHTTnnlMN/SUv6+KErf18FF3S169g57GiJ9vYb5kvt2PXuADTiQZTKaCbCgAAAAASUVORK5CYII="},
        {"common-time", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAACYAAAAqCAAAAAD6lHYJAAACLElEQVR4nK2US2hTQRSGzx1No5JquWmDr2ILdmdVtFFCFImPgCCC" +
            "KHYhuKvtrit3gg/Ehda6U3xshIKLirpwoRs3aiAgaINVsfapQpvWii0kMSb3yL05M3MunWAW/qv5z/9l7s2ZO8dC0HLSqeEP2cWF" +
            "gG3b4Y54NKgjVMp0RdhPACCYeFiiTGGfDus8oFattxyOOVfoAXb3s8kCzmX6thN46LvGcieoeHpO7l6+ZlVKje8l5hwj6qx+U8RL" +
            "VFw3Sth5KuznFF6UL7ij5GGPaHt4x6ku/Zf6XGzBJhfl1HXWmaYCIF6W7hyjput4BwcELPZL08KCO0WOPRVwb16aZha88p3HkIAn" +
            "ymxgwbgPy4r518qsYEG9D1stnpfBpITPbRFvjRQc9blOOKLNF9YQJ8mogw60mTGcWavqDVMI4SoYTu6j8oEJRKirhmF54GSDiCTv" +
            "ux8mLKuKcQnf2VXVf8YitWEba8Oaa8M2/ROZzfwBsUf7khmLbbsNIrZS+V9GKj8GYRDBuCr8NGJjCK0g4LgqfDRiL8DaDIAF1ZJO" +
            "NCkOMW843JSYnTNQKQtueNhvdT/vLqXKOyE0XRk1aXmu62eXYD0AV+XgerCcuGTeDxV7AdqLagw+DhG3a4pTQx0ALd/cFQ3Vkd3E" +
            "BXvSNJazg+7lahvxjEUDHwcvyLbVb21cU/jx9bO77u5fRbmU8/KMvnOeAqfeyFDuVtFoanh8YiaXy4fCTdG9Cf3J/gWgegdvg8zZ" +
            "IgAAAABJRU5ErkJggg=="},
        {"common-time-upper-half", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAACYAAAAVCAAAAAAPuMNxAAABNUlEQVR4nGP8z4AA/04eu3rt1edPrEJCQsIm1qbsCKn/cHApVQxJ" +
            "CwMDA7vjmj9QObiyG54IeVY4S3H6P2Rl/9qgFgil73j44/+bSz0GUIWuTxHKvoVABePewEz/280IERK5AlP2LxCqqhTh0v//m6CC" +
            "knehyuqhAk7Iqv43whxo9AesbB3UeIYLyKpSEV7qASn7JATlmSKr6kUKGdEfDP//t8B4NUiqXrAhh+ASJobPfTCOApLErF/IyrYw" +
            "Mcx5B+PIIkkcQYmPi0wMG+AcaSSJ+yjKXjG9OwrncCBJ8KIo42Pa+ZcBG3BE4ekwnceqisEPhRfO4IPg3EEKkH9uSKpc/jGoYlf2" +
            "/6UEXFzg0X8GYRzK/j+0hwo7P/j/nwEptFGV/f+7JEyAScxtIShhMjDjVIYMAN09IdlWCI6vAAAAAElFTkSuQmCC"},
        {"cut-time", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAACYAAAA/CAAAAACpj+UhAAACVUlEQVR4nO3VT0gUURwH8O8+/+xmZbuly9pBF4wC+wuWJuFq0D+I" +
            "unQIxT1GBF0iokNdUugQ3cKDlh0SPAZFh6BLUCQZsbJI/xVFpQ2pCGq1mdFfzOx7896bncE9dPQHC/P7vc/Mzrw383shgieujaHj" +
            "urcY8jIjDFQYXsa8BeK/VZh/rDFPlKuJ8fL5+3cArOq6ukTycHuVHCI3XnWv1y9R2Xnf4GMuy6QUsEEc1N+xNHaTL8e2XvtOaG6k" +
            "ncPUvGTmuUKN9ZpLDiOi/rJCrTYjWP4EP3WIyGXUx4uJKc7O80KaFLZyRdzgPtNhj0T+SWHLZ+UT3bZZrkacRQqzH0VEzSKI3Iv3" +
            "KWxOm/dhht+DImlSBgYtlT1muPdLJHFlYFRbjyzDiLwFyJjW2AL7/tZNKpSBao1F2bMV+MURLdvFxnwVTmtZF07K5Is6b6o7Ttge" +
            "wH40uPXYLGFzAKOc+J+jM0SoDGJED9O1LH7sgfO+sWCmBFMnKzhKZetKY/HSWOK/sqZVyULWBDsoc8Ofte0dADsg5/ebr/oziS1g" +
            "G/e7ha++7DOQBIP4loGcLxsFGgHKx0ThjO+atqHZaQ5XBQv/9GEZ4JbD8juFGyhmVjMis4VWk41wtnW+iF0CbojGNRTirmVRZ9YF" +
            "YMeS2waHxTucGlfZiz1Aw7TSVMd3c8d6BJvsb7Wb7JTWe8279XKqQj2nDkXtg7LL+cKwbPjLT7vVJgJg08UJMaht4jTx+uOHJwAi" +
            "0ViypbWjKniv/xsByk3viq3tzp4ocUK0zcSOcPoNOovYPxSGybJtLYpTAAAAAElFTkSuQmCC"},
        {"cut-time-upper-half", "-1",
            "iVBORw0KGgoAAAANSUhEUgAAACYAAAAfCAAAAACuI+AXAAABTUlEQVR4nGP8z4AGqk8x2NegCzKiK/vFzsDA+gtdGRO6wH8oJqAM" +
            "OxhVhgZYkDm/jhy4fo2BgeEPn6SkhIKjLRdC6j8cHIviRjWCzWHeL6gcXNl5OyQFPDCG3OQ/KMraoNGh0gRyyf8ny2yhCu2eIpT9" +
            "ToWIMTX9/gFW9v///6nMEDHR8zBl3zygWuf+/w9X9r8ZKihxD6osHSoQ+x9J2b9SmAMNfoOVbYTxbyEp+xuO8FEPSNkLEZiu/0jK" +
            "QF6BAZHvDP//ww1vRlL2BCXcFzMxfJkF42ghScz6g6xsExPDnI8wjhiSxHGU+LjExLAM4QQGBHiAouw109uzcA4rkgQfijIBpt3/" +
            "UARgwAWFp8N0CqsqBj8UXiSDN4JzBznckNW5/2dQw6HsnTxcXPDxfwYhHMr+v4DZ4/rw/38GNlzK/v9fFyvKJOa2CMQEABKv4XEh" +
            "sSkWAAAAAElFTkSuQmCC"},
    };
}
