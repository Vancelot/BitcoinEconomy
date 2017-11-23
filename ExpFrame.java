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

        add(Generator);
        add(Transducer);

        addInport("inHashRate");
        addInport("inNumBitcoin");
        addInport("inTransaction");

        addCoupling(this, "inHashRate", Transducer, "inHashRate");
        addCoupling(this, "inNumBitcoin", Transducer, "inNumBitcoin");
        addCoupling(this, "inTransaction", Generator, "inTransaction");

        addCoupling(Transducer, "out", Generator, "Stop");
    }
    
    public void layoutForSimView() {
        preferredSize = new Dimension(950, 400);
        ((ViewableComponent) withName("Transducer")).setPreferredLocation(new Point(10, 150));
        ((ViewableComponent) withName("Generator")).setPreferredLocation(new Point(300, 50));
    }
}
