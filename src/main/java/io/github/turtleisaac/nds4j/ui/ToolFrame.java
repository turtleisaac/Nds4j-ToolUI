/*
 * Created by JFormDesigner
 */

package io.github.turtleisaac.nds4j.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import net.miginfocom.swing.*;

/**
 * @author turtleisaac
 */
public class ToolFrame extends JFrame {
    private Tool tool;

    public ToolFrame(Tool tool) {
        initComponents();
        this.tool = tool;
        tabbedPane1.setUI(new FlatTabbedPaneUI() {
            @Override
            protected boolean hideTabArea()
            {
                return true;
            }
        });
        toolBar1.add(Box.createHorizontalStrut(70),0);
        setIcons();


        if (tabbedPane1.getTabCount() <= 1) {
            toolBar1.remove(tabsButton);
            toolBar1.remove(1);
        }

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

    protected void addToolPanel(ToolPanel panel)
    {
        tabbedPane1.addTab(tool.getName(), panel);
    }

    private void tabsButton(ActionEvent e) {
        tabbedPane1.setUI(new FlatTabbedPaneUI() {
            @Override
            protected boolean hideTabArea()
            {
                return !tabsButton.isSelected();
            }
        });
    }

    private void changeThemeItem(ActionEvent e) {
        ThemeUtils.changeTheme();
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
            }
            menuBar1.add(popMenu);

            //======== viewMenu ========
            {
                viewMenu.setText(bundle.getString("ToolFrame.viewMenu.text"));

                //---- changeThemeItem ----
                changeThemeItem.setText(bundle.getString("ToolFrame.changeThemeItem.text"));
                changeThemeItem.addActionListener(e -> changeThemeItem(e));
                viewMenu.add(changeThemeItem);
            }
            menuBar1.add(viewMenu);

            //======== debugMenu ========
            {
                debugMenu.setText(bundle.getString("ToolFrame.debugMenu.text"));
            }
            menuBar1.add(debugMenu);

            //======== helpMenu ========
            {
                helpMenu.setText(bundle.getString("ToolFrame.helpMenu.text"));
            }
            menuBar1.add(helpMenu);
        }
        setJMenuBar(menuBar1);

        //======== tabbedPane1 ========
        {
            tabbedPane1.setTabPlacement(SwingConstants.LEFT);
        }
        contentPane.add(tabbedPane1, "cell 0 0,grow");

        //======== toolBar1 ========
        {
            toolBar1.setFloatable(false);

            //---- tabsButton ----
            tabsButton.setIcon(UIManager.getIcon("RadioButtonMenuItem.dashIcon"));
            tabsButton.addActionListener(e -> tabsButton(e));
            toolBar1.add(tabsButton);
            toolBar1.addSeparator();

            //---- backButton ----
            backButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrowleft.png")));
            toolBar1.add(backButton);

            //---- forwardsButton ----
            forwardsButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrowright.png")));
            toolBar1.add(forwardsButton);
            toolBar1.addSeparator();

            //---- openButton ----
            openButton.setIcon(UIManager.getIcon("Tree.openIcon"));
            toolBar1.add(openButton);

            //---- saveButton ----
            saveButton.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
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
