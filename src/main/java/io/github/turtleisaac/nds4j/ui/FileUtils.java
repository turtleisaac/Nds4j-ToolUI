package io.github.turtleisaac.nds4j.ui;

import io.github.turtleisaac.nds4j.framework.BinaryWriter;
import io.github.turtleisaac.nds4j.framework.Buffer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        if (f.getName().length() > 1)
            return f.getName().substring(f.getName().lastIndexOf("."));
        return null;
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
        if (directory.isDirectory())
        {
            for (File subfile : Objects.requireNonNull(directory.listFiles()))
            {
                if (subfile.isDirectory())
                {
                    clearDirectory(subfile);
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

    public static void promptLocationAndWriteFile(Component parent, String operationKey, byte[] data, String description, String extension) throws IOException
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
            if (!selected.getAbsolutePath().contains(extension))
                selected = new File(selected.getAbsolutePath() + extension);

            Tool.preferences.put(operationKey, selected.getAbsolutePath());
            BinaryWriter writer = new BinaryWriter(selected);
            writer.write(data);
            writer.close();
        }
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
            Tool.preferences.put(operationKey, selected.getAbsolutePath());
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
                List<String> fileNames = Arrays.stream(f.listFiles()).map(File::getName).toList();
                if (fileNames.contains(projectFileName)) {
                    type = "Nintendo DS Project";
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
                List<String> fileNames = Arrays.stream(f.listFiles()).map(File::getName).toList();
                if (fileNames.contains(projectFileName)) {
                    icon = ThemeUtils.gamepadIcon;
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
            for (String str : extensions)
            {
                if (f.getName().endsWith(str))
                    return true;
                else if (f.getName().endsWith(str))
                    return true;
                else if (f.isDirectory())
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
