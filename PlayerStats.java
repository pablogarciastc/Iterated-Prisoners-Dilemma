import jade.core.AID;
import agents.*;

public class PlayerStats {
	
	String name;
	float defeat;
	float coop;
	double points;
	AID aid;
	float defeatPCTGE , coopPCTGE;
	
	

	public PlayerStats(String name, float defeat, float coop, double points, AID aid , float defeatPCTGE , float coopPCTGE) {
		this.name = name;
		this.defeat = defeat;
		this.coop = coop;
		this.points = points;
		this.aid = aid;
		this.defeatPCTGE = defeatPCTGE;
		this.coopPCTGE = coopPCTGE;
	}

	public AID getAid() {
		return aid;
	}

	public void setAid(AID aid) {
		this.aid = aid;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public float getDefeat() {
		return defeat;
	}
	public void setDefeat(float defeat) {
		this.defeat = defeat;
	}
	public float getCoop() {
		return coop;
	}
	public void setCoop(float coopp1) {
		this.coop = coopp1;
	}
	public double getPoints() {
		return points;
	}
	public void setPoints(double f) {
		this.points = f;
	}

	public float getDefeatPCTGE() {
		return defeatPCTGE;
	}

	public void setDefeatPCTGE(float defeatPCTGE) {
		this.defeatPCTGE = defeatPCTGE;
	}

	public float getCoopPCTGE() {
		return coopPCTGE;
	}

	public void setCoopPCTGE(float coopPCTGE) {
		this.coopPCTGE = coopPCTGE;
	}
	
	

}
