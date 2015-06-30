package client.display;

import GUI.SBFrame;
import GUI.SBGamePanel;
import client.Client;
import gameLogic.Pitch;
import gameLogic.Player;
import network.SBProtocolMessage;
import util.ResourceManager;

import javax.swing.*;
import javax.vecmath.Vector2d;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * The client game panel
 * Created by milan on 23.3.15.
 */
public class GamePanel extends SBGamePanel {

    public static final int D = 10, CONTROLS_HEIGHT = 240, FRAME_RATE = 60; // frame rate in fps

    private JTextField chatField, gameActionOrEventField;
    private JTextArea chatArea;
    private JScrollPane chatAreaPane;
    private JButton sendMessageButton, sendGameActionOrEventButton, surrenderButton, endTurnButton, fullscreenButton;
    private JComboBox gameAnswerBox, gameActionBox, gameEventBox, chooseActionOrEventBox;
    private CardLayout gameActionOrEventLayout;
    private JPanel gameActionOrEventPanel, chatPanel, gameMessagePanel, otherControlsPanel, gameMessageCommandPanel;
    private GameFrame gameFrame;
    private HashMap<String, String> gameActions = new HashMap<String, String>(),
                                    gameEvents = new HashMap<String, String>(),
                                    gameAnswers = new HashMap<String, String>();
    private ClientFrame clientFrame;
    private boolean autoFilledChatRecipient;

    /**
     * Create a panel with client login GUI to be displayed in a game frame.
     * @param gameFrame The game frame to display this panel in.
     */
    public GamePanel(GameFrame gameFrame) {
        super(new GridBagLayout());
        this.gameFrame = gameFrame;
        this.clientFrame = gameFrame.getClientFrame();
        setUp();
    }

    private void setUp() {
        // add actions, events and their descriptions
        putActions();
        putEvents();
        putAnswers();

        // prepare areas
        prepareChatArea();
        prepareChatPanel();
//        prepareGameMessagePanel();
        prepareOtherControls();

        // DOSTUFF™
        prepareLayouts();
        unsetOpaque();
        addComponents();
        addListeners();
//        showGameActionBox();
        SBFrame.addScroller(chatArea, chatAreaPane);
    }

    public void setSizes(Dimension size) {
        int w = size.width, h = size.height;

        chatAreaPane.setPreferredSize(new Dimension(w / 2, h - 4 * D - 5 * D));
        chatPanel.setPreferredSize(new Dimension(w / 2, 4 * D));
//        gameMessagePanel.setPreferredSize(new Dimension(w / 2, 4 * D));
//        gameMessageCommandPanel.setPreferredSize(new Dimension(w / 2, 4 * D));
        otherControlsPanel.setPreferredSize(new Dimension(w / 2, 5 * D));

        chatField.setPreferredSize(new Dimension(chatPanel.getWidth() - sendMessageButton.getWidth() - 3 * D, 3 * D));
//        gameActionOrEventField.setPreferredSize(new Dimension(gameMessagePanel.getWidth() - sendGameActionOrEventButton.getWidth() - 3 * D, 3 * D));
        setPreferredSize(size);
    }

    /**
     * Set focus to the chat field.
     */
    public void focusChatField() {
        chatField.requestFocusInWindow();
    }

    /**
     * Set focus to the change name field.
     */
    public void focusGameActionOrEventField() {
        gameActionOrEventField.requestFocusInWindow();
    }

    public void showGameActionBox() {
        gameFrame.setGameActionEventOrAnswer(0);
        gameActionOrEventLayout.show(gameActionOrEventPanel, "ACTIONBOX");
        gameActionBox.requestFocusInWindow();
        gameActionOrEventPanel.setPreferredSize(gameActionBox.getPreferredSize());
    }

    public void showGameEventBox() {
        gameFrame.setGameActionEventOrAnswer(1);
        gameActionOrEventLayout.show(gameActionOrEventPanel, "EVENTBOX");
        gameEventBox.requestFocusInWindow();
        gameActionOrEventPanel.setPreferredSize(gameEventBox.getPreferredSize());
    }

    public void showGameAnswerBox() {
        gameFrame.setGameActionEventOrAnswer(2);
        gameActionOrEventLayout.show(gameActionOrEventPanel, "ANSWERBOX");
        gameAnswerBox.requestFocusInWindow();
        gameActionOrEventPanel.setPreferredSize(gameAnswerBox.getPreferredSize());
    }

    /**
     * Append a message without a sender to the message panel.
     * @param message The message to append.
     */
    public void writeMessage(String message) {
        if(chatArea.getText().length() <= 0) chatArea.append(message);
        else chatArea.append("\n" + message);
    }

    /**
     * Append a chat message to the message panel.
     * @param sender The name of the sender of this message.
     * @param message The message itself.
     */
    public void addChatMessage(String sender, String message) {
        if(chatArea.getText().length() <= 0) chatArea.append(sender + ": " + message);
        else chatArea.append("\n" + sender + ": " + message);
    }

    /**
     * Reset the fields and re-enable game-start-components.
     */
    public void resetGamePanel() {
        focusGameActionOrEventField();
        chatField.setText("");
        chatArea.setText("");
        gameActionOrEventField.setText("");
    }

    /**
     * Prompt the client to follow a pushed player.
     * @return Whether the client wants to follow.
     */
    public boolean getFollowAnswer() {
        JFrame frame = gameFrame.isFullscreen() ? gameFrame.getFullscreenFrame() : gameFrame;
        return JOptionPane.showInternalConfirmDialog(frame.getContentPane(),
                "Follow pushed player?",
                "Follow?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new ImageIcon(ResourceManager.IMAGE_MODERATOR_WALRUS)) == 0;
    }

    // SETUP HELPERS

    /**
     * Outsourced method for setting any handlers to avoid monolith constructor.
     */
    private void addListeners() {

        chatField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoFilledChatRecipient = clientFrame.sendMessage(chatField);
            }
        });
        chatField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && autoFilledChatRecipient) chatField.setText("");
                if (e.getKeyCode() != KeyEvent.VK_ENTER) autoFilledChatRecipient = false;
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        sendMessageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoFilledChatRecipient = clientFrame.sendMessage(chatField);
            }
        });
        sendMessageButton.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(KeyEvent e) {
                // submit form if enter was typed on logoutButton
                if (e.getKeyCode() == KeyEvent.VK_ENTER) autoFilledChatRecipient = clientFrame.sendMessage(chatField);
            }
        });
//        gameActionOrEventField.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                gameFrame.sendGameMessage();
//            }
//        });
//        sendGameActionOrEventButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                gameFrame.sendGameMessage();
//            }
//        });
//        sendGameActionOrEventButton.addKeyListener(new KeyListener() {
//            public void keyTyped(KeyEvent e) {
//            }
//
//            public void keyPressed(KeyEvent e) {
//            }
//
//            public void keyReleased(KeyEvent e) {
                // submit form if enter was typed on logoutButton
//                if (e.getKeyCode() == KeyEvent.VK_ENTER) gameFrame.sendGameMessage();
//            }
//        });
//        chooseActionOrEventBox.addActionListener(new ActionListener() {
//            @SuppressWarnings("SuspiciousMethodCalls")
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (chooseActionOrEventBox.getSelectedItem().equals("GAM EVENT")) { // show event box
//                    showGameEventBox();
//                    gameActionOrEventField.setText(gameEvents.get(gameEventBox.getSelectedItem()));
//                } else if (chooseActionOrEventBox.getSelectedItem().equals("GAM ACTIO")) { // show action box
//                    showGameActionBox();
//                    gameActionOrEventField.setText(gameActions.get(gameActionBox.getSelectedItem()));
//                } else { // show answer box
//                    showGameAnswerBox();
//                    gameActionOrEventField.setText(gameAnswers.get(gameAnswerBox.getSelectedItem()));
//                }
//            }
//        });
//        gameActionBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                String actionString = (String) gameActionBox.getSelectedItem();
//                for (String action : gameActions.keySet()) {
//                    if (actionString.equals(action)) {
//                        gameActionOrEventField.setText(gameActions.get(action));
//                    }
//                }
//            }
//        });
//        gameEventBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                String actionString = (String) gameEventBox.getSelectedItem();
//                for (String action : gameEvents.keySet()) {
//                    if (actionString.equals(action)) {
//                        gameActionOrEventField.setText(gameEvents.get(action));
//                    }
//                }
//            }
//        });
//        surrenderButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                gameFrame.surrender();
//            }
//        });
//        endTurnButton.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                gameFrame.endTurn();
//            }
//        });
        fullscreenButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameFrame.toggleFullscreen();
            }
        });

        chatArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                focusChatField();
            }
        });

    }

    private void prepareChatArea() {
        chatArea = new JTextArea();
        chatAreaPane = new JScrollPane(chatArea);
        chatArea.setFont(new Font("monospaced", Font.PLAIN, 12));
        chatArea.setEditable(false);
    }

    private void prepareChatPanel() {
        chatField = new JTextField();
        sendMessageButton = new JButton("Send");

        chatPanel = new JPanel();
    }

    @SuppressWarnings("unchecked")
    private void prepareGameMessagePanel() {
        chooseActionOrEventBox = new JComboBox(new String[]{"GAM ACTIO", "GAM EVENT", "ANSWER"});

        String[] gameActionsArray = gameActions.keySet().toArray(new String[gameActions.size()]);
        Arrays.sort(gameActionsArray);
        gameActionBox = new JComboBox(gameActionsArray);
        String[] gameEventsArray = gameEvents.keySet().toArray(new String[gameEvents.size()]);
        Arrays.sort(gameEventsArray);
        gameEventBox = new JComboBox(gameEventsArray);
        String[] gameAnswersArray = gameAnswers.keySet().toArray(new String[gameAnswers.size()]);
        // switch fail and success so success is selected first
        String          fai = gameAnswersArray[0];
        gameAnswersArray[0] = gameAnswersArray[1];
        gameAnswersArray[1] = fai;
        gameAnswerBox = new JComboBox(gameAnswersArray);

        gameActionOrEventPanel = new JPanel();
        gameActionOrEventLayout = new CardLayout();
        gameActionOrEventPanel.setLayout(gameActionOrEventLayout);
        gameActionOrEventPanel.add("ACTIONBOX", gameActionBox);
        gameActionOrEventPanel.add("EVENTBOX", gameEventBox);
        gameActionOrEventPanel.add("ANSWERBOX", gameAnswerBox);
        gameActionOrEventLayout.addLayoutComponent(gameActionBox, "ACTIONBOX");
        gameActionOrEventLayout.addLayoutComponent(gameEventBox, "EVENTBOX");
        gameActionOrEventLayout.addLayoutComponent(gameAnswerBox, "ANSWERBOX");

        gameActionOrEventField = new JTextField();

        sendGameActionOrEventButton = new JButton("Send");

        gameMessagePanel = new JPanel();
        gameMessageCommandPanel = new JPanel();
    }

    private void prepareOtherControls() {
        fullscreenButton = new JButton("Fullscreen");
//        surrenderButton = new JButton("Surrender");
//        endTurnButton = new JButton("End Turn");

        otherControlsPanel = new JPanel();
    }

    private void prepareLayouts() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.X_AXIS));
//        gameMessageCommandPanel.setLayout(new BoxLayout(gameMessageCommandPanel, BoxLayout.X_AXIS));
//        gameMessagePanel.setLayout(new BoxLayout(gameMessagePanel, BoxLayout.X_AXIS));
        otherControlsPanel.setLayout(new BoxLayout(otherControlsPanel, BoxLayout.X_AXIS));
    }

    private void unsetOpaque() {
        chatAreaPane.setOpaque(false);
        chatPanel.setOpaque(false);
//        gameMessageCommandPanel.setOpaque(false);
//        gameMessagePanel.setOpaque(false);
        otherControlsPanel.setOpaque(false);
    }

    private void addComponents() {
        chatAreaPane.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));

        chatPanel.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));
        chatPanel.add(chatField);
        chatPanel.add(Box.createRigidArea(new Dimension(D, 0)));
        chatPanel.add(sendMessageButton);

//        gameMessageCommandPanel.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));
//        gameMessageCommandPanel.add(Box.createHorizontalGlue());
//        gameMessageCommandPanel.add(chooseActionOrEventBox);
//        gameMessageCommandPanel.add(Box.createRigidArea(new Dimension(D, 0)));
//        gameMessageCommandPanel.add(gameActionOrEventPanel);

//        gameMessagePanel.setBorder(BorderFactory.createEmptyBorder(D, D, 0, D));
//        gameMessagePanel.add(gameActionOrEventField);
//        gameMessagePanel.add(Box.createRigidArea(new Dimension(D, 0)));
//        gameMessagePanel.add(sendGameActionOrEventButton);

        otherControlsPanel.setBorder(BorderFactory.createEmptyBorder(D, D, D, D));
        otherControlsPanel.add(fullscreenButton);
//        otherControlsPanel.add(Box.createHorizontalGlue());
//        otherControlsPanel.add(surrenderButton);
//        otherControlsPanel.add(Box.createRigidArea(new Dimension(D, 0)));
//        otherControlsPanel.add(endTurnButton);

        add(chatAreaPane);
        add(chatPanel);
//        add(gameMessageCommandPanel);
//        add(gameMessagePanel);
        add(otherControlsPanel);

    }

    /**
     * Add game actions and their descriptions to the game actions map
     */
    private void putActions() {
        gameActions.put("", "");
        gameActions.put("MOVE", "[\"playerID\", \"position1\", \"positionN\"]");
        gameActions.put("THRW", "[\"playerID\", \"coordX\", \"coordY\"]");
        gameActions.put("BLCK", "[\"playerID\", \"defenderID\"]");
        gameActions.put("PUSH", "[\"playerID\", \"defenderID\"]");
        gameActions.put("SPCL", "[\"playerID\", \"action\", \"param1\", \"paramN\"]");
        gameActions.put("SET TEAM POLARREGION", "[\"ICECOLD&space;BEASTS\", \"PolarRegion\", \"Penguin\", \"Puffin\", \"ArcticHare\", \"PolarBear\", \"Reindeer\", \"Seacow\",\"Penguin\",\"Puffin\",\"Polarbear\",\"Reindeer\"]");
        gameActions.put("SET TEAM SAVANNA", "[\"DUST-DRY&space;BEASTS\", \"Savanna\", \"Zebra\", \"Pelican\", \"Giraffe\", \"Rhino\", \"Elephant\",\"Zebra\",\"Pelican\", \"Giraffe\", \"Rhino\", \"Elephant\"]");
        gameActions.put("SET TEAM APES", "[\"FUCKIN&space;BEASTS\", \"Apes\", \"Gorilla\", \"BlackCappedSquirrelMonkey\", \"Chimpanzee\", \"Orangutan\", \"HowlerMonkey\", \"Marsupilami\",\"Gorilla\", \"BlackCappedSquirrelMonkey\", \"Chimpanzee\", \"Orangutan\"]");
        gameActions.put("SET PLAYER", "[\"playerID\", \"coordX\", \"coordY\"]");
        gameActions.put("KICK", "[\"coordX\", \"coordY\"]");
        gameActions.put("GIVE BALL", "[\"playerID\"]");
        gameActions.put("COOL LINEUP RIGHT", "");
        gameActions.put("COOL LINEUP LEFT", "");
    }

    /**
     * Add game actions and their descriptions to the game actions map
     */
    private void putEvents() {
        gameEvents.put("", "");
        gameEvents.put("ALL PLAYERS SET", "");
        gameEvents.put(SBProtocolMessage.EVENT_END_TURN, "");
    }

    /**
     * Add game actions and their descriptions to the game actions map
     */
    private void putAnswers() {
        gameAnswers.put("SUC WORKD", "MIDcopyfromabove [\"param1\"]");
        gameAnswers.put("FAI FAILD", "MIDcopyfromabove [\"param1\"]");
    }

    public String getGameActionBoxSelected() {
        return (String) gameActionBox.getSelectedItem();
    }

    public String getGameEventBoxSelected() {
        return (String) gameEventBox.getSelectedItem();
    }

    public String getGameAnswerBoxSelected() {
        return (String) gameAnswerBox.getSelectedItem();
    }

    public String getGameActionOrEventFieldText() {
        return gameActionOrEventField.getText();
    }

    public Client getClient() {
        return clientFrame.getClient();
    }

    public ClientFrame getClientFrame() {
        return clientFrame;
    }

    /**
     * Set the pitch for the pitch canvas to draw.
     * @param pitch The pitch to draw on the pitch canvas.
     */
    public void setPitchOnCanvas(Pitch pitch) {
        gameFrame.setPitchOnCanvas(pitch);
    }

    public void setCanKickOff(boolean canKickOff) {
        gameFrame.setCanKickOff(canKickOff);
    }

    public void setIsYourTurn(boolean yourTurn) {
        gameFrame.setIsYourTurn(yourTurn);
    }

    public void setCanSetUp(boolean canSetUp) {
        gameFrame.setCanSetUp(canSetUp);
    }

    public void setHasSetUpTeam(boolean hasSetUpTeam) {
        gameFrame.setHasSetUpTeam(hasSetUpTeam);
    }

    public void setHasSentChooseTeam(boolean hasSentChooseTeam) {
        gameFrame.setHasSentChooseTeam(hasSentChooseTeam);
    }

    public void setChoseTeam(boolean choseTeam) {
        gameFrame.setChoseTeam(choseTeam);
    }

    public void setCanGiveBall(boolean canGiveBall) {
        gameFrame.setCanGiveBall(canGiveBall);
    }
    
    // API

    public void setHighlightAPIPositionsAndColors(Vector2d[] fields, Color[] colors){
        if(fields.length == colors.length)
            gameFrame.getGameCanvas().setHighlightAPIPositionsAndFields(fields, colors);
        else
            getClient().log(Level.SEVERE, "Highlight API: fields and colours length do not match!");
    }

    public void clearHighlightAPIPositionsAndColors() {
        gameFrame.getGameCanvas().setHighlightAPIPositionsAndFields(null, null);
    }

    public void showMe(String player, String showMe) {
        gameFrame.getGameCanvas().showMe(player, showMe);
    }
    
    public void setAPIMessage(SBProtocolMessage message) {
        gameFrame.getGameCanvas().setAPIMessage(message);
    }

    public void setDiceAPI(boolean set) {
        gameFrame.getGameCanvas().setDiceAPI(set);
    }

    public void setDiceAPIDice(int[] dice){
        gameFrame.getGameCanvas().setDiceAPIDice(dice);
    }

    public void setAimAPI(boolean set){
    	gameFrame.getGameCanvas().setAimAPI(set);
    }

    public void setAimAPIIndex(int index){
        gameFrame.getGameCanvas().setAimAPIIndex(index);
    }

    public void setAimAPIDistance(int distance){
        gameFrame.getGameCanvas().setAimAPIDistance(distance);
    }
    
    public void setChoiceAPI(boolean set){
    	gameFrame.getGameCanvas().setChoiceAPI(set);
    }

    public void setChoiceAPIPlayers(Player[] players){
        gameFrame.getGameCanvas().setChoiceAPIPlayers(players);
    }

	public void setFieldAPI(boolean set){
		gameFrame.getGameCanvas().setFieldAPI(set);
	}

	public void setFieldAPIFields(Vector2d[] fields){
        gameFrame.getGameCanvas().setFieldAPIFields(fields);
	}
}
