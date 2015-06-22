package client.display;

import GUI.SBColor;
import client.Client;
import client.logic.GameRenderer;
import client.logic.PitchMouseLogic;
import gameLogic.*;
import gameLogic.dice.BlockDie;
import gameLogic.rules.SpecialRule;
import network.SBProtocolMessage;
import util.ResourceManager;

import javax.swing.*;
import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Level;

/**
 * The container in which the pitch is drawn.
 */
public class GameCanvas extends JPanel {

    public static final int MAX_TIMER_VALUE = 360, TIMER_DELAY = 10,
                            SHOW_ME_ALPHA = 300, SHOW_ME_ALPHA_DELAY = 200;

    private Pitch pitch = null;
    private GameFrame frame;
    private GameRenderer gameRenderer;
    private PitchMouseLogic pitchMouseLogic;

    // API fields
    private SBProtocolMessage APIMessage;
    private Vector2d[] highlightAPIPositions;
    private Color[] highlightAPIColors;
    private int showMeAlpha;
    private String showMeNow, showMeNowPlayer;
    private LinkedList<String> showMe = new LinkedList<String>(), showMePlayer = new LinkedList<String>();
    private boolean fieldAPI, choiceAPI, aimAPI, diceAPI;
    private int aimAPIIndex = -1, aimAPIDistance;
    private Player[] choiceAPIPlayers;
    private Vector2d[] fieldAPIFields;
    private int[] diceAPIDice;

    private Player playerChoosingSpecialRuleFor = null;
    private int[] specialRuleMenuPosition, specialRuleMenuSize;
    private int specialRuleMenuNameHeight, specialRuleMenuItemHeight;

    private double w, h, pW, t, pWSquared, pitchWidth, pitchHeight;
    private Player movingPlayer = null;
    private int[] mousePosition;
    private boolean canKickOff, canSetUp, canGiveBall;
    private boolean choseTeamType, choseTeam, sentChooseTeam, hasSetUpTeam;
    private boolean yourTurn;
    private int remainingMoney = GameController.MAX_MONEY_TO_SPEND;
    /**
     * <code>shiftPressed</code> is no boolean because if left and right shift
     * are both pressed and then right shift is being released again,
     * a boolean would be false even tough left shift is still pressed down.
     */
    private int shiftPressed = 0;
    public int frameCount = 0, frameRate = GamePanel.FRAME_RATE;
    public int timer = 0;
    private Team teamChosen = null;
    private Vector<Player> playersChosen = new Vector<Player>(),
            playersOnBench = new Vector<Player>(),
            playersOnOpponentBench = new Vector<Player>();

    public GameCanvas(GameFrame frame) {
        this.frame = frame;
        for (int i = 0; i < GameFrame.OLD_POSITIONS_STORED; i++)
            getGameFrame().getOldMousePositions().add(new int[]{-1, -1});

        // prepare and start mouse logic
        this.pitchMouseLogic = new PitchMouseLogic(this);

        final GameCanvas thisCanvas = this;
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                gameRenderer = new GameRenderer(thisCanvas);
                getGameFrame().getGamePanel().getInputMap().put(KeyStroke.getKeyStroke("released Y"), "releasedY");
                getGameFrame().getGamePanel().getActionMap().put("releasedY", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        shiftPressed = 0;
                    }
                });
                getGameFrame().getGamePanel().getInputMap().put(KeyStroke.getKeyStroke("pressed Y"), "pressedShift");
                getGameFrame().getGamePanel().getActionMap().put("pressedShift", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        shiftPressed = 1;
                    }
                });
                getGameFrame().getGamePanel().getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "pressedEnter");
                getGameFrame().getGamePanel().getActionMap().put("pressedEnter", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if(getGamePhase() == 0 && choseTeamType() && !sentChooseTeam) { // finished choosing team
                            chooseTeam();
                        }
                    }
                });
                getGameFrame().getGamePanel().getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "pressedSpace");
                getGameFrame().getGamePanel().getActionMap().put("pressedSpace", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if(isChoosingSpecialRule()) {
                            setPlayerChoosingSpecialRuleFor(null);
                        } else if(getPitchMouseLogic().isHoveringOwnPlayer()
                                && getPitchMouseLogic().getHoveringPlayer().getSpecialRules().length > 0
                                && !getPlayersOnBench().contains(getPitchMouseLogic().getHoveringPlayer())
                                && getGamePhase() == 3) {
                            setPlayerChoosingSpecialRuleFor(getPitchMouseLogic().getHoveringPlayer());
                            setSpecialRuleMenuPosition(getPitchMouseLogic().getMXYCoord());
                        }
                    }
                });
                getGameFrame().getGamePanel().getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "pressedEscape");
                getGameFrame().getGamePanel().getActionMap().put("pressedEscape", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setPlayerChoosingSpecialRuleFor(null);
                    }
                });
            }
        });
        addMouseListener(pitchMouseLogic.createMouseActionLogic());
        addMouseMotionListener(pitchMouseLogic.createMouseMotionLogic());

        // prepare and start the renderer
        setDoubleBuffered(true);
        ActionListener renderer = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
                frameCount++;
            }
        };
        (new Timer(1000/GamePanel.FRAME_RATE, renderer)).start();

        // framerate counter
        (new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frameRate = frameCount;
                frameCount = 0;
            }
        })).start();

        // framerate counter
        (new Timer(TIMER_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer++;
                if(timer > MAX_TIMER_VALUE) timer = 0;
            }
        })).start();
    }

    // HELPERS

    private String promptForTeamName() {
        JFrame frame = getGameFrame().isFullscreen() ? getGameFrame().getFullscreenFrame() : getGameFrame();
        return JOptionPane.showInternalInputDialog(frame.getContentPane(), "Choose a name for your team.", "Name Team", JOptionPane.QUESTION_MESSAGE);
    }

    public void chooseTeam() {
        if(teamChosen != null) {
            String[] playersChosenNames = new String[playersChosen.size()];
            for (int i = 0; i < playersChosen.size(); i++) playersChosenNames[i] = playersChosen.get(i).getName();
            String name = promptForTeamName();
            if(name != null) getGameFrame().chooseTeam(name, teamChosen.getType(), playersChosenNames);
        } else getClient().log(Level.WARNING, "Tried to choose null team.");
    }

    // DRAWING

    @Override
    public void paint(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        render(g2D);
    }

    public void render(Graphics2D g) {
        try {
            if(getClient().getMatch().getWeather() != null && !getGameRenderer().preparedWeather()) getGameRenderer().prepareWeather(getClient().getMatch().getWeather(), getW(), getH(), getPitchHeight());

            if(getPitch() != null) {

                movingPlayer = pitchMouseLogic.getMovingPlayer();
                getGameFrame().getOldMousePositions().remove(0);
                getGameFrame().getOldMousePositions().add(mousePosition);
                mousePosition = pitchMouseLogic.getMXYCoord();

                // 0: Team Choosing Phase, 1: Team Setup Phase, 2: Kick Phase, 3: Normal Playing Phase, 4: Finishing Phase, 5: Waiting Phase
                getGameFrame().setGamePhase(getClient().getMatch().getGamePhase());

                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                g.setPaint(SBColor.WHITE);
                g.fillRect(0, 0, (int) getW(), (int) getH());
                GameRenderer r = getGameRenderer();
                int phase = getGamePhase();

                if(r != null) {

                    if(phase >= 1) {
                        r.drawGUI(g);
                        //                    r.drawPitch(g);
                        if(getHighlightAPIPositions() != null && getHighlightAPIColors() != null)
                            r.drawHighlightAPI(g, getHighlightAPIPositions(), getHighlightAPIColors());
                        r.drawFieldsReachable(g);
                        r.drawPlayers(g);
                        r.drawPlayersBench(g);
                    }

                    r.drawGameActionFields(g);

                    if(phase >= 2) {
                        if(requestedDiceAPI()) r.drawDiceAPI(g, getDiceAPIDice()); // Dice API
                    }

                    if(phase >= 1) {
                        if(canSetUp()) r.drawSetPlayer(g);
                    }

                    if(phase >= 2) {
                        r.drawPath(g);
                        r.drawFieldAimingAt(g);
                        if(canGiveBall()) r.drawGiveBall(g);
                        // API
                        if(requestedAimAPI()) r.drawAimAPI(g, getAimAPIIndex(), getAimAPIDistance());
                        if(requestedChoiceAPI()) r.drawChoiceAPI(g, getChoiceAPIPlayers());
                        if(requestedFieldAPI()) r.drawFieldAPI(g, getFieldAPIFields());
                    }

                    if(phase >= 3) {
                        r.drawMovingPlayer(g);
                    }

                    if(phase >= 1) {
                        r.drawScore(g);
                        r.drawRoundCount(g);
                        r.drawPositionMarker(g);
                        r.drawFrameRate(g);
                        r.drawWeather(g);
                    }

                    r.drawGamePhaseInfo(g);

                    if(getPitchMouseLogic().getHoveringPlayer() != null || isChoosingSpecialRule())
                        r.drawTooltip(g, getPitchMouseLogic().getHoveringPlayer(), getPitchMouseLogic().getMXYCoord());
                    else if(getPitchMouseLogic().getHoveringPlayerOnBench() != null)
                        r.drawTooltip(g, getPitchMouseLogic().getHoveringPlayerOnBench(), getPitchMouseLogic().getMXYCoord());

                    if(showMe.size() >= 0) {
                        if(showMeAlpha > 0 && showMeNow != null && showMeNowPlayer != null) {
                            r.drawShowMe(g, showMeNowPlayer, showMeNow, showMeAlpha);
                            if(showMeAlpha > SHOW_ME_ALPHA_DELAY) showMeAlpha -= 1;
                            else {
                                double sub = (SHOW_ME_ALPHA_DELAY + 1 - showMeAlpha) / 8;
                                showMeAlpha -= sub > 0 ? sub : 1;
                            }
                        } else if(showMe.size() > 0) {
                            showMeNow = showMe.poll();
                            showMeNowPlayer = showMePlayer.poll();
                            showMeAlpha = SHOW_ME_ALPHA;
                        }
                    }

                }
            }
        } catch(NullPointerException e) {
            getClient().log(Level.WARNING, "Received nullpointer in PitchCanvas but continuing anyway.");
            e.printStackTrace();
        }
    }

    // API

    public void setHighlightAPIPositionsAndFields(Vector2d[] positions, Color[] colors) {
        this.highlightAPIPositions = positions;
        this.highlightAPIColors = colors;
    }

    public Vector2d[] getHighlightAPIPositions() {
        return highlightAPIPositions;
    }

    public Color[] getHighlightAPIColors() {
        return highlightAPIColors;
    }

    public void showMe(String player, String showMe) {
        if(player != null && showMe != null) {
            this.showMe.add(showMe);
            this.showMePlayer.add(player);
        }
    }

    public void setAPIMessage(SBProtocolMessage APIMessage) {
        this.APIMessage = APIMessage;
    }

    public SBProtocolMessage getAPIMessage() {
        return APIMessage;
    }

    public void setFieldAPI(boolean fieldAPI) {
        this.fieldAPI = fieldAPI;
    }

    public boolean requestedFieldAPI() {
        return fieldAPI;
    }

    public void setFieldAPIFields(Vector2d[] fieldAPIFields) {
        this.fieldAPIFields = fieldAPIFields;
    }

    public Vector2d[] getFieldAPIFields() {
        return fieldAPIFields;
    }

    public void setChoiceAPI(boolean choiceAPI) {
        this.choiceAPI = choiceAPI;
    }

    public boolean requestedChoiceAPI() {
        return choiceAPI;
    }

    public void setChoiceAPIPlayers(Player[] choiceAPIPlayers) {
        this.choiceAPIPlayers = choiceAPIPlayers;
    }

    public Player[] getChoiceAPIPlayers() {
        return choiceAPIPlayers;
    }

    public void setAimAPI(boolean aimAPI) {
        this.aimAPI = aimAPI;
    }

    public boolean requestedAimAPI() {
        return aimAPI;
    }

    public void setAimAPIIndex(int aimAPIIndex) {
        this.aimAPIIndex = aimAPIIndex;
    }

    public int getAimAPIIndex() {
        return aimAPIIndex;
    }

    public void setAimAPIDistance(int aimAPIDistance) {
        this.aimAPIDistance = aimAPIDistance;
    }

    public int getAimAPIDistance() {
        return aimAPIDistance;
    }

    public void setDiceAPI(boolean diceAPI) {
        this.diceAPI = diceAPI;
    }

    public boolean requestedDiceAPI() {
        return diceAPI;
    }

    public void setDiceAPIDice(int[] diceAPIDice) {
        this.diceAPIDice = diceAPIDice;
    }

    public int[] getDiceAPIDice() {
        return diceAPIDice;
    }

    // GETTERS & SETTERS

    public Client getClient() {
        return getGameFrame().getClient();
    }

    public PitchMouseLogic getPitchMouseLogic() {
        return pitchMouseLogic;
    }

    public GameRenderer getGameRenderer() {
        return gameRenderer;
    }

    /**
     * Set the size at which this pitch will be drawn.
     * @param size The size at which this pitch will be drawn.
     */
    public void setPreferredSize(Dimension size) {
        super.setPreferredSize(size);
        w = size.getWidth();
        h = size.getHeight();
        pitchWidth = w;
        pitchHeight = h / 1.5;
        pW = w/26;
        t = pW / ResourceManager.PEDESTAL_WIDTH;
        pWSquared = pW*pW;

        // move camera
        GameRenderer r = getGameRenderer();
        if(r != null) {
            double pitchHeight = getPitchHeight();
            r.vX = size.getWidth()/2;
            r.dY = pitchHeight;
            r.vZ = 2.04*pitchHeight;
            r.vY = 2.814*pitchHeight;
        }

        if(getGameRenderer() != null) getGameRenderer().emptyImageBuffer();
    }

    /**
     * Set a pitch to draw on this pitch canvas. If pitch is null, the pitch is not drawn.
     * @param pitch The pitch to draw.
     */
    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
    }

    public Pitch getPitch() {
        return pitch;
    }

    public GameFrame getGameFrame() {
        return frame;
    }

    public double getW() {
        return w;
    }

    public double getH() {
        return h;
    }

    public double getPW() {
        return pW;
    }

    public double getPH() {
        return pW;
    }

    public double getT() {
        return t;
    }

    public double getPitchWidth() {
        return pitchWidth;
    }

    public double getPitchHeight() {
        return pitchHeight;
    }

    public double getPWSquared() {
        return pWSquared;
    }

    public int[] getStoredMousePosition() {
        return mousePosition;
    }

    public int[] getSpecialRuleMenuPosition() {
        return specialRuleMenuPosition;
    }

    public void setSpecialRuleMenuPosition(int[] specialRuleMenuPosition) {
        this.specialRuleMenuPosition = specialRuleMenuPosition;
    }

    public int[] getSpecialRuleMenuSize() {
        return specialRuleMenuSize;
    }

    public void setSpecialRuleMenuSize(int[] specialRuleMenuSize) {
        this.specialRuleMenuSize = specialRuleMenuSize;
    }

    public int getSpecialRuleMenuNameHeight() {
        return specialRuleMenuNameHeight;
    }

    public void setSpecialRuleMenuNameHeight(int specialRuleMenuNameHeight) {
        this.specialRuleMenuNameHeight = specialRuleMenuNameHeight;
    }

    public int getSpecialRuleMenuItemHeight() {
        return specialRuleMenuItemHeight;
    }

    public void setSpecialRuleMenuItemHeight(int specialRuleMenuItemHeight) {
        this.specialRuleMenuItemHeight = specialRuleMenuItemHeight;
    }

    public Player getMovingPlayer() {
        return movingPlayer;
    }

    public PitchField getFieldAimingAt() {
        return getPitchMouseLogic().getFieldAimingAt();
    }

    public int getClientIndex() {
        return getGameFrame().getClientIndex();
    }

    public void setCanKickOff(boolean canKickOff) {
        this.canKickOff = canKickOff;
    }

    public boolean canKickOff() {
        return canKickOff;
    }

    public void setIsYourTurn(boolean yourTurn) {
        this.yourTurn = yourTurn;
    }

    public void setPlayerChoosingSpecialRuleFor(Player playerChoosingSpecialRuleFor) {
        this.playerChoosingSpecialRuleFor = playerChoosingSpecialRuleFor;
        if(playerChoosingSpecialRuleFor == null) setSpecialRuleMenuPosition(new int[]{-1000, -1000});
    }

    public void setCanSetUp(boolean canSetUp) {
        this.canSetUp = canSetUp;
    }

    public boolean canSetUp() {
        return canSetUp;
    }

    public void setChoseTeam(boolean choseTeam) {
        this.choseTeam = choseTeam;
    }

    public boolean choseTeam() {
        return choseTeam;
    }

    public void setHasSentChooseTeam(boolean sentChooseTeam) {
        this.sentChooseTeam = sentChooseTeam;
    }

    public boolean hasSentChooseTeam() {
        return sentChooseTeam;
    }

    public boolean canGiveBall() {
        return canGiveBall;
    }

    public void setCanGiveBall(boolean canGiveBall) {
        this.canGiveBall = canGiveBall;
    }

    public boolean choseTeamType() {
        return choseTeamType;
    }

    public void setChoseTeamType(boolean choseTeamType) {
        this.choseTeamType = choseTeamType;
    }

    public int getRemainingMoney() {
        return remainingMoney;
    }

    public void setRemainingMoney(int remainingMoney) {
        this.remainingMoney = remainingMoney;
    }

    public boolean isYourTurn() {
        return yourTurn;
    }

    public boolean isChoosingSpecialRule() {
        return getPlayerChoosingSpecialRuleFor() != null;
    }

    public Player getPlayerChoosingSpecialRuleFor() {
        return playerChoosingSpecialRuleFor;
    }

    public boolean isShiftPressed() {
        return shiftPressed > 0;
    }

    public Team getTeamChosen() {
        return teamChosen;
    }

    public void addPlayerChosen(Player player) {
        if(playersChosen.size() < Team.MAX_TEAM_SIZE) {
            if(getRemainingMoney() - player.getPrice() >= 0) {
                playersChosen.add(player);
                setRemainingMoney(getRemainingMoney() - player.getPrice());
            }
        }
        if(playersChosen.size() >= Team.MAX_TEAM_SIZE) chooseTeam();
    }

    public void removePlayerChosen(int index) {
        if(index >= 0 && index < playersChosen.size()) {
            Player playerRemoved = playersChosen.remove(index);
            if(playerRemoved != null)
                setRemainingMoney(getRemainingMoney() + playerRemoved.getPrice());
        }
    }

    public void resetPlayersChosen() {
        this.playersChosen = new Vector<Player>();
        this.remainingMoney = GameController.MAX_MONEY_TO_SPEND;
    }

    public Vector<Player> getPlayersChosen() {
        return playersChosen;
    }

    public void addPlayerOnBench(Player player) {
        if(player != null) playersOnBench.add(player);
    }

    public Vector<Player> getPlayersOnBench() {
        return playersOnBench;
    }

    public void addPlayerOnOpponentBench(Player player) {
        if(player != null) playersOnOpponentBench.add(player);
    }

    public Vector<Player> getPlayersOnOpponentBench() {
        return playersOnOpponentBench;
    }

    public void setTeamChosen(Team teamChosen) {
        this.teamChosen = teamChosen;
    }

    public void setHasSetUpTeam(boolean hasSetUpTeam) {
        this.hasSetUpTeam = hasSetUpTeam;
    }

    public boolean hasSetUpTeam() {
        return hasSetUpTeam;
    }

    public int getGamePhase() {
        return getGameFrame().getGamePhase();
    }

    public boolean isLeft() {
        return getClientIndex() == 0;
    }
}
