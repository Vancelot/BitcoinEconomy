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

    private ArrayList<Order> buyList;
    private ArrayList<Order> sellList;

    public OrderBook() {
        this("OrderBook");
    }

    public OrderBook(String name) {
        super(name);
        matchQ = new Queue();
        
        buyList = new ArrayList<Order>();
        sellList = new ArrayList<Order>();
        
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
        // - Sell orders with the lower limit price si  are placed on top of the sell list
        // - If new orders have the same limit price, orders with older issue time are 
        //     placed before the recent orders.

        if (phaseIs("passive")) {
            for (int i = 0; i < x.getLength(); i++)
                if (messageOnPort(x, "InOrders", i)) {
                    OrderEntity message = (OrderEntity) x.getValOnPort("InOrders", i);
                    Order order = message.getv();
                    if (order.orderType == Order.OrderType.BUY) {
                        buyList.add(order);
                    } else if (order.orderType == Order.OrderType.SELL) {
                        buyList.add(order);
                    }
                }
        }
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
