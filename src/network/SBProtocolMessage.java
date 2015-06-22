package network;

import util.SBLogger;

import java.util.Date;
import java.util.UUID; // A UUID has the hexadecimal form XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX, e.g. 550e8400-e29b-11d4-a716-446655440000.
import java.util.logging.Level;

/**
 * Object representation of a protocol message that is being sent from socket to socket.
 * Created by milan on 24.3.15.
 */
public class SBProtocolMessage {

    private static final SBLogger L = new SBLogger(SBProtocolMessage.class.getName(), util.SBLogger.LOG_LEVEL);
    // GAME ACTIO STRINGS
    public static final String ACTIO_SET_TEAM = "SET TEAM";
    public static final String ACTIO_SET_PLAYER = "SET PLAYER";
    public static final String ACTIO_MOVE = "MOVE";
    public static final String ACTIO_THRW = "THRW";
    public static final String ACTIO_BLCK = "BLCK";
    public static final String ACTIO_SPCL = "SPCL";
    // GAME EVENT STRINGS
    public static final String EVENT_END_TURN = "END TURN";
    public static final String EVENT_INITIATE_KICK = "INITIATE KICK";
    public static final String EVENT_GIVE_THE_BALL_TO_SOMEONE = "GIVE THE BALL TO SOMEONE";
    public static final String EVENT_SETUP_YOUR_TEAM = "SETUP YOUR TEAM";
    public static final String EVENT_OPPONENT_SURRENDERED = "OPPONENT SURRENDERED";
    public static final String EVENT_WON_GAME = "WON GAME";
    public static final String EVENT_LOST_GAME = "LOST GAME";
    public static final String EVENT_DRAW = "THE GAME ENDED IN A DRAW";
    public static final String EVENT_SEND_NEW_TEAM = "SEND NEW TEAM";
    public static final String EVENT_SEND_PLAYER = "SEND PLAYER";
    public static final String EVENT_SEND_ROUND_COUNT = "SEND ROUND COUNT";
    public static final String EVENT_SEND_BALL_POS = "SEND BALL POS";
    public static final String EVENT_SEND_SCORE = "SEND SCORE";
    public static final String EVENT_SEND_GAME_PHASE = "SEND GAME PHASE";
    public static final String EVENT_SEND_MOVING_PLAYER = "SEND MOVING PLAYER";
    public static final String EVENT_MAKE_PLAYER_TURN = "MAKE PLAYER TURN";
    public static final String EVENT_WHAT_DIE = "WHAT DIE?";
    public static final String EVENT_BALL_CATCHED = "BALL CATCHED";
    public static final String EVENT_BALL_NOT_CATCHED = "BALL NOT CATCHED";
    public static final String EVENT_FOLLOW = "FOLLOW?";
    public static final String EVENT_ALL_PLAYERS_SET = "ALL PLAYERS SET";
    public static final String EVENT_YOUR_ENEMY_IS_TACKLED = "YOUR ENEMY IS TACKLED";
	public static final String EVENT_WHICH_DIRECTION = "WHICH DIRECTION?";
    public static final String EVENT_BALL_THROWN_IN_BY_THE_CROWD = "BALL THROWN IN BY THE CROWD";
    public static final String EVENT_ENDED_TURN = "ENDED TURN";
    public static final String EVENT_YOUR_TURN = "YOUR TURN";
    public static final String EVENT_CROWD_BEATS_UP_YOUR_PLAYER = "CROWD BEATS UP YOUR PLAYER";
    public static final String EVENT_SHOW_ME = "SHOW ME";
    public static final String EVENT_API_AIM = "API AIM";
    public static final String EVENT_API_CHOICE = "API CHOICE";
    public static final String EVENT_API_FIELD = "API FIELD";
    public static final String EVENT_SEND_BLITZ_PASS_FOUL = "SEND BLITZ PASS FOUL";
    public static final String EVENT_SEND_WEATHER = "SEND WEATHER";
    public static final String EVENT_ASK_FOR_INTERCEPTOR = "$intercept";
    public static final String EVENT_API_HIGHLIGHT = "API HIGHLIGHT";
    // SUCCESS ANSWER STRINGS
    public static final String WORKD_DIE_CHOSEN = "DIECHOSEN";
    public static final String WORKD_DECIDED = "DECIDED";
    public static final String WORKD_PLAYER_SET = "PLAYER SET";
    public static final String WORKD_TEAMSETUP_SUCCESSFUL = "TEAMSETUP SUCCESSFUL";
    public static final String WORKD_PICKED_UP_BALL = "PICKED UP BALL";
    public static final String WORKD_FAILED_PICK_UP_BALL = "FAILED PICK UP BALL";
    public static final String WORKD_YOU_HAVE_LOST_YOUR_BALLS = "YOU HAVE LOST YOUR BALLS";
    public static final String WORKD_YOU_ARE_TACKLED = "YOUR ARE TACKLED";
    public static final String WORKD_PLAYER_IS_NOW_FINE = "PLAYER IS NOW FINE";
    public static final String WORKD_PLAYER_IS_NOW_PRONE = "PLAYER IS NOW PRONE";
    public static final String WORKD_DEFENDER_PUSHED = "DEFENDER PUSHED";
    public static final String WORKD_NOT_WELL_THROWN = "NOT WELL THROWN";
    public static final String WORKD_SUCCESSFUL_THROW = "SUCCESSFUL THROW";
    public static final String WORKD_GIVE_BALL = "GIVEBALL";
    public static final String WORKD_KICK = "KICK";
	public static final String WORKD_DIRECTION = "DIRECTION";
	public static final String WORKD_FOLLOWED = "YOU FOLLOWED HIM";
	public static final String WORKD_NOTFOLLOWED = "YOU STAYED AT YOUR PLACE";
	public static final String WORKD_PLAYER_HAS_BEEN_SENT_OFF_THE_PITCH_BY_REFEREE = "PLAYER HAS BEEN SENT OFF THE PITCH BY REFEREE";
    // FAILURE ANSWER STRINGS
    public static final String FAILD_PARAMANIA_HAS_TAKEN_OVER = "PARAMANIA HAS TAKEN OVER";
    public static final String FAILD_WHERE_IS_THE_GURU = "WHERE IS THE GURU";
    public static final String FAILD_PLAYER_DOESNT_EXIST = "PLAYER DOESNT EXIST";
    public static final String FAILD_PATH_NOT_REACHABLE = "PATH NOT REACHABLE";
    public static final String FAILD_GIMME_AN_INT = "GIMME_AN_INT";
    public static final String FAILD_THIS_IS_NO_VALID_COORDINATE = "THIS IS NO VALID COORINATE";
    public static final String FAILD_WRONG_SIDE = "WRONG SIDE";
    public static final String FAILD_NOT_ON_THE_PITCH = "NOT ON THE PITCH";
    public static final String FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION = "PLAYER IS NOT IN A GOOD CONDITION";
    public static final String FAILD_FIELD_ALREADY_TAKEN = "FIELD ALREADY TAKEN";
    public static final String FAILD_SOME_PLAYERS_DONT_EXIST = "SOME PLAYERS DONT EXIST"; 
    public static final String FAILD_NO_SUCH_TEAM_TYPE = "NO SUCH TEAM TYPE"; 
    public static final String FAILD_YOUR_TEAM_IS_ALREADY_SET = "YOUR TEAM IS ALREADY SET";
    public static final String FAILD_GIVE_A_VALID_PLAYER_INDEX = "GIVE A VALID PLAYER INDEX";
    public static final String FAILD_YOU_DONT_BELONG_HERE = "YOU DONT BELONG HERE";
    public static final String FAILD_BLOCKING_NOT_POSSIBLE = "BLOCKING NOT POSSIBLE";
    public static final String FAILD_NO_BLITZ_LEFT = "NO BLITZ LEFT";
    public static final String FAILD_PLAYER_CANNOT_TAKE_ACTION = "PLAYER CANNOT TAKE ACTION";
    public static final String FAILD_YOU_ARE_EXHAUSTED = "YOU ARE EXHAUSTED";
    public static final String FAILD_PATH_IS_BLOCKED = "PATH IS BLOCKED";
    public static final String FAILD_PATH_TOO_LONG = "PATH TOO LONG";
    public static final String FAILD_CANT_BACKUP_FOR_SOME_ODD_REASON = "CANT BACKUP FOR SOME ODD REASON";
    public static final String FAILD_PUSHING_NOT_POSSIBLE = "PUSHING NOT POSSIBLE";
    public static final String FAILD_TOO_FAR_AWAY = "TOO FAR AWAY";
    public static final String FAILD_NO_PASS_LEFT = "NO PASS LEFT";
    public static final String FAILD_PLAYER_DOESNT_HOLD_THE_BALL = "PLAYER DOESNT HOLD THE BALL";
    public static final String FAILD_PLAYER_IS_NOT_ON_THE_PITCH = "PLAYER IS NOT ON THE PITCH";
    public static final String FAILD_RECEIVED_WRONG_GAME_DATA = "RECEIVED WRONG GAME DATA";
    public static final String FAILD_NOT_YOUR_TURN = "NOT YOUR TURN";
    public static final String FAILD_WRONG_GAME_PHASE = "WRONG GAME PHASE";
	public static final String FAILD_WRONG_DIRECTION = "WRONG DIRECTION";
	public static final String FAILD_INVALID_TEAMSETUP = "INVALID TEAMSETUP";
	public static final String FAILD_NOT_ENOUGH_PLAYERS = "NOT ENOUGH PLAYERS";
	public static final String FAILD_NO_FOUL_LEFT = "NO FOUL LEFT";
	public static final String FAILD_NOT_ENOUGH_MONEY = "NOT ENOUGH MONEY";
	public static final String FAILD_NO_SUCH_SPECIAL_RULE = "NO SUCH SPECIAL RULE";
	public static final String FAILD_PLAYER_IS_BANNED_FROM_THE_GAME = "PLAYER IS BANNED FROM THE GAME";

    private UUID UID;
    private UUID MID;
    private SBProtocolCommand.SBProtocolModule module;
    private SBProtocolCommand command;
    private SBProtocolParameterArray parameters;
    private Date sentDate = new Date(0);
    private SBSocket socket; // the socket to return answers to. Is being set after message has been received by a socket.

    /**
     * Create a new protocol Message as user with the ID UID.
     * @param UID The UID of the user sending this message.
     * @param command The command for the protocol message.
     * @param parameters An string array with the parameters for the protocol message.
     */
    public SBProtocolMessage(UUID UID, SBProtocolCommand command, String... parameters) {
        this.UID = UID;
        this.MID = UUID.randomUUID();
        this.module = command.getModule();
        this.command = command;
        this.parameters = new SBProtocolParameterArray(parameters);
    }

    /**
     * Create a new protocol Message as user with the ID UID.
     * @param UID The UID of the user sending this message.
     * @param command The command for the protocol message.
     * @param parameters An protocol array with the parameters for the protocol message.
     */
    public SBProtocolMessage(UUID UID, SBProtocolCommand command, SBProtocolParameterArray parameters) {
        this.UID = UID;
        this.MID = UUID.randomUUID();
        this.module = command.getModule();
        this.command = command;
        this.parameters = parameters;
    }

    /**
     * Create a new protocol Message as user with the ID UID and a MID.
     * @param UID The UID of the user sending this message.
     * @param MID The message ID of this message.
     * @param command The command for the protocol message.
     * @param parameters An protocol array with the parameters for the protocol message.
     */
    public SBProtocolMessage(UUID UID, UUID MID, SBProtocolCommand command, SBProtocolParameterArray parameters) {
        this.UID = UID;
        this.MID = MID;
        this.module = command.getModule();
        this.command = command;
        this.parameters = parameters;
    }

    public SBProtocolMessage(SBProtocolMessage answer) {
		this.UID = answer.UID;
		this.MID = answer.MID;
		this.module = answer.module;
		this.command = answer.command;
        this.parameters = answer.parameters;
        this.socket = answer.socket;
	}

	/**
     * Parse a message from a string.
     * @param message The message string to parse. Null if the message if malformed
     * @return The parsed protocol message.
     */
    public static SBProtocolMessage fromString(String message) {
        // modules: CHT, AUT, GAM, PNG, SUC, FAI
        // commands:
        //   module CHT:    SENDM/CHT, BDCST/CHT
        //   module AUT:    LOGUT/AUT, LOGIN/AUT, SGNUP/AUT, CHNGE/AUT, RMUSR/AUT, OPUSR/AUT
        //   module GAM:    ACTIO/GAM, START/GAM, SRNDR/GAM, EVENT/GAM
        //   other modules: HELLO/PNG, WORKD/SUC, FAILD/FAI,

        // e.g. XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX AUT LOGIN ["peter", "6d8aa0c02bb914a4c47d27f563fa3dca"]
        //      |0                                  |36                                  |73 |77   |83

        try {
            // first of all check if message is not null
            if (message != null) {
                // parse UID
                UUID UID;
                try {
                    UID = UUID.fromString(message.substring(0, 36));
                } catch (IllegalArgumentException e) {
                    // UID is malformed
                    L.log(Level.SEVERE, "Tried to parse malformed message. Ignoring it.");
                    return null;
                }

                // parse MID
                UUID MID;
                try {
                    MID = UUID.fromString(message.substring(37, 73));
                } catch (IllegalArgumentException e) {
                    // MID is malformed
                    L.log(Level.SEVERE, "Tried to parse malformed message. Ignoring it.");
                    return null;
                }

                // parse module and command
                SBProtocolCommand command;
                String moduleString = message.substring(74, 77);
                String commandString = message.substring(78, 83);
                try {
                    command = SBProtocolCommand.valueOf(commandString);
                } catch (IllegalArgumentException e) {
                    L.log(Level.SEVERE, "Tried to parse malformed message. Ignoring it.");
                    return null;
                }

                // parse parameters if there are any
                SBProtocolParameterArray parameters;
                if (!message.substring(83).equals("")) {
                    parameters = SBProtocolParameterArray.paraception(message.substring(84));
                    if (parameters == null) { // paraception returned null
                        L.log(Level.SEVERE, "Tried to parse malformed message. Ignoring it.");
                        return null;
                    }
                } else {
                    // save empty parameter array
                    parameters = new SBProtocolParameterArray();
                }

                // return parsed message
                return new SBProtocolMessage(UID, MID, command, parameters);
            } else return null;
        } catch(StringIndexOutOfBoundsException e) {
            L.log(Level.SEVERE, "Some strange client tried to connect. No chance!");
            return null;
        }
    }

    /**
     * Create a string in SBProcotol-standard format to send.
     * @return The message as a string.
     */
    public String toString() {
        return UID+" "+MID+" "+command.toMessageString()+" "+parameters;
    }

    /**
     * Create a string in SBProcotol-standard format but with shortened UUIDs for better readability.
     * @return The message as a string with shortened UUIDs.
     */
    public String toStringShortenUUID() {
        return UID.toString().substring(0, 8)+" "+MID.toString().substring(0, 8)+" "+command.toMessageString()+" "+parameters.toStringUnescaped();
    }

    /**
     * Get the parameter at index i.
     * @param i The index of the parameter to return.
     * @return The parameter at index i.
     */
    public SBProtocolParameter getParameter(int i) {
        return parameters.getParameter(i);
    }

    /**
     * Get the content of the parameter at index i.
     * @param i The index of the parameter whose content to return.
     * @return The content of the parameter at index i.
     */
    public String getParameterContent(int i) {
        return parameters.getParameter(i).getContent();
    }

    /**
     * Get all parameters as a parameter array.
     * @return the parameter array with all parameters in it
     */
    public SBProtocolParameterArray getParameters() {
        return parameters;
    }

    /**
     * Create a new ping message.
     * @param UID The UID of the sender.
     * @return The ping message.
     */
    public static SBProtocolMessage createPingMessage(UUID UID) {
        return new SBProtocolMessage(UID, SBProtocolCommand.PIING, new SBProtocolParameterArray());
    }

    /**
     * Create a new ping answer for an existing ping MID
     * @param UID The UID of the sender.
     * @param MID The MID of the original ping message.
     * @return The ping answer for the ping message with the given MID.
     */
    public static SBProtocolMessage createPingAnswer(UUID UID, UUID MID) {
        return new SBProtocolMessage(UID, MID, SBProtocolCommand.POONG, new SBProtocolParameterArray());
    }

    /**
     * Create a new success message to answer a certain message with MID.
     * @param UID The UID of the user sending the message.
     * @param MID The MID of the message that is being answered.
     * @param reply The success reply for the message with MID.
     * @return The created success message
     */
    public static SBProtocolMessage createSuccessMessage(UUID UID, UUID MID, SBProtocolParameterArray reply) {
        // add the MID of the message that is being answered to the start of the reply parameters
        reply.addParameter(0, new SBProtocolParameter(MID.toString()));
        // return the message
        return new SBProtocolMessage(UID, SBProtocolCommand.WORKD, reply);
    }

    /**
     * Create a new failure message to answer a certain message with MID.
     * @param UID The UID of the user sending the message.
     * @param MID The MID of the message that is being answered.
     * @param reply The failure reply for the message with MID.
     * @return the created failure message
     */
    public static SBProtocolMessage createFailureMessage(UUID UID, UUID MID, SBProtocolParameterArray reply) {
        // add the MID of the message that is being answered to the start of the reply parameters
        reply.addParameter(0, new SBProtocolParameter(MID.toString()));
        // return the message
        return new SBProtocolMessage(UID, SBProtocolCommand.FAILD, reply);
    }

    /**
     * Return a new failure message sent from a given UID with parameters.
     * @param UID The UID of the sender.
     * @param params The params to send with the answer.
     */
    public void returnFailureMessage(UUID UID, SBProtocolParameterArray params) {
        if(getSocket() != null) getSocket().sendMessage(createFailureMessage(UID, getMID(), params));
    }

    /**
     * Return a new success message sent from a given UID with parameters.
     * @param UID The UID of the sender.
     * @param params The params to send with the answer.
     */
    public void returnSuccessMessage(UUID UID, SBProtocolParameterArray params) {
        if(getSocket() != null) getSocket().sendMessage(createSuccessMessage(UID, getMID(), params));
    }

    /**
     * Return a new success message sent from a given UID with parameters.
     * @param UID The UID of the sender.
     * @param params The params to send with the answer.
     */
    public void returnSuccessMessage(UUID UID, String... params) {
        if(getSocket() != null) getSocket().sendMessage(createSuccessMessage(UID, getMID(), new SBProtocolParameterArray(params)));
    }

    /**
     * Return a new failure message sent from a given UID.
     * @param UID The UID of the sender.
     */
    public void returnFailureMessage(UUID UID) {
        if(getSocket() != null) getSocket().sendMessage(createFailureMessage(UID, getMID(), new SBProtocolParameterArray()));
    }

    /**
     * Return a new success message sent from a given UID.
     * @param UID The UID of the sender.
     */
    public void returnSuccessMessage(UUID UID) {
        if(getSocket() != null) getSocket().sendMessage(createSuccessMessage(UID, getMID(), new SBProtocolParameterArray()));
    }

    /**
     * Return an answer to this message.
     * @param message The answer to return.
     */
    public void returnMessage(SBProtocolMessage message) {
        if(getSocket() != null) getSocket().sendMessage(message);
    }

    // getters and setters


    public SBSocket getSocket() {
        return socket;
    }

    public void setSocket(SBSocket socket) {
        this.socket = socket;
    }

    public UUID getUID() {
        return UID;
    }

    public void setUID(UUID UID) {
        this.UID = UID;
    }

    public UUID getMID() {
        return MID;
    }

    public SBProtocolCommand getCommand() {
        return command;
    }

    /**
     * Set the command and its corresponding module.
     * @param command The command to set.
     */
    public void setCommand(SBProtocolCommand command) {
        this.command = command;
        this.module = command.getModule();
    }

    public SBProtocolCommand.SBProtocolModule getModule() {
        return module;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }
}
