package io.github.turtleisaac.nds4j.ui;

import io.github.turtleisaac.nds4j.ui.exceptions.ToolCreationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The builder's promises, held to.
 * <p>
 * Every case here is one a tool developer can hit by following the documentation, and every one of
 * them used to fail silently or with an exception that named nothing useful. They are grouped by
 * what the developer was trying to do, because that is what the failure has to explain back to
 * them.
 */
@DisplayName("A Tool holds its builder contract")
class ToolContractTest
{
    /** The constructor is private because tools are built through create(); tests need it directly. */
    private static Tool tool() throws Exception
    {
        Constructor<Tool> constructor = Tool.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Nested
    @DisplayName("when the program type and the work given to it disagree")
    class TypeAgreement
    {
        /**
         * addFunction() runs against a packed ROM, and only startRomBasedTool() ever calls the
         * functions. Combining it with PROJECT used to be accepted: the tool opened a project start
         * window with a blank title, then a tool window with no tabs, and the functions never ran.
         * Nothing reported a problem, because from the framework's point of view nothing had gone
         * wrong - it had simply been asked for two different programs at once.
         */
        @Test
        @DisplayName("a function-based tool asked to be project-based is refused, not quietly started")
        void functionsWithProjectTypeAreRejected() throws Exception
        {
            Tool tool = tool();
            tool.setType(ProgramType.PROJECT).addFunction(rom -> { });

            assertThatThrownBy(tool::init)
                    .isInstanceOf(ToolCreationException.class)
                    .hasMessageContaining("ProgramType.ROM");
        }
    }

    @Nested
    @DisplayName("when a ROM fails more than one validation check")
    class ValidationOrdering
    {
        /**
         * isRomSupported() returns on the first failing check, so which message the user sees is
         * decided by iteration order. Holding the checks in a HashMap keyed by the predicate meant
         * that order followed lambda identity hash codes: it varied between runs of the same tool on
         * the same ROM, so a user's bug report and the developer's reproduction could disagree about
         * what the tool had said. Registration order is the only order the developer can reason
         * about, so it is the one that is kept.
         */
        @Test
        @DisplayName("the message is the first check that was registered, every time")
        void firstRegisteredFailureWins() throws Exception
        {
            for (int attempt = 0; attempt < 50; attempt++)
            {
                Tool tool = tool();
                tool.addValidationCheck(rom -> false, "checked first")
                    .addValidationCheck(rom -> false, "checked second")
                    .addValidationCheck(rom -> false, "checked third");

                Tool.RomSupportContext result = tool.isRomSupported(null);

                assertThat(result.isSupported()).isFalse();
                assertThat(result.getErrorMessage()).contains("checked first");
            }
        }

        @Test
        @DisplayName("a check with no predicate is refused where it is written")
        void nullPredicateIsRejected() throws Exception
        {
            Tool tool = tool();
            assertThatThrownBy(() -> tool.addValidationCheck(null, "never reached"))
                    .hasMessageContaining("predicate");
        }
    }

    @Nested
    @DisplayName("when a tool reads back what it registered")
    class Encapsulation
    {
        /**
         * The builder refuses every setter once init() has run, which only means anything if the
         * state behind those setters cannot be reached another way. getGameCodes() handed out the
         * live list, so a panel could clear it long after start-up and turn every subsequent ROM
         * into a supported one.
         */
        @Test
        @DisplayName("the supported game codes cannot be edited through the getter")
        void gameCodesAreNotLive() throws Exception
        {
            Tool tool = tool();
            tool.addGame("Pokémon Platinum", "CPU");

            assertThatThrownBy(() -> tool.getGameCodes().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThat(tool.getGameCodes()).containsExactly("CPU");
        }
    }

    @Nested
    @DisplayName("when no look and feel was registered")
    class Theming
    {
        /**
         * ThemeUtils is public, so a tool can wire its own "change theme" action and reach
         * changeTheme() before registering anything. The iterator's empty-list branch reset its
         * index and indexed into the empty list anyway, so the tool died on an
         * IndexOutOfBoundsException from a method whose only documented failure is a
         * ToolAttributeModificationException.
         */
        /**
         * ThemeUtils holds its themes in a static list shared by the whole JVM, so this states the
         * condition it depends on rather than trusting that no other test registered one.
         */
        @Test
        @DisplayName("changing theme says what is missing instead of indexing an empty list")
        void changingThemeWithNoThemesIsExplained() throws Exception
        {
            java.lang.reflect.Field field = ThemeUtils.class.getDeclaredField("themes");
            field.setAccessible(true);
            java.util.ArrayList<?> themes = (java.util.ArrayList<?>) field.get(null);
            java.util.List<Object> saved = new java.util.ArrayList<>(themes);

            themes.clear();
            try {
                assertThatThrownBy(ThemeUtils::changeTheme)
                        .isInstanceOf(java.util.NoSuchElementException.class)
                        .hasMessageContaining("addLookAndFeel");
            }
            finally {
                @SuppressWarnings("unchecked")
                java.util.List<Object> restore = (java.util.List<Object>) themes;
                restore.addAll(saved);
            }
        }
    }
}
