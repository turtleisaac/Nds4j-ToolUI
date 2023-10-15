package io.github.turtleisaac.nds4j.ui;

import javax.swing.*;

public abstract class ToolPanel extends JPanel
{
    private final Tool tool;

    public ToolPanel(Tool tool)
    {
        this.tool = tool;
        //todo pass the two functions as action listener code for the ToolFrame
    }

    public abstract void doForwardsButtonAction();
    public abstract void doBackButtonAction();
}
