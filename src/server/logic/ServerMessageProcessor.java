package server.logic;

import network.*;
import server.Server;
import util.FinishedGame;
import util.MessageProcessor;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The message processor on the server side.
 * Created by milan on 1.4.15.
 */
public class ServerMessageProcessor implements MessageProcessor {

    private final Server server;
    private HashMap<String, SBProtocolMessage> unansweredStartGameMessages = new HashMap<String, SBProtocolMessage>();

    public ServerMessageProcessor(Server server) {
        this.server = server;
    }

    /**
     * Sorts messages to process by module and continues in processMessageMODULE(message).
     * @param message The message to process
     */
    @Override
    public void processMessage(SBProtocolMessage message) {
        try {

            boolean processMessage;
            switch (message.getModule()) {
                case CHT:
                    processMessage = processMessageCHT(message);
                    break;
                case AUT:
                    processMessage = processMessageAUT(message);
                    break;
                case GAM:
                    processMessage = processMessageGAM(message);
                    break;
                default:
                    processMessage = false;
                    break;
            }
            if(!processMessage) returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);

        } catch (IndexOutOfBoundsException e) { returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER); }
    }

    /**
     * Sorts answers to process by module and continues in processAnswerMODULE(answer).
     * @param answer The answer to process
     */
    @Override
    public void processAnswer(SBProtocolMessage answer) {
        try {
            SBProtocolMessage message = null;
            while(message == null){
            	try {
                    for (SBProtocolMessage messageThatIsPotentiallyAnswered : getProtocolManager().getUnansweredMessages()) // for all unanswered messages
                        if (messageThatIsPotentiallyAnswered.getMID().equals(UUID.fromString(answer.getParameterContent(0)))) { // get the message whose MID equals the MID in the answer
                            message = messageThatIsPotentiallyAnswered;
                            break;
                        }
                } catch(ConcurrentModificationException e) {
                    getServer().log(Level.SEVERE, "Catched concurrent modification exception! But continuing anyway.");
                    e.printStackTrace();
                }
            }
            getProtocolManager().removeUnansweredMessage(message);

            if(message != null) {

                switch (answer.getModule()) {
                    case SUC:
                        processAnswerSUC(answer, message);
                        break;
                    case FAI:
                        processAnswerFAI(answer, message);
                        break;
                }

            } else getServer().log(Level.WARNING, "Received answer but found no message it belonged to: " + answer.toStringShortenUUID() + " Ignoring it.");

        } catch (IndexOutOfBoundsException e) { /* Don't return failure message because answers don't expect (e.g. ignore) answers anyway */ }
    }



    // MESSAGE PROCESSORS

    /**
     * Processes messages in the module CHT.
     * @param message The message to process.
     * @return Whether the message was processed successfully.
     */
    private boolean processMessageCHT(SBProtocolMessage message) {
        switch (message.getCommand()) {
            case SENDM:
                User recipient = getUserManager().getUser(message.getParameterContent(0));
                if(recipient != null) { // user with name exists
                    SBSocket returnSocket = getServer().getSocketByUID(recipient.getUID());
                    if(returnSocket != null) { // socket for recipient is connected
                        User sender = getUserManager().getAuthenticatedUser(message.getUID());
                        if (sender != null) { // sender is logged in
                            returnSocket.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.SENDM, new SBProtocolParameterArray(sender.getName(), message.getParameterContent(1))));
                            getServer().log(Level.INFO, "Sent message by " + sender.getName() + " to " + recipient.getName() + ".");
                        } else { // sender is not logged in. send anonymously
                            returnSocket.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.SENDM, new SBProtocolParameterArray("", message.getParameterContent(1))));
                            getServer().log(Level.INFO, "Sent message by anonymous user to " + recipient.getName() + ".");
                        }
                        returnSuccessMessage(message, "SENT");
                    } else returnFailureMessage(message, "USER OFFLINE");
                } else returnFailureMessage(message, "USER NONEXISTENT");
                return true;

            case BDCST:
                sendBroadcastMessage(SBProtocolCommand.BDCST, message.getParameterContent(0), message.getParameterContent(1));
                getServer().log(Level.INFO, "Sent broadcast message by "+message.getParameterContent(0)+".");
                return true;

            default:
                return false;
        }
    }

    /**
     * Processes messages in the module AUT.
     * @param message The message to process.
     * @return Whether the message was processed successfully.
     */
    private boolean processMessageAUT(SBProtocolMessage message) {
        switch (message.getCommand()) {
            case LOGUT:
                User userToLogOut = getUserManager().getAuthenticatedUser(message.getUID());
                if (userToLogOut != null) { // client is logged in
                    userToLogOut.setLoggedOut(); // log out
                    userToLogOut.setUID(new UUID(0, 0)); // reset UID

                    sendBroadcastMessage(SBProtocolCommand.BDCST, Server.MODERATOR_NAME, userToLogOut.getName() + " ran away into the bushes!"); // send broadcast to tell user has logged out
                    getServer().log(Level.INFO, "Logged out " + userToLogOut.getName() + ".");
                    returnSuccessMessage(message); // user was logged out
                } else returnFailureMessage(message); // client was not logged in

                getServer().broadcastUpdatedUsersList();
                return true;

            case LOGIN:
                String name = message.getParameterContent(0), passwordEncrypted = message.getParameterContent(1);

                if(name.length() > 0 && !passwordEncrypted.equals("d41d8cd98f00b204e9800998ecf8427e")) { // only continue if no field was empty. (password: empty string digested by md5 = d41d8cd98f00b204e9800998ecf8427e)

                    if (getUserManager().existsName(name)) { // user with name exists
                        User userToLogIn = getUserManager().getUser(name);
                        if (userToLogIn.getPasswordEncrypted().equals(passwordEncrypted)) { // password matched
                            if (!userToLogIn.isLoggedIn()) { // not logged in yet, log in
                                userToLogIn.setLoggedIn(); // log in
                                message.getSocket().setUID(message.getUID()); // set UID of socket to UID of received message
                                userToLogIn.setUID(message.getUID()); // set UID of user to UID of received message

                                sendBroadcastMessage(SBProtocolCommand.BDCST, Server.MODERATOR_NAME, "A wild " + name + " appeared!"); // send broadcast to tell user has logged in
                                returnSuccessMessage(message, "LOGGED IN");
                                getServer().log(Level.INFO, "Logged in " + name + ".");
                            } else { // user was already logged in
                                returnFailureMessage(message, "ALREADY LOGGED IN");
                                getServer().log(Level.INFO, "Refused login for " + name + ". Already logged in.");
                            }
                        } else { // wrong password
                            returnFailureMessage(message, "WRONG PASS");
                            getServer().log(Level.INFO, "Refused login for " + name + ". Wrong password.");
                        }

                    } else { // create user
                        if(getServer().validateName(name)) { // name is valid (no spaces, not yet registered, not 'all')
                            getUserManager().writeUser(new User(getUserManager(), name, passwordEncrypted, message.getUID()));

                            sendBroadcastMessage(SBProtocolCommand.BDCST, Server.MODERATOR_NAME, name + " was born."); // send broadcast to tell user has been created
                            returnSuccessMessage(message, "CREATED USER");
                            getServer().log(Level.INFO, "Created new user with name " + name + ".");
                        } else {
                            returnFailureMessage(message, "ILLEGAL NAME");
                            getServer().log(Level.INFO, "Refused creation of " + name + ". Illegal name.");
                        }
                    }
                } else { // name and/or password was empty
                    returnFailureMessage(message, "EMPTY FIELD");
                }

                getServer().broadcastUpdatedGamesList();
                return true;

            case EXIST:
                if (getUserManager().existsName(message.getParameterContent(0)))
                    returnSuccessMessage(message, "EXISTS");
                else returnSuccessMessage(message, "EXISTS NOT");
                return true;

            case CHNGE:
                User userToChangeName = getUserManager().getUser(message.getParameterContent(0));

                if (userToChangeName != null) { // user exists
                    if (userToChangeName.getName().equals(message.getParameterContent(0)) // current name equals name in message
                            && userToChangeName.getName().equals(getUserManager().getAuthenticatedUser(message.getUID()).getName()) // current name equals name of sender (if logged in)
                            && getServer().validateName(message.getParameterContent(1))) // new name is valid (no spaces, not yet registered, not 'all')
                        userToChangeName.setName(message.getParameterContent(1)); // change name

                    else if (getUserManager().getAuthenticatedUser(message.getUID()).isOp() // client is authenticated as operator (which allows him to change any names he desires)
                            && getServer().validateName(message.getParameterContent(1))) // new name is valid (no spaces, not yet registered, not 'all')
                        userToChangeName.setName(message.getParameterContent(1)); // change name

                    else { // old name doesn't match and client is no authenticated operator
                        returnFailureMessage(message);
                        return true; // return to suppress sending the messages below
                    }

                    sendBroadcastMessage(SBProtocolCommand.BDCST, Server.MODERATOR_NAME, message.getParameterContent(0) + " evolves to " + userToChangeName.getName()); // send broadcast to tell user has changed their name
                    returnSuccessMessage(message);
                    getServer().log(Level.INFO, "Changed user name of " + message.getParameterContent(0) + " to " + userToChangeName.getName() + ".");
                } else returnFailureMessage(message); // user doesn't exist

                getServer().broadcastUpdatedUsersList();
                return true;

            case RMUSR:
                User userToRemove = getUserManager().getUser(message.getParameterContent(0));

                if (userToRemove != null) { // user exists
                    if (userToRemove.getName().equals(message.getParameterContent(0))
                            && userToRemove.getName().equals(getUserManager().getAuthenticatedUser(message.getUID()).getName())) // name matches name in params and name of authenticated client
                        getUserManager().removeUser(userToRemove.getName()); // remove user

                    else if (getUserManager().getAuthenticatedUser(message.getUID()).isOp()) // client is authenticated as operator (which allows him to remove any users he desires)
                        getUserManager().removeUser(userToRemove.getName()); // remove user

                    else {
                        returnFailureMessage(message);
                        return true; // return to suppress sending the messages below
                    }

                    sendBroadcastMessage(SBProtocolCommand.BDCST, Server.MODERATOR_NAME, userToRemove.getName() + " went neverland and never came back."); // send broadcast to tell user has been deleted
                    returnSuccessMessage(message);
                    getServer().log(Level.INFO, "Removed user "+userToRemove.getName()+".");
                } else returnFailureMessage(message); // user doesn't exist

                getServer().broadcastUpdatedUsersList();
                return true;

            case OPUSR:
                User userToOp = getUserManager().getUser(message.getParameterContent(0));

                if (userToOp != null) { // user exists
                    if (userToOp.getName().equals(message.getParameterContent(0))
                            && userToOp.getName().equals(getUserManager().getAuthenticatedUser(message.getUID()).getName())) // name matches name in params and name of authenticated client
                        userToOp.setIsOp(true);

                    else if (getUserManager().getAuthenticatedUser(message.getUID()).isOp()) // client is authenticated as operator (which allows him to op any users he desires)
                        userToOp.setIsOp(true);

                    else { // old name doesn't match and client is no authenticated operator
                        returnFailureMessage(message);
                        return true; // return to suppress sending the messages below
                    }
                } else { // user doesn't exist
                    returnFailureMessage(message);
                    return true; // return to suppress sending the messages below
                }

                sendBroadcastMessage(SBProtocolCommand.BDCST, Server.MODERATOR_NAME, "That puny " + userToOp.getName() + " is now suddenly VERY important."); // send broadcast to tell user has been granted op rights
                returnSuccessMessage(message);
                getServer().log(Level.INFO, "Granted user "+userToOp.getName()+" operator rights.");
                return true;

            case LSUSR:
                SBProtocolParameterArray usersParameters = new SBProtocolParameterArray();

                for(User user: getUserManager().getUsers()) {
                    if(user.isLoggedIn()) {
                        String inGame = user.isInGame() ? "true" : "false";
                        usersParameters.addParameter(new SBProtocolParameterArray(user.getName(), inGame));
                    }
                }

                message.returnSuccessMessage(getServer().UID, usersParameters);
                return true;

            case LSGAM:
                SBProtocolParameterArray gamesParameters = new SBProtocolParameterArray();

                // add finished matches
                for(String finishedGameString: getServer().getFinishedGames()) {
                    FinishedGame g = new FinishedGame(finishedGameString);
                    gamesParameters.addParameter(new SBProtocolParameterArray(g.getCoach1Name(), g.getCoach2Name(), g.getWinnerString(), g.getTeam1Score(), g.getTeam2Score()));
                }
                // add running matches
                for(ServerMatch match: getServer().getRunningMatches())
                    if(match.isRunning())
                        gamesParameters.addParameter(new SBProtocolParameterArray(match.getUser(0).getName(), match.getUser(1).getName(), match.getScoreFromTeam(0)+"", match.getScoreFromTeam(1)+""));
                // add not yet running matches
                for(User user: getServer().getUsersWaitingForRandomGame())
                    gamesParameters.addParameter(new SBProtocolParameterArray(user.getName()));

                message.returnSuccessMessage(getServer().UID, gamesParameters);
                return true;
                
            default:
                return false;
        }
    }

    /**
     * Processes messages in the module GAM.
     * @param message The message to process.
     * @return Whether the message was processed successfully.
     */
    private boolean processMessageGAM(SBProtocolMessage message) {
        switch (message.getCommand()) {
            case START:
                if(getUserManager().getAuthenticatedUser(message.getUID()) != null) { // user is logged in
                    User userStartingGame = getUserManager().getAuthenticatedUser(message.getUID());

                    if(message.getParameters().size() > 0) { // Start game against name in parameter 0
                        User invitedUser = getUserManager().getUser(message.getParameterContent(0)); // set the invited user

                        if(invitedUser != null) { // invited user exists
                            if(invitedUser.isLoggedIn()) { // invited user is logged in
                                if(getServer().userWaitingForRandomGame(invitedUser)) { // invited user is waiting for a random game

                                    if(userStartingGame != null && !userStartingGame.equals(invitedUser)) { // if both users still are logged in and actually are two different users
                                        SBProtocolMessage originalStartGameMessage = getUnansweredStartGameMessage(invitedUser.getName());

                                        if(originalStartGameMessage != null) { // the original unanswered message still exists
                                            removeUnansweredStartGameMessage(invitedUser.getName());
                                            returnSuccessMessage(originalStartGameMessage, userStartingGame.getName()); // tell waiting user that game has been created
                                            returnSuccessMessage(message, invitedUser.getName()); // tell inviting user that game has been created
                                            getServer().createGame(userStartingGame, invitedUser);
                                            getServer().removeUserWaitingForRandomGame(invitedUser); // remove invited user from list of users waiting for random game
                                            getServer().log(Level.INFO, "Starting match. "+invitedUser.getName()+" versus "+userStartingGame.getName()+"!");
                                        } else {
                                            getServer().log(Level.SEVERE, "Original start-game-message not found. Ignoring request:\n"+message.toStringShortenUUID());
                                            returnFailureMessage(message);
                                        }
                                    } else {
                                        getServer().log(Level.SEVERE, "Error while checking if both users wanting to start a game are still online. Ignoring request:\n"+message.toStringShortenUUID());
                                        returnFailureMessage(message);
                                    }

                                } else { // ask invited user to accept invitation

                                    if (addUnansweredStartGameMessage(message)) { // add message to unanswered start game messages to be able to reply later
                                        SBSocket inviteSocket = getServer().getSocketByUID(invitedUser.getUID()); // prepare socket to invite
                                        if (inviteSocket != null) { // if socket exists, invite client to play against user starting game
                                            inviteSocket.sendMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.START, new SBProtocolParameterArray(userStartingGame.getName())));
                                            getServer().log(Level.INFO, "Inviting " + invitedUser.getName() + " to play against " + userStartingGame.getName() + ".");
                                        }
                                    } else returnFailureMessage(message);

                                }
                            } else returnFailureMessage(message);
                        } else returnFailureMessage(message);

                    } else // Add or remove user to or from waiting list for a random game
                        if(!getServer().userWaitingForRandomGame(userStartingGame)) { // authenticated user is not waiting for a random game yet
                            if(addUnansweredStartGameMessage(message)) {
                                getServer().addUserWaitingForRandomGame(userStartingGame);
                                getServer().log(Level.INFO, userStartingGame.getName() + " is now waiting for a game.");
                                getServer().broadcastUpdatedGamesList();
                            } else returnFailureMessage(message);
                        } else {
                            getServer().removeUserWaitingForRandomGame(userStartingGame);
                            getServer().log(Level.INFO, userStartingGame.getName() + " is not waiting for a game anymore.");
                            getServer().broadcastUpdatedGamesList();
                        }
                }
                return true;

            case SRNDR:
            case EVENT:
            case ACTIO:
                addMessageOrAnswerToMatch(message);
                return true;

            default:
                return false;
        }
    }

    // ANSWER PROCESSORS

    /**
     * Processes success answers.
     * @param answer The answer to process.
     * @param message The original message that was sent by this server.
     */
    private void processAnswerSUC(SBProtocolMessage answer, SBProtocolMessage message) {
        switch (message.getCommand()) {
            case START: // client has confirmed starting a game against suggested opponent
                User confirmingUser = getUserManager().getAuthenticatedUser(answer.getUID()); // the user that confirmed starting the match
                User otherUser = getUserManager().getUser(message.getParameterContent(0)); // the opponent of the first user

                if(confirmingUser != null && otherUser != null && !confirmingUser.equals(otherUser)) { // if both users still are logged in and actually are two different users
                    SBProtocolMessage originalStartGameMessage = getUnansweredStartGameMessage(otherUser.getName());

                    if(originalStartGameMessage != null) { // the original unanswered message still exists
                        removeUnansweredStartGameMessage(otherUser.getName());
                        getServer().removeUserWaitingForRandomGame(otherUser);
                        returnSuccessMessage(originalStartGameMessage, confirmingUser.getName());
                        getServer().createGame(otherUser, confirmingUser);
                        getServer().log(Level.INFO, "Starting match. "+otherUser.getName()+" versus "+confirmingUser.getName()+"!");
                    } else getServer().log(Level.SEVERE, "Original start-game-message not found. Ignoring answer:\n"+answer.toStringShortenUUID());
                } else getServer().log(Level.SEVERE, "Error while checking if both users wanting to start a game are still online. Ignoring answer:\n"+answer.toStringShortenUUID());
                break;

            case SRNDR:
            case EVENT:
            case ACTIO:
                addMessageOrAnswerToMatch(answer);
                getProtocolManager().addUnansweredMessage(message); // add answered message back to unanswered messages so it isn't lost
                break;
        }
    }

    /**
     * Processes failure answers.
     * @param answer The answer to process.
     * @param message The original message that was sent by this server.
     */
    private void processAnswerFAI(SBProtocolMessage answer, SBProtocolMessage message) {
        switch (message.getCommand()) {
            case START: // client has declined starting a game against suggested opponent
                User otherUser = getUserManager().getUser(message.getParameterContent(0)); // the opponent of the first user
                SBProtocolMessage originalStartGameMessage = getUnansweredStartGameMessage(otherUser.getName());

                if(originalStartGameMessage != null) { // only return failure answers if the original message is still unanswered
                    removeUnansweredStartGameMessage(otherUser.getName());
                    returnFailureMessage(originalStartGameMessage, otherUser.getName());
                }
                break;

            case SRNDR:
            case EVENT:
            case ACTIO:
                addMessageOrAnswerToMatch(answer);
                getProtocolManager().addUnansweredMessage(message); // add answered message back to unanswered messages so it isn't lost
                break;
        }
    }

    // HELPERS

    /**
     * Returns a success message for the given message with parameters.
     * @param returnTo The message to answer.
     * @param parameters The parameters to send back with the answer.
     */
    @Override
    public void returnSuccessMessage(SBProtocolMessage returnTo, String... parameters) {
        getServer().returnSuccessMessage(returnTo, parameters);
    }

    /**
     * Returns a failure message for the given message with parameters.
     * @param returnTo The message to answer.
     * @param parameters The parameters to send back with the answer.
     */
    @Override
    public void returnFailureMessage(SBProtocolMessage returnTo, String... parameters) {
        getServer().returnFailureMessage(returnTo, parameters);
    }

    /**
     * Sends a new message to all connected clients.
     * @param command The command of the message to send.
     * @param parameters The parameters for the message to send.
     */
    private void sendBroadcastMessage(SBProtocolCommand command, String... parameters) {
        getServer().sendBroadcastMessage(command, parameters);
    }

    /**
     * Add an incoming game message or answer to the match incoming messages or answers queue.
     * @param messageOrAnswer The message or answer to forward.
     */
    private void addMessageOrAnswerToMatch(SBProtocolMessage messageOrAnswer) {
        if(getUserManager().getAuthenticatedUser(messageOrAnswer.getUID()) != null) { // user is logged in
            User userSendingMessage = getUserManager().getAuthenticatedUser(messageOrAnswer.getUID());

            if(userSendingMessage.isInGame()) { // user is in game
                for(ServerMatch match: getServer().getRunningMatches()) {
                    if(match.getOpponent(userSendingMessage) != null) { // user is in this match
                        if(messageOrAnswer.getModule() == SBProtocolCommand.SBProtocolModule.GAM) // is message
                            match.addIncomingMessage(messageOrAnswer); // add message to incoming messages in match
                        else // is answer
                            match.addIncomingAnswer(messageOrAnswer); // add answer to incoming answers in match
                        break;
                    }
                }
            } else returnFailureMessage(messageOrAnswer);
        } else returnFailureMessage(messageOrAnswer);
    }

    // SETTERS & GETTERS

    /**
     * Adds a message and the name of its sender to the unansweredStartGameMessages queue.
     * @param message The message to add.
     * @return False if the sender of the message was not logged in (no user was logged in with the UID of the message).
     */
    private boolean addUnansweredStartGameMessage(SBProtocolMessage message) {
        String username = getUserManager().getAuthenticatedUser(message.getUID()).getName();
        if(username != null) {
            unansweredStartGameMessages.put(username, message);
            return true;
        }
        else return false;
    }

    /**
     * Remove a message from the unansweredStartGameMessages queue by the name of its sender.
     * @param senderName The name of the sender that sent the message.
     */
    private void removeUnansweredStartGameMessage(String senderName) {
        unansweredStartGameMessages.remove(senderName);
    }

    /**
     * Get a message from the unansweredStartGameMessages queue by the name of its sender.
     * @param senderName The name of the sender that sent the message.
     */
    private SBProtocolMessage getUnansweredStartGameMessage(String senderName) {
        return unansweredStartGameMessages.get(senderName);
    }

    /**
     * Get the server this message processor works for.
     * @return The server this message processor works for.
     */
    public Server getServer() {
        return server;
    }

    private UserManager getUserManager() {
        return getServer().getUserManager();
    }

    private SBSocketManager getSocketManager() {
        return getServer().getSocketManager();
    }

    private SBProtocolManager getProtocolManager() {
        return getServer().getProtocolManager();
    }

    private UUID getUID() {
        return getServer().UID;
    }
}
