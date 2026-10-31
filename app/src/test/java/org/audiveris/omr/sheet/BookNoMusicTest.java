package org.audiveris.omr.sheet;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.audiveris.omr.CLI;
import org.audiveris.omr.Main;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.util.OmrExecutors;
import org.junit.Test;
import static org.junit.Assert.*;

public class BookNoMusicTest
{
    private static class TestBook extends Book
    {
        List<SheetStub> reduced;

        TestBook () { super(Path.of("no-music-test.pdf")); }

        @Override public void swapAllSheets () {}

        @Override public int reduceScores (List<SheetStub> stubs, List<Score> scores)
        {
            reduced = List.copyOf(stubs);
            return 0;
        }
    }

    private static class TestStub extends SheetStub
    {
        final boolean noMusic;
        final Exception failure;
        OmrStep latest;
        int calls;

        TestStub (Book book, int number, boolean noMusic, Exception failure)
        {
            super(book, number);
            this.noMusic = noMusic;
            this.failure = failure;
            book.addStub(this);
        }

        @Override public OmrStep getLatestStep () { return latest; }
        @Override public void printWatch (boolean force) {}
        @Override public void swapSheet () {}

        @Override public void reachStep (OmrStep target, boolean force) throws Exception
        {
            calls++;
            if (noMusic) { invalidate(); }
            if (failure != null) { throw failure; }
            if (noMusic) {
                throw new ExecutionException(new StepException("No system found"));
            }
            latest = target;
        }
    }

    private void check (boolean parallel) throws Exception
    {
        Field constantsField = Book.class.getDeclaredField("constants");
        constantsField.setAccessible(true);
        Object constants = constantsField.get(null);
        Field parallelField = constants.getClass().getDeclaredField("processAllStubsInParallel");
        parallelField.setAccessible(true);
        Constant.Boolean setting = (Constant.Boolean) parallelField.get(constants);
        boolean old = setting.isSet();
        Boolean oldDefault = OmrExecutors.defaultParallelism.getValue();
        Field cliField = Main.class.getDeclaredField("cli");
        cliField.setAccessible(true);
        Object oldCli = cliField.get(null);
        CLI cli = new CLI("BookNoMusicTest");
        cli.parseParameters("-batch");
        cliField.set(null, cli);
        try {
            setting.setValue(parallel);
            OmrExecutors.defaultParallelism.setSpecific(true);
            TestBook book = new TestBook();
            TestStub first = new TestStub(book, 1, false, null);
            TestStub empty = new TestStub(book, 2, true, null);
            TestStub last = new TestStub(book, 3, false, null);
            assertTrue(book.transcribe(book.getStubs(), new ArrayList<>(), false));
            assertEquals(List.of(first, last), book.reduced);
            assertEquals("Invalid sheet is not scheduled again for final steps", 1, empty.calls);
            assertEquals(OmrStep.last(), first.latest);
            assertEquals(OmrStep.last(), last.latest);
            assertTrue(book.transcribe(book.getStubs(), new ArrayList<>(), false));
            assertEquals("The caller may retain a stale selection", 1, empty.calls);

            TestBook blank = new TestBook();
            new TestStub(blank, 1, true, null);
            new TestStub(blank, 2, true, new StepException("No system found"));
            assertFalse("No readable page is still an error",
                    blank.transcribe(blank.getStubs(), new ArrayList<>(), false));
            assertFalse("An already invalid book must also fail",
                    blank.transcribe(blank.getStubs(), new ArrayList<>(), false));

            TestBook failed = new TestBook();
            new TestStub(failed, 1, false, null);
            new TestStub(failed, 2, false, new ExecutionException(new StepException("Bad step")));
            assertFalse("A valid page failing a step must not be hidden",
                    failed.transcribe(failed.getStubs(), new ArrayList<>(), false));

            TestBook broken = new TestBook();
            new TestStub(broken, 1, false, null);
            new TestStub(broken, 2, true, new IllegalStateException("Unexpected failure"));
            assertFalse("Invalidation must not hide an unrelated exception",
                    broken.transcribe(broken.getStubs(), new ArrayList<>(), false));
        } finally {
            cliField.set(null, oldCli);
            setting.setValue(old);
            OmrExecutors.defaultParallelism.setSpecific(oldDefault);
        }
    }

    @Test public void sequentialBookSkipsOnlyNoMusicPages () throws Exception { check(false); }
    @Test public void parallelBookSkipsOnlyNoMusicPages () throws Exception { check(true); }
}
