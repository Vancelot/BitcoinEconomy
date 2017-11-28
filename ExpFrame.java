package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;

import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

public class ExpFrame extends ViewableDigraph {

    public ExpFrame() {
        this("ExpFrame");
    }

    public ExpFrame(String name) {
        super(name);

        ViewableAtomic Generator = new Generator("Generator", 0.0649, 23274, 0);
        ViewableAtomic Transducer = new Transducer("Transducer", 1856);
        ViewableAtomic clock = new clock("clock");

        add(Generator);
        add(Transducer);
        add(clock);

        addInport("inHashRate");
        addInport("inTransaction");

        addOutport("outPriceBitcoin");
        addOutport("outTime");
        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");

        addCoupling(this, "inHashRate", Transducer, "inHashRate");
        addCoupling(this, "inTransaction", Generator, "inTransaction");

        addCoupling(Transducer, "out", Generator, "Stop");
        addCoupling(Transducer, "out", clock, "Stop");
        
        addCoupling(clock, "outTime", Transducer, "inTime");

        addCoupling(Generator, "outPriceBitcoin", this, "outPriceBitcoin");
        addCoupling(clock, "outTime", this, "outTime");
        addCoupling(Transducer, "outTotHashRate", this, "outTotHashRate");
        addCoupling(Transducer, "outTotNumBitcoin", this, "outTotNumBitcoin");

    }

    public void layoutForSimView() {
        preferredSize = new Dimension(700, 160);
        ((ViewableComponent) withName("Transducer")).setPreferredLocation(new Point(0, 75));
        ((ViewableComponent) withName("Generator")).setPreferredLocation(new Point(0, 20));
        ((ViewableComponent) withName("clock")).setPreferredLocation(new Point(300, 20));
    }
}
