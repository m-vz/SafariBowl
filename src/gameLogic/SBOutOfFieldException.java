package gameLogic;


import util.SBException;

/**
 * Thrown when fields outside the pitch are accessed.
 * Created by matias on 25.3.15.
 */
public class SBOutOfFieldException extends SBException {
	public SBOutOfFieldException(String s){
		super(s);
	}
}
