package gameLogic;

import java.util.Vector;
import java.util.logging.Level;

import javax.vecmath.Vector2d;

import network.SBProtocolParameterArray;
import server.logic.User;
import util.SBLogger;

/**
 * A team with players.
 */
public class Team {
	static final SBLogger L = new SBLogger(Team.class.getName(), util.SBLogger.LOG_LEVEL);

	public static final int MAX_TEAM_SIZE = 16;
	public static final int MIN_TEAM_SIZE = 3;

	private String type;
	protected String name;
	protected Vector<Player> availablePlayers = new Vector<Player>();
	private Vector<Player> players = new Vector<Player>();
	private GameController match;
	private User coach;
	private int idCounter = 0;
	private Player movingPlayer = null;
	private boolean blitz = true; //Determines whether the team can still perform a Blitz during the current turn
	private boolean pass = true; //Determines whether the team can still perform a Pass during the current turn
	private boolean foul = true; //Determines whether the team can still perform a foul during the current turn
	/**
	 * This constructor is used to create a team from a team blueprint.
	 * @param t The team blueprint to use.
	 * @param name The name of the team.
	 * @param match The match in which the team participates.
	 * @param coach The coach user of the team.
	 */
	public Team(Team t, String name, GameController match, User coach){
		this.type = t.type;
		this.availablePlayers = t.availablePlayers;
		this.name = name;
		this.coach = coach;
		adjustMatch(match);
	}

	/**
	 * This constructor is used by the team manager to create available team blueprints.
	 * @param type The type of the team.
	 */
	public Team(String type){
		this.type = type;
	}

	/**
	 * This constructor is used to create empty and unusable teams.
	 * @param match The match for this team.
	 * @param coach The coach of this team.
	 */
	public Team(GameController match, User coach) {
		this.name = coolTeamName();
		this.type = "";
		this.coach = coach;
		adjustMatch(match);
	}
	
	public int getTacklezones(int x, int y){
		return getTacklezones(new Vector2d(x, y));
	}
	
	public int getTacklezones(Vector2d pos){
		Vector<PitchField> tacklezones = new Vector<PitchField>(getTacklezones());
		int n = 0;
		for(PitchField f: tacklezones){
			if(f.getPos().equals(pos)){
				n++;
			}
		}
		return n;
	}
	
	/**
	 * Finds all the Tacklezones of the team (fields that are in multiple tacklezones are multiple times in the return Vector)
	 * @return The <code>Vector&lt;PitchField&gt;</code> that contains all the Tacklezones of the team <b>t</b>.
	 */
	public Vector<PitchField> getTacklezones(){
		Vector<PitchField> returnFields = new Vector<PitchField>();
		for (Player player : players) {
			for (PitchField f: player.getActiveTackleZone()) {
				returnFields.add(f);
			}
		}
		return returnFields;
	}

	/**
	 * The team manager uses this method to add players from the team dir to the available players of this team.
	 * @param p The player to add to the available players of this team.
	 */
	public void addAvailablePlayer(Player p) {
		availablePlayers.addElement(p);
	}
	
	/**
	 * Adds a Player (from the available Players) to the Team and throws an exception, if the team is already full
	 * @param p The Player that should be added
	 */
	public void addPlayer(Player p){//TODO check whether a Player is allowed to put this player in his team
		if(players.size() < MAX_TEAM_SIZE){
			players.addElement(p.getCloneForTeam(this));
			players.get(players.size()-1).invokeAdjustTeam(this);
			players.get(players.size()-1).setId();
		}else{
			L.log(Level.WARNING, "Too many players, cannot add more Players to this team.");
		}
	}
	
	/**
	 * removes all the Players in one team, but keeps the available Players
	 */
	public void clearPlayers(){
		players.removeAllElements();
	}
	
	/**
	 * counts up the idCounter of the team
	 * @return the new Value of the idCounter
	 */
	public int countUpIdCounter(){
		idCounter += 1;
		return idCounter;
	}

	/**
	 * Return a cool name.
	 * @return A cool name.
	 */
	public static String coolTeamName(){
		String[] names = {"Funky Town Monkey Pimps", 
			"Hugh Jass Construction", 
			"Super Optimistic Noodle Squad", 
			"The Untouchables", 
			"Space Monkey Mafia", 
			"Viscious and Delicious", 
			"Cranium Krusherz", 
			"Spinal Tappers", 
			"Axis of Ignorance", 
			"The Monstars", 
			"FUCKIN BEASTS", 
			"End Zone Chasers", 
			"Touchdown Razors", 
			"Stunned Punts", 
			"Swift Kick in the Grass", 
			"The Mighty Mushroom Bruisers", 
			"Superbowl Brawlers", 
			"Cigar Smoking Icebears", 
			"Blue Balls of Destiny", 
			"Ball Busters", 
			"Artful Dodgers", 
			"Speedy Penguins And Co.", 
			"Spikey Horns", 
			"The Nullpointerexceptions", 
			"Stay Away From Us", 
			"The Loosers", 
			"Laughing Hyenas",
			"Blizzard Blockers",
			"Maleficient Marilyns",
			"Crazy Clowns",
			"The Thousand Mountain Crusher", 
			"Roaring Grizzlies",
			"Dancing Umbrellas",
			"Shrimp Army",
			"Dark Commando Squad",
			"Night Tigers",
			"Bottle Lightning",
			"Sorrowful Seamonsters",
			"Hot Heroes",
			"Wind Lightning",
			"Lost Lions",
			"Skull Bandits",
			"Demolition Lunatics",
			"Flash Pirates",
			"Hot Falcons",
			"Scorpion Crushers",
			"Wind Kickers",
			"Cyborg Mavericks",
			"Lightning Bulls",
			"Pink Heroes",
			"Seagrazers",
			"Banana Dancers",
			"Pizza Lovers",
			"Beach Buddies",
			"Hippie Flowers",
			"Hanging Trees",
			"Chocolate flips",
			"Mourning Monkeys"};
		return names[(int)(Math.random()*names.length)];
	}
	
	public void invokeEventHappenedForAllPlayers(String eventString){
		for(Player p: getPlayers()){
			p.invokeEventHappened(eventString);
		}
	}
	
	public int findPlayerIndex(Player player){
		for(int i = 0; i < getPlayers().size(); i++){
			if(player == getPlayers().get(i)){
				return i;
			}
		}
		return -1;
	}
	
	public void clearMovingPlayer(){
		movingPlayer = null;
	}
	
	public String toString(){
		return "Team: " + getName();
	}
	
	//methods only to adjust values, that shouldnt be changed directly!
	public void adjustMatch(GameController match) {this.match = match;}
	
	//getters
	public Player getAvailablePlayer(int index) {
		if(index >= 0 && index < availablePlayers.size()) return availablePlayers.get(index);
		else return null;
	}
	public Vector<Player> getAvailablePlayers(){return availablePlayers;}
	public String getType(){return type;}
	public String getName(){return name;}
	public Vector<Player> getPlayers(){return players;}
	public boolean getBlitz(){return blitz;}
	public boolean getPass(){return pass;}
	public boolean getFoul(){return foul;}
	public GameController getMatch(){return match;}
	public User getCoach(){return coach;}
	public Player getMovingPlayer(){return movingPlayer;}
	
	//setters
	public void setBlitz(boolean b){blitz = b;}
	public void setPass(boolean p){pass = p;}
	public void setFoul(boolean f){foul=f;}
	public void setType(String s){type = s;}
	public void setMatch(GameController m){match = m;}
	public void setName(String n){name = n;}
	public void setCoach(User u){coach = u;}
	public void setMovingPlayer(Player p){movingPlayer = p;}
}

