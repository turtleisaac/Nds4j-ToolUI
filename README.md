# Nds4j-ToolUI
Framework for development of Nintendo DS ROMhacking tools.

This library provides all of the code needed for the initial GUI setup, ROM opening, and more for making your own DS hacking tools.

It supports two modes, which are a project-based system, or direct ROM opening.

See [Nds4j](https://github.com/turtleisaac/Nds4j), my library which this was designed to complement, for more info on the backend side of things.

Example code:

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
                .addToolPanel(() -> new ToolPanel(tool)
                {
                    @Override
                    public void doForwardsButtonAction()
                    {
                        // not important yet
                    }

                    @Override
                    public void doBackButtonAction()
                    {
                        // not important yet
                    }
                })
                .init();
```

The above code can produce the below results:

<img width="407" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/d9505fe5-3f24-4a7e-8f84-88d64635c6a2">
<img width="612" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/1b86fe9c-6edf-42b3-b0af-9d5509d06d7e">
<img width="772" alt="image" src="https://github.com/turtleisaac/Nds4j-ToolUI/assets/7987859/aafea869-8575-46c7-b0f4-8a72e6983bf0">
