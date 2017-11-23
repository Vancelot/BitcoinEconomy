package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Transducer extends ViewableAtomic {

    protected Queue hashRateQ;
    
    protected Double modelTime;
    

    public Transducer() {
        this("Transducer", 1856);
    }

    public Transducer(String name, int modelTime) {
        super(name);

        addInport("inHashRate");
        addInport("inNumBitcoin");

        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");
        addOutport("outTime");
        addOutport("out");

        this.modelTime = (double) modelTime;
    }

    public void initialize() {
        super.initialize();
        phase = "passive";
        sigma = INFINITY;
    }

    public void deltext(double e, message x) {
        modelTime = modelTime + e;
        Continue(e);

       
        if (phaseIs("passive")) {
            for (int i = 0; i < x.getLength(); i++)

                if (messageOnPort(x, "inHashRate", i)) {
                    holdIn("updateHashRate", 0);
                    // TODO: extract current hash rate from message
                    // Remove old and hash rate
                    // Sum current total hash rate

                } else if (messageOnPort(x, "inNumBitcoin", i)) { //**** may not need *****
                    holdIn("updateNumBitcoin", 0);
                    // TODO: extract current number of Bitcoin from message
                    // Remove old and number of Bitcoin
                    // Sum current total number of Bitcoin
                }
        }
    }

    public void deltint() {
        modelTime = modelTime + sigma;
        passivate();
    }

    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltext(e, x);
        deltint();
    }

    public message out() {

        message m = new message();

        content con1 = makeContent("totHashRate", new entity()); // TODO: message for totHashRate
        content con2 = makeContent("totNumBitcoin", new entity()); // TODO: message for totNumBitcoin
        content con3 = makeContent("out", new entity("Stop"));
        content con4 = makeContent("outTime", new entity(modelTime.toString()));

        m.add(con1);
        m.add(con2);
        m.add(con3);
        m.add(con4);

        return m;
    }
    
    public void showState() {
        super.showState();
    }
}
