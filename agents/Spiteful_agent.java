package agents;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Random;

public class Spiteful_agent extends Agent {

	private State state;
	private AID mainAgent;
	private int myId, opponentId;
	private int N, R;
	private String OpLastMove = "F";
	private int SpiteMode = 0;
	private ACLMessage msg;
	private String name;

	/**
	 * setups the agent
	 */
	protected void setup() {
		state = State.s0NoConfig;
		String[] names = getAID().getName().split("@");
		name = names[0]; // to get only the first part, not the part after the @

		// Register in the yellow pages as a player
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Player");
		sd.setName("Game");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new Play());
		System.out.println("Spiteful " + name + " is ready.");

	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		System.out.println("Spiteful " + name + " terminating.");
	}

	private enum State {
		s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
	}

	private class Play extends CyclicBehaviour {
		Random random = new Random(1000);

		@Override
		public void action() {
			System.out.println(name + ":" + state.name());
			msg = blockingReceive();
			if (msg != null) {
				System.out.println(name + " received " + msg.getContent() + " from " + msg.getSender().getName()); // DELETEME
				// -------- Agent logic
				switch (state) {
				case s0NoConfig:
					// If INFORM Id#_#_,_,_,_ PROCESS SETUP --> go to state 1
					// Else ERROR
					if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
						boolean parametersUpdated = false;
						try {
							parametersUpdated = validateSetupMessage(msg);
						} catch (NumberFormatException e) {
							System.out.println(name + ":" + state.name() + " - Bad message");
						}
						if (parametersUpdated)
							state = State.s1AwaitingGame;

					} else {
						System.out.println(name + ":" + state.name() + " - Unexpected message");
					}
					break;
				case s1AwaitingGame:

					if (msg.getPerformative() == ACLMessage.INFORM) {
						if (msg.getContent().startsWith("Id#")) { // Game settings updated
							try {
								validateSetupMessage(msg);
							} catch (NumberFormatException e) {
								System.out.println(name + ":" + state.name() + " - Bad message");
							}
						} else if (msg.getContent().startsWith("NewGame#")) {
							boolean gameStarted = false;
							try {
								gameStarted = validateNewGame(msg.getContent());
							} catch (NumberFormatException e) {
								System.out.println(name + ":" + state.name() + " - Bad message");
							}
							if (gameStarted)
								state = State.s2Round;
						}
					} else {
						System.out.println(name + ":" + state.name() + " - Unexpected message");
					}
					break;
				case s2Round:
					// If REQUEST POSITION --> INFORM POSITION --> go to state 3
					// If INFORM CHANGED stay at state 2
					// If INFORM ENDGAME go to state 1
					// Else error
					if (msg.getPerformative() == ACLMessage.REQUEST /* && msg.getContent().startsWith("Position") */) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(mainAgent);
						if (OpLastMove.equals("D")) {
							SpiteMode = 1;
						}
						if (SpiteMode == 1) {
							msg.setContent("Action#" + "D");
						} else {
							msg.setContent("Action#" + "C");
						} //cooperates till the opponent defeats, then spitemode is activated and always defeat
						System.out.println(name + " sent " + msg.getContent());
						send(msg);
						state = State.s3AwaitingResult;
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
						// Process changed message, in this case nothing
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver")) {
						OpLastMove = "F";
						SpiteMode = 0;
						state = State.s1AwaitingGame;
					} else {
						System.out.println(name + ":" + state.name() + " - Unexpected message:" + msg.getContent());
					}
					break;
				case s3AwaitingResult:
					// If INFORM RESULTS --> go to state 2
					// Else error
					if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
						// Process results
						String partes[] = msg.getContent().split("#");
						String acciones[] = partes[2].split(",");
						String ids[] = partes[1].split(",");

						if (Integer.parseInt(ids[0]) == myId) {
							if (acciones[1].equals("D")) {
								OpLastMove = acciones[1];
							}
						} else if (Integer.parseInt(ids[1]) == myId) {
							if (acciones[0].equals("D")) {
								OpLastMove = acciones[0];
							}
						}

						state = State.s2Round;
					} else {
						System.out.println(name + ":" + state.name() + " - Unexpected message");
					}
					break;
				}
			}
		}

		/**
		 * Validates and extracts the parameters from the setup message
		 *
		 * @param msg ACLMessage to process
		 * @return true on success, false on failure
		 */
		private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
			int tN, tS, tR, tI, tP, tMyId;
			String msgContent = msg.getContent();
			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 3)
				return false;
			if (!contentSplit[0].equals("Id"))
				return false;
			tMyId = Integer.parseInt(contentSplit[1]);

			String[] parametersSplit = contentSplit[2].split(",");
			tN = Integer.parseInt(parametersSplit[0]);
			tR = Integer.parseInt(parametersSplit[1]);

			// At this point everything should be fine, updating class variables
			mainAgent = msg.getSender();
			N = tN;
			R = tR;
			myId = tMyId;
			return true;
		}

		/**
		 * Processes the contents of the New Game message
		 * 
		 * @param msgContent Content of the message
		 * @return true if the message is valid
		 */
		public boolean validateNewGame(String msgContent) {
			int msgId0, msgId1;
			String[] contentSplit = msgContent.split("#");
			if (contentSplit.length != 3)
				return false;
			if (!contentSplit[0].equals("NewGame"))
				return false;
			String[] idSplit = contentSplit[1].split(",");
			if (idSplit.length != 1)
				return false;
			msgId0 = Integer.parseInt(contentSplit[1]);
			msgId1 = Integer.parseInt(contentSplit[2]);
			if (myId == msgId0) {
				opponentId = msgId1;
				return true;
			} else if (myId == msgId1) {
				opponentId = msgId0;
				return true;
			}
			return false;
		}
	}
}
