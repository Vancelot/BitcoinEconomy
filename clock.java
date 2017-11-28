package BitcoinEcon;

import java.util.Collections;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class clock extends ViewableAtomic {

    protected Double marketTime;

    public clock() {
        this("clock");
    }

    public clock(String name) {
        super(name);

        addInport("Stop");

        addOutport("outTime");

    }

    public void initialize() {
        super.initialize();
        phase = "active";
        sigma = 1;
        marketTime = 0.0;
    }

    public void deltext(double e, message x) {
        Continue(e);

        if (phaseIs("active")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "Stop", i))
                    passivate();
        }
    }

    public void deltint(message x) {

        if (phaseIs("active")) {
            if (marketTime == 0) {
                holdIn("outTime", 1);
               
            } else if (marketTime > 0) {
                holdIn("outTime", 1);
                
            }
        }

        else
            passivate();

    }

    public message out() {

        message m = new message();

        content con = makeContent("outTime", new entity("none"));

        if (marketTime == 0)
            con = makeContent("outTime", new entity(marketTime.toString()));
        else
            for (int i = 1; i < 80; i++)
                if (marketTime == i)
                    con = makeContent("outTime", new entity(marketTime.toString()));

        m.add(con);
        marketTime++;

        return m;
    }

    public void showState() {
        super.showState();
    }
}
