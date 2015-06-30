package client.display;

import GUI.SBGUIPanel;
import server.Server;
import server.ServerController;
import util.SBLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Level;

/**
 * The client connect panel
 * Created by milan on 23.3.15.
 */
public class ConnectPanel extends SBGUIPanel {

    private final JLabel messageLabel;
    private final JTextField addressField, portField;
    private final JButton connectButton;
    private ClientFrame parent;

    /**
     * Create a panel with client connect GUI to be displayed in a client frame.
     * @param parent The client frame to display this panel in.
     */
    public ConnectPanel(ClientFrame parent) {
        super(new GridBagLayout());
        this.parent = parent;
        // prepare layout
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        setLayout(layout);

        // prepare components
        messageLabel = new JLabel("Connect to SafariBowl server.");
        JLabel addressLabel = new JLabel("IP Address");
        addressField = new JTextField("localhost", 10);
        JLabel portLabel = new JLabel("Port");
        portField = new JTextField(ServerController.DEFAULT_PORT+"", 10);
        connectButton = new JButton("Connect");

        // set constraints
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.insets = new Insets(0, DEFAULT_PADDING, 0, 0);
        constraints.weightx = 16;
        layout.setConstraints(addressLabel, constraints);
        layout.setConstraints(portLabel, constraints);

        constraints.insets = new Insets(0, 0, 0, DEFAULT_PADDING);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(addressField, constraints);
        layout.setConstraints(portField, constraints);

        constraints.weightx = 0;

        constraints.insets = new Insets(DEFAULT_PADDING/4, DEFAULT_PADDING, 0, DEFAULT_PADDING);
        layout.setConstraints(connectButton, constraints);

        constraints.insets = new Insets(0, DEFAULT_PADDING, DEFAULT_PADDING/2, DEFAULT_PADDING);
        layout.setConstraints(messageLabel, constraints);

        // add event listeners
        addressField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                portField.requestFocusInWindow();
            }
        });
        portField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectButton.doClick();
            }
        });
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(connectButton.isEnabled()) submitConnectForm();
            }
        });
        connectButton.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // submit form if enter was typed on connectButton
                if (e.getKeyCode() == KeyEvent.VK_ENTER && connectButton.isEnabled()) submitConnectForm();
            }
        });

        // add components
        add(messageLabel);
        add(addressLabel);
        add(addressField);
        add(portLabel);
        add(portField);
        add(connectButton);
    }

    /**
     * Set the message that is being thrown at the user above the connect field.
     * @param message The story to tell. Please don't be rude to our users.
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * Set focus to the address field.
     */
    public void focusAddressField() {
        addressField.requestFocusInWindow();
    }

    /**
     * Set focus to the port field.
     */
    public void focusPortField() {
        portField.requestFocusInWindow();
    }

    /**
     * Enable or disable the connect button.
     * @param enable Whether the connect button should be enabled or disabled.
     */
    public void setControlsEnabled(boolean enable) {
        connectButton.setEnabled(enable);
    }

    /**
     * Tell parent to connect to server.
     */
    private void submitConnectForm() {
        // get address and port from fields
        String address = addressField.getText();
        int port = Integer.parseInt(portField.getText());
        // submit form if valid
        if(address.length() <= 0) addressField.requestFocusInWindow();
        else if(port < 0 || port > 65535) portField.requestFocusInWindow();
        else parent.getClient().connect(address, port);
    }
}
