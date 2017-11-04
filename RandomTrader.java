package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class RandomTrader extends Agent {

    public RandomTrader() {
        super();
        addInport("TPort");
        addInport("PBPort");
        addOutport("OrderPort");
        addTestInput("TPort", new entity("Transaction"));
        addTestInput("PBPort", new entity("Price"));
    }

    public void initialize() {
        super.initialize();
        sigma = INFINITY;
        phase = "passive";
    }

    public void deltext(double e, message x) {
        Continue(e);

        if (phaseIs("ordering")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "TPort", i)) {
                    holdIn("passive", INFINITY);
                } else if (messageOnPort(x, "PBPort", i)) {
                    holdIn("updating", 50);
                }
        }

        if (phaseIs("updating")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "inRed", i)) {
                    holdIn("ordering", 50);
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
        if (phaseIs("ordering")) {
            m.add(con);
        }

        return m;
    }

    public void showState() {
        super.showState();
    }

}
