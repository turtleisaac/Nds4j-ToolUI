# Nds4j-ToolUI
Framework for development of Nintendo DS ROMhacking tools.

This library provides all of the code needed for the initial GUI setup, ROM opening, and more for making your own DS hacking tools.

Probably not usable currently since I don't have the newest changes to Nds4j on maven yet.



It supports two modes, which are a project-based system, or direct ROM opening. Within direct ROM opening, you can break that down to window mode and function mode.

See [Nds4j](https://github.com/turtleisaac/Nds4j), my library which this was designed to complement, for more info on the backend side of things. If you are looking to develop tools, please familiarize yourself with what Nds4j has to offer before you begin writing code which incorporates this library.

## Example for Project Mode

**Note:** The intended use is for you to create your own class (or multiple) which extends the abstract class `PanelManager` and implements all of the functionality needed for creating your tool panels and managing the sharing of data between them. What I have below is just a demonstration of a valid input.
```java
        Tool tool = Tool.create();
        tool.setType(ProgramType.PROJECT)
                .setName("Test Tool")
                .setVersion("1.0.0")
                .setFlavorText("Hello world")
                .setAuthor("Developed by Turtleisaac")
                .addLookAndFeel(new FlatDarkPurpleIJTheme())
                .addLookAndFeel(new FlatArcOrangeIJTheme())
                .addGame("Pokémon Platinum", "CPU")
                .addGame("Pokémon HeartGold","IPK")
                .addGame("Pokémon SoulSilver","IPG")
                .addPanelManager(() -> new PanelManager(tool, "Test Manager")
                {
                    @Override
                    public List<JPanel> getPanels()
                    {
                        JPanel panel = new JPanel();
                        panel.setName("Panel 1");
                        panel.setBackground(Color.RED);

                        JPanel panel2 = new JPanel();
                        panel2.setBackground(Color.BLUE);
                        panel2.setName("Panel 2");

                        JPanel panel3 = new JPanel();
                        panel3.setBackground(Color.GREEN);
                        panel3.setName("Panel 3");
                        return Arrays.asList(panel, panel2, panel3);
                    }

                    @Override
                    public boolean hasUnsavedChanges()
                    {
                        return false;
                    }

                    @Override
                    public void doForwardsButtonAction(ActionEvent e)
                    {

                    }

                    @Override
                    public void doBackButtonAction(ActionEvent e)
                    {

                    }

                    @Override
                    public void doInfoButtonAction(ActionEvent e)
                    {

                    }
                })
                .init();
```

The above code can produce the below results:

<img width="407" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/d9505fe5-3f24-4a7e-8f84-88d64635c6a2">
<img width="612" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/1b86fe9c-6edf-42b3-b0af-9d5509d06d7e">
<img width="612" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/26882865-f376-465e-b52f-048d93753e0f">
<img width="687" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/4ff329d8-ab39-4d32-97c0-7ae78d472de2">
<img width="687" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/8ec610c2-141c-44f9-8864-96608a8c16ad">
<img width="687" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/bea3b02f-1a80-4484-ae4e-a38ae907eb3d">




## Example for ROM Function Mode

```java
        Tool tool = Tool.create();
        tool.setType(ProgramType.ROM)
                .addGame("Pokémon HeartGold","IPK")
                .addGame("Pokémon SoulSilver","IPG")
                .addFunction(rom ->
                {
                    Narc personal = new Narc(rom.getFileByName("a/0/0/2"));
                    for (int i = 0; i < personal.getNumFiles(); i++)
                    {
                        personal.setFile(i, personal.getFile(399));
                    }
                    return null;
                })
                .init();
```

The above code does the following:
1. Gives the user a file select prompt for loading a ROM file.
2. Ensures they select a ROM which is either Pokémon HeartGold or Pokémon SoulSilver
3. Runs the code specified by `.addFunction()` on their ROM
   * (this code in particular sets the personal species data for all species to that of the Pokémon Bidoof, which is at index 399 in the personal NARC)
5. Gives the user a prompt to save the output ROM file to disk.
