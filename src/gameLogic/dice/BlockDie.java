package gameLogic.dice;

import util.ResourceManager;

import java.awt.image.BufferedImage;

/**
 * The die thrown while blocking
 */
public class BlockDie implements Die {
	private int throwCounter = 0;
	/**
	 * @return	Returns a int from 1 to 5 where the numbers mean 1: "Attacker Down", 2: "Both Down", 3: "Pushed", 4: "Defender Stumbles", 5: "Defender Down"
	 */
	public int throwDie() {
		/*int[] throwsForPresentation = {5, 3, 3, 1, 3, 5, 1, 3, 5, 4, 3, 2, 3, 4, 1, 1, 3, 3, 3, 5, 3, 4, 5, 2, 4, 4};
		if(throwCounter < throwsForPresentation.length){
			int i = throwsForPresentation[throwCounter];
			throwCounter++;
			System.out.println("DB: " + i);
			return i;
		}*/
		int i = rand.nextInt(6) + 1;
		if(i == 6){
			i = 3;
		}
//		System.out.println("DB: " + i);
		return i;
	}

	public static String toName(int side) {
		switch (side) {
			case 1:
				return "Attacker Down";
			case 2:
				return "Both Down";
			case 3:
				return "Pushed";
			case 4:
				return "Defender Stumbles";
			case 5:
				return "Defender Down";
			default:
				return "Strange, I don't know this die.";
		}
	}

    public static BufferedImage getImageFromSide(int side) {
        switch (side) {
            case 1:
                return ResourceManager.DIE_ATTACKER_DOWN;
            case 2:
                return ResourceManager.DIE_BOTH_DOWN;
            case 3:
                return ResourceManager.DIE_PUSHED;
            case 4:
                return ResourceManager.DIE_DEFENDER_STUMBLES;
            case 5:
                return ResourceManager.DIE_DEFENDER_DOWN;
            default:
                return null;
        }
    }
}
