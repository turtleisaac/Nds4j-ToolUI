package io.github.turtleisaac.nds4j.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import io.github.turtleisaac.nds4j.ui.exceptions.ToolAttributeModificationException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Provides functionality relating to theme switching for a <code>Tool</code>
 */
public class ThemeUtils
{
    private static final ArrayList<LookAndFeel> themes = new ArrayList<>();
    private static final ThemeIterator iterator = new ThemeIterator();
    private static LookAndFeel currentTheme;

    /**
     * A color to be used for miscellaneous purposes in dark themes
     */
    public static final Color darkIconForegroundColor = new Color(250, 225, 0);

    /**
     * A color to be used for miscellaneous purposes in light themes
     */
    public static final Color lightIconForegroundColor = new Color(117, 0, 250);

    /**
     * A filter to be used for adjusting the color of a SVG file displayed as an icon.
     */
    public static final FlatSVGIcon.ColorFilter iconColorFilter = FlatSVGIcon.ColorFilter.getInstance();

    /**
     * A symbol representing a right arrow
     */
    public static final FlatSVGIcon rightIcon;
    /**
     * A symbol representing a left arrow
     */
    public static final FlatSVGIcon leftIcon;
    /**
     * A symbol representing a folder selection operation
     */
    public static final FlatSVGIcon folderSearchIcon;
    /**
     * A symbol representing a file selection operation
     */
    public static final FlatSVGIcon fileSearchIcon;
    /**
     * A symbol representing a project selection and opening operation
     */
    public static final FlatSVGIcon folderOpenIcon;
    /**
     * A symbol representing a ROM or project save operation
     */
    public static final FlatSVGIcon saveIcon;
    /**
     * A symbol representing an info display operation
     */
    public static final FlatSVGIcon infoIcon;
    /**
     * A symbol representing a tool selection operation
     */
    public static final FlatSVGIcon appWindowIcon;
    /**
     * A symbol representing valid data
     */
    public static final FlatSVGIcon validIcon;
    /**
     * A symbol representing invalid data
     */
    public static final FlatSVGIcon invalidIcon;
    /**
     * A symbol representing a game file
     */
    public static final FlatSVGIcon gamepadIcon;
    /**
     * A symbol representing a zoom-in operation
     */
    public static final FlatSVGIcon zoomInIcon;
    /**
     * A symbol representing a zoom-out operation
     */
    public static final FlatSVGIcon zoomOutIcon;
    /**
     * A symbol representing a refresh/reload operation
     */
    public static final FlatSVGIcon reloadIcon;
    /**
     * A symbol representing a file import operation
     */
    public static final FlatSVGIcon fileImportIcon;
    /**
     * A symbol representing a file export operation
     */
    public static final FlatSVGIcon fileExportIcon;

    static {
        rightIcon = loadIcon("/icons/svg/chevron-right.svg");
        leftIcon = loadIcon("/icons/svg/chevron-left.svg");
        folderSearchIcon = loadIcon("/icons/svg/folder-search.svg");
        fileSearchIcon = loadIcon("/icons/svg/file-search.svg");
        folderOpenIcon = loadIcon("/icons/svg/folder-open.svg");
        saveIcon = loadIcon("/icons/svg/device-floppy.svg");
        infoIcon = loadIcon("/icons/svg/info-square-rounded.svg");
        appWindowIcon = loadIcon("/icons/svg/app-window.svg");
        validIcon = loadIcon("/icons/svg/checks.svg");
        invalidIcon = loadIcon("/icons/svg/alert-circle.svg");
        gamepadIcon = loadIcon("/icons/svg/device-gamepad.svg");
        zoomInIcon = loadIcon("/icons/svg/zoom-in.svg");
        zoomOutIcon = loadIcon("/icons/svg/zoom-out.svg");
        reloadIcon = loadIcon("/icons/svg/refresh.svg");
        fileImportIcon = loadIcon("/icons/svg/file-import.svg");
        fileExportIcon = loadIcon("/icons/svg/file-export.svg");

        rightIcon.setColorFilter(iconColorFilter);
        leftIcon.setColorFilter(iconColorFilter);
        folderSearchIcon.setColorFilter(iconColorFilter);
        fileSearchIcon.setColorFilter(iconColorFilter);
        folderOpenIcon.setColorFilter(iconColorFilter);
        saveIcon.setColorFilter(iconColorFilter);
        infoIcon.setColorFilter(iconColorFilter);
        appWindowIcon.setColorFilter(iconColorFilter);
        validIcon.setColorFilter(iconColorFilter);
        invalidIcon.setColorFilter(iconColorFilter);
        gamepadIcon.setColorFilter(iconColorFilter);
        zoomInIcon.setColorFilter(iconColorFilter);
        zoomOutIcon.setColorFilter(iconColorFilter);
        reloadIcon.setColorFilter(iconColorFilter);
        fileImportIcon.setColorFilter(iconColorFilter);
        fileExportIcon.setColorFilter(iconColorFilter);
    }

    /**
     * Loads a SVG icon from the provided resource path
     * @param resourcePath a <code>String</code> containing the path of the icon resource
     * @return a <code>FlatSVGIcon</code>
     * @throws ToolAttributeModificationException if the resource is missing or could not be read
     */
    private static FlatSVGIcon loadIcon(String resourcePath)
    {
        try (InputStream stream = ToolFrame.class.getResourceAsStream(resourcePath)) {
            if (stream == null)
                throw new ToolAttributeModificationException("Missing icon resource: " + resourcePath);
            return new FlatSVGIcon(stream);
        }
        catch (IOException e) {
            throw new ToolAttributeModificationException("An error occurred while reading the icon resource: " + resourcePath, e);
        }
    }

    /**
     * Changes the selected theme to the next one in the theme list
     */
    public static void changeTheme()
    {
        try {
            currentTheme = iterator.next();
            System.out.println(currentTheme.getClass().getName());
//            System.out.println("\t" + currentTheme.getDescription());
//            UIManager.setLookAndFeel(currentTheme);



            FlatAnimatedLafChange.showSnapshot();
            UIManager.setLookAndFeel(currentTheme);
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();

            iconColorFilter.removeAll();
            iconColorFilter.add(Color.black, UIManager.getColor("Button.focusedBorderColor"));

//            if (Tool.instance.projectStartFrame != null) {
//                SwingUtilities.updateComponentTreeUI(Tool.instance.projectStartFrame);
//                Tool.instance.projectStartFrame.setPreferredSize(Tool.instance.projectStartFrame.getPreferredSize());
//                Tool.instance.projectStartFrame.setMinimumSize(Tool.instance.projectStartFrame.getPreferredSize());
//                Tool.instance.projectStartFrame.validate();
//            }
            Tool.preferences.put("laf", currentTheme.getClass().getName());
        }
        catch(UnsupportedLookAndFeelException | NullPointerException e) {
            throw new ToolAttributeModificationException("An error occurred while setting the look and feel of the tool", e);
        }
    }

    /**
     * Changes the current theme to the saved user preference, if it is available.
     * <p>Otherwise, uses the first theme in the theme list</p>
     */
    protected static void changeToPreferredTheme()
    {
        String className = Tool.preferences.get("laf", null);

        ArrayList<String> names = new ArrayList<>();
        themes.forEach(laf -> names.add(laf.getClass().getName()));

        int idx = names.indexOf(className);
        if (idx != -1)
        {
            iterator.idx = idx;
            changeTheme();
        }
        else if (!themes.isEmpty())
        {
            changeTheme();
        }
        else
        {
            Tool.preferences.remove("laf");
        }
    }

    /**
     * Adds a new theme to a <code>Tool</code>'s theme options
     * @param lookAndFeel a <code>LookAndFeel</code>
     */
    protected static void addLookAndFeel(LookAndFeel lookAndFeel)
    {
        themes.add(lookAndFeel);
    }

    /**
     * Gets the number of loaded themes
     * @return the number of loaded themes
     */
    protected static int themeCount()
    {
        return themes.size();
    }

    private static class ThemeIterator implements Iterator<LookAndFeel>
    {
        private int idx = 0;

        @Override
        public boolean hasNext()
        {
            return idx < themes.size();
        }

        @Override
        public LookAndFeel next()
        {
            if (!hasNext())
                idx = 0;
            return themes.get(idx++);
        }
    }
}
