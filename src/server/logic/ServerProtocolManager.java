package server.logic;

import network.SBProtocolManager;
import util.SBApplication;

/**
 * A protocol manager that will communicate with a client socket manager and handle messages for a server.
 * Created by milan on 28.3.15.
 */
public class ServerProtocolManager extends SBProtocolManager {

    /**
     * Create new server protocol manager.
     * @param parent The server that is parent of this protocol manager.
     */
    public ServerProtocolManager(SBApplication parent) {
        super(parent);
    }

}
