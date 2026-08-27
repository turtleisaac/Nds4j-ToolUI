package io.github.turtleisaac.nds4j.ui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Keeps a copy of everything the program prints, in a file the user can actually find.
 * <p>
 * A tool built on this is launched by double-clicking a jar. On Windows that runs through
 * <code>javaw</code>, which has no console at all; on macOS the output goes to the unified log
 * rather than a terminal. So a stack trace printed to <code>System.err</code> is written to a
 * stream nobody can read, and an error dialog telling the user to check the command line is
 * pointing at something that does not exist. The diagnosis is discarded at the exact moment it
 * is needed - when a user is trying to report what went wrong.
 * <p>
 * Rather than convert several hundred existing print statements, this copies the two streams
 * they already use. Everything printed anywhere in the application, including a stack trace from
 * an uncaught exception handler, lands in the file without a single call site changing. The
 * original streams still receive it, so running from a terminal behaves exactly as before.
 * <p>
 * The file is deliberately <b>not</b> inside the project directory. A project-based tool commits
 * backups with <code>git add .</code> over that directory, so a log written there would be
 * committed into the user's project history on every save.
 * <p>
 * Nothing here is allowed to stop a program starting. A log is a convenience; if the file cannot
 * be opened - a read-only home directory, a permission the user does not have - the streams are
 * left alone and {@link #getLogFile()} reports that there is no file.
 */
public final class ToolLog
{
    /**
     * The most this will write in one session.
     * <p>
     * Not a tidiness limit. A program that fails in a loop - an exception handler whose own
     * dialog throws, a retry that never gives up - prints without pause, and the first version of
     * this wrote 326MB in sixty seconds when that happened. Filling a user's disk is a worse
     * failure than the one being diagnosed. Ten megabytes is far more than any bug report needs
     * and small enough to send.
     */
    private static final long MAXIMUM_BYTES = 10L * 1024 * 1024;

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private static volatile Path logFile;

    private ToolLog() {}

    /**
     * Starts copying {@code System.out} and {@code System.err} into a log file for this program.
     * <p>
     * Call this before anything else, and before installing an uncaught exception handler: the
     * streams are replaced here, and anything that captured the originals beforehand keeps
     * writing only to them.
     *
     * @param programName the name of the program, used for the directory the log lives in
     * @return the file being written to, or {@code null} if no file could be opened
     */
    public static synchronized Path begin(String programName)
    {
        if (logFile != null)
            return logFile;

        Path file;
        OutputStream out;
        try {
            Path directory = logDirectory(programName);
            Files.createDirectories(directory);
            file = directory.resolve("log.txt");

            // Truncated per run rather than appended. A log that grows without bound is one the
            // user eventually cannot send, and the run that matters is the one that just failed.
            // The previous run is kept beside it, because "it worked, then I restarted and it
            // did not" is a report that needs both.
            Path previous = directory.resolve("log-previous.txt");
            if (Files.exists(file))
                Files.move(file, previous, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            out = new FileOutputStream(file.toFile(), true);
        }
        catch (IOException | RuntimeException e) {
            // No file, no logging, and no interference with a program that was about to start.
            System.err.println("[WARNING]: No log file could be opened, so this session's output "
                    + "will only appear on the console: " + e);
            return null;
        }

        System.setOut(new PrintStream(new TeeOutputStream(System.out, out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new TeeOutputStream(System.err, out), true, StandardCharsets.UTF_8));
        logFile = file;

        // Touching the root logger here, after the streams are replaced, is what routes
        // java.util.logging into this file: its default handler writes to System.err, and it
        // resolves that when the handler is built. Build it now, while System.err is the copy.
        // With slf4j-jdk14 on the classpath that carries JGit's diagnostics in too - which had
        // no binding at all before, so every one of them was being discarded.
        Logger.getLogger("");

        System.out.println("=== " + programName + " started " + LocalDateTime.now().format(TIMESTAMP)
                + " === (" + System.getProperty("os.name") + ", Java "
                + System.getProperty("java.version") + ")");
        return file;
    }

    /**
     * @return the file this session's output is being copied to, or {@code null} if there is none
     */
    public static Path getLogFile()
    {
        return logFile;
    }

    /**
     * The conventional per-user location for a program's logs on this platform. Never the project
     * directory, which a backup commit would sweep up.
     */
    private static Path logDirectory(String programName)
    {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");

        if (os.contains("win"))
        {
            String appData = System.getenv("APPDATA");
            Path base = (appData == null || appData.isBlank()) ? Paths.get(home) : Paths.get(appData);
            return base.resolve(programName).resolve("logs");
        }
        if (os.contains("mac"))
            return Paths.get(home, "Library", "Logs", programName);

        String stateHome = System.getenv("XDG_STATE_HOME");
        Path base = (stateHome == null || stateHome.isBlank())
                ? Paths.get(home, ".local", "state") : Paths.get(stateHome);
        return base.resolve(programName);
    }

    /** Writes to both streams; a failure on the file must not cost the console its output. */
    private static final class TeeOutputStream extends OutputStream
    {
        private final OutputStream console;
        private final OutputStream file;
        private volatile boolean fileFailed;
        private long written;

        TeeOutputStream(OutputStream console, OutputStream file)
        {
            this.console = console;
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException
        {
            console.write(b);
            if (budgetRemains(1))
                toFile(() -> file.write(b));
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            console.write(b, off, len);
            if (budgetRemains(len))
                toFile(() -> file.write(b, off, len));
        }

        /**
         * Whether there is room left, saying so once when there is not. The console keeps
         * everything either way - what is being bounded is the file on the user's disk.
         */
        private synchronized boolean budgetRemains(int length)
        {
            if (fileFailed)
                return false;

            written += length;
            if (written <= MAXIMUM_BYTES)
                return true;

            fileFailed = true;
            try {
                file.write(((System.lineSeparator() + "[WARNING]: This log reached its "
                        + (MAXIMUM_BYTES / 1024 / 1024) + "MB limit and stops here. Something is "
                        + "printing without pause - the repeated lines above are the place to look."
                        + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)));
                file.flush();
            }
            catch (IOException ignored) {
                // the file has gone as well; the console still has everything
            }
            return false;
        }

        @Override
        public void flush() throws IOException
        {
            console.flush();
            toFile(file::flush);
        }

        /**
         * The file is best effort. A disk that fills up mid-session must not start throwing out of
         * every println in the program, so the first failure stops further attempts and says so
         * once - on the console, which is still working.
         */
        private void toFile(IoAction action)
        {
            if (fileFailed)
                return;
            try {
                action.run();
            }
            catch (IOException e) {
                fileFailed = true;
                try {
                    console.write(("[WARNING]: The log file could not be written to and has been "
                            + "abandoned for this session: " + e + System.lineSeparator())
                            .getBytes(StandardCharsets.UTF_8));
                }
                catch (IOException ignored) {
                    // the console has gone too; there is nowhere left to report anything
                }
            }
        }

        private interface IoAction { void run() throws IOException; }
    }
}
