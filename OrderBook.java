package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import GenCol.entity;
import GenCol.Queue;
import model.modeling.content;
import model.modeling.message;
import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

public class OrderBook extends ViewableAtomic {

    protected Queue matchQ;
    protected boolean match;

    public OrderBook() {
        this("OrderBook");
    }

    public OrderBook(String name) {
        super(name);
        matchQ = new Queue();
        addInport("InOrders");
        addOutport("OutTransactions");
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
                if (messageOnPort(x, "InOrders", i)) {
                    holdIn("matching", 100);
                }
        }
    }

    public void deltint() {
        passivate();
    }

    public message out() {

        message m = new message();

        content con = makeContent("OutTransactions", new entity("Transaction"));
        if (phaseIs("matching")) {
            m.add(con);
        }

        return m;
    }

    public void showState() {
        super.showState();
    }

}
