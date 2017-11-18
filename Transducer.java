package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Transducer extends ViewableAtomic {

    protected int modelTime;

    public Transducer() {
        this("Transducer", 1856);
    }

    public Transducer(String name, int modelTime) {
        super(name);

        addInport("inHashRate");
        addInport("inNumBitcoin");

        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");
        addOutport("out");

        this.modelTime = modelTime;
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

                if (messageOnPort(x, "inHashRate", i)) {
                    holdIn("updateHashRate", 0);
                    // TODO: extract current hash rate from message
                    // Remove old and hash rate
                    // Sum current total hash rate

                } else if (messageOnPort(x, "inNumBitcoin", i)) {
                    holdIn("updateNumBitcoin", 0);
                    // TODO: extract current number of Bitcoin from message
                    // Remove old and number of Bitcoin
                    // Sum current total number of Bitcoin
                }
        }
    }

    public void deltint() {
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

        m.add(con1);
        m.add(con2);
        m.add(con3);

        return m;
    }
    
    public void showState() {
        super.showState();
    }
}
