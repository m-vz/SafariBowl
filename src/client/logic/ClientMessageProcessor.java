package client.logic;

import client.display.ClientFrame;
import gameLogic.GameController;
import network.*;
import client.Client;
import util.MessageProcessor;

import java.util.UUID;
import java.util.logging.Level;

/**
 * The message processor on the client side.
 * Created by milan on 1.4.15.
 */
public class ClientMessageProcessor implements MessageProcessor {

    private final Client client;
    public String potentialNewUsername = "", loggedInWithUsername = "", loggedInWithEncryptedPassword = "", invitedOpponent = "";
    public boolean waitingForGame = false;
    private int userExists = -1; // user by checkIfUserExists() to wait for an answer. States: -1: waiting, 0: doesn't exist, 1: exists, 100: error

    public ClientMessageProcessor(Client client) {
        this.client = client;
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
            for(SBProtocolMessage messageThatIsPotentiallyAnswered: getProtocolManager().getUnansweredMessages()) // for all unanswered messages
                if(messageThatIsPotentiallyAnswered.getMID().equals(UUID.fromString(answer.getParameterContent(0)))) { // get the message whose MID equals the MID in the answer
                    message = messageThatIsPotentiallyAnswered;
                    break;
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

            } else getClient().log(Level.FINER, "Received answer but found no message it belonged to: " + answer.toStringShortenUUID() + " Ignoring it.");

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
                getFrame().addChatMessage(message.getParameterContent(0), message.getParameterContent(1));
                return true;

            case BDCST:
                if(!message.getParameterContent(0).toLowerCase().equals(getUsername().toLowerCase())) // if was not sent by this client
                    getFrame().addChatMessage(message.getParameterContent(0)+"@all", message.getParameterContent(1));
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
            case UPGAM:
                getClient().updateGamesList(message.getParameters());
                return true;

            case UPUSR:
                getClient().updateUsersList(message.getParameters());
                return true;

            case SCORE:
                getClient().updateHighscoreTable(message.getParameters());
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
            case START: // server asks user to start game against player in message parameter 0
                String opponent = message.getParameterContent(0);

                if (waitingForGame) { // client was waiting for a random game. decline invitations
                    returnFailureMessage(message);

                } else { // client was not waiting for a game. message is an invitation
                    if(!getClient().isPlayingMatch()) { // client is not in a match currently
                        if(getFrame().getInvitedAnswer(opponent)) { // user accepted invitation
                            getClient().createGame(opponent);
                            returnSuccessMessage(message);
                        } else { // user declined invitation
                            returnFailureMessage(message);
                        }
                    } else returnFailureMessage(message); // client was playing already
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
     * @param message The original message that was sent by this client.
     */
    private void processAnswerSUC(SBProtocolMessage answer, SBProtocolMessage message) {
        switch (message.getCommand()) {
            case LOGIN:
                if (answer.getParameterContent(1).equals("LOGGED IN")) {
                    getClient().log(Level.INFO, "Received login success answer.");
                    if (!isLoggedIn()) { // client is not logged in yet
                        setLoggedIn(true); // log in
                        if (loggedInWithUsername.length() > 0) {
                            if(loggedInWithUsername.equals(getUsername())) { // was logged in with the same user before
                                if(getClient().isPlayingMatch()) { // client lost connection during match
                                    getFrame().showGamePanel(false);
                                }
                            } else { // logged in with new username
                                setUsername(loggedInWithUsername); // set username
                                getClient().setPlayingMatch(false);
                                getFrame().resetLobbyPanel();
                                getFrame().showLobbyPanel(Client.MODERATOR_NAME + ": Hello " + getUsername() + "!");
                                if(getClient().automatchstart) { // starts a new match automatically to speed up testing
                                    if (getUsername().equals("milan")) getClient().startGame();
                                    else if (getUsername().equals("pikachu")) getClient().invitePlayer("milan");
                                }
                            }

                        } else getFrame().showLobbyPanel(Client.MODERATOR_NAME + ": Hello " + getUsername() + "!");
                    }
                } else if (answer.getParameterContent(1).equals("CREATED USER")) {
                    getClient().log(Level.INFO, "Received sign up success answer, you can log in now.");
                    getFrame().getLoginPanel().setMessage("Signed up successfully. Log in now.");
                    if (!isLoggedIn()) // log in automatically
                        answer.returnMessage(new SBProtocolMessage(getUID(), SBProtocolCommand.LOGIN, message.getParameters()));
                }
                break;

            case LOGUT:
                if (isLoggedIn()) {
                    getClient().log(Level.INFO, "Received logout success answer.");
                    setLoggedIn(false);
                    setUsername("");
                    getFrame().showLoginPanel("Successfully logged out.");
                }
                break;

            case CHNGE:
                if (potentialNewUsername.length() > 0) {
                    setUsername(potentialNewUsername);
                    potentialNewUsername = "";
                    getFrame().getLoginPanel().changeSavedName(getUsername());
                    getFrame().getLobbyPanel().writeMessage("Changed username to " + getUsername() + ".");
                }
                break;

            case EXIST:
                if (answer.getParameterContent(1).equals("EXISTS")) userExists = 1;
                else if (answer.getParameterContent(1).equals("EXISTS NOT")) userExists = 0;
                else userExists = 100; // unknown answer
                break;

            case START:
                getClient().createGame(answer.getParameterContent(1));
                break;

            case LSUSR:
                for(int i = 1; i < answer.getParameters().size(); i++) {
                    if(answer.getParameter(i).isArray())
                        if(answer.getParameter(i).toArray().size() == 2) {
                            // write the user to the lobby
                            String userString = answer.getParameter(i).toArray().getParameter(0).getContent();
                            if(answer.getParameter(i).toArray().getParameter(1).getContent().equals("true")) userString += " – in game";
                            getFrame().writeMessage(userString);
                        } else getClient().log(Level.WARNING, "Received invalid parameter in users list: "+answer.getParameter(i));
                }
                break;

            case LSGAM:
                for(int i = 1; i < answer.getParameters().size(); i++) {
                    if(answer.getParameter(i).isArray())
                        if(answer.getParameter(i).toArray().size() == 5) { // is finished game
                            // write the game info to the lobby
                        	SBProtocolParameterArray a = answer.getParameter(i).toArray();
                            String winner = a.getParameter(2).getContent();
                            String looser = a.getParameter(0).getContent(); // set looser to first opponent (is checked on line below)
                            if(winner.equals(looser)) looser = a.getParameter(1).getContent(); // if looser was second opponent, set to second opponent
                            String score1 = a.getParameter(3).getContent();
                            String score2 = a.getParameter(4).getContent();
                            if(winner.equals("")) {
                            	getFrame().writeMessage(a.getParameter(0).getContent() + " tied against " + a.getParameter(1).getContent() + ". " + score1 + ":" + score2);
                            } else {
                            	getFrame().writeMessage(winner + " won against " + looser + ". " + score1 + ":" + score2);
                            }
                        } else if(answer.getParameter(i).toArray().size() == 4) { // is running game
                            // write the game info to the lobby
                            String opponent1 = answer.getParameter(i).toArray().getParameter(0).getContent();
                            String opponent2 = answer.getParameter(i).toArray().getParameter(1).getContent();
                            String score1 = answer.getParameter(i).toArray().getParameter(2).getContent();
                            String score2 = answer.getParameter(i).toArray().getParameter(3).getContent();
                            getFrame().writeMessage(opponent1 + " is playing against " + opponent2 + ". " + score1 + ":" + score2);
                        } else if(answer.getParameter(i).toArray().size() == 1) { // is player waiting for opponent
                            // write the game info to the lobby
                            String playerWaitingForGame = answer.getParameter(i).toArray().getParameter(0).getContent();
                            getFrame().writeMessage(playerWaitingForGame + " is waiting for someone to join his game.");
                        } else getClient().log(Level.WARNING, "Received invalid parameter in games list: "+answer.getParameter(i));
                }
                if(answer.getParameters().size() <= 1) { // no games returned
                    getFrame().writeMessage("No games on the server right now.");
                    getClient().log(Level.INFO, "Received empty games list.");
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

    /**
     * Processes failure answers.
     * @param answer The answer to process.
     * @param message The original message that was sent by this client.
     */
    private void processAnswerFAI(SBProtocolMessage answer, SBProtocolMessage message) {
        switch (message.getCommand()) {
            case LOGIN:
                if (answer.getParameterContent(1).equals("ALREADY LOGGED IN")) {
                    getClient().log(Level.INFO, loggedInWithUsername + " is already logged in.");
                    getFrame().showLoginPanel(loggedInWithUsername + " is already logged in.");
                    if(getClient().autologin) { // logs in automatically to speed up testing
                        if(loggedInWithUsername.equals("milan"))
                            client.login("pikachu", "33a9113983a8ec726fc4165fd99915eb");
                        else client.login("milan", "30f6bee6387a90c504c0116b968db626");
                    }

                } else if (answer.getParameterContent(1).equals("WRONG PASS")) {
                    getClient().log(Level.INFO, "Wrong password.");
                    getFrame().showLoginPanel("Wrong password.");

                } else if (answer.getParameterContent(1).equals("ILLEGAL NAME")) {
                    getClient().log(Level.INFO, "Name not allowed.");
                    getFrame().showLoginPanel("Name not allowed. Please, PLEASE don't break my logic with spaces in your name.");

                } else if (answer.getParameterContent(1).equals("EMPTY FIELD")) {
                    if (message.getParameterContent(0).length() <= 0) { // if name was empty
                        getClient().log(Level.INFO, "Name cannot be empty.");
                        getFrame().showLoginPanel("Name cannot be empty.");

                    } else if (message.getParameterContent(1).equals("d41d8cd98f00b204e9800998ecf8427e")) { // if password was empty (empty string digested by md5 = d41d8cd98f00b204e9800998ecf8427e)
                        getClient().log(Level.INFO, "Password cannot be empty.");
                        getFrame().showLoginPanel("Password cannot be empty.");

                    } else { // if something strange happened
                        getClient().log(Level.INFO, "Empty login field. Is there a ghost in here?");
                        getFrame().showLoginPanel("Empty login field. Is there a ghost in here?");
                    }
                }
                break;

            case LOGUT:
                if (isLoggedIn()) {
                    getClient().log(Level.WARNING, "Could not log out. Maybe, "+Client.MODERATOR_NAME+" has a bad day today.");
                    getFrame().getLobbyPanel().writeMessage("Could not log out. Maybe, " + Client.MODERATOR_NAME + " has a bad day today.");
                }
                break;

            case CHNGE:
                getFrame().getLobbyPanel().writeMessage("Could not change username to " + potentialNewUsername + ".");
                getFrame().getLobbyPanel().focusChangeNameField();
                break;

            case START:
                getClient().startingGameFailed(true);
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
     * Add an incoming game message or answer to the match incoming messages or answers queue.
     * @param messageOrAnswer The message or answer to forward.
     */
    private void addMessageOrAnswerToMatch(SBProtocolMessage messageOrAnswer) {
        if(getClient().isPlayingMatch() && getClient().getMatch() != null) { // client is in match
            if (messageOrAnswer.getModule() == SBProtocolCommand.SBProtocolModule.GAM) // is message
                getClient().getMatch().addIncomingMessage(messageOrAnswer); // add message to incoming messages in match
            else // is answer
                getClient().getMatch().addIncomingAnswer(messageOrAnswer); // add answer to incoming answers in match
        } else returnFailureMessage(messageOrAnswer);
    }

    // HELPERS

    /**
     * Returns a success message for the given message with parameters.
     * @param returnTo The message to answer.
     * @param parameters The parameters to send back with the answer.
     */
    @Override
    public void returnSuccessMessage(SBProtocolMessage returnTo, String... parameters) {
        getClient().returnSuccessMessage(returnTo, parameters);
    }

    /**
     * Returns a failure message for the given message with parameters.
     * @param returnTo The message to answer.
     * @param parameters The parameters to send back with the answer.
     */
    @Override
    public void returnFailureMessage(SBProtocolMessage returnTo, String... parameters) {
        getClient().returnFailureMessage(returnTo, parameters);
    }

    // GETTERS & SETTERS

    public int getUserExists() {
        return userExists;
    }

    public void setUserExists(int userExists) {
        this.userExists = userExists;
    }

    private String getUsername() {
        return getClient().getUsername();
    }

    private void setUsername(String name) {
        getClient().setUsername(name);
    }

    private ClientFrame getFrame() {
        return getClient().getFrame();
    }

    private SBSocketManager getSocketManager() {
        return getClient().getSocketManager();
    }

    private SBProtocolManager getProtocolManager() {
        return getClient().getProtocolManager();
    }

    private UUID getUID() {
        return getClient().UID;
    }

    public Client getClient() {
        return client;
    }

    public boolean isLoggedIn() {
        return getClient().isLoggedIn();
    }

    public void setLoggedIn(boolean loggedIn) {
        getClient().setLoggedIn(loggedIn);
    }

}
