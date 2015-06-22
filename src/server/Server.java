package server;

import gameLogic.GameController;
import server.logic.*;
import network.*;
import server.display.*;
import util.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

/**
 * The main server application
 * Created by milan on 18.3.15.
 */
public class Server extends SBApplication {
	
    private static final SBLogger L = new SBLogger(Server.class.getName(), util.SBLogger.LOG_LEVEL);

    public static final int DEFAULT_PORT = 9989, TIME_AFTER_CONNECTION_LOST_BEFORE_AUTOSURRENDER = 40*1000;
    public static final Level LOG_OUTPUT_LEVEL = Level.INFO;

    private Vector<User> usersWaitingForRandomGame = new Vector<User>();
    private Vector<ServerMatch> runningMatches = new Vector<ServerMatch>();
    private ServerSocketManager socketManager;
    private ServerProtocolManager protocolManager;
    private ServerFrame frame;
    private UserManager userManager;
    private ServerMessageProcessor messageProcessor;

    /**
     * The main method of the server.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {

        Server server = new Server();
        server.runServer();

    }

    /**
     * Start the server application
     */
    public void runServer() {
        // prepare and show server frame
        frame = new ServerFrame(this);
        frame.showServerPanel();
        frame.setVisible(true);
        frame.getServerPanel().setControlsEnabled(false);

        // prepare team manager
        loadAvailableTeamsLocally();
        frame.getServerPanel().setControlsEnabled(true);

        // prepare the message processor
        messageProcessor = new ServerMessageProcessor(this);

        // prepare the protocol and socket managers
        protocolManager = new ServerProtocolManager(this);
        socketManager = new ServerSocketManager(this, protocolManager);

        // prepare the user manager
        File userDatabase = new File("data/users");
        boolean madeDir = userDatabase.getParentFile().mkdirs();
        try {
            if (!userDatabase.exists())
                if(!userDatabase.createNewFile()) throw new SBException();
            userManager = new UserManager(this, userDatabase);
        } catch (Exception e) {
            log(Level.SEVERE, "Couldn't create user manager. Quitting.");
            logStackTrace(e);
            for (int i = 0; i < e.getStackTrace().length; i++) {
                L.log(Level.SEVERE, e.getStackTrace()[i].toString());
            }
            System.exit(-1);
        }

        // read old games from file
        getLoggedGames(GameController.SERVER_GAMES_FILE.getPath());

        // prepare games and users updater
        (new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                broadcastUpdatedGamesList();
            }
        })).start();
    }

    // MESSAGE & ANSWER PROCESSING

    /**
     * Tell the message processor to process any new incoming messages.
     */
    public void processMessages() {
        while (getProtocolManager() == null) // sometimes accessing getProtocolManager() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { logStackTrace(e); }
        while (getProtocolManager().getMessagesToProcess() == null) // sometimes accessing getMessagesToProcess() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { logStackTrace(e); }

        while (getProtocolManager().getMessagesToProcess().size() > 0) {
            SBProtocolMessage message = getProtocolManager().getNextMessageToProcessAndStoreIt();
            if (message.getSocket() != null) getMessageProcessor().processMessage(message);
            else log(Level.WARNING, "Message " + message.toStringShortenUUID() + " has no socket to return answers to. Dropping.");
        }
    }

    /**
     * Tell the answer processor to process any new incoming answers.
     */
    public void processAnswers() {
        while(getProtocolManager() == null) { // sometimes accessing getProtocolManager() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { logStackTrace(e); } }
        while(getProtocolManager().getAnswersToProcess() == null) { // sometimes accessing getAnswersToProcess() throws a mysterious null pointer exception. Wait until it can't anymore
            try { Thread.sleep(1); } catch (InterruptedException e) { logStackTrace(e); } }

        while(getProtocolManager().getAnswersToProcess().size() > 0) {
            SBProtocolMessage answer = getProtocolManager().getAnswersToProcess().poll(); // get message
            if(answer.getSocket() != null) getMessageProcessor().processAnswer(answer); // process
            else log(Level.WARNING, "Message "+answer.toStringShortenUUID()+" has no socket to return answers to. Dropping.");
        }
    }

    // ACTIONS

    public void broadcastUpdatedGamesList() {
        SBProtocolParameterArray gamesParameters = new SBProtocolParameterArray();

        // add running matches
        for(ServerMatch match: getRunningMatches())
            if(match.isRunning())
                gamesParameters.addParameter(new SBProtocolParameterArray(match.getUser(0).getName(), match.getUser(1).getName(), match.getScoreFromTeam(0)+"", match.getScoreFromTeam(1)+""));
        // add not yet running matches
        for(User user: getUsersWaitingForRandomGame())
            gamesParameters.addParameter(new SBProtocolParameterArray(user.getName()));

        sendBroadcastMessage(SBProtocolCommand.UPGAM, gamesParameters);
        broadcastUpdatedUsersList();
        broadcastUpdatedHighscoresList();
    }

    public void broadcastUpdatedUsersList() {
        SBProtocolParameterArray usersParameters = new SBProtocolParameterArray();

        for(User user: getUserManager().getUsers()) {
            if(user.isLoggedIn()) {
                SBProtocolParameterArray userParameters = new SBProtocolParameterArray();
                userParameters.addParameter(new SBProtocolParameter(user.getName()));
                if(getUsersWaitingForRandomGame().contains(user))
                    userParameters.addParameter(new SBProtocolParameter(""));
                else if(user.isInGame()) {
                    ServerMatch match = null;
                    int userIndex = 0;
                    for(ServerMatch potentialMatchWithUserInIt: getRunningMatches()) {
                        if(potentialMatchWithUserInIt.getUser(0).equals(user)) {
                            match = potentialMatchWithUserInIt;
                            userIndex = 0;
                            break;
                        } else if(potentialMatchWithUserInIt.getUser(1).equals(user)) {
                            match = potentialMatchWithUserInIt;
                            userIndex = 1;
                            break;
                        }
                    }
                    if(match != null) {
                        userParameters.addParameter(new SBProtocolParameter(match.getScoreFromTeam(userIndex) + ""));
                        userParameters.addParameter(new SBProtocolParameter(match.getScoreFromTeam(userIndex == 0 ? 1 : 0) + ""));
                    }
                }
                usersParameters.addParameter(userParameters);
            }
        }

        sendBroadcastMessage(SBProtocolCommand.UPUSR, usersParameters);
    }

    public void broadcastUpdatedHighscoresList() {
        SBProtocolParameterArray scoresParameters = new SBProtocolParameterArray(),
                                 scoreParameters;
        //noinspection unchecked
        Vector<User> usersTemp = (Vector<User>) getUserManager().getUsers().clone();
        DecimalFormat format = new DecimalFormat("0.###");
        double max;
        int maxIndex;

        while(scoresParameters.size() < getUserManager().getUsers().size()) {
            max = maxIndex = -1;

            for(int i = 0; i < usersTemp.size(); i++) {
                User user = usersTemp.get(i);
                double ratio = (double)user.getWins()/(user.getLosses() == 0 ? 1 : user.getLosses());
                if(ratio > max) {
                    maxIndex = i;
                    max = ratio;
                }
            }

            User user = usersTemp.get(maxIndex);
            usersTemp.remove(maxIndex);
            scoreParameters = new SBProtocolParameterArray();
            scoreParameters.addParameter(new SBProtocolParameter(user.getName()));
            scoreParameters.addParameter(new SBProtocolParameter(user.getWins()+""));
            scoreParameters.addParameter(new SBProtocolParameter(user.getLosses()+""));
            scoreParameters.addParameter(new SBProtocolParameter(format.format((double)user.getWins()/(user.getLosses() == 0 ? 1 : user.getLosses()))+""));
            scoreParameters.addParameter(new SBProtocolParameter(user.getTouchdownsScored()+""));
            scoreParameters.addParameter(new SBProtocolParameter(user.getTouchdownsReceived()+""));
            scoreParameters.addParameter(new SBProtocolParameter(format.format((double)user.getTouchdownsScored()/(user.getTouchdownsReceived() == 0 ? 1 : user.getTouchdownsReceived()))+""));
            scoreParameters.addParameter(new SBProtocolParameter(user.getCasualties()+""));
            scoresParameters.addParameter(scoreParameters);
        }

        sendBroadcastMessage(SBProtocolCommand.SCORE, scoresParameters);
    }

    /**
     * Start listening on port.
     * @param port The port to set the server socket to listen to.
     */
    public void start(int port) {
        try {
            log(Level.INFO, "Starting server on port " + port + ".");
            // assign a UID to this server
            UID = UUID.randomUUID();
            log(Level.INFO, "My UID is " + UID);
            // start server
            socketManager.startServer(port);
            frame.getServerPanel().setServerRunning();
            frame.getServerPanel().focusPortField();
        } catch (SBNetworkException e) {
            log(Level.SEVERE, "Exception while starting server on port " + port + ". " + e.toString());
            frame.getServerPanel().focusPortField();
        }
    }

    /**
     * Stop the server socket
     */
    public void stop() {
        try {
            log(Level.INFO, "Stopping server.");
            socketManager.stopServer();
            logOutAllUsers();
            finishAllMatches();
            frame.getServerPanel().setServerStopped();
            frame.getServerPanel().focusPortField();
        } catch (SBNetworkException e) {
            log(Level.SEVERE, "Could not stop server. " + e.toString());
        }
    }

    /**
     * Create a new game with two opposing users.
     * @param opponent1 The first user in the game.
     * @param opponent2 The second user in the game.
     */
    public void createGame(User opponent1, User opponent2) {
        opponent1.setInGame(true);
        opponent2.setInGame(true);
        ServerMatch match = new ServerMatch(this, opponent1, opponent2);
        addRunningMatch(match);
        broadcastUpdatedGamesList();
    }

    public void finishMatch(ServerMatch match) {
    	if(logmatches)addFinishedGame(match);
        removeRunningMatch(match);
        broadcastUpdatedGamesList();
        match.getUser(0).setInGame(false);
        match.getUser(1).setInGame(false);
    }

    // HELPERS

    /**
     * Logs a message with a level to the logger and to the server window.
     * @param level The level of the log message.
     * @param message The message to log.
     */
    public void log(Level level, String message) {
        if(getFrame() != null)
            if(getFrame().getServerPanel() != null)
                if(level.intValue() >= LOG_OUTPUT_LEVEL.intValue()) getFrame().getServerPanel().writeMessage(message);
        if(!message.endsWith("....")) L.log(level, message);
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
     * Return a failure answer for a received message.
     * @param returnTo The message to return an answer to.
     * @param parameters The parameters to send with the answer.
     */
    public void returnFailureMessage(SBProtocolMessage returnTo, String... parameters) {
        returnTo.returnFailureMessage(UID, new SBProtocolParameterArray(parameters));
    }

    /**
     * Sends a message to all users.
     * @param command The command of the message to send.
     * @param parameters The parameters of the message to send.
     */
    public void sendBroadcastMessage(SBProtocolCommand command, String... parameters) {
        socketManager.sendBroadcastMessage(new SBProtocolMessage(UID, command, new SBProtocolParameterArray(parameters)));
    }

    /**
     * Sends a message to all users.
     * @param command The command of the message to send.
     * @param parameters The parameters of the message to send.
     */
    public void sendBroadcastMessage(SBProtocolCommand command, SBProtocolParameterArray parameters) {
        socketManager.sendBroadcastMessage(new SBProtocolMessage(UID, command, parameters));
    }

    /**
     * Gets a socket by its assigned UUID.
     * @param UID The UUID to get the socket for.
     * @return The socket found for this UUID or null if no socket exists with this UUID.
     */
    public SBSocket getSocketByUID(UUID UID) {
        for(SBSocket socket: getSocketManager().getSockets()) {
            if(socket.getUID().equals(UID)) return socket;
        }
        return null;
    }

    /**
     * Checks whether a user exists.
     * @param name The name to check.
     * @return Whether the user with the given name exists.
     */
    public boolean checkIfUserExists(String name) {
        return getUserManager().existsName(name);
    }

    /**
     * Log out a user with a specific UID.
     * @param UID The UID of the user to log out.
     */
    public void logOutUserWithUID(final UUID UID) {
        final User userToLogOut = getUserManager().getAuthenticatedUser(UID);
        if(userToLogOut != null) {
            getUsersWaitingForRandomGame().remove(userToLogOut);
            for(final ServerMatch match: runningMatches) {
                if(match.getUser(0).getUID().equals(UID) || match.getUser(1).getUID().equals(UID)) {
                    // if the user was in a running match, end the match if the user doesn't log in again after some time.
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            UUID uidWhenLostConnection = getUID();
                            try { Thread.sleep(Server.TIME_AFTER_CONNECTION_LOST_BEFORE_AUTOSURRENDER); } catch (InterruptedException ignored) {}
                            // if user has not logged back in, auto-surrender for him.
                            // also check that the server UID has not changed (which would mean it was restarted)
                            if(getUserManager().getAuthenticatedUser(UID) == null && uidWhenLostConnection.equals(getUID())) {
                                for(ServerMatch matchCheck: runningMatches) { // check if match is still running
                                    if(matchCheck.getUser(0).getUID().equals(UID) || matchCheck.getUser(1).getUID().equals(UID)
                                            || matchCheck.getUser(0).getLastUID().equals(UID) || matchCheck.getUser(1).getLastUID().equals(UID)) { // users are in this match
                                        if(match.isRunning()) match.finishGame(userToLogOut);
                                        break;
                                    }
                                }
                            } else {
                                userToLogOut.setInGame(true);
                            }
                        }
                    })).start();
                }
            }

            String name = userToLogOut.getName();
            if (name != null) socketManager.sendBroadcastMessage(new SBProtocolMessage(UID, SBProtocolCommand.BDCST, new SBProtocolParameterArray(MODERATOR_NAME, name + " fell down a cliff!")));
            else socketManager.sendBroadcastMessage(new SBProtocolMessage(UID, SBProtocolCommand.BDCST, new SBProtocolParameterArray(MODERATOR_NAME, "Some unknown user fell down a cliff!")));

            if (getUserManager().getAuthenticatedUser(UID) != null) getUserManager().getAuthenticatedUser(UID).setLoggedOut();
        } else socketManager.sendBroadcastMessage(new SBProtocolMessage(UID, SBProtocolCommand.BDCST, new SBProtocolParameterArray(MODERATOR_NAME, "Some unknown user fell down a cliff!")));
    }

    /**
     * Log out all users on this server.
     */
    public void logOutAllUsers() {
        getUserManager().logoutAllUsers();
    }

    /**
     * Finishes all matches.
     */
    public void finishAllMatches() {
        for(int i = 0; i < getRunningMatches().size(); i++)
            getRunningMatches().get(i).finishGame();
    }

    /**
     * Checks whether a name is allowed. (doesn't exist yet, contains no spaces, longer than 0, not 'all').
     * @param name The name to be checked.
     * @return Whether the name is allowed.
     */
    public boolean validateName(String name) {
        return !getUserManager().existsName(name) && !name.contains(" ") && name.length() > 0 && !name.toLowerCase().equals("all");
    }

    /**
     * Check if there are any users waiting for a game against a random opponent.
     * @return Whether there are any users waiting for a game against a random opponent.
     */
    public boolean anyUsersWaitingForRandomGame() {
        return !getUsersWaitingForRandomGame().isEmpty();
    }

    /**
     * Check if a specific user is waiting for a game against a random opponent.
     * @param user The user to check if they are waiting for a game against a random opponent.
     * @return Whether the user is waiting for a game against a random opponent.
     */
    public boolean userWaitingForRandomGame(User user) {
        return getUsersWaitingForRandomGame().contains(user);
    }

    /**
     * Get the number of users waiting for a game against a random opponent.
     * @return The number of users waiting for a game against a random opponent.
     */
    public int getNumberOfUsersWaitingForRandomGame() {
        return getUsersWaitingForRandomGame().size();
    }

    /**
     * Request a list with all logged-in users from the server.
     */
    public void getUsersList() {
        String usersList = "";

        for(User user: getUserManager().getUsers()) {
            if(user.isLoggedIn()) {
                usersList += user.getName();
                if(user.isInGame()) usersList += " – in game";
                usersList += "\n";
            }
        }
        usersList = usersList.replaceAll("\\n$", ""); // remove last newline

        if(usersList.length() > 0) getFrame().getServerPanel().writeMessage(usersList);
        else getFrame().getServerPanel().writeMessage("No users online right now.");
    }

    /**
     * Request a list with all running and not running games from the server.
     */
    public void getGamesList() {
        String gamesList = "";

        // add finished matches
        for(String finishedGameString: getFinishedGames()) {
            FinishedGame finishedGame = new FinishedGame(finishedGameString);
            gamesList += finishedGame.getWinnerString() + " won against " + finishedGame.getLooserString() + ". " + finishedGame.getTeam1Score() + ":" + finishedGame.getTeam2Score() + "\n";
        }
        // add running matches
        for(ServerMatch match: getRunningMatches())
            if(match.isRunning())
                gamesList += match.getOpponent(0).getName() + " is playing against " + match.getOpponent(1).getName() + ". " + match.getScoreFromTeam(0) + ":" + match.getScoreFromTeam(1) + " UID: " + match.getMatchID().toString().substring(0, 5) + "\n";
        // add not yet running matches
        for(User user: getUsersWaitingForRandomGame())
            gamesList += user.getName() + " is waiting for someone to join his game.\n";
        gamesList = gamesList.replaceAll("\\n$", ""); // remove last newline

        if(gamesList.length()> 0) getFrame().getServerPanel().writeMessage(gamesList);
        else getFrame().getServerPanel().writeMessage("No games on the server right now.");
    }

    /**
     * Send a chat message to a user or to all users.
     * @param recipientName The name of the user to send the message to. If this is 'all', the message is sent as a broadcast message to everyone.
     * @param message The message to send.
     */
    public void chat(String recipientName, String message) {
        if(recipientName.equals("all")) {
            socketManager.sendBroadcastMessage(new SBProtocolMessage(UID, SBProtocolCommand.BDCST, new SBProtocolParameterArray(MODERATOR_NAME, message)));
            log(Level.INFO, "Sent broadcast message by " + MODERATOR_NAME + ": " + message);
        }
        else {
            User recipient = getUserManager().getUser(recipientName);
            if(recipient != null) { // user with name exists
                SBSocket returnSocket = getSocketByUID(recipient.getUID());
                if(returnSocket != null) { // socket for recipient is connected
                    returnSocket.sendMessage(new SBProtocolMessage(UID, SBProtocolCommand.SENDM, new SBProtocolParameterArray(MODERATOR_NAME, message)));
                    log(Level.INFO, "Sent message by " + MODERATOR_NAME + " to " + recipientName + ": " + message);
                } else log(Level.WARNING, recipientName + " is offline.");
            } else log(Level.WARNING, recipientName + " does not exist");
        }
    }

    /**
     * Cheat.
     * @param text The cheat command to execute.
     */
    public void cheat(String text) {
        if(text.startsWith("round")) { // change round and set to playing phase
            text = text.substring(6);
            ServerMatch match = getRunningMatchById(text.substring(0, 5));
            int newRoundCount;
            if(match == null) { // check if match was found
                log(Level.INFO, "If cheat, cheat right. Match not found.");
                return;
            }
            try { // try to parse the round count to set the match to
                newRoundCount = Integer.parseInt(text.substring(6));
            } catch(NumberFormatException e) {
                log(Level.INFO, "If cheat, cheat right. Illegal round count.");
                return;
            }
            if(newRoundCount <= 0) {
                log(Level.INFO, "If cheat, cheat right. Round count below zero.");
                return;
            }
            match.setRoundCount(newRoundCount);
            match.setGamePhase(3); // reset to playing phase
            match.sendGame();
        } else {
            log(Level.INFO, "Cheat not known.");
            getFrame().getServerPanel().emptyMessageField();
        }
    }

    // GETTERS & SETTERS

    public void addUserWaitingForRandomGame(User user) {
        getUsersWaitingForRandomGame().add(user);
    }

    public void removeUserWaitingForRandomGame(User user) {
        getUsersWaitingForRandomGame().remove(user);
    }

    public ServerFrame getFrame() {
        return frame;
    }

    public Vector<User> getUsersWaitingForRandomGame() {
        return usersWaitingForRandomGame;
    }

    public ServerMessageProcessor getMessageProcessor() {
        return messageProcessor;
    }

    public SBSocketManager getSocketManager() {
        return socketManager;
    }

    public ServerProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Returns a copy of all running matches (to iterate on, etc.).
     * @return A copy of all running matches.
     */
    @SuppressWarnings("unchecked")
    public Vector<ServerMatch> getRunningMatches() {
        return (Vector<ServerMatch>) runningMatches.clone();
    }

    /**
     * Returns the running match with match ID beginning with MID.
     * @param MID The match ID of the match searched for.
     * @return The running match with match ID beginning with MID.
     */
    public ServerMatch getRunningMatchById(String MID) {
        for(ServerMatch match: getRunningMatches())
            if(match.getMatchID().toString().startsWith(MID))
                return match;
        return null;
    }

    /**
     * Adds a match to the vector of all running matches.
     * @param match The match to add.
     */
    public void addRunningMatch(ServerMatch match) {
        this.runningMatches.add(match);
    }

    public void removeRunningMatch(ServerMatch match) {
        this.runningMatches.remove(match);
    }

    // UNUSED

    public void lostConnection() {}

    public void isConnecting() {}

    public void hasConnected(InetAddress address, int port, boolean connected) {}
}
