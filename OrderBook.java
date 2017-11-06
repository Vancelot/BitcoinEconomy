package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import GenCol.Queue;
import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

public class OrderBook extends ViewableAtomic {

	protected Queue matchQ;
	protected int id;
	protected float numBitcoin;
	protected float cash;
	protected boolean match;
	
	public OrderBook() {
		this("OrderBook",0,0,0,0,0,false);
	}
	
		public OrderBook(
				String name,
				int idKey,
				int idSell,
				int idBuy,
				float numBitcoin, 
				float cash,
				boolean match) 
		{
			super(name);
			matchQ = new Queue();
			addInport("InOrders");
			addOutport("OutTransactions");
			this.idKey = idKey;
			this.idSell = idSell;
			this.idBuy = idBuy;
			this.numBitcoin = numBitcoin;
			this.cash = cash;			
			this.match = match;
		}
	
		public void initialize() {
			super.initialize();
			matchQ = new Queue();
			sigma = INFINITY;
			phase = "passive";		
		}
		
		public void deltext(double e, message x) {
			Continue(e);
			
			if (phaseIs("passive")) {
				for (int i = 0; i < x.getLength(); i++)
					if (messageOnPort(x, "Orders", i)){
						holdIn("mathcing", 100);
					}
			}
		}
		
		public void deltint() {
			passivate();			
		}
		
		public message out() {

			message m = new message();
					
			content con = makeContent("Transactions", new entity("Transaction"));		
			if (phaseIs ("matching")) {
				m.add(con);	
			}
						
			return m;		
		}
		
		public void showState() {
			super.showState();
		}
		
}
