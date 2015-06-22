package gameLogic.dice;

import java.util.Random;

/**
 * Interface for all dice
 */
public interface Die {
	Random rand = new Random();
	int throwDie();
}
