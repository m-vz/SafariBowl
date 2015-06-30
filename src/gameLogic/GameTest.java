package gameLogic;

import static org.junit.Assert.*;

import network.SBProtocolCommand;
import network.SBProtocolMessage;

import org.junit.Test;

import server.Server;
import client.Client;
import server.ServerController;
import server.shells.LineShell;

public class GameTest {

	private static Server server;
	private static Client client1, client2;

	static {
		server = new Server(new LineShell(ServerController.DEFAULT_PORT));
		server.runServer();
		server.start(9989);
		server.logmatches = false;
		client1 = new Client();
		client1.autologin = false;
		client1.automatchstart = false;
		client2 = new Client();
		client2.autologin = false;
		client2.automatchstart = false;
		client1.runClient();
		client2.runClient();
		client1.connect("localhost", 9989);
		client2.connect("localhost", 9989);
		String password = "1234";
		client1.login("Client1", password);
		try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
		if(!client1.isLoggedIn()){
			client1.login("Client1", password);
		}
		client2.login("Client2", password);
		try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
		if(!client2.isLoggedIn()){
			client2.login("Client2", password);
		}
		client1.startGame();
		try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
		client2.invitePlayer("Client1");
		try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

		SBProtocolMessage message1 = new SBProtocolMessage(client1.getUID(), SBProtocolCommand.ACTIO, "SET TEAM","Team1", "SAVANNA", "Giraffe", "Elephant", "Rhino");
		SBProtocolMessage message2 = new SBProtocolMessage(client2.getUID(), SBProtocolCommand.ACTIO, "SET TEAM","Team2", "POLARREGION", "Puffin", "Seacow", "Penguin");
		
		client1.sendGameMessage(message1);
		try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
		client2.sendGameMessage(message2);
		try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
		
		
	}
	
	@Test
	public void testGetOpponentUser() {
		assertEquals(server.getRunningMatches().get(0).getUser(0), server.getRunningMatches().get(0).getOpponent(1));
		assertEquals(server.getRunningMatches().get(0).getUser(1), server.getRunningMatches().get(0).getOpponent(0));
		assertEquals(null, server.getRunningMatches().get(0).getOpponent(2));
		assertNotEquals(server.getRunningMatches().get(0).getUser(0), server.getRunningMatches().get(0).getOpponent(0));
		assertNotEquals(server.getRunningMatches().get(0).getUser(1), server.getRunningMatches().get(0).getOpponent(1));
	}

	@Test
	public void testGetOpponentInt() {
		assertEquals(server.getRunningMatches().get(0).getUser(1),server.getRunningMatches().get(0).getOpponent(0));
		assertEquals(server.getRunningMatches().get(0).getUser(0),server.getRunningMatches().get(0).getOpponent(1));
		assertNotEquals(null,server.getRunningMatches().get(0).getOpponent(0));
		assertNotEquals(null,server.getRunningMatches().get(0).getOpponent(1));
		assertNotEquals(server.getRunningMatches().get(0).getUser(0),server.getRunningMatches().get(0).getOpponent(0));
		assertNotEquals(server.getRunningMatches().get(0).getUser(1),server.getRunningMatches().get(0).getOpponent(1));
		assertEquals(null, server.getRunningMatches().get(0).getOpponent(2));
		
	}

	@Test
	public void testGetOpposingTeam() {
		assertEquals(server.getRunningMatches().get(0).getTeam(0), server.getRunningMatches().get(0).getOpposingTeam(server.getRunningMatches().get(0).getTeam(1)));
		assertEquals(server.getRunningMatches().get(0).getTeam(1), server.getRunningMatches().get(0).getOpposingTeam(server.getRunningMatches().get(0).getTeam(0)));
		assertNotEquals(null, server.getRunningMatches().get(0).getOpponent(1));
		assertNotEquals(null, server.getRunningMatches().get(0).getOpponent(0));
		assertNotEquals(server.getRunningMatches().get(0).getTeam(0), server.getRunningMatches().get(0).getOpposingTeam(server.getRunningMatches().get(0).getTeam(0)));
		assertNotEquals(server.getRunningMatches().get(0).getTeam(1), server.getRunningMatches().get(0).getOpposingTeam(server.getRunningMatches().get(0).getTeam(1)));
	}

	@Test
	public void testGetWinner() {
		if(server.getRunningMatches().get(0).getScoreFromTeam(0)>server.getRunningMatches().get(0).getScoreFromTeam(1)){
			assertEquals(server.getRunningMatches().get(0).getWinner(),server.getRunningMatches().get(0).getUser(0));
			assertNotEquals(server.getRunningMatches().get(0).getWinner(),server.getRunningMatches().get(0).getUser(1));
		} else if(server.getRunningMatches().get(0).getScoreFromTeam(0)<server.getRunningMatches().get(0).getScoreFromTeam(1)){
			assertEquals(server.getRunningMatches().get(0).getWinner(),server.getRunningMatches().get(0).getUser(1));
			assertNotEquals(server.getRunningMatches().get(0).getWinner(),server.getRunningMatches().get(0).getUser(0));
		} else if(server.getRunningMatches().get(0).getScoreFromTeam(0)==server.getRunningMatches().get(0).getScoreFromTeam(1)){
			assertEquals(server.getRunningMatches().get(0).getWinner(),server.getRunningMatches().get(0).getUser(1));
			assertNotEquals(server.getRunningMatches().get(0).getWinner(),server.getRunningMatches().get(0).getUser(0));
		}
	}

	@Test
	public void testSetWinner() {
		if(server.getRunningMatches().get(0).getScoreFromTeam(0)>server.getRunningMatches().get(0).getScoreFromTeam(1)){
			server.getRunningMatches().get(0).setWinner(server.getRunningMatches().get(0).getUser(0));
			
			assertEquals(server.getRunningMatches().get(0).getUser(0), server.getRunningMatches().get(0).getWinner());
			assertNotEquals(server.getRunningMatches().get(0).getUser(1), server.getRunningMatches().get(0).getWinner());
			
		} else if(server.getRunningMatches().get(0).getScoreFromTeam(0)<server.getRunningMatches().get(0).getScoreFromTeam(1)){
			server.getRunningMatches().get(0).setWinner(server.getRunningMatches().get(0).getUser(1));
			
			assertEquals(server.getRunningMatches().get(0).getUser(1), server.getRunningMatches().get(0).getWinner());
			assertNotEquals(server.getRunningMatches().get(0).getUser(0), server.getRunningMatches().get(0).getWinner());
			
		} else if(server.getRunningMatches().get(0).getScoreFromTeam(0)==server.getRunningMatches().get(0).getScoreFromTeam(1)){
			server.getRunningMatches().get(0).setWinner(server.getRunningMatches().get(0).getUser(1));
			
			assertEquals(server.getRunningMatches().get(0).getUser(1), server.getRunningMatches().get(0).getWinner());
			assertNotEquals(server.getRunningMatches().get(0).getUser(0), server.getRunningMatches().get(0).getWinner());
		}
	} 

	@Test
	public void testGetParent() {
		assertEquals(server,server.getRunningMatches().get(0).getParent());
	}

	@Test
	public void testIsRunning() {
		assertEquals(true,server.getRunningMatches().get(0).isRunning());
		assertNotEquals(false,server.getRunningMatches().get(0).isRunning());
		
		server.getRunningMatches().get(0).setRunning(false);
		assertEquals(false,server.getRunningMatches().get(0).isRunning());
		assertNotEquals(true,server.getRunningMatches().get(0).isRunning());
	}

	@Test
	public void testGetRoundCount() {
		for(int j=0;j<100;j++){
			assertEquals(j,server.getRunningMatches().get(0).getRoundCount());
			server.getRunningMatches().get(0).countUpRound();
		}
	}

	@Test
	public void testGetScoreFromTeam() {
		assertEquals(0, server.getRunningMatches().get(0).getScoreFromTeam(0));
		assertEquals(0, server.getRunningMatches().get(0).getScoreFromTeam(1));
		
		for(int i=1;i<10;i++){
			server.getRunningMatches().get(0).countUpScore(0);
			assertEquals(i,server.getRunningMatches().get(0).getScoreFromTeam(0));
		}
		for(int i=1;i<10;i++){
			server.getRunningMatches().get(0).countUpScore(1);
			assertEquals(i,server.getRunningMatches().get(0).getScoreFromTeam(1));
		}
		for(int i=2;i<10;i++){
			try{
				server.getRunningMatches().get(0).getScoreFromTeam(i);
				fail("Throw an Index Out Of Bounds");
			}catch(IndexOutOfBoundsException ignored){
			}
		}
		
		
	}

	@Test
	public void testGetTeam() {
		Team dickeMaenner = new Team("Savanna");
		server.getRunningMatches().get(0).setTeam(0, dickeMaenner);
		Team dickeFrauen = new Team("Savanna");
		server.getRunningMatches().get(0).setTeam(1, dickeFrauen);
		assertEquals(dickeMaenner,server.getRunningMatches().get(0).getTeam(0));
		assertEquals(dickeFrauen,server.getRunningMatches().get(0).getTeam(1));
		assertNotEquals(dickeFrauen,server.getRunningMatches().get(0).getTeam(0));
		assertNotEquals(dickeMaenner,server.getRunningMatches().get(0).getTeam(1));
		for(int i=2;i<10;i++){
			try{
				server.getRunningMatches().get(0).getTeam(i);
				fail("The Team doent exist");
			}catch(IndexOutOfBoundsException ignored){}
		}
			
	}

	@Test
	public void testGetAvailableTeams() {
		boolean found = false;
		Team dickeMaenner = new Team("Savanna");
		server.getRunningMatches().get(0).setTeam(0, dickeMaenner);
		for (int i=0;i<3;i++){
			if(server.getRunningMatches().get(0).getTeam(0).getClass().equals(server.getRunningMatches().get(0).getAvailableTeam(i).getClass())){
				found = true;
			}
		}
		if(!found){
			fail("Team dosent seam to exist");
		}
		Team dickeFrauen = new Team("PolarRegion");
		server.getRunningMatches().get(0).setTeam(1, dickeFrauen);
		found = false;
		for (int i=0;i<3;i++){
			if(server.getRunningMatches().get(0).getTeam(1).getClass().equals(server.getRunningMatches().get(0).getAvailableTeam(i).getClass())){
				found = true;
			}
		}
		if(!found){
			fail("Team dosent seam to exist");
		}
	}

	@Test
	public void testSetTeamType(){
		SBProtocolMessage message0 = new SBProtocolMessage(client1.getUID(), SBProtocolCommand.EVENT, "SET TEAM","Team1", "SAVANNA", "Giraffe", "Elephant", "Rhino");
		SBProtocolMessage message1 = new SBProtocolMessage(client2.getUID(), SBProtocolCommand.EVENT, "SET TEAM","Team2", "POLARREGION", "Puffin", "SeaCow", "Penguin");
		Team team1 = server.getRunningMatches().get(0).setTeamType(message0, 2);
		boolean found = false;
		for(Team t: server.getTeamManager().getTeamBlueprints()){
			if(t.getType().equals(team1.getType())){
				found = true;
			}
		}
		if(!found){
			fail("This Team doesnt exist");
		}
		Team team2 = server.getRunningMatches().get(0).setTeamType(message1, 2);
		found = false;
		for(Team t: server.getTeamManager().getTeamBlueprints()){
			if(t.getType().equals(team2.getType())){
				found = true;
			}
		}
		if(!found){
			fail("This Team doesnt exist");
		}
	}

	@Test
	public void testFindUserIndex() {
		SBProtocolMessage message0 = new SBProtocolMessage(server.getRunningMatches().get(0).getUser(0).getUID(), SBProtocolCommand.EVENT, "Penis");
		SBProtocolMessage message1 = new SBProtocolMessage(server.getRunningMatches().get(0).getUser(1).getUID(), SBProtocolCommand.EVENT, "blah");
		assertEquals(0,server.getRunningMatches().get(0).findUserIndex(message0));
		assertEquals(1,server.getRunningMatches().get(0).findUserIndex(message1));
		assertNotEquals(-1,server.getRunningMatches().get(0).findUserIndex(message0));
		assertNotEquals(-1,server.getRunningMatches().get(0).findUserIndex(message1));
		assertNotEquals(0,server.getRunningMatches().get(0).findUserIndex(message1));
		assertNotEquals(1,server.getRunningMatches().get(0).findUserIndex(message0));
	}

	@Test
	public void testSetGamePhase(){
		server.getRunningMatches().get(0).setGamePhase(1);
		assertEquals(1, server.getRunningMatches().get(0).getGamePhase());
		server.getRunningMatches().get(0).setGamePhase(5);
		assertEquals(5, server.getRunningMatches().get(0).getGamePhase());
		server.getRunningMatches().get(0).setGamePhase(0);
		assertEquals(0, server.getRunningMatches().get(0).getGamePhase());
		server.getRunningMatches().get(0).setGamePhase(2);
		assertEquals(2, server.getRunningMatches().get(0).getGamePhase());
		server.getRunningMatches().get(0).setGamePhase(4);
		assertEquals(4, server.getRunningMatches().get(0).getGamePhase());
		server.getRunningMatches().get(0).setGamePhase(3);
		assertEquals(3, server.getRunningMatches().get(0).getGamePhase());
	}
	
	@Test
	public void testClearAllPlayerPos(){
		
		client2.coolLineup(0);
		try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
		client1.coolLineup(1);
		try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
		client2.coolLineup(0);
		try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
		client1.coolLineup(1);
		try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
		server.getRunningMatches().get(0).clearAllPlayerPos();
		for(int y =0; y<Pitch.PITCH_WIDTH;y++){
			for(int x=0;x<Pitch.PITCH_LENGTH;x++){
				assertEquals(null,server.getRunningMatches().get(0).getPitch().getFields()[x][y].getPlayer());
			}
		}
	}
	
}

