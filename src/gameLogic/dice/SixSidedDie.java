package gameLogic.dice;

/**
 * A die with six sides
 */
public class SixSidedDie implements Die {
	int throwCounter = 0;
	/**
	 * @return returns a int from 1 to 6
	 */
	public int throwDie(){
		/*int[] throwsForPresentation = {5,5,4,4,5,2,2,5,6,2,6,1,1,4,3,2,4,2,2,2,3,5,4,1,3,6,1,2,2,3,1,1,5,4,2,2,4,5,4,6,2,1,3,4,3,5,2,1,3, 3, 3, 5, 1, 2, 3, 6, 4, 5, 6, 6, 6, 1, 4, 6, 1, 1, 2, 1, 2, 3, 4, 5, 1, 1, 5, 1, 2, 4, };
		if(throwCounter < throwsForPresentation.length){
			int i = throwsForPresentation[throwCounter];
			throwCounter++;
			System.out.println("D6: " + i);
			return i;
		}*/
		int i = rand.nextInt(6) + 1;
//		System.out.println("D6: " + i);
		return i;
		
	}
}
