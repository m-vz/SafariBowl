package gameLogic;

import server.Server;
import util.ResourceManager;
import util.SBApplication;

import javax.imageio.ImageIO;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

/**
 * The manager that reads, processes and creates teams from external files.
 */
public class TeamManager {

    private SBApplication parent;
    private static final ScriptEngineManager manager = new ScriptEngineManager();
    private HashMap<String, Invocable> invocables = new HashMap<String, Invocable>();
    private Vector<Team> teamBlueprints = new Vector<Team>();
    private String loadingMessage = "Loading teams";

    /**
     * Create a team manager that reads teams from a remote zip file.
     * @param parent The parent of the team manager.
     * @param teamsURL The URL to read the teams zip from.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public TeamManager(SBApplication parent, URL teamsURL) {

        this.parent = parent;

        // load team files
        File tempFile = new File("data/"+(int)(Math.random()*1000)+"teams.zip"), destinationFile;
        if(this.parent instanceof Server) destinationFile = new File("data/server");
        else destinationFile = new File("data/client");

        if(teamsURL != null) { // url exists
            DataInputStream teamsStream = null;
            try {
                teamsStream = new DataInputStream(teamsURL.openStream());
            } catch (IOException e) {
                getParent().logStackTrace(e);
            }

            if(teamsStream != null) { // stream was successfully opened
                try {
                    tempFile.delete(); // delete old temp file
                    Files.copy(teamsStream, Paths.get(tempFile.toURI()));
                } catch (IOException e) {
                    getParent().logStackTrace(e);
                }
            }
        }

        destinationFile.delete(); // delete old teams
        ResourceManager.extractZipFile(tempFile, destinationFile);
        tempFile.delete(); // delete temp file

        setUp(new File(destinationFile.getPath()+"/teams"));

    }

    /**
     * Create a team manager that reads teams from a local directory.
     * @param parent The parent of the team manager.
     * @param teamsDir The directory to read the teams from.
     */
    public TeamManager(SBApplication parent, File teamsDir) {

        this.parent = parent;
        setUp(teamsDir);

    }

    /**
     * Constructor to create team manager without loading teams if teams folder was not found.
     * @param parent The parent of the team manager.
     */
    public TeamManager(SBApplication parent) {

        this.parent = parent;

    }

    private void setUp(File teamsDir) {

        String filename = "NOTAFILE";

        try {

            HashMap<String, BufferedImage> teamImages = new HashMap<String, BufferedImage>();

            // handle team files
            if(teamsDir != null) {
                //noinspection ConstantConditions
                for (File teamDir: teamsDir.listFiles()) {
                    if (teamDir.isDirectory()) {
                        String teamType = teamDir.getName();
                        Team createdTeam = createTeam(teamType);

                        //noinspection ConstantConditions
                        for (File teamFile : teamDir.listFiles()) {
                            filename = addSpacesToName(teamFile.getName());

                            if (filename.toLowerCase().endsWith(".js")) { // is player js file

                                // create new engine for script
                                ScriptEngine engine = manager.getEngineByName("JS");
                                engine.eval(new FileReader(teamFile.getAbsolutePath()));
                                // add invocable for script and create player
                                String name = addSpacesToName(teamFile.getName().replaceAll("\\.[^\\.]+$", ""));
                                invocables.put(name, (Invocable) engine);
                                createPlayer(name, createdTeam);

                            } else if (filename.toLowerCase().endsWith(".png")) { // is image

                                try {
                                    String imageName = filename.substring(0, filename.length() - ".png".length());
                                    teamImages.put(imageName, ImageIO.read(teamFile));
                                } catch (IOException e) {
                                    getParent().log(Level.WARNING, "IOException while reading team image " + teamFile.getName() + ".");
                                }

                            }
                        }

                        teamBlueprints.add(createdTeam);
                    }
                }

                // handle player images
                int w = ResourceManager.IMAGE_WIDTH, h = ResourceManager.IMAGE_HEIGHT;
                for (Team team : teamBlueprints) {
                    for (Player player : team.availablePlayers) {

                        // get sprites
                        BufferedImage imageR = teamImages.get(player.getName() + " R");
                        BufferedImage imageL = teamImages.get(player.getName() + " L");

                        // resize if needed and add as sprites
                        if (imageR != null) {
                            if (imageR.getWidth() != w || imageR.getHeight() != h) imageR = resize(imageR, w, h);
                            player.setSpriteR(imageR);
                        } else player.setSpriteR(ResourceManager.DEFAULT_PLAYER_R);
                        if (imageL != null) {
                            if (imageL.getWidth() != w || imageL.getHeight() != h) imageL = resize(imageL, w, h);
                            player.setSpriteL(imageL);
                        } else player.setSpriteL(ResourceManager.DEFAULT_PLAYER_L);

                    }
                }
            } else {
                getParent().log(Level.SEVERE, "Could not load available teams. Make sure you are connected to the Internet.");
            }

        } catch (FileNotFoundException e) {
            getParent().logStackTrace(e);
        } catch (NullPointerException e) {
            getParent().logStackTrace(e);
        } catch (ScriptException e) {
            getParent().log(Level.SEVERE, "Exception in script " + filename + ".");
            getParent().logStackTrace(e);
            System.exit(-1);
        }

    }

    private Team createTeam(String type) {
        getParent().log(Level.INFO, (loadingMessage += ".."));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        return new Team(type);
    }

    private void createPlayer(String name, Team teamCreatedFor) {
        getParent().log(Level.INFO, (loadingMessage += ".."));
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        teamCreatedFor.addAvailablePlayer(new Player(name, this));
    }

    public Object invokeFunction(String functionName, Player actor, Object... params) {
        Invocable invocable = invocables.get(actor.getName());
        try {
            Object[] paramsWithActor = null;
            if(params != null) {
                paramsWithActor = new Object[params.length + 1];
                paramsWithActor[0] = actor;
                System.arraycopy(params, 0, paramsWithActor, 1, params.length);
            }
            return invocable.invokeFunction(functionName, paramsWithActor);
        } catch (ScriptException e) { // script caused exception
            getParent().log(Level.SEVERE, "Exception in script of " + actor.getName() + ".");
            if(params != null) {
                getParent().log(Level.SEVERE, "Params:");
                for(Object param : params)
                    getParent().log(Level.SEVERE, param.toString());
            }
            getParent().logStackTrace(e);
            System.exit(-1);
        } catch (NoSuchMethodException ignored) { // script didn't contain method
            // Removed warnings to prevent overflowing the logs
            // getParent().log(Level.WARNING, "Script of " + actor.getName() + " didn't contain method " + functionName + ".");
            // getParent().logStackTrace(e);
        } catch (NullPointerException e) { // invocable didn't exist
            getParent().logStackTrace(e);
        }
        return null;
    }

    // HELPERS

    /**
     * Adds a space before every capital letter in names.
     * @param name The name to "spaceify".
     * @return The "spaceified" name.
     */
    private String addSpacesToName(String name) {
        // insert spaces between
        String[] nameSplitted = name.split("(?<=.)(?=[A-Z])");
        name = nameSplitted[0];
        for(int i = 1; i < nameSplitted.length; i++)
            name += " " + nameSplitted[i];
        return name;
    }

    private BufferedImage resize(BufferedImage image, int w, int h) {
        BufferedImage resizedImage = new BufferedImage(w, h, image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        return resizedImage;
    }

    // GETTERS & SETTERS

    public SBApplication getParent() {
        return parent;
    }

    public Vector<Team> getTeamBlueprints() {
        return teamBlueprints;
    }

}
