package gameLogic.dice;

/**
 * Created by matias on 24.3.15.
 */
public class EightSidedDie implements Die {
	/**
	 * @return returns a int from 1 to 8
	 */
	public int throwDie(){
		return rand.nextInt(8) + 1;
	}
}
