package GUI;

import javax.swing.*;
import java.awt.*;

/**
 * A superclass for GUI panels.
 */
public class SBGUIPanel extends JPanel {

    protected static final int DEFAULT_PADDING = 40;

    /**
     * Create new GUI panel.
     */
    public SBGUIPanel() {
        super();
    }

    /**
     * Create new GUI panel with a layout.
     * @param layout The layout to create the GUI panel with.
     */
    public SBGUIPanel(LayoutManager layout) {
        super(layout);
    }
}
