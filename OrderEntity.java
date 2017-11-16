package BitcoinEcon;

import java.lang.*;

import GenCol.entity;

public class OrderEntity extends entity {
    private int buyerId;
    private int sellerId;
    private double bitcoins;

    public OrderEntity(double t) {
        bitcoins = t;

    }

    public boolean greaterThan(entity ent) {
        return (this.v > ((OrderEntity) ent).getv());
    }

    public void setv(double t) {
        v = t;
    }

    public double getv() {
        return v;
    }

    public void print() {
        System.out.print(v);
    }

    public boolean equal(entity ent) {
        // System.out.println(v + " " + ((doubleEnt)ent).getv());
        return (Math.abs(this.v - ((OrderEntity) ent).getv()) < 0.0000001);
    }

    public boolean equals(Object ent) { // needed for Relation
        if (!(ent instanceof OrderEntity))
            return false;
        return equal((entity) ent);
    }

    public entity copy() {
        OrderEntity ip = new OrderEntity(getv());
        return (entity) ip;
    }

    public String getName() {
        return Double.toString(v);
    }

}
