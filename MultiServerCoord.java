package BitcoinEcon;

import GenCol.*;
import view.modeling.ViewableAtomic;
import model.modeling.*;
import model.simulation.*;
import view.simView.*;

public class MultiServerCoord extends ViewableAtomic {

    protected Queue transactionQ;
    protected Queue orderQ;

    protected entity bitcoinPriceMessage;
    protected entity timeMessage;
    
    public MultiServerCoord() {
        this("MultiServerCoord");
    }

    public MultiServerCoord(String name) {
        super(name);

        transactionQ = new Queue();
        orderQ = new Queue();

        addInport("inTransactions");
        addInport("inBitcoinPrice");
        addInport("inOrders");
        addInport("inTimer");

        // TODO - add test inputs

        addInport("outTransactions");
        addInport("outBitcoinPrice");
        addInport("outOrders");
        addInport("outTimer");
    }

    public void initialize() {
        phase = "passive";
        sigma = INFINITY;

        super.initialize();
        
        bitcoinPriceMessage = null;
        timeMessage = null;
    }

    public void showState() {
        super.showState();
        System.out.println("number of Orders: " + orderQ.size());
        System.out.println("number of Transactions: " + transactionQ.size());
    }

    public void deltext(double e, message x) {

        Continue(e);

        if (phaseIs("passive")) {
            for (int i = 0; i < x.size(); i++) {
                if (messageOnPort(x, "inTransactions", i)) {
                    entity val = x.getValOnPort("inTransactions", i);
                    transactionQ.add(val);

                    holdIn("send_out", 0);
                } else if (messageOnPort(x, "inBitcoinPrice", i)) {
                    entity bitcoinPriceMessage = x.getValOnPort("inBitcoinPrice", i);

                    holdIn("send_out", 0);
                } else if (messageOnPort(x, "inTimer", i)) {
                    entity timeMessage = x.getValOnPort("inTimer", i);

                    holdIn("send_out", 0);
                } else if (messageOnPort(x, "inOrders", i)) {
                    entity val = x.getValOnPort("inOrders", i);
                    orderQ.add(val);

                    holdIn("send_out", 0);
                }
            }
        }
    }

    public void deltint() {
            passivate();
    }

    public message out() {
        message m = new message();
        if (phaseIs("send_out")) {
            for (int i = 0; i < transactionQ.size(); i++) {
                entity transactionEntity = (entity) transactionQ.get(i);
                m.add(makeContent("outTransactions", transactionEntity));
            }
            transactionQ = new Queue();
            
            if (bitcoinPriceMessage != null) {
                m.add(makeContent("outBitcoinPrice", bitcoinPriceMessage));
                bitcoinPriceMessage = null;
            }
            
            if (timeMessage != null) {
                m.add(makeContent("outTimer", timeMessage));
                timeMessage = null;
            }
            
            for (int i = 0; i < orderQ.size(); i++) {
                entity orderEntity = (entity) orderQ.get(i);
                m.add(makeContent("outOrders", orderEntity));
            }
            orderQ = new Queue();
        }

        return m;
    }

}