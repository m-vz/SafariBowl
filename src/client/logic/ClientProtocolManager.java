package client.logic;

import network.SBProtocolManager;
import util.SBApplication;

/**
 * A protocol manager that will communicate with a server socket manager and handle messages for a client.
 * Created by milan on 28.3.15.
 */
public class ClientProtocolManager extends SBProtocolManager {

    /**
     * Create new client protocol manager.
     * @param parent The client that is parent of this protocol manager.
     */
    public ClientProtocolManager(SBApplication parent) {
        super(parent);
    }

}
