package bitcoinecon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Agent extends ViewableAtomic {

	protected Queue procQ;
	protected int id;
	protected float bitcoinPrice; 
	protected float numBitcoin;
	protected float cash;
	protected boolean enterMarket;	
	//types of agents are decided when agent enters the market
	
	
	public Agent() {
		this("Agent",0,0,0,false);
	}
		
		public Agent(String name, int id, float cash, float numBitcoin, boolean enterMarket) {
			super (name);
			procQ = new Queue();
			addInport("TPort");
			addInport("PBPort");
			addOutport("OrderPort");
			addTestInput("TPort", new entity("Transaction"));
			addTestInput("PBPort", new entity("Price"));
			this.id = id;
			this.cash = cash;
			this.numBitcoin = numBitcoin;
			this.enterMarket = enterMarket;
		}
	
		public void initialize() {
			super.initialize();
			procQ = new Queue();
			bitcoinPrice = 0;
			sigma = INFINITY;
			phase = "passive";
		}
			
		public void deltext(double e, message x) {
			Continue(e);
			
			if (phaseIs("passive")) {
				for (int i = 0; i < x.getLength(); i++)
					if (messageOnPort(x, "PBPort", i)){
						holdIn("ordering", 100);
					}
						else if (messageOnPort(x, "TPort", i)){
							holdIn("updatingTrans", 50);
						}
			}
			
			if (phaseIs("passive")) {
				for (int i = 0; i < x.getLength(); i++)
					if (messageOnPort(x, "PBPort", i)){
					holdIn("updatingPrice", 100);
					}
			}
		}
		
		public void deltint() {
			 passivate();
			 //wake up agent to enter market
		}
		
		public void deltcon(double e, message x) {
			System.out.println("confluent");
			deltint();
			deltext(0, x);
		}
		
		public message out() {

			message m = new message();
					
			content con = makeContent("OrderPort", new entity("Order"));		
			if (phaseIs ("ordering")) {
				m.add(con);	
			}
						
			return m;		
		}
		
		public void showState() {
			super.showState();
		}
		
}



