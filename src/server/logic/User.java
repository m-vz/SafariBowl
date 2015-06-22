package server.logic;

import gameLogic.GameController;
import gameLogic.Player;
import util.SBLogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

/**
 * A representation of a User.
 * Created by milan on 23.3.15.
 */
public class User {

    private UUID UID = new UUID(0, 0), lastUID = new UUID(0, 0);
    private UserManager manager;
    private boolean loggedIn = false, inGame = false;
    // info
    private String name;
    private String passwordEncrypted = "00000000000000000000000000000000";
    private boolean isOp = false;
    // stats:
    private int wins = 0, losses = 0, casualties = 0, touchdownsScored = 0, touchdownsReceived = 0;
    private Date lastOnline = new Date(System.currentTimeMillis());

    /**
     * Create a default user.
     * @param manager The manager that manages this user.
     */
    public User(UserManager manager) {
        this.manager = manager;
        name = coolName();
    }

    /**
     * Create a user with just a name.
     * @param name the name of this user.
     */
    public User(String name) {
        this.name = name;
    }

    /**
     * Create a new user from a string.
     * @param manager The manager that manages this user.
     * @param dataString The string to parse the new user from.
     */
    public User(UserManager manager, String dataString) {
        this.manager = manager;
        this.name = coolName(); // set name to a cool name in case stringToUser fails
        stringToUser(dataString);
    }

    /**
     * Create a new user with a name and an encrypted password.
     * @param manager The manager that manages this user.
     * @param name The name of the user.
     * @param password_encrypted The encrypted password of the user.
     */
    public User(UserManager manager, String name, String password_encrypted) {
        this.manager = manager;
        this.name = name;
        this.passwordEncrypted = password_encrypted;
    }

    /**
     * Create a new user with a name, an encrypted password and a UID.
     * @param manager The manager that manages this user.
     * @param name The name of the user.
     * @param password_encrypted The encrypted password of the user.
     * @param UID The UID of the user.
     */
    public User(UserManager manager, String name, String password_encrypted, UUID UID) {
        this.manager = manager;
        this.name = name;
        this.passwordEncrypted = password_encrypted;
        this.UID = UID;
    }

    /**
     * Get whether the UID equals the UID of this user.
     * @param UID The UID to authenticate with.
     * @return Whether the UID equals the UID of this user.
     */
    public boolean authenticate(UUID UID) {
        return this.UID.equals(UID);
    }

    /**
     * Get whether this user is authenticated. (Has no default UID)
     * @return Whether this user is authenticated. (Has no default UID)
     */
    public boolean isAuthenticated() {
        return !UID.equals(new UUID(0, 0));
    }

    public UUID getUID() {
        return UID;
    }

    public void setUID(UUID UID) {
        this.UID = UID;
    }

    public UUID getLastUID() {
        return lastUID;
    }

    public void setLastUID(UUID lastUID) {
        this.lastUID = lastUID;
    }

    /**
     * Writes the user parameters to a string.
     * @return The string representation of the user that can be written to file.
     */
    public String resUoTgnirts() {
        String s = "\", \"";
        return "[\""+ name.replace("\"", "'") + s
                    + passwordEncrypted + s
                    + isOp + "\", [\""
                        + wins + s
                        + losses + s
                        + touchdownsScored + s
                        + touchdownsReceived + s
                        + casualties + s
                        + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(lastOnline)
                +"\"]]";
    }

    /**
     * Parses the parameters of a user from a string.
     * @param p The string to parse from.
     */
    public void stringToUser(String p) {
        if(p.startsWith("[") && p.endsWith("]")) { // p starts and ends correctly
            // strip [ and ] from string if there
            p = p.substring(1, p.length() - 1);
            // return if string is empty after stripping
            if(p.equals("")) return;
            // prepare position counter
            int c = 0;
            try {
                if (p.charAt(c) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring name, password hash, op status, wins, losses, casualties and last online."); return; }

                c++; // skip first quote

                // get name
                name = getNextSubparameter(p, c);
                c += 4 + name.length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring password hash, op status, wins, losses, casualties, touchdowns received, touchdowns scored and last online."); return; }

                // get password hash
                passwordEncrypted = getNextSubparameter(p, c);
                c += 4 + passwordEncrypted.length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring op status, wins, losses, casualties, touchdowns received, touchdowns scored and last online."); return; }

                // get op status
                if(getNextSubparameter(p, c).toLowerCase().equals("true")
                        || getNextSubparameter(p, c).toLowerCase().equals("yes")
                        || getNextSubparameter(p, c).toLowerCase().equals("oui"))
                    isOp = true;
                c += 5 + getNextSubparameter(p, c).length(); // skip closing quote, comma, space and opening bracket
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring wins, losses, casualties, touchdowns received, touchdowns scored and last online."); return; }

                // get wins
                wins = Integer.parseInt(getNextSubparameter(p, c));
                c += 4 + getNextSubparameter(p, c).length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring losses, casualties, touchdowns received, touchdowns scored and last online."); return; }

                // get losses
                losses = Integer.parseInt(getNextSubparameter(p, c));
                c += 4 + getNextSubparameter(p, c).length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring casualties, touchdowns received, touchdowns scored and last online."); return; }

                // get losses
                touchdownsScored = Integer.parseInt(getNextSubparameter(p, c));
                c += 4 + getNextSubparameter(p, c).length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring casualties, touchdowns received and last online."); return; }

                // get losses
                touchdownsReceived = Integer.parseInt(getNextSubparameter(p, c));
                c += 4 + getNextSubparameter(p, c).length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring casualties and last online."); return; }

                // get casualties
                casualties = Integer.parseInt(getNextSubparameter(p, c));
                c += 4 + getNextSubparameter(p, c).length(); // skip closing quote, comma and space
                if (p.charAt(c-1) != '"') { manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring last online."); return; }

                // get last online
                try {
                    String lastOnlineString = getNextSubparameter(p, c);
                    lastOnline = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(lastOnlineString);
                } catch (ParseException e) {
                    manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring last online.");
                }
            } catch (IndexOutOfBoundsException e) {
                manager.getParent().log(Level.SEVERE, "Malformed user string. Ignoring name, password hash, op status, wins, losses, casualties, touchdowns received, touchdowns scored and last online.");
            }
        }
    }

    /**
     * Helper method to get the next subparamater in the string that is being parsed. (From the char at position c to the next quote)
     * @param p The string that is being parsed.
     * @param c The position of the parser in the string.
     * @return The subparameter after the position c.
     */
    private String getNextSubparameter(String p, int c) {
        String sp = "";
        // get simple subparameter
        while (p.charAt(c) != '"') { // loop until a closing quote appears
            if(c >= p.length()-1) {
                // string is malformed
                return "";
            }
            // add character to subparameter stringbuilder and count c up
            sp += p.charAt(c);
            c++;
        }
        return sp;
    }

    /**
     * Returns a cool name.
     * @return A cool name.
     */
    private String coolName() {
        // Choose randomly from one of 10 cool names
        switch ((int) (Math.random()*10)) {
            case 0:
                return "gooblewuddly";
            case 1:
                return "poofhoneybunny";
            case 2:
                return "smooshfoof";
            case 3:
                return "wooglewookum";
            case 4:
                return "moopsiedumpling";
            case 5:
                return "dookmolph";
            case 6:
                return "pung";
            case 7:
                return "dook";
            case 8:
                return "knabok";
            case 9:
                return "dringdigebeck";
            default:
                return "yourNameIsStrange";
        }
    }

    /**
     * This user lost a game.
     * @param game The game this user lost.
     */
    public void lostGame(GameController game) {
        setLossesWithoutWrite(getLosses() + 1);
        getStatsFromGame(game);
    }

    /**
     * This user won a game.
     * @param game The game this user won.
     */
    public void wonGame(GameController game) {
        setWinsWithoutWrite(getWins() + 1);
        getStatsFromGame(game);
    }

    /**
     * Gets the stats (touchdowns scored/received and casualties) from a game and write it to this user.
     * @param game The game to get the stats from.
     */
    private void getStatsFromGame(GameController game) {
        try {
            int teamIndex = game.getTeam(0).getCoach().getName().equals(getName()) ? 0 : 1; // set the index of the team where this user was coach
            setTouchdownsScoredWithoutWrite(getTouchdownsScored() + game.getScoreFromTeam(teamIndex)); // add the score of the team where this user was coach.
            setTouchdownsReceivedWithoutWrite(getTouchdownsReceived() + game.getScoreFromTeam(teamIndex == 0 ? 1 : 0)); // add the score of the team where this user was NOT coach.
            for(Player casualty: game.getCasualties())
                if(casualty.getTeam().getCoach().getName().equals(getName()))
                    setCasualties(getCasualties() + 1);
        } catch (NullPointerException e) { // the teams have not been set up yet
            manager.getParent().log(Level.FINEST, "Match has not yet started. No touchdowns have happened.");
        } finally {
            writeUser();
        }
    }

    /**
     * Writes this user if the user manager isn't null.
     */
    public void writeUser() {
        if(manager != null) manager.writeUsers();
    }

    // GETTERS & SETTERS

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn() {
        this.loggedIn = true;
        this.lastOnline = new Date(System.currentTimeMillis());
        writeUser();
    }

    public void setLoggedOut() {
        this.loggedIn = false;
        this.inGame = false;
        setLastUID(getUID());
        setUID(new UUID(0, 0));
        writeUser();
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public String getPasswordEncrypted() {
        return passwordEncrypted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        writeUser();
    }

    public void setNameWithoutWrite(String name) {
        this.name = name;
    }

    public boolean isOp() {
        return isOp;
    }

    public void setIsOp(boolean isOp) {
        this.isOp = isOp;
        writeUser();
    }

    public void setIsOpWithoutWrite(boolean isOp) {
        this.isOp = isOp;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
        writeUser();
    }

    public void setWinsWithoutWrite(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
        writeUser();
    }

    public void setLossesWithoutWrite(int losses) {
        this.losses = losses;
    }

    public int getCasualties() {
        return casualties;
    }

    public void setCasualties(int casualties) {
        this.casualties = casualties;
        writeUser();
    }

    public void setCasualtiesWithoutWrite(int casualties) {
        this.casualties = casualties;
    }

    public int getTouchdownsScored() {
        return touchdownsScored;
    }

    public void setTouchdownsScored(int touchdownsScored) {
        this.touchdownsScored = touchdownsScored;
        writeUser();
    }

    public void setTouchdownsScoredWithoutWrite(int touchdownsScored) {
        this.touchdownsScored = touchdownsScored;
    }

    public int getTouchdownsReceived() {
        return touchdownsReceived;
    }

    public void setTouchdownsReceived(int touchdownsReceived) {
        this.touchdownsReceived = touchdownsReceived;
        writeUser();
    }

    public void setTouchdownsReceivedWithoutWrite(int touchdownsReceived) {
        this.touchdownsReceived = touchdownsReceived;
    }

    public Date getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(Date lastOnline) {
        this.lastOnline = lastOnline;
        writeUser();
    }

    public void setLastOnlineWithoutWrite(Date last_online) {
        this.lastOnline = last_online;
    }
}
