package util;

import gameLogic.GameController;
import gameLogic.TeamManager;
import network.SBSocketManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

/**
 * An abstract class to group client.Client and server.Server.
 * Created by milan on 28.3.15.
 */
public abstract class SBApplication {

    public boolean logmatches = true, autologin = false, automatchstart = false; // for debugging purposes

    public static final String MODERATOR_NAME = "Al Capone";
    public UUID UID;
    private Vector<String> finishedGames = new Vector<String>();
    private TeamManager teamManager;

    /**
     * Loads the available Teams from this jar
     */
    public void loadAvailableTeamsLocally() {
        try {
            File jarFile = new File(SBApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            if(jarFile.getAbsolutePath().endsWith(".jar")) {
                log(Level.INFO, "Loading Teams from jar.");
                File destination = new File(jarFile.getAbsolutePath().replaceAll(".jar$", UUID.randomUUID().toString().substring(0, 8))); // create temporary storage for whole jar
                ResourceManager.extractZipFile(jarFile, destination);
                File teamsDir = new File(destination.getAbsolutePath() + "/teams");
                teamManager = new TeamManager(this, teamsDir);
                ResourceManager.deleteDirectory(destination); // delete the temporary directory again
            } else {
                log(Level.INFO, "Loading Teams from dir.");
                URL teamsURL = SBApplication.class.getResource("/teams");
                if(teamsURL != null) teamManager = new TeamManager(this, new File(teamsURL.toURI()));
                else {
                    log(Level.SEVERE, "Could not find dir /teams");
                    teamManager = new TeamManager(this);
                }
            }
            log(Level.INFO, "Loaded teams.");
        } catch (IOException e) {
            log(Level.SEVERE, "Could not load available teams.");
            teamManager = new TeamManager(this);
        } catch (URISyntaxException e) {
            log(Level.SEVERE, "Could not load available teams because of misformed URL.");
            teamManager = new TeamManager(this);
        }
    }

    /**
     * Loads the available teams from a zip on a url and creates a team manager.
     * @param host The host where the zip file is stored.
     * @param file The path of the zip file on the host.
     */
    public void loadAvailableTeams(String host, String file) {
        URL teamsURL = null;
        try {
            teamsURL = new URL("http", host, 80, file);
        } catch (MalformedURLException e) {
            log(Level.SEVERE, "Could not load available teams. Make sure you are connected to the Internet and the URL is correct.");
            logStackTrace(e);
        }
        teamManager = new TeamManager(this, teamsURL);
    }

    /**
     * Reads logged games from file.
     * @param pathname The path to get the logged games file from.
     */
    protected void getLoggedGames(String pathname) {
        // read games from log
        File gamesFile = new File(pathname);
        if(gamesFile.exists()) { // only read file if it exists
            try {
                BufferedReader reader = new BufferedReader(new FileReader(gamesFile));
                String gameString;
                while((gameString = reader.readLine()) != null)
                    addFinishedGame(new FinishedGame(gameString));
                log(Level.INFO, "Welcome back!");
            } catch (IOException e) {
                log(Level.SEVERE, "Could not read games file. Shutting down not to accidentally overwrite games in unread file.");
                System.exit(-1);
            }
        } else log(Level.INFO, "Al Capone: Seems like you're new to SafariBowl! Welcome!");
    }

    public void addFinishedGame(String game) {
        this.finishedGames.add(game);
    }

    public void addFinishedGame(GameController game) {
        this.finishedGames.add(game.toLogString());
    }

    /**
     * Get a clone of the finished games vector (to iterate over, etc.).
     * @return A clone of the finished games vector.
     */
    @SuppressWarnings("unchecked")
    public Vector<String> getFinishedGames() {
        return (Vector<String>) finishedGames.clone();
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public abstract SBSocketManager getSocketManager();

    public UUID getUID() {
        return UID;
    }

    public abstract void processAnswers();

    public abstract void processMessages();

    public abstract void logOutUserWithUID(UUID UID);

    public abstract void logOutAllUsers();

    public abstract void lostConnection();

    public abstract void isConnecting();

    public abstract void hasConnected(InetAddress address, int port, boolean connected);

    public abstract boolean checkIfUserExists(String name);

    public abstract void log(Level level, String message);

    /**
     * Log all elements of a stack trace.
     * @param e An Exception whose stack trace to print.
     */
    public void logStackTrace(Exception e) {
        for(StackTraceElement element: e.getStackTrace())
            log(Level.SEVERE, element.toString());
    }
}
