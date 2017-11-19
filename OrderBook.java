package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import GenCol.entity;
import GenCol.Queue;
import model.modeling.content;
import model.modeling.message;
import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class OrderBook extends ViewableAtomic {

    protected Queue transactionQ;

    protected ArrayList<Order> buyList;
    protected ArrayList<Order> sellList;
    protected double marketPrice;

    public OrderBook() {
        this("OrderBook");
    }

    public OrderBook(String name) {
        super(name);
        transactionQ = new Queue();

        buyList = new ArrayList<Order>();
        sellList = new ArrayList<Order>();

        marketPrice = 0;

        addInport("InOrders");
        addOutport("OutTransactions");
    }

    public void initialize() {
        super.initialize();
        transactionQ = new Queue();
        sigma = INFINITY;
        phase = "passive";
    }

    public void deltext(double e, message x) {
        Continue(e);

        // When a new order is issued from any agent,
        // the Order Book will first insert it to the buy/sell list as follows:
        // - Buy orders with higher limit price bi are placed on top of the buy list
        // - Sell orders with the lower limit price si are placed on top of the sell
        // list
        // - If new orders have the same limit price, orders with older issue time are
        // placed before the recent orders.

        if (phaseIs("passive")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "InOrders", i)) {
                    OrderEntity message = (OrderEntity) x.getValOnPort("InOrders", i);
                    Order order = message.getv();
                    if (order.type == Order.OrderType.BUY) {
                        // Initialize Residual Amount to Amount
                        order.residualAmount = order.amount;
                        // Then save to list in the sorted order
                        buyList.add(order);
                        Collections.sort(buyList);

                        holdIn("matching", 0);
                    } else if (order.type == Order.OrderType.SELL) {
                        // Initialize Residual Amount to Amount
                        order.residualAmount = order.amount;
                        // Then save to list in the sorted order
                        sellList.add(order);
                        Collections.sort(sellList);

                        holdIn("matching", 0);
                    }
                }

            // Perform the matching process until no match found
            matchOrders();
        }
    }

    // Match the orders from the Buy list and Sell list as follows:
    // The first buy order and the first sell order of the lists are inspected to
    // verify if they match
    // If they match a transaction occurs.
    // The order with the smallest residual amount is fully executed
    // Whereas the order with the largest amount is only partially executed, and
    // remains at the head of the list,
    // With the residual amount reduced by the amount of the matching order
    // If both orders have the same amount they are fully executed
    // After the transaction:
    // The next pair of orders at the head of the lists are checked for matching
    // If they match they are executed
    // And so on until they do not match anymore
    // Before the book can accept new orders, all the matching orders are satisfied
    // A sell order of index j matches a buy order of index i and vice versa, only
    // if
    // s_j <= b_ior if one of the two limit prices, or both, are equal to 0
    private void matchOrders() {
        // A sell order (with index j) and buy order (with index i) are considered
        // a match if sj <= bi
        while ((buyList.get(0).limitPrice >= sellList.get(0).limitPrice) || (buyList.get(0).limitPrice == 0)
                || (sellList.get(0).limitPrice == 0)) {
            Transaction transaction;

            // First determine the price for the transaction
            double price = determinePrice(buyList.get(0).limitPrice, sellList.get(0).limitPrice);
            double buyerResidualAmountInCash = buyList.get(0).residualAmount;
            double sellerResidualAmountInCash = sellList.get(0).residualAmount * price;

            // Start to execute order: compare orders in terms of Residual Amount in cash
            if (buyerResidualAmountInCash == sellerResidualAmountInCash) {
                transaction = new Transaction(buyList.get(0), sellList.get(0), price);
                transactionQ.add(transaction);

                sellList.remove(0); // This sell order is completed
                buyList.remove(0); // This buy order is completed
            } else if (buyerResidualAmountInCash > sellerResidualAmountInCash) {
                buyList.get(0).residualAmount = buyerResidualAmountInCash - sellerResidualAmountInCash;

                transaction = new Transaction(buyList.get(0), sellList.get(0), price);
                transactionQ.add(transaction);

                sellList.remove(0); // This sell order is completed
            } else {
                sellList.get(0).residualAmount = buyerResidualAmountInCash / price - sellList.get(0).residualAmount;

                transaction = new Transaction(buyList.get(0), sellList.get(0), price);
                transactionQ.add(transaction);

                buyList.remove(0); // This buy order is completed
            }
        }
    }

    // Return the price with the following logic:
    // - When one of the two orders has limit price equal to zero
    // If bi>0, then pT = min(bi,p(t))
    // If sj>0, then pT = max(sj,p(t))
    // - When both orders have limit price equal to zero
    // pT = p(t)
    // - When both orders have limit price higher than zero
    // pT = (b_i+s_j)/2
    private double determinePrice(double buyLimitPrice, double sellLimitPrice) {
        double price = 0;
        if ((buyLimitPrice == 0) && (sellLimitPrice == 0)) {
            price = marketPrice;
        } else if ((buyLimitPrice == 0) || (sellLimitPrice == 0)) {
            if (buyLimitPrice > 0)
                price = Math.min(buyLimitPrice, marketPrice);
            else
                price = Math.max(sellLimitPrice, marketPrice);
        } else {
            price = (buyLimitPrice + sellLimitPrice) / 2;
        }

        return price;
    }

    public void deltint() {
        passivate();
    }

    public message out() {

        message m = new message();

        content con = makeContent("OutTransactions", new entity("Transaction"));
        if (phaseIs("matching")) {
            m.add(con);
        }

        return m;
    }

    public void showState() {
        super.showState();
    }

}
