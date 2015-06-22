package gameLogic;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;

import javax.vecmath.*;

import gameLogic.rules.*;

/**
 * The abstract representation of a player.
 */
public class Player {

	/**
	 * The stats that are assigned to the player if they aren't overwritten.
	 */
	private static final int DEFAULT_GE = 3,
							 DEFAULT_RS = 7,
							 DEFAULT_ST = 3,
							 DEFAULT_BE = 5,
							 DEFAULT_PRICE = 50000,
							 DEFAULT_MIN_HEADCOUNT = 0,
							 DEFAULT_MAX_HEADCOUNT = 16;

	private int ge, rs, st, be, remainingBe;
    private Rule[] rules = new Rule[5]; //0: MOVE, 1: BLOCK, 2: PUSH, 3: THROW, 4:CATCH
    private SpecialRule[] specialRules;
    private Vector<int[]> tackleZone;
    private Vector<PitchField> activeTackleZone = new Vector<PitchField>();
    private PlayerCondition playerCondition = PlayerCondition.FINE;
    protected PitchField position; // needs to be directly accessible from Kicker
    private boolean isHoldingBall = false;
    private boolean redCard = false;

	protected String name;
	private String[] descriptionLines;
    private int id, price, minHeadcount, maxHeadcount;
    private Team team;
	/**
	 * The images of the player looking left (<code>spriteL</code>) and looking right (<code>spriteR</code>).
	 */
	private BufferedImage spriteR, spriteL;
	/**
	 * A hash map for scripts to store data in. Is not used by standard players.
	 */
	private HashMap<String, String> specialStats = new HashMap<String, String>();
	/**
	 * The team manager that manages the script for this player.
	 */
	private TeamManager manager;

	/**
	 * Constructs a new player with the given name and a team manager that manages its script.
	 * @param name The name of the player.
	 * @param manager The manager that manages the script of this player.
	 */
	public Player(String name, TeamManager manager) {
		this.manager = manager;
		setName(name);
	}

	/**
	 * Private initializer to clone a player.
	 * @param p The player blueprint to clone.
	 * @param team The team this clone is made for.
	 */
	private Player(Player p, Team team) {
		this.manager = p.manager;
		setName(p.getName());
		setSpriteR(p.getSpriteR());
		setSpriteL(p.getSpriteL());
		invokeAdjustTeam(team);
		invokeSetGe(DEFAULT_GE);
		invokeSetRs(DEFAULT_RS);
		invokeSetSt(DEFAULT_ST);
		invokeSetBe(DEFAULT_BE);
		invokeAdjustRemainingBe(invokeGetBe());
		invokeSetPrice(DEFAULT_PRICE);
		invokeAdjustMinHeadcount(DEFAULT_MIN_HEADCOUNT);
		invokeAdjustMaxHeadcount(DEFAULT_MAX_HEADCOUNT);
		invokeSetPosition(Pitch.THE_VOID);
		setStandardRules();
		invokeSetSpecialRules();
		setStandardTackleZone();
		invokeSetPlayerCondition(PlayerCondition.FINE);
        invokeSetDescriptionLines(new String[0]);
	}

	public Player getCloneForTeam(Team team) {
		return new Player(this, team);
	}

	// METHODS SETTING STANDARDS

    /**
     * setts the standard rules for all players
     */
	public void setStandardRules() {
		Rule[] rules = new Rule[]{new RuleMove(this), new RuleBlock(this), new RulePush(this), new RuleThrow(this), new RuleCatch(this)};
		invokeSetRules(rules);
	}

    /**
     * sets the standard Tacklezone, witch is exactly one field around the Player
     */
    public void setStandardTackleZone() {
    	Vector<int[]> tackleZone = new Vector<int[]>();
    	for(int x = -1; x <= 1; x++){
    		for(int y = -1; y <= 1; y++){
    			if(!(x == 0 && y == 0)){
    				tackleZone.add(new int[]{x,y});
    			}
    		}
    	}
		invokeSetTackleZone(tackleZone);
    }
    
    /**
     * Makes an update of the Players Tacklezone, depending on his Condition
     */
    public void updateActiveTackleZone() {
    	Vector<PitchField> activeTackleZone = new Vector<PitchField>();
    	if(playerCondition == PlayerCondition.FINE && getPosition() != Pitch.THE_VOID){
			for (int[] tackleZoneField : tackleZone) {
				//setzte tacklezonen, falls das betreffende feld auf dem platz liegt
				if (getTeam().getMatch().getPitch().isOnField(new Vector2d(tackleZoneField[0] + (int) getPos().x, tackleZoneField[1] + (int) getPos().y))) {
					activeTackleZone.addElement(team.getMatch().getPitch().getFields()[tackleZoneField[0] + (int) getPos().x][tackleZoneField[1] + (int) getPos().y]);
				}
			}
    	}
		invokeSetActiveTackleZone(activeTackleZone);
    }

	// INVOKING SETTERS

	public void invokeSetDescriptionLines(String[] descriptionLines) {
		this.descriptionLines = descriptionLines;

		Object descriptionLinesFromScript = manager.invokeFunction("setDescriptionLines", this, descriptionLines);
		if(descriptionLinesFromScript != null) this.descriptionLines = (String[]) descriptionLinesFromScript;
	}

	public void invokeSetGe(int ge) {
		this.ge = ge;

		Object geFromScript = manager.invokeFunction("setGe", this, ge);
		if(geFromScript != null) this.ge = (Integer) geFromScript;
	}

	public void invokeSetRs(int rs) {
		this.rs = rs;

		Object rsFromScript = manager.invokeFunction("setRs", this, rs);
		if(rsFromScript != null) this.rs = (Integer) rsFromScript;
	}

	public void invokeSetSt(int st) {
		this.st = st;

		Object stFromScript = manager.invokeFunction("setSt", this, st);
		if(stFromScript != null) this.st = (Integer) stFromScript;
	}

	public void invokeSetBe(int be) {
		this.be = be;

		Object beFromScript = manager.invokeFunction("setBe", this, be);
		if(beFromScript != null) this.be = (Integer) beFromScript;
	}

	public void invokeSetRemainingBe(int remainingBe) {
		this.remainingBe = remainingBe;

		Object remainingBeFromScript = manager.invokeFunction("setRemainingBe", this, remainingBe);
		if(remainingBeFromScript != null) this.remainingBe = (Integer) remainingBeFromScript;
	}

	public void invokeSetRules(Rule[] rules) {
		this.rules = rules;

		Object rulesFromScript = manager.invokeFunction("setRules", this, (Object[]) rules);
		if(rulesFromScript != null) this.rules = (Rule[]) rulesFromScript;
	}
	
	public void invokeSetRedCard(boolean redCard){
		this.redCard = redCard;
		
		Object redCardFromScript = manager.invokeFunction("setRedCard", this, redCard);
		if(redCardFromScript != null) this.redCard = (Boolean) redCardFromScript;
	}

	@SuppressWarnings("unchecked")
	public void invokeSetSpecialRules() {
		this.specialRules = new SpecialRule[0];

		Object specialRulesFromScript = manager.invokeFunction("setSpecialRules", this);
		if(specialRulesFromScript != null) this.specialRules = (SpecialRule[]) specialRulesFromScript;
	}

	@SuppressWarnings("unchecked")
	public void invokeSetTackleZone(Vector<int[]> tackleZone) {
		this.tackleZone = tackleZone;

		Object tackleZoneFromScript = manager.invokeFunction("setTackleZone", this, tackleZone);
		if(tackleZoneFromScript != null) this.tackleZone = (Vector<int[]>) tackleZoneFromScript;
	}

	@SuppressWarnings("unchecked")
	public void invokeSetActiveTackleZone(Vector<PitchField> activeTackleZone) {
		this.activeTackleZone = activeTackleZone;
		
		Object activeTackleZoneFromScript = manager.invokeFunction("setActiveTackleZone", this, activeTackleZone);
		if(activeTackleZoneFromScript != null) this.activeTackleZone = (Vector<PitchField>) activeTackleZoneFromScript;
	}

	public void invokeSetPlayerCondition(PlayerCondition playerCondition) {
		this.playerCondition = playerCondition;

		Object playerConditionFromScript = manager.invokeFunction("setPlayerCondition", this, playerCondition);
		if(playerConditionFromScript != null) this.playerCondition = (PlayerCondition) playerConditionFromScript;

		if(isHoldingBall()){
			if(invokeGetPlayerCondition() != PlayerCondition.FINE){
				invokeSetIsHoldingBall(false);
				Vector2d newBallPos = new Vector2d(position.getPos());
				newBallPos.add(getTeam().getMatch().scatter());
				getTeam().getMatch().getPitch().setBallPos(newBallPos);
				getTeam().getMatch().sendBallPos();
			}
		}
		if(team != null) updateActiveTackleZone(); // only update active tackle zone if this player is used in a team
	}

	public void invokeSetPosition(int x, int y) {
		invokeSetPosition(new Vector2d(x, y));
	}

	public void invokeSetPosition(Vector2d position) {
		invokeSetPosition(getTeam().getMatch().getPitch().getFields()[(int) position.x][(int) position.y]);
	}

	public void invokeSetPosition(PitchField position) {
		if(this.position != null) this.position.adjustPlayer(null); // remove the player from the old field
		this.position = position;

		Object positionFromScript = manager.invokeFunction("setPosition", this, position);
		if(positionFromScript != null) this.position = (PitchField) positionFromScript;

		position.adjustPlayer(this);
		if(isHoldingBall()) {
			getTeam().getMatch().getPitch().adjustBallPos(this.getPos());
			if(getTeam().getMatch().getPitch().isInTouchdownZone(getTeam(), getPos()))
				getTeam().getMatch().touchdown(getTeam());
			getTeam().getMatch().sendBallPos();
		}
		if(team != null) updateActiveTackleZone(); // only update active tackle zone if this player is used in a team
		if(!this.position.equals(Pitch.THE_VOID)) getTeam().getMatch().sendPlayer(this);
	}

	public void invokeSetIsHoldingBall(boolean isHoldingBall) {
		this.isHoldingBall = isHoldingBall;
		
		Object isHoldingBallFromScript = manager.invokeFunction("setIsHoldingBall", this, isHoldingBall);
		if(isHoldingBallFromScript != null) this.isHoldingBall = (Boolean) isHoldingBallFromScript;
		
		if(isHoldingBall() && getTeam().getMatch().getPitch().isInTouchdownZone(getTeam(), getPos())){
			getTeam().getMatch().touchdown(getTeam());
		}
	}

	public void invokeSetPrice(int price) {
		this.price = price;

		Object priceFromScript = manager.invokeFunction("setPrice", this, price);
		if(priceFromScript != null) this.price = (Integer) priceFromScript;
	}

	// INVOKING ADJUSTERS

	public void invokeAdjustPosition(PitchField position) {
		this.position = position;

		Object positionFromScript = manager.invokeFunction("adjustPosition", this, position);
		if(positionFromScript != null) this.position = (PitchField) positionFromScript;
	}

	void invokeAdjustTeam(Team team) {
		this.team = team;

		Object teamFromScript = manager.invokeFunction("adjustTeam", this, team);
		if(teamFromScript != null) this.team = (Team) teamFromScript;
	}

	void invokeAdjustRemainingBe(int remainingBe) {
		this.remainingBe = remainingBe;

		Object remainingBeFromScript = manager.invokeFunction("adjustRemainingBe", this, remainingBe);
		if(remainingBeFromScript != null) this.remainingBe = (Integer) remainingBeFromScript;
	}

	void invokeAdjustMinHeadcount(int minHeadcount) {
		this.minHeadcount = minHeadcount;

		Object minHeadcountFromScript = manager.invokeFunction("adjustMinHeadcount", this, minHeadcount);
		if(minHeadcountFromScript != null) this.minHeadcount = (Integer) minHeadcountFromScript;
	}

	void invokeAdjustMaxHeadcount(int maxHeadcount) {
		this.maxHeadcount = maxHeadcount;

		Object maxHeadcountFromScript = manager.invokeFunction("adjustMaxHeadcount", this, maxHeadcount);
		if(maxHeadcountFromScript != null) this.maxHeadcount = (Integer) maxHeadcountFromScript;
	}

	// INVOKING HELPERS

	public void invokeCountDownRemainingBe(int i) {
		invokeSetRemainingBe(invokeGetRemainingBe() - i);
	}

	public void invokeClearPosition() {
		invokeSetPosition(Pitch.THE_VOID);
	}

	public void invokeResetRemainingBe() {
		invokeAdjustRemainingBe(invokeGetBe());
	}

	// INVOKING GETTERS

	public int invokeGetGe() {
		int geToReturn = ge;
		Object geFromScript = manager.invokeFunction("getGe", this);
		if(geFromScript != null) geToReturn = (Integer) geFromScript;
		return geToReturn;
	}

	public int invokeGetRs() {
		int rsToReturn = rs;
		Object rsFromScript = manager.invokeFunction("getRs", this);
		if(rsFromScript != null) rsToReturn = (Integer) rsFromScript;
		return rsToReturn;
	}

	public int invokeGetSt() {
		int stToReturn = st;
		Object stFromScript = manager.invokeFunction("getSt", this);
		if(stFromScript != null) stToReturn = (Integer) stFromScript;
		return stToReturn;
	}

	public int invokeGetBe() {
		int beToReturn = be;
		Object beFromScript = manager.invokeFunction("getBe", this);
		if(beFromScript != null) beToReturn = (Integer) beFromScript;
		return beToReturn;
	}

	public int invokeGetRemainingBe() {
		int remainingBeToReturn = remainingBe;
		Object remainingBeFromScript = manager.invokeFunction("getRemainingBe", this);
		if(remainingBeFromScript != null) remainingBeToReturn = (Integer) remainingBeFromScript;
		return remainingBeToReturn;
	}

	public PlayerCondition invokeGetPlayerCondition() {
		PlayerCondition playerConditionToReturn = playerCondition;
		Object playerConditionFromScript = manager.invokeFunction("getPlayerCondition", this);
		if(playerConditionFromScript != null) playerConditionToReturn = (PlayerCondition) playerConditionFromScript;
		return playerConditionToReturn;
	}
	
	public void invokeEventHappened(String eventString){
		manager.invokeFunction("eventHappened", this, eventString);
	}

	public Object invokeFunctionByName(String functionName, Object... params){
		return manager.invokeFunction(functionName, this, params);
	}
	
	public String toString() {
		return getName() + " No. " + getId();
	}

	public int findPlayerIndex(){
		for(int i = 0; i < this.getTeam().getPlayers().size(); i++){
			if(this.getTeam().getPlayers().get(i) == this){
				return i;
			}
		}
		return -1;
	}

    // DIRECT GETTERS FOR GUI

    public boolean $GUIisKOInjuredOrDead() {
        return 	   playerCondition == PlayerCondition.INJURED
                || playerCondition == PlayerCondition.KO
                || playerCondition == PlayerCondition.DEAD;
    }

    public String[] $GUIgetDescriptionLines() {
        return descriptionLines;
    }

    public PlayerCondition $GUIgetPlayerCondition() {
        return playerCondition;
    }

    public int $GUIgetGe() {
        return ge;
    }

    public int $GUIgetRs() {
        return rs;
    }

    public int $GUIgetSt() {
        return st;
    }

    public int $GUIgetBe() {
        return be;
    }

    public int $GUIgetRemainingBe() {
        return remainingBe;
    }

	// NORMAL GETTERS AND SETTERS

	public GameController getMatch() {
		if(team == null) {
			System.out.println("WARNING: Tried to getMatch() on a player without team. Create usable player with getCloneForTeam().");
			return null;
		} else return getTeam().getMatch();
	}

	public Team getTeam() {
		if(team == null) {
			System.out.println("WARNING: Tried to getTeam() on a player without team. Create usable player with getCloneForTeam().");
			return null;
		} else return team;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BufferedImage getSpriteR() {
		return spriteR;
	}

	public void setSpriteR(BufferedImage spriteR) {
		this.spriteR = spriteR;
	}

	public BufferedImage getSpriteL() {
		return spriteL;
	}

	public void setSpriteL(BufferedImage spriteL) {
		this.spriteL = spriteL;
	}

	public void setId() {
		this.id = getTeam().countUpIdCounter();
	}

	public int getId() {
		return id;
	}

	public int getPrice() {
		return price;
	}

	public int getMinHeadcount() {
		return minHeadcount;
	}

	public int getMaxHeadcount() {
		return maxHeadcount;
	}
	
	public boolean getRedCard(){
		return redCard;
	}

	public Rule getRule(int i) {
		if(i < 0 || i > 4) {
			getTeam().getMatch().getParent().log(Level.WARNING, "Tried to get a standard rule that doesn't exist. Returning null.");
			return null;
		}
		else return rules[i];
	}

	public SpecialRule[] getSpecialRules() {
		return specialRules;
	}
	
	public SpecialRule getSpecialRule(int i){
		return specialRules[i];
	}

	public Vector<int[]> getTackleZone() {
		return tackleZone;
	}

	public Vector<PitchField> getActiveTackleZone() {
		return activeTackleZone;
	}

	public PitchField getPosition() {
		return position;
	}

	public Vector2d getPos() {
		return position.getPos();
	}

	public boolean isHoldingBall() {
		return isHoldingBall;
	}

	public String getSpecialStat(String key) {
		return specialStats.get(key);
	}

	public void addSpecialStat(String key, String value) {
		specialStats.put(key, value);
	}

	public void removeSpecialStat(String key) {
		specialStats.remove(key);
	}
	
	public void setSpecialStat(String key, String value){
		removeSpecialStat(key);
		addSpecialStat(key, value);
	}

}
