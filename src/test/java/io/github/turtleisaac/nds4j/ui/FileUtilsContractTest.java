package io.github.turtleisaac.nds4j.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Contracts for the file-chooser helpers.
 * <p>
 * These run inside Swing's rendering path: {@code FileView} methods are called once per visible
 * entry while a chooser paints. A helper that throws for some legal filename therefore does not
 * fail politely &mdash; it throws on the event dispatch thread, per row, and the dialog breaks.
 * So the operative property is <em>totality over legal filenames</em>: every name a filesystem
 * permits must produce an answer, including the ones with no dot at all.
 */
@DisplayName("File chooser helpers are total over legal filenames")
class FileUtilsContractTest
{
    @Nested
    @DisplayName("extension extraction")
    class ExtensionExtraction
    {
        @Test
        @DisplayName("returns the extension for ordinary names")
        void ordinaryNames()
        {
            assertThat(FileUtils.getExtension(new File("rom.nds"))).isEqualTo(".nds");
            assertThat(FileUtils.getExtension(new File("archive.tar.gz"))).isEqualTo(".gz");
            assertThat(FileUtils.getExtension(new File("/a/b/sprite.png"))).isEqualTo(".png");
        }

        @Test
        @DisplayName("returns no extension, rather than throwing, for names without a dot")
        void dotlessNamesAreLegal()
        {
            // Every one of these appears in a real project directory, and the last one is this
            // framework's OWN project file -- the chooser is pointed at it by design.
            for (String name : new String[]{"Projectfile", "Makefile", "LICENSE", "README", "a"})
            {
                assertThatCode(() -> FileUtils.getExtension(new File(name)))
                        .as("\"%s\" is a legal filename and must not throw", name)
                        .doesNotThrowAnyException();
                assertThat(FileUtils.getExtension(new File(name)))
                        .as("\"%s\" has no extension", name)
                        .isNull();
            }
        }

        @Test
        @DisplayName("handles dotfiles and trailing dots without throwing")
        void degenerateDotPlacement()
        {
            // A leading dot marks a hidden file, it does not introduce an extension;
            // a trailing dot leaves an empty one. Neither may crash the chooser.
            for (String name : new String[]{".gitignore", ".DS_Store", "trailing.", ".", "..", ""})
            {
                assertThatCode(() -> FileUtils.getExtension(new File(name)))
                        .as("\"%s\" must not throw", name)
                        .doesNotThrowAnyException();
            }
            assertThat(FileUtils.getExtension(new File(".gitignore")))
                    .as("a leading dot marks a hidden file, it is not an extension")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("extension filtering")
    class Filtering
    {
        @Test
        @DisplayName("accepts a matching extension regardless of case")
        void caseInsensitive()
        {
            // Windows and macOS preserve case but do not distinguish it, so a ROM extracted as
            // POKEMON.NDS is the same file type as pokemon.nds. Rejecting it hides the user's
            // ROM from the only dialog that can open it.
            FileUtils.ExtensionFilter filter = new FileUtils.ExtensionFilter("Nintendo DS ROM", ".nds");
            for (String name : new String[]{"game.nds", "game.NDS", "game.Nds", "GAME.nDs"})
                assertThat(filter.accept(new File(name)))
                        .as("\"%s\" must be accepted by a .nds filter", name)
                        .isTrue();
        }

        @Test
        @DisplayName("rejects non-matching extensions")
        void rejectsOthers()
        {
            FileUtils.ExtensionFilter filter = new FileUtils.ExtensionFilter("Nintendo DS ROM", ".nds");
            for (String name : new String[]{"game.gba", "game.txt", "ndsnotanextension"})
                assertThat(filter.accept(new File(name)))
                        .as("\"%s\" must not be accepted", name)
                        .isFalse();
        }

        @Test
        @DisplayName("always accepts directories so the chooser stays navigable")
        void directoriesAreAlwaysAccepted(@TempDir Path tmp) throws IOException
        {
            // If directories are filtered out the user cannot browse anywhere, which makes the
            // dialog useless even though every individual file decision is correct.
            Path dir = Files.createDirectory(tmp.resolve("subdir"));
            FileUtils.ExtensionFilter filter = new FileUtils.ExtensionFilter("Nintendo DS ROM", ".nds");
            assertThat(filter.accept(dir.toFile()))
                    .as("directories must remain navigable under any filter")
                    .isTrue();
        }

        @Test
        @DisplayName("accepts any of several extensions")
        void multipleExtensions()
        {
            FileUtils.ExtensionFilter filter = new FileUtils.ExtensionFilter("images", ".png", ".bmp");
            assertThat(filter.accept(new File("a.png"))).isTrue();
            assertThat(filter.accept(new File("a.BMP"))).isTrue();
            assertThat(filter.accept(new File("a.gif"))).isFalse();
        }
    }

    @Nested
    @DisplayName("recursive deletion")
    class Deletion
    {
        @Test
        @DisplayName("removes a nested tree and reports success")
        void deletesNestedTree(@TempDir Path tmp) throws IOException
        {
            Path root = Files.createDirectory(tmp.resolve("project"));
            Files.createDirectories(root.resolve("a/b/c"));
            Files.write(root.resolve("top.bin"), new byte[]{1});
            Files.write(root.resolve("a/mid.bin"), new byte[]{2});
            Files.write(root.resolve("a/b/c/deep.bin"), new byte[]{3});

            assertThat(FileUtils.clearDirectory(root.toFile()))
                    .as("a fully removable tree must report success")
                    .isTrue();
            assertThat(Files.exists(root)).isFalse();
        }

        @Test
        @DisplayName("reports the true outcome rather than assuming success")
        void reportsOutcome(@TempDir Path tmp) throws IOException
        {
            // Callers use the return value to decide whether it is safe to continue writing.
            // A method that always returns true makes that decision meaningless.
            Path empty = Files.createDirectory(tmp.resolve("empty"));
            assertThat(FileUtils.clearDirectory(empty.toFile())).isTrue();

            Path absent = tmp.resolve("never-existed");
            assertThatCode(() -> FileUtils.clearDirectory(absent.toFile()))
                    .as("a missing directory must not throw")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("does not throw when handed something that is not a directory")
        void handlesNonDirectories(@TempDir Path tmp) throws IOException
        {
            Path file = Files.write(tmp.resolve("plain.bin"), new byte[]{1});
            assertThatCode(() -> FileUtils.clearDirectory(file.toFile()))
                    .as("a plain file is misuse, but must be reported, not thrown from deep inside")
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("hexadecimal spinner formatting")
    class HexFormatting
    {
        /**
         * Reached through the public component rather than the private formatter class, so this
         * also verifies the formatter is actually installed on the editor.
         */
        private javax.swing.JFormattedTextField.AbstractFormatter formatter()
        {
            System.setProperty("java.awt.headless", "true");
            HexadecimalSpinner spinner = new HexadecimalSpinner(
                    new javax.swing.SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
            javax.swing.JFormattedTextField field =
                    ((javax.swing.JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            return field.getFormatterFactory().getFormatter(field);
        }

        @Test
        @DisplayName("formatting then parsing recovers the original value for every boundary")
        void roundTrip() throws Exception
        {
            var formatter = formatter();
            // ARM9 addresses and file offsets routinely exceed 0x7FFFFFFF. If the formatter
            // sign-extends, the text it produces cannot be parsed back and the field is unusable
            // for exactly the values a ROM hacker needs.
            int[] values = {0, 1, 0x7F, 0x80, 0xFFFF, 0x02000000, 0x7FFFFFFF, 0x80000000, 0xFFFFFFFF, -1};
            for (int value : values)
            {
                String text = formatter.valueToString(value);
                assertThat(text)
                        .as("0x%08X must format to at most 8 hex digits", value)
                        .matches("0[xX][0-9A-Fa-f]{1,8}");
                assertThat(formatter.stringToValue(text))
                        .as("0x%08X must survive format then parse", value)
                        .isEqualTo(value);
            }
        }

        @Test
        @DisplayName("a null value formats without throwing")
        void nullIsTolerated()
        {
            // Swing hands the formatter a null value whenever the editor is empty.
            var formatter = formatter();
            assertThatCode(() -> formatter.valueToString(null)).doesNotThrowAnyException();
        }
    }
}
