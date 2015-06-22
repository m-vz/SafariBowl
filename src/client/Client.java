package client;

import client.display.*;
import client.logic.ClientMessageProcessor;
import client.logic.ClientProtocolManager;
import client.logic.ClientSocketManager;
import client.logic.ClientMatch;
import gameLogic.GameController;
import network.*;
import util.SBApplication;
import util.SBLogger;

import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The main server application
 * Created by milan on 18.3.15.
 */
public class Client extends SBApplication {

    private static final SBLogger L = new SBLogger(Client.class.getName(), util.SBLogger.LOG_LEVEL);
    public static final Level LOG_OUTPUT_LEVEL = Level.INFO;

    private ClientSocketManager socketManager;
    private ClientProtocolManager protocolManager;
    private ClientMessageProcessor messageProcessor;
    private ClientFrame frame;
    private boolean loggedIn = false, playingMatch = false;
    private String username = "";
    private ClientMatch match = null;
    private int clientRole = -1;

    /**
     * The main method of the client.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        // run client
        Client client = new Client();
        client.runClient();

    }

    /**
     * Start the client application.
     */
    public void runClient() {
        // assign a UID to this client
        UID = UUID.randomUUID();
        L.log(Level.INFO, "My UID is " + UID);

        // prepare and show connect pane
        frame = new ClientFrame(this);
        frame.showConnectPanel("Loading teams...");
        frame.getConnectPanel().setControlsEnabled(false);
        frame.center();
        frame.setVisible(true);
        loadAvailableTeamsLocally(); // prepare team manager while loading is displayed
        frame.showConnectPanel("Connect to SafariBowl server");
        frame.getConnectPanel().setControlsEnabled(true);

        // prepare the message processor
        messageProcessor = new ClientMessageProcessor(this);

        // prepare the protocol and socket managers
        protocolManager = new ClientProtocolManager(this);
        socketManager = new ClientSocketManager(this, protocolManager);

        // read old games from file
        getLoggedGames(GameController.CLIENT_GAMES_FILE.getPath());

    }

    // MESSAGE & ANSWER PROCESSING

    /**
     * Tell the message processor to process any new incoming messages.
     */
    public void processMessages() {
        while(getProtocolManager() == null) { // sometimes accessing getProtocolManager() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } }
        while(getProtocolManager().getMessagesToProcess() == null) { // sometimes accessing getMessagesToProcess() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } }

        while(getProtocolManager().getMessagesToProcess().size() > 0) {
            SBProtocolMessage message = getProtocolManager().getNextMessageToProcessAndStoreIt();
            if(message.getSocket() != null) getMessageProcessor().processMessage(message);
            else L.log(Level.WARNING, "Message "+message.toStringShortenUUID()+" has no socket to return answers to. Dropping.");
        }
    }

    /**
     * Tell the answer processor to process any new incoming answers.
     */
    public void processAnswers() {
        while(getProtocolManager() == null) { // sometimes accessing getProtocolManager() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } }
        while(getProtocolManager().getAnswersToProcess() == null) { // sometimes accessing getAnswersToProcess() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); } }

        while(getProtocolManager().getAnswersToProcess().size() > 0) {
            SBProtocolMessage answer = getProtocolManager().getAnswersToProcess().poll();
            if(answer.getSocket() != null) getMessageProcessor().processAnswer(answer);
            else L.log(Level.WARNING, "Message "+answer.toStringShortenUUID()+" has no socket to return answers to. Dropping.");
        }
    }

    // ACTIONS

    /**
     * Connect client to address and port.
     * @param address The address to connect to.
     * @param port The port to connect to.
     */
    public void connect(String address, int port) {
        try {
            L.log(Level.INFO, "Connecting to "+address+" on port "+port+".");
            socketManager.startClient(InetAddress.getByName(address), port);
        } catch (Exception e) {
            L.log(Level.WARNING, "Could not connect to " + address + " on port " + port + ". "+e.toString());
            frame.showConnectPanel("Could not connect to server. Try again.");
            frame.getConnectPanel().focusAddressField();
        }
    }

    /**
     * Try to log a user in.
     * @param name The name of the user to log in.
     * @param password The encrypted password of the user to log in.
     */
    public void login(String name, String password) {
        if(name.length() > 0) {
            getMessageProcessor().loggedInWithUsername = name;
            getMessageProcessor().loggedInWithEncryptedPassword = password;
            socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.LOGIN, new SBProtocolParameterArray(name, password)));
        }
    }

    /**
     * Send a chat message to another user or to all users.
     * @param recipientName The name of the user to send the message to. If this is 'all', the message is sent as a broadcast message to everyone.
     * @param message The message to send.
     */
    public void chat(String recipientName, String message) {
        if(recipientName.equals("all")) socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.BDCST, new SBProtocolParameterArray(username, message)));
        else socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.SENDM, new SBProtocolParameterArray(recipientName, message)));
        L.log(Level.INFO, "Sent message to " + recipientName + ": " + message);
    }

    /**
     * Send a game message to the server.
     * @param message The message to send.
     */
    public void sendGameMessage(SBProtocolMessage message) {
        log(Level.FINER, "   Sending game message "
                + message.getMID().toString().substring(0, 5)
                + ": " + message.getParameters().toStringUnescaped());
        socketManager.sendMessage(message);
    }

    /**
     * Send a game answer to the server.
     * @param beginningOfMID The beginning of the MID of the message to answer to.
     * @param answerType WORKD or FAILD
     * @param answerParams The params to pass with the answer.
     */
    public void sendGameAnswer(String beginningOfMID, SBProtocolCommand answerType, SBProtocolParameterArray answerParams) {
        SBProtocolMessage messageToRemove = null;
        for(SBProtocolMessage message: getProtocolManager().getProcessedMessages()) {
            if(message.getMID().toString().startsWith(beginningOfMID)) {
                messageToRemove = message;
                log(Level.INFO, "Sending game answer for "
                        + message.getMID().toString().substring(0, 5)
                        + ": " + answerParams.toStringUnescaped());
                if(answerType == SBProtocolCommand.FAILD) message.returnFailureMessage(getUID(), answerParams);
                else message.returnSuccessMessage(getUID(), answerParams);
                break;
            }
        }
        if(messageToRemove == null) {
            log(Level.WARNING, "Message with MID beginning with "+beginningOfMID+" not found.");
        }
        getProtocolManager().removeUnansweredMessage(messageToRemove);
    }

    /**
     * Surrender to your opponent.
     */
    public void surrender() {
        if(isPlayingMatch()) socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.SRNDR));
        else log(Level.WARNING, "Client is not in game. Won't surrender!");
    }

    /**
     * End your turn.
     */
    public void endTurn() {
        if(isPlayingMatch()) socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.EVENT, new SBProtocolParameterArray(SBProtocolMessage.EVENT_END_TURN)));
        else log(Level.WARNING, "Client is not in game. Cannot end turn.");
    }

    /**
     * Set up team in a cool lineup
     * @param side >0 = right side, <0 = left side.
     */
    public void coolLineup(int side) {
        if(isPlayingMatch()) {
            getMatch().settingCoolLayout = side;
            int posX = -1, posY = -1;
            if(side > 0) {
                if(side > 10) getMatch().settingCoolLayout = 0;
                else {
                    switch (side) {
                        case 1:
                            posX = 13; posY = 6; break;
                        case 2:
                            posX = 13; posY = 7; break;
                        case 3:
                            posX = 13; posY = 8; break;
                        case 4:
                            posX = 14; posY = 5; break;
                        case 5:
                            posX = 14; posY = 9; break;
                        case 6:
                            posX = 15; posY = 3; break;
                        case 7:
                            posX = 15; posY = 11; break;
                        case 8:
                            posX = 17; posY = 6; break;
                        case 9:
                            posX = 17; posY = 8; break;
                        case 10:
                            posX = 20; posY = 7; break;
                    }
                    side -= 1;
                }
            } else if(side < 0) {
                if(side < -10) getMatch().settingCoolLayout = 0;
                else {
                    switch (side) {
                        case -1:
                            posX = 12; posY = 6; break;
                        case -2:
                            posX = 12; posY = 7; break;
                        case -3:
                            posX = 12; posY = 8; break;
                        case -4:
                            posX = 11; posY = 5; break;
                        case -5:
                            posX = 11; posY = 9; break;
                        case -6:
                            posX = 10; posY = 3; break;
                        case -7:
                            posX = 10; posY = 11; break;
                        case -8:
                            posX = 8; posY = 6; break;
                        case -9:
                            posX = 8; posY = 8; break;
                        case -10:
                            posX = 5; posY = 7; break;
                    }
                    side += 1;
                }
            }
            if(posX >= 0 && posY >= 0) socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.ACTIO, new SBProtocolParameterArray("SET PLAYER", Math.abs(side)+"", posX+"", posY+"")));
        }
    }

    /**
     * Tell the server to put this client on a waiting list for a new game against a random opponent.
     */
    public void startGame() {
        if(!isPlayingMatch()) { // only start a new game if not currently in another game
            socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.START, new SBProtocolParameterArray()));
            getMessageProcessor().waitingForGame = true;
            getMessageProcessor().invitedOpponent = "";

            getFrame().getLobbyPanel().setGameStartComponentsEnabled(false);
            getFrame().getLobbyPanel().writeMessage("Waiting for a random coach to lose against you.");
            L.log(Level.INFO, "Waiting for a random coach to lose against you.");
        } else {
            L.log(Level.WARNING, "Tried to start a new game while being in another game currently. Ignoring attempt.");
            startingGameFailed(false);
        }
    }

    public void stopWaitingForGame() {
        socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.START, new SBProtocolParameterArray()));
        getMessageProcessor().waitingForGame = false;
        getMessageProcessor().invitedOpponent = "";

        getFrame().getLobbyPanel().setGameStartComponentsEnabled(true);
        getFrame().getLobbyPanel().writeMessage("Stopped waiting for someone to join your game.");
        L.log(Level.INFO, "Stopped waiting for someone to join your game.");
    }

    /**
     * Tell the server to invite another player to play against this client.
     * @param opponent The name of the opponent to invite.
     */
    public void invitePlayer(String opponent) {
        if(!isPlayingMatch()) { // only start a new game if not currently in another game
            socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.START, new SBProtocolParameterArray(opponent)));
            getMessageProcessor().invitedOpponent = opponent;
            setPlayingMatch(true); // set playing so other invitations are ignored

            getFrame().getLobbyPanel().writeMessage("Inviting " + opponent + " to lose against you.");
            L.log(Level.INFO, "Inviting " + opponent + " to lose against you.");
        } else {
            L.log(Level.WARNING, "Tried to start a new game while being in another game currently. Ignoring attempt.");
            startingGameFailed(false);
        }
    }

    /**
     * Start a match against an opponent.
     * @param opponent The name of the opponent to play against.
     */
    public void createGame(String opponent) {
        clientRole = 0;                                                             // 0: was invited and accepted
        if(opponent.equals(getMessageProcessor().invitedOpponent)) clientRole = 1;  // 1: invited other player
        if(getMessageProcessor().waitingForGame) clientRole = 2;                    // 2: was waiting for game

        String creationMessage = "Starting match against "+opponent+".";
        if(clientRole == 1) creationMessage = opponent+" accepted invitation. Starting match.";
        L.log(Level.INFO, creationMessage);

        getMessageProcessor().waitingForGame = false;
        getMessageProcessor().invitedOpponent = "";
        getFrame().getLobbyPanel().setGameStartComponentsEnabled(true);
        if(clientRole == 0 || clientRole == 2) setMatch(new ClientMatch(this, opponent, getUsername())); // user was invited or was waiting for game therefore is second user
        else setMatch(new ClientMatch(this, getUsername(), opponent));
        setPlayingMatch(true);

        getFrame().writeMessage(creationMessage);
        getFrame().showGamePanel(true);
        getFrame().getGamePanel().setPitchOnCanvas(getMatch().getPitch());

    }

    /**
     * Notifies the user that starting a game has failed.
     * @param resetPlayingMatch Whether the playingMatch-field should be reset to false. (Which it shouldn't if starting a new match failed because the client was already in a game)
     */
    public void startingGameFailed(boolean resetPlayingMatch) {
        String failureMessage = "Failed to start a game.";
        if(getMessageProcessor().invitedOpponent.length() > 0) failureMessage = "Failed to start a game against "+getMessageProcessor().invitedOpponent+".";

        getMessageProcessor().waitingForGame = false;
        getMessageProcessor().invitedOpponent = "";
        if(resetPlayingMatch) setPlayingMatch(false);
        getFrame().getLobbyPanel().setGameStartComponentsEnabled(true);

        L.log(Level.WARNING, failureMessage);
        getFrame().getLobbyPanel().writeMessage(failureMessage);
    }

    /**
     * Log out this client.
     */
    public void logout() {
        socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.LOGUT, new SBProtocolParameterArray()));
    }

    /**
     * A game has finished. Update UI.
     */
    public void finishedGame() {
        if(isPlayingMatch() && getMatch() != null) {
            if(getMatch().getWinnerString().equals(getUsername()))
                getFrame().showLobbyPanel("Won match against " + match.getOpponentString(getUsername()) + "!");
            else if(getMatch().getWinnerString().equals(""))
            	getFrame().showLobbyPanel("Tied against " + match.getOpponentString(getUsername()) + "!");
            else
            	getFrame().showLobbyPanel("Lost match against " + match.getOpponentString(getUsername()) + "!");
            int clientTeamIndex = match.getOpponentString(0).equals(getUsername()) ? 0 : 1;
//            getGameFrame().writeMessage("Stats:");
            getFrame().writeMessage("Final score: " + match.getScoreFromTeam(clientTeamIndex) + ":" + match.getScoreFromTeam(clientTeamIndex==1?0:1));

            setMatch(null);
            setPlayingMatch(false);
        }
    }

    /**
     * Change the name of the user this client is logged in as.
     * @param newName The name to change to.
     */
    public void changeName(String newName) {
        if(username.length() > 0) {
            getMessageProcessor().potentialNewUsername = newName; // store new name until name change is confirmed
            socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.CHNGE, new SBProtocolParameterArray(username, newName)));
        }
    }

    /**
     * This is run whenever this client looses connection to the server.
     */
    public void lostConnection() {
        setLoggedIn(false);
        getFrame().showConnectPanel("Lost connection. Connect again.");
    }

    /**
     * This is run whenever this client tries to connect to a server.
     */
    public void isConnecting() {
        getFrame().showConnectPanel("Connecting...");
    }

    /**
     * This is run whenever this client has been connected to a server or a connection attempt has failed.
     * @param address The address of the server this client has tried to connecte to.
     * @param port The port of the server this client has tried to connecte to.
     * @param connected Whether the connection attempt was successful.
     */
    public void hasConnected(InetAddress address, int port, boolean connected) {
        if (connected) {
            L.log(Level.INFO, "Successfully connected to " + address + " on port " + port + ".");
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            getSocketManager().startConnectionListener();
            frame.showLoginPanel("Connected to server. Log in.");
            if(autologin) login("milan", "30f6bee6387a90c504c0116b968db626"); // logs in automatically to speed up testing
        } else {
            L.log(Level.WARNING, "Could not connect to " + address + " on port " + port + ".");
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            frame.showConnectPanel("Could not connect to server. Try again.");
            frame.getConnectPanel().focusAddressField();
        }
    }

    /**
     * Request a list with all logged-in users from the server.
     */
    public void getUsersList() {
        socketManager.sendMessage(new SBProtocolMessage(UID, SBProtocolCommand.LSUSR, new SBProtocolParameterArray()));
    }

    /**
     * Request a list with all running and not running games from the server.
     */
    public void getGamesList() {
        socketManager.sendMessage(new SBProtocolMessage(UID, SBProtocolCommand.LSGAM, new SBProtocolParameterArray()));
    }

    public void updateUsersList(SBProtocolParameterArray users) {
        String[][] usersString = new String[users.size()][];
        for(int i = 0; i < users.size(); i++) {
            SBProtocolParameterArray user = users.getParameter(i).toArray();
            if(user.size() == 1)
                usersString[i] = new String[]{user.getParameter(0).getContent()};
            else if(user.size() == 2)
                usersString[i] = new String[]{user.getParameter(0).getContent(),
                                              user.getParameter(1).getContent()};
            else if(user.size() == 3)
                usersString[i] = new String[]{user.getParameter(0).getContent(),
                                              user.getParameter(1).getContent(),
                                              user.getParameter(2).getContent()};
        }
        getFrame().updateUsersList(usersString);
    }

    public void updateGamesList(SBProtocolParameterArray games) {
        String[][] gamesString = new String[games.size()][];
        for(int i = 0; i < games.size(); i++) {
            SBProtocolParameterArray game = games.getParameter(i).toArray();
            if(game.size() == 1)
                gamesString[i] = new String[]{game.getParameter(0).getContent()};
            else if(game.size() == 4)
                gamesString[i] = new String[]{game.getParameter(0).getContent(),
                                              game.getParameter(1).getContent(),
                                              game.getParameter(2).getContent(),
                                              game.getParameter(3).getContent()};
        }
        getFrame().updateGamesList(gamesString);
    }

    public void updateHighscoreTable(SBProtocolParameterArray scores) {
        String[][] scoresString = new String[scores.size()][];
        for(int i = 0; i < scores.size(); i++) {
            SBProtocolParameterArray score = scores.getParameter(i).toArray();
            if(score.size() == LobbyPanel.SCORE_COLUMN_COUNT) {
                String[] scoreString = new String[LobbyPanel.SCORE_COLUMN_COUNT];
                for(int j = 0; j < LobbyPanel.SCORE_COLUMN_COUNT; j++)
                    scoreString[j] = score.getParameter(j).getContent();
                scoresString[i] = scoreString;
            }
        }
        getFrame().updateHighscoreList(scoresString);
    }

    // HELPERS

    public void log(Level level, String message) {
        if(getFrame() != null) {
            if(getFrame().getLobbyPanel() != null)
                if(level.intValue() >= LOG_OUTPUT_LEVEL.intValue()) getFrame().getLobbyPanel().writeMessage(message);
            if(getFrame().getGamePanel() != null)
                if(level.intValue() >= LOG_OUTPUT_LEVEL.intValue()) getFrame().getGamePanel().writeMessage(message);
        }
        if(!message.endsWith("....")) L.log(level, message);
    }

    /**
     * Checks whether a name is allowed. (contains no spaces, longer than 0, not 'all').
     * @param name The name to be checked.
     * @return Whether the name is allowed.
     */
    public boolean checkName(String name) {
        return !name.contains(" ") && name.length() > 0 && !name.toLowerCase().equals("all");
    }

    /**
     * Return a success answer for a received message.
     * @param returnTo The message to return an answer to.
     * @param parameters The parameters to send with the answer.
     */
    public void returnSuccessMessage(SBProtocolMessage returnTo, String... parameters) {
        returnTo.returnSuccessMessage(UID, new SBProtocolParameterArray(parameters));
    }

    /**
     * Return a success answer for a received message.
     * @param returnTo The message to return an answer to.
     * @param parameters The parameters to send with the answer.
     */
    public void returnFailureMessage(SBProtocolMessage returnTo, String... parameters) {
        returnTo.returnFailureMessage(UID, new SBProtocolParameterArray(parameters));
    }

    /**
     * Checks on the server whether a user exists.
     * @param name The name to check.
     * @return Whether the user with the given name exists.
     */
    public boolean checkIfUserExists(String name) {
        getMessageProcessor().setUserExists(-1);
        socketManager.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.EXIST, new SBProtocolParameterArray(name)));
        while(getMessageProcessor().getUserExists() < 0) {
            if(socketManager.getSocket() != null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        int exists = getMessageProcessor().getUserExists();
        getMessageProcessor().setUserExists(-1);
        if(exists == 0) return false;
        else if(exists == 1) return true;
        else {
            L.log(Level.SEVERE, "Error while looking if user exists.");
            return true;
        }
    }

    /**
     * On the client this method just sets this client as logged-out.
     */
    public void logOutAllUsers() {
        setLoggedIn(false);
    }

    // GETTERS & SETTERS

    public ClientMessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    public UUID getUID() {
        return UID;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public SBSocketManager getSocketManager() {
        return socketManager;
    }

    public ClientProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public ClientFrame getFrame() {
        return frame;
    }

    public boolean isPlayingMatch() {
        return playingMatch;
    }

    public void setPlayingMatch(boolean playingMatch) {
        this.playingMatch = playingMatch;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public ClientMatch getMatch() {
        return match;
    }

    public void setMatch(ClientMatch match) {
        this.match = match;
    }

    public int getClientRole() {
        return clientRole;
    }

    // UNUSED

    public void logOutUserWithUID(UUID UID) {}
}
