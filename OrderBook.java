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

import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class OrderBook extends ViewableAtomic {
    public static final double BITCOIN_PRICE_ERROR_LIMIT = 99999;
    
    protected Queue transactionQ;

    protected LinkedList<Order> buyList;
    protected LinkedList<Order> sellList;
    protected double marketPrice;

    protected int time;

    public OrderBook() {
        this("OrderBook");
    }

    public OrderBook(String name) {
        super(name);
        transactionQ = new Queue();

        buyList = new LinkedList<Order>();
        sellList = new LinkedList<Order>();

        addInport("inBitcoinPrice");
        addInport("inOrders");
        addInport("inTime");
        addOutport("outTransactions"); // Port to output matched transactions
    }

    public void initialize() {
        super.initialize();
        transactionQ.clear();
        sigma = INFINITY;
        phase = "passive";

        marketPrice = 0;
        time = 0;
        passivate();
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

        for (int i = 0; i < x.getLength(); i++)
            if (messageOnPort(x, "inOrders", i)) {
                OrderEntity message = (OrderEntity) x.getValOnPort("inOrders", i);
                Order order = message.getv();
                if (order.type == Order.OrderType.BUY) {
                    // Initialize Residual Amount to Amount
                    order.residualAmount = order.amount;
                    // Then save to list in the sorted order
                    buyList.add(order);
                    Collections.sort(buyList);
                } else if (order.type == Order.OrderType.SELL) {
                    // Initialize Residual Amount to Amount
                    order.residualAmount = order.amount;
                    // Then save to list in the sorted order
                    sellList.add(order);
                    Collections.sort(sellList);
                }
            } else if (messageOnPort(x, "inTime", i)) {
                time++;

                // Perform the matching process until no match found
                matchOrders();

                removeExpiredBuyOrders();
                removeExpiredSellOrders();
            } else if (messageOnPort(x, "inBitcoinPrice", i)) { // Updating Price of Bitcoin
                entity marketPriceEntity;
                marketPriceEntity = x.getValOnPort("inBitcoinPrice", i);
                marketPrice = Double.parseDouble(marketPriceEntity.toString()); // Updates Price of Bitcoin
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
        while (((buyList.size() > 0) && (sellList.size() > 0) &&
                ((buyList.getFirst().limitPrice >= sellList.getFirst().limitPrice) || (buyList.getFirst().limitPrice == 0)
                || (sellList.getFirst().limitPrice == 0)))) {
            Transaction transaction;
            TransactionEntity transEntity;

            // First determine the price for the transaction
            double price = determinePrice(buyList.getFirst().limitPrice, sellList.getFirst().limitPrice);
            double buyerResidualAmountInCash = buyList.getFirst().residualAmount;
            double sellerResidualAmountInCash = sellList.getFirst().residualAmount * price;
            double bitcoinAmount = 0;

            if (buyerResidualAmountInCash == 0) {
                buyList.remove();
                continue;
            }
            if (sellerResidualAmountInCash == 0) {
                sellList.remove();
                continue;
            }
            
            // Start to execute order: compare orders in terms of Residual Amount in cash
            if (buyerResidualAmountInCash == sellerResidualAmountInCash) {
                bitcoinAmount = sellList.getFirst().residualAmount;
                buyList.getFirst().residualAmount = 0;
                sellList.getFirst().residualAmount = 0;

                transaction = new Transaction(buyList.getFirst(), sellList.getFirst(), price, bitcoinAmount);
                transEntity = new TransactionEntity(transaction);
                transactionQ.add(transEntity);

                sellList.remove(); // This sell order is completed
                buyList.remove(); // This buy order is completed
            } else if (buyerResidualAmountInCash > sellerResidualAmountInCash) {
                bitcoinAmount = buyerResidualAmountInCash / price - sellList.getFirst().residualAmount;
                buyList.getFirst().residualAmount = buyerResidualAmountInCash - sellerResidualAmountInCash;
                sellList.getFirst().residualAmount = 0;

                transaction = new Transaction(buyList.getFirst(), sellList.getFirst(), price, bitcoinAmount);
                transEntity = new TransactionEntity(transaction);
                transactionQ.add(transEntity);

                sellList.remove(); // This sell order is completed
            } else {
                bitcoinAmount = sellList.getFirst().residualAmount - buyerResidualAmountInCash / price;
                sellList.getFirst().residualAmount = sellList.getFirst().residualAmount - buyerResidualAmountInCash / price;
                buyList.getFirst().residualAmount = 0;

                transaction = new Transaction(buyList.getFirst(), sellList.getFirst(), price, bitcoinAmount);
                transEntity = new TransactionEntity(transaction);
                transactionQ.add(transEntity);

                buyList.remove(); // This buy order is completed
            }

            holdIn("outputTransactions", 0); // Output now
        }
    }

    private void removeExpiredBuyOrders() {
        for (int counter = 0; counter < buyList.size(); counter++) {
            if (time >= buyList.get(counter).expirationTime) {
                Transaction transaction = new Transaction(buyList.get(counter), null, 0, 0);
                TransactionEntity transEntity = new TransactionEntity(transaction);
                transactionQ.add(transEntity);

                buyList.remove(counter);
            }
        }
    }

    private void removeExpiredSellOrders() {
        for (int counter = 0; counter < sellList.size(); counter++) {
            if (time >= sellList.get(counter).expirationTime) {
                Transaction transaction = new Transaction(null, sellList.get(counter), 0, 0);
                TransactionEntity transEntity = new TransactionEntity(transaction);
                transactionQ.add(transEntity);

                sellList.remove(counter);
            }
        }
    }

    // Check whether the order expires
    private boolean isOrderValid(Order order) {
        boolean valid = false;

        return valid;
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

        content con = makeContent("outTransactions", new entity("Transaction"));

        while ((transactionQ.size()) > 0 && (transactionQ.first() != null)) {
            TransactionEntity transactionEntity = (TransactionEntity) transactionQ.first();

            con = makeContent("outTransactions", transactionEntity);
            m.add(con);

            transactionQ.remove();
            // Make sure all messages will be sent out
        }

        return m;
    }

    public void showState() {
        super.showState();
    }

}
