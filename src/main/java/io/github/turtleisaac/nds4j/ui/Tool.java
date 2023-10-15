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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import io.github.turtleisaac.nds4j.ui.exceptions.ToolAttributeModificationException;
import io.github.turtleisaac.nds4j.ui.exceptions.ToolCreationException;
import io.github.turtleisaac.nds4j.NintendoDsRom;

/**
 * Provides a generalized framework for development of Nintendo DS hacking tools which operate on Nintendo DS ROM files.
 * This currently does not provide any form of support for tools which operate solely on isolated extracted files from ROMs.
 */
public class Tool {
    /**
     * A <code>Preferences</code> node containing stored properties of user preferences
     */
    public static Preferences preferences = Preferences.userNodeForPackage(Tool.class);

    private boolean started;
    private boolean windowMode;

    // usable by developers
    private ProgramType type;
    private String name;
    private String version;
    private String flavorText;
    private String author;
    private JPanel startPanel;
    private ArrayList<JPanel> alternateStartPanels;
    private List<Locale> locales;
    private boolean gitEnabled;

    private final List<String> gameCodes;
    private final List<String> gameTitles;
    private final Map<Predicate<NintendoDsRom>, String> validationChecks;

    private Image icon;

    private List<Supplier<PanelManager>> panelManagerSuppliers;
    private List<Function<NintendoDsRom, Void>> functions;

    // internal usage only
    protected JFrame projectStartFrame;
    private ToolFrame toolFrame;

    private NintendoDsRom rom;
    private JsonNode info;

    private Tool() {
        alternateStartPanels = new ArrayList<>();
        gameCodes = new ArrayList<>();
        gameTitles = new ArrayList<>();
        validationChecks = new HashMap<>();
        panelManagerSuppliers = new ArrayList<>();
        functions = new ArrayList<>();
        locales = new ArrayList<>();
    }

    /**
     * Creates a new <code>Tool</code> object which can then be modified before running <code>init()</code>
     * @return a new <code>Tool</code> object
     */
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

    /**
     * Sets the type of this program.
     * <p>ProgramType.ROM: Makes this <code>Tool</code> open directly to a ROM selection window and save a ROM file when the user dictates it</p>
     * <p>ProgramType.PROJECT: Makes this <code>Tool</code> operate on a project basis, where a user creates and edits projects instead of a
     *      packed ROM file</p>
     * @param type a <code>ProgramType</code>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setType(ProgramType type)
    {
        testStarted();
        this.type = type;
        return this;
    }

    /**
     * Sets the name of this <code>Tool</code> to be displayed to the user
     * @param name a <code>String</code> containing the name of this <code>Tool</code>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setName(String name)
    {
        testStarted();
        this.name = name;
        return this;
    }

    /**
     * Sets the version text to be displayed in this <code>Tool</code>
     * @param version a <code>String</code> containing the version text
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setVersion(String version)
    {
        testStarted();
        this.version = version;
        return this;
    }

    /**
     * Sets the flavor text to be displayed in this <code>Tool</code>'s start window
     * @param text a <code>String</code> containing the flavor text
     *             <p>This will do nothing if <code>ProgramType.ROM</code> has been set</p>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setFlavorText(String text)
    {
        testStarted();
        this.flavorText = text;
        return this;
    }

    /**
     * Sets the author text to be displayed in this <code>Tool</code>
     * @param author a <code>String</code> containing the author text
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setAuthor(String author)
    {
        testStarted();
        this.author = author;
        return this;
    }

//    /**
//     *
//     * @param startPanel
//     * @return a reference to this object
//     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
//     */
//    public Tool setStartPanel(JPanel startPanel)
//    {
//        testStarted();
//        this.startPanel = startPanel;
//        return this;
//    }

//    /**
//     * Adds additional panels to this <code>Tool</code>'s start window
//     * <p>This will do nothing if <code>ProgramType.ROM</code> has been set</p>
//     * @param panel a <code>JPanel</code>
//     * @return a reference to this object
//     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
//     */
//    public Tool addAlternateStartPanel(JPanel panel)
//    {
//        testStarted();
//        alternateStartPanels.add(panel);
//        return this;
//    }

    /**
     * Adds support for another language to this <code>Tool</code>
     * @param locale a <code>Locale</code>
     *               <p>It is entirely up to the developer to define multi-language support via Locale in their GUIs through
     *               resource bundles.</p>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool addLocale(Locale locale)
    {
        testStarted();
        locales.add(locale);
        return this;
    }

    /**
     * Enables Git support for project-based tools.
     * <p></p>
     * @param enabled a <code>boolean</code> representing whether Git mode should be enabled.
     *                <p>This will do nothing if <code>ProgramType.ROM</code> has been set</p>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setGitEnabled(boolean enabled)
    {
        testStarted();
        this.gitEnabled = enabled;
        return this;
    }

    /**
     * Sets the icon of this <code>Tool</code>
     * @param icon a <code>ImageIcon</code> to serve as the icon of this <code>Tool</code>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool setIcon(ImageIcon icon)
    {
        testStarted();
        this.icon = icon.getImage();
        return this;
    }

    /**
     * Adds a theme to this <code>Tool</code>'s options.
     * @param lookAndFeel a LookAndFeel object
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool addLookAndFeel(LookAndFeel lookAndFeel)
    {
        testStarted();
        ThemeUtils.addLookAndFeel(lookAndFeel);

        return this;
    }

    /**
     * Adds a supported game to this <code>Tool</code>
     * @param title a <code>String</code> containing the name of the game being added to the compatibility list.
     * @param gameCode a <code>String</code> containing the game code of the game being added to the compatibility list.
     *                 <p>The game code must either be three or four characters long.</p>
     *                 <p>If it is three characters long, then all game codes which match the first three letters will be compatible</p>
     *                 <p>If it is four characters long, then the provided ROMs must have game codes which match exactly.</p>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool addGame(String title, String gameCode)
    {
        testStarted();
        if (gameCode.length() != 3 && gameCode.length() != 4) {
            throw new ToolAttributeModificationException("Invalid game code provided, must be of length 3 or 4: " + gameCode);
        }
        gameTitles.add(title);
        gameCodes.add(gameCode);
        return this;
    }

    /**
     * Adds a check to be performed on the selected ROM to this <code>Tool</code>
     * @param predicate a <code>Predicate</code><<code>NintendoDsRom</code>> which performs a check on the selected ROM
     * @param errorMessage a <code>String</code> containing an error message to be displayed if the validation fails.
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool addValidationCheck(Predicate<NintendoDsRom> predicate, String errorMessage)
    {
        testStarted();
        if (errorMessage == null || errorMessage.isEmpty())
            throw new ToolAttributeModificationException("You need to provide an actual error message to be displayed to the user if their provided ROM fails the check");
        validationChecks.put(predicate, errorMessage);
        return this;
    }

    /**
     * Adds a function to be run on the selected ROM to this <code>Tool</code>.
     * <p>This cannot be used in conjunction with the <code>addFunction()</code> function</p>
     * @param panelManagerSupplier a <code>Supplier</code><<code>PanelManager</code>> which creates and returns a
     *                             <code>PanelManager</code>to be added to the tool window created
     *                             by <code>init()</code>
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool addPanelManager(Supplier<PanelManager> panelManagerSupplier)
    {
        testStarted();
        panelManagerSuppliers.add(panelManagerSupplier);
        return this;
    }

    /**
     * Adds a function to be run on the selected ROM to this <code>Tool</code>.
     * <p>This cannot be used in conjunction with the <code>addPanelManager()</code> function</p>
     * @param function a <code>Function</code> which takes a <code>NintendoDsRom</code> as input and performs
     *                 operations on it.
     * @return a reference to this object
     * @throws ToolAttributeModificationException if ran after calling <code>init()</code>
     */
    public Tool addFunction(Function<NintendoDsRom, Void> function)
    {
        testStarted();
        functions.add(function);
        return this;
    }

    /**
     * Begins running this <code>Tool</code> using the user-provided settings
     * @throws ToolCreationException if there is invalid data preventing the tool from starting
     */
    public void init() throws ToolCreationException
    {
        started = true;
        if (functions.isEmpty() && panelManagerSuppliers.isEmpty())
            throw new ToolCreationException("No panel managers or functions were provided");
        if (!functions.isEmpty() && ! panelManagerSuppliers.isEmpty())
            throw new ToolCreationException("Both panel managers and functions were provided - please only use one type of tool functionality");
        if (functions.isEmpty() && type == null)
            throw new ToolCreationException("Program type not specified");
        if (functions.isEmpty() && (name == null || name.isEmpty()))
            throw new ToolCreationException("Program name not specified");
        if (functions.isEmpty() && (version == null || version.isEmpty()))
            throw new ToolCreationException("Program version not specified");
        if (functions.isEmpty() && (author == null || author.isEmpty()))
            throw new ToolCreationException("Program author not specified");
        if (icon == null) {
            icon = new ImageIcon(Tool.class.getResource("/icons/cartridge.png")).getImage();
        }

        windowMode = functions.isEmpty(); // if functions is empty, then panelManagerSuppliers is not, and vice-versa

        if (windowMode && SystemInfo.isMacOS)
        {
            System.setProperty("apple.awt.application.name", name);
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", name);
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
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
        if (windowMode && SystemInfo.isMacFullWindowContentSupported) {
            frame.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            frame.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            frame.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }
    }

    private void startGit()
    {
        if (gitEnabled)
        {
            if (type == ProgramType.ROM)
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
        String romPath = selectAndValidateRom(toolFrame);

        if (romPath == null)
            return;

        if (windowMode)
            startToolWindow(romPath);
        else {
            runProvidedFunctions();
            String outputPath = selectRomToExport();
            try {
                rom.saveToFile(outputPath, true);
            }
            catch(IOException e) {
                throw new RuntimeException(e);
            }
        }
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

    /**
     * Starts the window of this <code>Tool</code>
     * @param path a <code>String</code> containing either the path to the selected ROM or selected project.
     *             This will be used to fill in the title bar of the tool window.
     */
    protected void startToolWindow(String path)
    {
        toolFrame = new ToolFrame(this);
        //todo uncomment
        toolFrame.setTitle(name + " " + version + " " + new File(path).getName());
//        toolFrame.setTitle(name + " " + version + " ~/Documents/Projects/HeartGold.nds");

        for (Supplier<PanelManager> panelManagerSupplier : panelManagerSuppliers) {
            toolFrame.addToolPanels(panelManagerSupplier.get());
        }

        toolFrame.setPreferredSize(toolFrame.getPreferredSize());
        toolFrame.setMinimumSize(toolFrame.getPreferredSize());

        toolFrame.validate();

        toolFrame.setLocationRelativeTo(null);
        handleMacOS(toolFrame);
        toolFrame.setVisible(true);
    }

    /**
     * Runs the provided functions on the loaded <code>NintendoDsRom</code>
     */
    protected void runProvidedFunctions()
    {
        for (Function<NintendoDsRom, Void> function : functions) {
            function.apply(rom);
        }
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
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
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

    /**
     * Gets the name of this <code>Tool</code>
     * @return a <code>String</code>
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the <code>NintendoDsRom</code> loaded by this project
     * @return a <code>NintendoDsRom</code>
     */
    public NintendoDsRom getRom()
    {
        return rom;
    }

    /**
     * Gets the contents of this <code>Tool</code>'s Projectfile, if one exists, in JSON format
     * @return a <code>JsonNode</code>
     */
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
     * Opens a JFileChooser configured for the user to select a Nintendo DS ROM to open, then
     *          performs validation checks on the ROM to ensure it is compatible with this <code>Tool</code>
     * @param parentComponent determines the <code>Frame</code>
     *          in which the dialog is displayed; if <code>null</code>,
     *          or if the <code>parentComponent</code> has no
     *          <code>Frame</code>, a default <code>Frame</code> is used
     * @return a <code>String</code> containing the absolute path of the selected file, or null if cancelled
     */
    protected String selectAndValidateRom(Component parentComponent)
    {
        String romPath = Tool.selectRomToOpen();
        if (romPath == null)
            return null;

        rom = NintendoDsRom.fromFile(romPath);
        return performValidation(parentComponent, romPath);
    }

    /**
     * Opens a JFileChooser configured for the user to select a Nintendo DS ROM to open
     * @return a <code>String</code> containing the absolute path of the selected file, or null if cancelled
     */
    public static String selectRomToOpen()
    {
        return romSelectionHelper(true,"Select ROM", "openRomPath");
    }

    /**
     * Opens a JFileChooser configured for the user to select a Nintendo DS ROM to save as
     * @return a <code>String</code> containing the absolute path of the selected file, or null if cancelled
     */
    public static String selectRomToExport()
    {
        String outputPath = romSelectionHelper(false,"Export ROM", "saveRomPath");
        if (outputPath == null)
            return null;

        boolean foundExtension = false;
        for (String ndsExtension : FileUtils.ndsExtensions) {
            if (outputPath.toLowerCase().endsWith(ndsExtension))
                foundExtension = true;
        }

        if (!foundExtension)
            outputPath += FileUtils.ndsExtensions[0];

        return outputPath;
    }

    private static String romSelectionHelper(boolean open, String title, String lastPathKey)
    {
        String lastPath = preferences.get(lastPathKey, null);

        if (lastPath == null) {
            lastPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(lastPath);
        fc.setDialogTitle(title);
        fc.setAcceptAllFileFilterUsed(true);

        fc.setFileFilter(FileUtils.romFilter);
        fc.setFileView(new FileUtils.ToolFileView());
        int returnVal;
        if (open)
            returnVal = fc.showOpenDialog(null);
        else
            returnVal = fc.showSaveDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            preferences.put(lastPathKey, selected.getParentFile().getAbsolutePath());
            return selected.getAbsolutePath();
        }

//        FileDialog fileDialog = new FileDialog(projectStartFrame, "Select ROM", FileDialog.LOAD);
//        fileDialog.setVisible(true);

        return null;
    }

    /**
     * Opens a JFileChooser configured for the user to select either a Projectfile or a folder containing one, then
     *          performs validation checks on the enclosed ROM to ensure it is compatible with this <code>Tool</code>
     * @param parentComponent determines the <code>Frame</code>
     *          in which the dialog is displayed; if <code>null</code>,
     *          or if the <code>parentComponent</code> has no
     *          <code>Frame</code>, a default <code>Frame</code> is used
     * @return a <code>String</code> containing the absolute path of the selected project directory, or null if cancelled
     */
    protected String selectAndValidateProject(Component parentComponent)
    {
        String projectPath = Tool.selectProject();

        if (projectPath == null)
            return null;

        rom = NintendoDsRom.fromUnpacked(FileUtils.getProjectUnpackedRomPath(projectPath));
        return performValidation(parentComponent, projectPath);
    }

    private String performValidation(Component parentComponent, String path)
    {
        RomSupportContext supportContext = isRomSupported(rom);

        if (!supportContext.isSupported()) {
            String errorMessage = supportContext.getErrorMessage().orElse("This ROM has failed a validation check for an unknown reason.");
            JOptionPane.showMessageDialog(parentComponent, errorMessage, "ROM Not Supported", JOptionPane.ERROR_MESSAGE);
            rom = null;
            return null;
        }
        return path;
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

    /**
     * Used to represent whether a rom is supported and an error message if one is needed
     */
    public static class RomSupportContext
    {
        private final boolean supported;
        private final String errorMessage;

        /**
         * Creates a new <code>RomSupportContext</code> with the given info
         * @param supported a <code>boolean</code> representing if a user-supplied ROM is supported by a <code>Tool</code>
         * @param errorMessage a <code>String</code> representing the error message to be displayed if their ROM is not supported
         */
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
