package gameLogic.dice;

/**
 * Created by milan on 23.3.15.
 */
public class SixSidedDie implements Die {
	/**
	 * @return returns a int from 1 to 6
	 */
	public int throwDie(){
		return rand.nextInt(6) + 1;
		
	}
}
