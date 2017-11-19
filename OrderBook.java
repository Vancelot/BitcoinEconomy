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

    protected Queue matchQ;
    protected boolean match;

    protected ArrayList<Order> buyList;
    protected ArrayList<Order> sellList;
    protected double marketPrice;

    public OrderBook() {
        this("OrderBook");
    }

    public OrderBook(String name) {
        super(name);
        matchQ = new Queue();

        buyList = new ArrayList<Order>();
        sellList = new ArrayList<Order>();

        marketPrice = 0;

        addInport("InOrders");
        addOutport("OutTransactions");
    }

    public void initialize() {
        super.initialize();
        matchQ = new Queue();
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
                    } else if (order.type == Order.OrderType.SELL) {
                        // Initialize Residual Amount to Amount
                        order.residualAmount = order.amount;
                        // Then save to list in the sorted order
                        sellList.add(order);
                        Collections.sort(sellList);
                    }
                }

            // A sell order (with index j) and buy order (with index i) are considered
            // a match if sj <= bi
            if (buyList.get(0).limitPrice > sellList.get(0).limitPrice) {
                // First determine the price for the transaction
                double price = determinePrice(buyList.get(0).limitPrice, sellList.get(0).limitPrice);
                
                if (sellList.get(0).residualAmount == buyList.get(0).residualAmount) {
                    sellList.remove(0);
                    buyList.remove(0);
                } else if (buyList.get(0).residualAmount > sellList.get(0).residualAmount) {
                    sellList.remove(0);
                    buyList.get(0).residualAmount = buyList.get(0).residualAmount - sellList.get(0).residualAmount;
                } else {
                    buyList.remove(0);
                    sellList.get(0).residualAmount = buyList.get(0).residualAmount - sellList.get(0).residualAmount;
                }
            }
        }
    }

    // Return the price with the following logic:
    //  - When one of the two orders has limit price equal to zero
    //      If bi>0, then pT = min(bi,p(t))
    //      If sj>0, then pT = max(sj,p(t))
    //  - When both orders have limit price equal to zero
    //      pT = p(t)
    //  - When both orders have limit price higher than zero
    //      pT = (b_i+s_j)/2
    private double determinePrice(double buyLimitPrice, double sellLimitPrice) {
        double price = 0;
        if ((buyLimitPrice == 0) && (sellLimitPrice == 0)) {
            price = marketPrice;
        }
        else if ((buyLimitPrice == 0) || (sellLimitPrice == 0)) {
            if (buyLimitPrice > 0)
                price = Math.min(buyLimitPrice, marketPrice);
            else
                price = Math.max(sellLimitPrice, marketPrice);
        }
        else {
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
