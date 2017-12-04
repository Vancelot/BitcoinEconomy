package BitcoinEcon;

import java.util.Collections;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Generator extends ViewableAtomic {

    protected Queue transQ;
    protected double numBitcoin;
    protected double marketPrice;

    protected double updatePriceTime;

    public Generator() {
        this("Generator", Market.INITIAL_BITCOIN_PRICE);
    }

    public Generator(String name, double marketPrice) {
        super(name);

        transQ = new Queue();

        addInport("inStop");
        addInport("inTransactions");

        addOutport("outPriceBitcoin");

        this.marketPrice = marketPrice;
    }

    public void initialize() {
        super.initialize();
        phase = "active";
        sigma = 1;

        transQ.clear();
    }

    public void deltext(double e, message x) {
        Continue(e);

        for (int i = 0; i < x.getLength(); i++)
            if (messageOnPort(x, "inTransactions", i)) {
                if (phaseIs("active")) {
                    // Retrieve Transaction
                    TransactionEntity message = (TransactionEntity) x.getValOnPort("inTransactions", i);
                    Transaction trans = message.getv();
                    transQ.add(trans);
                }
            } else if (messageOnPort(x, "inStop", i)) {
                passivate();
            }

        // Process all the transactions to get the weighted average price
        if (phaseIs("active") && !transQ.isEmpty()) {
            double weightedSumOfPrice = 0;
            double sumOfBitcoins = 0;

            while (!transQ.isEmpty()) {
                Transaction transaction = (Transaction) transQ.first();
                transQ.remove();

                // Make sure this transaction is not about expired orders
                if ((transaction.buyOrder != null) && (transaction.sellOrder != null)) {
                    double transPrice = transaction.price;
                    double transBitcoin = transaction.bitcoinAmount;

                    sumOfBitcoins += transBitcoin;
                    weightedSumOfPrice += transPrice * transBitcoin;
                }
            }
            if (sumOfBitcoins > 0)
                marketPrice = weightedSumOfPrice / sumOfBitcoins;

            holdIn("active", 1);
        }
    }

    public void deltint(message x) {
        // Price of Bitcoin from messages is summed with weight --> sumOfPrice
        // Weighted number of transactions --> weights
        // priceBitcoin = sumOfPrice / numBitcoin

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

        System.out.println("Generator: Bitcoin price " + marketPrice);
        
        return m;
    }

    public void showState() {
        super.showState();
    }
}
