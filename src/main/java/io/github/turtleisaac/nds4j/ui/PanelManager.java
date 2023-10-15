package io.github.turtleisaac.nds4j.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * A class which manages a set of panels which require cross-communication and sharing of data for a <code>Tool</code>
 */
public abstract class PanelManager
{
    private final Tool tool;

    /**
     * Creates a new <code>PanelManager</code> which has access to the information contained
     * in the provided <code>Tool</code>
     * @param tool a <code>Tool</code>
     */
    public PanelManager(Tool tool)
    {
        this.tool = tool;
        //todo pass the abstract action functions as action listener code for the ToolFrame
    }

    /**
     * A function which returns the panels controlled by this <code>PanelManager</code> to add to
     * the global <code>ToolFrame</code>'s <code>JTabbedPane</code>
     * @return a <code>List</code><<code>JPanel</code>> containing the panels controlled by this <code>PanelManager</code>
     */
    public abstract List<JPanel> getPanels();

    /**
     * A function which will be called when the forwards button (right facing arrow) is pressed in the tool frame.
     */
    public abstract void doForwardsButtonAction(ActionEvent e);

    /**
     * A function which will be called when the back button (left facing arrow) is pressed in the tool frame.
     */
    public abstract void doBackButtonAction(ActionEvent e);

    /**
     * A function which will be called when the info button (the one with the letter "i") is pressed in the tool frame.
     */
    public abstract void doInfoButtonAction(ActionEvent e);
}
