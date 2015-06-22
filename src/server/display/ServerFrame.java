package server.display;

import GUI.SBFrame;
import server.Server;

import javax.swing.*;
import java.awt.*;

/**
 * The main frame for the main server application.
 * Created by milan on 23.3.15.
 */
public class ServerFrame extends SBFrame {

    private static final String MAIN_TITLE = "SafariBowl", SERVER_TITLE = "SafariBowl Server";
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private ServerPanel serverPanel;
    private Server parent;

    /**
     * Create a new server frame with a parent server.
     * @param parent The server that is parent of this frame.
     */
    public ServerFrame(Server parent) {
        super(MAIN_TITLE);
        setUp(parent);
    }

    /**
     * Create a new server frame with a parent server and a frame title.
     * @param parent The server that is parent of this frame.
     * @param title The title for the frame.
     */
    public ServerFrame(Server parent, String title) {
        super(title);
        setUp(parent);
    }

    /**
     * Set the frame up.
     * @param parent The server that is parent of this frame.
     */
    private void setUp(Server parent) {
        this.parent = parent;

        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        // create panels
        serverPanel = new ServerPanel(this);
        // add panels
        mainPanel.add(SERVER_TITLE, serverPanel);
        cardLayout.addLayoutComponent(serverPanel, SERVER_TITLE);
        add(mainPanel);
    }

    /**
     * Show the server configuration panel.
     */
    public void showServerPanel() {
        setSize(new Dimension(600, 420));
        setResizable(false);
        cardLayout.show(mainPanel, SERVER_TITLE);
    }

    /**
     * Get the parent server.
     * @return The server that is parent of this frame.
     */
    public Server getServer() {
        return parent;
    }

    /**
     * Get the server configuration panel.
     * @return The server configuration panel of this frame.
     */
    public ServerPanel getServerPanel() {
        return serverPanel;
    }
}
