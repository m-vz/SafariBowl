package server.shells;

import server.ServerListener;
import server.Server;
import java.util.logging.Level;

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
    }

    public void startException() {
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
        if(level.intValue() >= LOG_OUTPUT_LEVEL.intValue()){
            System.out.println(message);
        }
    }
}
