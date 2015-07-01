package server;

import java.util.logging.Level;
import server.Server;

/**
 * Defines the events a server shell needs to implement.
 * @since   2015-06-26
 */
public interface ServerListener {

    /**
     * Set the server instance used by the ServerListener.
     *
     * @param  server SafariBowl server instance.
     */
    public void setServer(Server server);

    /**
     * Called after the server is setup and ready to run.
     */
    public void teamsLoaded();

    /**
     * Called after the server started successfuly.
     */
    public void started();

    /**
     * Called after a startup error.
     */
    public void startException();

    /**
     * Called after the server stopped successfuly.
     */
    public void stopped();

    /**
     * Called after a new userlist was requested.
     *
     * @param userList Newline delimited userlist.
     */
    public void gotUserList(String userList);

    /**
     * Called after a new gamelist was requested.
     *
     * @param userList Newline delimited gamelist.
     */
    public void gotGameList(String gameList);

    /**
     * Called after a new gamelist was requested.
     *
     * @param level Loglevel.
     * @param message Logmessage.
     */
    public void log(Level level, String message);
}
