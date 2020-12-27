import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.df;
import jade.lang.acl.ACLMessage;
import java.util.concurrent.locks.*;
import java.util.concurrent.Semaphore;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import agents.*;
/**
 * 
 * @author Pablo
 *
 */

public class MainAgent extends Agent {

	// Initialization of parameters
	private GUI gui;
	private AID[] playerAgents; // AID are agents identifiers
	private int playedRounds = 0, numberOfPlayers = 0, matrixSize = 4, numberOfRounds = 2, roundNumber = 0;
	private float p1payoff = 0, p2payoff = 0; // for the payoff of each player each round
	private float p1coops = 0, p1defeats = 0, p2coops = 0, p2defeats = 0; // the amount of times p1 and p2 coops and
																			// defeats in a game for stats purposes
	public HashMap<AID, PlayerStats> stats = new HashMap<>(); // a HashMap with the ID of each player and the object of
																// his stats
	public HashMap<String, AID> NombreAID = new HashMap<>(); // a HashMap identifying each ID with the name of the
																// player
	Semaphore semaphore = new Semaphore(1); // I use semaphores for the buttons of stop and resume game

	@Override
	protected void setup() {
		gui = new GUI(this); //new instance of the gui
		System.setOut(new PrintStream(gui.getLoggingOutputStream()));

		updatePlayers();
		gui.logLine("Agent " + getAID().getName() + " is ready.");
	}

	public AID[] getAgents() {
		return playerAgents;
	}

	public void StopGame() throws InterruptedException {
		semaphore.acquire(); // when I use this the game is stopped and the code doesnt run the next round
								// till the resumegame is clicked
	}

	public void ContinueGame() {

		semaphore.release(); // when I use this, the semaphore liberates and the game continues
	}

	/**
	 * This function gets the players and put them in the array PlayerAgents and the
	 * HashMaps NombreAID and stats, then it uses this to create the table of stats
	 * and the table of players
	 * 
	 * @return
	 */
	public int updatePlayers() {

		gui.logLine("Updating player list");
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		template.addServices(sd);
		try {

			DFAgentDescription[] result = DFService.search(this, template); // searchs for the agents

			if (result.length > 0) {
				gui.logLine("Found " + result.length + " players");
			}
			playerAgents = new AID[result.length];
			for (int i = 0; i < result.length; ++i) {
				playerAgents[i] = result[i].getName(); // updates the list of players with the found agents
			}
		} catch (FIPAException fe) {
			gui.logLine(fe.getMessage());
		}

		String[] playerNames = new String[playerAgents.length];
		for (int i = 0; i < playerAgents.length; i++) {
			playerNames[i] = playerAgents[i].getName(); // auxiliar parameter to push it in the HashMap
			gui.logLine(playerNames[i]);
			String[] partes = playerNames[i].split("@");
			stats.put(playerAgents[i], new PlayerStats(playerNames[i], 0, 0, 0, playerAgents[i], 0, 0)); // updates the
																											// HashMap
																											// with a
																											// new
																											// object
																											// with each
																											// agent
			NombreAID.put(partes[0], playerAgents[i]); // updates the HashMap with the name and the ID of each agent

		}

		numberOfPlayers = playerAgents.length;
		gui.setPlayersUI(playerNames); // creates the Players Table
		gui.refreshPlayerStats(stats); // creates the stats Table
		return 0;
	}

	/**
	 * Method use to return the players removed previously
	 */
	public void returnPlayers() {
		updatePlayers(); // reinitializing players
		roundNumber = 0;
		p1payoff = 0;
		p2payoff = 0;
		p1coops = 0;
		p1defeats = 0;
		p2coops = 0;
		p2defeats = 0;
		semaphore = new Semaphore(1);
		// reseting the variables
		gui.logLine("Reseting player list");

	}
	
	/** 
	 * Method use to reset the stats of all the players
	 */
	public void resetStats() {
		roundNumber = 0;
		p1payoff = 0;
		p2payoff = 0;
		p1coops = 0;
		p1defeats = 0;
		p2coops = 0;
		p2defeats = 0;
		// reseting the variables

		stats.forEach((k, v) -> v.setCoop(0));
		stats.forEach((k, v) -> v.setCoopPCTGE(0));
		stats.forEach((k, v) -> v.setDefeat(0));
		stats.forEach((k, v) -> v.setDefeatPCTGE(0));
		stats.forEach((k, v) -> v.setPoints(0));
		//reseting each stat of each player in the HashMap stats
		gui.refreshPlayerStats(stats); //updating the changes (every stat reset)
		semaphore = new Semaphore(1); 

	}

	
	 /**
	  * For the GUI to know how much Rounds to play each game
	  * @param rounds
	  */
	public void Rounds(int rounds) {

		gui.logLine("The number of Rounds submitted was " + rounds);
		numberOfRounds = rounds;

	}
	
	/**
	 * Starts a new Game
	 * @return
	 */

	public int newGame() {
		playedRounds++;
		addBehaviour(new GameManager());
		return 0;
	}

	/**
	 * Removes the player received 
	 * @param player
	 */
	public void Remove(String player) {

		String partes[] = player.split("@");
		for (int m = 0; m < stats.size(); m++) { //Drops the player from the Players array
			if (playerAgents[m] != null) {
				if (playerAgents[m].equals(stats.get(NombreAID.get(player)).getAid())) {

					gui.logLine(stats.get(NombreAID.get(player)).getName());
					playerAgents[m] = null; 
				}
			}
		}

		stats.remove(NombreAID.get(player)); //drops the player from the stats map

		String[] playerNames = new String[stats.size()];

		gui.refreshPlayerStats(stats); // refresh the stats table

		int m = 0;
		for (Map.Entry<AID, PlayerStats> entry : stats.entrySet()) {
			PlayerStats value = entry.getValue();
			playerNames[m] = value.getName();
			System.out.println("Player" + m + ": " + playerNames[m]);
			m++;
			//updates the playerNames map
		}

		gui.setPlayersUI(playerNames); // drop the player from the table of players

	}

	/**
	 * In this behavior this agent manages the course of a match during all the
	 * rounds.
	 */
	private class GameManager extends SimpleBehaviour {

		@Override
		public synchronized void action() {
			// Assign the IDs
			gui.logLine("Se va a iniciar nuevo juego");
			ArrayList<PlayerInformation> players = new ArrayList<>();
			int lastId = 0;
			for (AID a : playerAgents) {
				if (a != null) {
					players.add(new PlayerInformation(a, lastId++));
				}
			}
			// Initialize (inform ID)

			
			//informing every player of his ids and the number of rounds 
			for (PlayerInformation player : players) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.setContent("Id#" + player.id + "#" + numberOfRounds + "," + numberOfPlayers);
				gui.logLine("Id#" + player.id + "#" + numberOfRounds + "," + numberOfPlayers);
				msg.addReceiver(player.aid);
				send(msg);
			}

			// Organize the matches
			for (int i = 0; i < players.size(); i++) {
				for (int j = i + 1; j < players.size(); j++) {

					Random rand = new Random();
					double randomNum = 0.9 + (1 - 0.9) * rand.nextDouble(); 
					double RoundsPrima = numberOfRounds * randomNum;// how many rounds are gonna be played in each game (0.9 - 1)
					long finalRounds = Math.round(RoundsPrima);

					for (int m = 0; m < finalRounds; m++) {
						try {
							semaphore.acquire(); //before each round in case the stop button is clicked
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						playGame(players.get(i), players.get(j), finalRounds);
						semaphore.release(); //liberates after each round

					}
				}

			}

		}

		/**
		 * A game between two players is performed
		 * @param player1
		 * @param player2
		 * @param finalRounds
		 */
		private void playGame(PlayerInformation player1, PlayerInformation player2, long finalRounds) {
			// Assuming player1.id < player2.id
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			if (roundNumber == 0) { //in the first round the mainagent notifies the players that they are going to play
				msg.addReceiver(player1.aid);
				msg.addReceiver(player2.aid);
				msg.setContent("NewGame#" + player1.id + "#" + player2.id);
				gui.logLine(msg.getContent());
				send(msg);
			}
			String pos1, pos2;

			msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent("Action");
			msg.addReceiver(player1.aid);
			send(msg); //asks the first player for an action

			gui.logLine("Main Waiting for player1 movement");
			ACLMessage move1 = blockingReceive();
			gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
			pos1 = move1.getContent().split("#")[1];
			//gets the action of the first one

			msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setContent("Action");
			msg.addReceiver(player2.aid);
			send(msg);
			//asks the second player for an action

			gui.logLine("Main Waiting for player2 movement");
			ACLMessage move2 = blockingReceive();
			gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
			pos2 = move2.getContent().split("#")[1];
			//gets the action of the second one
			
			msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(player1.aid);
			msg.addReceiver(player2.aid);
			String results = comparePos(pos1, pos2, player1, player2);			//gets the payoff of each player

			msg.setContent("Results#" + player1.id + "," + player2.id + "#" + pos1 + "," + pos2 + "#" + results); //notifies the players the results of the round
			
			send(msg);
			roundNumber++;

			if (roundNumber == finalRounds) { // the last round
				roundNumber = 0; //resets the rounds for the next game
				String info = "GameOver#" + player1.id + "," + player2.id + "#" + p1payoff / finalRounds + ","
						+ p2payoff / finalRounds; //the final results for each players, the average of points: total payoff / number of rounds

				float defp1 = Math.round((((stats.get(player1.aid).getDefeat()
						/ (stats.get(player1.aid).getDefeat() + stats.get(player1.aid).getCoop())) * 100) * 100) / 100); // the % of times the player 1 defeated
				float coopp1 = 100 - defp1;  // the % of times the player 1 cooperated
				float defp2 = Math.round((((stats.get(player2.aid).getDefeat()
						/ (stats.get(player2.aid).getDefeat() + stats.get(player2.aid).getCoop())) * 100) * 100) / 100);  // the % of times the player 2 defeated
				float coopp2 = 100 - defp2;// the % of times the player 2 cooperated

				p1defeats = 0;
				p2defeats = 0;
				p1coops = 0;
				p2coops = 0;
				//resets the variables

				stats.get(player1.aid).setCoopPCTGE(coopp1);
				stats.get(player1.aid).setDefeatPCTGE(defp1);
				stats.get(player2.aid).setCoopPCTGE(coopp2);
				stats.get(player2.aid).setDefeatPCTGE(defp2);
				//updates the percentages of cooperations and defeats for both players

				stats.get(player1.aid).setPoints(stats.get(player1.aid).getPoints()
						+ Math.round(((float) p1payoff / finalRounds) * 100.0) / 100.0);
				stats.get(player2.aid).setPoints(stats.get(player2.aid).getPoints()
						+ Math.round(((float) p2payoff / finalRounds) * 100.0) / 100.0);
				//updates the points for both players

				gui.refreshPlayerStats(stats); //updates the stats table

				p1payoff = 0;
				p2payoff = 0;

				gui.logLine(info);
				msg.setContent(info);

				send(msg);
			}
		}

		@Override
		public boolean done() {
			return true;
		}

	}
	
	/**
	 * returns the stats map
	 * @return
	 */
	public HashMap<AID, PlayerStats> getStats() {

		return stats;

	}
	
	/**
	 * 
	 * Class PlayerInformation with the AID and the id
	 *
	 */
	public class PlayerInformation {

		AID aid;
		int id;

		public PlayerInformation(AID a, int i) {
			aid = a;
			id = i;
		}

		@Override
		public boolean equals(Object o) {
			return aid.equals(o);
		}
	}

	/**
	 * Returns the payoff for each player in a round
	 * @param pos1
	 * @param pos2
	 * @param player1
	 * @param player2
	 * @return
	 */
	String comparePos(String pos1, String pos2, PlayerInformation player1, PlayerInformation player2) {

		if (pos1.equals("C")) {
			stats.get(player1.aid).setCoop(stats.get(player1.aid).getCoop() + 1);
			p1coops++;
			if (pos2.equals("C")) {
				stats.get(player2.aid).setCoop(stats.get(player2.aid).getCoop() + 1);
				p1payoff = p1payoff + 3;
				p2payoff = p2payoff + 3;
				p2coops++;
				gui.refreshPlayerStats(stats);
				return "3,3";
				
			} else if (pos2.equals("D")) {
				stats.get(player2.aid).setDefeat(stats.get(player2.aid).getDefeat() + 1);
				p2defeats++;
				p1payoff = p1payoff + 0;
				p2payoff = p2payoff + 5;
				gui.refreshPlayerStats(stats);
				return "0,5";
			}

		} else if (pos1.equals("D")) {
			stats.get(player1.aid).setDefeat(stats.get(player1.aid).getDefeat() + 1);
			p1defeats++;
			if (pos2.equals("C")) {
				stats.get(player2.aid).setCoop(stats.get(player2.aid).getCoop() + 1);
				p2coops++;
				p1payoff = p1payoff + 5;
				p2payoff = p2payoff + 0;
				gui.refreshPlayerStats(stats);
				return "5,0";

			} else if (pos2.equals("D")) {
				p2defeats++;
				stats.get(player2.aid).setDefeat(stats.get(player2.aid).getDefeat() + 1);
				p1payoff = p1payoff + 1;
				p2payoff = p2payoff + 1;
				gui.refreshPlayerStats(stats);
				return "1,1";

			} //returns the appropriate payoff for each player depending on their actions in each round and updates the stats table

		}
		return "-1,-1";

	}

}
