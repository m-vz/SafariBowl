package server.logic;

import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import javax.vecmath.Vector2d;

import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameterArray;
import server.Server;
import util.SBApplication;
import gameLogic.GameController;
import gameLogic.Pitch;
import gameLogic.PitchField;
import gameLogic.Player;
import gameLogic.PlayerCondition;
import gameLogic.Team;
import gameLogic.Weather;
import gameLogic.rules.*;

/**
 * A game on a server.
 */
public class ServerMatch extends GameController {

	private boolean[] teamsChosen = new boolean[]{false, false}; //Determines whether the Teams are chosen
	private boolean[] teamsSet = new boolean[]{false, false}; //Determines whether the Teams are set on the Pitch
	private volatile Player currentActorWaitingForAnswer, currentDefenderWaitingForAnswer, currentPusherWaitingForAnswer;
	private volatile Vector2d currentDefenderFieldWaitingForAnswer, currentBackUpPosWaitingForAnswer;
	private volatile Vector<Player> currentPlayersBeingPushed = new Vector<Player>();
	private volatile Vector<Vector2d> currentPlayerPositionsBeingPushed = new Vector<Vector2d>(), currentHighlitedFields = new Vector<Vector2d>();
	private volatile int currentModificaorWaitingForAnswer;
	private volatile SBProtocolMessage currentMessageWaitingForAnswer;
	
	/**
	 * Initializes the Match on the Server with the super Constructor from the Class Game, and then starts the Match tread itself.
	 * @param parent The parent client or server (SBApplication) to create this game for.
	 * @param coach1 The coach of the first team.
	 * @param coach2 The coach of the second team.
	 */
	public ServerMatch(SBApplication parent, User coach1, User coach2) {
		super(parent, coach1, coach2);
		for(Team team: parent.getTeamManager().getTeamBlueprints()) addAvailableTeam(team);
		setFirstPlayer();
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
					boolean allowed = checkUserTurn(message);
					if(allowed){
						switch(message.getCommand()){
						case ACTIO:
							processMessageACTIO(message);
							sendGame();
							break;
						case EVENT:
							processMessageEVENT(message);
							sendGame();
							break;
						case SRNDR:
							processMessageSRNDR(message);
							break;
						default:
							returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);
							break;
						}
					}else{
						returnFailureMessage(message, SBProtocolMessage.FAILD_NOT_YOUR_TURN);
					}
				}catch(IndexOutOfBoundsException e){
					e.printStackTrace();
					returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);
				}
			}

			@Override
			public void processAnswer(SBProtocolMessage answer) {
				try {
					SBProtocolMessage message = null;
					try {
						for(SBProtocolMessage messageThatIsPotentiallyAnswered: ((Server) getParent()).getProtocolManager().getUnansweredMessages()) { // for all unanswered messages
							message = messageThatIsPotentiallyAnswered;
							if (messageThatIsPotentiallyAnswered.getMID().equals(UUID.fromString(answer.getParameterContent(0)))) { // get the message whose MID equals the MID in the answer
								message = messageThatIsPotentiallyAnswered;
								break;
							} else message = null;
						}
					} catch (ConcurrentModificationException ignored) {} // ignoring strange ConcurrentModificationException that does not affect any arse

					if(message != null) {

						boolean delete;

						switch (answer.getModule()) {
							case SUC:
								delete = processAnswerSUC(answer, message);
								break;
							case FAI:
								delete = processAnswerFAI(answer, message);
								break;
							default:
								delete = false;
								break;
						}

						if(delete) ((Server) getParent()).getProtocolManager().removeUnansweredMessage(message);

					} else getParent().log(Level.WARNING, "Received answer but found no message it belonged to: " + answer.toStringShortenUUID() + " Ignoring it.");

				} catch (IndexOutOfBoundsException e) { // Don't return failure message because answers don't expect (e.g. ignore) answers anyway
					getParent().log(Level.WARNING, "Index out of bounds at process answer.");
				}
			}

			private void processMessageEVENT(SBProtocolMessage message) {
				String eventString = message.getParameterContent(0);
				if(teamsSet[0] && teamsSet[1] && gamePhase == 3){
					if(eventString.equals(SBProtocolMessage.EVENT_END_TURN)){
						endTurn(message);

					}
				}else{
					if(eventString.equals(SBProtocolMessage.EVENT_ALL_PLAYERS_SET)){
						allPlayersSet(message);
					}else{
						returnFailureMessage(message, SBProtocolMessage.FAILD_WRONG_GAME_PHASE);
					}
				}
			}

			private void processMessageACTIO(SBProtocolMessage message) {
				if(teamsSet[0] && teamsSet[1] && gamePhase == 3){
					if(message.getParameterContent(0).equals(SBProtocolMessage.ACTIO_MOVE)){
						move(message);
					}else if(message.getParameterContent(0).equals(SBProtocolMessage.ACTIO_THRW)){
						pass(message);
					}else if(message.getParameterContent(0).equals(SBProtocolMessage.ACTIO_BLCK)){
						block(message);
					}else if(message.getParameterContent(0).equals(SBProtocolMessage.ACTIO_SPCL)){
						invokeSpecialRule(message);
					}
				}else{
					if(message.getParameterContent(0).equals(SBProtocolMessage.ACTIO_SET_TEAM)){
						setTeam(message);
					}else if(message.getParameterContent(0).equals(SBProtocolMessage.ACTIO_SET_PLAYER) && gamePhase == 1){
						setPlayer(message);
					}else{
						returnFailureMessage(message, SBProtocolMessage.FAILD_WRONG_GAME_PHASE);
					}
				}
			}

			private void processMessageSRNDR(SBProtocolMessage message) {
				User surrenderingUser = ((Server) getParent()).getUserManager().getAuthenticatedUser(message.getUID());
				if(surrenderingUser != null) { // surrendering user is logged in
					if(surrenderingUser.isInGame()) { // surrendering user is currently in a game
						finishGame(surrenderingUser);
						returnSuccessMessage(message);
					} else returnFailureMessage(message);
				} else returnFailureMessage(message);
			}

			private boolean processAnswerSUC(SBProtocolMessage answer, SBProtocolMessage message) {
				try{
					switch (message.getCommand()) {
						case EVENT:
							return processSuccessAnswerEVENT(answer, message);
						case ACTIO:
							return processSuccessAnswerACTIO(answer, message);
						default:
							return false;
					}
				}catch(IndexOutOfBoundsException e){
					e.printStackTrace();
					returnFailureMessage(message, SBProtocolMessage.FAILD_PARAMANIA_HAS_TAKEN_OVER);
					return false;
				}
			}

			private boolean processSuccessAnswerACTIO(SBProtocolMessage answer, SBProtocolMessage message) {
				// TODO Auto-generated method stub
				return false;
			}
			private boolean processSuccessAnswerEVENT(SBProtocolMessage answer, SBProtocolMessage message) {
				if(answer.getParameters().size() > 1) {
					if (answer.getParameterContent(1).equals(SBProtocolMessage.WORKD_DIE_CHOSEN)) {
						try {
							getParent().log(Level.INFO, "Client chose die with index " + answer.getParameterContent(2) + ".");
							boolean removeMessage = ((RuleBlock)currentActorWaitingForAnswer.getRule(1)).blockReaction(message, answer, currentDefenderWaitingForAnswer);
							sendGame();
							return removeMessage;
						} catch (IndexOutOfBoundsException e) {
							getParent().log(Level.WARNING, "Client returned DIE CHOSEN but without a chosen die. What a weirdo.");
							return false;
						}

					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.WORKD_GIVE_BALL) && message.getParameterContent(0).equals(SBProtocolMessage.EVENT_GIVE_THE_BALL_TO_SOMEONE)){
						boolean removeMessage = giveBall(answer);
						sendGame();
						return removeMessage;
					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.WORKD_KICK) && message.getParameterContent(0).equals(SBProtocolMessage.EVENT_INITIATE_KICK)){
						boolean removeMessage = kick(answer);
						sendGame();
						return removeMessage;
					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.WORKD_DIRECTION) &&  message.getParameterContent(0).equals(SBProtocolMessage.EVENT_WHICH_DIRECTION)){
						boolean removeMessage = ((RulePush)currentActorWaitingForAnswer.getRule(2)).chosenDirection(currentDefenderWaitingForAnswer, message, answer, currentPusherWaitingForAnswer, currentBackUpPosWaitingForAnswer);
						sendGame();
						return removeMessage;
					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.WORKD_DECIDED) && message.getParameterContent(0).equals(SBProtocolMessage.EVENT_FOLLOW)){
						boolean removeMessage = ((RulePush)currentActorWaitingForAnswer.getRule(2)).follow(currentDefenderFieldWaitingForAnswer, message, answer);
						sendGame();
						return removeMessage;
					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.EVENT_API_AIM) && message.getParameterContent(0).equals(SBProtocolMessage.EVENT_API_AIM)){
						try{
							int xPos = Integer.parseInt(answer.getParameterContent(2));
							int yPos = Integer.parseInt(answer.getParameterContent(3));
							currentActorWaitingForAnswer.invokeFunctionByName(message.getParameterContent(1), xPos, yPos, findUserIndex(answer));
							sendGame();
							return true;
						}catch(NumberFormatException e){
							returnFailureMessage(answer, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
						}
					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.EVENT_API_CHOICE) && message.getParameterContent(0).equals(SBProtocolMessage.EVENT_API_CHOICE)){
						try{
							int messageIndex = Integer.parseInt(answer.getParameterContent(2));
							int teamIndex = Integer.parseInt(message.getParameterContent(messageIndex*2+2));
							int playerIndex = Integer.parseInt(message.getParameterContent(messageIndex*2+3));
							if(message.getParameterContent(1).equals("$intercept")){
								Player defender = getTeam(teamIndex).getPlayers().get(playerIndex);
								((RuleThrow)defender.getRule(3)).intercept(currentActorWaitingForAnswer, teamIndex, playerIndex, findUserIndex(answer));
							}else{
								currentActorWaitingForAnswer.invokeFunctionByName(message.getParameterContent(1), teamIndex, playerIndex, findUserIndex(answer));
							}
							sendGame();
							return true;
						}catch(NumberFormatException e){
							returnFailureMessage(answer, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
						}
					} else if(answer.getParameterContent(1).equals(SBProtocolMessage.EVENT_API_FIELD) && message.getParameterContent(0).equals(SBProtocolMessage.EVENT_API_FIELD)){
						try{
							if(message.getParameterContent(1).equals("$push")){
								boolean removeMessage = ((RulePush)currentActorWaitingForAnswer.getRule(2)).chosenDirection(currentDefenderWaitingForAnswer, message, answer, currentPusherWaitingForAnswer, currentBackUpPosWaitingForAnswer);
								sendGame();
								return removeMessage;
							}else{
								int xPos = Integer.parseInt(answer.getParameterContent(2));
								int yPos = Integer.parseInt(answer.getParameterContent(3));
								currentActorWaitingForAnswer.invokeFunctionByName(message.getParameterContent(1), xPos, yPos, findUserIndex(answer));
								sendGame();
								return true;
							}
						}catch(NumberFormatException e){
							returnFailureMessage(answer, SBProtocolMessage.FAILD_RECEIVED_WRONG_GAME_DATA);
						}
					}
				}
				return false;
			}

			private boolean processAnswerFAI(SBProtocolMessage answer, SBProtocolMessage message) {
				switch (message.getCommand()) {
					case EVENT:
						return true;
					case ACTIO:
						return true;
					default:
						return false;
				}
			}
		};
		messageListener.start();
		
		while(isRunning()) {
			//TODO: GAME LOOP
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// HELPERS

	/**
	 * sends a Message from this Match to a User
	 * @param destinationUser the User to whom the message is sent
	 * @param command the Command of the Message
	 * @param parameters the Parameters of the Message
	 */
	public void sendMessage(User destinationUser, SBProtocolCommand command, String... parameters){
		sendMessage(destinationUser.getUID(), new SBProtocolMessage(new SBProtocolMessage(getParent().UID, command, parameters)));
	}
	
	public void sendMessage(UUID destinationUID, SBProtocolCommand command, String... parameters) {
		sendMessage(destinationUID, new SBProtocolMessage(new SBProtocolMessage(getParent().UID, command, parameters)));
	}
	
	public boolean checkUserTurn(int actingUserIndex){
		if(gamePhase == 0){
			return true;
		}else if(gamePhase == 1){
			if(actingPlayer == 0){
				if(actingUserIndex == 0){
					return true;
				}else if(actingUserIndex == 1){
					return false;
				}
			}else if(actingPlayer == 1){
				if(actingUserIndex == 0){
					return false;
				}else if(actingUserIndex == 1){
					return true;
				}
			}
			return false;
		}else if(gamePhase == 5){
			return false;
		}else{
			if(firstPlayer == 0) {
				if(actingUserIndex == 0)
					return (roundCount < NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 0) || (roundCount >= NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 1);
				else if(actingUserIndex == 1)
					return (roundCount < NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 1) || (roundCount >= NUMBER_OF_ROUNDS_IN_GAME/2&& roundCount%2 == 0);
			} else if(firstPlayer == 1) {
				if(actingUserIndex == 0)
					return (roundCount < NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 1) || (roundCount >= NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 0);
				else if(actingUserIndex == 1)
					return (roundCount < NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 0) || (roundCount >= NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount%2 == 1);
			}
			return false;
		}
	}
	
	public boolean checkUserTurn(SBProtocolMessage message) {
		return message.getCommand().equals(SBProtocolCommand.SRNDR) || checkUserTurn(findUserIndex(message));
	}
	
	public void endTurn(SBProtocolMessage message){
		endTurn(findUserIndex(message));
	}
	
	public void endTurn(int actingUserIndex){
		resetRemainingBe();
		resetFoul();
		teams[actingUserIndex].setBlitz(true);
		teams[actingUserIndex].setPass(true);
		countUpRound();
		unstunPlayers(getOpposingTeam(teams[actingUserIndex]));
		getOpposingTeam(teams[actingUserIndex]).clearMovingPlayer();
		sendMessage(coaches[actingUserIndex], SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_ENDED_TURN);
		sendMessage(getOpponent(actingUserIndex), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_YOUR_TURN);
		if(roundCount == NUMBER_OF_ROUNDS_IN_GAME/2){
			actingPlayer = firstPlayer;
			sendMessageShowMe("Referee", "Half time!");
			halfTime();
		}else if(roundCount == NUMBER_OF_ROUNDS_IN_GAME){
			sendMessageShowMe("Referee", "The game has ended!");
			finishGame();
		}else{
			sendMessageShowMe(coaches[actingUserIndex], "Referee", "Turnover!");
			sendMessageShowMe(coaches[actingUserIndex == 1 ? 0 : 1], "Referee", "Your turn!");
		}
		sendGame();
	}
	
	private void resetRemainingBe(){
		resetRemainingBe(0);
		resetRemainingBe(1);
	}
	
	private void resetFoul(){
		getTeam(0).setFoul(true);
		getTeam(1).setFoul(true);
	}
	private void resetRemainingBe(int teamIndex){
		for(Player p: teams[teamIndex].getPlayers()){
			p.invokeResetRemainingBe();
		}
	}
	
	private void halfTime() {
		Vector<Player> playersOnThePitch = findPlayersOnThePitch();
		clearAllPlayerPos();
		getPitch().adjustBallPos(new Vector2d(-1, -1));
		teamsSet[0] = false;
		teamsSet[1] = false;
		gamePhase = 1;
		resetPlayerConditions();
		checkForSwelteringHeat(playersOnThePitch);
		sendGame();
		checkForTeamCondition();
		cleanPlayersAfterTouchdownOrHalfTime();
		if(roundCount >= NUMBER_OF_ROUNDS_IN_GAME){
			finishGame();
		}else{
			initiateTeamSetup(coaches[actingPlayer]);
		}
	}
	
	private Vector<Player> findPlayersOnThePitch() {
		Vector<Player> players = new Vector<Player>();
		for(Player p:getTeam(0).getPlayers()){
			if(p.getPosition() != Pitch.THE_VOID){
				players.add(p);
			}
		}
		for(Player p:getTeam(1).getPlayers()){
			if(p.getPosition() != Pitch.THE_VOID){
				players.add(p);
			}
		}
		return players;
	}

	private void checkForTeamCondition() {
		int roundsBeforeCheckTeamCondition = roundCount;
		int finePlayers0 = 0;
		for(Player p: teams[0].getPlayers()){
			if(p.invokeGetPlayerCondition()==PlayerCondition.FINE){
				finePlayers0++;
			}
		}
		int finePlayers1 = 0;
		for(Player p: teams[1].getPlayers()){
			if(p.invokeGetPlayerCondition()==PlayerCondition.FINE){
				finePlayers1++;
			}
		}
		if(finePlayers0 < 3 || finePlayers1 < 3){
			if(finePlayers0 < 3 && !(finePlayers1 < 3)){
				countUpScore(1);
			}else if(finePlayers1 < 3 && !(finePlayers0 < 3)){
				countUpScore(0);
			}
			for(int i = 0; i < 4; i++){
				countUpRound();
			}
			resetPlayerConditions();
			if(roundsBeforeCheckTeamCondition < NUMBER_OF_ROUNDS_IN_GAME/2 && roundCount >= NUMBER_OF_ROUNDS_IN_GAME/2){
				roundCount = NUMBER_OF_ROUNDS_IN_GAME/2;
				actingPlayer = firstPlayer;
				halfTime();
			}else if(roundCount >= NUMBER_OF_ROUNDS_IN_GAME){
				finishGame();
			}else{
				checkForTeamCondition();
			}
		}
	}

	private void resetPlayerConditions(){
		resetPlayerConditions(0);
		resetPlayerConditions(1);
	}
	
	private void resetPlayerConditions(int teamIndex){
		for(Player p: teams[teamIndex].getPlayers()){
			if(p.invokeGetPlayerCondition() == PlayerCondition.STUNNED || p.invokeGetPlayerCondition() == PlayerCondition.PRONE){
				p.invokeSetPlayerCondition(PlayerCondition.FINE);
			}else if(p.invokeGetPlayerCondition() == PlayerCondition.KO){
				if(d6.throwDie() > 3){
					p.invokeSetPlayerCondition(PlayerCondition.FINE);
				}
			}
		}
	}

	private void unstunPlayers(Team t) {
		for(Player p: t.getPlayers()){
			if(p.invokeGetPlayerCondition() == PlayerCondition.STUNNED){
				p.invokeSetPlayerCondition(PlayerCondition.PRONE);
				p.invokeSetRemainingBe(0);
			}
		}
	}

	private void allPlayersSet(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		if(getPitch().teamSetCorrect(actingUserIndex)){
			teamsSet[actingUserIndex] = true;
			if(actingPlayer == 0){
				actingPlayer = 1;
			}else if(actingPlayer == 1){
				actingPlayer = 0;
			}
			if(teamsSet[0] && teamsSet[1]){
				gamePhase = 2;
				initiateKick(coaches[actingPlayer]);
			}else{
				initiateTeamSetup(coaches[actingPlayer]);
			}
		}else{
			sendMessageShowMe(getUser(actingUserIndex), "Referee", "Sry, but this Teamsetup is not Valid.");
			returnFailureMessage(message, SBProtocolMessage.FAILD_INVALID_TEAMSETUP);
		}
	}
	
	private void block(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		int playerIndex, defenderIndex;
		try{
			playerIndex = Integer.parseInt(message.getParameterContent(1));
			defenderIndex = Integer.parseInt(message.getParameterContent(2));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_GIMME_AN_INT);
			return;
		}
		((RuleBlock)getTeam(actingUserIndex).getPlayers().get(playerIndex).getRule(1)).apply(message, getOpposingTeam(getTeam(actingUserIndex)).getPlayers().get(defenderIndex));
	}

	private void pass(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		int x, y, playerIndex;
		try{
			playerIndex = Integer.parseInt(message.getParameterContent(1));
			x = Integer.parseInt(message.getParameterContent(2));
			y = Integer.parseInt(message.getParameterContent(3));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_GIMME_AN_INT);
			return;
		}
		((RuleThrow)getTeam(actingUserIndex).getPlayers().get(playerIndex).getRule(3)).apply(message, getPitch().getFields()[x][y]);
	}
	
	private void move(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		int playerIndex;
		int[] x, y;
		int messageLength = message.getParameters().size();
		x = new int[(messageLength-2)/2];
		y = new int[(messageLength-2)/2];
		try{
			playerIndex = Integer.parseInt(message.getParameterContent(1));
			for(int i = 0; i < (messageLength-2)/2; i++){
				x[i] = Integer.parseInt(message.getParameterContent(2*i+2));
				y[i] = Integer.parseInt(message.getParameterContent(2*i+3));
			}
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_GIMME_AN_INT);
			return;
		} catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return;
		}
		PitchField[] path = new PitchField[x.length];
		for(int i = 0; i < path.length; i++){
			path[i] = getPitch().getFields()[x[i]][y[i]];
		}
		if(getTeam(actingUserIndex).getPlayers().size() <= playerIndex)
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_DOESNT_EXIST);
		else
			((RuleMove)getTeam(actingUserIndex).getPlayers().get(playerIndex).getRule(0)).apply(message, path);
	}
	
	private void invokeSpecialRule(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		int playerIndex;
		try{
			playerIndex = Integer.parseInt(message.getParameterContent(1));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_GIMME_AN_INT);
			return;
		}
		Player p = teams[actingUserIndex].getPlayers().get(playerIndex);
		if(findSpecialRule(p, message.getParameterContent(2)) == -1){
			returnFailureMessage(message, SBProtocolMessage.FAILD_NO_SUCH_SPECIAL_RULE);
		}else{
			p.getSpecialRules()[findSpecialRule(p, message.getParameterContent(2))].apply(message);
		}
	}
	
	private int findSpecialRule(Player player, String name){
		for(int i = 0; i < player.getSpecialRules().length; i++){
			if(name.equals(player.getSpecialRules()[i].getName())){
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Chooses randomly which Player starts the game
	 */
	private void setFirstPlayer(){
		int firstPlayerThrow = d6.throwDie();
		if(firstPlayerThrow < 4){
			firstPlayer = 0;
			actingPlayer = 1;
		}else{
			firstPlayer = 1;
			actingPlayer = 0;
		}
		weatherRoll();
	}
	
	/**
	 * makes a kick
	 * @param message the Message which initiated this Kick
	 */
	private boolean kick(SBProtocolMessage message){
		int x, y;
		try{
			x = Integer.parseInt(message.getParameterContent(2));
			y = Integer.parseInt(message.getParameterContent(3));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_THIS_IS_NO_VALID_COORDINATE);
			return false;
		}
		if(getPitch().isOnField(x, y)){
			if(!(actingPlayer == 0 || actingPlayer == 1)) return false;
			boolean isOnCorrectHalf;
			//überprüft ob der Ball die richtige Spielhälfte gekickt wird
			if(actingPlayer == 0){
				isOnCorrectHalf = getPitch().isOnRightHalf(x, y);
			}else{
				isOnCorrectHalf = getPitch().isOnLeftHalf(x, y);
			}
			if(isOnCorrectHalf){
				Vector2d direction = scatter();
				int distance = d6.throwDie();
				boolean isOnCorrectHalfAfterScatter;
				//überprüft ob der Ball auch nach der Abweichung auf der richtigen Spielhälfte ist
				if(actingPlayer == 0){
					isOnCorrectHalfAfterScatter = getPitch().isOnRightHalf(x + (int)(distance*direction.x), y + (int)(distance*direction.y));
				}else{
					isOnCorrectHalfAfterScatter = getPitch().isOnLeftHalf(x + (int)(distance*direction.x), y + (int)(distance*direction.y));
				}
				//kickt den ball dahin, oder initiert das übergeben des Balls an einen Spieler
				if(isOnCorrectHalfAfterScatter){
					getPitch().setBallPos(new Vector2d(x + distance*direction.x, y + distance*direction.y));
					boolean isOnCorrectHalfAfterSecondScatter;
					if(actingPlayer == 0){
						isOnCorrectHalfAfterSecondScatter = getPitch().isOnRightHalf((int)getPitch().getBallPos().x, (int)getPitch().getBallPos().y);
					}else{
						isOnCorrectHalfAfterSecondScatter = getPitch().isOnLeftHalf((int)getPitch().getBallPos().x, (int)getPitch().getBallPos().y);
					}
					if(isOnCorrectHalfAfterSecondScatter){
						sendMessageShowMe(getOpponent(actingPlayer), "Referee", "Your turn!");
						sendMessage(getOpponent(actingPlayer), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_YOUR_TURN);
						gamePhase = 3;
					}else{
						if(getPitch().isOnField(getPitch().getBallPos())){
							if(getPitch().getFields()[(int)getPitch().getBallPos().x][(int)getPitch().getBallPos().y].getPlayer() != null){
								getPitch().getFields()[(int)getPitch().getBallPos().x][(int)getPitch().getBallPos().y].getPlayer().invokeSetIsHoldingBall(false);
							}
						}
						sendMessage(getOpponent(actingPlayer), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_GIVE_THE_BALL_TO_SOMEONE);
					}
				}else{
					sendMessage(getOpponent(actingPlayer), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_GIVE_THE_BALL_TO_SOMEONE);
				}
			}else{
				returnFailureMessage(message, SBProtocolMessage.FAILD_WRONG_SIDE);
				return false;
			}
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_NOT_ON_THE_PITCH);
			return false;
		}
		return true;
	}
	
	/**
	 * Gives the Permission to make a Kickoff
	 * @param u the user that makes the Kickoff
	 */
	private void initiateKick(User u){
		sendMessageShowMe(u, "Referee", "Kick the Ball!");
		sendMessage(u, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_INITIATE_KICK);	
	}
	
	private void weatherRoll(){
		int weatherThrow = d6.throwDie() + d6.throwDie();
		if(weatherThrow == 2 || weatherThrow == 3){
			setWeather(Weather.SWELTERING_HEAT);
		}else if(weatherThrow == 4 || weatherThrow == 5){
			setWeather(Weather.VERY_SUNNY);
		}else if(weatherThrow < 9){
			setWeather(Weather.NICE);
		}else if(weatherThrow == 9 || weatherThrow == 10){
			setWeather(Weather.POURING_RAIN);
		}else if(weatherThrow == 11 || weatherThrow == 12){
			setWeather(Weather.BLIZZARD);
			checkForBlizzard();
		}
		sendWeather();
		sendMessageShowMe("Petrus", "Weather: " + getWeather().toNiceString());
	}
	
	/**
	 * Gives the Permission to set up the team
	 * @param u the user that gets the permission
	 */
	private void initiateTeamSetup(User u){
		sendMessageShowMe(u, "Referee", "Setup your Team!");
		sendMessage(u, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_SETUP_YOUR_TEAM);
	}
	
	/**
	 * sets a Player on a given Position
	 * @param message the message that tells which player to place where
	 */
	private void setPlayer(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		if(!(actingUserIndex == 0 || actingUserIndex == 1)){return;}
		int x, y, playerIndex;
		try{
			playerIndex = Integer.parseInt(message.getParameterContent(1));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_DOESNT_EXIST);
			return;
		}
		// check if player exists in team
		if(playerIndex >= getTeam(actingUserIndex).getPlayers().size()) {
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_DOESNT_EXIST);
			return;
		}
		if(getTeam(actingUserIndex).getPlayers().get(playerIndex).invokeGetPlayerCondition().equals(PlayerCondition.DEAD)
				|| getTeam(actingUserIndex).getPlayers().get(playerIndex).invokeGetPlayerCondition().equals(PlayerCondition.INJURED)
				|| getTeam(actingUserIndex).getPlayers().get(playerIndex).invokeGetPlayerCondition().equals(PlayerCondition.KO)){
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_IS_NOT_IN_A_GOOD_CONDITION);
			return;
		}else if(getTeam(actingUserIndex).getPlayers().get(playerIndex).getRedCard()){
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_IS_BANNED_FROM_THE_GAME);
			return;
		}
		try {
			x = Integer.parseInt(message.getParameterContent(2));
			y = Integer.parseInt(message.getParameterContent(3));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_THIS_IS_NO_VALID_COORDINATE);
			return;
		}
		Vector2d pos = new Vector2d(x, y);
		if(getPitch().isOnField(pos)){
			if (getPitch().getFields()[(int) pos.x][(int) pos.y].getPlayer() == null) {
				if (actingUserIndex == 0) {
					if (getPitch().isOnRightHalf(pos)) {
						returnFailureMessage(message, SBProtocolMessage.FAILD_WRONG_SIDE);
						return;
					}
				} else {
					if (getPitch().isOnLeftHalf(pos)) {
						returnFailureMessage(message, SBProtocolMessage.FAILD_WRONG_SIDE);
						return;
					}
				}
				if(getTeam(actingUserIndex).getPlayers().size() > playerIndex)
					getTeam(actingUserIndex).getPlayers().get(playerIndex).invokeSetPosition(getPitch().getFields()[(int) pos.x][(int) pos.y]);
				else {
					returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_DOESNT_EXIST);
					return;
				}
				returnSuccessMessage(message, SBProtocolMessage.WORKD_PLAYER_SET);
			} else returnFailureMessage(message, SBProtocolMessage.FAILD_FIELD_ALREADY_TAKEN);
			
		} else if(x == -1 && y == -1){
			getTeam(actingUserIndex).getPlayers().get(playerIndex).invokeClearPosition();
		} else { returnFailureMessage(message, SBProtocolMessage.FAILD_NOT_ON_THE_PITCH); }
	}
	
	/**
	 * sets all the Players in a Team
	 * @param message the message, where all the Infos are set
	 */
	private void setTeam(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		if(!(actingUserIndex == 0 || actingUserIndex == 1)){return;}
		if(!(teamsChosen[actingUserIndex])){
			//find the actual team type
			if(message.getParameters().size() >= 3+Team.MIN_TEAM_SIZE){
				Team newTeam = setTeamType(message, 2);
				if(newTeam!=null){
					newTeam = new Team(newTeam, message.getParameterContent(1), this, getUser(actingUserIndex));
					int existingPlayers = 0;
					// TODO: handle team setup messages without any players (["name", "type"])
					for(int i = 3; i < message.getParameters().size(); i++){
						for(int j = 0; j < newTeam.getAvailablePlayers().size(); j++){
							if(message.getParameterContent(i).equalsIgnoreCase(newTeam.getAvailablePlayers().get(j).getName())){
								newTeam.addPlayer(newTeam.getAvailablePlayers().get(j));
								existingPlayers++;
								break;
							}
						}
					}
					setTeam(actingUserIndex, newTeam);
					if(existingPlayers == message.getParameters().size()-3){
						int teamPrice=0;
						for (Player p:teams[actingUserIndex].getPlayers()){
							teamPrice +=p.getPrice();
						}
						if(teamPrice <= GameController.MAX_MONEY_TO_SPEND){
							teamsChosen[actingUserIndex] = true;
							getPitch().setTeam(actingUserIndex, newTeam);
							returnSuccessMessage(message, SBProtocolMessage.WORKD_TEAMSETUP_SUCCESSFUL);
							if(teamsChosen[0] && teamsChosen[1]){
								gamePhase = 1;
								initiateTeamSetup(coaches[actingPlayer]);
							}
						}
						else{
							returnFailureMessage(message, SBProtocolMessage.FAILD_NOT_ENOUGH_MONEY);
						}
					}else{
						newTeam.clearPlayers();
						returnFailureMessage(message, SBProtocolMessage.FAILD_SOME_PLAYERS_DONT_EXIST);
					}
				}else{
					returnFailureMessage(message, SBProtocolMessage.FAILD_NO_SUCH_TEAM_TYPE);
				}
			}else{
				returnFailureMessage(message, SBProtocolMessage.FAILD_NOT_ENOUGH_PLAYERS);
			}
		}else{
			returnFailureMessage(message, SBProtocolMessage.FAILD_YOUR_TEAM_IS_ALREADY_SET);
		}
		sendNewTeam(actingUserIndex);
	}
	
	/**
	 * gives the ball to a specific player
	 * @param message the message which contains the player to give the ball to
	 */
	private boolean giveBall(SBProtocolMessage message){
		int actingUserIndex = findUserIndex(message);
		int playerIndex;
		sendMessageShowMe(coaches[actingUserIndex], "Referee", "Give the Ball to someone!");
		try{
			playerIndex = Integer.parseInt(message.getParameterContent(2));
		}catch(NumberFormatException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_GIVE_A_VALID_PLAYER_INDEX);
			return false;
		}
		try{
			getPitch().adjustBallPos(new Vector2d(-1, -1));
			sendBallPos();
			return ((RuleCatch)getTeam(actingUserIndex).getPlayers().get(playerIndex).getRule(4)).giveBall(message);
		}catch(IndexOutOfBoundsException e){
			returnFailureMessage(message, SBProtocolMessage.FAILD_PLAYER_DOESNT_EXIST);
			return false;
		}
	}

	/**
	 * Write this server match to file.
	 */
	public void logServerMatch() {
		logGame(SERVER_GAMES_FILE);
	}
	
	/**
	 * initiates everything that is necessary at the end of a game
	 * @param surrenderingUser The user that surrendered.
	 */
	public void finishGame(User surrenderingUser) {
		gamePhase = 4;
		setWinner(getOpponent(surrenderingUser));
		sendMessage(getOpponent(surrenderingUser), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_OPPONENT_SURRENDERED);
		setRunning(false);
		updateHighscore();
		logServerMatch();
		((Server) getParent()).finishMatch(this);
	}

	/**
	 * initiates everything that is necessary at the end of a game
	 */
	public void finishGame() {
		gamePhase = 4;
		determineWinner();
		setRunning(false);
		updateHighscore();
		if(winner == null){
			sendMessage(getUser(0), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_DRAW);
			sendMessage(getUser(1), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_DRAW);
		}else{
			sendMessage(winner, SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_WON_GAME);
	        sendMessage(getOpponent(winner), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_LOST_GAME);
		}
		logServerMatch();
		((Server) getParent()).finishMatch(this);
	}

	/**
	 * Sets winner to who would have won the game if it ended now.
	 */
	private void determineWinner() {
		// TODO: coach 1 wins on tie?!
		if(score[0] > score[1]){
			setWinner(coaches[0]);
		}else if(score[1] > score[0]){
			setWinner(coaches[1]);
		}else{
			setWinner(null);
		}
	}
	
	/**
	 * sets the new Player Highscore
	 */
	private void updateHighscore(){
		if(winner != null) {
			winner.wonGame(this);
			getOpponent(winner).lostGame(this);
		}
	}

	@Override
	public void touchdown(Team t) {
		sendMessageShowMe("Referee", "TOUCHDOWN!");
		countUpScore(t);
		Vector<Player> playersOnThePitch = findPlayersOnThePitch(); 
		clearAllPlayerPos();
		getPitch().adjustBallPos(new Vector2d(-1, -1));
		teamsSet[0] = false;
		teamsSet[1] = false;
		int actingUserIndex = -1;
		if(t.equals(teams[0])){
			actingUserIndex = 0;
		}else if(t.equals(teams[1])){
			actingUserIndex = 1;
		}
		countUpRound();
		if(checkUserTurn(actingUserIndex)){
			countUpRound();
		}
		resetPlayerConditions();
		checkForSwelteringHeat(playersOnThePitch);
		int roundsBeforeCheckTeamCondition = roundCount;
		checkForTeamCondition();
		cleanPlayersAfterTouchdownOrHalfTime();
		sendGame();
		if(roundCount >= NUMBER_OF_ROUNDS_IN_GAME){
			finishGame();
		}else if(roundCount >= NUMBER_OF_ROUNDS_IN_GAME/2 && roundsBeforeCheckTeamCondition < NUMBER_OF_ROUNDS_IN_GAME/2){
			roundCount = NUMBER_OF_ROUNDS_IN_GAME/2;
			actingPlayer = firstPlayer;
			halfTime();
		}else{
			gamePhase = 1;
			actingPlayer = actingUserIndex;
			initiateTeamSetup(coaches[actingUserIndex]);
		}
	}
	
	private void cleanPlayersAfterTouchdownOrHalfTime(){
		getTeam(0).clearMovingPlayer();
		getTeam(1).clearMovingPlayer();
		resetFoul();
		getTeam(0).setBlitz(true);
		getTeam(1).setBlitz(true);
		getTeam(0).setPass(true);
		getTeam(1).setPass(true);
	}

	// GETTERS & SETTERS

	public void setCurrentActorWaitingForAnswer(Player currentActorWaitingForAnswer) {
		this.currentActorWaitingForAnswer = currentActorWaitingForAnswer;
	}

	public void setCurrentDefenderWaitingForAnswer(Player currentDefenderWaitingForAnswer) {
		this.currentDefenderWaitingForAnswer = currentDefenderWaitingForAnswer;
	}
	
	public int findTeamHoldingTheBall(){
		for(int i = 0; i < teams[0].getPlayers().size(); i++){
			if(teams[0].getPlayers().get(i).isHoldingBall()){
				return 0;
			}
		}for(int i = 0; i < teams[1].getPlayers().size(); i++){
			if(teams[1].getPlayers().get(i).isHoldingBall()){
				return 1;
			}
		}
		return -1;
	}

	public void setCurrentDefenderFieldWaitingForAnswer(Vector2d defenderField) {
		this.currentDefenderFieldWaitingForAnswer = defenderField;
	}

	public void setCurrentPusherWaitingForAnswer(Player pusherWaitingForAnswer) {
		this.currentPusherWaitingForAnswer = pusherWaitingForAnswer;		
	}

	public void setCurrentBackUpPosWaitingForAnswer(Vector2d posToBackup) {
		this.currentBackUpPosWaitingForAnswer = posToBackup;
	}
	
	public void clearCurrentPlayersBeingPushed(){
		this.currentPlayersBeingPushed.removeAllElements();
	}
	
	public void addCurrentPlayersBeingPushed(Player p){
		this.currentPlayersBeingPushed.add(p);
	}
	
	public void setCurrentModificatorWaitingForAnser(int mod){
		this.currentModificaorWaitingForAnswer = mod;
	}
	
	public Vector<Player> getCurrentPlayersBeingPushed(){
		return this.currentPlayersBeingPushed;
	}
	
	public void clearCurrentPlayerPositionsBeingPushed(){
		this.currentPlayerPositionsBeingPushed.removeAllElements();
	}
	
	public void addCurrentPlayerPositionsBeingPushed(Vector2d pos){
		this.currentPlayerPositionsBeingPushed.add(pos);
	}
	
	public void setCurrentMessageWaitingForAnswer(SBProtocolMessage message){
		this.currentMessageWaitingForAnswer = message;
	}
	
	public void addCurrentHighlitedFields(Vector2d pos){
		this.currentHighlitedFields.add(pos);
	}
	
	public void clearCurrentHighlitedFields(){
		this.currentHighlitedFields.removeAllElements();
	}
	
	public Vector<Vector2d> getCurrentPlayerPositionsBeingPushed(){
		return this.currentPlayerPositionsBeingPushed;
	}
	
	public Player getCurrentDefenderWaitingForAnswer(){
		return this.currentDefenderWaitingForAnswer;
	}
	
	public int getCurrentModificatorWaitingForAnser(){
		return this.currentModificaorWaitingForAnswer;
	}
	
	public SBProtocolMessage getCurrentMessageWaitingForAnswer(){
		return this.currentMessageWaitingForAnswer;
	}
	
	public Vector2d getCurrentDefenderFieldWaitingForAnser(){
		return this.currentDefenderFieldWaitingForAnswer;
	}
	
	public Vector<Vector2d> getCurrentHighlitedFields(){
		return this.currentHighlitedFields;
	}
}
