package util;

import server.Server;

import java.util.logging.Level;

/**
 * A helper class for command parsing to eliminate the need of every shell reimplementing it.
 * @since   2015-07-01
 */
public class CommandParser {

    public enum Command {
        INVALID,
        MESSAGE,
        MESSAGE_ALL,
        GAMES,
        USERS,
        CHEAT
    }

    /**
     * Parse a command string and run the appropriate actions.
     *
     * @param  cmd Raw command string.
     * @param  server Server instance on which to run the actions.
     * @return A CommandResult instance. Both message and recipient may be null.
     */
    public static CommandResult parse(String cmd, Server server) {
        String message = null, recipient = null;

        Command commandType = Command.INVALID;

        if (cmd != null && !cmd.isEmpty()) {
            if(cmd.startsWith("@")) {
                // "@milan hello there!" -> message = "hello there!" recipient = "milan"
                message = cmd.replaceAll("^@\\S+", "");
                if(message.length() > 1) {
                    message = message.substring(1, message.length());
                    recipient = cmd.substring(1, cmd.length() - message.length() - 1);

                    commandType = Command.MESSAGE;
                }
            } else {
                // check if it is a command
                if(cmd.toLowerCase().equals("/games") || cmd.toLowerCase().equals("/g")) { // get games list command
                    server.log(Level.INFO, "Getting games list.");
                    server.getGamesList();

                    commandType = Command.GAMES;
                } else if(cmd.toLowerCase().equals("/users") || cmd.toLowerCase().equals("/u")) { // get users list command
                    server.log(Level.INFO, "Getting list of users online.");
                    server.getUsersList();

                    commandType = Command.USERS;
                } else if(cmd.toLowerCase().startsWith("/cheat ")) { // get begin cheating
                    server.cheat(cmd.toLowerCase().substring(7));

                    commandType = Command.CHEAT;
                } else {
                    message = cmd;
                    recipient = "all";

                    commandType = Command.MESSAGE_ALL;
                }
            }

            if (recipient != null && !recipient.isEmpty()) {
                server.chat(recipient.toLowerCase().trim(), message);
            }
        }

        return new CommandResult(commandType, recipient, message);
    }
}
