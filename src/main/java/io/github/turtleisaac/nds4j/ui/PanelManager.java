package io.github.turtleisaac.nds4j.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

/**
 * A class which manages a set of panels which require cross-communication and sharing of data for a <code>Tool</code>
 */
public abstract class PanelManager
{
    private final Tool tool;
    private final String name;
    private List<JPanel> cachedPanels;

    /**
     * Creates a new <code>PanelManager</code> which has access to the information contained
     * in the provided <code>Tool</code>
     * @param tool a <code>Tool</code>
     */
    public PanelManager(Tool tool, String name)
    {
        this.tool = tool;
        this.name = name;
        //todo pass the abstract action functions as action listener code for the ToolFrame
    }

    /**
     * A function which returns the panels controlled by this <code>PanelManager</code> to add to
     * the global <code>ToolFrame</code>'s <code>JTabbedPane</code>
     * @return a <code>List</code><<code>JPanel</code>> containing the panels controlled by this <code>PanelManager</code>
     */
    public abstract List<JPanel> getPanels();

    /**
     * Gets the panels controlled by this <code>PanelManager</code>, calling <code>getPanels()</code> only once.
     * <p>Implementations are free to build their panels inside <code>getPanels()</code>, so it must not be
     * called repeatedly - the <code>ToolFrame</code> keeps the instances it was handed the first time, and a
     * fresh set would no longer be the panels that are actually mounted.</p>
     * @return a <code>List</code><<code>JPanel</code>> containing the panels controlled by this <code>PanelManager</code>
     */
    public final List<JPanel> panels()
    {
        if (cachedPanels == null)
            cachedPanels = getPanels();
        return cachedPanels;
    }

    /**
     * Gets whether the panels controlled by this <code>PanelManager</code> have unsaved changes
     * @return a <code>boolean</code> containing whether this <code>PanelManager</code> has unsaved changes
     */
    public abstract boolean hasUnsavedChanges();

//    /**
//     * A function which will be called when the forwards button (right facing arrow) is pressed in the tool frame.
//     */
//    public abstract void doForwardsButtonAction(ActionEvent e);
//
//    /**
//     * A function which will be called when the back button (left facing arrow) is pressed in the tool frame.
//     */
//    public abstract void doBackButtonAction(ActionEvent e);

    /**
     * A function which will be called when the info button (the one with the letter "i") is pressed in the tool frame.
     */
    public abstract void doInfoButtonAction(ActionEvent e);

    public void doToolFrameSelectedTabChangedAction(ChangeEvent e)
    {
        for (JPanel panel : panels())
        {
            if (panel instanceof PanelGroup group)
            {
                group.containerSelectedTabChanged();
            }
        }
    }

    public void addMenu(JMenu menu)
    {
        tool.getToolFrame().addMenuToBar(menu);
    }

    public void addMenu(JMenu menu, int idx)
    {
        tool.getToolFrame().addMenuToBar(menu, idx);
    }

    public Optional<JMenu> getMenu(String name)
    {
        return tool.getToolFrame().getMenu(name);
    }

//    /**
//     * This is to be used for a project-based tool saving changes which are currently stored in memory back to disk.
////     */
//    public boolean wipeAndWriteUnpacked()
//    {
//        return tool.wipeAndWriteUnpacked(null);
//    }
//
//    /**
//     * This is to be used for a project-based tool saving changes which are currently stored in memory back to disk,
//     * <p>and creating a commit in the process </p>
//     * @param commitMessage the <code>String</code> to use as the commit message for the commit associated with this action
////     */
//    public boolean wipeAndWriteUnpacked(String commitMessage)
//    {
//        return tool.wipeAndWriteUnpacked(commitMessage);
//    }

    /**
     * Collects several sections to be written together, so a save spanning more than one file
     * either happens or does not. Prefer this to a run of the single-section methods below: those
     * replace each file as they reach it, so a failure part way through leaves the project half
     * saved.
     * @return a builder; nothing is written until <code>write()</code> is called
     * @throws UnsupportedOperationException if the tool is not project-based
     */
    public Tool.SaveBatch saveBatch()
    {
        return tool.saveBatch();
    }

    /**
     * This is to be used for a project-based tool saving changes which are currently stored in memory back to disk.
     * @param pathWithinRom a <code>String</code> containing the path of the file within the ROM's filesystem
     */
    public void writeModifiedFile(String pathWithinRom)
    {
        tool.writeModifiedFile(pathWithinRom);
    }

    /**
     * This is to be used for a project-based tool saving the modified arm9 binary back to disk.
     */
    public void writeModifiedArm9()
    {
        tool.writeModifiedArm9();
    }

    /**
     * This is to be used for a project-based tool saving the modified arm7 binary back to disk.
     */
    public void writeModifiedArm7()
    {
        tool.writeModifiedArm7();
    }

    /**
     * This is to be used for a project-based tool saving the modified arm9 overlay table back to disk.
     */
    public void writeModifiedY9()
    {
        tool.writeModifiedY9();
    }

    /**
     * This is to be used for a project-based tool saving the modified arm7 overlay table back to disk.
     */
    public void writeModifiedY7()
    {
        tool.writeModifiedY7();
    }

    /**
     * This is to be used for a project-based tool saving a modified arm9 overlay back to disk.
     * @param overlayId the ID of the overlay to write
     */
    public void writeModifiedOverlay(int overlayId)
    {
        tool.writeModifiedOverlay(overlayId);
    }

    /**
     * This is to be used for a project-based tool saving the modified icon banner back to disk.
     */
    public void writeModifiedBanner()
    {
        tool.writeModifiedBanner();
    }

    /**
     * This is to be used for a project-based tool saving the modified ROM header back to disk.
     */
    public void writeHeader()
    {
        tool.writeHeader();
    }

    /**
     * This is to be used for a project-based tool saving the contents of the Projectfile back to disk.
     */
    public boolean writeProjectInfo()
    {
        return tool.writeProjectInfo();
    }

    public boolean commit(String commitMessage)
    {
        return tool.commit(commitMessage);
    }

    public static class PanelGroup extends JPanel {
        private final String groupName;
        private final JPanel[] panels;

        private final JLabel nameLabel;
        private final JComboBox<String> panelSelector;

        private JTabbedPane container;

        public PanelGroup(String name, JPanel... panels)
        {
            this.groupName = name;
            this.panels = panels;
            this.nameLabel = new JLabel(name);

            Dimension largestNeeded = new Dimension();
            this.panelSelector = new JComboBox<>();
            for (JPanel panel : panels)
            {
                Dimension panelPreferredSize = panel.getPreferredSize();
                if (largestNeeded.height < panelPreferredSize.height)
                    largestNeeded.height = panelPreferredSize.height;
                if (largestNeeded.width < panelPreferredSize.width)
                    largestNeeded.width = panelPreferredSize.width;

                panelSelector.addItem(panel.getName());
            }

            for (JPanel panel : panels)
                panel.setMinimumSize(largestNeeded);

            add(nameLabel);
            add(panelSelector);

            panelSelector.addActionListener(this::performPanelChange);
            setBackground(new Color(0,0,0,0));
        }

        public String getName()
        {
            return groupName;
        }

        public JPanel[] getPanels()
        {
            return panels;
        }

        protected int getPanelCount()
        {
            return panels.length;
        }

        protected int getSelectedIndex()
        {
            return panelSelector.getSelectedIndex();
        }

        /**
         * Sets which of this group's panels is the selected one, without touching the container.
         * @param index the index of the panel to select
         */
        protected void setSelectedIndex(int index)
        {
            if (index >= 0 && index < panels.length)
                panelSelector.setSelectedIndex(index);
        }

        protected void performPanelChange(ActionEvent e)
        {
            if (container != null && panels.length > 1)
            {
                int selected = container.getSelectedIndex();
                container.removeTabAt(selected);
                container.insertTab(null, null, panels[panelSelector.getSelectedIndex()], null, selected);
                container.setTabComponentAt(selected, this);
                container.setSelectedIndex(selected);
            }
        }

        /**
         * Sets this <code>PanelGroup</code>'s container to the provided <code>JTabbedPane</code>
         * @param container a <code>JTabbedPane</code>
         */
        public void setContainer(JTabbedPane container)
        {
            this.container = container;
        }

        protected void containerSelectedTabChanged()
        {
            if (container != null)
            {
                int selected = container.getSelectedIndex();
                if (selected < 0)
                    return;

                if (container.getTabComponentAt(selected) == this) {
                    nameLabel.setText(groupName + ":");
                    add(panelSelector);
                }
                else if (panels.length > 1)
                {
                    nameLabel.setText(groupName);
                    remove(panelSelector);
                }
            }
        }
    }
}
