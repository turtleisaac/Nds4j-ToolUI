package io.github.turtleisaac.nds4j.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Durability guarantees for saving a user's project files.
 * <p>
 * The property that matters is the one a database calls atomicity: at every instant, an observer
 * reading the target path sees either the complete old contents or the complete new contents,
 * and never a truncated or partially-written file. A save implemented as
 * "open for writing, truncate, then write" violates this &mdash; the window between truncation and
 * the last byte is a window in which a crash, a full disk, or a yanked USB stick destroys data
 * that the user still believes is on disk.
 * <p>
 * These tests assert the guarantee itself, not the mechanism: they check what survives a failed
 * write, not which system calls were used to achieve it.
 */
@DisplayName("Saving a file is atomic with respect to failure")
class AtomicWriteDurabilityTest
{
    private static final byte[] OLD = "the original contents that must not be lost".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW = "replacement".getBytes(StandardCharsets.UTF_8);

    /** Any file the implementation created and failed to clean up. */
    private static List<Path> strayFiles(Path dir, Path... expected) throws IOException
    {
        List<Path> allowed = List.of(expected);
        try (var stream = Files.list(dir))
        {
            return stream.filter(p -> !allowed.contains(p)).toList();
        }
    }

    @Test
    @DisplayName("a successful write replaces the contents completely")
    void successReplacesContents(@TempDir Path dir) throws IOException
    {
        Path target = dir.resolve("data.bin");
        Files.write(target, OLD);

        FileUtils.atomicWrite(target, NEW);

        assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
    }

    @Test
    @DisplayName("a successful write into a fresh path creates it")
    void successCreatesNewFile(@TempDir Path dir) throws IOException
    {
        Path target = dir.resolve("fresh.bin");
        FileUtils.atomicWrite(target, NEW);
        assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
    }

    @Test
    @DisplayName("a successful write leaves no temporary files behind")
    void successLeavesNoTemporaries(@TempDir Path dir) throws IOException
    {
        Path target = dir.resolve("data.bin");
        FileUtils.atomicWrite(target, NEW);

        assertThat(strayFiles(dir, target))
                .as("the save must not leave scratch files in the user's project directory")
                .isEmpty();
    }

    @Test
    @DisplayName("a failing write leaves the previous contents completely intact")
    void failureDoesNotDestroyExistingData()
            throws IOException
    {
        // The whole point of the guarantee. The failure is induced by making the move step
        // impossible (the destination is a non-empty directory), which is the closest a test
        // can get to "the write died partway" without killing the JVM.
        Path dir = Files.createTempDirectory("atomic");
        try
        {
            Path target = dir.resolve("target");
            Files.createDirectory(target);
            Files.write(target.resolve("occupant"), OLD);

            assertThatCode(() -> FileUtils.atomicWrite(target, NEW))
                    .as("an impossible write must report failure rather than pretend success")
                    .isInstanceOf(IOException.class);

            assertThat(Files.readAllBytes(target.resolve("occupant")))
                    .as("the pre-existing data must be byte-for-byte unchanged after a failed save")
                    .isEqualTo(OLD);
        }
        finally
        {
            try (var walk = Files.walk(dir))
            {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    @DisplayName("a failing write leaves no temporary files behind")
    void failureLeavesNoTemporaries() throws IOException
    {
        Path dir = Files.createTempDirectory("atomic");
        try
        {
            Path target = dir.resolve("target");
            Files.createDirectory(target);
            Files.write(target.resolve("occupant"), OLD);

            try { FileUtils.atomicWrite(target, NEW); }
            catch (IOException expected) { /* the point of this test is what is left behind */ }

            assertThat(strayFiles(dir, target))
                    .as("a failed save must clean up after itself, or the project fills with debris")
                    .isEmpty();
        }
        finally
        {
            try (var walk = Files.walk(dir))
            {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    @DisplayName("a null payload is rejected before the target is touched")
    void nullPayloadDoesNotTruncate(@TempDir Path dir) throws IOException
    {
        // A bug upstream that produces no data must not manifest as an emptied file.
        Path target = dir.resolve("data.bin");
        Files.write(target, OLD);

        assertThatCode(() -> FileUtils.atomicWrite(target, null)).isInstanceOf(IOException.class);

        assertThat(Files.readAllBytes(target))
                .as("a rejected save must not have modified the target")
                .isEqualTo(OLD);
    }

    @Test
    @DisplayName("missing parent directories are created rather than failing the save")
    void createsMissingParents(@TempDir Path dir) throws IOException
    {
        Path target = dir.resolve("a").resolve("b").resolve("data.bin");
        FileUtils.atomicWrite(target, NEW);
        assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
    }

    @Test
    @DisplayName("repeated writes converge on the last value")
    void repeatedWritesAreIdempotentInTheLastWriter(@TempDir Path dir) throws IOException
    {
        Path target = dir.resolve("data.bin");
        for (int i = 0; i < 25; i++)
            FileUtils.atomicWrite(target, ("revision " + i).getBytes(StandardCharsets.UTF_8));

        assertThat(Files.readAllBytes(target)).isEqualTo("revision 24".getBytes(StandardCharsets.UTF_8));
        assertThat(strayFiles(dir, target))
                .as("25 saves must not accumulate 25 temp files")
                .isEmpty();
    }

    @Test
    @DisplayName("an empty payload is written as an empty file, not rejected")
    void emptyPayloadIsLegal(@TempDir Path dir) throws IOException
    {
        // Zero-length files are legal NARC entries, so an empty payload is data, not an error.
        Path target = dir.resolve("empty.bin");
        FileUtils.atomicWrite(target, new byte[0]);
        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.size(target)).isZero();
    }
}
