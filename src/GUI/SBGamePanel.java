package GUI;

import javax.swing.*;
import java.awt.*;

/**
 * A superclass for Game panels.
 */
public class SBGamePanel extends JPanel {

    public static final int DEFAULT_PADDING = 40;

    /**
     * Create new game panel.
     */
    public SBGamePanel() {
        super();
    }

    /**
     * Create new game panel with a layout.
     * @param layout The layout to create the game panel with.
     */
    public SBGamePanel(LayoutManager layout) {
        super(layout);
    }

}
