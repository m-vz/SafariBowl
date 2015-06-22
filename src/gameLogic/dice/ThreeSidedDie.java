package gameLogic.dice;

/**
 * A die with three sides
 */
public class ThreeSidedDie implements Die {
	private int throwCounter = 0;
	/**
	 * @return returns a int from 1 to 3
	 */
	public int throwDie(){
		/*int[] throwsForPresentation = {1, 2, 3, 2, 3, 1, 2};
		if(throwCounter < throwsForPresentation.length){
			int i = throwsForPresentation[throwCounter];
			throwCounter++;
			System.out.println("D3: " + i);
			return i;
		}*/
		int i = rand.nextInt(3) + 1;
//		System.out.println("D3: " + i);
		return i;
	}
}
