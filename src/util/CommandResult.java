package util;

import util.CommandParser.Command;

/**
 * Holds the result of a command parse action.
 * Message and recipient may be null.
 * @since   2015-07-01
 */
public class CommandResult {

    private Command commandType;
    private String recipient;
    private String message;

    public CommandResult(Command commandType, String recipient, String message) {
        this.commandType = commandType;
        this.recipient = recipient;
        this.message = message;
    }

    public Command getCommandType() {
        return this.commandType;
    }

    public String getRecipient() {
        return this.recipient;
    }

    public String getMessage() {
        return this.message;
    }
}
