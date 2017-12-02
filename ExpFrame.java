package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;

import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

public class ExpFrame extends ViewableDigraph {
    public static final int TOTAL_SIMULATION_TIME = 1856;

    public ExpFrame() {
        this("ExpFrame");
    }

    public ExpFrame(String name) {
        super(name);

        ViewableAtomic Generator = new Generator("Generator", 0.0649, 23274, 0);
        ViewableAtomic Transducer = new Transducer("Transducer", 5);

        add(Generator);
        add(Transducer);

        addInport("inTransaction");
        addInport("inHashRate");
        addInport("inNumBitcoin");

        addOutport("outPriceBitcoin");
        addOutport("outTime");
        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");

        addCoupling(this, "inTransaction", Generator, "inTransaction");
        addCoupling(this, "inHashRate", Transducer, "inHashRate");
        addCoupling(this, "inNumBitcoin", Transducer, "inNumBitcoin");

        addCoupling(Generator, "outPriceBitcoin", this, "outPriceBitcoin");

        addCoupling(Transducer, "outStop", Generator, "inStop");
        
        addCoupling(Transducer, "outTime", this, "outTime");
        addCoupling(Transducer, "outTotHashRate", this, "outTotHashRate");
        addCoupling(Transducer, "outTotNumBitcoin", this, "outTotNumBitcoin");

    }

    public void layoutForSimView() {
        preferredSize = new Dimension(700, 160);
        ((ViewableComponent) withName("Transducer")).setPreferredLocation(new Point(0, 75));
        ((ViewableComponent) withName("Generator")).setPreferredLocation(new Point(0, 20));
    }
}
