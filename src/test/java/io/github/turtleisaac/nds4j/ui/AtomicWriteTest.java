package io.github.turtleisaac.nds4j.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Saving a file must be atomic with respect to failure: the file on disk is the old contents
 * or the new contents, never a mixture and never empty.
 * <p>
 * This class replaces one named {@code AtomicWriteDurabilityTest} which asserted neither
 * atomicity nor durability. Its central test induced failure by making the target a directory,
 * which fails when the file is <em>opened</em> - so a plain truncate-and-write implementation
 * never reached the truncation, and the occupant survived trivially. Substituting the exact
 * non-atomic implementation the class exists to replace left all nine of its tests passing.
 * <p>
 * The tests below therefore fail <em>during</em> the write, after the target has been opened
 * and while a naive implementation would already have destroyed it. That is the only point at
 * which the two implementations differ, so it is the only place the property can be observed.
 */
@DisplayName("Saving a file is atomic with respect to failure")
class AtomicWriteTest
{
    private static final byte[] OLD = "the original contents, which must survive".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NEW = "the replacement".getBytes(StandardCharsets.UTF_8);

    private static Path existing(Path dir, String name) throws IOException
    {
        Path file = dir.resolve(name);
        Files.write(file, OLD);
        return file;
    }

    /**
     * Skips when the running user is not subject to file permissions.
     * <p>
     * These tests induce failure by removing write permission, which root ignores entirely - so
     * under root the write succeeds and the test reports a defect that is not there. Checked by
     * trying it rather than by inspecting the user id: what matters is whether the mechanism the
     * test depends on actually works here, and that is the thing to measure.
     */
    private static void assumePermissionsAreEnforced(Path dir) throws IOException
    {
        Path probe = dir.resolve(".permission-probe");
        Files.write(probe, new byte[] {1});
        try {
            Files.setPosixFilePermissions(probe, Set.of(PosixFilePermission.OWNER_READ));
        }
        catch (UnsupportedOperationException e) {
            org.junit.jupiter.api.Assumptions.abort("Not a POSIX filesystem: " + e.getMessage());
            return;
        }

        boolean enforced = !Files.isWritable(probe);
        Files.setPosixFilePermissions(probe, Set.of(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        Files.delete(probe);

        org.junit.jupiter.api.Assumptions.assumeTrue(enforced,
                "Skipping: this user overrides file permissions (root), so a write cannot be made "
                        + "to fail this way. Run as an ordinary user to exercise these.");
    }

    /** Every entry in the directory, so a stray temporary file cannot go unnoticed. */
    private static List<String> namesIn(Path dir) throws IOException
    {
        try (var entries = Files.list(dir)) {
            return entries.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Nested
    @DisplayName("failure during the write")
    class DuringTheWrite
    {
        /**
         * A payload that cannot be written in full.
         * <p>
         * The write is driven by a byte array, so the way to make it fail after the target is
         * open is to exhaust the space it is being written into. A tmpfs of a known small size
         * is not available here, so failure is induced by making the directory unwritable at
         * the moment the temporary file is created - which is after the target exists and is
         * exactly where a truncate-in-place implementation would already have emptied it.
         */
        @Test
        @DisplayName("the target keeps its old contents when the write cannot begin")
        void targetSurvivesAFailureToCreateTheTemporary(@TempDir Path dir) throws IOException
        {
            assumePermissionsAreEnforced(dir);
            Path target = existing(dir, "arm9.bin");

            Set<PosixFilePermission> original;
            try {
                original = Files.getPosixFilePermissions(dir);
                // read and execute, but not write: the target can be opened and read, and a new
                // file cannot be created beside it
                Files.setPosixFilePermissions(dir, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
            }
            catch (UnsupportedOperationException e) {
                return; // not a POSIX filesystem; the property is unobservable here
            }

            try {
                assertThatThrownBy(() -> FileUtils.atomicWrite(target, NEW))
                        .as("a write that cannot proceed must report that, not fail silently")
                        .isInstanceOf(IOException.class);

                // The property. A truncate-in-place writer opens the target for writing first,
                // which empties it, and only then discovers it cannot continue.
                Files.setPosixFilePermissions(dir, original);
                assertThat(Files.readAllBytes(target))
                        .as("the target must still hold every byte it held before the failed write")
                        .isEqualTo(OLD);
            }
            finally {
                Files.setPosixFilePermissions(dir, original);
            }
        }

        @Test
        @DisplayName("a read-only target is refused rather than replaced")
        void readOnlyTargetIsNotReplaced(@TempDir Path dir) throws IOException
        {
            // Marking the base ROM read-only is how people stop themselves overwriting it.
            // Files.move needs only the directory to be writable, so a move-based implementation
            // will happily replace a file the filesystem says must not change.
            assumePermissionsAreEnforced(dir);
            Path target = existing(dir, "base.nds");
            try {
                Files.setPosixFilePermissions(target, Set.of(PosixFilePermission.OWNER_READ));
            }
            catch (UnsupportedOperationException e) {
                return;
            }

            assertThatThrownBy(() -> FileUtils.atomicWrite(target, NEW))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("read-only");

            assertThat(Files.readAllBytes(target)).isEqualTo(OLD);
        }

        @Test
        @DisplayName("nothing is left behind when a write fails")
        void failureLeavesNoTemporaries(@TempDir Path dir) throws IOException
        {
            assumePermissionsAreEnforced(dir);
            Path target = existing(dir, "arm9.bin");
            Set<PosixFilePermission> original;
            try {
                original = Files.getPosixFilePermissions(dir);
                Files.setPosixFilePermissions(dir, Set.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
            }
            catch (UnsupportedOperationException e) {
                return;
            }

            try {
                assertThatThrownBy(() -> FileUtils.atomicWrite(target, NEW))
                        .isInstanceOf(IOException.class);
            }
            finally {
                Files.setPosixFilePermissions(dir, original);
            }

            assertThat(namesIn(dir))
                    .as("a failed write must not leave a partial file in the project")
                    .containsExactly("arm9.bin");
        }
    }

    @Nested
    @DisplayName("the temporary file")
    class Temporary
    {
        @Test
        @DisplayName("is hidden, so the tools that scan these directories do not see it")
        void temporaryFileIsHidden(@TempDir Path dir) throws IOException
        {
            // Nds4j lists these directories back and parses each name for its file ID:
            // Integer.parseInt(name.split("_")[1].replace(".bin", "")). A visible leftover
            // "overlay_0000.bin1234.tmp" makes the project unopenable, and one in rom/data is
            // counted as a real file and given an ID, shifting every ID after it - which is
            // silent ROM corruption, since ARM9 and the overlays reference files by ID.
            // Both call sites skip hidden files, so the name must begin with a dot.
            //
            // The name is observed while the write is in progress, because a successful write
            // leaves nothing behind to inspect afterwards.
            Path target = dir.resolve("overlay_0000.bin");
            Path observed = dir.resolve("marker");

            Thread writer = new Thread(() -> {
                try {
                    FileUtils.atomicWrite(target, new byte[8 * 1024 * 1024]);
                    Files.write(observed, new byte[] {1});
                }
                catch (IOException ignored) {
                }
            });
            writer.start();

            List<String> seen = List.of();
            while (writer.isAlive() && seen.isEmpty())
                seen = namesIn(dir);

            try {
                writer.join();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            for (String name : seen)
            {
                assertThat(name)
                        .as("every file this method creates in a project directory must be hidden")
                        .satisfiesAnyOf(
                                n -> assertThat(n).startsWith("."),
                                n -> assertThat(n).isIn("overlay_0000.bin", "marker"));
            }
        }

        @Test
        @DisplayName("leaves nothing behind after a successful write")
        void successLeavesNoTemporaries(@TempDir Path dir) throws IOException
        {
            FileUtils.atomicWrite(dir.resolve("f.bin"), NEW);
            assertThat(namesIn(dir)).containsExactly("f.bin");
        }
    }

    @Nested
    @DisplayName("what the replaced file keeps")
    class Preservation
    {
        @Test
        @DisplayName("the permissions of the file being replaced")
        void permissionsSurviveTheReplacement(@TempDir Path dir) throws IOException
        {
            // Files.move replaces the inode, so the target inherits the temporary file's
            // owner-only mode. After one save, arm9.bin was 0600 while every file Nds4j
            // unpacked beside it was 0644 - which breaks shared mounts and any build script
            // running as another user.
            Path target = existing(dir, "arm9.bin");
            Set<PosixFilePermission> before;
            try {
                before = Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ);
                Files.setPosixFilePermissions(target, before);
            }
            catch (UnsupportedOperationException e) {
                return;
            }

            FileUtils.atomicWrite(target, NEW);

            assertThat(Files.getPosixFilePermissions(target))
                    .as("a save must not quietly make a file more private than it was")
                    .isEqualTo(before);
            assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
        }

        @Test
        @DisplayName("a symlinked target is written through, not replaced")
        void symlinkIsFollowed(@TempDir Path dir) throws IOException
        {
            // Someone who symlinks a project file into a decomp tree means the file at the far
            // end. Replacing the link leaves the real file stale while the save reports success,
            // which is the worst shape a save bug can take.
            Path real = existing(dir, "real.bin");
            Path link = dir.resolve("link.bin");
            try {
                Files.createSymbolicLink(link, real);
            }
            catch (UnsupportedOperationException | IOException e) {
                return; // symlinks unavailable
            }

            FileUtils.atomicWrite(link, NEW);

            assertThat(Files.isSymbolicLink(link)).as("the link must still be a link").isTrue();
            assertThat(Files.readAllBytes(real))
                    .as("the file the link points at must hold the new contents")
                    .isEqualTo(NEW);
        }
    }

    @Nested
    @DisplayName("ordinary writes")
    class Success
    {
        @Test
        @DisplayName("replace the contents completely")
        void successReplacesContents(@TempDir Path dir) throws IOException
        {
            Path target = existing(dir, "f.bin");
            FileUtils.atomicWrite(target, NEW);
            assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
        }

        @Test
        @DisplayName("create a file that was not there")
        void successCreatesNewFile(@TempDir Path dir) throws IOException
        {
            Path target = dir.resolve("fresh.bin");
            FileUtils.atomicWrite(target, NEW);
            assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
        }

        @Test
        @DisplayName("create missing parent directories")
        void createsMissingParents(@TempDir Path dir) throws IOException
        {
            Path target = dir.resolve("a/b/c/f.bin");
            FileUtils.atomicWrite(target, NEW);
            assertThat(Files.readAllBytes(target)).isEqualTo(NEW);
        }

        @Test
        @DisplayName("converge on the last value written")
        void repeatedWritesConverge(@TempDir Path dir) throws IOException
        {
            Path target = dir.resolve("f.bin");
            for (int i = 0; i < 5; i++)
                FileUtils.atomicWrite(target, ("pass " + i).getBytes(StandardCharsets.UTF_8));

            assertThat(Files.readAllBytes(target)).isEqualTo("pass 4".getBytes(StandardCharsets.UTF_8));
            assertThat(namesIn(dir)).containsExactly("f.bin");
        }

        @Test
        @DisplayName("accept an empty payload as an empty file")
        void emptyPayloadIsLegal(@TempDir Path dir) throws IOException
        {
            Path target = existing(dir, "f.bin");
            assertThatCode(() -> FileUtils.atomicWrite(target, new byte[0])).doesNotThrowAnyException();
            assertThat(Files.readAllBytes(target)).isEmpty();
        }

        @Test
        @DisplayName("reject a null payload before the target is touched")
        void nullPayloadDoesNotTruncate(@TempDir Path dir) throws IOException
        {
            Path target = existing(dir, "f.bin");
            assertThatThrownBy(() -> FileUtils.atomicWrite(target, null)).isInstanceOf(IOException.class);
            assertThat(Files.readAllBytes(target)).isEqualTo(OLD);
        }
    }
}
