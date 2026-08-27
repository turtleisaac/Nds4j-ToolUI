package io.github.turtleisaac.nds4j.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A failed backup commit must cost the user the backup, and nothing else.
 * <p>
 * The worker runs on its own thread and claims a slot before it starts, so a failure there has two
 * ways to be worse than it looks. It can die uncaught - which is how a JGit signing refusal reached
 * users as "an unexpected error occurred" after a save that had in fact succeeded, with nothing
 * connecting the dialog to the backup. And it can leave the slot claimed, after which every commit
 * for the rest of the session is turned away with "no backup commit was created", because the flag
 * that says one is in flight never clears.
 * <p>
 * Both are reachable without a project on disk: a tool with git enabled and no project path fails
 * inside the worker at the first thing it asks for, which is exactly the shape of an unchecked
 * failure arriving from JGit.
 */
@DisplayName("A backup commit that fails leaves the tool usable")
class CommitWorkerTest
{
    private Thread.UncaughtExceptionHandler previousHandler;

    @BeforeAll
    static void headless()
    {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void restoreHandler()
    {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler);
    }

    /** The constructor is private because tools are built through create(); tests need it directly. */
    private static Tool tool() throws Exception
    {
        Constructor<Tool> constructor = Tool.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /** Waits for the worker to finish, rather than assuming it has. */
    private static void awaitWorker(Tool tool) throws InterruptedException
    {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (tool.isCommitPending() && System.nanoTime() < deadline)
            Thread.sleep(10);
    }

    @Test
    @DisplayName("the slot is released and nothing dies uncaught")
    void aFailedCommitReleasesTheSlot() throws Exception
    {
        AtomicReference<Throwable> uncaught = new AtomicReference<>();
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, thrown) -> uncaught.set(thrown));

        Tool tool = tool();
        tool.setGitEnabledInternal(true);

        assertThat(tool.commit(null)).as("the commit is scheduled").isTrue();
        awaitWorker(tool);

        assertThat(uncaught.get())
                .as("an unchecked failure inside the worker must be reported as a failed backup, "
                        + "not thrown off the end of its own thread as an unexplained error")
                .isNull();
        assertThat(tool.isCommitPending())
                .as("the slot must be free again - if it is not, every later save reports that a "
                        + "commit is still running and no backup is ever made again")
                .isFalse();
    }

    @Test
    @DisplayName("a later commit is still accepted after one has failed")
    void theToolStillCommitsAfterAFailure() throws Exception
    {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, thrown) -> { });

        Tool tool = tool();
        tool.setGitEnabledInternal(true);

        tool.commit(null);
        awaitWorker(tool);

        // the property the flag exists to protect, stated as the user would notice it
        assertThat(tool.commit(null))
                .as("a failed backup must not disable backups for the rest of the session")
                .isTrue();
        awaitWorker(tool);
    }
}
