package io.github.turtleisaac.nds4j.ui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class FileUtils
{
    public static final String projectFileName = "Projectfile";
    public static final String[] ndsExtensions = {".nds", ".srl"};
    protected static final RomFilter romFilter = new RomFilter("Nintendo DS ROM", ndsExtensions);
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

    public static String getProjectfilePath(String projectPath)
    {
        return Path.of(projectPath, projectFileName).toString();
    }

    public static String getProjectUnpackedRomPath(String projectPath)
    {
        return Path.of(projectPath, "rom").toString();
    }

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

    static class RomFilter extends FileFilter
    {
        private final String[] extensions;
        private final String description;

        public RomFilter(String description, String... extensions)
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
