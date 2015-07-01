package server.display;

import GUI.SBFrame;
import GUI.SBGUIPanel;
import server.Server;
import server.ServerController;
import util.SBLogger;
import util.CommandParser;
import util.CommandParser.Command;
import util.CommandResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;

/**
 * The client configuration panel
 * Created by milan on 23.3.15.
 */
public class ServerPanel extends SBGUIPanel {

    private JScrollPane messagesAreaPane;
    private JTextArea messagesArea;
    private JTextField portField, messageField;
    private JLabel portLabel;
    private JButton stopButton, startButton, sendMessageButton;
    private ServerFrame parent;
    private boolean autoFilledChatRecipient;

    /**
     * Create a panel with server configuration to be displayed in a server frame.
     * @param parent The server frame to display this panel in.
     */
    public ServerPanel(ServerFrame parent) {
        super(new GridBagLayout());
        this.parent = parent;

        // prepare components
        messageField = new JTextField(20);
        portLabel = new JLabel("Port");
        portField = new JTextField(ServerController.DEFAULT_PORT+"", 4);
        messagesArea = new JTextArea(1, 20);
        messagesAreaPane = new JScrollPane(messagesArea);
        messagesAreaPane.setPreferredSize(new Dimension(messagesArea.getWidth(), 200));
        messagesArea.setEditable(false);
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        sendMessageButton = new JButton("Send");

        addEventListeners();
        setLayout();

        // add components
        add(messagesAreaPane);

        add(messageField);
        add(sendMessageButton);

        add(portLabel);
        add(portField);

        add(startButton);
        add(stopButton);

        // DOSTUFF™
        SBFrame.addScroller(messagesArea, messagesAreaPane);
    }

    /**
     * Set focus to the port field.
     */
    public void focusPortField() {
        portField.requestFocusInWindow();
    }

    /**
     * Set the GUI to running state.
     */
    public void setServerRunning() {
        startButton.setText("Restart Server");
        stopButton.setEnabled(true);
    }

    /**
     * Set the GUI to stopped state.
     */
    public void setServerStopped() {
        startButton.setText("Start Server");
        stopButton.setEnabled(false);
    }

    /**
     * Enable or disable the buttons and fields.
     * @param enable Whether the buttons should be enabled or disabled.
     */
    public void setControlsEnabled(boolean enable) {
        startButton.setEnabled(enable);
        portField.setEnabled(enable);
        messageField.setEnabled(enable);
        sendMessageButton.setEnabled(enable);
    }

    /**
     * Empties the text in the chat/message field.
     */
    public void emptyMessageField() {
        messageField.setText("");
    }

    /**
     * Tell parent to stop and start the server.
     */
    private void restartServer() {
        stopServer();
        startServer();
    }

    /**
     * Tell parent to start the server.
     */
    private void startServer() {
        // get port from field
        int port = Integer.parseInt(portField.getText());
        // start server if port is valid
        if(port < 0 || port > 65535) portField.requestFocusInWindow();
        else parent.getServer().start(port);
    }

    /**
     * Tell parent to stop the server.
     */
    private void stopServer() {
        parent.getServer().stop();
    }

    /**
     * Append a message without a sender to the message panel.
     * @param message The message to append.
     */
    public void writeMessage(String message) {
        if(messagesArea.getText().length() <= 0) messagesArea.append(message);
        else {
            if(message.startsWith("Loading")) messagesArea.setText(message);
            else messagesArea.append("\n" + message);
        }
    }

    /**
     * Append a chat message to the message panel.
     * @param sender The name of the sender of this message.
     * @param message The message itself.
     */
    public void addChatMessage(String sender, String message) {
        if(messagesArea.getText().length() <= 0) messagesArea.append(sender + ": " + message);
        else messagesArea.append("\n" + sender + ": " + message);
    }

    /**
     * Send a chat message to some- or everybody.
     */
    private void sendMessage() {
        CommandResult cmdResult = CommandParser.parse(
            messageField.getText(),
            parent.getServer());

        switch (cmdResult.getCommandType()) {
            case MESSAGE:
            case GAMES:
            case USERS:
            case CHEAT:
                emptyMessageField();
                break;
            case MESSAGE_ALL:
                messageField.setText("@" + cmdResult.getRecipient() + " ");
                autoFilledChatRecipient = true;
                break;
        }
    }

    /**
     * Outsourced method for setting any handlers to avoid monolith constructor.
     */
    private void addEventListeners() {
        portField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.doClick();
            }
        });
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(startButton.getText().equals("Start Server")) startServer();
                else restartServer();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
        });
        messagesAreaPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                messagesArea.select(messagesArea.getHeight() + 1000000000, 0);
            }
        });
        messageField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        messageField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && autoFilledChatRecipient) emptyMessageField();
                if (e.getKeyCode() != KeyEvent.VK_ENTER) autoFilledChatRecipient = false;
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        sendMessageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        sendMessageButton.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                // submit form if enter was typed on logoutButton
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });
    }

    /**
     * Outsourced method for setting the layout to avoid monolith constructor.
     */
    private void setLayout() {
        // prepare layout
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        setLayout(layout);

        // set constraints
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.insets = new Insets(0, DEFAULT_PADDING, DEFAULT_PADDING/2, 0);
        constraints.gridwidth = 1;
        layout.setConstraints(portLabel, constraints);

        constraints.insets = new Insets(0, 0, DEFAULT_PADDING/2, 0);
        constraints.gridwidth = 1;
        layout.setConstraints(portField, constraints);

        constraints.insets = new Insets(0, DEFAULT_PADDING, DEFAULT_PADDING/4, 0);
        constraints.gridwidth = 4;
        layout.setConstraints(messageField, constraints);

        constraints.insets = new Insets(0, 0, DEFAULT_PADDING/2, 0);
        constraints.gridwidth = 3;
        layout.setConstraints(startButton, constraints);

        constraints.gridwidth = GridBagConstraints.REMAINDER;

        constraints.insets = new Insets(0, 0, DEFAULT_PADDING/4, DEFAULT_PADDING);
        layout.setConstraints(sendMessageButton, constraints);

        constraints.insets = new Insets(DEFAULT_PADDING/2, DEFAULT_PADDING, DEFAULT_PADDING/4, DEFAULT_PADDING);
        layout.setConstraints(messagesAreaPane, constraints);

        constraints.insets = new Insets(0, 0, DEFAULT_PADDING/2, DEFAULT_PADDING);
        layout.setConstraints(stopButton, constraints);
    }
}
