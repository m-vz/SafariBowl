package client.display;

import GUI.SBGUIPanel;
import util.SBLogger;

import javax.swing.*;
import javax.xml.bind.DatatypeConverter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.MessageDigest;
import java.util.logging.Level;

/**
 * The client login panel
 * Created by milan on 23.3.15.
 */
public class LoginPanel extends SBGUIPanel {

    private static final SBLogger L = new SBLogger(LoginPanel.class.getName(), util.SBLogger.LOG_LEVEL);

    private final JLabel messageLabel;
    private final JTextField nameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private ClientFrame parent;

    /**
     * Create a panel with client login GUI to be displayed in a client frame.
     * @param parent The client frame to display this panel in.
     */
    public LoginPanel(ClientFrame parent) {
        super(new GridBagLayout());
        this.parent = parent;
        // prepare layout
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        setLayout(layout);

        // prepare components
        messageLabel = new JLabel("Log in to SafariBowl.");
        JLabel nameLabel = new JLabel("Name");
        nameField = new JTextField(System.getProperty("user.name"), 10);
        JLabel passwordLabel = new JLabel("Password");
        passwordField = new JPasswordField(10);
        loginButton = new JButton("Login or Sign up");

        // set constraints
        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.insets = new Insets(0, DEFAULT_PADDING, 0, 0);
        constraints.weightx = 16;
        layout.setConstraints(nameLabel, constraints);
        layout.setConstraints(passwordLabel, constraints);

        constraints.insets = new Insets(0, 0, 0, DEFAULT_PADDING);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        layout.setConstraints(nameField, constraints);
        layout.setConstraints(passwordField, constraints);

        constraints.weightx = 0;

        constraints.insets = new Insets(DEFAULT_PADDING/4, DEFAULT_PADDING, 0, DEFAULT_PADDING);
        layout.setConstraints(loginButton, constraints);

        constraints.insets = new Insets(0, DEFAULT_PADDING, DEFAULT_PADDING/2, DEFAULT_PADDING);
        layout.setConstraints(messageLabel, constraints);

        // add event listeners
        nameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.requestFocusInWindow();
            }
        });
        passwordField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginButton.doClick();
            }
        });
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitLoginForm();
            }
        });
        loginButton.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                // submit form if enter was typed on loginButton
                if(e.getKeyCode() == KeyEvent.VK_ENTER) submitLoginForm();
            }
        });

        // add components
        add(messageLabel);
        add(nameLabel);
        add(nameField);
        add(passwordLabel);
        add(passwordField);
        add(loginButton);
    }

    /**
     * Prompt the user to retype his password to confirm account creation.
     * @param userToCreate The name of the new user to create.
     * @param password The password to check the retyped password against.
     * @return Whether the user wanted to sign up or not.
     */
    public boolean promptForCreationConfirmation(String userToCreate, String password) {
        // prepare stuff
        String passwordConfirmation = "";
        JPanel passwordPromptPanel = new JPanel();
        JPasswordField passwordPromptField = new JPasswordField(10);
        JLabel passwordPromptLabel = new JLabel("User "+userToCreate+" does not yet exist.\nRetype password to create new user.");

        passwordPromptPanel.setLayout(new BorderLayout(0, DEFAULT_PADDING/4));
        passwordPromptPanel.add(passwordPromptLabel, BorderLayout.NORTH);
        passwordPromptPanel.add(passwordPromptField, BorderLayout.SOUTH);

        // show initial dialog
        int logIn = JOptionPane.showOptionDialog(this,
                passwordPromptPanel,
                "Confirm to sign up as "+userToCreate,
                JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Sign up", "Return to login"}, passwordPromptField);

        // get the typed in password confirmation
        char[] passwordArray = passwordPromptField.getPassword();
        for (char passwordChar: passwordArray) passwordConfirmation += passwordChar;

        // loop until user types in correct password or aborts
        while(logIn == 0 && !passwordConfirmation.equals(password)) {
            passwordPromptLabel.setText("Passwords do not match.\nRetype the password you chose in the login form.");
            logIn = JOptionPane.showOptionDialog(this,
                    passwordPromptPanel,
                    "Confirm to sign up as "+userToCreate,
                    JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                    new String[]{"Sign up", "Return to login"}, passwordPromptField);
            // get the typed in password confirmation
            passwordArray = passwordPromptField.getPassword();
            for (char passwordChar: passwordArray) passwordConfirmation += passwordChar;
        }

        // return whether the user aborted or typed in the correct password
        return logIn == 0;
    }

    /**
     * Set the message that is being thrown at the user above the login field.
     * @param message The story to tell. Please don't be rude to our users.
     */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    /**
     * Change the name that was saved. I don't remember why exactly but that probably is exactly why I need to save names to avoid forgetting them. Have I mentioned I forget a lot?
     * @param newName What was your name again?
     */
    public void changeSavedName(String newName) {
        nameField.setText(newName);
    }

    /**
     * Set focus to the name field.
     */
    public void focusNameField() {
        nameField.requestFocusInWindow();
    }

    /**
     * Submit the login form. Duh.
     */
    private void submitLoginForm() {
        // get name and password from fields
        String name = nameField.getText().toLowerCase().trim(), password = "";
        char[] passwordArray = passwordField.getPassword();
        for (char passwordChar: passwordArray) password += passwordChar;
        // submit form only if valid
        if(name.length() <= 0) {
            setMessage("Name cannot be empty.");
            nameField.requestFocusInWindow();
        }
        else if(password.length() <= 0) {
            setMessage("Password cannot be empty.");
            passwordField.requestFocusInWindow();
        }
        else {
            // check if the chosen username exists already
            if(!parent.getClient().checkIfUserExists(name)) { // username does not yet exist
                // check if user really wants to create a new user
                if (promptForCreationConfirmation(name, password)) {
                    sendLoginForm(name, password); // try to sign up
                }
            } else sendLoginForm(name, password); // if name does exist already, try to log in
        }
    }

    /**
     * Send the login form. Duh.
     * @param name Who wants to be logged in?
     * @param password What is the password of that humble person (or pokémon)?
     */
    private void sendLoginForm(String name, String password) {
        // encrypt password
        MessageDigest md;
        byte[] digest = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes("UTF-8"));
            digest = md.digest(password.getBytes("UTF-8"));
        } catch (Exception e) {
            parent.getClient().logStackTrace(e);
        }
        password = DatatypeConverter.printHexBinary(digest).toLowerCase();
        // submit form
        L.log(Level.INFO, "Submitting login form for " + name);
        if (parent != null) parent.getClient().login(name, password);
        else L.log(Level.SEVERE, "Who are my parents?");
    }
}
