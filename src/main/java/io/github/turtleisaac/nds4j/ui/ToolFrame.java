/*
 * Created by JFormDesigner
 */

package io.github.turtleisaac.nds4j.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import com.formdev.flatlaf.util.SystemInfo;
import net.miginfocom.swing.*;

/**
 * A class which represents the frame for a tool, incorporating pre-defined elements and user-defined panels
 * @author turtleisaac
 */
public class ToolFrame extends JFrame {
    private final Tool tool;

    private final Component macOsSpacer;

    private List<PanelManager> panelManagers;

    protected ToolFrame(Tool tool) {
        initComponents();
        this.tool = tool;
        panelManagers = new ArrayList<>();
//        tabbedPane1.setUI(new FlatTabbedPaneUI() {
//            @Override
//            protected boolean hideTabArea()
//            {
//                return true;
//            }
//        });


        macOsSpacer = Box.createHorizontalStrut(70);
        toolBar1.add(macOsSpacer,0);
        setIcons();

//        addComponentListener(new ComponentAdapter()
//        {
//            @Override
//            public void componentResized(ComponentEvent e)
//            {
//                super.componentResized(e);
//                setTitle(getSize().toString());
//            }
//        });
    }

    private void setIcons()
    {
        backButton.setIcon(ThemeUtils.leftIcon);
        forwardsButton.setIcon(ThemeUtils.rightIcon);
        openButton.setIcon(ThemeUtils.folderOpenIcon);
        saveButton.setIcon(ThemeUtils.saveIcon);
        infoButton.setIcon(ThemeUtils.infoIcon);
        tabsButton.setIcon(ThemeUtils.appWindowIcon);
        pack();
    }

    /**
     * Adds the panels controlled by the provided <code>PanelManager</code> to this <code>ToolFrame</code>
     * @param manager a <code>PanelManager</code> defined by the developer
     */
    protected void addToolPanels(PanelManager manager)
    {
        panelManagers.add(manager);
        for (JPanel panel : manager.getPanels()) {
            tabbedPane1.addTab(panel.getName(), panel);
        }
    }

    /**
     * Appends the specified menu to the end of this frame's menu bar.
     * @param menu the <code>JMenu</code> component to add
     */
    protected void addMenuToBar(JMenu menu)
    {
        menuBar1.add(menu);
    }

    /**
     * Appends the specified menu to the end of this frame's menu bar at
     * the given position
     * @param menu the component to be added
     * @param index the position at which to insert the component,
     *              or {@code -1} to append the component to the end
     * @throws NullPointerException if {@code comp} is {@code null}
     * @throws IllegalArgumentException if {@code index} is invalid
     */
    protected void addMenuToBar(JMenu menu, int index)
    {
        menuBar1.add(menu, index);
    }

    /**
     * Gets the menu with the specified name from this frame's menu bar
     * @param name a <code>String</code> containing the name of the <code>JMenu</code> to look for
     * @return the <code>JMenu</code> in this frame's menu bar with the given name wrapped in a <code>Optional</code> if it exists,
     * <p>otherwise <code>Optional.empty()</code></p>
     */
    protected Optional<JMenu> getMenu(String name)
    {
        for (int i = 0; i < menuBar1.getMenuCount(); i++) {
            String menuName = menuBar1.getMenu(i).getName();
            if (menuName != null && menuName.equals(name))
                return Optional.ofNullable(menuBar1.getMenu(i));
        }
        return Optional.empty();
    }

    private void tabsButtonPressed(ActionEvent e) {
//        tabbedPane1.setUI(new FlatTabbedPaneUI() {
//            @Override
//            protected boolean hideTabArea()
//            {
//                return !tabsButton.isSelected();
//            }
//        });
    }

    private void changeThemeItem(ActionEvent e) {
        ThemeUtils.changeTheme();
    }

    private void saveButtonPressed(ActionEvent e) {
        //todo query PanelManagers for unsaved changes status

        String outputPath = Tool.selectRomToExport();
        try {
            tool.getRom().saveToFile(outputPath, true);
        }
        catch(IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void openProjectButtonPressed(ActionEvent e) {
        // TODO add your code here
    }

    private void infoButtonPressed(ActionEvent e) {
        // TODO support for multiple PanelManager instances
        if(panelManagers.size() == 1) {
            panelManagers.get(0).doInfoButtonAction(e);
        }
    }

    private void backButtonPressed(ActionEvent e) {
        // TODO support for multiple PanelManager instances
        if(panelManagers.size() == 1) {
            panelManagers.get(0).doBackButtonAction(e);
        }
    }

    private void forwardsButtonPressed(ActionEvent e) {
        // TODO support for multiple PanelManager instances
        if (panelManagers.size() == 1) {
            panelManagers.get(0).doForwardsButtonAction(e);
        }
    }

    private void thisWindowStateChanged(WindowEvent e) {
        boolean isMaximized = e.getNewState() == MAXIMIZED_BOTH;
        boolean wasMaximized = e.getOldState() == MAXIMIZED_BOTH;

        if (SystemInfo.isMacFullWindowContentSupported)
        {
            if (isMaximized && !wasMaximized)
                toolBar1.remove(macOsSpacer);
            else if (wasMaximized && !isMaximized)
                toolBar1.add(macOsSpacer,0);
        }

    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        ResourceBundle bundle = ResourceBundle.getBundle("tool_gui");
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        menuItem1 = new JMenuItem();
        menuItem2 = new JMenuItem();
        menuItem3 = new JMenuItem();
        menuItem4 = new JMenuItem();
        popMenu = new JMenu();
        viewMenu = new JMenu();
        changeThemeItem = new JMenuItem();
        debugMenu = new JMenu();
        helpMenu = new JMenu();
        tabbedPane1 = new JTabbedPane();
        toolBar1 = new JToolBar();
        tabsButton = new JToggleButton();
        backButton = new JButton();
        forwardsButton = new JButton();
        openButton = new JButton();
        saveButton = new JButton();
        hSpacer1 = new JPanel(null);
        titleLabel = new JLabel();
        hSpacer2 = new JPanel(null);
        infoButton = new JButton();

        //======== this ========
        addWindowStateListener(e -> thisWindowStateChanged(e));
        var contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[grow,fill]",
            // rows
            "[grow]"));

        //======== menuBar1 ========
        {

            //======== fileMenu ========
            {
                fileMenu.setText(bundle.getString("ToolFrame.fileMenu.text"));
                fileMenu.setName("File");

                //---- menuItem1 ----
                menuItem1.setText(bundle.getString("ToolFrame.menuItem1.text"));
                fileMenu.add(menuItem1);

                //---- menuItem2 ----
                menuItem2.setText(bundle.getString("ToolFrame.menuItem2.text"));
                fileMenu.add(menuItem2);
                fileMenu.addSeparator();

                //---- menuItem3 ----
                menuItem3.setText(bundle.getString("ToolFrame.menuItem3.text"));
                fileMenu.add(menuItem3);

                //---- menuItem4 ----
                menuItem4.setText(bundle.getString("ToolFrame.menuItem4.text"));
                fileMenu.add(menuItem4);
            }
            menuBar1.add(fileMenu);

            //======== popMenu ========
            {
                popMenu.setText(bundle.getString("ToolFrame.popMenu.text"));
                popMenu.setName("Pop");
            }
            menuBar1.add(popMenu);

            //======== viewMenu ========
            {
                viewMenu.setText(bundle.getString("ToolFrame.viewMenu.text"));
                viewMenu.setName("View");

                //---- changeThemeItem ----
                changeThemeItem.setText(bundle.getString("ToolFrame.changeThemeItem.text"));
                changeThemeItem.addActionListener(e -> changeThemeItem(e));
                viewMenu.add(changeThemeItem);
            }
            menuBar1.add(viewMenu);

            //======== debugMenu ========
            {
                debugMenu.setText(bundle.getString("ToolFrame.debugMenu.text"));
                debugMenu.setName("Debug");
            }
            menuBar1.add(debugMenu);

            //======== helpMenu ========
            {
                helpMenu.setText(bundle.getString("ToolFrame.helpMenu.text"));
                helpMenu.setName("Help");
            }
            menuBar1.add(helpMenu);
        }
        setJMenuBar(menuBar1);
        contentPane.add(tabbedPane1, "cell 0 0,grow");

        //======== toolBar1 ========
        {
            toolBar1.setFloatable(false);

            //---- tabsButton ----
            tabsButton.setIcon(UIManager.getIcon("RadioButtonMenuItem.dashIcon"));
            tabsButton.addActionListener(e -> tabsButtonPressed(e));
            toolBar1.add(tabsButton);
            toolBar1.addSeparator();

            //---- backButton ----
            backButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrowleft.png")));
            backButton.addActionListener(e -> backButtonPressed(e));
            toolBar1.add(backButton);

            //---- forwardsButton ----
            forwardsButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrowright.png")));
            forwardsButton.addActionListener(e -> forwardsButtonPressed(e));
            toolBar1.add(forwardsButton);
            toolBar1.addSeparator();

            //---- openButton ----
            openButton.setIcon(UIManager.getIcon("Tree.openIcon"));
            openButton.addActionListener(e -> openProjectButtonPressed(e));
            toolBar1.add(openButton);

            //---- saveButton ----
            saveButton.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
            saveButton.addActionListener(e -> saveButtonPressed(e));
            toolBar1.add(saveButton);
            toolBar1.addSeparator();
            toolBar1.add(hSpacer1);

            //---- titleLabel ----
            titleLabel.setText(bundle.getString("ToolFrame.titleLabel.text"));
            titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD));
            toolBar1.add(titleLabel);
            toolBar1.add(hSpacer2);
            toolBar1.addSeparator();

            //---- infoButton ----
            infoButton.setIcon(UIManager.getIcon("Tree.leafIcon"));
            infoButton.addActionListener(e -> infoButtonPressed(e));
            toolBar1.add(infoButton);
        }
        contentPane.add(toolBar1, "north");
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    @Override
    public void setTitle(String title)
    {
        super.setTitle(title);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem menuItem1;
    private JMenuItem menuItem2;
    private JMenuItem menuItem3;
    private JMenuItem menuItem4;
    private JMenu popMenu;
    private JMenu viewMenu;
    private JMenuItem changeThemeItem;
    private JMenu debugMenu;
    private JMenu helpMenu;
    private JTabbedPane tabbedPane1;
    private JToolBar toolBar1;
    private JToggleButton tabsButton;
    private JButton backButton;
    private JButton forwardsButton;
    private JButton openButton;
    private JButton saveButton;
    private JPanel hSpacer1;
    private JLabel titleLabel;
    private JPanel hSpacer2;
    private JButton infoButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
