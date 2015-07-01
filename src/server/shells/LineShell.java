package server.shells;

import java.util.logging.Level;
import java.util.Scanner;
import server.ServerListener;
import server.Server;

public class LineShell implements ServerListener {

    private Server server;
    private int port;

    public static final Level LOG_OUTPUT_LEVEL = Level.INFO;

    public LineShell(int port) {
        this.port = port;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public void teamsLoaded() {
        this.server.start(this.port);
    }

    public void started() {
        (new Thread(new LineReader())).start();
    }

    public void startException(SBNetworkException e) {
    }

    public void stopped() {
    }

    public void gotUserList (String userList) {
        if(userList.length() > 0) System.out.println(userList);
        else System.out.println("No users online right now.");
    }

    public void gotGameList (String gameList) {
        if(gameList.length() > 0) System.out.println(gameList);
        else System.out.println("No games on the server right now.");
    }

    public void log(Level level, String message) {
        if(level.intValue() >= LOG_OUTPUT_LEVEL.intValue()) {
            System.out.println(message);
        }
    }

    class LineReader implements Runnable {

        public void run() {
            Scanner scanner = new Scanner(System.in);

            //noinspection InfiniteLoopStatement
            while (true) {
                System.out.print("sb> ");
                String rawCmd = scanner.next();

                parseCmd(rawCmd);
            }
        }

        private void parseCmd(String rawCmd) {
            String message, recipient;

            if(rawCmd.startsWith("@")) {
                // "@milan hello there!" -> message = "hello there!" recipient = "milan"
                message = rawCmd.replaceAll("^@\\S+", "");
                if(message.length() > 1) {
                    message = message.substring(1, message.length());
                    recipient = rawCmd.substring(1, rawCmd.length() - message.length() - 1);
                }
                else return; // don't send if message was empty
            } else {
                // check if it is a command
                if(rawCmd.toLowerCase().equals("/games") || rawCmd.toLowerCase().equals("/g")) { // get games list command
                    server.log(Level.INFO, "Getting games list.");
                    server.getGamesList();

                    return;
                } else if(rawCmd.toLowerCase().equals("/users") || rawCmd.toLowerCase().equals("/u")) { // get users list command
                    server.log(Level.INFO, "Getting list of users online.");
                    server.getUsersList();

                    return;
                } else if(rawCmd.toLowerCase().equals("/exit") || rawCmd.toLowerCase().equals("/quit")) { // quit application
                    System.exit(0);

                    return;
                } else if(rawCmd.toLowerCase().startsWith("/cheat ")) { // get begin cheating
                    server.cheat(rawCmd.toLowerCase().substring(7));
                    return;
                } else {
                    message = rawCmd;
                    recipient = "all";
                }
            }
            server.chat(recipient.toLowerCase().trim(), message);
        }
    }
}
