package io.github.turtleisaac.nds4j.ui;

import javax.swing.*;

/**
 * A panel which contains the functionality of a <code>Tool</code>
 */
public abstract class ToolPanel extends JPanel
{
    private final Tool tool;

    /**
     * Creates a new <code>ToolPanel</code> which has access to the information contained in the provided <code>Tool</code>
     * @param tool a <code>Tool</code>
     */
    public ToolPanel(Tool tool)
    {
        this.tool = tool;
        //todo pass the abstract functions as action listener code for the ToolFrame
    }

    /**
     * A function which will be called when the forwards button (right facing arrow) is pressed in the tool frame.
     */
    public abstract void doForwardsButtonAction();

    /**
     * A function which will be called when the back button (left facing arrow) is pressed in the tool frame.
     */
    public abstract void doBackButtonAction();

    /**
     * A function which will be called when the info button (the one with the letter "i") is pressed in the tool frame.
     */
    public abstract void doInfoButtonAction();
}
