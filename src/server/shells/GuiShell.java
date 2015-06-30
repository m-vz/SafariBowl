package server.shells;

import server.ServerListener;
import server.display.ServerFrame;
import server.Server;
import java.util.logging.Level;

public class GuiShell implements ServerListener {

    private ServerFrame frame;
    private Server server;

    public static final Level LOG_OUTPUT_LEVEL = Level.INFO;

    public void setServer(Server server) {
        this.server = server;
    }

    public void teamsLoaded() {
        System.out.println("blah)");
        this.frame = new ServerFrame(this.server);
        this.frame.showServerPanel();
        this.frame.setVisible(true);
        this.frame.getServerPanel().setControlsEnabled(true);
    }

    public void started() {
        this.frame.getServerPanel().setServerRunning();
        this.frame.getServerPanel().focusPortField();
    }

    public void startException() {
        this.frame.getServerPanel().focusPortField();
    }

    public void stopped() {
        this.frame.getServerPanel().setServerStopped();
        this.frame.getServerPanel().focusPortField();
    }

    public void gotUserList (String userList) {
        if(userList.length() > 0) this.frame.getServerPanel().writeMessage(userList);
        else this.frame.getServerPanel().writeMessage("No users online right now.");
    }

    public void gotGameList (String gameList) {
        if(gameList.length() > 0) this.frame.getServerPanel().writeMessage(gameList);
        else this.frame.getServerPanel().writeMessage("No games on the server right now.");
    }

    public void log(Level level, String message) {
        if(this.frame != null)
            if(this.frame.getServerPanel() != null)
                if(level.intValue() >= LOG_OUTPUT_LEVEL.intValue()) this.frame.getServerPanel().writeMessage(message);
    }
}
