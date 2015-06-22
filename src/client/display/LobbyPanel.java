package client.display;

import GUI.SBFrame;
import GUI.SBGUIPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;

/**
 * The client lobby panel
 * Created by milan on 23.3.15.
 */
public class LobbyPanel extends SBGUIPanel {

    private static final int D = 10;
    public static final int SCORE_COLUMN_COUNT = 8;
    private JPanel chatPanel, usersAndGamesPanel, usersPanel, gamesPanel, otherControlsPanel;
    @SuppressWarnings("FieldCanBeLocal")
    private JTable highscoreTable;
    private DefaultTableModel highscoreTableModel;
    private JLabel gamesLabel, usersLabel, highscoreLabel;
    @SuppressWarnings("FieldCanBeLocal")
    private JList<String> gamesList, usersList;
    private DefaultListModel<String> gamesListModel, usersListModel;
    private JTextField chatField, newNameField;
    private JTextArea chatArea;
    private JScrollPane chatAreaPane, usersListPane, gamesListPane, highscorePane;
    private JButton logoutButton, sendMessageButton, changeNameButton, startGameButton, inviteButton;
    private ClientFrame parent;
    private boolean autoFilledChatRecipient;
    private int lastSelectedUserIndex = -1;

    /**
     * Create a panel with client login GUI to be displayed in a client frame.
     * @param parent The client frame to display this panel in.
     */
    public LobbyPanel(ClientFrame parent) {
        super(new GridBagLayout());
        this.parent = parent;
        setUp();
    }

    private void setUp() {
        // prepare areas
        prepareChatArea();
        prepareChatPanel();
        prepareUsersAndGames();
        prepareHighscore();
        prepareOtherControls();

        // DOSTUFF™
        prepareLayouts();
        addComponents();
        addListeners();
        SBFrame.addScroller(chatArea, chatAreaPane);
    }

    public void setSizes(Dimension size) {
        int w = size.width, h = size.height;

        chatAreaPane.setPreferredSize(new Dimension(w / 2, h - 4 * D - 5 * D - 28 * D - 14 * D));
        chatPanel.setPreferredSize(new Dimension(w / 2, 4 * D));
        chatField.setPreferredSize(new Dimension(chatPanel.getWidth() - sendMessageButton.getWidth() - 3 * D, 3 * D));

        otherControlsPanel.setPreferredSize(new Dimension(w / 2, 5 * D));

        int wThird = w/3 - D/2, wTwoThird = 2*w/3 - D/2;
        usersAndGamesPanel.setPreferredSize(new Dimension(w / 2, 28 * D));
        usersPanel.setPreferredSize(new Dimension(wThird, 28 * D));
        gamesPanel.setPreferredSize(new Dimension(wTwoThird, 28 * D));

        usersLabel.setPreferredSize(new Dimension(wThird - 2 * D, 3 * D));
        usersListPane.setPreferredSize(new Dimension(wThird - 2 * D, 22 * D));
        inviteButton.setPreferredSize(new Dimension(wThird - 2 * D, 3 * D));

        highscoreLabel.setPreferredSize(new Dimension(w / 2, 3 * D));
        highscorePane.setPreferredSize(new Dimension(w / 2, 14 * D));

        gamesLabel.setPreferredSize(new Dimension(wTwoThird - 2 * D, 3 * D));
        gamesListPane.setPreferredSize(new Dimension(wTwoThird - 2 * D, 22 * D));
        startGameButton.setPreferredSize(new Dimension(wTwoThird - 2 * D, 3 * D));

        setPreferredSize(size);
    }

    /**
     * Set focus to the chat field.
     */
    public void focusChatField() {
        chatField.requestFocusInWindow();
    }

    /**
     * Set focus to the change name field.
     */
    public void focusChangeNameField() {
        newNameField.requestFocusInWindow();
    }

    /**
     * Some cool magic stuff.
     * @param name The name of someone. Maybe you?
     */
    public void setInvitePlayerName(String name) {
        if(name.length() <= 0) name = "someone";
        inviteButton.setText("Play against " + name);
        if(inviteButton.getText().equals("someone")) inviteButton.setEnabled(false);
        else if(name.equals(parent.getClient().getUsername())) {
            inviteButton.setText("This is you");
            inviteButton.setEnabled(false);
        } else if(name.matches("[^ ]+ – in game.*")) {
            inviteButton.setText("Already in game");
            inviteButton.setEnabled(false);
        } else if(!startGameButton.getText().startsWith("Stop")) {
            inviteButton.setEnabled(true);
        }
    }

    /**
     * Append a message without a sender to the message panel.
     * @param message The message to append.
     */
    public void writeMessage(String message) {
        if(chatArea.getText().length() <= 0) chatArea.append(message);
        else chatArea.append("\n" + message);
        if(message.startsWith("Loading")) parent.getConnectPanel().setMessage(message);
    }

    /**
     * Append a chat message to the message panel.
     * @param sender The name of the sender of this message.
     * @param message The message itself.
     */
    public void addChatMessage(String sender, String message) {
        if(chatArea.getText().length() <= 0) chatArea.append(sender + ": " + message);
        else chatArea.append("\n" + sender + ": " + message);
    }

    /**
     * Reset the fields and re-enable game-start-components.
     */
    public void resetLobby() {
        focusChatField();
        chatField.setText("");
        chatArea.setText("");
        newNameField.setText("");
        setGameStartComponentsEnabled(true);
    }

    /**
     * Invite a player to start a game.
     */
    public void invitePlayerToGame() {
        String opponent = usersList.getSelectedValue().replaceFirst(" – .*", "");
        if(opponent != null) {
            opponent = opponent.trim().toLowerCase();
            if(parent.getClient().checkName(opponent)) {
                startGameButton.setText("Waiting for answer");
                startGameButton.setEnabled(false);
                inviteButton.setText("Invited " + usersList.getSelectedValue().replaceFirst(" – .*", ""));
                inviteButton.setEnabled(false);
                parent.getClient().invitePlayer(opponent);
            } else {
                writeMessage(opponent + " is no valid name.");
            }
        }
    }

    /**
     * Log out.
     */
    private void logout() {
        parent.getClient().logout();
    }

    /**
     * Start a new game.
     */
    private void startGameOrStopWaiting() {
        if(startGameButton.getText().toLowerCase().startsWith("start")) {
            parent.getClient().startGame();
            focusChatField();
        } else {
            parent.getClient().stopWaitingForGame();
            focusChatField();
        }
    }

    /**
     * Change the name of the logged-in user.
     */
    public void changeName() {
        String newName = newNameField.getText().toLowerCase().trim();
        parent.getClient().changeName(newName);
        newNameField.setText("");
        focusChatField();
    }

    /**
     * Let the user to start a new game or not.
     * @param set Whether the user should be able to start a new game from the GUI.
     */
    public void setGameStartComponentsEnabled(boolean set) {
        if(usersList.getSelectedValue() != null) {
            setInvitePlayerName(usersList.getSelectedValue().replaceFirst(" – waiting.*", ""));
            inviteButton.setEnabled(true);
        } else {
            setInvitePlayerName("someone");
            inviteButton.setEnabled(false);

        }
        if(set) {
            startGameButton.setText("Start Game");
            startGameButton.setEnabled(true);
        } else {
            startGameButton.setText("Stop Waiting");
        }
    }

    // SETUP HELPERS

    private void prepareChatArea() {
        chatArea = new JTextArea();
        chatAreaPane = new JScrollPane(chatArea);
        chatAreaPane.setOpaque(false);
//        chatArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        chatArea.setEditable(false);
    }

    private void prepareChatPanel() {
        chatField = new JTextField();
        sendMessageButton = new JButton("Send");

        chatPanel = new JPanel();
    }

    private void prepareUsersAndGames() {
        gamesLabel = new JLabel("Games:");
        gamesLabel.setHorizontalAlignment(SwingConstants.LEFT);
        gamesListModel = new DefaultListModel<String>();
        gamesList = new JList<String>(gamesListModel);
        gamesList.setEnabled(false);
        gamesList.setLayoutOrientation(JList.VERTICAL);
        gamesList.setVisibleRowCount(-1);
        gamesListPane = new JScrollPane(gamesList);
        gamesListPane.setOpaque(false);

        usersLabel = new JLabel("Users online:");
        usersLabel.setHorizontalAlignment(SwingConstants.LEFT);
        usersListModel = new DefaultListModel<String>();
        usersList = new JList<String>(usersListModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersList.setLayoutOrientation(JList.VERTICAL);
        usersList.setVisibleRowCount(-1);
        usersListPane = new JScrollPane(usersList);
        usersListPane.setOpaque(false);

        startGameButton = new JButton("Start Game");
        inviteButton = new JButton();
        setInvitePlayerName("someone");

        usersPanel = new JPanel();
        gamesPanel = new JPanel();
        usersAndGamesPanel = new JPanel();
    }

    private void prepareHighscore() {
        highscoreLabel = new JLabel("Highscores:");
        highscoreLabel.setHorizontalAlignment(SwingConstants.LEFT);
        highscoreTableModel = new DefaultTableModel(new Object[0][SCORE_COLUMN_COUNT+1], new String[]{"Rank", "Name", "Wins", "Losses", "Ratio", "Scored", "Received", "Ratio", "Casualties"});
        highscoreTable = new JTable(highscoreTableModel);
        highscoreTable.setEnabled(false);

        highscorePane = new JScrollPane(highscoreTable);
        highscorePane.setOpaque(false);
    }

    private void prepareOtherControls() {
        newNameField = new JTextField(10);
        changeNameButton = new JButton("Change username");
        logoutButton = new JButton("Logout");

        otherControlsPanel = new JPanel();
    }

    private void prepareLayouts() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.X_AXIS));
        usersAndGamesPanel.setLayout(new BoxLayout(usersAndGamesPanel, BoxLayout.X_AXIS));
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        gamesPanel.setLayout(new BoxLayout(gamesPanel, BoxLayout.Y_AXIS));
        otherControlsPanel.setLayout(new BoxLayout(otherControlsPanel, BoxLayout.X_AXIS));
    }

    private void addComponents() {
        chatAreaPane.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));

        chatPanel.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));
        chatPanel.add(chatField);
        chatPanel.add(Box.createRigidArea(new Dimension(D, 0)));
        chatPanel.add(sendMessageButton);

        usersPanel.add(usersLabel);
        usersPanel.add(Box.createRigidArea(new Dimension(0, D)));
        usersPanel.add(usersListPane);
        usersPanel.add(Box.createRigidArea(new Dimension(0, D)));
        usersPanel.add(inviteButton);
        gamesPanel.add(gamesLabel);
        gamesPanel.add(Box.createRigidArea(new Dimension(0, D)));
        gamesPanel.add(gamesListPane);
        gamesPanel.add(Box.createRigidArea(new Dimension(0, D)));
        gamesPanel.add(startGameButton);

        highscoreLabel.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));
        highscorePane.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));

        usersAndGamesPanel.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));
        usersAndGamesPanel.add(usersPanel);
        usersAndGamesPanel.add(Box.createRigidArea(new Dimension(D, D)));
        usersAndGamesPanel.add(gamesPanel);

        otherControlsPanel.setBorder(BorderFactory.createEmptyBorder(D, D, D, D));
        otherControlsPanel.add(newNameField);
        otherControlsPanel.add(Box.createRigidArea(new Dimension(D, 0)));
        otherControlsPanel.add(changeNameButton);
        otherControlsPanel.add(Box.createHorizontalGlue());
        otherControlsPanel.add(logoutButton);

        add(chatAreaPane);
        add(chatPanel);
        add(usersAndGamesPanel);
        add(highscoreLabel);
        add(highscorePane);
        add(otherControlsPanel);
    }

    /**
     * Outsourced method for setting any handlers to avoid monolith constructor.
     */
    private void addListeners() {

        chatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoFilledChatRecipient = parent.sendMessage(chatField);
            }
        });
        chatField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE && autoFilledChatRecipient) chatField.setText("");
                if(e.getKeyCode() != KeyEvent.VK_ENTER) autoFilledChatRecipient = false;
            }
        });
        sendMessageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoFilledChatRecipient = parent.sendMessage(chatField);
            }
        });
        sendMessageButton.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                // submit form if enter was typed on logoutButton
                if(e.getKeyCode() == KeyEvent.VK_ENTER) autoFilledChatRecipient = parent.sendMessage(chatField);
            }
        });
        startGameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startGameOrStopWaiting();
            }
        });
        inviteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                invitePlayerToGame();
            }
        });
        usersList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if(!e.getValueIsAdjusting()) {
                    if(usersList.getSelectedValue() != null) {
                        setInvitePlayerName(usersList.getSelectedValue().replaceFirst(" – waiting.*", ""));
                    }
                }
            }
        });
        usersList.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER) invitePlayerToGame();
            }
        });
        changeNameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeName();
            }
        });
        logoutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });

        // listener for size changes
        addHierarchyBoundsListener(new HierarchyBoundsListener() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                if(e.getChanged() == getParent()) setSizes(e.getChanged().getSize());
            }
        });
    }

    // GETTERS & SETTERS

    public void addUserToList(String[] user) {
        switch(user.length) {
            case 1: // in lobby
                usersListModel.addElement(user[0]);
                break;
            case 2: // waiting for game
                usersListModel.addElement(user[0] + " – waiting for game");
                break;
            case 3: // in game
                usersListModel.addElement(user[0] + " – in game: " + user[1] + ":" + user[2]);
                break;
        }
        usersList.setSelectedIndex(lastSelectedUserIndex);
    }

    public void removeAllUsersFromList() {
        lastSelectedUserIndex = usersList.getSelectedIndex();
        usersListModel.removeAllElements();
    }

    public void addGameToList(String[] game) {
        switch(game.length) {
            case 1: // user waiting for game
                gamesListModel.addElement(game[0] + " is waiting for someone to join");
                break;
            case 4: // running game
                gamesListModel.addElement(game[0] + " is playing against " + game[1] + ". " + game[2] + ":" + game[3]);
                break;
        }
    }

    public void removeAllHighscoresFromTable() {
        for(int i = 0; i < highscoreTableModel.getRowCount(); i++) {
            highscoreTableModel.removeRow(0);
        }
        highscoreTableModel.setRowCount(0);
    }

    public void addHighscoreToTable(String[] score) {
        if(score.length == SCORE_COLUMN_COUNT) {
            String[] newData = new String[SCORE_COLUMN_COUNT+1];
            newData[0] = (highscoreTableModel.getRowCount()+1) + "";
            System.arraycopy(score, 0, newData, 1, SCORE_COLUMN_COUNT);
            highscoreTableModel.addRow(newData);
        }
    }

    public void removeAllGamesFromList() {
        gamesListModel.removeAllElements();
    }

}
