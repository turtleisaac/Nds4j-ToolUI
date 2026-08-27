package io.github.turtleisaac.nds4j.ui;

import javax.swing.*;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.text.ParseException;

public class HexadecimalSpinner extends JSpinner
{
    public HexadecimalSpinner()
    {
        super();
        configureEditor();
    }

    public HexadecimalSpinner(SpinnerModel model) {
        super(model);
        configureEditor();
    }

    private void configureEditor()
    {
        DefaultEditor editor = (DefaultEditor) getEditor();
        JFormattedTextField tf = editor.getTextField();
        tf.setFormatterFactory(new HexFormatterFactory());
    }

    private static class HexFormatterFactory extends DefaultFormatterFactory
    {
        public JFormattedTextField.AbstractFormatter getDefaultFormatter() {
            return new HexFormatter();
        }
    }

    private static class HexFormatter extends DefaultFormatter
    {
        public Object stringToValue(String text) throws ParseException
        {
            try {
                if (text.startsWith("0x"))
                    return Integer.parseUnsignedInt(text.substring(2), 16);
                return Integer.parseInt(text);
            } catch (NumberFormatException nfe) {
                throw new ParseException(text,0);
            }
        }

        public String valueToString(Object value)
        {
            if (value == null)
                return "";
            return "0x" + Integer.toHexString(((Number) value).intValue()).toUpperCase();
        }
    }
}
