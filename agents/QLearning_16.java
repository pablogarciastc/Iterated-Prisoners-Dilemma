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
import java.util.Vector;

public class QLearning_16 extends Agent {

	final static double dDecFactorLR = 0.999; // Value that will decrement the learning rate in each generation
	final static double dEpsilon = 0.8; // Used to avoid selecting always the best action
	final static double dMINLearnRate = 0.05; // We keep learning, after convergence, during 5% of times

	double reward = 0;
	boolean bAllActions = false; // At the beginning we did not try all actions
	int iNewAction2Play; // This is the new action to be played
	static int iNumActions = 2; // For C or D for instance
	int iLastAction; // The last action that has been played by this player
	int[] iNumTimesAction = new int[iNumActions]; // Number of times an action has been played
	double[] dPayoffAction = new double[iNumActions]; // Accumulated payoff obtained by the different actions
	static StateAction oPresentStateAction; // Contains the present state we are and the actions that are available
	static StateAction oLastStateAction; // Contains the present state we are and the actions that are available
	static StateAction[] oVStateActions = new StateAction[5]; // A vector containing strings with the possible States
																// and Actions
	// available at each one
	static double dLastFunEval = 0;
	static double dLearnRate = 0.08;
	static double dGamma = 0.5;
	static int iAction = 1;
	static int iNewAction = -1;
	static int pos = 0;

	private State state;
	private AID mainAgent;
	private int myId, opponentId;
	private int N, R;
	private static StateAction OpLastMove = new StateAction("F", 2);
	private StateAction MyLastMove = new StateAction("F", 2);;
	private ACLMessage msg;
	private String name;


	/**
	 * setups the agent
	 */
	protected void setup() {
		oVStateActions[0] = new StateAction("FF", 2);
		oVStateActions[1] = new StateAction("CD", 2);
		oVStateActions[2] = new StateAction("DC", 2);
		oVStateActions[3] = new StateAction("DD", 2);
		oVStateActions[4] = new StateAction("CC", 2); //Declarating the possible states
		state = State.s0NoConfig;
		String[] names = getAID().getName().split("@");
		name = names[0]; //to get only the first part, not the part after the @
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
		System.out.println("QLearning " + name + " is ready.");

	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		System.out.println("QLearning " + name + " terminating.");
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
						// msg.setContent("Position#" + random.nextInt(S));
						int numero = (int) (Math.random() * 2);

						String choice = vGetNewActionQLearning(MyLastMove.sGetState() + OpLastMove.sGetState(), 2,
								reward); //asks the method for an action, parameters the actual state, the number of actions and the reward of the agent in the last action
						msg.setContent(choice);
						System.out.println(name + " sent " + msg.getContent());
						send(msg);
						state = State.s3AwaitingResult;
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
						// Process changed message, in this case nothing
					} else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver")) {
						OpLastMove.sSetState("F");
						MyLastMove.sSetState("F");

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
							OpLastMove.sSetState(acciones[1]);
							MyLastMove.sSetState(acciones[0]);

						} else if (Integer.parseInt(ids[1]) == myId) {
							OpLastMove.sSetState(acciones[0]);
							MyLastMove.sSetState(acciones[1]);

						}

						reward = Reward(MyLastMove.sGetState(), OpLastMove.sGetState());

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
		 * 
		 */

		public String vGetNewActionQLearning(String sState, int iNActions, double dFunEval) {

			boolean bFound;
			int iBest = -1, iNumBest = 1;
			double dR, dQmax;
			StateAction oStateAction;

			bFound = false;
			//gets the present state action
			for (int i = 0; i < oVStateActions.length; i++) { //
				oStateAction = (StateAction) oVStateActions[i];
				if (!(oStateAction == null)) {
					if (oStateAction.sState.equals(sState)) {
						oPresentStateAction = oStateAction;
						bFound = true;
						break;
					}
				}
			}

			dQmax = 0;
			for (int i = 0; i < iNActions; i++) { // Determining the action to get Qmax{a'}
				if (oPresentStateAction.dValAction[i] > dQmax) {
					iBest = i;
					iNumBest = 1; // Reseting the number of best actions
					dQmax = oPresentStateAction.dValAction[i];
				} else if ((oPresentStateAction.dValAction[i] == dQmax) && (dQmax > 0)) { // If there is another one
																							// equal
																							// we must select one of
																							// them
																							// randomly
					iNumBest++;
					if (Math.random() < 1.0 / (double) iNumBest) { // Choose randomly with reducing probabilities
						iBest = i;
						dQmax = oPresentStateAction.dValAction[i];
					}
				}
			}
			// Adjusting Q(s,a)
			if (oLastStateAction != null) {
				dR = dFunEval - dLastFunEval; // Note that dR is also used as reward in the QL formulae
				 if (dR > -1)
					oLastStateAction.dValAction[iAction] += dLearnRate
							* (dFunEval + dGamma * dQmax - oLastStateAction.dValAction[iAction]);
				// If reward grows and the previous action was allowed --> reinforce the
				// previous action considering present values
			}

			if ((iBest > -1) && (Math.random() > dEpsilon)) // Using the e-greedy policy to select the best action or
															// any of
															// the rest
				iNewAction = iBest;
			else
				do { //taking an action randomly
					iNewAction = (int) (Math.random() * (double) iNumActions);
				} while (iNewAction == iBest);

			oLastStateAction = oPresentStateAction; // Updating values for the next time
			dLastFunEval = dFunEval;
			dLearnRate *= dDecFactorLR; // Reducing the learning rate
			if (dLearnRate < dMINLearnRate)
				dLearnRate = dMINLearnRate;
			String myMove = "F";

			if (iNewAction == 0) {
				myMove = "Action#C";
			} else if (iNewAction == 1) {
				myMove = "Action#D";
			}

			iAction = iNewAction;

			return myMove;
		}

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

		public double Reward(String MyAction, String OpAction) {

			if (MyAction.equals("C") && OpAction.equals("C"))
				return 0.5;
			if (MyAction.equals("C") && OpAction.equals("D"))
				return 0;
			if (MyAction.equals("D") && OpAction.equals("D"))
				return 0.2;
			if (MyAction.equals("D") && OpAction.equals("C"))
				return 1;

			return 0;
		}

	}
}
