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
        this("Generator", 0.0649, 23274, 1);
    }

    public Generator(String name, double marketPrice, double numBitcoin, double updatePriceTime) {
        super(name);

        addInport("inStop");
        addInport("inTransactions");

        addOutport("outPriceBitcoin");

        this.marketPrice = marketPrice;
        this.numBitcoin = numBitcoin;
        this.updatePriceTime = updatePriceTime;
    }

    public void initialize() {
        super.initialize();
        phase = "active";
        sigma = 1;
        
        transQ = new Queue();
    }

    public void deltext(double e, message x) {
        Continue(e);

        for (int i = 0; i < x.getLength(); i++)
            if (messageOnPort(x, "inTransactions", i)) {
                if (phaseIs("active")) {
                    // Retrieve Transaction
                    TransactionEntity message = (TransactionEntity) x.getValOnPort("inTransactions", i);
                    Transaction order = message.getv();

                    // Extract price of Bitcoin from Transaction

                    transPriceOfBitcoin = order.price;

                    // Extract number of Bitcoin from Transaction

                    Order aBuyOrder = order.buyOrder;
                    transNumBitcoin = aBuyOrder.amount;

                    holdIn("updatePrice", updatePriceTime);
                } else if (phaseIs("updatePrice")) {
                    transaction = x.getValOnPort("inTransactions", i);
                    transQ.add(transaction);

                    transaction = (entity) transQ.first();
                }
            } else if (messageOnPort(x, "inStop", i)) {
                passivate();
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
        message m = new message();

        content con = makeContent("outPriceBitcoin", new entity(String.valueOf(marketPrice)));

        m.add(con);

        return m;
    }

    public void showState() {
        super.showState();
    }
}
