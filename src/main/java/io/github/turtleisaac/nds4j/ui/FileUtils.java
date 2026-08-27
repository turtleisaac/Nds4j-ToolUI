package io.github.turtleisaac.nds4j.ui;

import io.github.turtleisaac.nds4j.framework.Buffer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contains data and methods which assist in the disk access operations of a <code>Tool</code>
 */
public class FileUtils
{
    /**
     * The name of the file contained within a project directory which contains any info stored by the developer
     */
    public static final String projectFileName = "Projectfile";
    /**
     * The name of the folder within a project directory which contains the unpacked ROM data
     */
    public static final String unpackedRomFolderName = "rom";
    /**
     * The accepted file extensions for Nintendo DS ROMs
     */
    public static final String[] ndsExtensions = {".nds", ".srl"};
    /**
     * A <code>ExtensionFilter</code> which displays only files with Nintendo DS ROM file extensions
     */
    protected static final ExtensionFilter romFilter = new ExtensionFilter("Nintendo DS ROM", ndsExtensions);
    /**
     * A <code>FileFilter</code> which displays only project files or folders which directly contain one.
     */
    protected static final FileFilter projectFilter = new FileFilter()
    {
        @Override
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory())
                return true;
            return pathname.getName().equals("Projectfile");
        }

        @Override
        public String getDescription()
        {
            return "Nintendo DS Project";
        }
    };

    protected static String getExtension(File f)
    {
        int i = f.getName().lastIndexOf('.');
        return (i > 0 && i < f.getName().length() - 1) ? f.getName().substring(i) : null;
    }

    /**
     * Given the path to a project, returns the path to the project file inside of it
     * @param projectPath a <code>String</code> containing the absolute path of a project directory
     * @return a <code>String</code>
     */
    public static String getProjectfilePath(String projectPath)
    {
        return Path.of(projectPath, projectFileName).toString();
    }

    /**
     * Given the path to a project, returns the path to the unpacked ROM data inside of it
     * @param projectPath a <code>String</code> containing the absolute path of a project directory
     * @return a <code>String</code>
     */
    public static String getProjectUnpackedRomPath(String projectPath)
    {
        return Path.of(projectPath, unpackedRomFolderName).toString();
    }


    /**
     * Recursively deletes the contents of a given directory, then deletes the directory itself
     * @param directory a <code>File</code> representing a file to delete
     * @return a <code>boolean</code> containing whether deletion occurred successfully
     */
    protected static boolean clearDirectory(File directory)
    {
        if (directory == null || directory.getPath().isBlank())
            return false;

        if (directory.isDirectory())
        {
            File[] subfiles = directory.listFiles();
            if (subfiles == null) // the directory could not be read
                return false;

            for (File subfile : subfiles)
            {
                if (subfile.isDirectory())
                {
                    if (!clearDirectory(subfile))
                        return false;
                }
                else
                {
                    if (!subfile.delete())
                        return false;
                }
            }
        }
        return directory.delete();
    }

    /**
     * Writes the provided data to the given file without ever truncating the existing one.
     * <p>
     * The data goes to a sibling temporary file, is forced to the storage device, and is then
     * moved into place, so the target is only ever the old contents or the new ones - never a
     * half-written mixture, and never empty. A failure part way through leaves the original
     * intact and removes the temporary file.
     * <p>
     * The {@code force} is what makes this durable rather than merely atomic. Without it,
     * {@link Files#write} returns once the operating system has the data in its page cache and
     * the rename is journaled while the contents are not - so a power loss can leave a target
     * that is present, renamed, and empty. That is the outcome this method exists to prevent,
     * and it costs one flush per file.
     *
     * @param target a <code>Path</code> to the file to write
     * @param data a <code>byte[]</code> containing the data to write
     * @throws IOException if an error occurs while writing
     */
    protected static void atomicWrite(Path target, byte[] data) throws IOException
    {
        StagedWrite staged = stage(target, data);
        try {
            staged.commit();
        }
        catch (IOException | RuntimeException e) {
            staged.discard();
            throw e;
        }
    }

    /**
     * Writes every entry as one batch: all of them are staged first, and only once all have been
     * written and flushed is any of them moved into place.
     * <p>
     * This is for a save that spans several files, where finishing half of it is worse than not
     * starting. A ROM project is exactly that - a new {@code personal.narc} beside an old TM table
     * in {@code arm9.bin} is a specific corruption, and the caller cannot tell it happened because
     * the failure arrives from whichever file failed rather than from the save as a whole.
     * <p>
     * <b>What this does and does not promise.</b> Staging is where a save actually fails: a full
     * disk, a bad path, a read-only file, an I/O error. All of that now happens before anything is
     * visible, and a failure leaves every target exactly as it was. What remains is the sequence
     * of renames at the end - each is atomic on its own, but the JVM being killed between two of
     * them still leaves some files new and some old. Closing that needs a journal and a recovery
     * pass on open, which this is not.
     *
     * @param writes the data to write, keyed by target path
     * @throws IOException if any entry could not be staged, before any file has been replaced; or
     *         if a rename fails, in which case earlier entries in the batch are already written
     */
    protected static void atomicWriteAll(Map<Path, byte[]> writes) throws IOException
    {
        if (writes == null)
            throw new IOException("No writes were provided");

        List<StagedWrite> staged = new ArrayList<>(writes.size());
        try {
            for (Map.Entry<Path, byte[]> write : writes.entrySet())
                staged.add(stage(write.getKey(), write.getValue()));
        }
        catch (IOException | RuntimeException e) {
            // nothing has been moved, so discarding the temporary files leaves every target as it
            // was - which is the whole point of staging the batch before committing any of it
            for (StagedWrite pending : staged)
                pending.discard();
            throw e;
        }

        for (StagedWrite pending : staged)
            pending.commit();
    }

    /**
     * A write that has been fully written and flushed to a temporary file beside its destination,
     * and needs only the rename to take effect.
     */
    private record StagedWrite(Path temp, Path destination)
    {
        void commit() throws IOException
        {
            try {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e) {
                // The temporary file is a sibling of the destination, so this should be
                // unreachable. If a filesystem does refuse anyway, say so rather than quietly
                // completing a move that no longer has the property the caller asked for.
                throw new IOException("Could not replace " + destination + " atomically. The file"
                        + " has not been modified.", e);
            }
        }

        void discard()
        {
            try {
                Files.deleteIfExists(temp);
            }
            catch (IOException ignored) {
                // a stray dot-file is not worth failing a save that has already failed
            }
        }
    }

    /**
     * Does everything a write needs except the rename: resolves the destination, refuses the cases
     * that must not be written, and writes and flushes the data to a temporary sibling.
     */
    private static StagedWrite stage(Path target, byte[] data) throws IOException
    {
        if (data == null)
            throw new IOException("No data was provided to write to " + target);

        Path parent = target.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        // A symlinked target is written through, not replaced: someone who has pointed a project
        // file at a decomp tree means the file at the far end, and Files.move would replace the
        // link itself and leave the real file stale.
        Path destination = Files.isSymbolicLink(target) ? Files.readSymbolicLink(target) : target;
        if (!destination.isAbsolute() && parent != null)
            destination = parent.resolve(destination);

        Path destinationParent = destination.getParent();
        Path tempDirectory = destinationParent != null ? destinationParent : Path.of(".");

        // The leading dot matters. This file is a sibling of the real ones, and it survives if
        // the process dies mid-write - which is the very case this method is for. Nds4j lists
        // these directories back and parses the file names for their IDs, so a visible stray
        // "overlay_0000.bin1234.tmp" makes the project unopenable, and in rom/data it is counted
        // as a real file and given an ID, shifting every ID after it. Both call sites there skip
        // hidden files, so a dot keeps it out of the way entirely.
        // Files.move only needs the directory to be writable, so a read-only file would be
        // replaced without complaint. Marking the base ROM read-only is how people protect it,
        // and the old writer honoured that because it opened the file itself.
        if (Files.exists(destination) && !Files.isWritable(destination))
        {
            throw new IOException(destination + " is read-only, so it has not been modified. "
                    + "Remove the write protection first if the change is intended.");
        }

        Path temp = Files.createTempFile(tempDirectory,
                "." + destination.getFileName().toString(), ".tmp");
        try {
            // The permissions of the file being replaced, so the move does not silently hand back
            // a file that is more private than the one it replaced. Files.move replaces the inode,
            // so without this every saved file drops to the temporary file's owner-only mode.
            Set<PosixFilePermission> permissions = null;
            try {
                if (Files.exists(destination))
                    permissions = Files.getPosixFilePermissions(destination);
            }
            catch (UnsupportedOperationException | IOException ignored) {
                // not a POSIX filesystem, or unreadable - the move will simply keep the default
            }

            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(data));
                channel.force(true);
            }

            if (permissions != null)
            {
                try {
                    Files.setPosixFilePermissions(temp, permissions);
                }
                catch (UnsupportedOperationException | IOException ignored) {
                    // best effort; a wrong mode is worth less than a failed save
                }
            }

            return new StagedWrite(temp, destination);
        }
        catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /**
     * Prompts the user for a location to write the provided data to, then writes it there
     * @param parent determines the <code>Frame</code> in which the dialog is displayed
     * @param operationKey a <code>String</code> containing the key under which the last used directory is stored
     * @param data a <code>byte[]</code> containing the data to write
     * @param description a <code>String</code> describing the file type
     * @param extension a <code>String</code> containing the file extension (including the "dot")
     * @return the <code>File</code> which was written to, or <code>null</code> if the user cancelled
     * @throws IOException if an error occurs while writing
     */
    public static File promptLocationAndWriteFile(Component parent, String operationKey, byte[] data, String description, String extension) throws IOException
    {
        String lastPath = Tool.preferences.get(operationKey, null);

        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(lastPath);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Export file");

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new ExtensionFilter(description, extension));
        int returnVal = fc.showSaveDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            if (!selected.getAbsolutePath().toLowerCase(Locale.ROOT).endsWith(extension.toLowerCase(Locale.ROOT)))
                selected = new File(selected.getAbsolutePath() + extension);

            if (!confirmOverwrite(parent, selected))
                return null;

            File parentDir = selected.getParentFile();
            if (parentDir != null)
                Tool.preferences.put(operationKey, parentDir.getAbsolutePath());

            atomicWrite(selected.toPath(), data);
            return selected;
        }
        return null;
    }

    /**
     * If the provided file already exists, asks the user whether they want to overwrite it
     * @param parent determines the <code>Frame</code> in which the dialog is displayed
     * @param target a <code>File</code> which is about to be written to
     * @return a <code>boolean</code> representing whether the write is allowed to proceed
     */
    protected static boolean confirmOverwrite(Component parent, File target)
    {
        if (target == null || !target.exists())
            return true;

        return JOptionPane.showConfirmDialog(parent,
                "\"" + target.getName() + "\" already exists.\nDo you want to replace it?",
                "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static byte[] promptLocationAndReadFile(Component parent, String operationKey, String description, String extension) throws IOException
    {
        String lastPath = Tool.preferences.get(operationKey, null);

        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(lastPath);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Import file");

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new ExtensionFilter(description, extension));
        int returnVal = fc.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            File parentDir = selected.getParentFile();
            if (parentDir != null)
                Tool.preferences.put(operationKey, parentDir.getAbsolutePath());
            return Buffer.readFile(selected.getAbsolutePath());
        }
        return null;
    }


    /**
     * An implementation of <code>FileView</code> which gives special icons to Nintendo DS ROMs and projects of this framework
     */
    static class ToolFileView extends FileView
    {
        public String getTypeDescription(File f) {
            String type = null;

            if (f.isFile()) {
                String extension = getExtension(f);
                if (extension == null) {
                    return null;
                }

                for (String ndsExtension : ndsExtensions) {
                    if (ndsExtension.equals(extension)) {
                        type = "Nintendo DS ROM";
                        break;
                    }
                }
                if (f.getName().equals(projectFileName)) {
                    type = "Nintendo DS Project";
                }
            }
            else if (f.isDirectory()) {
                File[] contents = f.listFiles();
                if (contents != null) {
                    List<String> fileNames = Arrays.stream(contents).map(File::getName).toList();
                    if (fileNames.contains(projectFileName)) {
                        type = "Nintendo DS Project";
                    }
                }
            }

            return type;
        }

        public Icon getIcon(File f) {
            ImageIcon icon = null;

            if (f.isFile()) {
                if (f.getName().equals(projectFileName)) {
                    return ThemeUtils.gamepadIcon;
                }

                for (String ndsExtension : ndsExtensions) {
                    if (f.getName().endsWith(ndsExtension)) {
                        icon = ThemeUtils.gamepadIcon;
                        break;
                    }
                }
            }
            else if (f.isDirectory()) {
                File[] contents = f.listFiles();
                if (contents != null) {
                    List<String> fileNames = Arrays.stream(contents).map(File::getName).toList();
                    if (fileNames.contains(projectFileName)) {
                        icon = ThemeUtils.gamepadIcon;
                    }
                }
            }

            return icon;
        }
    }

    /**
     * An implementation of <code>FileFilter</code> which filters out files which do not have the specified file extensions
     */
    static class ExtensionFilter extends FileFilter
    {
        private final String[] extensions;
        private final String description;

        /**
         * Creates a new <code>ExtensionFilter</code> which only shows files with the specified extensions
         * @param description a <code>String</code> containing the description to show for the allowed file types
         * @param extensions a <code>String[]</code> containing the allowed file types (including the "dot")
         */
        public ExtensionFilter(String description, String... extensions)
        {
            this.extensions = extensions;
            this.description = description;
        }

        @Override
        public boolean accept(File f)
        {
            if (f.isDirectory())
                return true;

            String name = f.getName().toLowerCase(Locale.ROOT);
            for (String str : extensions)
            {
                if (name.endsWith(str.toLowerCase(Locale.ROOT)))
                    return true;
            }
            return false;
        }

        @Override
        public String getDescription()
        {
            StringBuilder extensions = new StringBuilder(" (");
            String extension;

            for (int i = 0; i < this.extensions.length; i++)
            {
                extension = this.extensions[i];
                extensions.append("*").append(extension);
                if (i != this.extensions.length-1)
                    extensions.append(", ");
            }
            extensions.append(")");

            return description + extensions.toString();
        }
    }

}
