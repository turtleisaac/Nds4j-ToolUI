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
        for (JPanel panel : getPanels())
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

    public boolean wipeAndWriteUnpacked()
    {
        return tool.wipeAndWriteUnpacked();
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

            this.panelSelector = new JComboBox<>();
            for (JPanel panel : panels)
            {
                panelSelector.addItem(panel.getName());
            }

            add(nameLabel);
            add(panelSelector);

            panelSelector.addActionListener(this::performPanelChange);
            setBackground(new Color(0,0,0,0));
        }

        public String getName()
        {
            return groupName;
        }

        protected JPanel[] getPanels()
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

        protected void performPanelChange(ActionEvent e)
        {
            if (panels.length > 1)
            {
                int selected = container.getSelectedIndex();
                System.out.println(selected);
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
