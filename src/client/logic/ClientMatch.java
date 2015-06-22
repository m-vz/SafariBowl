package client.logic;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.*;
import javax.vecmath.Vector2d;

import GUI.SBFrame;
import client.Client;
import gameLogic.*;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameterArray;
import server.logic.User;
import util.SBApplication;

/**
 * A game on a client.
 */
public class ClientMatch extends GameController {

	protected String[] coachesNames = new String[2]; // the logged-in users coaching the opposing teams.
	protected String winner;
	public int settingCoolLayout = 0;
	SBProtocolMessage initiateKickMessage = null, giveBallMessage = null;

	/**
	 * The client does not have access to the Users so it needs to create a match with only the names of the coaches.
	 * @param parent The client that plays this game.
	 * @param coach1 The name of the first coach.
	 * @param coach2 The name of the second coach.
	 */
	public ClientMatch(SBApplication parent, String coach1, String coach2) {
		super(parent, new User(coach1), new User(coach2));
		for(Team team: parent.getTeamManager().getTeamBlueprints()) addAvailableTeam(team);
		this.coachesNames = new String[]{coach1, coach2};
		setUp();
	}

	/**
	 * The method called by all constructors.
	 */
	private void setUp() {
		setRunning(true);
		// start message listener
		start();
	}

	@Override
	public void run() {
		MessageListener messageListener = new MessageListener(this) {
			@Override
			public void processMessage(SBProtocolMessage message) {
				try{
					switch(message.getCommand()){
						case ACTIO:
							processMessageACTIO(message);
							break;
						case EVENT:
							processMessageEVENT(message);
							break;
						case SRNDR:
							processMessageSRNDR(message);
							break;
						default:
							returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);
							break;
					}
				}catch(IndexOutOfBoundsException e){
					getClient().logStackTrace(e);
					returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);
				}
			}

			@Override
			public void processAnswer(SBProtocolMessage answer) {
				try {
					SBProtocolMessage message = null;
					for(SBProtocolMessage messageThatIsPotentiallyAnswered: getClient().getProtocolManager().getUnansweredMessages()) // for all unanswered messages
						if(messageThatIsPotentiallyAnswered.getMID().equals(UUID.fromString(answer.getParameterContent(0)))) { // get the message whose MID equals the MID in the answer
							message = messageThatIsPotentiallyAnswered;
							break;
						}
					getClient().getProtocolManager().removeUnansweredMessage(message);

					if(message != null) {

						switch (answer.getModule()) {
							case SUC:
								processAnswerSUC(answer, message);
								break;
							case FAI:
								processAnswerFAI(answer, message);
								break;
							default:
								break;
						}

					} else getParent().log(Level.FINER, "Received answer but found no message it belonged to: " + answer.toStringShortenUUID() + " Ignoring it.");

				} catch (IndexOutOfBoundsException e) { // Don't return failure message because answers don't expect (e.g. ignore) answers anyway
					getParent().log(Level.WARNING, "Index out of bounds at process answer.");
				}
			}

			private void processMessageSRNDR(SBProtocolMessage message) {


			}

			private void processMessageEVENT(final SBProtocolMessage message) {
				String eventString = message.getParameterContent(0);

				if(eventString.equals(SBProtocolMessage.EVENT_INITIATE_KICK)) {

					if(getClient().getFrame().getGamePanel() != null) {
						getClient().getFrame().getGamePanel().setCanKickOff(true);
						getClient().getFrame().getGamePanel().setIsYourTurn(false);
					}
					initiateKickMessage = message;

				} else if(eventString.equals(SBProtocolMessage.EVENT_GIVE_THE_BALL_TO_SOMEONE)) {

					if(getClient().getFrame().getGamePanel() != null) {
						getClient().getFrame().getGamePanel().setCanGiveBall(true);
					}
					giveBallMessage = message;

				} else if(eventString.equals(SBProtocolMessage.EVENT_SETUP_YOUR_TEAM)) {

					if(getClient().getFrame().getGamePanel() != null) {
						getClient().getFrame().getGamePanel().setCanSetUp(true);
						getClient().getFrame().getGamePanel().setHasSetUpTeam(false);
					}

				} else if(eventString.equals(SBProtocolMessage.EVENT_OPPONENT_SURRENDERED)) {
					setWinnerString(getClient().getUsername());
					finishGame();

				} else if(eventString.equals(SBProtocolMessage.EVENT_WON_GAME)) {
					setWinnerString(getClient().getUsername());
					finishGame();

				} else if(eventString.equals(SBProtocolMessage.EVENT_LOST_GAME)) {
					setWinnerString(getOpponentString(getClient().getUsername()));
					finishGame();

				} else if(eventString.equals(SBProtocolMessage.EVENT_DRAW)) {
					setWinnerString("");
					finishGame();

				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_NEW_TEAM)) {
					setNewTeam(message);
					
				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_PLAYER)) {
					adjustPlayer(message);

				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_ROUND_COUNT)) {
					setRoundCount(message);
					
				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_WEATHER)){
					Weather newWeather = Weather.valueOf(message.getParameterContent(1));
					setWeather(newWeather);
					checkForBlizzard();
				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_MOVING_PLAYER)){
					try{
						int teamIndex = Integer.parseInt(message.getParameterContent(1));
						int movingPlayerIndex = Integer.parseInt(message.getParameterContent(2));
						if(movingPlayerIndex == -1){
							teams[teamIndex].setMovingPlayer(null);
						}else{
							teams[teamIndex].setMovingPlayer(teams[teamIndex].getPlayers().get(movingPlayerIndex));
						}
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}catch(IndexOutOfBoundsException e) {
                        returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
                    }
				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_BALL_POS)) {
					try{
						int x = Integer.parseInt(message.getParameterContent(1));
						int y = Integer.parseInt(message.getParameterContent(2));
						getPitch().adjustBallPos(new Vector2d(x, y));
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_SCORE)) {
					try{
						score[0] = Integer.parseInt(message.getParameterContent(1));
						score[1] = Integer.parseInt(message.getParameterContent(2));
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_BLITZ_PASS_FOUL)){
					try{
						int teamIndex = Integer.parseInt(message.getParameterContent(1));
						if(Integer.parseInt(message.getParameterContent(2)) == 1){
							getTeam(teamIndex).setBlitz(true);
						}else{
							getTeam(teamIndex).setBlitz(false);
						}
						if(Integer.parseInt(message.getParameterContent(3)) == 1){
							getTeam(teamIndex).setPass(true);
						}else{
							getTeam(teamIndex).setPass(false);
						}
						if(Integer.parseInt(message.getParameterContent(4)) == 1){
							getTeam(teamIndex).setFoul(true);
						}else{
							getTeam(teamIndex).setFoul(false);
						}
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_YOUR_TURN)) {

					if(getClient().getFrame().getGamePanel() != null) getClient().getFrame().getGamePanel().setIsYourTurn(true);

				} else if(eventString.equals(SBProtocolMessage.EVENT_ENDED_TURN)) {

					if(getClient().getFrame().getGamePanel() != null) getClient().getFrame().getGamePanel().setIsYourTurn(false);

				} else if(eventString.equals(SBProtocolMessage.EVENT_SEND_GAME_PHASE)) {

					setGamePhase(Integer.parseInt(message.getParameterContent(1)));

				} else if(eventString.equals(SBProtocolMessage.EVENT_WHAT_DIE)) {
					int numberOfChoices = message.getParameters().size()-1;
					if(numberOfChoices > 0) { // needs at least one die to choose from

                        int[] dieToChooseFrom = new int[numberOfChoices];
                        for(int i = 0; i < numberOfChoices; i++) {
                            try {
                                dieToChooseFrom[i] = Integer.parseInt(message.getParameterContent(i + 1));
                            } catch(NumberFormatException e) {
                                getClient().log(Level.WARNING, "Received illegal die choice. Ignoring.");
                            }
                        }

                        getClient().getFrame().getGamePanel().setDiceAPIDice(dieToChooseFrom);
						getClient().getFrame().getGamePanel().setDiceAPI(true);
                        getClient().getFrame().getGamePanel().setAPIMessage(message);

					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_WHICH_DIRECTION)) {

					int directionButtonSize = 100;
					final JFrame directionChoiceFrame = new JFrame("Choose direction");
					String[] directionStrings = new String[]{"up left", "up", "up right", "left", "right", "down left", "down", "down right"};
					final int[][] directionAnswers = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
					Box row1 = Box.createHorizontalBox(), row2 = Box.createHorizontalBox(), row3 = Box.createHorizontalBox();
					for (int i = 0; i < 8; i++) {
						JButton directionButton = new JButton(directionStrings[i]);
						final int returnI = i;
						directionButton.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								message.returnSuccessMessage(getClient().getUID(), SBProtocolMessage.WORKD_DIRECTION, directionAnswers[returnI][0] + "", directionAnswers[returnI][1] + "");
								directionChoiceFrame.setVisible(false);
								directionChoiceFrame.dispose();
							}
						});
						directionButton.setMinimumSize(new Dimension(directionButtonSize, directionButtonSize));
						directionButton.setPreferredSize(new Dimension(directionButtonSize, directionButtonSize));
						if(i < 3) row1.add(directionButton);
						else if(i < 5) row2.add(directionButton);
						else row3.add(directionButton);
						if(i == 3) row2.add(Box.createRigidArea(new Dimension(directionButtonSize, directionButtonSize)));
					}
					row1.setSize(new Dimension(3*directionButtonSize, directionButtonSize));
					row2.setSize(new Dimension(3*directionButtonSize, directionButtonSize));
					row3.setSize(new Dimension(3*directionButtonSize, directionButtonSize));
					directionChoiceFrame.add(row1);
					directionChoiceFrame.add(row2);
					directionChoiceFrame.add(row3);
					directionChoiceFrame.setLayout(new BoxLayout(directionChoiceFrame.getContentPane(), BoxLayout.Y_AXIS));
					directionChoiceFrame.setUndecorated(true);
					directionChoiceFrame.setSize(new Dimension(3 * directionButtonSize, 3 * directionButtonSize));
					SBFrame.center(directionChoiceFrame);
					directionChoiceFrame.setVisible(true);
					directionChoiceFrame.toFront();

				} else if(eventString.equals(SBProtocolMessage.EVENT_FOLLOW)) {

                    if(getClient().getFrame().getGamePanel().getFollowAnswer())
                        message.returnSuccessMessage(getClient().getUID(), SBProtocolMessage.WORKD_DECIDED, "1");
                    else
                        message.returnSuccessMessage(getClient().getUID(), SBProtocolMessage.WORKD_DECIDED, "0");

				} else if(eventString.equals(SBProtocolMessage.EVENT_API_AIM)){
					try{
						int     playerIndex = Integer.parseInt(message.getParameterContent(2)),
                                distance = Integer.parseInt(message.getParameterContent(3));
						getClient().getFrame().getGamePanel().setAimAPIIndex(playerIndex);
						getClient().getFrame().getGamePanel().setAimAPIDistance(distance);
						getClient().getFrame().getGamePanel().setAimAPI(true);
						getClient().getFrame().getGamePanel().setAPIMessage(message);
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_API_CHOICE)){
					try{
						Player[] players = new Player[(message.getParameters().size()-2)/2];
						for(int i = 0; i < (message.getParameters().size()-2)/2; i++){
							players[i] = teams[Integer.parseInt(message.getParameterContent(i*2+2))].getPlayers().get(Integer.parseInt(message.getParameterContent(i*2+3)));
						}
						getClient().getFrame().getGamePanel().setChoiceAPIPlayers(players);
						getClient().getFrame().getGamePanel().setChoiceAPI(true);
						getClient().getFrame().getGamePanel().setAPIMessage(message);
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_API_FIELD)){
					try{
						Vector2d[] fields = new Vector2d[(message.getParameters().size()-2)/2];
						for(int i = 0; i < (message.getParameters().size()-2)/2; i++){
							fields[i] = new Vector2d(Integer.parseInt(message.getParameterContent(i*2+2)), Integer.parseInt(message.getParameterContent(i*2+3)));
						}
						getClient().getFrame().getGamePanel().setFieldAPIFields(fields);
						getClient().getFrame().getGamePanel().setFieldAPI(true);
						getClient().getFrame().getGamePanel().setAPIMessage(message);
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_API_HIGHLIGHT)){
					try{
						if(message.getParameters().size()-1 < 1){
							getClient().getFrame().getGamePanel().clearHighlightAPIPositionsAndColors();
						}else{
							Vector2d[] fields = new Vector2d[(message.getParameters().size()-1)/6];
							Color[] colors = new Color[(message.getParameters().size()-1)/6];
							for(int i = 0; i < fields.length; i++){
								fields[i] = new Vector2d(Integer.parseInt(message.getParameterContent(i*6+1)), Integer.parseInt(message.getParameterContent(i*6+2)));
								colors[i] = new Color(Integer.parseInt(message.getParameterContent(i*6+3)), Integer.parseInt(message.getParameterContent(i*6+4)), Integer.parseInt(message.getParameterContent(i*6+5)), Integer.parseInt(message.getParameterContent(i*6+6)));
							}
							getClient().getFrame().getGamePanel().setHighlightAPIPositionsAndColors(fields, colors);
						}
					}catch(NumberFormatException e){
						returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
					}
				} else if(eventString.equals(SBProtocolMessage.EVENT_SHOW_ME)){

					getClient().getFrame().getGamePanel().showMe(message.getParameterContent(1), message.getParameterContent(2));

				}
			}

			private void processMessageACTIO(SBProtocolMessage message) {



			}

			private void processAnswerSUC(SBProtocolMessage answer, SBProtocolMessage message) {
				switch (message.getCommand()) {
					case SRNDR:
						setWinnerString(getOpponentString(getClient().getUsername()));
						finishGame();
						break;
					case ACTIO:
						String actionString = message.getParameterContent(0);

						if(actionString.equals(SBProtocolMessage.ACTIO_SET_PLAYER) && settingCoolLayout != 0) {

							if(settingCoolLayout > 0) getClient().coolLineup(settingCoolLayout + 1);
							else getClient().coolLineup(settingCoolLayout - 1);

						} else if(actionString.equals(SBProtocolMessage.ACTIO_SET_TEAM)) {

							if(getClient().getFrame().getGamePanel() != null) {
								getClient().getFrame().getGamePanel().setChoseTeam(true);
								getClient().getFrame().getGamePanel().setIsYourTurn(false);
							}

						}
						break;
				}
			}

			private void processAnswerFAI(SBProtocolMessage answer, SBProtocolMessage message) {
				switch (message.getCommand()) {
					case SRNDR:
						getParent().log(Level.WARNING, "Surrender failed. You're an even greater turtle than I thought at first.");
						break;
					case ACTIO:
//						if(message.getParameterContent(1).equals(SBProtocolMessage.ACTIO_SET_PLAYER) && settingCoolLayout < 0) // REMOVE THIS
//							getClient().coolLineup(1); 										 // REMOVE THIS
						if(answer.getParameterContent(1).equals(SBProtocolMessage.FAILD_NOT_ENOUGH_PLAYERS)) {
							getClient().getFrame().getGamePanel().setChoseTeam(false);
							getClient().getFrame().getGamePanel().setHasSentChooseTeam(false);
						}else if(answer.getParameterContent(1).equals(SBProtocolMessage.FAILD_NOT_ENOUGH_MONEY)) {
							getClient().getFrame().getGamePanel().setChoseTeam(false);
							getClient().getFrame().getGamePanel().setHasSentChooseTeam(false);
						}
						break;
					case EVENT:
						if(answer.getParameterContent(1).equals(SBProtocolMessage.FAILD_INVALID_TEAMSETUP)) {
							// set back to false so player can try setting up again
							getClient().getFrame().getGamePanel().setHasSetUpTeam(false);
                        }
                        break;
				}
			}

		};
		messageListener.start();

		while(isRunning()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				getClient().logStackTrace(e);
			}
		}
	}

	// ACTIONS

	public void giveBall(Player playerReceivingBall) {
		if(giveBallMessage != null) {
			SBProtocolParameterArray kickParams = new SBProtocolParameterArray(SBProtocolMessage.WORKD_GIVE_BALL, (playerReceivingBall.getId()-1)+"");
			giveBallMessage.returnSuccessMessage(getClient().getUID(), kickParams);
		} else getClient().log(Level.WARNING, "Did not get permission to give ball. Not giving ball then.");
	}

	public void kick(PitchField kickDestination) {
		if(initiateKickMessage != null) {
			SBProtocolParameterArray kickParams = new SBProtocolParameterArray(SBProtocolMessage.WORKD_KICK, (int) kickDestination.getPos().x+"", (int) kickDestination.getPos().y+"");
			initiateKickMessage.returnSuccessMessage(getClient().getUID(), kickParams);
		} else getClient().log(Level.WARNING, "Did not get permission to kick. Not kicking then.");
	}

	// HELPERS

	public SBProtocolMessage sendMessage(SBProtocolCommand command, String... parameters){
		SBProtocolMessage message = new SBProtocolMessage(getParent().UID, command, parameters);
		sendMessage(new UUID(0, 0), message);
		return message;
	}

	@Override
	public void sendMessage(User destinationUser, SBProtocolCommand command, String... parameters) {
		sendMessage(command, parameters);
	}

	@Override
	public void touchdown(Team t) {
		
	}
	
	/**
	 * Create a string of this game that can be written to file and read again.
	 * @return The string that represents this game.
	 */
	public String toLogString() {
		String beginParam = "\", \"", beginArray = "\", [\"", endArray = "\"], \"", beginArrayFromArray = "\"], [\"";
		/*String r = "[[\""+escape(coachesNames[0]) + beginArray + escape(getTeam(0).getName()) + beginParam + escape(getTeam(0).getType()) + endArray + escape(score[0]+"") + beginArrayFromArray
				+ escape(coachesNames[1]) + beginArray + escape(getTeam(1).getName()) + beginParam + escape(getTeam(1).getType()) + endArray + escape(score[1]+"") + endArray
				+ escape(winner) + beginParam
				+ escape((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(finishedOn));*/
		String team1Coach="", team1Name="", team1Type="", team1Score=escape(score[0]+""),
				team2Coach="", team2Name="", team2Type="", team2Score=escape(score[1]+""),
				winnerName="", finishedOnString="";
		if(coaches[0] != null) team1Coach = escape(coachesNames[0]);
		if(teams[0] != null) team1Name = escape(teams[0].getName());
		if(teams[0] != null) team1Type = escape(teams[0].getType());
		if(coaches[1] != null) team2Coach = escape(coachesNames[1]);
		if(teams[1] != null) team2Name = escape(teams[1].getName());
		if(teams[1] != null) team2Type = escape(teams[1].getType());
		if(winner != null) winnerName = escape(winner);
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
	}

	/**
	 * Write this client match to file.
	 */
	public void logClientMatch() {
		logGame(CLIENT_GAMES_FILE);
	}

	/**
	 * initiates everything that is necessary at the end of a game
	 */
	private void finishGame(){
		if(getWinnerString().equals("")){
			getParent().log(Level.INFO, "The match ended in a Draw!");
		}else{
			getParent().log(Level.INFO, "The match ended and the winner is " + getWinnerString() + "!");
		}
		setRunning(false);
		getClient().finishedGame();
		logClientMatch();
	}

	public Client getClient() {
		return (Client) getParent();
	}

	/**
	 * The client match always returns null here because it has no access to Users.
	 * @param user The user whose opponent is searched. Is ignored in client match.
	 * @return Always null because the client match has no access to Users.
	 */
	public User getOpponent(User user) {
		getParent().log(Level.WARNING, "Tried to get opponent from within client match. Returning null because client doesn't know the opponent user object.");
		return null;
	}

	/**
	 * The client match always returns null here because it has no access to Users.
	 * @param index The index of the coach to get (0 or 1). Is ignored in client match.
	 * @return Always null because the client match has no access to Users.
	 */
	public User getOpponent(int index) {
		getParent().log(Level.WARNING, "Tried to get opponent from within client match. Returning null because client doesn't know the opponent user object.");
		return null;
	}

	/**
	 * The client match stores the coaches as strings. Get the name of the opponent of the coach at given index.
	 * @param index The index of the coach whose opponent is searched.
	 * @return The name of the opponent of the coach at given index. Null if index is not 0 or 1.
	 */
	public String getOpponentString(int index) {
		if(index == 0) return coachesNames[1];
		if(index == 1) return coachesNames[0];
		else return null;
	}

	/**
	 * The client match stores the coaches as strings. Get the name of the opponent of the coach with a given name.
	 * @param coachName The name of the coach whose opponent is searched.
	 * @return The name of the opponent of the coach with given name. Null if index the coach is not in this game.
	 */
	public String getOpponentString(String coachName) {
		if(coachesNames[0].equals(coachName)) return getOpponentString(0);
		if(coachesNames[1].equals(coachName)) return getOpponentString(1);
		else return null;
	}

	public String getWinnerString() {
		return winner;
	}

	public void setWinnerString(String winner) {
		this.winner = winner;
	}
	
	private void setNewTeam(SBProtocolMessage message){
		int actingUserIndex;
		try{
			actingUserIndex = Integer.parseInt(message.getParameterContent(1));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
			return;
		}
		if(!(actingUserIndex == 0 || actingUserIndex == 1)){
			returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
			return;
		}
		Team newTeam = setTeamType(message, 3);
		if(newTeam!=null){
			newTeam = new Team(newTeam, message.getParameterContent(2), this, getUser(actingUserIndex));
			int existingPlayers = 0;
			for(int i = 4; i < message.getParameters().size(); i++){
				for(int j = 0; j < newTeam.getAvailablePlayers().size(); j++){
					if(message.getParameterContent(i).equalsIgnoreCase(newTeam.getAvailablePlayers().get(j).getName())){
						newTeam.addPlayer(newTeam.getAvailablePlayers().get(j));
						existingPlayers++;
					}
				}
			}
			setTeam(actingUserIndex, newTeam);
			if(existingPlayers == message.getParameters().size()-4){
				getPitch().setTeam(actingUserIndex, newTeam);
			}else{
				newTeam.clearPlayers();
				returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
			}
		}
	}
	
	private void adjustPlayer(SBProtocolMessage message){
		try{
			int actingUserIndex = Integer.parseInt(message.getParameterContent(1));
			int playerIndex = Integer.parseInt(message.getParameterContent(2));
			int playerPosX = Integer.parseInt(message.getParameterContent(10));
			int playerPosY = Integer.parseInt(message.getParameterContent(11));
			if(!(actingUserIndex == 0 || actingUserIndex == 1)){
				return;
			}
			if(playerIndex < 0 || playerIndex >= teams[actingUserIndex].getPlayers().size() || playerPosX < -1 || playerPosX > 25 || playerPosY < -1 || playerPosY > 14){
				return;
			}
			teams[actingUserIndex].getPlayers().get(playerIndex).setName(message.getParameterContent(3));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetBe(Integer.parseInt(message.getParameterContent(4)));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetSt(Integer.parseInt(message.getParameterContent(5)));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetGe(Integer.parseInt(message.getParameterContent(6)));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetRs(Integer.parseInt(message.getParameterContent(7)));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetRemainingBe(Integer.parseInt(message.getParameterContent(8)));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetPlayerCondition(PlayerCondition.valueOf(message.getParameterContent(9)));
			try{
				if(playerPosX == -1 && playerPosY == -1){
					teams[actingUserIndex].getPlayers().get(playerIndex).invokeClearPosition();
				}else{
					teams[actingUserIndex].getPlayers().get(playerIndex).getPosition().adjustPlayer(null);
					teams[actingUserIndex].getPlayers().get(playerIndex).invokeAdjustPosition(getPitch().getFields()[playerPosX][playerPosY]);
				}
			}catch(NullPointerException e){
				getClient().logStackTrace(e);
			}
			teams[actingUserIndex].getPlayers().get(playerIndex).getPosition().adjustPlayer(teams[actingUserIndex].getPlayers().get(playerIndex));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetIsHoldingBall(Boolean.parseBoolean(message.getParameterContent(12)));
			teams[actingUserIndex].getPlayers().get(playerIndex).invokeSetRedCard(Boolean.parseBoolean(message.getParameterContent(13)));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
		}
	}
	
	private void setRoundCount(SBProtocolMessage message){
		try{
			roundCount = Integer.parseInt(message.getParameterContent(1));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
		}
	}
	
	public void sendSpecialRule(Player player, String name){
		int playerIndex = -1;
		for(int i = 0; i < player.getTeam().getPlayers().size(); i++){
			if(player == player.getTeam().getPlayers().get(i)){
				playerIndex = i;
			}
		}
		sendMessage(SBProtocolCommand.ACTIO, SBProtocolMessage.ACTIO_SPCL, playerIndex + "", name);
	}
	
	// API

    public void sendDiceAPI(SBProtocolMessage message, int choice){
        returnSuccessMessage(message, SBProtocolMessage.WORKD_DIE_CHOSEN, choice+"");
    }

	public void sendAimAPI(SBProtocolMessage message, Vector2d destination){
		returnSuccessMessage(message, SBProtocolMessage.EVENT_API_AIM, (int)destination.x + "", (int)destination.y + "");
	}
	
	public void sendChoiceAPI(SBProtocolMessage message, int playerIndex){
		returnSuccessMessage(message, SBProtocolMessage.EVENT_API_CHOICE, playerIndex + "");
	}
	
	public void sendFieldAPI(SBProtocolMessage message, Vector2d choice){
		returnSuccessMessage(message, SBProtocolMessage.EVENT_API_FIELD, (int)choice.x + "", (int)choice.y + "");
	}
}
