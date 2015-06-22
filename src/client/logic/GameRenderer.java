package client.logic;

import GUI.SBColor;
import client.Client;
import client.display.GameCanvas;
import client.display.GameFrame;
import gameLogic.*;
import gameLogic.dice.BlockDie;
import gameLogic.rules.RuleThrow;
import gameLogic.rules.SpecialRule;
import util.ResourceManager;

import javax.vecmath.Vector2d;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;

/**
 * The Engine calculating rendering.
 */
public class GameRenderer {

    private static final int MAX_IMAGE_BUFFER_SIZE = 200,
                             STANDARD_BEACON_ALPHA = 30,
                             DIE_PADDING = 5;
    public static final String CURRENCY_SYMBOL = "₪";

    GameCanvas gameCanvas;

    public double vX = 500, vY = 2220, vZ = 1590, dY = 590, pZ = 0;

    private volatile HashMap<Integer, HashMap<Double, BufferedImage>> imageBuffer = new HashMap<Integer, HashMap<Double, BufferedImage>>();
    private volatile HashMap<Integer, HashMap<Integer, BufferedImage>> imageFilterBuffer = new HashMap<Integer, HashMap<Integer, BufferedImage>>();

    private int aimingFieldDistanceClass = 0;
    private Weather weather = null;
    private int[][] weatherParticles;
    private int[] weatherParticleSizes, weatherParticleVelocity;

    public GameRenderer(GameCanvas gameCanvas) {
        this.gameCanvas = gameCanvas;
    }

    public void drawPitch(Graphics2D g) {
        if(g != null) {
            g.setStroke(new BasicStroke((int) (getPW()/24), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            for (int x = 0; x < getGameCanvas().getPitch().getFields().length; x++) {
//                for (int y = 0; y < getGameCanvas().getPitch().getFields()[x].length; y++) {
//                    PitchField field = getGameCanvas().getPitch().getFields()[x][y];
//                    if (field != null) {
                        // draw background
//                        if(x == 0 || x == 25) g.setPaint(SBColor.GREEN_DARK);
//                        else g.setPaint(SBColor.GREEN_BRIGHT);
//                        fillRectangleOnPitch(g, x * getPW(), y * getPW(), getPW(), getPW());
//                    }
//                }
                // draw vertical lines
                g.setPaint(SBColor.WHITE);
                drawLineOnPitch(g, x * getPW(), 0, x * getPW(), getPitchHeight());
            }
            // draw last vertical line
            g.setPaint(SBColor.WHITE);
            drawLineOnPitch(g, 26 * getPW(), 0, 26 * getPW(), getPitchHeight());
            // draw horizontal lines
            for (int y = 0; y < getGameCanvas().getPitch().getFields()[0].length+1; y++)
                drawLineOnPitch(g, 0, y * getPW(), getPitchWidth(), y * getPW());
            // draw extra stuff
            g.setStroke(new BasicStroke((int) (getPW() / 12), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            drawLineOnPitch(g, getPitchWidth() / 2, 0, getPitchWidth() / 2, getPitchHeight());
            drawLineOnPitch(g, 0, 4 * getPW(), getPitchWidth(), 4 * getPW());
            drawLineOnPitch(g, 0, 11 * getPW(), getPitchWidth(), 11 * getPW());
        }
    }

    public void drawPlayers(Graphics2D g) {
        Pitch pitch = getGameCanvas().getPitch();
        PitchField[][] fields = pitch.getFields();
        int ballX = (int) pitch.getBallPos().x,
            ballY = (int) pitch.getBallPos().y;
        for (int x = 0; x < fields.length; x++) {
            for (int y = 0; y < fields[x].length; y++) {
                if(fields[x][y] != null) {
                    // draw player
                    Player player = fields[x][y].getPlayer();
                    if(player != null) drawPlayer(g, player);
                    // draw ball
                    if(x == ballX && y == ballY) drawBall(g, ballX, ballY);
                }
            }
        }
    }

    public void drawPlayer(Graphics2D g, Player player) {
        boolean left = player.getTeam().equals(player.getMatch().getTeam(0));

        if(getGameCanvas().isYourTurn()
                && getGameCanvas().getGamePhase() < 4
                && (player.$GUIgetRemainingBe() > 0 || player.isHoldingBall() && player.getTeam().getPass())
                && isOwnPlayer(player)
                && getGameCanvas().getGamePhase() >= 3
                && !(
                    player != getPitch().getTeam(getGameCanvas().getClientIndex()).getMovingPlayer()
                    && player.$GUIgetRemainingBe() == 0
                )) {

            highlightFieldEdges(g, player.getPos().x, player.getPos().y, SBColor.GREEN_DARK);

        }

        BufferedImage sprite;
        if (left) sprite = player.getSpriteR();
        else sprite = player.getSpriteL();
        if (sprite == null) sprite = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);

        double  gap = (ResourceManager.IMAGE_WIDTH - ResourceManager.PEDESTAL_WIDTH)/2 * ((Pitch.PITCH_WIDTH+1)/(player.getPos().y+1)/20 + 1),
                tx = player.getPos().x * ResourceManager.PEDESTAL_WIDTH - gap,
                ty = player.getPos().y * ResourceManager.PEDESTAL_WIDTH;// + ResourceManager.PEDESTAL_WIDTH - ResourceManager.IMAGE_HEIGHT;
        if(left) tx -= gap;

        float[] scaleFactors = new float[]{1, 1, 1, 1};
        if(getGameCanvas().getClientIndex() != (player.getTeam().equals(player.getMatch().getTeam(0)) ? 0 : 1)) scaleFactors = new float[]{0.8f, 0.8f, 0.8f, 1};
        RescaleOp rescaleOp = new RescaleOp(scaleFactors, new float[4], null);

        drawImageWithOp(g, sprite, rescaleOp, tx, ty, getT(), 0, true);

        if(player.$GUIgetPlayerCondition() == PlayerCondition.PRONE || player.$GUIgetPlayerCondition() == PlayerCondition.STUNNED) { // draw star if proned
            drawImageWithOp(
                    g, ResourceManager.PROP_STAR, null,
                    -(left ? 0 : ResourceManager.IMAGE_WIDTH/2)+tx+ResourceManager.IMAGE_WIDTH-(double) ResourceManager.PROP_STAR.getWidth(), ty + ResourceManager.IMAGE_HEIGHT/4,
                    getT(), 0, true);
        }
        if(player.$GUIgetPlayerCondition() == PlayerCondition.STUNNED) { // draw second star if stunned
            drawImageWithOp(
                    g, ResourceManager.PROP_STAR, null,
                    -(left ? 0 : ResourceManager.IMAGE_WIDTH/2)+tx+ResourceManager.IMAGE_WIDTH-3*(double) ResourceManager.PROP_STAR.getWidth()/2, ty + ResourceManager.IMAGE_HEIGHT/4,
                    getT(), 0, true);
        }

        // if this player holds the ball, draw it right in front of it
        double  ballX = getPitch().getBallPos().x,
                ballY = getPitch().getBallPos().y;
        if(player.getTeam().getMatch().getPitch().getBallPos().equals(player.getPos())) drawBall(g, ballX, ballY);
    }

    public void drawPlayersBench(Graphics2D g) {

        double  playersInRow = Team.MAX_TEAM_SIZE / 2,
                seatsInRow = playersInRow + 2,
                playerScale = getPitchWidth() / 2 / seatsInRow / ResourceManager.IMAGE_WIDTH;
        int playersOffBench = 0;

        // set players on bench
        getGameCanvas().getPlayersOnBench().removeAllElements();
        getGameCanvas().getPlayersOnOpponentBench().removeAllElements();
        for (Player player : getClient().getMatch().getTeam(getGameCanvas().getClientIndex()).getPlayers())
            if (player.getPosition().equals(Pitch.THE_VOID)) // player is outside of field
                getGameCanvas().addPlayerOnBench(player);
        for (Player player : getClient().getMatch().getTeam(getGameCanvas().getClientIndex() == 0 ? 1 : 0).getPlayers())
            if (player.getPosition().equals(Pitch.THE_VOID)) // player is outside of field
                getGameCanvas().addPlayerOnOpponentBench(player);

        // draw players on own bench
        for (int i = 1; i <= getGameCanvas().getPlayersOnBench().size(); i++) {
            Player playerToDraw = getGameCanvas().getPlayersOnBench().get(i-1);

            if(playerToDraw.$GUIisKOInjuredOrDead())
                playersOffBench++;

            drawPlayerOnOwnBench(g, playerToDraw, playersInRow, seatsInRow, i-playersOffBench, playerScale);
        }

        // draw players on opponent bench
        playersOffBench = 0;
        for (int i = 1; i <= getGameCanvas().getPlayersOnOpponentBench().size(); i++) {
            Player playerToDraw = getGameCanvas().getPlayersOnOpponentBench().get(i-1);

            if(playerToDraw.$GUIisKOInjuredOrDead())
                playersOffBench++;

            drawPlayerOnOpponentBench(g, playerToDraw, playersInRow, seatsInRow, i-playersOffBench, playerScale);
        }

    }

    private void drawPlayerOnOwnBench(Graphics2D g, Player player, double playersInRow, double seatsInRow, int i, double scale) {
        BufferedImage sprite = isLeft() ? player.getSpriteR() : player.getSpriteL();

        if(player.$GUIisKOInjuredOrDead()) { // draw players in hospital or on graveyard

            drawInjuredOrDeadPlayer(g, player, sprite, isLeft(), scale);

        } else {

            double tx, ty = getPitchHeight()/scale + 2*ResourceManager.IMAGE_HEIGHT/5;
            if(i <= playersInRow) tx = (isLeft() ? i : i+seatsInRow) * ResourceManager.IMAGE_WIDTH;
            else {
                tx = (isLeft() ? i-playersInRow : i+seatsInRow-playersInRow) * ResourceManager.IMAGE_WIDTH;
                ty += ResourceManager.IMAGE_HEIGHT;
            }

            drawImageWithOp(g, sprite, null, tx, ty, scale, 0, false); // draw on bench
            if(player.getRedCard()) // draw red card if player was expelled from match
                drawImageWithOp(g, ResourceManager.PROP_RED_CARD, null, tx, ty, scale, 0, false);
            if(player.$GUIgetPlayerCondition() == PlayerCondition.PRONE || player.$GUIgetPlayerCondition() == PlayerCondition.STUNNED) { // draw star if proned
                drawImageWithOp(
                        g, ResourceManager.PROP_STAR, null,
                        tx+ResourceManager.IMAGE_WIDTH, ty + 3*ResourceManager.IMAGE_HEIGHT/4,
                        getT(), 0, true);
            }
            if(player.$GUIgetPlayerCondition() == PlayerCondition.STUNNED) { // draw second star if stunned
                drawImageWithOp(
                        g, ResourceManager.PROP_STAR, null,
                        tx+ResourceManager.IMAGE_WIDTH-(double) ResourceManager.PROP_STAR.getWidth()/2, ty + 3*ResourceManager.IMAGE_HEIGHT/4,
                        getT(), 0, true);
            }

        }
    }

    private void drawPlayerOnOpponentBench(Graphics2D g, Player player, double playersInRow, double seatsInRow, int i, double scale) {
        BufferedImage sprite = isLeft() ? player.getSpriteL() : player.getSpriteR();

        if(player.$GUIisKOInjuredOrDead()) { // draw players in hospital or on graveyard

            drawInjuredOrDeadPlayer(g, player, sprite, !isLeft(), scale);

        } else {

            double tx, ty = getPitchHeight()/scale + 2*ResourceManager.IMAGE_HEIGHT/5;
            if(i <= playersInRow) tx = (isLeft() ? i+seatsInRow : i) * ResourceManager.IMAGE_WIDTH;
            else {
                tx = (isLeft() ? i+seatsInRow-playersInRow : i-playersInRow) * ResourceManager.IMAGE_WIDTH;
                ty += ResourceManager.IMAGE_HEIGHT;
            }

            drawImageWithOp(g, sprite, new RescaleOp(new float[]{0.8f, 0.8f, 0.8f, 1}, new float[4], null), tx, ty, scale, 0, false); // draw on bench
            if(player.getRedCard()) // draw red card if player was expelled from match
                drawImageWithOp(g, ResourceManager.PROP_RED_CARD, null, tx, ty, scale, 0, false);
            if(player.$GUIgetPlayerCondition() == PlayerCondition.PRONE || player.$GUIgetPlayerCondition() == PlayerCondition.STUNNED) { // draw star if proned
                drawImageWithOp(
                        g, ResourceManager.PROP_STAR, null,
                        tx+ResourceManager.IMAGE_WIDTH, ty + 3*ResourceManager.IMAGE_HEIGHT/4,
                        getT(), 0, true);
            }
            if(player.$GUIgetPlayerCondition() == PlayerCondition.STUNNED) { // draw second star if stunned
                drawImageWithOp(
                        g, ResourceManager.PROP_STAR, null,
                        tx+ResourceManager.IMAGE_WIDTH-(double) ResourceManager.PROP_STAR.getWidth()/2, ty + 3*ResourceManager.IMAGE_HEIGHT/4,
                        getT(), 0, true);
            }

        }
    }

    private void drawInjuredOrDeadPlayer(Graphics2D g, Player player, BufferedImage sprite, boolean left, double scale) {
        double  pitchWidthHalved = getPitchWidth() / 2 / scale,
                actionFieldSize = getPitchHeight() / 4 / scale,
                seatWidth = pitchWidthHalved / (Team.MAX_TEAM_SIZE / 2 + 2),
                graveyardAndHospitalWidth = pitchWidthHalved - seatWidth - actionFieldSize,
                hospitalWidth = 5*graveyardAndHospitalWidth/9, // don't change this ratio because of drawn graveyard and hospital!
                tx, ty;

        int     addX = (new Random(player.hashCode()*player.getId())).nextInt((int) (actionFieldSize - ResourceManager.IMAGE_WIDTH/2)),
                addY = (new Random(player.hashCode()*player.getId())).nextInt((int) (actionFieldSize));
        ty = getPitchHeight()/scale + actionFieldSize + addY/2 + ResourceManager.IMAGE_HEIGHT*scale*4;

        if(player.$GUIgetPlayerCondition() == PlayerCondition.INJURED
                || player.$GUIgetPlayerCondition() == PlayerCondition.KO) { // player is injured or ko

            if(left) tx = actionFieldSize + addX - ResourceManager.IMAGE_WIDTH*scale*2;
            else tx = getW()/scale - actionFieldSize - hospitalWidth + addX - ResourceManager.IMAGE_WIDTH*scale*2;

            drawImageWithOp(g, sprite, null, tx, ty, scale, 0, false);
            BufferedImage bandAid = player.$GUIgetPlayerCondition() == PlayerCondition.INJURED ? ResourceManager.PROP_BAND_AID_DOUBLE : ResourceManager.PROP_BAND_AID;
            drawImageWithOp(g, bandAid, null, tx + bandAid.getWidth() / 2, ty + ResourceManager.IMAGE_HEIGHT - bandAid.getHeight(), scale, 0, false);

        } else { // player is dead

            if(left) tx = actionFieldSize + hospitalWidth + addX - ResourceManager.IMAGE_WIDTH*scale*4;
            else tx = getW()/scale - actionFieldSize - graveyardAndHospitalWidth + addX - ResourceManager.IMAGE_WIDTH*scale*4;

            drawImageWithOp(g, sprite, null, tx, ty, scale, 0, false);

        }

    }

    public void drawGUI(Graphics2D g) {
        BufferedImage background = ResourceManager.BACKGROUND;
        double scale = getW()/background.getWidth();
        background = scaleImage(background, background.hashCode(), scale);
        g.drawImage(background, null, 0, 0);
    }

    public void drawTooltip(Graphics2D g, Player player, int[] mXY) {
        if(getGameCanvas().isChoosingSpecialRule())
            player = getGameCanvas().getPlayerChoosingSpecialRuleFor();

        Font    textFont = new Font("SansSerif", Font.PLAIN, (int) (12*10*getT())),
                textFontSmall = new Font("Monospaced", Font.PLAIN, (int) (9*10*getT())),
                textFontStats = new Font("Monospaced", Font.PLAIN, (int) (12*10*getT())),
                textFontBold = new Font("SansSerif", Font.BOLD, (int) (12*10*getT())),
                textFontBig = new Font("SansSerif", Font.PLAIN, (int) (14*10*getT())),
                textFontBigBold = new Font("SansSerif", Font.BOLD, (int) (14*10*getT()));
        FontMetrics metrics = g.getFontMetrics(textFont),
                    metricsSmall = g.getFontMetrics(textFontSmall),
                    metricsStats = g.getFontMetrics(textFontStats),
                    metricsBold = g.getFontMetrics(textFontBold),
                    metricsBig = g.getFontMetrics(textFontBig),
                    metricsBigBold = g.getFontMetrics(textFontBigBold);
        float padding = (int) (75*getT()),
              wN = metricsBold.stringWidth(player.toString()),
              hN = metricsBold.getHeight(),
              hD = metricsSmall.getHeight(),
              hS = metricsStats.getHeight(),
              hO = metrics.getHeight(),
              hR = metricsBig.getHeight(),
              corr = 0,
              width = wN;

        if(getGameCanvas().isChoosingSpecialRule()) { // special rules menu

            int[] pos = getGameCanvas().getSpecialRuleMenuPosition();
            int hovering = getGameCanvas().getPitchMouseLogic().getSpecialRuleChoiceHovering();
            ArrayList<String> specialRules = new ArrayList<String>();
            for(SpecialRule specialRule : player.getSpecialRules())
                specialRules.add("◆ " + specialRule.getName() + " ◆");
            for(int i = 0; i < specialRules.size(); i++) {
                int w;
                if(hovering == i) w = metricsBigBold.stringWidth(specialRules.get(i));
                else w = metricsBig.stringWidth(specialRules.get(i));
                if(w > width) width = w;
            }
            width += 2 * padding;

            if(getW() - pos[0] - width < 0) corr = width;

            g.setPaint(SBColor.BLACK_180);
            float height = 2 * padding + hN + specialRules.size() * (padding + hR);
            g.fillRect((int) (pos[0] + padding / 2 - corr), (int) (pos[1] + padding / 2), (int) width, (int) height);

            g.setPaint(SBColor.WHITE);
            g.setFont(textFontBold);
            g.drawString(player.toString(), pos[0] + 3 * padding / 2 - corr, pos[1] + padding + hN);

            for(int i = 0; i < specialRules.size(); i++) {
                float dy = pos[1] + padding + 2 * hN + (i + 1) * padding + i * hR;
                if(hovering == i) g.setFont(textFontBigBold);
                else g.setFont(textFontBig);

                g.drawString(specialRules.get(i), pos[0] + 3 * padding / 2 - corr, dy);
            }

            // set sizes for mouse listeners
            getGameCanvas().setSpecialRuleMenuSize(new int[]{(int) width, (int) height});
            getGameCanvas().setSpecialRuleMenuNameHeight((int) (padding + 2 * hN));
            getGameCanvas().setSpecialRuleMenuItemHeight((int) (padding + hR));

        } else { // normal tooltip

            String[] description = player.$GUIgetDescriptionLines();
            for(String string : description) {
                int w = metricsSmall.stringWidth(string);
                if(w > width) width = w;
            }

            String[] stats = new String[]{
                    "MA: " + player.$GUIgetRemainingBe() + "/" + player.$GUIgetBe() + "  ST: " + player.$GUIgetSt(),
                    "AG: " + player.$GUIgetGe() + "    AV: " + player.$GUIgetRs()};
            for(String string : stats) {
                int w = metricsStats.stringWidth(string);
                if(w > width) width = w;
            }

            ArrayList<String> other = new ArrayList<String>();
            if(player.getRedCard()) other.add("Banned from game!");
            other.add(player.$GUIgetPlayerCondition().randomConditionDescription(player));
            for(SpecialRule specialRule : player.getSpecialRules())
                other.add("◆ Can " + specialRule.getName());
            if(player.isHoldingBall()) {
                if(player.getTeam().getPass()) other.add("I wanna toss this ball!");
                else other.add("I can't pass this turn.");
            }
            for(String string : other) {
                int w = metrics.stringWidth(string);
                if(w > width) width = w;
            }

            width += 2 * padding;
            if(getW() - mXY[0] - width < 0) corr = width;

            g.setPaint(SBColor.BLACK_180);
            float height = 2 * padding + hN + description.length * hD + stats.length * (padding + hS) + other.size() * (padding + hO);
            g.fillRect((int) (mXY[0] + padding / 2 - corr), (int) (mXY[1] + padding / 2), (int) width, (int) height);

            g.setPaint(SBColor.WHITE);
            g.setFont(textFontBold);
            g.drawString(player.toString(), mXY[0] + 3 * padding / 2 - corr, mXY[1] + padding + hN);

            g.setFont(textFontSmall);
            for(int i = 0; i < description.length; i++) {
                float dy = mXY[1] + padding + 2 * hN + i * hD;
                g.drawString(description[i], mXY[0] + 3 * padding / 2 - corr, dy);
            }

            g.setFont(textFontStats);
            for(int i = 0; i < stats.length; i++) {
                float dy = mXY[1] + padding + 2 * hN + description.length * hD + (i + 1) * padding + i * hS;
                g.drawString(stats[i], mXY[0] + 3 * padding / 2 - corr, dy);
            }

            g.setFont(textFont);
            for(int i = 0; i < other.size(); i++) {
                float dy = mXY[1] + padding + 2 * hN + description.length * hD + (stats.length + 1) * padding + stats.length * hS + i * padding + i * hO;
                g.drawString(other.get(i), mXY[0] + 3 * padding / 2 - corr, dy);
            }
        }
    }

    public void drawMovingPlayer(Graphics2D g) {
        if(getGameCanvas().getMovingPlayer() != null) {
            AffineTransform transform = new AffineTransform();
            double  tx = getGameCanvas().getStoredMousePosition()[0] / getT(),
                    ty = getGameCanvas().getStoredMousePosition()[1] / getT();

            BufferedImage sprite;
            if(isLeft()) sprite = getGameCanvas().getMovingPlayer().getSpriteR();
            else sprite = getGameCanvas().getMovingPlayer().getSpriteL();
            if(sprite == null) sprite = new BufferedImage(100, 200, BufferedImage.TYPE_INT_ARGB);

            transform.scale(getT(), getT());
            transform.translate(tx, ty);
            double rotation = -(getGameCanvas().getGameFrame().getOldMousePositions().get(0)[0]-getGameCanvas().getGameFrame().getOldMousePositions().get(1)[0]) / getPW();
            if(rotation > GameFrame.MAX_DRAG_ROTATION) rotation = GameFrame.MAX_DRAG_ROTATION;
            else if(rotation < -GameFrame.MAX_DRAG_ROTATION) rotation = -GameFrame.MAX_DRAG_ROTATION;
            transform.rotate(rotation);
            g.transform(transform);

            g.drawImage(sprite, new RescaleOp(new float[]{0.8f, 0.8f, 0.8f, 0.6f}, new float[4], null), -ResourceManager.IMAGE_WIDTH / 2, 0);

            try { g.transform(transform.createInverse()); }
            catch (NoninvertibleTransformException ignored) {}
        }
    }

    public void drawSetPlayer(Graphics2D g) {
        drawMovingPlayer(g);

        if(getGameCanvas().getMovingPlayer() != null && getGameCanvas().getFieldAimingAt() != null) {

            if(getGameCanvas().getFieldAimingAt().getPlayer() == null) { // there is nobody on this field yet
                highlightField(g,
                        getPitchMouseLogic().getFieldAimingAt().getPos().x,
                        getPitchMouseLogic().getFieldAimingAt().getPos().y,
                        SBColor.YELLOW_80);
            }

        }
    }

    public void drawBall(Graphics2D g, double x, double y) {
        if(getPitch()!= null) {
            if(x >= 0 && x < Pitch.PITCH_LENGTH && y >= 0 && y < Pitch.PITCH_WIDTH) {
                double  tx = x * ResourceManager.PEDESTAL_WIDTH * 2,
                        ty = y * ResourceManager.PEDESTAL_WIDTH * 2;
                // draw beacon
                drawBeacon(g, x, y, SBColor.BLUE_BRIGHT);
                // draw ball
                Player playerWithBall = getPitch().getFields()[(int) x][(int) y].getPlayer();
                if(playerWithBall == null) { // no player has the ball
                    tx += ResourceManager.PEDESTAL_WIDTH/4;
                    ty += 3*ResourceManager.PEDESTAL_WIDTH/2;
                } else { // a player has the ball
                    boolean left = playerWithBall.getTeam().equals(getPitch().getTeam(0));
                    tx = left ? tx + ResourceManager.PEDESTAL_WIDTH/2 : tx - ResourceManager.PEDESTAL_WIDTH/8;
                    ty += ResourceManager.PEDESTAL_WIDTH/2;
                }
                drawImageWithOp(g, ResourceManager.PROP_FOOTBALL, null, tx, ty, getT() / 2, 0, true);
            }
        }
    }

    public void drawPath(Graphics2D g) {
        // draw path
        if(getPitchMouseLogic().getDrawingPath() != null) {
            for (int i = 1; i < getPitchMouseLogic().getDrawingPath().getPath().size(); i++) {
                PitchField field = getPitchMouseLogic().getDrawingPath().getPath().get(i);
                if(field != null) {
                    highlightField(g, field.getPos().x, field.getPos().y, SBColor.YELLOW_80);
                }
            }
        }

        // draw blitz field
        if(getPitchMouseLogic().getFieldBlitzing() != null) {
            highlightField(g,
                    getPitchMouseLogic().getFieldBlitzing().getPos().x,
                    getPitchMouseLogic().getFieldBlitzing().getPos().y,
                    SBColor.BLACK_60);
        }
    }

    public void drawFieldsReachable(Graphics2D g) {
        Player aimingPlayer = getPitchMouseLogic().getAimingPlayer();

        if(getPitchMouseLogic().getFieldAimingAt() != null && aimingPlayer != null) {
            if(!getPitchMouseLogic().isAimingKickoff()) { // only draw radii and color differently for normal passes

                double  mX = getPitchMouseLogic().getMXY()[0],
                        mY = getPitchMouseLogic().getMXY()[1];
                Color paint;

                for(int y = 0; y < Pitch.PITCH_WIDTH; y++) {
                    for(int x = 0; x < Pitch.PITCH_LENGTH; x++) {
                        PitchField field = getPitch().getFields()[x][y];
                        if(field != null) {

                            Vector2d dist = (Vector2d) aimingPlayer.getPos().clone();
                            dist.sub(field.getPos());

                            paint = SBColor.YELLOW_80;
                            if (dist.length() > RuleThrow.SHORT_PASS) paint = SBColor.ORANGE_BRIGHT_80;
                            if (dist.length() > RuleThrow.LONG_PASS) paint = SBColor.ORANGE_DARK_80;
                            if (dist.length() > RuleThrow.LONG_BOMB) paint = SBColor.RED_80;

                            if(dist.length() <= RuleThrow.LONG_BOMB)
                                highlightField(g, field.getPos().x, field.getPos().y, paint);

                        }
                    }
                }

                // field
                Vector2d dist = (Vector2d) aimingPlayer.getPos().clone();
                dist.sub(new Vector2d(mX, mY));

                paint = SBColor.YELLOW_80;
                aimingFieldDistanceClass = 0;
                if (dist.length() > RuleThrow.SHORT_PASS) {
                    paint = SBColor.ORANGE_BRIGHT_80;
                    aimingFieldDistanceClass = 1;
                }
                if (dist.length() > RuleThrow.LONG_PASS) {
                    paint = SBColor.ORANGE_DARK_80;
                    aimingFieldDistanceClass = 2;
                }
                if (dist.length() > RuleThrow.LONG_BOMB) {
                    paint = SBColor.RED_80;
                    aimingFieldDistanceClass = 3;
                }

                if(getPitchMouseLogic().getFieldAimingAt() != null)
                    highlightField(g,
                            getPitchMouseLogic().getFieldAimingAt().getPos().x,
                            getPitchMouseLogic().getFieldAimingAt().getPos().y,
                            paint);

            }
        }
    }

    public void drawFieldAimingAt(Graphics2D g) {
        Player aimingPlayer = getPitchMouseLogic().getAimingPlayer();

        if(getPitchMouseLogic().getFieldAimingAt() != null && aimingPlayer != null) {
            Color paint;

            if(getPitchMouseLogic().isAimingKickoff()) {
                highlightField(g,
                        getPitchMouseLogic().getFieldAimingAt().getPos().x,
                        getPitchMouseLogic().getFieldAimingAt().getPos().y,
                        SBColor.YELLOW_80);
                paint = SBColor.YELLOW;
            } else  { // only check if curve is drawn for normal passes
                // curve
                paint = SBColor.YELLOW;
                if (aimingFieldDistanceClass == 1) paint = SBColor.ORANGE_BRIGHT;
                if (aimingFieldDistanceClass == 2) paint = SBColor.ORANGE_DARK;
                if (aimingFieldDistanceClass == 3) paint = SBColor.RED;
            }

            Vector2d aimingPlayerPos = getPitchMouseLogic().getAimingPlayer().getPos();

            if(aimingPlayerPos != null) {
                drawCurveToMouse(g, aimingPlayerPos, paint);
            }

        }
    }

    public void drawGiveBall(Graphics2D g) {
        if(getGameCanvas().canGiveBall()) { // only draw if can give ball
            if(getPitchMouseLogic().getHoveringPlayer() != null) {
                double x = getPitchMouseLogic().getHoveringPlayer().getPos().x, y = getPitchMouseLogic().getHoveringPlayer().getPos().y;

                g.setStroke(new BasicStroke((int) (getPW() / 36), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                // draw hovering field
                g.setPaint(SBColor.YELLOW_80);
                highlightField(g, x, y, SBColor.YELLOW_80);
                // draw ball on hovering field
                drawBall(g, x, y);
            }
        }
    }

    public void drawFrameRate(Graphics2D g) {
        g.setPaint(SBColor.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * 10* getT())));
        g.drawString(getGameCanvas().frameRate+"", (int) (18 * getT()), (int) (288 * getT()));
    }

    public void drawPositionMarker(Graphics2D g) {
        double[] mXY = getPitchMouseLogic().getMXY();
        int mX = (int) mXY[0], mY = (int) mXY[1];

        String pos = mX + ", " + mY;
        if(mX < 0) pos = "0, " + mY;
        if(mY < 0) pos = mX + ", 0" ;
        if(mX < 0 && mY < 0) pos = "0, 0";

        if(getPitchMouseLogic().getHoveringPlayer() != null) {
            pos += " – " + getPitchMouseLogic().getHoveringPlayer().getName() + " " + getPitchMouseLogic().getHoveringPlayer().getId();
        }
        if(getGameCanvas().getMovingPlayer() != null) {
            pos += " – " + "Moving " + getGameCanvas().getMovingPlayer().getName() + " " + getGameCanvas().getMovingPlayer().getId();
        }
        if(getPitchMouseLogic().getAimingPlayer() != null && !getPitchMouseLogic().isAimingKickoff()) {
            pos += " – aiming";
        }
        g.setPaint(SBColor.WHITE);
        g.setFont(new Font("Monospaced" , Font.PLAIN, (int) (12 * 10* getT())));
        g.drawString(pos, (int) (18 * getT()), (int) (138 * getT()));
    }

    public void drawScore(Graphics2D g) {
        try {
            String score1 = getPitch().getTeam(0).getMatch().getScoreFromTeam(0) + "", score2 = getPitch().getTeam(1).getMatch().getScoreFromTeam(1) + "",
                    name1 = getPitch().getTeam(0).getName(), name2 = getPitch().getTeam(1).getName(),
                    divider = ":";

            Font textFontBold = new Font("Monospaced", Font.BOLD, (int) (12*10*getT()));
            FontMetrics metrics = g.getFontMetrics(textFontBold);
            int wN1 = metrics.stringWidth(name1);
            int wS1 = metrics.stringWidth(score1);
            int wS2 = metrics.stringWidth(score2);
            int wD = metrics.stringWidth(divider);

            g.setPaint(SBColor.WHITE);
            g.setFont(textFontBold);
            g.drawString(divider, (int) (getPitchWidth() / 2 - wD / 2), (int) (138 * getT()));
            g.drawString(score1, (int) (getPitchWidth() / 2 - wS1 - wD / 2), (int) (138 * getT()));
            g.drawString(score2, (int) (getPitchWidth() / 2 + wD / 2), (int) (138 * getT()));
            g.drawString(name1, (int) (getPitchWidth() / 2 - wN1 - wS1 - 2 * wD), (int) (138 * getT()));
            g.drawString(name2, (int) (getPitchWidth() / 2 + wS2 + 2*wD), (int) (138 * getT()));
        } catch(NullPointerException ignored) {}
    }

    public void drawRoundCount(Graphics2D g) {
        try {
            String count = "Round " + ((getPitch().getTeam(0).getMatch().getRoundCount()+2)/2);

            Font textFontBold = new Font("Monospaced", Font.BOLD, (int) (12*10*getT()));
            FontMetrics metrics = g.getFontMetrics(textFontBold);
            int wC = metrics.stringWidth(count);

            g.setPaint(SBColor.WHITE);
            g.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * 10* getT())));
            g.drawString(count, (int) (getPitchWidth() - wC - 18 * getT()), (int) (138 * getT()));
        } catch(NullPointerException ignored) {}
    }

    public boolean preparedWeather() {
        return weather != null;
    }

    public void prepareWeather(Weather weather, double w, double h, double pitchH) {
        this.weather = weather;

        switch(weather) {
            case SWELTERING_HEAT:

                break;

            case VERY_SUNNY:

                break;

            case NICE:

                break;

            case POURING_RAIN:
                weatherParticles = new int[1000][2];
                for(int i = 0; i < weatherParticles.length; i++) {
                    weatherParticles[i][0] = (int) (Math.random()*w);
                    weatherParticles[i][1] = (int) (Math.random()*h);
                }
                weatherParticleSizes = new int[]{-10, 20};
                weatherParticleVelocity = new int[]{-10, 20};
                break;

            case BLIZZARD:

                break;
        }
    }

    public void drawWeather(Graphics2D g) {
        try {

            String weather = "Weather: " + this.weather.toNiceString();

            Font textFontBold = new Font("Monospaced", Font.BOLD, (int) (12*10*getT()));
            FontMetrics metrics = g.getFontMetrics(textFontBold);
            int wC = metrics.stringWidth(weather);

            g.setPaint(SBColor.WHITE);
            g.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * 10* getT())));
            g.drawString(weather, (int) (getPitchWidth() - wC - 18 * getT()), (int) (288 * getT()));

            drawWeatherEffect(g);

        } catch(NullPointerException ignored) {}
    }

    private void drawWeatherEffect(Graphics2D g) {
        int x, y;

        switch(weather) {
            case SWELTERING_HEAT:

                break;

            case VERY_SUNNY:

                break;

            case NICE:

                break;

            case POURING_RAIN:
                g.setStroke(new BasicStroke((int) (getPW() / 20), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
                g.setPaint(SBColor.BLACK_20);
//                for(int i = 0; i < weatherParticles.length; i++) {
//                    g.drawLine(weatherParticles[i][0], weatherParticles[i][1], weatherParticles[i][0]+weatherParticleSizes[0], weatherParticles[i][1]+weatherParticleSizes[1]);
//                    weatherParticles[i][0] += weatherParticleVelocity[0];
//                    weatherParticles[i][1] += weatherParticleVelocity[1];
//                    if(weatherParticles[i][0] < 0) {
//                        weatherParticles[i][0] = (int) getW();
//                        weatherParticles[i][1] = (int) (Math.random()*getH() + i/getH());
//                    }
//                    if(weatherParticles[i][1] > getH()) {
//                        weatherParticles[i][0] = (int) (Math.random()*getW() + i/getW());
//                        weatherParticles[i][1] = 0;
//                    }
//                }
                for(int i = 0; i < 1000; i++) {
                    x = (int) (Math.random()*getW());
                    y = (int) (Math.random()*getH());
                    g.drawLine(x, y, x-10, y+20);
                }
                break;

            case BLIZZARD:
                g.setPaint(SBColor.WHITE);
                for(int i = 0; i < 500; i++) {
                    x = (int) (Math.random()*getW());
                    y = (int) (Math.random()*getH());
                    g.fillOval(x, y, 8, 5);
                }
                break;

        }
    }

    public void drawGamePhaseInfo(Graphics2D g) {
        String gamePhaseInfo = "";
        switch (getGameCanvas().getGamePhase()) {
            case 0: // Team Choosing Phase
                if(!getGameCanvas().choseTeam()) gamePhaseInfo = "Choose your team";
                else gamePhaseInfo = "Waiting for opponent to choose team";
                if(!getGameCanvas().choseTeamType()) drawChooseTeam(g);
                else drawChoosePlayers(g);
                break;
            case 1: // Team Setup Phase
                if(getGameCanvas().canSetUp() && !getGameCanvas().hasSetUpTeam()) gamePhaseInfo = "Setup your team";
                else gamePhaseInfo = "Waiting for opponent to set up";
                break;
            case 2: // Kick Phase
                if(getGameCanvas().canKickOff()) gamePhaseInfo = "Kick";
                else if(getGameCanvas().canGiveBall()) gamePhaseInfo = "Give ball to player";
                else gamePhaseInfo = "Kickoff: Waiting for opponent";
                break;
            case 3: // Normal Playing Phase
                if(getGameCanvas().isYourTurn()) gamePhaseInfo = "Your turn";
                else gamePhaseInfo = "Opponent turn";
                break;
            case 4: // Finishing Phase
                gamePhaseInfo = "Finishing";
                break;
            case 5: // Waiting Phase
                if(getGameCanvas().requestedDiceAPI()) gamePhaseInfo = "Choose Die";
                else if(getGameCanvas().requestedAimAPI()) gamePhaseInfo = "Aiming";
                else if(getGameCanvas().requestedChoiceAPI()) gamePhaseInfo = "Choose Player";
                else if(getGameCanvas().requestedFieldAPI()) gamePhaseInfo = "Choose Field";
                else gamePhaseInfo = "Waiting for decision";
                break;
        }
        if(gamePhaseInfo.length() > 0) {
            Font textFontBig = new Font("SansSerif", Font.BOLD, (int) (28*10*getT()));
            FontMetrics metricsBig = g.getFontMetrics(textFontBig);
            int wGPI = metricsBig.stringWidth(gamePhaseInfo);
            g.setFont(textFontBig);
            g.setPaint(SBColor.WHITE);
            if(getGameCanvas().getGamePhase() == 0) g.setPaint(SBColor.BLACK);
            if(getGameCanvas().getGamePhase() > 0)
                g.drawString(gamePhaseInfo, (int) (getPitchWidth()/2 - wGPI/2), (int) (getPitchHeight()/10));
            else {
                g.drawString(gamePhaseInfo, (int) (getPitchWidth()/2 - wGPI/2), (int) (getPitchHeight() + 2*metricsBig.getHeight()));
                if(getGameCanvas().choseTeamType()) {
                    String remainingMoneyMessage = "Remaining Money: " + getGameCanvas().getRemainingMoney() + CURRENCY_SYMBOL;
                    Font textFont = new Font("SansSerif", Font.PLAIN, (int) (12*10*getT()));
                    g.setFont(textFont);
                    g.drawString(remainingMoneyMessage, (int) (getPitchWidth() / 2 - wGPI / 2), (int) (getPitchHeight() + metricsBig.getHeight()));
                }
            }
        }
    }

    public void drawGameActionFields(Graphics2D g) {
        double actionFieldSize = getPitchHeight()/4;
        if(getGameCanvas().getGamePhase() == 0 && getGameCanvas().choseTeamType()) {

            // choose arrow
            double x0 = getW() - actionFieldSize, y0 = getH() - actionFieldSize;
            g.setPaint(SBColor.ORANGE_BRIGHT);
            GeneralPath arrowChoose = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 7);
            arrowChoose.moveTo(x0 + 5f/6f * actionFieldSize, y0 + 1f/2f * actionFieldSize);
            arrowChoose.lineTo(x0 + 7f/12f * actionFieldSize, y0 + 1f/4f * actionFieldSize);
            arrowChoose.lineTo(x0 + 7f/12f * actionFieldSize, y0 + 5f/12f * actionFieldSize);
            arrowChoose.lineTo(x0 + 1f/6f * actionFieldSize, y0 + 5f/12f * actionFieldSize);
            arrowChoose.lineTo(x0 + 1f/6f * actionFieldSize, y0 + 7f/12f * actionFieldSize);
            arrowChoose.lineTo(x0 + 7f/12f * actionFieldSize, y0 + 7f/12f * actionFieldSize);
            arrowChoose.lineTo(x0 + 7f/12f * actionFieldSize, y0 + 3f/4f * actionFieldSize);
            arrowChoose.closePath();
            g.fill(arrowChoose);

            // back arrow
            x0 = 0;
            y0 = getH() - actionFieldSize;
            g.setPaint(SBColor.ORANGE_DARK);
            GeneralPath arrowBack = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 7);
            arrowBack.moveTo(x0 + 1f/6f * actionFieldSize, y0 + 1f/2f * actionFieldSize);
            arrowBack.lineTo(x0 + 5f/12f * actionFieldSize, y0 + 1f/4f * actionFieldSize);
            arrowBack.lineTo(x0 + 5f/12f * actionFieldSize, y0 + 5f/12f * actionFieldSize);
            arrowBack.lineTo(x0 + 5f/6f * actionFieldSize, y0 + 5f/12f * actionFieldSize);
            arrowBack.lineTo(x0 + 5f/6f * actionFieldSize, y0 + 7f/12f * actionFieldSize);
            arrowBack.lineTo(x0 + 5f/12f * actionFieldSize, y0 + 7f/12f * actionFieldSize);
            arrowBack.lineTo(x0 + 5f/12f * actionFieldSize, y0 + 3f/4f * actionFieldSize);
            arrowBack.closePath();
            g.fill(arrowBack);
        }
    }

    public void drawChooseTeam(Graphics2D g) {
        int numberOfTeams = getClient().getMatch().getNumberOfAvailableTeams();
        double teamWidth = getPitchWidth() / numberOfTeams;
        Font textFont = new Font("SansSerif", Font.PLAIN, (int) (12*10*getT()));
        g.setFont(textFont);
        FontMetrics metrics = g.getFontMetrics(textFont);
        int hN = metrics.getHeight();

        for (int i = 0; i < numberOfTeams; i++) {
            Team team = getClient().getMatch().getAvailableTeam(i);
            int wN = metrics.stringWidth(team.getType());

            g.setPaint(i % 2 == 0 ? SBColor.ORANGE_DARK : SBColor.ORANGE_BRIGHT);
            g.fillRect((int) (i * teamWidth), 0, (int) teamWidth, (int) teamWidth);
            g.setPaint(SBColor.BLACK);
            g.drawString(team.getType(), (int) (i * teamWidth + teamWidth / 2 - wN / 2), (int) (teamWidth / 2 - hN / 2));
        }
    }

    public void drawChoosePlayers(Graphics2D g) {

        // draw available players
        int numberOfAvailablePlayers = getClient().getMatch().getAvailableTeam(getPitchMouseLogic().getTeamIndexHovering()).getAvailablePlayers().size();
        double calcScale = getPitchWidth()/numberOfAvailablePlayers / ResourceManager.IMAGE_WIDTH;
        if(calcScale * ResourceManager.IMAGE_HEIGHT > getPitchHeight()/2) calcScale = getPitchHeight()/2 / ResourceManager.IMAGE_HEIGHT;
        Font textFont = new Font("SansSerif", Font.PLAIN, (int) (20*10*getT())),
             textFontSmall = new Font("Monospace", Font.PLAIN, (int) (16*10*getT())),
             textFontBold = new Font("SansSerif", Font.BOLD, (int) (20*10*getT()));
        FontMetrics metrics = g.getFontMetrics(textFont),
                    metricsSmall = g.getFontMetrics(textFontSmall);
        int hN = metrics.getHeight(),
            hD = metricsSmall.getHeight();

        for (int i = 0; i < getGameCanvas().getTeamChosen().getAvailablePlayers().size(); i++) {
            Player p = getGameCanvas().getTeamChosen().getAvailablePlayer(i).getCloneForTeam(getGameCanvas().getTeamChosen());
            int wN = metrics.stringWidth(p.getName());
            int headcountLeft = p.getMaxHeadcount();
            for(Player player: getGameCanvas().getPlayersChosen()) if(player.getName().equals(p.getName())) headcountLeft--;

            AffineTransform transform = new AffineTransform();
            transform.scale(calcScale, calcScale);
            transform.translate(i * ResourceManager.IMAGE_WIDTH, 0);
            g.transform(transform);

            if(headcountLeft > 0 && getGameCanvas().getRemainingMoney() - p.getPrice() >= 0) g.drawImage(p.getSpriteL(), null, 0, 0);
            else g.drawImage(p.getSpriteL(), new RescaleOp(new float[]{0.6f, 0.6f, 0.6f, 0.6f}, new float[4], null), 0, 0);

            g.setPaint(SBColor.BLACK);
            g.setFont(textFontBold);
            g.drawString(p.getName(), i == 0 ? hD : 0, ResourceManager.IMAGE_HEIGHT + 2 * hN);
            g.setFont(textFontSmall);
            String[] description = p.$GUIgetDescriptionLines();
            description = wrapLines(description, ResourceManager.IMAGE_WIDTH - hD, metricsSmall);
            for(int j = 0; j < description.length; j++) {
                g.drawString(description[j], i == 0 ? hD : 0, ResourceManager.IMAGE_HEIGHT + j * hD + 3 * hN);
            }
            g.setFont(textFont);
            String[] strings = new String[]{
                    p.getPrice()+CURRENCY_SYMBOL,
                    "MV: "+p.$GUIgetBe(),
                    "ST: "+p.$GUIgetSt(),
                    "AG: "+p.$GUIgetGe(),
                    "AV: "+p.$GUIgetRs(),
                    "Left: "+headcountLeft};
            for (int j = 0; j < strings.length; j++) {
                g.drawString(strings[j], i == 0 ? hD : 0, ResourceManager.IMAGE_HEIGHT + (description.length+1)*hD + j*hN + 3*hN);
            }

            try { g.transform(transform.createInverse()); }
            catch (NoninvertibleTransformException e) {
                getClient().log(Level.WARNING, "Tried to invert noninvertible transform.");
                e.printStackTrace();
            }
        }

        // draw chosen players
        calcScale = getPitchWidth() / Team.MAX_TEAM_SIZE / ResourceManager.IMAGE_WIDTH;

        for (int i = 0; i < getGameCanvas().getPlayersChosen().size(); i++) {
            Player p = getGameCanvas().getPlayersChosen().get(i);

            AffineTransform transform = new AffineTransform();
            transform.scale(calcScale, calcScale);
            transform.translate(i * ResourceManager.IMAGE_WIDTH, getPitchHeight() / calcScale - ResourceManager.IMAGE_HEIGHT);
            g.transform(transform);

            g.drawImage(p.getSpriteL(), null, 0, 0);
            if(!getGameCanvas().hasSentChooseTeam()) g.drawImage(ResourceManager.PROP_CROSS, null,
                    ResourceManager.IMAGE_WIDTH - ResourceManager.PROP_CROSS.getWidth(),
                    -ResourceManager.PROP_CROSS.getHeight() / 3);

            try { g.transform(transform.createInverse()); }
            catch (NoninvertibleTransformException e) {
                getClient().log(Level.WARNING, "Tried to invert noninvertible transform.");
                e.printStackTrace();
            }
        }
    }

    private String[] wrapLines(String[] lines, int maxWidth, FontMetrics metrics) {
        int newSize = lines.length, offset = 0;
        for(String line: lines) {
            if(metrics.stringWidth(line) > maxWidth)
                newSize++;
        }
        String[] newLines = new String[newSize];
        boolean testAgain = false;
        for(int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if(metrics.stringWidth(line) > maxWidth) {
                String[] splitted = splitInHalf(line);
                newLines[i+offset] = splitted[0];
                newLines[i+1+offset] = splitted[1];
                offset++;
                testAgain = true;
            } else newLines[i+offset] = line;
        }
        if(testAgain) return wrapLines(newLines, maxWidth, metrics);
        else return newLines;
    }

    private String[] splitInHalf(String line) {
        String[] words = line.split(" ");
        String part1 = "", part2 = "";
        for(int k = 0; k < words.length / 2; k++) part1 += words[k] + " ";
        for(int k = words.length / 2; k < words.length; k++) part2 += words[k] + " ";
        return new String[]{part1, part2};
    }

    // API

    public void drawHighlightAPI(Graphics2D g, Vector2d[] positions, Color[] colors) {
        if(positions.length == colors.length) {
            for(int i = 0; i < positions.length; i++) {
                highlightField(g, positions[i].x, positions[i].y, colors[i]);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void drawShowMe(Graphics2D g, String player, String showMe, int alpha) {
        Font textFontBig = new Font("SansSerif", Font.BOLD, (int) (28*10*getT()));
        FontMetrics metrics = g.getFontMetrics(textFontBig);
        String message = player + ": \"" + showMe + "\"";
        int wSM = metrics.stringWidth(message),
            hSM = metrics.getHeight();
        int alphaMax = GameCanvas.SHOW_ME_ALPHA;
        if(alpha > 255) alpha = 255;
        if(alphaMax > 255) alphaMax = 255;
        g.setFont(textFontBig);
        g.setPaint(new Color(0, 0, 0, (int) ((float) alphaMax/255 * alpha)));
        g.drawString(message, (int) (getPitchWidth()/2 - wSM/2), (int) (getPitchHeight()/2 - hSM/2));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void drawDiceAPI(Graphics2D g, int[] sides) {
        double  actionFieldSize = getPitchHeight()/4,
                dieSize = (actionFieldSize - 3*DIE_PADDING) / 2,
                scale = dieSize / ResourceManager.DIE_IMAGE_WIDTH;

        for(int i = 0; i < sides.length; i++) {
            int side = sides[i];
            BufferedImage sideImage = BlockDie.getImageFromSide(side);

            if(sideImage == null) { // create brown rect if image was not loaded
                sideImage = new BufferedImage(ResourceManager.DIE_IMAGE_WIDTH, ResourceManager.DIE_IMAGE_WIDTH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D dieG = sideImage.createGraphics();
                dieG.setPaint(SBColor.BROWN);
                dieG.drawRect(0, 0, sideImage.getWidth(), sideImage.getHeight());
                dieG.dispose();
            }

            sideImage = scaleImage(sideImage, sideImage.hashCode(), scale);
            g.drawImage(sideImage, (int) (DIE_PADDING + (i % 2) * (DIE_PADDING + dieSize)), (int) (getH() - (i > 1 ? 1 : 2) * (DIE_PADDING + dieSize)), null);

        }
    }

    public void drawAimAPI(Graphics2D g, int playerIndex, int distance) {
        if(getGameCanvas().requestedAimAPI() && playerIndex >= 0 && playerIndex < Team.MAX_TEAM_SIZE && distance > 0) {
            Team aimingTeam = isLeft() ? getClient().getMatch().getTeam(0) : getClient().getMatch().getTeam(1);

            if(aimingTeam != null) {
                Player aimingPlayer = aimingTeam.getPlayers().get(playerIndex);

                if(aimingPlayer != null) {
                    Vector2d aimingPlayerPos = (Vector2d) aimingPlayer.getPos().clone();

                    if(aimingPlayerPos != null) {
                        Vector2d dist = new Vector2d(getGameCanvas().getPitchMouseLogic().getMXY()[0], getGameCanvas().getPitchMouseLogic().getMXY()[1]);
                        dist.sub(aimingPlayerPos);

                        if(dist.length() <= distance) {
                            Color paint = SBColor.YELLOW;
                            if(aimingFieldDistanceClass == 1) paint = SBColor.ORANGE_BRIGHT;
                            if(aimingFieldDistanceClass == 2) paint = SBColor.ORANGE_DARK;
                            if(aimingFieldDistanceClass == 3) paint = SBColor.RED;
                            drawCurveToMouse(g, aimingPlayerPos, paint);

                            paint = SBColor.YELLOW_80;
                            if(aimingFieldDistanceClass == 1) paint = SBColor.ORANGE_BRIGHT_80;
                            if(aimingFieldDistanceClass == 2) paint = SBColor.ORANGE_DARK_80;
                            if(aimingFieldDistanceClass == 3) paint = SBColor.RED_80;
                            if(getGameCanvas().getPitchMouseLogic() != null)
                                if(getGameCanvas().getPitchMouseLogic().getFieldAimingAt() != null)
                                    highlightField(g, getGameCanvas().getPitchMouseLogic().getFieldAimingAt().getPos().x, getGameCanvas().getPitchMouseLogic().getFieldAimingAt().getPos().y, paint);
                        }
                    }
                }
            }

        }
    }

    public void drawChoiceAPI(Graphics2D g, Player[] playerChoices) {
        if(playerChoices != null) {
            Player hoveringPlayer = getGameCanvas().getPitchMouseLogic().getHoveringPlayer();

            for(Player player: playerChoices) {
                Vector2d playerPos = player.getPos();
                if(playerPos != null) {
                    if(hoveringPlayer != null && playerPos.equals(hoveringPlayer.getPos())) {
                        highlightField(g, playerPos.x, playerPos.y, SBColor.BLUE_BRIGHT_80);
                        drawBeacon(g, playerPos.x, playerPos.y, SBColor.BLUE_BRIGHT);
                    } else {
                        highlightField(g, playerPos.x, playerPos.y, SBColor.YELLOW_80);
                    }
                }
            }

        }
    }

    public void drawFieldAPI(Graphics2D g, Vector2d[] fieldChoices) {
        if(fieldChoices != null) {
            for(Vector2d field: fieldChoices) {
                double[] hoveringFieldCoords = getGameCanvas().getPitchMouseLogic().getMXY();
                Vector2d hoveringField = new Vector2d(hoveringFieldCoords[0], hoveringFieldCoords[1]);

                if(field != null) {
                    if(field.equals(hoveringField)) {
                        highlightField(g, field.x, field.y, SBColor.BLUE_BRIGHT_80);
                        drawBeacon(g, field.x, field.y, SBColor.BLUE_BRIGHT);
                    } else {
                        highlightField(g, field.x, field.y, SBColor.YELLOW_80);
                    }
                }

            }
        }
    }

    // HELPERS

    private void drawCurveToMouse(Graphics2D g, Vector2d aimingPlayerPos, Color color) {
        aimingPlayerPos = (Vector2d) aimingPlayerPos.clone(); // clone incoming vector not to accidentally overwrite positions

        g.setStroke(new BasicStroke((int) (getPW() / 12), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g.setPaint(color);

        double  mXCoord = getGameCanvas().getStoredMousePosition()[0],
                mYCoord = getGameCanvas().getStoredMousePosition()[1];
        aimingPlayerPos.scale(getPW());
        aimingPlayerPos.add(new Vector2d(getPW() / 2, getPW() / 2));
        aimingPlayerPos = posToDisp(aimingPlayerPos);
        double  x1 = aimingPlayerPos.x,
                y1 = aimingPlayerPos.y;

        if(mYCoord < getPitchHeight()) {
            QuadCurve2D c = new QuadCurve2D.Double();
            double d = Math.sqrt((mXCoord - x1) * (mXCoord - x1) + (mYCoord - y1) * (mYCoord - y1));
            c.setCurve(x1, y1, (x1 + mXCoord) / 2, y1 > mYCoord ? mYCoord - d / 2 : y1 - d / 2, mXCoord, mYCoord);

            g.draw(c);
        }
    }

    private void highlightFieldEdges(Graphics2D g, double x, double y, Color color) {
        highlightFieldEdges(g, (int) x, (int) y, color);
    }

    private void highlightFieldEdges(Graphics2D g, int x, int y, Color color) {
        g.setPaint(color);
        drawRectangleOnPitch(g, x * getPW(), y * getPW(), getPW(), getPW());
    }

    private void highlightField(Graphics2D g, double x, double y, Color color) {
        highlightField(g, (int) x, (int) y, color);
    }

    private void highlightField(Graphics2D g, int x, int y, Color color) {
        g.setPaint(color);
        fillRectangleOnPitch(g, x * getPW(), y * getPW(), getPW(), getPW());
    }

    private void drawBeacon(Graphics2D g2D, double x, double y, Color color) {
        drawBeacon(g2D, x, y, color, STANDARD_BEACON_ALPHA);
    }

    private void drawBeacon(Graphics2D g2D, double x, double y, Color color, int a) {
        g2D.setPaint(
                new Color(color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        (int) (a + 20 * Math.cos((double) getGameCanvas().timer * 3 /180*Math.PI)))
        );
        Vector2d pXYleft = posToDisp(new Vector2d(x * getPW() + getPW()/4, y * getPW() + getPW()/2)),
                pXYright = posToDisp(new Vector2d(x * getPW() + 3*getPW()/4, y * getPW() + getPW()/2));
        GeneralPath beacon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
        beacon.moveTo((pXYleft.x + pXYright.x) / 2, pXYleft.y - 1000);
        beacon.lineTo(pXYright.x, pXYright.y);
        beacon.lineTo(pXYleft.x, pXYleft.y);
        beacon.closePath();
        g2D.fill(beacon);
    }

    private void drawImageWithOp(Graphics2D g, BufferedImage sprite, RescaleOp op, double x, double y, double scale, double rotation, boolean transformPerspective) {
        int hash = sprite.hashCode();
        AffineTransform transform = new AffineTransform();
        if(op != null) sprite = filterImage(sprite, hash, op);

        Vector2d v = new Vector2d(x*scale, y*scale);
        if(transformPerspective) v = posToDisp(v);
        double scaleDrag = 30, scaleDragFactor = 1.1;
        double distanceScale = ((v.y/getPH() + scaleDrag*scaleDragFactor) / (Pitch.PITCH_WIDTH + scaleDrag)) * 0.99;

        v.y -= 11*ResourceManager.IMAGE_HEIGHT/20 * scale;
        v.x += (v.x - getPitchWidth()/2) * 0.01;

        transform.translate(v.x, v.y);
        if(scale != 1) transform.scale(scale, scale);
        if(rotation != 0) transform.rotate(rotation);
        g.transform(transform);

        sprite = scaleImage(sprite, hash, distanceScale);
        g.drawImage(sprite, null, 0, 0);

        try { g.transform(transform.createInverse()); }
        catch (NoninvertibleTransformException e) {
            getClient().log(Level.WARNING, "Tried to invert noninvertible transform.");
            e.printStackTrace();
        }
    }

    private BufferedImage filterImage(BufferedImage original, int hash, RescaleOp op) {
        HashMap<Integer, BufferedImage> imageMap = imageFilterBuffer.get(hash);
        float[] scaleFactors = op.getScaleFactors(null);
        int opHash = (int) (10*scaleFactors[0]+100*scaleFactors[1]+1000*scaleFactors[2]+10000*scaleFactors[3]);
        if(imageMap != null) {
            if(imageMap.size() > MAX_IMAGE_BUFFER_SIZE) emptyImageBuffer(); // empty image buffer of this image if it is too full.
            else if(imageMap.containsKey(opHash)) return imageMap.get(opHash); // return the buffered resized image if it was resized before.
        }

        BufferedImage filtered = gameCanvas.getGraphicsConfiguration().createCompatibleImage(original.getWidth(), original.getHeight(), Transparency.BITMASK);

        Graphics2D filteredG = filtered.createGraphics();
        filteredG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        filteredG.drawImage(original, op, 0, 0);
        filteredG.dispose();

        if(imageMap != null) imageMap.put(opHash, filtered);
        else {
            imageMap = new HashMap<Integer, BufferedImage>();
            imageMap.put(opHash, filtered);
            imageFilterBuffer.put(hash, imageMap);
        }

        return filtered;
    }

    private BufferedImage scaleImage(BufferedImage original, int hash, double scale) {
        HashMap<Double, BufferedImage> imageMap = imageBuffer.get(hash);
        if(imageMap != null) {
            if(imageMap.size() > MAX_IMAGE_BUFFER_SIZE) emptyImageBuffer(); // empty image buffer of this image if it is too full.
            else if(imageMap.containsKey(scale)) return imageMap.get(scale); // return the buffered resized image if it was resized before.
        }

        int scaledW = (int) (original.getWidth()*scale), scaledH = (int) (original.getHeight()*scale);
//        BufferedImage scaled = (new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_BILINEAR)).filter(original, gameCanvas.getGraphicsConfiguration().createCompatibleImage(scaledW, scaledH, Transparency.BITMASK));

        BufferedImage scaled = gameCanvas.getGraphicsConfiguration().createCompatibleImage(scaledW, scaledH, Transparency.BITMASK);

        Graphics2D scaledG = scaled.createGraphics();
        scaledG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        scaledG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        scaledG.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        scaledG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        scaledG.drawImage(original, 0, 0, scaledW, scaledH, 0, 0, original.getWidth(), original.getHeight(), null);
        scaledG.dispose();

        if(imageMap != null) imageMap.put(scale, scaled);
        else {
            imageMap = new HashMap<Double, BufferedImage>();
            imageMap.put(scale, scaled);
            imageBuffer.put(hash, imageMap);
        }

        return scaled;
    }

    private void drawLineOnPitch(Graphics2D g, double x1, double y1, double x2, double y2) {
        Vector2d v1 = posToDisp(new Vector2d(x1, y1)), v2 = posToDisp(new Vector2d(x2, y2));
        g.drawLine((int) v1.x, (int) v1.y, (int) v2.x, (int) v2.y);
    }

    private void drawRectangleOnPitch(Graphics2D g, double x, double y, double w, double h) {
        // v0, v1, v2, v3 clockwise with v0 is upper left.
        Vector2d v0 = posToDisp(new Vector2d(x, y)),
                 v1 = posToDisp(new Vector2d(x + w, y)),
                 v2 = posToDisp(new Vector2d(x + w, y + h)),
                 v3 = posToDisp(new Vector2d(x, y + h));
        GeneralPath poly = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
        poly.moveTo(v0.x, v0.y);
        poly.lineTo(v1.x, v1.y);
        poly.lineTo(v2.x, v2.y);
        poly.lineTo(v3.x, v3.y);
        poly.closePath();
        g.setStroke(new BasicStroke((int) (getPW() / 18), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g.draw(poly);
    }

    private void fillRectangleOnPitch(Graphics2D g, double x, double y, double w, double h) {
        // v0, v1, v2, v3 clockwise with v0 is upper left.
        Vector2d v0 = posToDisp(new Vector2d(x, y)),
                 v1 = posToDisp(new Vector2d(x + w, y)),
                 v2 = posToDisp(new Vector2d(x + w, y + h)),
                 v3 = posToDisp(new Vector2d(x, y + h));
        GeneralPath poly = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
        poly.moveTo(v0.x, v0.y);
        poly.lineTo(v1.x, v1.y);
        poly.lineTo(v2.x, v2.y);
        poly.lineTo(v3.x, v3.y);
        poly.closePath();
        g.fill(poly);
    }

    /**
     * Converts a position on the pitch to a position on the display.
     * @param pos The position on the pitch.
     * @return The position on the display.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public Vector2d posToDisp(Vector2d pos) {
        // ATTEMPT 1
//        double  dispX = vX - vZ * (vX - pos.x) / (vZ - pos.y),
//                dispY = getPitchHeight() - (vY - vZ * vY / (vZ - pos.y));
        // ATTEMPT 2
//        double  temp = (mZ - vZ) / (vZ - pos.y),
//                dispX = vX + temp * (vX - pos.x),
//                dispY = vY + temp * (vY - pY);
        // ATTEMPT 3
//        HashMap<Integer, Vector2d> row = null;
//
//        if(storedCalculations.containsKey((int) pos.x)) {
//            row = storedCalculations.get((int) pos.x);
//            if(row.containsKey((int) pos.y)) {
//                Vector2d stored = row.get((int) pos.y);
//                return stored;
//            }
//        }
//
        double  t = (dY - vY) / (vY - pos.y),
                dX = vX + t * (vX - pos.x),
                dZ = getPitchHeight() - (vZ + t * (vZ - pZ));
        Vector2d calc = new Vector2d(dX, dZ);
//
//        if(row != null) {
//            row.put((int) pos.y, calc);
//        } else {
//            HashMap<Integer, Vector2d> newRow = new HashMap<Integer, Vector2d>();
//            newRow.put((int) pos.y, calc);
//            storedCalculations.put((int) pos.x, newRow);
//        }
//        System.out.println(storedCalculations.size() + " – " + pos.x + ", " + pos.y + ": " + calc.x + ", " + calc.y);

        return calc;
    }

    /**
     * Converts a position on the display to a position on the pitch.
     * @param disp The position on the display.
     * @return The position on the pitch.
     */
    public Vector2d dispToPos(Vector2d disp) {
        // ATTEMPT 1
//        double  posX = vX - vY * (vX - disp.x) / (vY - (getPitchHeight() - disp.y)),
//                posY = vZ - vY * vZ / (vY - (getPitchHeight() - disp.y));
        // ATTEMPT 2
//        double  posX = vX - (disp.x - vX) / (vY - pY) * (disp.y - vY),
//                posY = vZ - (mZ - vZ) / (disp.y - vY) * (vY - pY);
        // ATTEMPT 3
        double  dZ = getPitchHeight() - disp.y,
                t = (pZ - vZ) / (vZ - dZ),
                pX = vX + t * (vX - disp.x),
                pY = vY + t * (vY - dY);
        return new Vector2d(pX, pY);
    }

    public boolean isOwnPlayer(Player player) {
        String hoveringPlayerCoach = player.getTeam().getCoach().getName();
        String username = getGameCanvas().getGameFrame().getClient().getUsername();
        return hoveringPlayerCoach.equals(username);
    }

    public void emptyImageBuffer() {
        imageBuffer = new HashMap<Integer, HashMap<Double, BufferedImage>>();
    }

    // GETTERS & SETTERS

    public GameCanvas getGameCanvas() {
        return gameCanvas;
    }

    public Client getClient() {
        return getGameCanvas().getClient();
    }

    public PitchMouseLogic getPitchMouseLogic() {
        return getGameCanvas().getPitchMouseLogic();
    }

    public Pitch getPitch() {
        return getGameCanvas().getPitch();
    }

    public double getPW() {
        return getGameCanvas().getPW();
    }

    public double getPH() {
        return getGameCanvas().getPH();
    }

    public double getT() {
        return getGameCanvas().getT();
    }

    public double getW() {
        return getGameCanvas().getW();
    }

    public double getH() {
        return getGameCanvas().getH();
    }

    public double getPitchWidth() {
        return getGameCanvas().getPitchWidth();
    }

    public double getPitchHeight() {
        return getGameCanvas().getPitchHeight();
    }

    public boolean isLeft() {
        return getGameCanvas().isLeft();
    }
}
