package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Generator extends ViewableAtomic {

    protected message transaction;
    protected double priceOfBitcoin;

    public Generator() {
        this("Generator", 0.0649);
    }

    public Generator(String name, double priceOfBitcoin) {
        super(name);

        addInport("Stop");
        addInport("inTransaction");

        addOutport("outPriceBitcoin");
        
        this.priceOfBitcoin = priceOfBitcoin;
    }

    public void initialize() {
        super.initialize();
        phase = "passive";
        sigma = INFINITY;
    }

    public void deltext(double e, message x) {
        Continue(e);

        if (phaseIs("passive")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "inTransaction", i)) {
                    holdIn("updatePrice", 0);
                }
        }
    }

    public void deltint(message x) {
        // Price of Bitcoin from messages is summed with weight --> sumOfPrice
        // Weighted number of transactions --> weights
        // priceBitcoin = sumOfPrice / weights
        
        if (phaseIs("updatePrice")) {
            for (int i = 0; i < x.getLength(); i++)
            if (x.getValOnPort("inRed", i).eq("transaction"))
                transaction = x;
            
            double priceFromTrans = 0; // TODO: Extract price from transaction message
            double sumOfPrice = 0;
                sumOfPrice = sumOfPrice + priceFromTrans; // TODO: Extract number of Bitcoin from transaction message
            int weights = 0;
            
            priceOfBitcoin = sumOfPrice / weights;
            
                
        }
        passivate();
        
        
    }
    
    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltint();
        deltext(0, x);
    }
    
    public message out() {
        // TODO: Messages need to have price of Bitcoin

        message m = new message();

        content con = makeContent("outPriceBitcoin", new entity("priceBitcoin"));
        if (phaseIs("ordering")) {
            m.add(con);
        }

        return m;
    }
    
    public void showState() {
        super.showState();
    }
}

