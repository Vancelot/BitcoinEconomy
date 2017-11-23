package BitcoinEcon;

import BitcoinEcon.Order.OrderType;

public class Transaction {
    public Order buyOrder;
    public Order sellOrder;
    public double price;
    
    public Transaction() {
        this(new Order(), new Order(), 0);
    }

    public Transaction(Order aBuyOrder, Order aSellOrder, double price) {
        this.buyOrder = aBuyOrder;
        this.sellOrder = aSellOrder;
        this.price = price;
    }
}
