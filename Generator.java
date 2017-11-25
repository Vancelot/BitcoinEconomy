package BitcoinEcon;

import java.util.Collections;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Generator extends ViewableAtomic {

    protected Queue transQ;

    protected entity transaction;

    protected double transPriceOfBitcoin;
    protected double transNumBitcoin;
    protected double numBitcoin;
    protected Double marketPrice;

    protected double updatePriceTime;

    public Generator() {
        this("Generator", 0.0649, 23274, 0);
    }

    public Generator(String name, double marketPrice, double numBitcoin, double updatePriceTime) {
        super(name);

        addInport("Stop");
        addInport("inTransaction");

        addOutport("outPriceBitcoin");

        this.marketPrice = marketPrice;
        this.numBitcoin = numBitcoin;
        this.updatePriceTime = updatePriceTime;
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

                    // Retrieve Transaction

                    TransactionEntity message = (TransactionEntity) x.getValOnPort("InTransaction", i);
                    Transaction order = message.getv();

                    // Extract price of Bitcoin from Transaction

                    transPriceOfBitcoin = order.price;

                    // Extract number of Bitcoin from Transaction

                    Order aBuyOrder = order.buyOrder;
                    transNumBitcoin = aBuyOrder.amount;

                    holdIn("updatePrice", updatePriceTime);

                } else if (messageOnPort(x, "Stop", i)) {
                    phase = "finishing";
                }
        }

        // TODO: put messages in queue
        if (phaseIs("updatePrice")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "inTransaction", i)) {
                    transaction = x.getValOnPort("inTransaction", i);
                    transQ.add(transaction);
                }

            transaction = (entity) transQ.first();
        }
    }

    public void deltint(message x) {
        // Price of Bitcoin from messages is summed with weight --> sumOfPrice
        // Weighted number of transactions --> weights
        // priceBitcoin = sumOfPrice / numBitcoin

        if ((phaseIs("updatePrice")) && !transQ.isEmpty()) {
            transaction = (entity) transQ.first(); // TODO: Does this begin updating price?
            holdIn("updatePrice", updatePriceTime);

            transQ.remove();

            double weightedSumOfPrice = 0;
            weightedSumOfPrice = (numBitcoin * marketPrice) + (transNumBitcoin * transPriceOfBitcoin);

            marketPrice = weightedSumOfPrice / numBitcoin + transNumBitcoin;

        } else
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

        content con = makeContent("outPriceBitcoin", new entity(marketPrice.toString())); // TODO: message needs to send
                                                                                          // the
        // price of Bitcoin
        if (phaseIs("updatePrice")) {
            m.add(con);
        }

        return m;
    }

    public void showState() {
        super.showState();
    }
}
