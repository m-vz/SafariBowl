package gameLogic.dice;

import util.ResourceManager;

import java.awt.image.BufferedImage;

/**
 * The die thrown while blocking
 */
public class BlockDie implements Die {
	/**
	 * @return	Returns a int from 1 to 5 where the numbers mean 1: "Attacker Down", 2: "Both Down", 3: "Pushed", 4: "Defender Stumbles", 5: "Defender Down"
	 */
	public int throwDie() {
		int i = rand.nextInt(6) + 1;
		if(i == 6){
			i = 3;
		}
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
