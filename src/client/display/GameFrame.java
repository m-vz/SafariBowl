package client.display;

import GUI.SBFrame;
import client.Client;
import client.logic.DrawingPath;
import client.sound.PlayerSound;
import gameLogic.Pitch;
import gameLogic.PitchField;
import gameLogic.Player;
import gameLogic.Team;
import gameLogic.rules.SpecialRule;
import network.SBProtocolCommand;
import network.SBProtocolMessage;
import network.SBProtocolParameter;
import network.SBProtocolParameterArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * The frame in which the game is played.
 */
public class GameFrame extends SBFrame {

    public static final double MAX_DRAG_ROTATION = Math.PI/6;
    public static final int OLD_POSITIONS_STORED = 2;

    private GraphicsDevice device;
    private JFrame fullscreenFrame;
    private GameCanvas gameCanvas;
    private GamePanel gamePanel;
    private ClientFrame parent;

    private ArrayList<int[]> oldMousePositions = new ArrayList<int[]>(); // a list with old [x, y] coordinates of the mouse pointer
    private int gameActionEventOrAnswer = 0; // 0: action, 1: event, 2: answer
    private String guiGameMessage = "[]";
    private SBProtocolCommand guiGameCommand;

    private DrawingPath drawingPath = null;
    private int gamePhase = -1, clientIndex = -1;

    public GameFrame(ClientFrame parent) {
        super("SafariBowl");

        // fullscreen
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        fullscreenFrame = new JFrame();
        fullscreenFrame.setUndecorated(true);
        fullscreenFrame.setResizable(false);

        this.parent = parent;
        this.gamePanel = new GamePanel(this);
        this.gameCanvas = new GameCanvas(this);

        this.clientIndex = getClient().getClientRole() == 1 ? 0 : 1;
        for (int i = 0; i < OLD_POSITIONS_STORED; i++)
            oldMousePositions.add(new int[]{-1, -1});

        addListeners();

        setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        fullscreenFrame.setLayout(new BoxLayout(fullscreenFrame.getContentPane(), BoxLayout.X_AXIS));

        setVisible(true);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                add(gameCanvas);
                add(gamePanel);
            }
        });
    }

    public boolean isFullscreen() {
        return device.getFullScreenWindow() != null;
    }

    /**
     * Toggle fullscreen mode.
     */
    public void toggleFullscreen() {
        if(!isFullscreen()) {
            setVisible(false);
            device.setFullScreenWindow(fullscreenFrame);
            fullscreenFrame.add(gameCanvas);
            fullscreenFrame.add(gamePanel);
            setSizes(fullscreenFrame.getContentPane().getSize(), false);
        } else {
            exitFullscreen();
        }
    }

    /**
     * Exit fullscreen mode.
     */
    public void exitFullscreen() {
        device.setFullScreenWindow(null);
        setVisible(true);
        setSizes(getContentPane().getSize(), true);
        add(gameCanvas);
        add(gamePanel);
    }

    public JFrame getFullscreenFrame() {
        return fullscreenFrame;
    }

    public void setSizes(Dimension size, final boolean resizeFrame) {
        double heightRatio = (double) Pitch.PITCH_LENGTH / ((double) Pitch.PITCH_WIDTH * 1.5);
        final Dimension   canvasNewSize = new Dimension((int) (heightRatio * size.getHeight()), (int) size.getHeight()),
                          panelNewSize = new Dimension((int) (size.getWidth() - canvasNewSize.getWidth()),
                                  (int) size.getHeight()),
                          frameBorderSizes = new Dimension(getWidth() - getContentPane().getWidth(),
                                  getHeight() - getContentPane().getHeight()),
                          frameNewSize = new Dimension((int) (size.getWidth()+frameBorderSizes.getWidth()),
                                  (int) (size.getHeight()+frameBorderSizes.getHeight()));

//        if(canvasNewSize.getWidth() > maxSize.getWidth()) {
//            canvasNewSize.setSize(maxSize.getWidth(), maxSize.getWidth() * heightRatio);
//            frameNewSize.setSize(canvasNewSize.getWidth()+frameBorderSizes.getWidth(),
//                    canvasNewSize.getHeight()+frameBorderSizes.getHeight());
//        }
//        if(canvasNewSize.getHeight() > maxSize.getHeight()) {
//            canvasNewSize.setSize(maxSize.getHeight() / heightRatio, maxSize.getHeight());
//            frameNewSize.setSize(canvasNewSize.getWidth()+frameBorderSizes.getWidth(),
//                    canvasNewSize.getHeight()+frameBorderSizes.getHeight());
//        }

        if(resizeFrame) setSize(frameNewSize);
        getGameCanvas().setPreferredSize(canvasNewSize);
        getGamePanel().setSizes(panelNewSize);
    }

    /**
     * Outsourced method for setting any handlers to avoid monolith constructor.
     */
    private void addListeners() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if(!isFullscreen()) setSizes(getContentPane().getSize(), true);
                    }
                });
            }
        });
    }

    // GUI GAME MESSAGES

    /**
     * Send a game action or event message to the server.
     */
    public void sendGameMessage() {
        String messageString;
        SBProtocolCommand actionOrEvent;
        switch (gameActionEventOrAnswer) {
            case 0:
                messageString = getGamePanel().getGameActionBoxSelected();
                if(messageString.startsWith("COOL LINEUP")) {
                    getClient().coolLineup(messageString.endsWith("RIGHT") ? 1 : -1);
                    return;
                }
                if(messageString.startsWith("SET TEAM")) messageString = "SET TEAM";
                actionOrEvent = SBProtocolCommand.ACTIO;
                break;
            case 1:
                messageString = getGamePanel().getGameEventBoxSelected();
                actionOrEvent = SBProtocolCommand.EVENT;
                break;
            case 2:
                String wholeString = getGamePanel().getGameActionOrEventFieldText(), answerTypeString = getGamePanel().getGameAnswerBoxSelected();
                String answerString = wholeString.replaceFirst("^\\S+ ", ""),
                        MID = wholeString.substring(0, wholeString.length()-answerString.length()-1);
                SBProtocolParameterArray answerParams = SBProtocolParameterArray.paraception(answerString);
                if(answerParams != null) {
                    SBProtocolCommand answerType = null;
                    if(answerTypeString.equals("SUC WORKD")) answerType = SBProtocolCommand.WORKD;
                    else if(answerTypeString.equals("FAI FAILD")) answerType = SBProtocolCommand.FAILD;
                    if(answerType != null) getClient().sendGameAnswer(MID, answerType, answerParams);
                    else {
                        getClient().log(Level.SEVERE, "This should not happen! GamePanel.java line 330.");
                        getGamePanel().writeMessage("Illegal answer type null. Not sending answer.");
                    }
                } else { // there was an error parsing the params
                    getGamePanel().writeMessage("Error parsing parameters. Check for typos.");
                    getGamePanel().focusGameActionOrEventField();
                }
                return;
            default:
                return;
        }
        SBProtocolParameterArray actionParams = SBProtocolParameterArray.paraception(getGamePanel().getGameActionOrEventFieldText());
        if(actionParams != null) {
            // prepare message params
            SBProtocolParameterArray messageParams = new SBProtocolParameterArray(messageString);
            for(int i = 0; i < actionParams.size(); i++){
                messageParams.addParameter(actionParams.getParameter(i));
            }
            // prepare message itself and send
            SBProtocolMessage message = new SBProtocolMessage(getClient().getUID(), actionOrEvent, messageParams);
            getClient().sendGameMessage(message);
        } else { // there was an error parsing the params
            getGamePanel().writeMessage("Error parsing parameters. Check for typos.");
            getGamePanel().focusGameActionOrEventField();
        }
    }

    public void sendPreparedGUIGameMessage() {
        SBProtocolParameterArray messageParams = SBProtocolParameterArray.paraception(guiGameMessage);
        if(messageParams != null) {
            if(guiGameCommand != null) {
                // prepare message itself and send
                SBProtocolMessage message = new SBProtocolMessage(getClient().getUID(), guiGameCommand, messageParams);
                getClient().sendGameMessage(message);
            } else {
                getGamePanel().writeMessage("Error sending game message. This should not happen, but has.");
                getGamePanel().focusGameActionOrEventField();
            }
        } else { // there was an error parsing the params
            getGamePanel().writeMessage("Error parsing parameters. Check for typos.");
            getGamePanel().focusGameActionOrEventField();
        }
    }

    /**
     * Prompts the user if they really want to surrender.
     */
    public void surrender() {
        if(getGamePanel().getClientFrame().getSurrenderAnswer()) {
            getGamePanel().writeMessage("You gave up. You turtle.");
            getClient().surrender();
        }
    }

    /**
     * End your turn.
     */
    public void endTurn() {
        getClient().endTurn();
    }

    public void chooseTeam(String teamName, String teamType, String... players) {
        if(players.length >= Team.MIN_TEAM_SIZE && players.length <= Team.MAX_TEAM_SIZE) {
            String s = "\", \"";
            guiGameMessage = "[\"SET&space;TEAM\", \"" + SBProtocolParameter.escape(teamName) + s + SBProtocolParameter.escape(teamType) + s;
            for (String player : players)
                guiGameMessage += SBProtocolParameter.escape(player) + s;
            guiGameMessage = guiGameMessage.substring(0, guiGameMessage.length() - s.length()) + "\"]";
            guiGameCommand = SBProtocolCommand.ACTIO;
            sendPreparedGUIGameMessage();
            setHasSentChooseTeam(true);
        }
    }

    public void setDrawingPath(DrawingPath path, Player player) {
        if(path == null) resetDrawingPath();
        else {
            this.drawingPath = path;
            updateDrawingPathString(player);
        }
    }

    public void resetDrawingPath() {
        this.drawingPath = null;
    }

    private void updateDrawingPathString(Player player) {
        String s = "\", \"";
        guiGameMessage = "[\"" + SBProtocolMessage.ACTIO_MOVE + s + (player.getId()-1) + "\"";
        if(drawingPath.getPath().size() == 0) guiGameMessage = "[\"" + SBProtocolMessage.ACTIO_MOVE + s + (player.getId()-1) + s + player.getPos().x + s + player.getPos().y + "\"";
        for(PitchField field: drawingPath.getPath())
            guiGameMessage += ", \"" + (int) field.getPos().x + s + (int) field.getPos().y + "\"";
        guiGameMessage += "]";
        guiGameCommand = SBProtocolCommand.ACTIO;
    }

    /**
     * Send a blitz or block message to the server.
     * @param path The path the player moves before the blitz or block.
     * @param destination The destination of the blitz or block.
     * @param actor The player blitzing or blocking.
     */
    public void blitzOrBlock(DrawingPath path, PitchField destination, Player actor) {
        if(path != null && destination != null && actor != null) {
            try {
                if(destination.getPlayer() != null) {
                    SBProtocolParameterArray params;
                    if (path.getPath().size() > 1) { // is blitz
                        PlayerSound.blitz(actor);

                        params = new SBProtocolParameterArray(SBProtocolMessage.ACTIO_MOVE, (actor.getId() - 1) + "");
                        for (PitchField field : path.getPath()) {
                            params.addParameter(new SBProtocolParameter((int) field.getPos().x + ""));
                            params.addParameter(new SBProtocolParameter((int) field.getPos().y + ""));
                        }
                        params.addParameter(new SBProtocolParameter((int) destination.getPos().x + ""));
                        params.addParameter(new SBProtocolParameter((int) destination.getPos().y + ""));
                    } else { // is block
                        PlayerSound.block(actor);

                        params = new SBProtocolParameterArray(SBProtocolMessage.ACTIO_BLCK, (actor.getId() - 1) + "");
                        params.addParameter(new SBProtocolParameter((destination.getPlayer().getId() - 1) + ""));
                    }
                    getClient().sendGameMessage(new SBProtocolMessage(getClient().getUID(), SBProtocolCommand.ACTIO, params));
                }
            } catch (NullPointerException e) {
                getClient().log(Level.WARNING, "Catched nullpointer while blitzing! (But continuing)");
                getClient().logStackTrace(e);
            }
        }
    }

    public void throwBall(PitchField throwDestination, Player thrower) {
        if(throwDestination != null && thrower != null) {
            try {
                PlayerSound.pass(thrower);

                SBProtocolParameterArray params = new SBProtocolParameterArray(
                        "THRW",
                        (thrower.getId() - 1) + "",
                        (int) throwDestination.getPos().x + "",
                        (int) throwDestination.getPos().y + "");
                getClient().sendGameMessage(new SBProtocolMessage(getClient().getUID(), SBProtocolCommand.ACTIO, params));
            } catch (NullPointerException e) {
                getClient().log(Level.WARNING, "Catched nullpointer while throwing! (But continuing)");
                getClient().logStackTrace(e);
            }
        }
    }

    public void kick(PitchField kickDestination) {
        if(kickDestination != null) {
            setCanKickOff(false);
            getClient().getMatch().kick(kickDestination);
        }
    }

    public void specialRule(Player player, int specialRuleIndex) {
        if(specialRuleIndex >= 0 && specialRuleIndex < player.getSpecialRules().length) {
            SpecialRule specialRule = player.getSpecialRules()[specialRuleIndex];

            if(specialRule != null) {
                SBProtocolParameterArray params = new SBProtocolParameterArray(SBProtocolMessage.ACTIO_SPCL, (player.getId()-1)+"", specialRule.getName());
                getClient().sendGameMessage(new SBProtocolMessage(getClient().getUID(), SBProtocolCommand.ACTIO, params));
                getGameCanvas().setPlayerChoosingSpecialRuleFor(null);
            }
        }
    }

    public void giveBall(Player playerReceivingBall) {
        if(playerReceivingBall != null) {
            setCanGiveBall(false);
            getClient().getMatch().giveBall(playerReceivingBall);
        }
    }

    public void setPlayer(Player player, PitchField destination) {
        boolean left = gameCanvas.getClientIndex() == 0;
        if(player != null) {
            guiGameCommand = SBProtocolCommand.ACTIO;
            String s = "\", \"";
            boolean send = false;
            if(left && getGameCanvas().getPitch().isOnLeftHalf(destination.getPos())
                    || !left && getGameCanvas().getPitch().isOnRightHalf(destination.getPos())
                    || destination.equals(Pitch.THE_VOID)) // player can and wants to set on same half or back on bench
                send = true;
            if(send) {
                guiGameMessage = "[\"" + SBProtocolParameter.escape(SBProtocolMessage.ACTIO_SET_PLAYER) + s + (player.getId()-1) + s + (int) destination.getPos().x + s + (int) destination.getPos().y + "\"]";
                sendPreparedGUIGameMessage();
            }
        }
    }

    public void finishedSettingPlayers() {
        getGameCanvas().setHasSetUpTeam(true);
        getClient().sendGameMessage(new SBProtocolMessage(getClient().getUID(), SBProtocolCommand.EVENT, SBProtocolMessage.EVENT_ALL_PLAYERS_SET));
    }

    // GETTERS & SETTERS

    public Client getClient() {
        return getGamePanel().getClient();
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public GameCanvas getGameCanvas() {
        return gameCanvas;
    }

    public ClientFrame getClientFrame() {
        return parent;
    }

    public String getGuiGameMessage() {
        return guiGameMessage;
    }

    public void setGameActionEventOrAnswer(int gameActionEventOrAnswer) {
        this.gameActionEventOrAnswer = gameActionEventOrAnswer;
    }

    public int getGamePhase() {
        return gamePhase;
    }

    public void setGamePhase(int gamePhase) {
        this.gamePhase = gamePhase;
    }

    public int getClientIndex() {
        return clientIndex;
    }

    public ArrayList<int[]> getOldMousePositions() {
        return oldMousePositions;
    }

    /**
     * Set the pitch for the pitch canvas to draw.
     * @param pitch The pitch to draw on the pitch canvas.
     */
    public void setPitchOnCanvas(Pitch pitch) {
        gameCanvas.setPitch(pitch);
    }

    public void setCanKickOff(boolean canKickOff) {
        gameCanvas.setCanKickOff(canKickOff);
    }

    public void setIsYourTurn(boolean yourTurn) {
        gameCanvas.setIsYourTurn(yourTurn);
    }

    public void setCanSetUp(boolean canSetUp) {
        gameCanvas.setCanSetUp(canSetUp);
    }

    public void setHasSetUpTeam(boolean hasSetUpTeam) {
        gameCanvas.setHasSetUpTeam(hasSetUpTeam);
    }

    public void setHasSentChooseTeam(boolean hasSentChooseTeam) {
        gameCanvas.setHasSentChooseTeam(hasSentChooseTeam);
    }

    public void setChoseTeam(boolean choseTeam) {
        gameCanvas.setChoseTeam(choseTeam);
    }

    public void setCanGiveBall(boolean canGiveBall) {
        gameCanvas.setCanGiveBall(canGiveBall);
    }

}
