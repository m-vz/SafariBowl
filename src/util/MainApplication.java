package util;

import client.Client;
import network.SBSocketManager;
import server.Server;

import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Level;

/**
 * The main application that can be started as server or client.
 * Created by milan on 9.4.15.
 */
public class MainApplication extends SBApplication {

    private static final SBLogger L = new SBLogger(MainApplication.class.getName(), util.SBLogger.LOG_LEVEL);
    private static SBApplication serverOrClient;

    public static void main(String[] args) {
        if(args.length == 2) {
            if(args[0].equals("server")) {
                try {
                    int port = Integer.parseInt(args[1]);
                    Server server = new Server();
                    serverOrClient = server;
                    server.runServer();
                    server.start(port);
                } catch(NumberFormatException e) {
                    L.log(Level.SEVERE, "Illegal port. Run with 'server <listenport>'.");
                    System.exit(-1);
                }
            } else if(args[0].equals("client")) {
                try {
                    int port = Integer.parseInt(args[1].replaceAll("^[^:]+:", ""));
                    String address = args[1].replaceAll(":.*$", "");
                    Client client = new Client();
                    serverOrClient = client;
                    client.runClient();
                    client.connect(address, port);
                } catch(NumberFormatException e) {
                    L.log(Level.SEVERE, "Illegal port. Run with 'client <serverip>:<serverport>'.");
                    System.exit(-1);
                }
            } else {
                L.log(Level.SEVERE, "Unknown command. Run with 'server <listenport>' or 'client <serverip>:<serverport>'.");
                System.exit(-1);
            }
        } else {
            L.log(Level.SEVERE, "Wrong number of arguments. Run with 'server <listenport>' or 'client <serverip>:<serverport>'.");
            System.exit(-1);
        }
    }

    @Override
    public SBSocketManager getSocketManager() {
        return serverOrClient.getSocketManager();
    }

    @Override
    public void processAnswers() {
        serverOrClient.processAnswers();
    }

    @Override
    public void processMessages() {
        serverOrClient.processMessages();
    }

    @Override
    public void logOutUserWithUID(UUID UID) {
        serverOrClient.logOutUserWithUID(UID);
    }

    @Override
    public void logOutAllUsers() {
        serverOrClient.logOutAllUsers();
    }

    @Override
    public void lostConnection() {
        serverOrClient.lostConnection();
    }

    @Override
    public void isConnecting() {
        serverOrClient.isConnecting();
    }

    @Override
    public void hasConnected(InetAddress address, int port, boolean connected) {
        serverOrClient.hasConnected(address, port, connected);
    }

    @Override
    public boolean checkIfUserExists(String name) {
        return serverOrClient.checkIfUserExists(name);
    }

    @Override
    public void log(Level level, String message) {
        serverOrClient.log(level, message);
    }
}
