package bitcoinecon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Agent extends ViewableAtomic {

	protected Queue procQ;
	protected float bitcoinPrice; 
	protected float numBitcoin;
	protected float cash;
	protected boolean enterMarket;	
	
	public Agent() {
		this("Agent");
	}
		
		public Agent(String name) {
			super (name);
			procQ = new Queue();
			addInport("TPort");
			addInport("PBPort");
			addOutport("OrderPort");
			addTestInput("TPort", new entity("Transaction"));
			addTestInput("PBPort", new entity("Price"));		
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




