package io.github.turtleisaac.nds4j.ui.exceptions;

public class ToolAttributeModificationException extends RuntimeException
{
    public ToolAttributeModificationException()
    {
        super();
    }

    public ToolAttributeModificationException(String errorMessage)
    {
        super(errorMessage);
    }

    public ToolAttributeModificationException(String errorMessage, Throwable throwable)
    {
        super(errorMessage, throwable);
    }
}
