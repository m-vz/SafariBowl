package gameLogic;

import javax.vecmath.*;

/**
 * One field on the pitch
 */
public class PitchField {
	private Player player = null;//at first there are no Players on the Pitch
	private Vector2d pos;
	
	public PitchField(int x, int y){
		pos = new Vector2d(x, y);
	}
	/**
	 * Constructor of a PitchField
	 * @param pos Position where the PitchField is placed.
	 */
	public PitchField(Vector2d pos){
		this.pos = pos;
	}
	
	/**
	 * checks whether two PitchFields have the same position
	 * @param f The second PitchField
	 * @return Boolean which is true if they have the same position and false if not
	 */
	public boolean isEqual(PitchField f){
		return (int) f.pos.x == (int) pos.x && (int) f.pos.y == (int) pos.y;
	}
	
	/**
	 * method only to adjust the Position if someone changes the Player on a Pitchfield 
	 * @param p the new Player
	 */
	public void adjustPlayer(Player p){player = p;}
	
	//getters and setters
	public Vector2d getPos(){return pos;}
	public Player getPlayer(){return player;}
}
