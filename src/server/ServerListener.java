package server;

import java.util.logging.Level;
import server.Server;

public interface ServerListener {
    public void setServer(Server server);
    public void teamsLoaded();
    public void started();
    public void startException();
    public void stopped();
    public void gotUserList(String userList);
    public void gotGameList(String gameList);
    public void log(Level level, String message);
}
