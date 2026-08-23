package io.github.turtleisaac.nds4j.ui;

import io.github.turtleisaac.nds4j.framework.Buffer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
     * Writes the provided data to the given file without ever truncating the existing one - the data is written
     * to a sibling temporary file which is then moved into place, so a failure part-way through leaves the
     * original file intact.
     * @param target a <code>Path</code> to the file to write
     * @param data a <code>byte[]</code> containing the data to write
     * @throws IOException if an error occurs while writing
     */
    protected static void atomicWrite(Path target, byte[] data) throws IOException
    {
        if (data == null)
            throw new IOException("No data was provided to write to " + target);

        Path parent = target.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        Path temp = Files.createTempFile(parent != null ? parent : Path.of("."), target.getFileName().toString(), ".tmp");
        try {
            Files.write(temp, data);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
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
