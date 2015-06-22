package gameLogic;

import javax.vecmath.Vector2d;

import gameLogic.rules.RuleCatch;
import network.SBProtocolCommand;
import network.SBProtocolMessage;

/**
 * The pitch in which a match takes place.
 */
public class Pitch {
	public static final int PITCH_LENGTH = 26;
	public static final int PITCH_WIDTH = 15;
	public static final PitchField THE_VOID = new PitchField(-1, -1);
	
	private PitchField[][] fields = new PitchField[PITCH_LENGTH][PITCH_WIDTH];
	private Vector2d ball = new Vector2d(-1, -1);// Position of the Ball in Coordinates x,y
	private Team[] teams = new Team[2];
	
	/**
	 * Initiates all the Pitch Fields and sets the ball outside the Pitch
	 */
	Pitch(){
		for(int i = 0; i < PITCH_LENGTH; i++){
			for(int j = 0; j < PITCH_WIDTH; j++){
				fields[i][j] = new PitchField(new Vector2d(i, j));
			}
		}
	}
	
	public Vector2d getBallPos(){return ball;}
	public Team getTeam(int i){return teams[i];}
	public void setBallPos(int x, int y){setBallPos(new Vector2d(x, y));}
	public void setBallPos(Vector2d pos){
		setBallPos(pos, false);
	}
	/**
	 * sets the ball position
	 * @param pos to this position
	 * @param b true, if it was a successful throw, that set the ball there.
	 */
	public void setBallPos(Vector2d pos, boolean b) {
		this.ball = pos;
		getTeam(0).getMatch().sendBallPos();
		if(!(isOnField(pos))){
			throwIn(pos);
		}
		if(fields[(int)ball.x][(int)ball.y].getPlayer() != null){
			if(!(fields[(int)ball.x][(int)ball.y].getPlayer().isHoldingBall())){
				((RuleCatch)(fields[(int)ball.x][(int)ball.y].getPlayer().getRule(4))).apply(b);
			}
		}
	}
	public void adjustBallPos(Vector2d pos){this.ball = pos;}
	
	private Vector2d getLastBallPos(Vector2d pos){
		Vector2d lastPos = new Vector2d(pos);
		if(pos.x < 0){
			if(pos.y < 0){
				lastPos = new Vector2d(0, 0);
			}else if(pos.y > PITCH_WIDTH-1){
				lastPos = new Vector2d(0, PITCH_WIDTH-1);
			}else{
				lastPos = new Vector2d(0, pos.y);
			}
		}else if(pos.x > PITCH_LENGTH-1){
			if(pos.y < 0){
				lastPos = new Vector2d(PITCH_LENGTH-1, 0);
			}else if(pos.y > PITCH_WIDTH-1){
				lastPos = new Vector2d(PITCH_LENGTH-1, PITCH_WIDTH-1);
			}else{
				lastPos = new Vector2d(PITCH_LENGTH-1, pos.y);
			}
		}else if(pos.y < 0){
			lastPos = new Vector2d(pos.x, 0);
		}else if(pos.y > PITCH_WIDTH-1){
			lastPos = new Vector2d(pos.x, PITCH_WIDTH-1);
		}
		return lastPos;
	}
	
	/**
	 * makes a throw in by the Crowd
	 * @param pos the position where the ball would have landed
	 */
	public void throwIn(Vector2d pos){
		getTeam(0).getMatch().sendMessageShowMe("Referee", "The ball was thrown in by the crowd!");
		Vector2d lastPos = getLastBallPos(pos);
		int directionThrow = getTeam(0).getMatch().d3.throwDie();
		Vector2d direction = new Vector2d(0, 0);
		if((int)lastPos.x == 0){
			direction = new Vector2d(1, directionThrow-2);
		}else if((int)lastPos.y == 0){
			direction = new Vector2d(directionThrow-2, 1);
		}else if((int)lastPos.x == PITCH_LENGTH-1){
			direction = new Vector2d(-1, directionThrow-2);
		}else if((int)lastPos.y == PITCH_WIDTH-1){
			direction = new Vector2d(directionThrow-2, -1);
		}
		int distance = getTeam(0).getMatch().d6.throwDie() + getTeam(0).getMatch().d6.throwDie();
		setBallPos((int)(lastPos.x + direction.x*distance), (int)(lastPos.y + direction.y*distance));
		getTeam(0).getMatch().sendMessage(getTeam(0).getMatch().getUser(0), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_BALL_THROWN_IN_BY_THE_CROWD);
		getTeam(0).getMatch().sendMessage(getTeam(1).getMatch().getUser(1), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_BALL_THROWN_IN_BY_THE_CROWD);
	}
	
	public boolean isOnField(int d, int e){
		return isOnField(new Vector2d(d, e));
	}
	
	/**
	 * Checks whether a given Position v is on the Pitch
	 * @param v Position as a Vector2d
	 * @return Boolean which is true if v is on the Pitch and false if not
	 */
	public boolean isOnField(Vector2d v){
		return !((int) v.x < 0 || (int) v.x > PITCH_LENGTH - 1 || (int) v.y < 0 || (int) v.y > PITCH_WIDTH - 1);
	}
	
	/**
	 * Checks whether a given Position is on the left half of the pitch
	 * @param x x position  
	 * @param y y position
	 * @return true if its on the left half, false if not
	 */
	public boolean isOnLeftHalf(int x, int y){
		return isOnLeftHalf(new Vector2d(x, y));
	}
	
	/**
	 * Checks whether a given Position is on the left half of the pitch
	 * @param v the position to be checked
	 * @return true if its on the left half, false if not
	 */
	public boolean isOnLeftHalf(Vector2d v){
		return !((int) v.x < 0 || (int) v.x > (PITCH_LENGTH / 2) - 1 || (int) v.y < 0 || (int) v.y > PITCH_WIDTH - 1);
	}
	
	/**
	 * Checks whether a given Position is on the right half of the pitch
	 * @param x x position  
	 * @param y y position
	 * @return true if its on the right half, false if not
	 */
	public boolean isOnRightHalf(int x, int y){
		return isOnRightHalf(new Vector2d(x, y));
	}
	
	/**
	 * Checks whether a given Position is on the right half of the pitch
	 * @param v the position to be checked
	 * @return true if its on the right half, false if not
	 */
	public boolean isOnRightHalf(Vector2d v){
		return !((int) v.x < (PITCH_LENGTH / 2) || (int) v.x > PITCH_LENGTH - 1 || (int) v.y < 0 || (int) v.y > PITCH_WIDTH - 1);
	}
	
	private boolean isOnUpperLeftWideZone(Vector2d v){
		return !((int) v.x < 0 || (int) v.x > (PITCH_LENGTH / 2) - 1 || (int) v.y < 0 || (int) v.y > 3);
	}
	
	private boolean isOnUpperRightWideZone(Vector2d v){
		return !((int) v.x < (PITCH_LENGTH / 2) || (int) v.x > PITCH_LENGTH - 1 || (int) v.y < 0 || (int) v.y > 3);
	}
	
	private boolean isOnLowerLeftWideZone(Vector2d v){
		return !((int) v.x < 0 || (int) v.x > (PITCH_LENGTH / 2) - 1 || (int) v.y < 11 || (int) v.y > PITCH_WIDTH - 1);
	}
	
	private boolean isOnLowerRightWideZone(Vector2d v){
		return !((int) v.x < (PITCH_LENGTH / 2) || (int) v.x > PITCH_LENGTH - 1 || (int) v.y < 11 || (int) v.y > PITCH_WIDTH - 1);
	}
	
	private boolean isOnLeftLineOfScrimmage(Vector2d v){
		return (int) v.x == 12 && (int) v.y > 3 && (int) v.y < 11;
	}
	
	private boolean isOnRightLineOfScrimmage(Vector2d v){
		return (int) v.x == 13 && (int) v.y > 3 && (int) v.y < 11;
	}
	
	public boolean teamSetCorrect(int userIndex){
		int playersInUpperWideZone = 0;
		int playersInLowerWideZone = 0;
		int playersInLineOfScrimmage = 0;
		int playersOnField = 0;
		if(userIndex == 0){
			for(Player p: teams[userIndex].getPlayers()){
				if(isOnUpperLeftWideZone(p.getPos())){
					playersInUpperWideZone++;
				}else if(isOnLowerLeftWideZone(p.getPos())){
					playersInLowerWideZone++;
				}else if(isOnLeftLineOfScrimmage(p.getPos())){
					playersInLineOfScrimmage++;
				}
				if(isOnField(p.getPos())){
					playersOnField++;
				}
			}
		}else if(userIndex == 1){
			for(Player p: teams[userIndex].getPlayers()){
				if(isOnUpperRightWideZone(p.getPos())){
					playersInUpperWideZone++;
				}else if(isOnLowerRightWideZone(p.getPos())){
					playersInLowerWideZone++;
				}else if(isOnRightLineOfScrimmage(p.getPos())){
					playersInLineOfScrimmage++;
				}
				if(isOnField(p.getPos())){
					playersOnField++;
				}
			}
		}
		return playersInLineOfScrimmage > 2 && playersInUpperWideZone < 3 && playersInLowerWideZone < 3 && playersOnField < 12;
	}
	
	/**
	 * checks whether this position is int the touchdownzone
	 * @param t the touchdownzone from the team t
	 * @param pos the position
	 * @return true if its in the touchdownzone, false if not
	 */
	public boolean isInTouchdownZone(Team t, Vector2d pos){
		int side;
		if(t.getMatch().getTeam(0).equals(t)){
			side = 25;
		}else if(t.getMatch().getTeam(1).equals(t)){
			side = 0;
		}else{
			return false;
		}
		return (int) pos.x == side;
	}
	
	public boolean isAdjacent(int x1, int y1, int x2, int y2){
		return isAdjacent(new Vector2d(x1, y1), new Vector2d(x2, y2));
	}
	
	/**
	 * Checks whether two Fields on the Pitch are adjacent
	 * @param f1 The first Field
	 * @param f2 The second Field
	 * @return Boolean which is true if the two fields are adjacent and false if not
	 */
	public boolean isAdjacent(PitchField f1, PitchField f2){
		return isAdjacent(f1.getPos(),f2.getPos());
	}
	
	/**
	 * Checks whether two field-positions are adjacent
	 * @param pos1 Position of the first Field (as a Vector 2d)
	 * @param pos2 Position of the second Field (as a Vector 2d)
	 * @return Boolean which is true if the two fields are adjacent and false if not
	 */
	public boolean isAdjacent(Vector2d pos1, Vector2d pos2){
		try{
			if(!(isOnField(pos1)) || !(isOnField(pos2))){
				throw new SBOutOfFieldException("given position is not on the pitch");
			}else{
				for(PitchField f: getNeighbours(pos2)){
					if(fields[(int)pos1.x][(int)pos1.y] == f){
						return true;
					}
				}
				return false;
			}
		}catch(SBOutOfFieldException e){
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Returns all the neighbors of a given Field
	 * @param f The field from whom you want to know the neighbors
	 * @return An array with the PitchFields that are neighbors to pos
	 */
	public PitchField[] getNeighbours(PitchField f){
		return getNeighbours(f.getPos());
	}
	/**
	 * Returns all the neighbors of a given Position
	 * @param x The x position from whom you want to know the neighbors
	 * @param y The y position from whom you want to know the neighbors 
	 * @return An array with the PitchFields that are neighbors to pos
	 */
	public PitchField[] getNeighbours(int x, int y){
		return getNeighbours(new Vector2d(x, y));
	}
	/**
	 * Returns all the neighbors of a given Position
	 * @param pos The position from whom you want to know the neighbors (as a Vector 2d)
	 * @return An array with the PitchFields that are neighbors to pos
	 */
	public PitchField[] getNeighbours(Vector2d pos){
		int x = (int)pos.x;
		int y = (int)pos.y;
		try{
			if(!isOnField(pos)){
				throw new SBOutOfFieldException("given position is not on the pitch");
			}
			PitchField[] returnFields;
			if(x == 0){
				if(y == 0){
					returnFields = new PitchField[3];
					returnFields[0] = fields[x+1][y];
					returnFields[1] = fields[x][y+1];
					returnFields[2] = fields[x+1][y+1];
				}else if(y == 14){
					returnFields = new PitchField[3];
					returnFields[0] = fields[x][y-1];
					returnFields[1] = fields[x+1][y-1];
					returnFields[2] = fields[x+1][y];
				}else{
					returnFields = new PitchField[5];
					returnFields[0] = fields[x][y-1];
					returnFields[1] = fields[x+1][y-1];
					returnFields[2] = fields[x+1][y];
					returnFields[3] = fields[x][y+1];
					returnFields[4] = fields[x+1][y+1];
					
				}
			}else if(x == 25){
				if(y == 0){
					returnFields = new PitchField[3];
					returnFields[0] = fields[x-1][y];
					returnFields[1] = fields[x-1][y+1];
					returnFields[2] = fields[x][y+1];
				}else if(y == 14){
					returnFields = new PitchField[3];
					returnFields[0] = fields[x-1][y-1];
					returnFields[1] = fields[x][y-1];
					returnFields[2] = fields[x-1][y];
				}else{
					returnFields = new PitchField[5];
					returnFields[0] = fields[x-1][y-1];
					returnFields[1] = fields[x][y-1];
					returnFields[2] = fields[x-1][y];
					returnFields[3] = fields[x-1][y+1];
					returnFields[4] = fields[x][y+1];
				}
			}else if(y == 0){
				returnFields = new PitchField[5];
				returnFields[0] = fields[x-1][y];
				returnFields[1] = fields[x+1][y];
				returnFields[2] = fields[x-1][y+1];
				returnFields[3] = fields[x][y+1];
				returnFields[4] = fields[x+1][y+1];
			}else if(y == 14){
				returnFields = new PitchField[5];
				returnFields[0] = fields[x-1][y-1];
				returnFields[1] = fields[x][y-1];
				returnFields[2] = fields[x+1][y-1];
				returnFields[3] = fields[x-1][y];
				returnFields[4] = fields[x+1][y];
			}else{
				returnFields = new PitchField[8];
				for(int i = 0; i < 3; i++){
					returnFields[i] = fields[x-1+i][y-1];
				}
				returnFields[3] = fields[x-1][y];
				returnFields[4] = fields[x+1][y];
				for(int i = 0; i < 3; i++){
					returnFields[i+5] = fields[x-1+i][y+1];
				}
			}
			return returnFields;
		}catch(SBOutOfFieldException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public PitchField[][] getFields(){
		return fields;
	}
	
	public void setTeam(int i, Team t){
		teams[i] = t;
	}
}
