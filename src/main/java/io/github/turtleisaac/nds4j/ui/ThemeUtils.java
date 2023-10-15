package io.github.turtleisaac.nds4j.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import io.github.turtleisaac.nds4j.ui.exceptions.ToolAttributeModificationException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class ThemeUtils
{
    private static final ArrayList<LookAndFeel> themes = new ArrayList<>();
    private static final ThemeIterator iterator = new ThemeIterator();
    private static LookAndFeel currentTheme;

    public static final Color darkIconForegroundColor = new Color(250, 225, 0);
    public static final Color lightIconForegroundColor = new Color(117, 0, 250);
    public static final FlatSVGIcon.ColorFilter iconColorFilter = FlatSVGIcon.ColorFilter.getInstance();

    public static final FlatSVGIcon rightIcon;
    public static final FlatSVGIcon leftIcon;
    public static final FlatSVGIcon folderSearchIcon;
    public static final FlatSVGIcon fileSearchIcon;
    public static final FlatSVGIcon folderOpenIcon;
    public static final FlatSVGIcon saveIcon;
    public static final FlatSVGIcon infoIcon;
    public static final FlatSVGIcon appWindowIcon;
    public static final FlatSVGIcon validIcon;
    public static final FlatSVGIcon invalidIcon;
    public static final FlatSVGIcon gamepadIcon;

    static {
        try {
            rightIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/chevron-right.svg"));
            leftIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/chevron-left.svg"));
            folderSearchIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/folder-search.svg"));
            fileSearchIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/file-search.svg"));
            folderOpenIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/folder-open.svg"));
            saveIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/device-floppy.svg"));
            infoIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/info-square-rounded.svg"));
            appWindowIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/app-window.svg"));
            validIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/checks.svg"));
            invalidIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/alert-circle.svg"));
            gamepadIcon = new FlatSVGIcon(ToolFrame.class.getResourceAsStream("/icons/svg/device-gamepad.svg"));

            leftIcon.setColorFilter(iconColorFilter);
            rightIcon.setColorFilter(iconColorFilter);
            folderSearchIcon.setColorFilter(iconColorFilter);
            fileSearchIcon.setColorFilter(iconColorFilter);
            folderOpenIcon.setColorFilter(iconColorFilter);
            saveIcon.setColorFilter(iconColorFilter);
            infoIcon.setColorFilter(iconColorFilter);
            appWindowIcon.setColorFilter(iconColorFilter);
            validIcon.setColorFilter(iconColorFilter);
            invalidIcon.setColorFilter(iconColorFilter);
            gamepadIcon.setColorFilter(iconColorFilter);
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }


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

    public static boolean isLight()
    {
        return !(currentTheme.getDescription().toLowerCase().contains("dark") || currentTheme.getName().toLowerCase().contains("dark"));
    }

    public static Color getLabelForegroundColor()
    {
        return isLight() ? lightIconForegroundColor : darkIconForegroundColor;
    }

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
            Tool.preferences.put("laf", null);
        }
    }

    protected static void addLookAndFeel(LookAndFeel lookAndFeel)
    {
        themes.add(lookAndFeel);
    }

    protected static int themeCount()
    {
        return themes.size();
    }

    private static class ThemeIterator implements Iterator<LookAndFeel>
    {
        int idx = 0;

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
