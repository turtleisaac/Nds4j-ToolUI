package io.github.turtleisaac.nds4j.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The output has to reach a file, because for the people running this it reaches nothing else.
 * <p>
 * A tool built on this is launched by double-clicking a jar, which on Windows means no console
 * exists at all. Every stack trace the program printed was written to a stream with no reader,
 * and the error dialog told the user to consult a command line they did not have. So what is
 * asserted here is not that a logger was configured - it is that something printed the ordinary
 * way, by code that knows nothing about logging, can be read back out of a file afterwards.
 */
@DisplayName("Program output is kept where a user can find it")
class ToolLogTest
{
    private PrintStream originalOut;
    private PrintStream originalErr;
    private String originalHome;

    @BeforeEach
    void isolate(@TempDir Path home)
    {
        originalOut = System.out;
        originalErr = System.err;
        originalHome = System.getProperty("user.home");
        System.setProperty("user.home", home.toString());
        reset();
    }

    @AfterEach
    void restore()
    {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (originalHome != null)
            System.setProperty("user.home", originalHome);
        reset();
    }

    /** begin() is once-per-process by design, so a test has to clear it to run a second one. */
    private static void reset()
    {
        try {
            Field field = ToolLog.class.getDeclaredField("logFile");
            field.setAccessible(true);
            field.set(null, null);
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError("ToolLog.logFile could not be cleared", e);
        }
    }

    @Test
    @DisplayName("what a program prints can be read back out of the file")
    void printedOutputIsKept() throws Exception
    {
        Path file = ToolLog.begin("TestTool");
        assertThat(file).as("a log file").isNotNull();

        // deliberately the plainest possible calls: this has to work for the hundreds of existing
        // print statements that will never be converted to anything
        System.out.println("a line on stdout");
        System.err.println("a line on stderr");
        new RuntimeException("a failure nobody caught").printStackTrace();
        System.out.flush();
        System.err.flush();

        assertThat(Files.readString(file))
                .as("the three things a user is asked to send after a crash")
                .contains("a line on stdout")
                .contains("a line on stderr")
                .contains("a failure nobody caught");
    }

    @Test
    @DisplayName("the console still receives everything as well")
    void theConsoleIsNotStolen()
    {
        ByteArrayOutputStream console = new ByteArrayOutputStream();
        System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));

        ToolLog.begin("TestTool");
        System.out.println("still on the console");
        System.out.flush();

        assertThat(console.toString(StandardCharsets.UTF_8))
                .as("running from a terminal must behave exactly as it did before")
                .contains("still on the console");
    }

    @Test
    @DisplayName("the log lives outside any project, where a backup commit cannot sweep it up")
    void theLogIsNotInsideAProject(@TempDir Path project)
    {
        Path file = ToolLog.begin("TestTool");

        // A project-based tool commits backups with "git add ." over the project directory. A log
        // written there would be committed into the user's own history on every single save.
        assertThat(file).isNotNull();
        assertThat(file.toAbsolutePath().startsWith(project.toAbsolutePath()))
                .as("the log must not be anywhere a project could contain it")
                .isFalse();
        assertThat(file.toAbsolutePath().startsWith(Path.of(System.getProperty("user.home"))))
                .as("it belongs with the user's other per-program files")
                .isTrue();
    }

    @Test
    @DisplayName("the previous session is kept beside the current one")
    void thePreviousRunSurvives() throws Exception
    {
        Path first = ToolLog.begin("TestTool");
        System.out.println("the run that worked");
        System.out.flush();
        reset();

        Path second = ToolLog.begin("TestTool");
        System.out.println("the run that did not");
        System.out.flush();

        assertThat(Files.readString(second)).contains("the run that did not");
        assertThat(Files.readString(first.resolveSibling("log-previous.txt")))
                .as("\"it worked, then I restarted and it did not\" needs both runs")
                .contains("the run that worked");
    }

    @Test
    @DisplayName("a log that cannot be opened does not stop the program starting")
    void aFailedLogIsNotFatal(@TempDir Path dir) throws Exception
    {
        // user.home is a regular file, so the log directory cannot be created. Chosen over a
        // permission because root ignores permissions and this has to hold there too.
        Path notADirectory = dir.resolve("home-is-a-file");
        Files.write(notADirectory, new byte[] {1});
        System.setProperty("user.home", notADirectory.toString());

        PrintStream before = System.out;

        assertThatCode(() -> ToolLog.begin("TestTool"))
                .as("a convenience must never be able to stop the program")
                .doesNotThrowAnyException();
        assertThat(ToolLog.getLogFile()).as("and it must admit there is no file").isNull();
        assertThat(System.out).as("the streams are left exactly as they were").isSameAs(before);
    }

    @Test
    @DisplayName("a program failing in a loop cannot fill the disk")
    void theLogIsBounded() throws Exception
    {
        // Found by running the real jar rather than reasoning about it: headless, the uncaught
        // handler's own dialog throws, which re-enters the handler, which prints again. The first
        // version of this wrote 326MB in sixty seconds. A log that can consume a user's disk is a
        // worse failure than the one it was added to diagnose.
        // Discard the console side before begin() captures it. Ten megabytes has to be written
        // to reach the cap, and surefire echoes whatever a test prints into the build log - the
        // first version of this put 40,000 lines into every CI run. The file still receives it
        // all, which is what is being measured.
        System.setOut(new PrintStream(OutputStream.nullOutputStream(), true, StandardCharsets.UTF_8));

        Path file = ToolLog.begin("TestTool");
        assertThat(file).isNotNull();

        String line = "a failure repeating without pause".repeat(20);
        for (int i = 0; i < 40_000; i++)
            System.out.println(line);
        System.out.flush();

        assertThat(Files.size(file))
                .as("the file must stop growing rather than follow the program down")
                .isLessThan(12L * 1024 * 1024);
        assertThat(Files.readString(file))
                .as("and it must say why it stops, or the truncation reads as the crash")
                .contains("reached its");
    }
}
