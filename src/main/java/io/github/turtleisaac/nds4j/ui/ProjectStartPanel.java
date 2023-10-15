/*
 * Created by JFormDesigner on Wed Oct 11 16:22:40 CDT 2023
 */

package io.github.turtleisaac.nds4j.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.formdev.flatlaf.util.SystemInfo;
import io.github.turtleisaac.nds4j.NintendoDsRom;
import net.miginfocom.swing.*;

public class ProjectStartPanel extends JPanel {

    private Tool tool;

    private String projectPath;
    private JsonNode projectInfo;
    private boolean projectOpened;

    private boolean languageButtonRemoved = false;

    protected ProjectStartPanel(Tool tool) {
        initComponents();
        projectOpened = false;

        this.tool = tool;
        if (SystemInfo.isMacFullWindowContentSupported) {
            toolBar1.add(Box.createHorizontalStrut(70),0);
        }
    }

    private void changeTheme(ActionEvent e) {
        ThemeUtils.changeTheme();
    }

    private void newProject(ActionEvent e) {
        ProjectCreateDialog dialog = new ProjectCreateDialog(tool);
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                super.windowClosed(e);
                if (dialog.wasProjectCreated())
                    prepareForToolWindowStart(dialog.getProjectPath());
            }
        });
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void openProjectButtonPressed(ActionEvent e) {
        String projectPath = tool.selectAndValidateProject(this);
        prepareForToolWindowStart(projectPath);
    }

    private void prepareForToolWindowStart(String projectPath)
    {
        projectOpened = true;
        this.projectPath = projectPath;
        tool.getProjectStartFrame().dispose();
    }

    /**
     * Gets whether a project was opened successfully by this <code>ProjectStartPanel</code>
     * @return a <code>boolean</code>
     */
    public boolean wasProjectOpened()
    {
        return projectOpened;
    }

    /**
     * Gets the path to the loaded project
     * @return a <code>String</code>
     */
    public String getProjectPath()
    {
        return projectPath;
    }

    /**
     * Gets the contents of the Projectfile for the opened project
     * @return a <code>JsonNode</code> containing the contents of the opened project's Projectfile
     */
    public JsonNode getProjectInfo()
    {
        return projectInfo;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        ResourceBundle bundle = ResourceBundle.getBundle("project_gui");
        toolBar1 = new JToolBar();
        hSpacer6 = new JPanel(null);
        button1 = new JButton();
        button2 = new JButton();
        titleLabel = new JLabel();
        versionLabel = new JLabel();
        flavorTextLabel = new JLabel();
        panel1 = new JPanel();
        newProjectButton = new JButton();
        openProjectButton = new JButton();
        changeThemeButton = new JButton();
        changeLanguageButton = new JButton();
        authorLabel = new JLabel();
        baseAuthorLabel = new JLabel();

        //======== this ========
        setLayout(new MigLayout(
            "hidemode 3",
            // columns
            "[grow,fill]",
            // rows
            "[]" +
            "[]" +
            "[]" +
            "[]ind" +
            "[]0" +
            "[]0" +
            "[grow]" +
            "[]" +
            "[]"));

        //======== toolBar1 ========
        {
            toolBar1.add(hSpacer6);

            //---- button1 ----
            button1.setText(bundle.getString("ProjectStartPanel.button1.text"));
            toolBar1.add(button1);

            //---- button2 ----
            button2.setText(bundle.getString("ProjectStartPanel.button2.text"));
            toolBar1.add(button2);
        }
        add(toolBar1, "north");

        //---- titleLabel ----
        titleLabel.setText(bundle.getString("ProjectStartPanel.titleLabel.text"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(34f));
        add(titleLabel, "cell 0 1,alignx center,grow 0 100");

        //---- versionLabel ----
        versionLabel.setText(bundle.getString("ProjectStartPanel.versionLabel.text"));
        versionLabel.setFont(versionLabel.getFont().deriveFont(19f));
        add(versionLabel, "cell 0 2,alignx center,grow 0 100");

        //---- flavorTextLabel ----
        flavorTextLabel.setText(bundle.getString("ProjectStartPanel.flavorTextLabel.text"));
        add(flavorTextLabel, "cell 0 3,alignx center,growx 0");

        //======== panel1 ========
        {
            panel1.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[grow,fill]",
                // rows
                "[]" +
                "[]" +
                "[]"));

            //---- newProjectButton ----
            newProjectButton.setText(bundle.getString("ProjectStartPanel.newProjectButton.text"));
            newProjectButton.addActionListener(e -> newProject(e));
            panel1.add(newProjectButton, "cell 0 0");

            //---- openProjectButton ----
            openProjectButton.setText(bundle.getString("ProjectStartPanel.openProjectButton.text"));
            openProjectButton.addActionListener(e -> openProjectButtonPressed(e));
            panel1.add(openProjectButton, "cell 0 1");

            //---- changeThemeButton ----
            changeThemeButton.setText(bundle.getString("ProjectStartPanel.changeThemeButton.text"));
            changeThemeButton.addActionListener(e -> changeTheme(e));
            panel1.add(changeThemeButton, "cell 0 2");

            //---- changeLanguageButton ----
            changeLanguageButton.setText(bundle.getString("ProjectStartPanel.changeLanguageButton.text"));
            panel1.add(changeLanguageButton, "cell 0 2");
        }
        add(panel1, "cell 0 6");

        //---- authorLabel ----
        authorLabel.setText(bundle.getString("ProjectStartPanel.authorLabel.text"));
        authorLabel.setFont(authorLabel.getFont().deriveFont(authorLabel.getFont().getStyle() | Font.ITALIC));
        add(authorLabel, "cell 0 7,alignx center,growx 0");

        //---- baseAuthorLabel ----
        baseAuthorLabel.setText(bundle.getString("ProjectStartPanel.baseAuthorLabel.text"));
        baseAuthorLabel.setFont(baseAuthorLabel.getFont().deriveFont(baseAuthorLabel.getFont().getStyle() | Font.ITALIC));
        add(baseAuthorLabel, "cell 0 8,align center center,grow 0 0");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JToolBar toolBar1;
    private JPanel hSpacer6;
    private JButton button1;
    private JButton button2;
    private JLabel titleLabel;
    private JLabel versionLabel;
    private JLabel flavorTextLabel;
    private JPanel panel1;
    private JButton newProjectButton;
    private JButton openProjectButton;
    private JButton changeThemeButton;
    private JButton changeLanguageButton;
    private JLabel authorLabel;
    private JLabel baseAuthorLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    protected void setTitle(String text) {
        titleLabel.setText(text);
    }

    protected void setVersion(String text) {
        versionLabel.setText(text);
    }

    protected void setFlavorText(String text) {
        flavorTextLabel.setText(text);
    }

    protected void setAuthor(String text) {
        authorLabel.setText(text);
    }

    private boolean themeButtonRemoved = false;

    /**
     * Removes the theme button from this panel and adjusts the position and properties of the
     * language button if it is still present
     */
    protected void removeThemeButton() {
        panel1.remove(changeThemeButton);
        if (!languageButtonRemoved) {
            remove(changeLanguageButton);
            panel1.add(changeLanguageButton, "cell 0 2,alignx center,growx 0");
        }
        themeButtonRemoved = true;
    }

    /**
     * Removes the language button from this panel and adjusts the position and properties of the
     * theme button if it is still present
     */
    protected void removeLanguageButton() {
        panel1.remove(changeLanguageButton);
        if (!themeButtonRemoved) {
            remove(changeThemeButton);
            panel1.add(changeThemeButton, "cell 0 2,alignx center,growx 0");
        }
        languageButtonRemoved = true;
    }
}
