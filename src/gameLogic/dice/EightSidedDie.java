package gameLogic.dice;

/**
 * A die with eight sides
 */
public class EightSidedDie implements Die {
	private int throwCounter = 0;
	/**
	 * @return returns a int from 1 to 8
	 */
	public int throwDie(){
		/*int[] throwsForPresentation = {3, 3, 6};
		if(throwCounter < throwsForPresentation.length){
			int i = throwsForPresentation[throwCounter];
			throwCounter++;
			System.out.println("D8: " + i);
			return i;
		}*/
		int i = rand.nextInt(8) + 1;
//		System.out.println("D8: " + i);
		return i;
	}
}
