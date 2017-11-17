package BitcoinEcon;

import java.lang.*;

import GenCol.entity;

public class OrderEntity extends entity {
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

    public OrderEntity(OrderType aOrderType, int aAgentId, double aAmount, double aLimitPrice, int aExpirationPeriod) {
        this.orderType = aOrderType;
        this.agentId = aAgentId;
        this.amount = aAmount;
        this.residualAmount = 0;
        this.limitPrice = aLimitPrice;
        this.expirationPeriod = aExpirationPeriod;
    }

    public boolean greaterThan(entity ent) {
        return (this.amount > ((OrderEntity) ent).getv());
    }

    public void setv(double t) {
        amount = t;
    }

    public double getv() {
        return amount;
    }

    public void print() {
        System.out.print(amount);
    }

    public boolean equal(entity ent) {
        // System.out.println(v + " " + ((doubleEnt)ent).getv());
        return (Math.abs(this.amount - ((OrderEntity) ent).getv()) < 0.0000001);
    }

    public boolean equals(Object ent) { // needed for Relation
        if (!(ent instanceof OrderEntity))
            return false;
        return equal((entity) ent);
    }

    public entity copy() {
        OrderEntity ip = new OrderEntity(orderType, agentId, amount, limitPrice, expirationPeriod);
        return (entity) ip;
    }

    public String getName() {
        return Double.toString(amount);
    }

}
