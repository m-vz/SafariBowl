package client.display;

import GUI.SBFrame;
import client.Client;
import gameLogic.Player;
import gameLogic.Team;
import util.ResourceManager;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * The main frame for the main client application.
 * Created by milan on 23.3.15.
 */
public class ClientFrame extends SBFrame {
    private static final String MAIN_TITLE = "SafariBowl",
                                CONNECT_TITLE = "Connect to a SafariBowl game server",
                                LOGIN_TITLE = "Log in to SafariBowl or create a new account",
                                LOBBY_TITLE = "Lobby",
                                GAME_TITLE = "Match";
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private ConnectPanel connectPanel;
    private LoginPanel loginPanel;
    private LobbyPanel lobbyPanel;
    private GameFrame gameFrame;
    private Client parent;
    public boolean wasPutToFront;

    /**
     * Create a new client frame with a parent client.
     * @param parent The client that is parent of this frame.
     */
    public ClientFrame(Client parent) {
        super(MAIN_TITLE);
        setUp(parent);
    }

    /**
     * Create a new client frame with a parent client and a frame title.
     * @param parent The client that is parent of this frame.
     * @param title The title for the frame.
     */
    public ClientFrame(Client parent, String title) {
        super(title);
        setUp(parent);
    }

    /**
     * Set the frame up.
     * @param parent The client that is parent of this frame.
     */
    private void setUp(Client parent) {
        this.parent = parent;

        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // create panels
        connectPanel = new ConnectPanel(this);
        loginPanel = new LoginPanel(this);
        lobbyPanel = new LobbyPanel(this);

        // add panels
        mainPanel.add(CONNECT_TITLE, connectPanel);
        mainPanel.add(LOGIN_TITLE, loginPanel);
        mainPanel.add(LOBBY_TITLE, lobbyPanel);
        cardLayout.addLayoutComponent(connectPanel, CONNECT_TITLE);
        cardLayout.addLayoutComponent(loginPanel, LOGIN_TITLE);
        cardLayout.addLayoutComponent(lobbyPanel, LOBBY_TITLE);
        add(mainPanel);
    }

    // PANELS

    /**
     * Show the connect panel.
     * @param message The message to display on the panel.
     */
    public void showConnectPanel(String message) {
        if(gameFrame != null) {
            gameFrame.exitFullscreen();
            gameFrame.setVisible(false);
        }
        setMinimumSize(new Dimension(0, 0));
        setSize(new Dimension(350, 200));
        setResizable(false);
        connectPanel.setMessage(message);
        cardLayout.show(mainPanel, CONNECT_TITLE);
        connectPanel.focusAddressField();
        setVisible(true);
    }

    /**
     * Show the login panel.
     * @param message The message to display on the panel.
     */
    public void showLoginPanel(String message) {
        if(gameFrame != null) {
            gameFrame.exitFullscreen();
            gameFrame.setVisible(false);
        }
        setMinimumSize(new Dimension(0, 0));
        setSize(new Dimension(350, 200));
        setResizable(false);
        loginPanel.setMessage(message);
        cardLayout.show(mainPanel, LOGIN_TITLE);
        loginPanel.focusNameField();
        setVisible(true);
    }

    /**
     * Show the lobby panel.
     * @param message The message to display on the panel.
     */
    public void showLobbyPanel(String message) {
        if(gameFrame != null) {
            gameFrame.exitFullscreen();
            gameFrame.setVisible(false);
        }
        setSize(new Dimension(749, 750));
        setResizable(true);
        writeMessage(message);
        cardLayout.show(mainPanel, LOBBY_TITLE);
        center();
        lobbyPanel.focusChatField();
        lobbyPanel.setInvitePlayerName("someone");
        lobbyPanel.setGameStartComponentsEnabled(true);
        setMinimumSize(new Dimension(400, 800));
        setVisible(true);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                lobbyPanel.setSizes(getSize());
                try {
                    Thread.sleep(50);
                } catch(InterruptedException ignored) {}
                setSize(new Dimension(750, 850));
            }
        });
    }

    /**
     * Show the game panel.
     */
    public void showGamePanel(boolean newGame) {
        setMinimumSize(new Dimension(0, 0));
        if(newGame) {
            gameFrame = new GameFrame(this);
            gameFrame.setSize(new Dimension(16 * 100 - 1, 10 * 100));
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    gameFrame.setSizes(gameFrame.getSize(), true);
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException ignored) {}
                    gameFrame.setSize(new Dimension(16 * 100, 10 * 100));
                }
            });
        }
        if(gameFrame != null) {
            setVisible(false);
            gameFrame.setVisible(true);
        } else getClient().log(Level.WARNING, "Tried to go back into game panel without having been in it before.");
    }

    // HELPERS

    /**
     * Send a chat message to some- or everybody.
     * @param chatField The field where the message is stored.
     * @return Whether the chat recipient was auto-filled.
     */
    public boolean sendMessage(JTextField chatField) {
        String message, recipient;
        String text = chatField.getText();
        if(chatField.getText().startsWith("@")) {
            // "@milan hello there!" -> message = "hello there!" recipient = "milan"
            message = text.replaceAll("^@\\S+", "");
            if(message.length() > 1) {
                message = message.substring(1, message.length());
                recipient = text.substring(1, text.length() - message.length() - 1);
            }
            else return false; // don't send if message was empty
        } else {
            // check if it is a command
            if(text.toLowerCase().equals("/games") || text.toLowerCase().equals("/g")) { // get games list command
                writeMessage("Requesting games list...");
                getClient().log(Level.FINE, "Requesting games list.");
                getClient().getGamesList();
                chatField.setText("");
                return false;
            } else if(text.toLowerCase().equals("/users") || text.toLowerCase().equals("/u")) { // get users list command
                writeMessage("Requesting list of users online...");
                getClient().log(Level.FINE, "Requesting list of users online.");
                getClient().getUsersList();
                chatField.setText("");
                return false;
            } else if(text.toLowerCase().equals("/full") && getClient().isPlayingMatch()) { // toggle fullscreen
                writeMessage("Toggling fullscreen");
                getClient().log(Level.FINE, "Toggling fullscreen.");
                gameFrame.toggleFullscreen();
                return false;
            } else if(text.toLowerCase().equals("/arschloch") && getClient().isPlayingMatch()) { // toggle fullscreen
                writeMessage("Al Capone: Fuck off.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.exit(-1);
                }
                System.exit(0);
                return false;
            } else if(text.toLowerCase().equals("/teams") && getClient().isPlayingMatch()) { // get list of available teams
                writeMessage("Available teams:");
                for(Team availableTeam: parent.getTeamManager().getTeamBlueprints()) writeMessage(availableTeam.getType());
                chatField.setText("");
                return false;
            } else if(text.toLowerCase().startsWith("/players ") && getClient().isPlayingMatch()) { // get list of available players in team
                String teamName = text.substring("/players ".length());
                for(Team availableTeam: parent.getTeamManager().getTeamBlueprints()) {
                    if(availableTeam.getType().equalsIgnoreCase(teamName)) {
                        writeMessage("Available players in team " + teamName + ":");
                        for(Player availablePlayer: availableTeam.getAvailablePlayers()) writeMessage(availablePlayer.getName());
                        chatField.setText("");
                        return false;
                    }
                }
                writeMessage("Team " + teamName + " does not exist.");
                return false;
            } else {
                message = text;
                recipient = "all";
            }
        }
        addChatMessage("@"+recipient.trim(), message);
        getClient().chat(recipient.toLowerCase().trim(), message);
        if(!recipient.equals("all")) {
            chatField.setText("@"+recipient+" ");
            return true;
        } else {
            chatField.setText("");
            return false;
        }
    }

    /**
     * Reset the lobby panel. (Clear fields, etc.)
     */
    public void resetLobbyPanel() {
        lobbyPanel.resetLobby();
    }

    public void updateUsersList(String[][] users) {
        if(lobbyPanel != null) {
            lobbyPanel.removeAllUsersFromList();
            for(String[] user: users)
                lobbyPanel.addUserToList(user);
        }
    }

    public void updateGamesList(String[][] games) {
        if(lobbyPanel != null) {
            lobbyPanel.removeAllGamesFromList();
            for(String[] game: games)
                lobbyPanel.addGameToList(game);
        }
    }

    public void updateHighscoreList(String[][] scores) {
        if(lobbyPanel != null) {
            lobbyPanel.removeAllHighscoresFromTable();
            for(String[] score: scores) {
                lobbyPanel.addHighscoreToTable(score);
            }
        }
    }

    /**
     * Reset the game panel. (Clear fields, etc.)
     */
    public void resetGamePanel() {
        getGamePanel().resetGamePanel();
    }

    /**
     * Prompt the client to accept a game invitation.
     * @param userInviting The name of the user inviting this client.
     * @return Whether the client accepted the invitation.
     */
    public boolean getInvitedAnswer(String userInviting) {
        return JOptionPane.showConfirmDialog(this,
                userInviting.substring(0, 1).toUpperCase() + userInviting.substring(1) + " and team invited you to a game of SafariBowl.\nAccept their invitation and show them who's the real walrus here!",
                "Game invitation from " + userInviting,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new ImageIcon(ResourceManager.IMAGE_MODERATOR_WALRUS)) == 0;
    }

    /**
     * Prompt the client to confirm surrender.
     * @return Whether the client confirmed surrender. If the client is not in a game, this will return false.
     */
    public boolean getSurrenderAnswer() {
        JFrame frame = gameFrame.isFullscreen() ? gameFrame.getFullscreenFrame() : gameFrame;
        return parent.isPlayingMatch()
                && JOptionPane.showInternalConfirmDialog(frame.getContentPane(),
                    "Do you really want to surrender?",
                    "Surrender?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new ImageIcon(ResourceManager.IMAGE_MODERATOR_TURTLE)) == 0;
    }

    /**
     * Write a chat message to both the lobby and the game panel.
     * @param sender The sender of the message.
     * @param message The message to write.
     */
    public void addChatMessage(String sender, String message) {
        getLobbyPanel().addChatMessage(sender, message);
        if(getGamePanel() != null) getGamePanel().addChatMessage(sender, message);
    }

    /**
     * Write a message to both the lobby and the game panel.
     * @param message The message to write.
     */
    public void writeMessage(String message) {
        getLobbyPanel().writeMessage(message);
        if(getGamePanel() != null) getGamePanel().writeMessage(message);
    }

    // GETTERS & SETTERS

    /**
     * Get the parent client.
     * @return The client that is parent of this frame.
     */
    public Client getClient() {
        return parent;
    }

    /**
     * Get the connect panel.
     * @return The connect panel of this frame.
     */
    public ConnectPanel getConnectPanel() {
        return connectPanel;
    }

    /**
     * Get the login panel.
     * @return The login panel of this frame.
     */
    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    /**
     * Get the lobby panel.
     * @return The lobby panel of this frame.
     */
    public LobbyPanel getLobbyPanel() {
        return lobbyPanel;
    }

    /**
     * Get the game panel.
     * @return The game panel of this frame.
     */
    public GamePanel getGamePanel() {
        return gameFrame == null ? null : gameFrame.getGamePanel();
    }
}
