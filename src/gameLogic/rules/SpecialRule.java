package gameLogic.rules;

import network.SBProtocolMessage;
import gameLogic.Player;

public abstract class SpecialRule extends Rule {

	private String name;
	
	public SpecialRule(Player actor, String name) {
		super(actor);
		this.name = name;
	}

	public abstract void apply(SBProtocolMessage message);
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
}
