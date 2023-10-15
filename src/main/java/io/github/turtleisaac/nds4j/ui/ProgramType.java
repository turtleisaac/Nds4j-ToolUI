package io.github.turtleisaac.nds4j.ui;

/**
 * Dictates the functionality of the tool.
 * <p><code>ProgramType.ROM</code> defines a tool which operates directly on Nintendo DS ROMs</p>
 * <p><code>ProgramType.PROJECT</code> defines a tool which operates on projects containing unpacked Nintendo DS ROMs</p>
 */
public enum ProgramType
{
    /**
     * Defines a tool which operates directly on Nintendo DS ROMs
     */
    ROM,

    /**
     * Defines a tool which operates on projects containing unpacked Nintendo DS ROMs
     */
    PROJECT
}
