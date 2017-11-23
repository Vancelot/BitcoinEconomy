package BitcoinEcon;

import java.lang.*;

import GenCol.entity;

public class TransactionEntity extends entity {
    private Transaction transaction;

    public TransactionEntity(Transaction aTransaction) {
        this.transaction = aTransaction;
    }

    public boolean greaterThan(entity ent) {
        return (this.transaction.price > ((TransactionEntity) ent).getv().price);
    }

    public void setv(Transaction t) {
        transaction = t;
    }

    public Transaction getv() {
        return transaction;
    }

    public void print() {
        System.out.print(transaction);
    }

    public boolean equal(entity ent) {
        // System.out.println(v + " " + ((doubleEnt)ent).getv());
        return (Math.abs(this.transaction.price - ((TransactionEntity) ent).getv().price) < 0.0000001);
    }

    public boolean equals(Object ent) { // needed for Relation
        if (!(ent instanceof TransactionEntity))
            return false;
        return equal((entity) ent);
    }

    public entity copy() {
        TransactionEntity ip = new TransactionEntity(transaction);
        return (entity) ip;
    }

    // This function should not be used
    public String getName() {
        return Double.toString(transaction.price);
    }

}
