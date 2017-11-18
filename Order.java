package BitcoinEcon;

public class Order {
    public enum OrderType {
        NONE, BUY, SELL
    }

    OrderType orderType;
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
        this(OrderType.NONE, 0, 0.0, 0.0, 0.0, 0);
    }
    
    public Order(OrderType aOrderType, int aAgentId, double aAmount, double aResidualAmount, double aLimitPrice,
            int aExpirationPeriod) {
        this.orderType = aOrderType;
        this.agentId = aAgentId;
        this.amount = aAmount;
        this.residualAmount = aResidualAmount;
        this.limitPrice = aLimitPrice;
        this.expirationPeriod = aExpirationPeriod;
    }
}
