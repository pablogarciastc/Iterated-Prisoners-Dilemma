package agents;
import jade.util.leap.Serializable;

public class StateAction implements Serializable
{
private static final long serialVersionUID = 1L;
String sState;
double[] dValAction;

StateAction (String sAuxState, int iNActions) {
  sState = sAuxState;
  dValAction = new double[iNActions];
  }

StateAction (String sAuxState, int iNActions, boolean bLA) {
  this (sAuxState, iNActions);
  if (bLA) for (int i=0; i<iNActions; i++)	// This constructor is used for LA and sets up initial probabilities
    dValAction[i] = 1.0 / iNActions;
  }


public String sGetState() {
  return sState;
}


public void sSetState(String state) {
	sState = state;
}

public double dGetQAction (int i) {
  return dValAction[i];
}
}
