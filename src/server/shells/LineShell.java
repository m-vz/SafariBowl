package server.shells;

import server.ServerListener;
import server.Server;
import network.SBNetworkException;
import util.CommandParser;

import java.util.logging.Level;
import java.util.Scanner;

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

                CommandParser.parse(rawCmd, server);
            }
        }
    }
}
