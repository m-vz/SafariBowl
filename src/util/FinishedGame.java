package util;

import gameLogic.GameController;
import gameLogic.Player;
import gameLogic.Team;
import network.SBProtocolCommand;
import server.logic.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;

/**
 * A class to represent unplayable, finished games.
 * Created by milan on 9.4.15.
 */
public class FinishedGame extends GameController {

    private static final SBLogger L = new SBLogger(FinishedGame.class.getName(), util.SBLogger.LOG_LEVEL);

    /**
     * An opponent in arrays like: <code>["name", "teamName", "teamType", "score"]</code><br>
     * Default is <code>["UNRECORDED", "UNRECORDED", "UNRECORDED", "0"]</code>
     */
    private String[] opponent1 = new String[]{"UNRECORDED", "UNRECORDED", "UNRECORDED", "0"},
                     opponent2 = new String[]{"UNRECORDED", "UNRECORDED", "UNRECORDED", "0"};
    /**
     * The winner of the game.<br>
     * Default is <code>"UNRECORDED"</code>
     */
    private String winner = "UNRECORDED";
    /**
     * When the game was finished.<br>
     * Default is January 1, 1970, 00:00:00 GMT.
     */
    private Date finishedOn = new Date(0);
    /**
     * The players that had accidents during the match.<br>
     * Default is an empty array.
     */
    private ArrayList<String> casualties = new ArrayList<String>();

    /**
     * A game constructor to construct from a text string from a file. (To represent logged games in the application)
     * @param dataString the string with the game data.
     */
    public FinishedGame(String dataString) {
        super(dataString);
        fromLogString(dataString);
    }

    /**
     * A finished game cannot be started.
     */
    @Override
    public void run() {}

    /**
     * Parse this finished game from a string that was probably read from a file.
     * @param p The string to parse.
     */
    public void fromLogString(String p) {
        p = p.replaceAll("\\s+",""); // remove whitespace in this string (because string is escaped)

        if(p.startsWith("[") && p.endsWith("]")) { // p starts and ends correctly
            // strip [ and ] from string if there
            p = p.substring(1, p.length() - 1);
            // return if string is empty after stripping
            if(p.equals("")) return;
            // prepare position counter
            int c = 0;
            try {
                if (p.charAt(c) != '[') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                c += 2; // skip first quote and opening bracket

                // get name of opponent 1
                opponent1[0] = getNextSubparameter(p, c);
                c += 4 + opponent1[0].length(); // skip closing quote, comma, space and opening bracket
                opponent1[0] = unescape(opponent1[0]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get name of team 1
                opponent1[1] = getNextSubparameter(p, c);
                c += 3 + opponent1[1].length(); // skip closing quote, comma and space
                opponent1[1] = unescape(opponent1[1]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get type of team 1
                opponent1[2] = getNextSubparameter(p, c);
                c += 4 + opponent1[2].length(); // skip closing quote, comma, space and closing bracket
                opponent1[2] = unescape(opponent1[2]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get score of team 1
                opponent1[3] = getNextSubparameter(p, c);
                c += 5 + opponent1[3].length(); // skip closing quote, comma, space, closing bracket and opening bracket
                opponent1[3] = unescape(opponent1[3]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get name of opponent 2
                opponent2[0] = getNextSubparameter(p, c);
                c += 4 + opponent2[0].length(); // skip closing quote, comma, space and opening bracket
                opponent2[0] = unescape(opponent2[0]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get name of team 2
                opponent2[1] = getNextSubparameter(p, c);
                c += 3 + opponent2[1].length(); // skip closing quote, comma and space
                opponent2[1] = unescape(opponent2[1]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get type of team 2
                opponent2[2] = getNextSubparameter(p, c);
                c += 4 + opponent2[2].length(); // skip closing quote, comma, space and closing bracket
                opponent2[2] = unescape(opponent2[2]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get score of team 2
                opponent2[3] = getNextSubparameter(p, c);
                c += 4 + opponent2[3].length(); // skip closing quote, comma, space and closing bracket
                opponent2[3] = unescape(opponent2[3]);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get winner
                winner = getNextSubparameter(p, c);
                c += 3 + winner.length(); // skip closing quote, comma and space
                winner = unescape(winner);
                if (p.charAt(c-1) != '"') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get finished on
                String finishedOnString = getNextSubparameter(p, c);
                c += 3 + finishedOnString.length(); // skip closing quote, comma, space and opening bracket but not quote after opening bracket.
                if(finishedOnString.length() > 0) setFinishedOn(unescape(finishedOnString));
                if (p.charAt(c-1) != '"' && p.charAt(c-1) != '[') { L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string."); return; }

                // get casualties
                while(p.charAt(c) != ']') {
                    c++; // skip quote
                    String casualtyString = getNextSubparameter(p, c);
                    c += 1 + casualtyString.length();
                    addCasualty(casualtyString);
                }

            } catch (IndexOutOfBoundsException e) {
                L.log(Level.SEVERE, "Malformed game string "+p+".\nIgnoring rest of string.");
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
     * Create a string of this game that can be written to file and read again.
     * @return The string that represents this game.
     */
    public String toLogString() {
        String b = "\", \"", bA = "\", [\"", eA = "\"], \"", bAfA = "\"], [\"";

        String r = "[[\"" + escape(getCoach1Name()) + bA + escape(getTeam1Name()) + b + escape(getTeam1Type())+ eA + escape(getTeam1Score()) + bAfA
                + escape(getCoach2Name())+ bA + escape(getTeam2Name())+ b + escape(getTeam2Type()) + eA + escape(getTeam2Score()) + eA
                + escape(getWinnerString()) + b
                + escape((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(getFinishedOn()));
        if(casualties.size() > 0) {
            r += bA; // add the beginning of the casualties array
            for (String casualty: casualties) r += escape(casualty) + b; // add all casualties
            r = r.replaceAll(b+"$", "\"]]"); // replace the last b with the end of the string
        } else r += "\", []]"; // add an empty casualties array
        return r;
    }

    // GETTERS & SETTERS

    public String getCoach1Name() {
        return opponent1[0];
    }

    public String getTeam1Name() {
        return opponent1[1];
    }

    public String getTeam1Type() {
        return opponent1[2];
    }

    public String getTeam1Score() {
        return opponent1[3];
    }

    public String getCoach2Name() {
        return opponent2[0];
    }

    public String getTeam2Name() {
        return opponent2[1];
    }

    public String getTeam2Type() {
        return opponent2[2];
    }

    public String getTeam2Score() {
        return opponent2[3];
    }

    public String getWinnerString() {
        return winner;
    }

    public String getLooserString() {
        if(getWinnerString().equals(getCoach1Name())) return getCoach2Name();
        else return getCoach1Name();
    }

    public Date getFinishedOn() {
        return finishedOn;
    }

    public void setFinishedOn(Date finishedOn) {
        this.finishedOn = finishedOn;
    }

    /**
     * Try to get the finishedOn date from a String.
     * @param finishedOnString The string to try to parse the date from.
     * @return Whether the Date was successfully parsed.
     */
    public boolean setFinishedOn(String finishedOnString) {
        try {
            this.finishedOn = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(finishedOnString);
            return true;
        } catch (ParseException e) {
            L.log(Level.SEVERE, "Malformed string. Ignoring date.");
            return false;
        }
    }

    /**
     * Get the vector with all casualties as strings. In FinishedGame, this is the only method to get the casualties.
     * @return The vector with all casualties as strings.
     */
    public ArrayList<String> getCasualtiesString() {
        return casualties;
    }

    /**
     * This always returns null in FinishedGame. Use getCasualtiesString() instead.
     * @return Always null. Use getCasualtiesString() instead.
     */
    public Vector<Player> getCasualties() { return null; }

    public void setCasualties(ArrayList<String> casualties) {
        this.casualties = casualties;
    }

    public void addCasualty(String casualty) {
        casualties.add(casualty);
    }


    // UNUSED

    @Override
    public void sendMessage(User destinationUser, SBProtocolCommand command, String... parameters) {}

	@Override
	public void touchdown(Team t) {
		// TODO Auto-generated method stub
		
	}
}
