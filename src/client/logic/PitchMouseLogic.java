package client.logic;

import client.display.GameCanvas;
import gameLogic.*;
import gameLogic.rules.RuleThrow;
import util.ResourceManager;

import javax.vecmath.Vector2d;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

/**
 * A wrapper for all mouse related classes in SafariBowl.
 */
public class PitchMouseLogic {

    MouseMotionLogic mouseMotionLogic;
    MouseActionLogic mouseActionLogic;
    GameCanvas gameCanvas;
    Player hoveringPlayer = null, hoveringPlayerOnBench = null, movingPlayer = null, aimingPlayer = null;
    PitchField fieldBlitzing = null, fieldAimingAt = null;
    DrawingPath drawingPath = null;
    int mX = 0, mY = 0, mXOld = 0, mYOld = 0, mXCoord = 0, mYCoord = 0;
    int teamIndexHovering = -1, playerChoiceHovering = -1, playerRemoveHovering = -1, dieChoiceHovering = -1, specialRuleChoiceHovering = -1;
    boolean isAimingKickoff;
    volatile boolean calculating = false;

    public PitchMouseLogic(GameCanvas gameCanvas) {
        this.gameCanvas = gameCanvas;
    }

    public MouseMotionLogic createMouseMotionLogic() {
        this.mouseMotionLogic = new MouseMotionLogic();
        return mouseMotionLogic;
    }

    public MouseActionLogic createMouseActionLogic() {
        this.mouseActionLogic = new MouseActionLogic();
        return mouseActionLogic;
    }

    public boolean isHoveringOwnPlayer() {
        if(getHoveringPlayer() != null){
            String hoveringPlayerCoach = getHoveringPlayer().getTeam().getCoach().getName();
            String username = getGameCanvas().getGameFrame().getClient().getUsername();
            return hoveringPlayerCoach.equals(username);
        }else{
            return false;
        }
    }

    public double[] getMXY() {
        return new double[]{mX, mY};
    }

    public int[] getMXYCoord() {
        return new int[]{mXCoord, mYCoord};
    }

    public Player getHoveringPlayer() {
        return hoveringPlayer;
    }

    public Player getHoveringPlayerOnBench() {
        return hoveringPlayerOnBench;
    }

    public Player getMovingPlayer() {
        return movingPlayer;
    }

    public Player getAimingPlayer() {
        return aimingPlayer;
    }

    public DrawingPath getDrawingPath() {
        return drawingPath;
    }

    public PitchField getFieldBlitzing() {
        return fieldBlitzing;
    }

    public PitchField getFieldAimingAt() {
        return fieldAimingAt;
    }

    public int getTeamIndexHovering() {
        return teamIndexHovering;
    }

    public int getDieChoiceHovering() {
        return dieChoiceHovering;
    }

    public int getSpecialRuleChoiceHovering() {
        return specialRuleChoiceHovering;
    }

    public boolean isAimingKickoff() {
        return isAimingKickoff;
    }

    Pitch getPitch() {
        return getGameCanvas().getPitch();
    }

    int getGamePhase() {
        return getGameCanvas().getGamePhase();
    }

    double getW() {
        return getGameCanvas().getW();
    }

    double getH() {
        return getGameCanvas().getH();
    }

    public double getPitchWidth() {
        return getGameCanvas().getPitchWidth();
    }

    public double getPitchHeight() {
        return getGameCanvas().getPitchHeight();
    }

    double getpW() {
        return getGameCanvas().getPW();
    }

    double getpH() {
        return getGameCanvas().getPH();
    }

    void setMXYCoord(MouseEvent e) {
        mXCoord = e.getX();
        mYCoord = e.getY();
    }

    void resetMXY() {
        mX = mY = mXOld = mYOld = -1;
    }

    void setMXY(MouseEvent e) {
        double  xNew = e.getX(), yNew = e.getY();
        Vector2d mXYNew = getGameCanvas().getGameRenderer().dispToPos(new Vector2d(xNew, yNew));
        double  mXNewTemp = mXYNew.x / getpW(),
                mYNewTemp = mXYNew.y / getpH();
        if(mXNewTemp < 0) mXNewTemp -= 1; // fix negative coordinates rounding error
        if(mYNewTemp < 0) mYNewTemp -= 1; //                   "
        int     mXNew = (int) mXNewTemp,
                mYNew = (int) mYNewTemp;
        mXOld = mX;
        mYOld = mY;
        if(mXNew >= 0 && mXNew < Pitch.PITCH_LENGTH && mYNew >= 0 && mYNew < Pitch.PITCH_WIDTH) {
            mX = mXNew;
            mY = mYNew;
        } else {
            resetMXY();
        }
    }

    void setMXYEverywhere(MouseEvent e) {
        double  xNew = e.getX(), yNew = e.getY();
        Vector2d mXYNew = getGameCanvas().getGameRenderer().dispToPos(new Vector2d(xNew, yNew));
        double  mXNewTemp = mXYNew.x / getpW(),
                mYNewTemp = mXYNew.y / getpH();
        if(mXNewTemp < 0) mXNewTemp -= 1; // fix negative coordinates rounding error
        if(mYNewTemp < 0) mYNewTemp -= 1; //                   "
        int     mXNew = (int) mXNewTemp,
                mYNew = (int) mYNewTemp;
        mXOld = mX;
        mYOld = mY;
        mX = mXNew;
        mY = mYNew;
    }

    void setTeamIndexHovering(int teamIndexHovering) {
        this.teamIndexHovering = teamIndexHovering;
    }

    void setPlayerChoiceHovering(int playerChoiceHovering) {
        this.playerChoiceHovering = playerChoiceHovering;
    }

    void setPlayerRemoveHovering(int playerRemoveHovering) {
        this.playerRemoveHovering = playerRemoveHovering;
    }

    void setDieChoiceHovering(int dieChoiceHovering) {
        this.dieChoiceHovering = dieChoiceHovering;
    }

    void setSpecialRuleChoiceHovering(int specialRuleChoiceHovering) {
        this.specialRuleChoiceHovering = specialRuleChoiceHovering;
    }

    void setMovingPlayer(Player movingPlayer) {
        if(movingPlayer == null)
            log(Level.WARNING, "Tried to set moving player to null via setMovingPlayer(). Use resetMovingPlayer() instead.");
        else {
            this.movingPlayer = movingPlayer;
            this.drawingPath = new DrawingPath(getPitch(), movingPlayer.getPosition());
        }
    }

    void setHoveringPlayer(Player hoveringPlayer) {
        if(hoveringPlayer == null)
            log(Level.WARNING, "Tried to set hovering player to null via setHoveringPlayer(). Use resetHoveringPlayer() instead.");
        else
            this.hoveringPlayer = hoveringPlayer;
    }

    public void setHoveringPlayerOnBench(Player hoveringPlayerOnBench) {
        if(hoveringPlayerOnBench == null)
            log(Level.WARNING, "Tried to set hovering player on bench to null via setHoveringPlayerOnBench(). Use resetHoveringPlayerOnBench() instead.");
        else
            this.hoveringPlayerOnBench = hoveringPlayerOnBench;
    }

    void setHoveringPlayerOffField(int index, Vector<Player> playersOnBench, boolean onBench) {
        ArrayList<Integer> injuredPlayers;
        int i = 0;
        do {
            injuredPlayers = new ArrayList<Integer>();
            for(; i <= index; i++) {
                if(i < playersOnBench.size()) {
                    if(playersOnBench.get(i).$GUIisKOInjuredOrDead())
                        injuredPlayers.add(i);
                }
            }
            index += injuredPlayers.size();
        } while(injuredPlayers.size() > 0);
        if(index < playersOnBench.size()) {
            if(onBench) setHoveringPlayerOnBench(playersOnBench.get(index));
            else setHoveringPlayer(playersOnBench.get(index));
        }
    }

    void setAimingPlayer(Player aimingPlayer) {
        if(aimingPlayer == null) log(Level.WARNING, "Tried to set aiming player to null via setAimingPlayer(). Use resetAimingPlayer() instead.");
        else {
            this.aimingPlayer = aimingPlayer;
        }
    }

    void setIsAimingKickoff(boolean isAimingKickoff) {
        this.isAimingKickoff = isAimingKickoff;
    }

    void resetMovingPlayer() {
        this.movingPlayer = null;
        this.drawingPath = null;
        getGameCanvas().getGameFrame().resetDrawingPath();
        resetFieldBlitzing();
    }

    void resetHoveringPlayer() {
        this.hoveringPlayer = null;
    }

    void resetHoveringPlayerOnBench() {
        this.hoveringPlayerOnBench = null;
    }

    void resetAimingPlayer() {
        this.aimingPlayer = null;
        this.fieldAimingAt = null;
    }

    boolean isMovingPlayer() {
        return movingPlayer != null;
    }

    boolean isHoveringPlayer() {
        return hoveringPlayer != null;
    }

    boolean isHoveringPlayerOnBench() {
        return hoveringPlayerOnBench != null;
    }

    boolean isAiming() {
        return aimingPlayer != null;
    }

    void setFieldBlitzing(PitchField fieldBlitzing) {
        if(fieldBlitzing == null) log(Level.WARNING, "Tried to set field blitzing to null via setFieldBlitzing(). Use resetFieldBlitzing() instead.");
        else {
            this.fieldBlitzing = fieldBlitzing;
        }
    }

    void setFieldAimingAt(PitchField fieldAimingAt) {
        if(fieldAimingAt == null) log(Level.WARNING, "Tried to set field aiming at to null via setFieldAimingAt(). Use resetAimingPlayer() instead.");
        else {
            this.fieldAimingAt = fieldAimingAt;
        }
    }

    void resetFieldBlitzing() {
        this.fieldBlitzing = null;
    }

    void resetFieldAimingAt() {
        this.fieldAimingAt = null;
    }

    void addPathElement() {
        if(getFieldBlitzing() == null && getMovingPlayer() != null) { // only move if moving player is not blitzing
            boolean move = false;
            if(getMovingPlayer().invokeGetPlayerCondition() == PlayerCondition.PRONE) { // player is prone and needs 3 BE to stand up
                if (getDrawingPath().getPath().size() <= getMovingPlayer().invokeGetRemainingBe() - 3) // only add if prone moving player can move further
                    move = true;
            } else if(getDrawingPath().getPath().size() <= getMovingPlayer().invokeGetRemainingBe()) // only add if moving player can move further
                move = true;
            if(move) {
                resetFieldBlitzing();
                getDrawingPath().addPathElement(getPitch().getFields()[mX][mY]);
                getGameCanvas().getGameFrame().setDrawingPath(getDrawingPath(), getMovingPlayer()); // set the drawing path on the game panel to send
            }
        }
    }

    void removeLastPathElement() {
        getDrawingPath().removeLastPathElement();
    }

    void log(Level level, String message) {
        getGameCanvas().getGameFrame().getClient().log(level, message);
    }

    public GameCanvas getGameCanvas() {
        return gameCanvas;
    }

    private class MouseMotionLogic implements MouseMotionListener {

        @Override
        public void mouseDragged(MouseEvent event) {
            if(!calculating && getPitch() != null) { // if no other calculating thread is running, calculate
                final MouseEvent e = event;
                calculating = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        calculate(e);
                        calculating = false;
                    }
                }).start();
            }
        }

        @Override
        public void mouseMoved(MouseEvent event) {
            if(!calculating && getPitch() != null) { // if no other calculating thread is running, calculate
                final MouseEvent e = event;
                calculating = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        calculate(e);
                        calculating = false;
                    }
                }).start();
            }
        }

        void calculate(MouseEvent e) {
            try {

                setMXYCoord(e);
                setSpecialRuleChoiceHovering(-1);

                if(getGameCanvas().isChoosingSpecialRule()) calculateChooseSpecialRule(e);
                else {
                    // API
                    if(getGameCanvas().requestedDiceAPI()) calculateDiceAPI(e);
                    else if(getGameCanvas().requestedAimAPI()) calculateAimAPI(e);
                    else if(getGameCanvas().requestedChoiceAPI()) calculateChoiceAPI(e);

                    else if(getGamePhase() == 0) calculateChooseTeam();
                    else if(getGamePhase() == 2) calculateKickAndGiveBall(e);

                    if(getGamePhase() >= 3 && e.getY() < getPitchHeight()) calculateOnPitch(e);

                    if(getGamePhase() >= 1) calculateSetPlayers(e);

                    if(getGameCanvas().requestedFieldAPI()) calculateFieldAPI(e);
                }

            } catch(IndexOutOfBoundsException ex) {
                getGameCanvas().getClient().logStackTrace(ex);
            } catch(NullPointerException ex) {
                getGameCanvas().getClient().logStackTrace(ex);
            }
        }

        void calculateChooseSpecialRule(MouseEvent e) {
            int[]   pos = getGameCanvas().getSpecialRuleMenuPosition(),
                    size = getGameCanvas().getSpecialRuleMenuSize();
            int     hN = getGameCanvas().getSpecialRuleMenuNameHeight(),
                    hR = getGameCanvas().getSpecialRuleMenuItemHeight();

            if(getMXYCoord()[0] > pos[0] && getMXYCoord()[0] < pos[0] + size[0]
                    && getMXYCoord()[1] > pos[1] + hN/2 && getMXYCoord()[1] < pos[1] + size[1]) { // is hovering item in special rules list

                setSpecialRuleChoiceHovering((getMXYCoord()[1] - pos[1] - hN) / hR);

            }
        }

        void calculateDiceAPI(MouseEvent e) {
            double  actionFieldSize = getPitchHeight()/4,
                    actionFieldSizeHalved = actionFieldSize/2;

            if(mXCoord < actionFieldSize && mXCoord > 0 && mYCoord > getH() - actionFieldSize && mYCoord < getH()) {

                int col = mXCoord > actionFieldSizeHalved ? 1 : 0,
                    row = mYCoord > getH() - actionFieldSizeHalved ? 1 : 0;
                setDieChoiceHovering(row*2 + col);

            } else setDieChoiceHovering(-1);
        }

        void calculateAimAPI(MouseEvent e) {
            setMXY(e);
            resetFieldAimingAt();

            Team aimingTeam = getGameCanvas().isLeft() ? getGameCanvas().getClient().getMatch().getTeam(0) : getGameCanvas().getClient().getMatch().getTeam(1);

            if(aimingTeam != null && getGameCanvas().getAimAPIIndex() >= 0) {
                Player aimingPlayer = aimingTeam.getPlayers().get(getGameCanvas().getAimAPIIndex());

                if(aimingPlayer != null)
                    if(mX >= 0 && mX < Pitch.PITCH_LENGTH && mY >= 0 && mY < Pitch.PITCH_WIDTH)
                        setFieldAimingAt(getPitch().getFields()[mX][mY]);
            }
        }

        void calculateChoiceAPI(MouseEvent e) {
            setMXY(e);
            
            if(mX >= 0 && mX < Pitch.PITCH_LENGTH && mY >= 0 && mY < Pitch.PITCH_WIDTH) {
            	 if (getPitch().getFields()[mX][mY].getPlayer() != null) { // there is a player on the hovered field
                     setHoveringPlayer(getPitch().getFields()[mX][mY].getPlayer());
                 } else {
                     resetHoveringPlayer();
                 }
            }
        }

        void calculateFieldAPI(MouseEvent e) {
            setMXYEverywhere(e);
        }

        void calculateChooseTeam() {
            resetMXY();
            if(!getGameCanvas().choseTeamType()) {
                int numberOfTeams = getGameCanvas().getClient().getMatch().getNumberOfAvailableTeams();
                double  teamWidth = getPitchWidth() / numberOfTeams;
                if (mXCoord > 0 && mXCoord < getPitchWidth() && mYCoord > 0 && mYCoord < teamWidth) { // mouse is over team choice
                    int mXTeam = (int) (mXCoord / teamWidth);
                    setTeamIndexHovering(mXTeam);
                } else setTeamIndexHovering(-1);
            } else {
                calculateChoosePlayers();
                calculateRemovePlayers();
            }
        }

        void calculateChoosePlayers() {
            if(!getGameCanvas().hasSentChooseTeam()) {
                int numberOfAvailablePlayers = getGameCanvas().getClient().getMatch().getAvailableTeam(teamIndexHovering).getAvailablePlayers().size();
                double playerWidth = getPitchWidth() / numberOfAvailablePlayers,
                        playerHeight = playerWidth / ResourceManager.IMAGE_RATIO;
                if (playerHeight > getPitchHeight() / 2) {
                    playerHeight = getPitchHeight() / 2;
                    playerWidth = playerHeight * ResourceManager.IMAGE_RATIO;
                }
                if (mXCoord > 0 && mXCoord < getPitchWidth() && mYCoord > 0 && mYCoord < playerHeight) { // mouse is over player choice
                    int mXPlayer = (int) (mXCoord / playerWidth);
                    setPlayerChoiceHovering(mXPlayer);
                } else setPlayerChoiceHovering(-1);
            }
        }

        void calculateRemovePlayers() {
            int numberOfChosenPlayers = getGameCanvas().getPlayersChosen().size();
            double  playerWidth = getPitchWidth() / Team.MAX_TEAM_SIZE,
                    playerHeight = playerWidth / ResourceManager.IMAGE_RATIO,
                    crossSize = playerWidth / ResourceManager.IMAGE_WIDTH * ResourceManager.PROP_CROSS.getWidth();
            boolean hoveringCross = false;
            for (int i = 0; i < numberOfChosenPlayers; i++) {
                if(mYCoord > getPitchHeight() - playerHeight - crossSize/3 && mYCoord < getPitchHeight() - playerHeight + 2*crossSize/3
                        && mXCoord > (i+1) * playerWidth - crossSize && mXCoord < (i+1) * playerWidth) {
                    setPlayerRemoveHovering(i);
                    hoveringCross = true;
                }
            }
            if(!hoveringCross) setPlayerRemoveHovering(-1);
        }

        void calculateSetPlayers(MouseEvent e) {
            resetHoveringPlayerOnBench();

            if(mYCoord > getPitchHeight()) { // mouse is in control panel
                resetHoveringPlayer();
                resetFieldAimingAt();

                if(getMovingPlayer() == null) {

                    boolean left = getGameCanvas().getClientIndex() == 0;

                    if (left) { // left bench is players bench

                        calculateHoveringPlayers(getGameCanvas().getPlayersOnBench(), true, false);
                        calculateHoveringPlayers(getGameCanvas().getPlayersOnOpponentBench(), false, true);

                    } else { // right bench is players bench

                        calculateHoveringPlayers(getGameCanvas().getPlayersOnBench(), false, false);
                        calculateHoveringPlayers(getGameCanvas().getPlayersOnOpponentBench(), true, true);

                    }

                } else { // is moving player off the field
                    setFieldAimingAt(Pitch.THE_VOID);
                }

            } else if(getGamePhase() == 1) { // mouse is on pitch and players can be set
                resetHoveringPlayer();

                setMXY(e);
                boolean left = getGameCanvas().getClientIndex() == 0;

                if(mX >= 0 && mX < Pitch.PITCH_LENGTH && mY >= 0 && mY < Pitch.PITCH_WIDTH) {

                    if(getMovingPlayer() == null) { // is not moving player
                        if(getPitch().getFields()[mX][mY].getPlayer() != null) {
                            setHoveringPlayer(getPitch().getFields()[mX][mY].getPlayer());
                        }
                    } else { // is moving player
                        if(left && mX < Pitch.PITCH_LENGTH / 2 || !left && mX >= Pitch.PITCH_LENGTH / 2)
                            setFieldAimingAt(getPitch().getFields()[mX][mY]);
                        else resetFieldAimingAt();
                    }

                } else {
                    setFieldAimingAt(Pitch.THE_VOID);
                }

            }
        }

        void calculateHoveringPlayers(Vector<Player> playersOnBench, boolean left, boolean onBench) {
            int n = playersOnBench.size();
            double  playersInRow = Team.MAX_TEAM_SIZE / 2,
                    seatsInRow = playersInRow + 2,
                    seatWidth = getPitchWidth() / 2 / seatsInRow,
                    seatHeight = seatWidth / ResourceManager.IMAGE_RATIO;

            if(playersOnBench.size() > 0) {
                if(left) {

                    if(mYCoord < getPitchHeight() + seatHeight) { // potentially hovering player on first row of bench
                        if(mXCoord > seatWidth && mXCoord < ((n > playersInRow ? playersInRow : n) + 1) * seatWidth) {

                            setHoveringPlayerOffField((int) ((mXCoord - seatWidth) / seatWidth), playersOnBench, onBench);

                        }
                    } else if(mYCoord < getPitchHeight() + 2 * seatHeight && n > playersInRow) { // potentially hovering player on second row of bench
                        if(mXCoord > seatWidth && mXCoord < (n - playersInRow + 1) * seatWidth) {

                            setHoveringPlayerOffField((int) ((mXCoord - seatWidth) / seatWidth + playersInRow), playersOnBench, onBench);

                        }
                    }

                } else {

                    if(mYCoord < getPitchHeight() + seatHeight) { // potentially hovering player on first row of bench
                        if(mXCoord > seatWidth + getPitchWidth() / 2 && mXCoord < getPitchWidth() - ((n > playersInRow ? 0 : playersInRow - n) + 1) * seatWidth) {

                            setHoveringPlayerOffField((int) ((mXCoord - seatWidth - getPitchWidth() / 2) / seatWidth), playersOnBench, onBench);

                        }
                    } else if(mYCoord < getPitchHeight() + 2 * seatHeight && n > playersInRow) { // potentially hovering player on second row of bench
                        if(mXCoord > seatWidth + getPitchWidth() / 2 && mXCoord < getPitchWidth() - (2 * playersInRow - n + 1) * seatWidth) {

                            setHoveringPlayerOffField((int) ((mXCoord - seatWidth - getPitchWidth() / 2) / seatWidth + playersInRow), playersOnBench, onBench);

                        }
                    }

                }
            }
        }

        void calculateKickAndGiveBall(MouseEvent e) {
            setMXY(e);
            if(mX >= 0 && mX < Pitch.PITCH_LENGTH && mY >= 0 && mY < Pitch.PITCH_WIDTH) {

                if(getGameCanvas().canKickOff()) { // only kick of if allowed

                    if(getGameCanvas().getClientIndex() == 0) { // team is on left side

                        setAimingPlayer(new Kicker(Kicker.LEFT, getPitch()));
                        setIsAimingKickoff(true);

                        if(mX >= 13 && mYCoord < getPitchHeight()) setFieldAimingAt(getPitch().getFields()[mX][mY]);

                    } else if(getGameCanvas().getClientIndex() == 1) { // team is on right side

                        setAimingPlayer(new Kicker(Kicker.RIGHT, getPitch()));
                        setIsAimingKickoff(true);
                        if(mX < 13 && mYCoord < getPitchHeight()) setFieldAimingAt(getPitch().getFields()[mX][mY]);

                    } else getGameCanvas().getClient().log(Level.WARNING, "Illegal client index in MouseLogic::calculateKickAndGiveBall()");

                } else setIsAimingKickoff(false);
                if(getGameCanvas().canGiveBall()) { // only hover players potentially receiving ball if allowed

                    Player playerPotentiallyReceivingBall = getPitch().getFields()[mX][mY].getPlayer();
                    if (playerPotentiallyReceivingBall != null) { // there is a player on the hovered field
                        setHoveringPlayer(playerPotentiallyReceivingBall);
                        if (!isHoveringOwnPlayer())
                            resetHoveringPlayer(); // don't set if hovering player is not own player
                    }

                }

            }
        }

        void calculateOnPitch(MouseEvent e) {
            if (!isMovingPlayer()) { // is not moving player
                setMXY(e);
            } else { // is moving player
                // some magic numbers
                double  x0 = mX * getpW() + getpW() / 2, y0 = mY * getpH() + getpH() / 2,
                        a = 7*getpW()/8;
                Vector2d mouse = getGameCanvas().getGameRenderer().dispToPos(new Vector2d(e.getX(), e.getY()));
                double dx = mouse.x - x0, dy = mouse.y - y0;
                if ((dx < -a || dx > a) || (dy < -a || dy > a)) { // moved outside of action zone (ask milan what the heck that means)
                    setMXY(e);
                }
            }

            if(mX >= 0 && mX < Pitch.PITCH_LENGTH && mY >= 0 && mY < Pitch.PITCH_WIDTH) {

                if (getPitch() != null && (mX != mXOld || mY != mYOld)) { // mouse is on different field and pitch exists
                    if (isAiming()) { // is aiming...
                        Vector2d dist = (Vector2d) getAimingPlayer().getPos().clone();
                        if (!dist.equals(getPitch().getFields()[mX][mY].getPos())) { // ... but not on itself
                            dist.sub(new Vector2d(mX, mY));
                            if (dist.length() <= RuleThrow.LONG_BOMB) // distance is shorter than max
                                setFieldAimingAt(getPitch().getFields()[mX][mY]);
                        } else { // ... but on itself
                            resetAimingPlayer();
                        }
                    } else { // is not aiming
                        if (getPitch().getFields()[mX][mY].getPlayer() != null) { // there is a player on the hovered field
                            setHoveringPlayer(getPitch().getFields()[mX][mY].getPlayer());
                        } else {
                            resetHoveringPlayer();
                        }

                        if (isMovingPlayer()) { // player was moved one field
                            if (!isHoveringPlayer() || getHoveringPlayer().equals(getMovingPlayer())) { // next field is empty or moved player stands on this field
                                if (getDrawingPath().getPath().get(getDrawingPath().getPath().size() - 1).getPos().equals(new Vector2d(mX, mY))) { // hovering last drawn field
                                    resetFieldBlitzing();
                                }
                                if (getDrawingPath().getPath().size() > 1) { // path is longer than one field
                                    if (getDrawingPath().getPath().get(getDrawingPath().getPath().size() - 2).getPos().equals(new Vector2d(mX, mY))) { // moved one field back
                                        removeLastPathElement();
                                    } else {
                                        addPathElement();
                                    }
                                } else if (getDrawingPath().getPath().size() == 1) {
                                    addPathElement();
                                }
                            } else { // there is already a player on this field
                                PitchField potentialBlitzField = getPitch().getFields()[mX][mY];
                                if (!isHoveringOwnPlayer() && potentialBlitzField != null) // cannot blitz own player
                                    if (getMovingPlayer().invokeGetRemainingBe() >= getDrawingPath().getPath().size() // if player has another move..
                                            && getMovingPlayer().getTeam().getBlitz() // .. and team can still blitz, or ..
                                            || getDrawingPath().getPath().size() <= 1 // .. player is blocking ..
                                            && getMovingPlayer().invokeGetRemainingBe() == getMovingPlayer().invokeGetBe()) // .. and has not moved yet
                                        if (getPitch().isAdjacent(potentialBlitzField, getDrawingPath().getPath().get(getDrawingPath().getPath().size() - 1)))
                                            setFieldBlitzing(potentialBlitzField);
                            }
                        }

                    }
                }

            } else {
                resetAimingPlayer();
            }

            if(mYCoord >= getPitchHeight()) {
                resetHoveringPlayer();
                resetMovingPlayer();
                resetAimingPlayer();
            }
        }
    }

    private class MouseActionLogic implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            double actionFieldSize = getPitchHeight()/4;
            if(mXCoord > getW() - actionFieldSize && mXCoord < getW()
                    && mYCoord > getH() - actionFieldSize && mYCoord < getH()) { // clicked on right action field

                if(getGamePhase() == 0 && !getGameCanvas().hasSentChooseTeam()) { // finished choosing team
                    getGameCanvas().chooseTeam();
                } else if(getGamePhase() == 1) { // finished setting up players
                    getGameCanvas().getGameFrame().finishedSettingPlayers();
                } else if(getGamePhase() >= 3) { // end turn
                    getGameCanvas().getGameFrame().endTurn();
                }

            } else if(mXCoord < actionFieldSize && mXCoord > 0
                    && mYCoord > getH() - actionFieldSize && mYCoord < getH()) { // clicked on left action field

                if(getGamePhase() == 0 && !getGameCanvas().hasSentChooseTeam()) { // clicked back button
                    setTeamIndexHovering(-1);
                    getGameCanvas().setTeamChosen(null);
                    getGameCanvas().resetPlayersChosen();
                    getGameCanvas().setChoseTeamType(false);
                }

            } else { // potentially clicked surrender

                double  seatsInRow = Team.MAX_TEAM_SIZE/2,
                        benchWidth = (seatsInRow + 1) / (seatsInRow + 2) * getW()/2,
                        surrenderHeight = (getW() - 2*benchWidth) / 2;

                if(mXCoord > benchWidth && mXCoord < getW() - benchWidth && mYCoord > getH() - surrenderHeight && mYCoord < getH()) {
                    getGameCanvas().getGameFrame().surrender();
                }

            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if(getGamePhase() == 0) { // choosing team and players

                if(!getGameCanvas().choseTeamType()) { // choosing team
                    if(teamIndexHovering >= 0) {
                        getGameCanvas().setTeamChosen(getGameCanvas().getClient().getMatch().getAvailableTeam(teamIndexHovering));
                        getGameCanvas().setChoseTeamType(true);
                    }
                } else { // choosing players
                    if(!getGameCanvas().hasSentChooseTeam()) { // only choose if hasn't chosen already
                        if (playerChoiceHovering >= 0) {
                            Player playerChosen = getGameCanvas().getTeamChosen().getAvailablePlayers().get(playerChoiceHovering).getCloneForTeam(getGameCanvas().getTeamChosen());
                            if (playerChosen != null) {
                                int numberOfThisType = 0;
                                for (Player player : getGameCanvas().getPlayersChosen())
                                    if (player.getName().equals(playerChosen.getName()))
                                        numberOfThisType++;
                                if (numberOfThisType < playerChosen.getMaxHeadcount())
                                    getGameCanvas().addPlayerChosen(playerChosen);
                            }
                        } else if(playerRemoveHovering >= 0) {
                            getGameCanvas().removePlayerChosen(playerRemoveHovering);
                        }
                    }
                }

            } else if(getGamePhase() == 1) { // setting players

                if(getHoveringPlayer() != null
                        && isHoveringOwnPlayer()
                        && getGameCanvas().canSetUp()
                        && !getGameCanvas().hasSetUpTeam()) { // is hovering own player and can set up team
                    if((!getGameCanvas().getPlayersOnBench().contains(getHoveringPlayer()) || getGamePhase() == 1)
                            && getHoveringPlayer() != null && !getHoveringPlayer().getRedCard())
                        setMovingPlayer(getHoveringPlayer());
                }

            } else if(getGamePhase() >= 3) {

                if(getGameCanvas().isYourTurn()) {
                    if (isHoveringPlayer() && !isMovingPlayer()) { // is hovering over player but not while moving player
                        // before moving player throwing ball, check if this user is coach of the player
                        if (isHoveringOwnPlayer()) {
                            if (e.getButton() == MouseEvent.BUTTON1 && !getGameCanvas().isShiftPressed()) { // left mouse button, move
                                if(!getGameCanvas().getPlayersOnBench().contains(getHoveringPlayer()) || getGamePhase() == 1)
                                    if(!getGameCanvas().isChoosingSpecialRule())
                                        if(getHoveringPlayer().invokeGetRemainingBe() > 0)
                                            setMovingPlayer(getHoveringPlayer());
                                addPathElement();
                            } else if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2 || getGameCanvas().isShiftPressed()) { // right mouse button throw
                                if (getHoveringPlayer().isHoldingBall()
                                        && !(getHoveringPlayer() != getPitch().getTeam(getGameCanvas().getClientIndex()).getMovingPlayer()
                                             && getHoveringPlayer().invokeGetRemainingBe() == 0))
                                    setAimingPlayer(getHoveringPlayer());
                            }
                        }
                    }
                }

            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if(getGamePhase() == 1) { // setting players

                if(getFieldAimingAt() != null) { // is hovering field to set
                    if(getFieldAimingAt().equals(Pitch.THE_VOID)) { // is setting player back on bench
                        getGameCanvas().getGameFrame().setPlayer(getMovingPlayer(), getFieldAimingAt());
                    } else if(getFieldAimingAt().getPlayer() == null) // there is no player on this field yet
                        getGameCanvas().getGameFrame().setPlayer(getMovingPlayer(), getFieldAimingAt());
                }
                resetMovingPlayer();
                resetFieldAimingAt();

            } else if(getGamePhase() >= 2) {

                // API

                if(getGameCanvas().requestedDiceAPI()) { // Dice API
                    if(getDieChoiceHovering() >= 0 && getDieChoiceHovering() < getGameCanvas().getDiceAPIDice().length) {

                        getGameCanvas().getClient().getMatch().sendDiceAPI(getGameCanvas().getAPIMessage(), getDieChoiceHovering());
                        getGameCanvas().setDiceAPI(false);
                        getGameCanvas().setDiceAPIDice(null);
                        setDieChoiceHovering(-1);

                    }
                } else if(getGameCanvas().requestedAimAPI() && getFieldAimingAt() != null) { // Aiming API
                    if(getGameCanvas().requestedAimAPI()
                            && getGameCanvas().getAimAPIIndex() >= 0
                            && getGameCanvas().getAimAPIIndex() < Team.MAX_TEAM_SIZE
                            && getGameCanvas().getAimAPIDistance() > 0) {
                        Team aimingTeam = getGameCanvas().isLeft() ? getGameCanvas().getClient().getMatch().getTeam(0) : getGameCanvas().getClient().getMatch().getTeam(1);

                        if(aimingTeam != null) {
                            Player aimingPlayer = aimingTeam.getPlayers().get(getGameCanvas().getAimAPIIndex());

                            if(aimingPlayer != null) {
                                Vector2d aimingPlayerPos = (Vector2d) aimingPlayer.getPos().clone();

                                if(aimingPlayerPos != null) {
                                    Vector2d dist = new Vector2d(getMXY()[0], getMXY()[1]);
                                    dist.sub(aimingPlayerPos);

                                    if(dist.length() <= getGameCanvas().getAimAPIDistance()) {
                                        getGameCanvas().getClient().getMatch().sendAimAPI(getGameCanvas().getAPIMessage(), getFieldAimingAt().getPos());
                                        getGameCanvas().setAimAPI(false);
                                        getGameCanvas().setAimAPIIndex(-1);
                                    }
                                }
                            }
                        }
                    }
                } else if(getGameCanvas().requestedChoiceAPI() && getHoveringPlayer() != null) { // Choice API

                    Player[] playerChoices = getGameCanvas().getChoiceAPIPlayers();
                    for(int i = 0; i < playerChoices.length; i++) {
                        if(playerChoices[i].equals(getHoveringPlayer())) {
                            getGameCanvas().getClient().getMatch().sendChoiceAPI(getGameCanvas().getAPIMessage(), i);
                            getGameCanvas().setChoiceAPI(false);
                            getGameCanvas().setChoiceAPIPlayers(null);
                        }
                    }

                } else if(getGameCanvas().requestedFieldAPI()) { // Field API

                    for(Vector2d field: getGameCanvas().getFieldAPIFields()) {
                        if(field.x == getMXY()[0] && field.y == getMXY()[1]) { // send if clicked field is option

                            getGameCanvas().getClient().getMatch().sendFieldAPI(getGameCanvas().getAPIMessage(), new Vector2d(getMXY()[0], getMXY()[1]));
                            getGameCanvas().setFieldAPI(false);
                            getGameCanvas().setFieldAPIFields(null);
                            break;

                        }
                    }

                }

                // special rules

                else if(getGameCanvas().isChoosingSpecialRule()) {

                    if(getSpecialRuleChoiceHovering() >= 0) {
                        getGameCanvas().getGameFrame().specialRule(getGameCanvas().getPlayerChoosingSpecialRuleFor(), getSpecialRuleChoiceHovering());
                    } else {
                        getGameCanvas().setPlayerChoosingSpecialRuleFor(null);
                    }

                }

                // normal actions

                else if (isAiming()) {
                    if(mYCoord < getPitchHeight()) {
                        if (isAimingKickoff() && getFieldAimingAt() != null) { // is aiming
                            getGameCanvas().getGameFrame().kick(getFieldAimingAt());
                            setIsAimingKickoff(false);
                        } else { // is aiming throw
                            getGameCanvas().getGameFrame().throwBall(getFieldAimingAt(), getAimingPlayer());
                        }
                    }
                } else if (!isHoveringPlayer() || getHoveringPlayer().equals(getMovingPlayer())) { // is not hovering over player or moved player stands on this field
                    if (isMovingPlayer()) { // was moving player
                        if (getDrawingPath().getPath().size() > 0) {
                            getGameCanvas().getGameFrame().sendPreparedGUIGameMessage();
                            setHoveringPlayer(getMovingPlayer());
                        }
                    }
                } else { // is hovering over player
                    if (isMovingPlayer()) { // was moving player
                        if (getFieldBlitzing() != null) { // is blitzing
                            getGameCanvas().getGameFrame().blitzOrBlock(getDrawingPath(), getFieldBlitzing(), getMovingPlayer());
                        }
                    } else if(getGameCanvas().canGiveBall()) { // wants to give ball to hovering player
                        getGameCanvas().getGameFrame().giveBall(getHoveringPlayer());
                    }
                }

                resetMovingPlayer();
                resetAimingPlayer();

            }

            getGameCanvas().getGameFrame().getGamePanel().requestFocusInWindow();
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {
            resetHoveringPlayer();
            resetMovingPlayer();
        }
    }
}
