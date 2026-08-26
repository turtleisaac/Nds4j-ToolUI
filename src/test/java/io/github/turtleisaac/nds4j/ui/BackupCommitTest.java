package io.github.turtleisaac.nds4j.ui;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The backup commit must not depend on how the user has configured commit signing.
 * <p>
 * A tool that commits on the user's behalf inherits their global git config, and
 * {@code gpg.format = ssh} is ordinary now - GitHub supports SSH commit signing, so people set it
 * once and forget it. JGit refused such a repository outright, and the refusal was an unchecked
 * {@code IllegalArgumentException}: it escaped the worker's handlers, died in the git thread, and
 * reached the user as "an unexpected error occurred" after every single save. The save itself had
 * worked. Only the backup was missing, and nothing said which.
 * <p>
 * Signing an automatic local backup asserts nothing about who made the change, so the fix is to
 * not sign - but that only became possible on JGit 7, which is why the version and the flag are
 * pinned together here. Both halves are asserted: that signing off works, and that leaving the
 * decision to the user's config still fails, so this cannot quietly stop testing anything.
 */
@DisplayName("A backup commit ignores the user's signing configuration")
class BackupCommitTest
{
    /** A project whose owner signs commits with SSH, which is all it takes to reproduce. */
    private static Git repositoryWithSshSigning(Path dir) throws Exception
    {
        Git git = Git.init().setDirectory(dir.toFile()).call();

        StoredConfig config = git.getRepository().getConfig();
        config.setString("user", null, "name", "A Project Owner");
        config.setString("user", null, "email", "owner@example.com");
        config.setString("gpg", null, "format", "ssh");
        config.save();

        Files.write(dir.resolve("personal.narc"), new byte[] {1, 2, 3});
        git.add().addFilepattern(".").call();
        return git;
    }

    @Test
    @DisplayName("a commit with signing turned off succeeds where the user signs with SSH")
    void anUnsignedBackupCommitSucceeds(@TempDir Path dir) throws Exception
    {
        try (Git git = repositoryWithSshSigning(dir)) {
            assertThatCode(() -> git.commit().setSign(Boolean.FALSE).setMessage("backup").call())
                    .as("a backup commit must not be blocked by how the user signs their own work")
                    .doesNotThrowAnyException();

            assertThat(git.log().call()).as("and it must actually produce a commit").hasSize(1);
        }
    }

    @Test
    @DisplayName("the same commit still fails if signing is left to the user's configuration")
    void signingFromConfigurationStillFails(@TempDir Path dir) throws Exception
    {
        // The control. Without it, this class would keep passing if setSign were dropped and JGit
        // had simply started tolerating the config - and the guard would be measuring nothing.
        try (Git git = repositoryWithSshSigning(dir)) {
            assertThatThrownBy(() -> git.commit().setMessage("backup").call())
                    .as("if this stops throwing, the reason for setSign(false) is gone and this "
                            + "test should be removed rather than left as decoration")
                    .isInstanceOf(Exception.class);
        }
    }
}
