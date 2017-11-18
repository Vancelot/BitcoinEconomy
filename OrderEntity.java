package BitcoinEcon;

import java.lang.*;

import GenCol.entity;

public class OrderEntity extends entity {
    private Order order;

    public OrderEntity(Order aOrder) {
        this.order = aOrder;
    }

    public boolean greaterThan(entity ent) {
        return (this.order.amount > ((OrderEntity) ent).getv().amount);
    }

    public void setv(Order t) {
        order = t;
    }

    public Order getv() {
        return order;
    }

    public void print() {
        System.out.print(order);
    }

    public boolean equal(entity ent) {
        // System.out.println(v + " " + ((doubleEnt)ent).getv());
        return (Math.abs(this.order.amount - ((OrderEntity) ent).getv().amount) < 0.0000001);
    }

    public boolean equals(Object ent) { // needed for Relation
        if (!(ent instanceof OrderEntity))
            return false;
        return equal((entity) ent);
    }

    public entity copy() {
        OrderEntity ip = new OrderEntity(order);
        return (entity) ip;
    }

    // This function should not be used
    public String getName() {
        return Double.toString(order.amount);
    }

}
