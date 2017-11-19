package BitcoinEcon;

public class Order implements Comparable<Order> {
    public enum OrderType {
        NONE, BUY, SELL
    }

    OrderType type;
    public int agentId;
    public double amount; // $ if BUY or Bitcoins if SELL
    public double residualAmount; // $ if BUY or Bitcoins if SELL,
                                  // Used when an order is partially completed by the previous matching
                                  // transaction
                                  // by the Order book
    public double limitPrice; // The price to which a trader desires to conclude their transaction
    public int expirationPeriod; // If the order is not fully satisfied it is removed from the book after
                                 // expiration period

    public Order() {
        this(OrderType.NONE, 0, 0.0, 0.0, 0);
    }

    public Order(OrderType aOrderType, int aAgentId, double aAmount, double aLimitPrice, int aExpirationPeriod) {
        this.type = aOrderType;
        this.agentId = aAgentId;
        this.amount = aAmount;
        this.residualAmount = aAmount; // Initial Residual Amount is always initialized to the Amount
        this.limitPrice = aLimitPrice;
        this.expirationPeriod = aExpirationPeriod;
    }

    @Override
    public int compareTo(Order other) {
        int compare = 0;
        if (type == OrderType.BUY) { // Descending order
            compare = (this.limitPrice < other.limitPrice ? 1 : (this.limitPrice == other.limitPrice ? 0 : -1));
        } else {
            compare = (this.limitPrice < other.limitPrice ? -1 : (this.limitPrice == other.limitPrice ? 0 : 1));
        }
        return compare;
    }

    @Override
    public String toString() {
        return " Order: " + this.type + ", Limit Price: " + this.limitPrice;
    }

}
