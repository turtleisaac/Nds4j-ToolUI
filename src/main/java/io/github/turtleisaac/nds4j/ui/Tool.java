package io.github.turtleisaac.nds4j.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import io.github.turtleisaac.nds4j.ui.exceptions.ToolAttributeModificationException;
import io.github.turtleisaac.nds4j.ui.exceptions.ToolCreationException;
import io.github.turtleisaac.nds4j.NintendoDsRom;

public class Tool {
    public static Preferences preferences = Preferences.userNodeForPackage(Tool.class);

    private boolean started;

    // usable by developers
    private ProgramType type;
    private String name;
    private String version;
    private String flavorText;
    private String author;
    private JPanel startPanel;
    private ArrayList<JPanel> alternateStartPanels;
    private Locale locale;
    private boolean gitEnabled;

    private final List<String> gameCodes;
    private final List<String> gameTitles;
    private final Map<Predicate<NintendoDsRom>, String> validationChecks;

    private Image icon;

    private List<Supplier<ToolPanel>> toolPanelSuppliers;

    // internal usage only
    public JFrame projectStartFrame;
    private ToolFrame toolFrame;

    private NintendoDsRom rom;
    private JsonNode info;

    private Tool() {
        alternateStartPanels = new ArrayList<>();
        gameCodes = new ArrayList<>();
        gameTitles = new ArrayList<>();
        validationChecks = new HashMap<>();
        toolPanelSuppliers = new ArrayList<>();
    }

    public static Tool create()
    {
//        instance = new Tool();
//        return instance;
        return new Tool();
    }

    private void testStarted()
    {
        if (started)
            throw new ToolAttributeModificationException("Tool has already been started, this attribute can no longer be modified");
    }

    public Tool setType(ProgramType type)
    {
        testStarted();
        this.type = type;
        return this;
    }

    public Tool setName(String name)
    {
        testStarted();
        this.name = name;
        return this;
    }

    public Tool setVersion(String version)
    {
        testStarted();
        this.version = version;
        return this;
    }

    public Tool setFlavorText(String text)
    {
        testStarted();
        this.flavorText = text;
        return this;
    }

    public Tool setAuthor(String author)
    {
        testStarted();
        this.author = author;
        return this;
    }

    public Tool setStartPanel(JPanel startPanel)
    {
        testStarted();
        this.startPanel = startPanel;
        return this;
    }

    public Tool addAlternateStartPanel(JPanel panel)
    {
        testStarted();
        alternateStartPanels.add(panel);
        return this;
    }

    public Tool setLocale(Locale locale)
    {
        this.locale = locale;
        return this;
    }

    public Tool setGitEnabled(boolean enabled)
    {
        testStarted();
        this.gitEnabled = enabled;
        return this;
    }

    public Tool setIcon(ImageIcon icon)
    {
        testStarted();
        this.icon = icon.getImage();
        return this;
    }

//    /**
//     * Adds a theme to the tool's options.
//     * @param lookAndFeel a LookAndFeel object
//     * @return a reference to this object
//     */
//    public Tool addLookAndFeel(Class<? extends LookAndFeel> lookAndFeel)
//    {
//        testStarted();
//        try {
//            ThemeUtils.addLookAndFeel(lookAndFeel.getDeclaredConstructor().newInstance());
//        }
//        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//            throw new ToolAttributeModificationException("An error occurred while creating the look and feel of the tool", e);
//        }
//        catch(NullPointerException ignored) {
//            System.err.println("fuck");
//        }
//
//        return this;
//    }

    /**
     * Adds a theme to the tool's options.
     * @param lookAndFeel a LookAndFeel object
     * @return a reference to this object
     */
    public Tool addLookAndFeel(LookAndFeel lookAndFeel)
    {
        testStarted();
        ThemeUtils.addLookAndFeel(lookAndFeel);

        return this;
    }

    public Tool addGame(String title, String gameCode)
    {
        if (gameCode.length() != 3 && gameCode.length() != 4) {
            throw new ToolAttributeModificationException("Invalid game code provided, must be of length 3 or 4: " + gameCode);
        }
        gameTitles.add(title);
        gameCodes.add(gameCode);
        return this;
    }

    public Tool addValidationCheck(Predicate<NintendoDsRom> predicate, String errorMessage)
    {
        if (errorMessage == null || errorMessage.isEmpty())
            throw new ToolAttributeModificationException("You need to provide an actual error message to be displayed to the user if their provided ROM fails the check");
        validationChecks.put(predicate, errorMessage);
        return this;
    }

    public Tool addToolPanel(Supplier<ToolPanel> panelSupplier)
    {
        toolPanelSuppliers.add(panelSupplier);
        return this;
    }

    /**
     * Begins running the tool
     * @throws ToolCreationException if there is invalid data preventing the tool from starting
     */
    public void init() throws ToolCreationException
    {
        if (SystemInfo.isMacOS)
        {
            System.setProperty("apple.awt.application.name", name);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        started = true;
        if (type == null)
            throw new ToolCreationException("Program type not specified");
        if (name == null || name.isEmpty())
            throw new ToolCreationException("Program name not specified");
        if (version == null || version.isEmpty())
            throw new ToolCreationException("Program version not specified");
        if (author == null || author.isEmpty())
            throw new ToolCreationException("Program author not specified");
        if (toolPanelSuppliers.isEmpty())
            throw new ToolCreationException("No tool panels were provided");
        if (icon == null) {
            icon = new ImageIcon(Tool.class.getResource("/icons/cartridge.png")).getImage();
        }

        startGit();

        setLookAndFeel();
        switch (type) {
            case PROJECT -> startProjectBasedTool();
            case ROM -> startRomBasedTool();
        }

        handleToolbarIfSupported();
    }

    private void handleMacOS(JFrame frame)
    {
        if (SystemInfo.isMacFullWindowContentSupported) {
            frame.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            frame.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            frame.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }
    }

    private void startGit()
    {
        if (gitEnabled)
        {
            if (type != ProgramType.PROJECT)
            {
                System.err.println("[WARNING]: This tool is configured to support git, but git is only available for project-based tools.");
                gitEnabled = false;
                return;
            }

            try
            {
                Process pb = new ProcessBuilder("git", "--version").start();
                int exitCode = pb.waitFor();
                if (exitCode != 0)
                    gitEnabled = false;
            }
            catch(IOException | InterruptedException e)
            {
                System.err.println("[WARNING]: This tool is configured to support git, but git is not installed on this system.");
                gitEnabled = false;
            }
        }
    }

    private void startRomBasedTool()
    {
        String path = selectRom();

        if (path == null)
            return;

        rom = NintendoDsRom.fromFile(path);
        startToolWindow(path);
    }

    private void startProjectBasedTool()
    {
        projectStartFrame = new JFrame();
        projectStartFrame.setTitle(name);
        ProjectStartPanel panel = new ProjectStartPanel(this);
        panel.setTitle(name);
        panel.setVersion(version);
        panel.setAuthor(author);
        if (flavorText != null)
            panel.setFlavorText(flavorText);
        panel.setPreferredSize(panel.getPreferredSize());
        panel.validate();

        projectStartFrame.setMinimumSize(panel.getPreferredSize());
        projectStartFrame.setPreferredSize(panel.getPreferredSize());
        startPanel = panel;

        if (ThemeUtils.themeCount() <= 1) {
            panel.removeThemeButton();
        }

        projectStartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        projectStartFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                super.windowClosed(e);
                if (panel.wasProjectOpened())
                {
                    rom = panel.getRom();
                    info = panel.getProjectInfo();
                    startToolWindow(panel.getProjectPath());
                }
                else {
                    System.exit(0);
                }
            }
        });

        projectStartFrame.setLocationRelativeTo(null);
        projectStartFrame.setContentPane(startPanel);
        handleMacOS(projectStartFrame);
        projectStartFrame.setVisible(true);
    }

    protected void startToolWindow(String path)
    {
        toolFrame = new ToolFrame(this);
        //todo uncomment
        toolFrame.setTitle(name + " " + version + " " + new File(path).getName());
//        toolFrame.setTitle(name + " " + version + " ~/Documents/Projects/HeartGold.nds");

        toolFrame.addToolPanel(toolPanelSuppliers.get(0).get());
        toolFrame.setPreferredSize(toolFrame.getPreferredSize());
        toolFrame.setMinimumSize(toolFrame.getPreferredSize());

        toolFrame.validate();

        toolFrame.setLocationRelativeTo(null);
        handleMacOS(toolFrame);
        toolFrame.setVisible(true);
    }

    private void handleToolbarIfSupported()
    {
        final Taskbar taskbar = Taskbar.getTaskbar();
        if (icon != null) {
            try {
                //set icon for macOS (and other systems which do support this method)
                taskbar.setIconImage(icon);
            } catch (final UnsupportedOperationException e) {
                System.out.println("[WARNING]: The OS does not support: 'taskbar.setIconImage'");
            } catch (final SecurityException e) {
                System.out.println("[WARNING]: There was a security exception for: 'taskbar.setIconImage'");
            }
        }
    }

    private void setLookAndFeel()
    {
        if (ThemeUtils.themeCount() == 0) {
            try {
//                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch(UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new ToolAttributeModificationException("An error occurred while creating the look and feel of the tool", e);
            }
        }
        else {
            if (preferences.get("laf", null) != null) {
                ThemeUtils.changeToPreferredTheme();
            } else {
                ThemeUtils.changeTheme();
            }

        }

        if (projectStartFrame != null)
        {
            SwingUtilities.updateComponentTreeUI(projectStartFrame);
            projectStartFrame.pack();
        }

        if (toolFrame != null)
        {
            SwingUtilities.updateComponentTreeUI(toolFrame);
            toolFrame.pack();
        }
    }

    public String getName()
    {
        return name;
    }

    public NintendoDsRom getRom()
    {
        return rom;
    }

    public JsonNode getInfo()
    {
        return info;
    }

    /**
     * Gets a list of the compatible game codes for this <code>Tool</code>
     * @return a <code>List</code><<code>String</code>> containing the compatible game codes.
     * <p>An empty list means that all ROMs are supported</p>
     */
    public List<String> getGameCodes()
    {
        return gameCodes;
    }

    /**
     * Performs tests to ensure the user-provided ROM is supported by this <code>Tool</code>
     * @param rom a <code>NintendoDsRom</code> representation of the user-provided ROM
     * @return a <code>RomSupportContext</code> object containing whether the ROM is supported and an error message if that is not the case
     */
    public RomSupportContext isRomSupported(NintendoDsRom rom)
    {
        boolean supported = false;

        for (String gameCode : gameCodes) {
            if (rom.getGameCode().startsWith(gameCode)) {
                supported = true;
                break;
            }
        }

        if (gameCodes.isEmpty())
            supported = true;

        if (!supported)
            return new RomSupportContext(false, createGameCodeNotSupportedMessage(rom));

        for (Map.Entry<Predicate<NintendoDsRom>, String> check : validationChecks.entrySet()) {
            if (!check.getKey().test(rom))
            {
                return new RomSupportContext(false, check.getValue());
            }
        }

        return new RomSupportContext(true, null);
    }

    /**
     * Creates the message to be displayed to the user if their provided ROM is not supported by this <code>Tool</code>
     * @param rom a <code>NintendoDsRom</code>
     * @return a <code>String</code> containing the error message
     */
    public String createGameCodeNotSupportedMessage(NintendoDsRom rom)
    {
        StringBuilder sb = new StringBuilder("The provided ROM (").append(rom.getGameCode()).append(") is not supported by this tool.\nThe following ROM(s) are supported:");
        for (int i = 0; i < gameTitles.size(); i++)
        {
            String gameCode = gameCodes.get(i);
            sb.append("\n").append(gameTitles.get(i)).append(" (").append(gameCode);
            if (gameCode.length() == 3) {
                sb.append("_");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * Opens a JFileChooser configured for the user to select a Nintendo DS ROM
     * @return a <code>String</code> containing the absolute path of the selected file, or null if cancelled
     */
    public static String selectRom()
    {
        String lastPath = preferences.get("openRomPath", null);

        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(lastPath);
        fc.setDialogTitle("Select ROM");
        fc.setAcceptAllFileFilterUsed(true);

        fc.setFileFilter(FileUtils.romFilter);
        fc.setFileView(new FileUtils.ToolFileView());
        int returnVal = fc.showOpenDialog(null);


        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            preferences.put("openRomPath", selected.getParentFile().getAbsolutePath());
            System.out.println(preferences.get("openPath", null));
            return selected.getAbsolutePath();
        }

//        FileDialog fileDialog = new FileDialog(projectStartFrame, "Select ROM", FileDialog.LOAD);
//        fileDialog.setVisible(true);

        return null;
    }

    /**
     * Opens a JFileChooser configured for the user to select either a Projectfile or a folder containing one
     * @return a <code>String</code> containing the absolute path of the selected project directory, or null if cancelled
     */
    public static String selectProject()
    {
        String lastPath = preferences.get("openProjectPath", null);

        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(lastPath);
        fc.setDialogTitle("Open Project");
        fc.setAcceptAllFileFilterUsed(false);

        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fc.addChoosableFileFilter(FileUtils.projectFilter);
        fc.setFileView(new FileUtils.ToolFileView());

        int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            if (selected.isFile()) {
                selected = selected.getParentFile();
            }
            preferences.put("openProjectPath", selected.getParentFile().getAbsolutePath());
            return selected.getAbsolutePath();
        }

        return null;
    }

    public static class RomSupportContext
    {
        private final boolean supported;
        private final String errorMessage;

        public RomSupportContext(boolean supported, String errorMessage)
        {
            this.supported = supported;
            this.errorMessage = errorMessage;
        }

        /**
         * Gets whether the user-provided rom is supported by the tool
         * @return a <code>boolean</code>
         */
        public boolean isSupported()
        {
            return supported;
        }

        /**
         * Gets this <code>RomSupportContext</code>'s error message if not supported
         * @return an <code>Optional<String></String>></code> possibly containing an error message
         */
        public Optional<String> getErrorMessage()
        {
            if (errorMessage == null)
                return Optional.empty();
            return Optional.of(errorMessage);
        }
    }
}
