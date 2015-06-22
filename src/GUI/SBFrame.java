package GUI;

import javax.swing.*;
import java.awt.*;

/**
 * A standard class for all frames in SafariBowl.
 * Created by milan on 18.3.15.
 */
public abstract class SBFrame extends JFrame {

    private static final Dimension DEFAULT_DIMENSIONS = new Dimension(700, 500);
    private static final int DEFAULT_CLOSE_OPERATION = WindowConstants.EXIT_ON_CLOSE;

    /**
     * Create a new frame with a title.
     * @param title The frame title.
     */
    public SBFrame(String title) {
        super(title);
        setBounds(0, 0, DEFAULT_DIMENSIONS.width, DEFAULT_DIMENSIONS.height);
        setDefaultCloseOperation(DEFAULT_CLOSE_OPERATION);
    }

    /**
     * Create a new frame with a title and a size.
     * @param title The frame title.
     * @param size The frame size.
     */
    public SBFrame(String title, Dimension size) {
        super(title);
        setBounds(0, 0, size.width, size.height);
        setDefaultCloseOperation(DEFAULT_CLOSE_OPERATION);
    }

    /**
     * Set the size of this frame.
     * @param size The size to resize this frame to.
     */
    public void setSize(Dimension size) {
        setBounds(getX(), getY(), size.width, size.height);
    }

    /**
     * Center the frame on the screen.
     */
    public void center() {
        int left = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width/2 - getWidth()/2;
        int top = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height/2 - getHeight()/2;
        setBounds(left, top, getWidth(), getHeight());
    }

    public static void center(JFrame frame) {
        int left = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width/2 - frame.getWidth()/2;
        int top = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height/2 - frame.getHeight()/2;
        frame.setBounds(left, top, frame.getWidth(), frame.getHeight());
    }

    public Dimension getAvailableSpaceOnScreen() {
//        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
//        return new Dimension(screenSize.width - screenInsets.left - screenInsets.right, screenSize.height - screenInsets.top - screenInsets.bottom);
        int     screenWidth =  GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width,
                screenHeight = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
        return new Dimension(screenWidth, screenHeight);
    }

    /**
     * A scroller class that scrolls textareas in scroll panes.
     */
    private static class Scroller extends Thread {
        private JTextArea area;
        private JScrollPane pane;

        public Scroller(JTextArea area, JScrollPane pane) {
            super();
            this.area = area;
            this.pane = pane;
        }

        @Override
        public void run() {
            int oldTextLength = 0;
            while(area != null) {
                int newTextLength = area.getText().length();
                if(newTextLength > oldTextLength) {
                    oldTextLength = newTextLength;
                    scrollToBottom();
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Scrolls the chat area to the bottom.
         */
        private void scrollToBottom() {
            pane.getVerticalScrollBar().setValue(pane.getVerticalScrollBar().getMaximum());
            pane.getHorizontalScrollBar().setValue(pane.getHorizontalScrollBar().getMinimum());
            pane.revalidate();
            pane.repaint();
        }
    }

    /**
     * Add a new scroller for the given area and scroll pane.
     * @param area The area to scroll.
     * @param pane The scroll pane to scroll on.
     */
    public static void addScroller(JTextArea area, JScrollPane pane) {
        (new Scroller(area, pane)).start();
    }

}