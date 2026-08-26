package io.github.turtleisaac.nds4j.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The toolbar icons are SVG, and the library that draws them is reached only at runtime.
 * <p>
 * {@code FlatSVGIcon} comes from flatlaf-extras, but the rendering behind it is jsvg, which no
 * source file here imports - flatlaf finds it on the classpath and uses it if it is there. So the
 * compiler cannot tell whether it is present, and neither can a test that only constructs an icon:
 * a missing renderer produces an icon that draws nothing rather than an error.
 * <p>
 * That is what makes it worth a test. This module used to declare jsvg itself, which overrode the
 * version flatlaf-extras asks for - the same shape as a JGit pin that held a broken version in
 * place until it was found by accident. Removing the declaration is only safe if something checks
 * that icons still draw, and drawing them is the only check that can.
 */
@DisplayName("SVG icons actually render")
class SvgIconRenderingTest
{
    @BeforeAll
    static void headless()
    {
        System.setProperty("java.awt.headless", "true");
    }

    /** @return how many pixels the icon actually painted */
    private static int paintedPixelsOf(String resource)
    {
        FlatSVGIcon icon = new FlatSVGIcon(resource);

        BufferedImage canvas = new BufferedImage(Math.max(icon.getIconWidth(), 1),
                Math.max(icon.getIconHeight(), 1), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();

        int painted = 0;
        for (int x = 0; x < canvas.getWidth(); x++)
            for (int y = 0; y < canvas.getHeight(); y++)
                if ((canvas.getRGB(x, y) >>> 24) != 0)
                    painted++;
        return painted;
    }

    @Test
    @DisplayName("an icon draws something, which is the only proof the SVG renderer is present")
    void anSvgIconPaintsPixels()
    {
        assertThat(paintedPixelsOf("icons/svg/refresh.svg"))
                .as("a blank icon is what a missing SVG renderer looks like - flatlaf reports no "
                        + "error, it simply draws nothing, and every toolbar button goes empty")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("an icon reports a real size rather than collapsing to nothing")
    void anSvgIconHasDimensions()
    {
        FlatSVGIcon icon = new FlatSVGIcon("icons/svg/refresh.svg");

        assertThat(icon.getIconWidth()).as("icon width").isGreaterThan(0);
        assertThat(icon.getIconHeight()).as("icon height").isGreaterThan(0);
    }
}
