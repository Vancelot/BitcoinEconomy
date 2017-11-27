package BitcoinEcon;

import BitcoinEcon.Order.OrderType;

public class Transaction {
    public Order buyOrder;
    public Order sellOrder;
    public double price;
    public double bitcoinAmount;
    
    public Transaction() {
        this(new Order(), new Order(), 0, 0);
    }

    public Transaction(Order aBuyOrder, Order aSellOrder, double price, double bitcoinAmount) {
        this.buyOrder = aBuyOrder;
        this.sellOrder = aSellOrder;
        this.price = price;
        this.bitcoinAmount = bitcoinAmount;
    }
}
