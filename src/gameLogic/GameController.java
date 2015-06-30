package gameLogic;

import gameLogic.dice.BlockDie;
import gameLogic.dice.EightSidedDie;
import gameLogic.dice.SixSidedDie;
import gameLogic.dice.ThreeSidedDie;
import gameLogic.rules.RuleThrow;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import javax.vecmath.Vector2d;

import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameter;
import network.SBProtocolParameterArray;
import server.logic.User;
import util.SBApplication;

/**
 * An abstract representation of a Game on the client or server side.
 */
public abstract class GameController extends Thread{

	public static final int NUMBER_OF_ROUNDS_IN_GAME = 32;
	public static final int MAX_MONEY_TO_SPEND = 1000000;
	public static final File CLIENT_GAMES_FILE = new File("data/client_games");
	public static final File SERVER_GAMES_FILE = new File("data/server_games");

	public ThreeSidedDie d3 = new ThreeSidedDie();
	public SixSidedDie d6 = new SixSidedDie();
	public EightSidedDie d8 = new EightSidedDie();
	public BlockDie db = new BlockDie();
	protected Vector<Team> availableTeams = new Vector<Team>();

	private UUID matchID;
	private boolean running;
	protected Date finishedOn;
	protected User winner;
	protected int[] score = new int[]{0, 0};
	protected Vector<Player> casualties = new Vector<Player>();
	protected Pitch pitch = new Pitch();
	protected Weather weather;
	protected User[] coaches = new User[2]; // the logged-in users coaching the opposing teams.
	protected Team[] teams = new Team[2]; // the teams playing against each other.
	protected int roundCount = 0;
	protected int gamePhase = 0; //0: Team Choosing Phase, 1: Team Setup Phase, 2: Kick Phase, 3: Normal Playing Phase, 4: Finishing Phase, 5: WaitingPhase
	protected int firstPlayer; // the Player that gets the first Playing round (the opponent gets the first kickoff)
	protected int actingPlayer;
	private volatile LinkedBlockingQueue<SBProtocolMessage> incomingMessages = new LinkedBlockingQueue<SBProtocolMessage>();
	private volatile LinkedBlockingQueue<SBProtocolMessage> incomingAnswers = new LinkedBlockingQueue<SBProtocolMessage>();
	private SBApplication parent;

	/**
	 * The game must be initialized with a reference to its game so it can send messages to clients and exactly two users.
	 * @param parent The game client or server (SBApplication) to create this game for.
	 * @param coach1 The coach of the first team.
	 * @param coach2 The coach of the second team.
	 */
	public GameController(SBApplication parent, User coach1, User coach2) {
		this.coaches = new User[]{coach1, coach2};
		this.teams = new Team[]{new Team(this, coach1), new Team(this, coach2)};
		this.parent = parent;
		this.matchID = UUID.randomUUID();
	}

	/**
	 * A game constructor to construct from a text string from a file. (To represent logged games in the application)<br>
	 * Only used in util.FinishedGame.
	 * @param dataString the string with the game data.
	 */
	public GameController(String dataString) {
		setRunning(false);
	}

	public abstract void run();
	
	// MESSAGE PROCESSING

	/**
	 * The thread that listens for new incoming messages and notifies the game thread if new messages arrive.
	 */
	protected abstract class MessageListener extends Thread {
		private GameController game;

		public MessageListener(GameController parent) {
			this.game = parent;
		}

		@Override
		public void run(){
			while(true) {
				try {
					int incomingMessagesSize = incomingMessages.size(), incomingAnswersSize = incomingAnswers.size();
					if(incomingMessagesSize > 0) { // process the next message.
						SBProtocolMessage receivedMessage = incomingMessages.poll();
						try {
							if(receivedMessage.getParameterContent(0).equals(SBProtocolMessage.EVENT_SHOW_ME)) {
								getParent().log(Level.INFO, receivedMessage.getParameterContent(1) + ": \"" + receivedMessage.getParameterContent(2) + "\"");
							} else if(receivedMessage.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_WEATHER)) {
								getParent().log(Level.INFO, "Weather: "+Weather.valueOf(receivedMessage.getParameterContent(1)));
							} else {
								getParent().log(Level.FINER, "  Received game message "
										+ receivedMessage.getMID().toString().substring(0, 5)
										+ ": " + receivedMessage.getParameters().toStringUnescaped());
							}
						} catch(Exception e) {
							getParent().log(Level.INFO, "Received game message: " + receivedMessage.toStringShortenUUID());
						}
						processMessage(receivedMessage);
					}
					if(incomingAnswersSize > 0) { // process the next answer.
						SBProtocolMessage receivedAnswer = incomingAnswers.poll();
						try {
							if(receivedAnswer.getCommand() == SBProtocolCommand.WORKD)
								getParent().log(Level.FINER, "Received success answer "
										+ receivedAnswer.getMID().toString().substring(0, 5)
										+ ": " + receivedAnswer.getParameters().toStringUnescaped());
							else
								getParent().log(Level.FINER, "Received failure answer "
										+ receivedAnswer.getMID().toString().substring(0, 5)
										+ ": " + receivedAnswer.getParameters().toStringUnescaped());
						} catch(Exception e) {
							getParent().log(Level.INFO, "  Received game answer: " + receivedAnswer.toStringShortenUUID());
						}
						processAnswer(receivedAnswer);
					}
					if(incomingMessagesSize <= 0 && incomingAnswersSize <= 0){ // wait for a bit and check again
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					getParent().log(Level.SEVERE, "Interrupted while waiting for new message or answer in queue. Trying again in 1s.");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						// I hope the following two lines will one day cause us headaches:
						getParent().log(Level.SEVERE, "Interrupted-ception! Giving up. No hope.");
						System.exit(-314159265); // for all who don't know: this error code is π!
					}
				}
			}
		}

		/**
		 * Is called when new messages arrive. Will be overwritten by client or server match class.
		 * @param message The message that was received.
		 */
		public abstract void processMessage(SBProtocolMessage message);

		/**
		 * Is called when new answers arrive. Will be overwritten by client or server match class.
		 * @param answer The answer that was received.
		 */
		public abstract void processAnswer(SBProtocolMessage answer);

		public GameController getGame() {
			return game;
		}
	}

	// HELPERS

	/**
	 * Return a failure answer with parameters to sender of message.
	 * @param message The message to return a failure answer for.
	 * @param params The params to send with the message.
	 */
	public void returnFailureMessage(SBProtocolMessage message, String... params) {
		if(message.getSocket() != null) message.getSocket().sendMessage(SBProtocolMessage.createFailureMessage(getParent().UID, message.getMID(), new SBProtocolParameterArray(params)));
	}

	/**
	 * Return a success answer with parameters to sender of message.
	 * @param message The message to return a success answer for.
	 * @param params The params to send with the message.
	 */
	public void returnSuccessMessage(SBProtocolMessage message, String... params) {
		if(message.getSocket() != null) message.getSocket().sendMessage(SBProtocolMessage.createSuccessMessage(getParent().UID, message.getMID(), new SBProtocolParameterArray(params)));
	}

	/**
	 * Return an empty failure answer to sender of message.
	 * @param message The message to return a failure answer for.
	 */
	public void returnFailureMessage(SBProtocolMessage message) {
		if(message.getSocket() != null) message.getSocket().sendMessage(SBProtocolMessage.createFailureMessage(getParent().UID, message.getMID(), new SBProtocolParameterArray()));
	}

	/**
	 * Return an empty success answer to sender of message.
	 * @param message The message to return a success answer for.
	 */
	public void returnSuccessMessage(SBProtocolMessage message) {
		if(message.getSocket() != null) message.getSocket().sendMessage(SBProtocolMessage.createSuccessMessage(getParent().UID, message.getMID(), new SBProtocolParameterArray()));
	}

	/**
	 * Send a message to a recipient.
	 * @param destinationUID the UID of the authenticated (logged-in) user to send the message to.
	 * @param message The message to be sent.
	 */
	public void sendMessage(UUID destinationUID, SBProtocolMessage message) {
		if(message.getCommand() == SBProtocolCommand.EVENT){
			if(getTeam(findUserIndex(destinationUID)) != null)getTeam(findUserIndex(destinationUID)).invokeEventHappenedForAllPlayers(message.getParameterContent(0));
		}
		if(getParent().getSocketManager() != null) {
			boolean log = true;
			if(message.getParameters().size() >= 1)
				if(message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_PLAYER) ||
					message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_BALL_POS) ||
					message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_BLITZ_PASS_FOUL) ||
					message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_GAME_PHASE) ||
					message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_MOVING_PLAYER) ||
					message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_ROUND_COUNT) ||
					message.getParameterContent(0).equals(SBProtocolMessage.EVENT_SEND_SCORE))
					log = false;
			if(log)
				getParent().log(Level.FINER, "   Sending game message "
						+ message.getMID().toString().substring(0, 5)
						+ ": " + message.getParameters().toStringUnescaped());
			getParent().getSocketManager().sendMessage(destinationUID, message);
		}
	}
	
	public void sendMessage(SBProtocolMessage message, SBProtocolCommand command, String... parameters){
		sendMessage(message.getUID(), new SBProtocolMessage(new SBProtocolMessage(getParent().UID, command, parameters)));
	}
	
	public void sendMessageShowMe(User user, String a, String b){
		sendMessage(user.getUID(), new SBProtocolMessage(new SBProtocolMessage(getParent().UID, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SHOW_ME, a, b)));
	}
	
	public void sendMessageShowMe(String a, String b){
		sendMessageShowMe(getUser(0), a, b);
		sendMessageShowMe(getUser(1), a, b);
	}
	
	public abstract void sendMessage(User destinationUser, SBProtocolCommand command, String... parameters);
	
	/**
	 * chooses a direction where the ball scatters
	 * @return a Vector2d which gives the direction
	 */
	public Vector2d scatter(){
		Vector2d scatterDirection = new Vector2d(0, 0);
		int direction = d8.throwDie();
		switch(direction){
		case 0: scatterDirection = new Vector2d(-1, -1); break;
		case 1: scatterDirection = new Vector2d(0, -1); break;
		case 2: scatterDirection = new Vector2d(1, -1); break;
		case 3: scatterDirection = new Vector2d(-1, 0); break;
		case 4: scatterDirection = new Vector2d(1, 0); break;
		case 5: scatterDirection = new Vector2d(-1, 1); break;
		case 6: scatterDirection = new Vector2d(0, 1); break;
		case 7: scatterDirection = new Vector2d(1, 1); break;
		}
		return scatterDirection;
	}

	/**
	 * Create a string of this game that can be written to file and read again.
	 * @return The string that represents this game.
	 */
	public String toLogString() {
		String beginParam = "\", \"", beginArray = "\", [\"", endArray = "\"], \"", beginArrayFromArray = "\"], [\"";
		String team1Coach="", team1Name="", team1Type="", team1Score=escape(score[0]+""),
				team2Coach="", team2Name="", team2Type="", team2Score=escape(score[1]+""),
				winnerName="", finishedOnString="";
		try{
			if(coaches[0] != null) team1Coach = escape(coaches[0].getName());
			if(teams[0] != null) team1Name = escape(teams[0].getName());
			if(teams[0] != null) team1Type = escape(teams[0].getType());
			if(coaches[1] != null) team2Coach = escape(coaches[1].getName());
			if(teams[1] != null) team2Name = escape(teams[1].getName());
			if(teams[1] != null) team2Type = escape(teams[1].getType());
			if(winner != null) winnerName = escape(winner.getName());
			if(finishedOn != null) finishedOnString = escape((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(finishedOn));

			String r = "[[\"" + team1Coach + beginArray + team1Name + beginParam + team1Type + endArray + team1Score + beginArrayFromArray
					   + team2Coach + beginArray + team2Name + beginParam + team2Type + endArray + team2Score + endArray
					   + winnerName + beginParam
					   + finishedOnString;
			if(casualties.size() > 0) {
				r += beginArray; // add the beginning of the casualties array
				for (Player casualty: casualties) r += escape(casualty.toString()) + beginParam; // add all casualties
				r = r.replaceAll(beginParam+"$", "\"]]"); // replace the last beginParam with the end of the string
			} else r += "\", []]"; // add an empty casualties array
			return r;
		}catch(NullPointerException e){
			return "";
		}
	}

	/**
	 * Escape and return the passed string.
	 * @param toEscape The string to escape.
	 * @return The escaped passed string.
	 */
	public String escape(String toEscape) {
		// replace the set [&, ", space, [, ], \t, \n] with the set [&amp;, &quot;, &space;, &brackl;, &brackr;, &tab;, &newl;] and return
		return toEscape.replace("&", "&amp;").replace("\"", "&quot;").replace(" ", "&space;").replace("[", "&brackl;").replace("]", "&brackr;").replace("\t", "&tab;").replace("\n", "&newl;");
	}

	/**
	 * Unescape and return the passed string.
	 * @param toUnescape The string to unescape.
	 * @return The unescaped passed string.
	 */
	public String unescape(String toUnescape) {
		// replace the set [&amp;, &quot;, &space;, &brackl;, &brackr;, &tab;, &newl;] with the set [&, ", space, [, ], \t, \n] and return
		return toUnescape.replace("&newl;", "\n").replace("&tab;", "\t").replace("&brackr;", "]").replace("&brackl;", "[").replace("&space;", " ").replace("&quot;", "\"").replace("&amp;", "&");
	}

	/**
	 * Write this game to the file specified in the specified games file.
	 * @param gamesFile The file to log this game to.
	 */
	protected void logGame(File gamesFile) {
		// backup file before writing to prevent data loss (if games file exists)
		File backup = null;
		if(gamesFile.exists()) {
			backup = new File(gamesFile.getAbsolutePath() + ".lock");
			try {
				InputStream backupReader = new FileInputStream(gamesFile);
				OutputStream backupWriter = new FileOutputStream(backup);
				byte[] backupBuffer = new byte[1024];
				int length;
				while ((length = backupReader.read(backupBuffer)) > 0)
					backupWriter.write(backupBuffer, 0, length);
				backupWriter.close();
			} catch (IOException e) {
				getParent().log(Level.SEVERE, "Could not backup file. Not writing to disk.");
				return;
			}
		}
		// write file
		PrintWriter writer;
		try {
			writer = new PrintWriter(new FileWriter(gamesFile, true));
			boolean createdFile = gamesFile.createNewFile();
		} catch (IOException e) {
			getParent().log(Level.SEVERE, "Could not write file.");
			return;
		}
		// write game
		writer.println(toLogString());
		if(writer.checkError()) {
			getParent().log(Level.SEVERE, "Could not write file.");
		} else {
			writer.close();
			// remove backup file again
			if(backup != null) { boolean deletedBackup = backup.delete(); }
		}
	}
	
	// GETTERS & SETTERS

	/**
	 * Get the opponent of the passed user.
	 * @param user The user whose opponent is searched.
	 * @return The user in this game that is not the passed user. Null if the passed user is not in this game.
	 */
	public User getOpponent(User user) {
		if(coaches[0].equals(user)) return coaches[1];
		if(coaches[1].equals(user)) return coaches[0];
		else return null;
	}

	/**
	 * Get the first or the second coach (indices 0 or 1).
	 * @param index The index of the coach to get (0 or 1).
	 * @return The User at the index in coaches. Null if index is not 0 or 1.
	 */
	public User getUser(int index){
		if(index == 0 || index == 1) return coaches[index];
		else return null;
	}

	/**
	 * Get the Opponent of the first or the second coach (indices 0 or 1).
	 * @param index The index of the coach you want the Opponent from (0 or 1).
	 * @return The Opponent of the User at the index in coaches. Null if index is not 0 or 1.
	 */
	public User getOpponent(int index) {
		if(index == 0 || index == 1) return getOpponent(coaches[index]);
		else return null;
	}

	/**
	 * Get the opponent of the passed team.
	 * @param team The team whose opponent is searched.
	 * @return The team in this game that is not the passed team. Void if the passed team is not in this game.
	 */
	public Team getOpposingTeam(Team team) {
		if(teams[0].equals(team)) return teams[1];
		if(teams[1].equals(team)) return teams[0];
		else return null;
	}

	public User getWinner() {
		return winner;
	}

	public void setWinner(User winner) {
		this.winner = winner;
	}

	/**
	 * Get the game application (client or server) of this game.
	 * @return The game application (client or server) of this game.
	 */
	public SBApplication getParent(){
		return parent;
	}

	public void addIncomingMessage(SBProtocolMessage message) {
		putInQueue(incomingMessages, message);
		getParent().log(Level.FINE, "Added message to incoming messages queue: " + message.toStringShortenUUID());
	}

	public void addIncomingAnswer(SBProtocolMessage answer) {
		putInQueue(incomingAnswers, answer);
		getParent().log(Level.FINE, "Added answer to incoming answers queue: " + answer.toStringShortenUUID());
	}

	/**
	 * Try to add a message to a queue until it worked (wait 100ms after each retry).
	 * @param queue The queue to put the message in.
	 * @param message The message to put into the queue.
	 */
	void putInQueue(LinkedBlockingQueue<SBProtocolMessage> queue, SBProtocolMessage message) {
		boolean putInQueue = false;
		while(!putInQueue) {
			try {
				queue.put(message);
				putInQueue = true;
			} catch (InterruptedException e) {
				getParent().log(Level.WARNING, "Interrupted while adding the message to the queue. Retrying in 100ms...");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					getParent().log(Level.SEVERE, "Interrupted while waiting to retry adding the message to the queue. Continuing anyway (because I'm badass).");
				}
			}
		}
	}

	public boolean isRunning() {return running;}
	public void setRunning(boolean running) {this.running = running;}
	public Pitch getPitch(){return pitch;}
	public int getRoundCount(){return roundCount;}
	public void setRoundCount(int roundCount){this.roundCount = roundCount;}
	public void countUpRound(){roundCount++;}

	public Vector<Player> getCasualties() {
		return casualties;
	}

	/**
	 * Get the score of team 0 or team 1.
	 * @param index The index (0 or 1) of the team to get the score for.
	 * @return The score of team with specified index (0 or 1).
	 * @throws IndexOutOfBoundsException If the index is not 0 or 1.
	 */
	public int getScoreFromTeam(int index) {
		if(index == 0 || index == 1) return score[index];
		else {
			getParent().log(Level.SEVERE, "Tried to get score from team with illegal index "+index+". (use 0 or 1)");
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Get team 0 or team 1.
	 * @param index The index (0 or 1) of the team to get.
	 * @return The team with specified index (0 or 1).
	 * @throws IndexOutOfBoundsException If the index is not 0 or 1.
	 */
	public Team getTeam(int index) {
		if(index == 0 || index == 1) return teams[index];
		else {
			getParent().log(Level.SEVERE, "Tried to get team with illegal index "+index+". (use 0 or 1)");
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Set team 0 or team 1.
	 * @param index The index (0 or 1) of the team to set.
	 * @param team The team to set at index (0 or 1).
	 * @throws IndexOutOfBoundsException If the index is not 0 or 1.
	 */
	protected void setTeam(int index, Team team) {
		if(index == 0 || index == 1) teams[index] = team;
		else {
			getParent().log(Level.SEVERE, "Tried to set team with illegal index "+index+". (use 0 or 1)");
			throw new IndexOutOfBoundsException();
		}
	}

	protected void addAvailableTeam(Team team) {
		availableTeams.add(team);
	}
	
	public abstract void touchdown(Team t);
	
	
	public void addCasualty(Player p){
		casualties.addElement(p);
	}
	
	protected void countUpScore(Team t){
		if(t == teams[0]){
			countUpScore(0);
		}else if(t == teams[1]){
			countUpScore(1);
		}
	}
	
	protected void countUpScore(int teamIndex){
		score[teamIndex]++;
	}
	
	public void sendGame(){
		sendTeam(0);
		sendTeam(1);
		sendGamePhase();
		sendScore();
		sendRoundCount();
		sendBallPos();

	}

	protected void sendGamePhase() {
		sendMessage(coaches[0], SBProtocolCommand.EVENT, new SBProtocolParameterArray(SBProtocolMessage.EVENT_SEND_GAME_PHASE, gamePhase+""));
		sendMessage(coaches[1], SBProtocolCommand.EVENT, new SBProtocolParameterArray(SBProtocolMessage.EVENT_SEND_GAME_PHASE, gamePhase+""));
	}
	
	protected void sendScore(){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_SCORE));
		parameters.addParameter(new SBProtocolParameter(score[0] + ""));
		parameters.addParameter(new SBProtocolParameter(score[1] + ""));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	public void sendPlayer(Player p){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		int actingUserIndex;
		if(p.getTeam().equals(teams[0])){
			actingUserIndex = 0;
		}else if(p.getTeam().equals(teams[1])){
			actingUserIndex = 1;
		}else{
			return;
		}
		int playerIndex = -1;
		for(int i = 0; i < teams[actingUserIndex].getPlayers().size(); i++){
			if(teams[actingUserIndex].getPlayers().get(i).equals(p)){
				playerIndex = i;
				break;
			}
		}
		if(playerIndex == -1){return;}
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_PLAYER));
		parameters.addParameter(new SBProtocolParameter(actingUserIndex+""));
		parameters.addParameter(new SBProtocolParameter(playerIndex+""));
		parameters.addParameter(new SBProtocolParameter(p.getName()));
		parameters.addParameter(new SBProtocolParameter(p.invokeGetBe()+""));
		parameters.addParameter(new SBProtocolParameter(p.invokeGetSt()+""));
		parameters.addParameter(new SBProtocolParameter(p.invokeGetGe()+""));
		parameters.addParameter(new SBProtocolParameter(p.invokeGetRs()+""));
		parameters.addParameter(new SBProtocolParameter(p.invokeGetRemainingBe()+""));
		parameters.addParameter(new SBProtocolParameter(p.invokeGetPlayerCondition().name()));
		parameters.addParameter(new SBProtocolParameter((int)p.getPos().x +""));
		parameters.addParameter(new SBProtocolParameter((int)p.getPos().y +""));
		parameters.addParameter(new SBProtocolParameter(p.isHoldingBall() +""));
		parameters.addParameter(new SBProtocolParameter(p.getRedCard()+ ""));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	protected void sendWeather(){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_WEATHER));
		parameters.addParameter(new SBProtocolParameter(getWeather().name()));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	protected void sendNewTeam(int teamIndex){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_NEW_TEAM));
		parameters.addParameter(new SBProtocolParameter(teamIndex + ""));
		parameters.addParameter(new SBProtocolParameter(getTeam(teamIndex).getName()));
		parameters.addParameter(new SBProtocolParameter(getTeam(teamIndex).getType()));
		for(int i = 0; i < teams[teamIndex].getPlayers().size(); i++){
			parameters.addParameter(new SBProtocolParameter(teams[teamIndex].getPlayers().get(i).getName()));
		}
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	void sendTeam(int teamIndex){
		for(int i = 0; i < teams[teamIndex].getPlayers().size(); i++){
			sendPlayer(teams[teamIndex].getPlayers().get(i));
		}
		sendMovingPlayer(teamIndex);
		sendBlitzPassFoul(teamIndex);
	}
	
	void sendBlitzPassFoul(int teamIndex){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_BLITZ_PASS_FOUL));
		parameters.addParameter(new SBProtocolParameter(teamIndex + ""));
		int blitz = 0;
		int pass = 0;
		int foul = 0;
		if(getTeam(teamIndex).getBlitz()) blitz = 1;
		if(getTeam(teamIndex).getPass()) pass = 1;
		if(getTeam(teamIndex).getFoul()) foul = 1;
		parameters.addParameter(new SBProtocolParameter(blitz + ""));
		parameters.addParameter(new SBProtocolParameter(pass + ""));
		parameters.addParameter(new SBProtocolParameter(foul + ""));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	void sendMovingPlayer(int teamIndex){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_MOVING_PLAYER));
		parameters.addParameter(new SBProtocolParameter(teamIndex + ""));
		int movingPlayerIndex = -1;
		for(int i = 0; i < teams[teamIndex].getPlayers().size(); i++){
			if(teams[teamIndex].getMovingPlayer() == teams[teamIndex].getPlayers().get(i)){
				movingPlayerIndex = i;
			}
		}
		parameters.addParameter(new SBProtocolParameter(movingPlayerIndex + ""));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	void sendRoundCount(){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_ROUND_COUNT));
		parameters.addParameter(new SBProtocolParameter(roundCount + ""));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	public void sendBallPos(){
		SBProtocolParameterArray parameters = new SBProtocolParameterArray();
		parameters.addParameter(new SBProtocolParameter(SBProtocolMessage.EVENT_SEND_BALL_POS));
		parameters.addParameter(new SBProtocolParameter((int)getPitch().getBallPos().x + ""));
		parameters.addParameter(new SBProtocolParameter((int)getPitch().getBallPos().y + ""));
		sendMessage(coaches[0], SBProtocolCommand.EVENT, parameters);
		sendMessage(coaches[1], SBProtocolCommand.EVENT, parameters);
	}
	
	public void sendMessage(User destinationUser, SBProtocolCommand command, SBProtocolParameterArray parameters) {
		SBProtocolMessage message = new SBProtocolMessage(this.getParent().UID, command, parameters);
		sendMessage(destinationUser.getUID(), message);
	}
	
	public Team getAvailableTeam(int index){
		return availableTeams.get(index);
	}

	public int getNumberOfAvailableTeams() {
		return availableTeams.size();
	}
	
	protected Team setTeamType(SBProtocolMessage message, int typeIndex){//If the message is sent to the client, the typeIndex must be set to 3, if sent to server, then it must be 2
		int teamTypeIndex = -1;
		for(int i = 0; i < availableTeams.size(); i++){
			if(availableTeams.get(i).getType().equalsIgnoreCase(message.getParameterContent(typeIndex))){
				teamTypeIndex = i;
			}
		}
		Team newTeam = null;
		if(teamTypeIndex < availableTeams.size() && teamTypeIndex >= 0) newTeam = getAvailableTeam(teamTypeIndex);
		return newTeam;
	}
		
	/**
	 * finds the internal User-index in this Game that sent a message
	 * @param message the message you want to find out the user
	 * @return 0 if its the first User, 1 if its the second, -1 if its nether and sends a failure message
	 */
	public int findUserIndex(SBProtocolMessage message){
		if(message.getUID().equals(coaches[0].getUID()) || message.getUID().equals(coaches[1].getUID())){
			return findUserIndex(message.getUID());
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_YOU_DONT_BELONG_HERE);
			return -1;
		}
	}
	
	public int findUserIndex(UUID UID){
		if(UID.equals(coaches[0].getUID())){
			return 0;
		}else if(UID.equals(coaches[1].getUID())){
			return 1;
		}else{
			return -1;
		}
	}
	
	protected void clearAllPlayerPos(){
		for(Player p:getTeam(0).getPlayers()){
			p.invokeClearPosition();
			p.invokeSetIsHoldingBall(false);
		}
		for(Player p:getTeam(1).getPlayers()){
			p.invokeClearPosition();
			p.invokeSetIsHoldingBall(false);
		}	
	}

	/**
	 * Set the game phase of this match.
	 * <br><code><b>0</b></code>: Team Choosing Phase
	 * <br><code><b>1</b></code>: Team Setup Phase
	 * <br><code><b>2</b></code>: Kick Phase
	 * <br><code><b>3</b></code>: Normal Playing Phase
	 * <br><code><b>4</b></code>: Finishing Phase
	 * <br><code><b>5</b></code>: WaitingPhase
	 * @param i The game phase of this match.
	 */
	public void setGamePhase(int i){
		if(i >= 0 && i < 6){
			this.gamePhase = i;
		}
	}

	public int getGamePhase() {
		return gamePhase;
	}

	public UUID getMatchID() {
		return matchID;
	}
	
	public Weather getWeather(){
		return weather;
	}
	
	protected void setWeather(Weather weather){
		this.weather = weather;
	}
	
	protected void checkForBlizzard(){
		if(weather == Weather.BLIZZARD){
			RuleThrow.LONG_PASS = 8;
			RuleThrow.LONG_BOMB = 8;
		}
	}
	
	protected void checkForSwelteringHeat(Vector<Player> players){
		if(getWeather().equals(Weather.SWELTERING_HEAT)){
			for(Player p: players){
				if(d6.throwDie() == 1){
					p.invokeSetPlayerCondition(PlayerCondition.KO);
				}
			}
		}
	}
}