package BitcoinEcon;

import java.lang.Math;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Agent extends ViewableAtomic {

    public enum AgentType {
        NONE, MINER, RANDOM_TRADER, CHARTIRST
    }

    protected Queue procQ; // Queue of input transactions
    protected int id; // ID of an agent, all agents will have ID starting from 0 to the total number
    protected double bitcoinPrice; // Bitcoin price, updated from the input from the Experimental Frame
    protected double numBitcoin; // Number of Bitcoins the agent holds
    protected double cash; // Fiat cash the agent holds
    protected boolean enterMarket; // Indication of whether the agent is active or not
    protected AgentType type; // Types of agent, to be decided when the agent enters the market

    public Agent() {
        this("Agent", 0, 0, 0, false);
    }

    public Agent(String name, int id, double numBitcoin, double bitcoinPrice, boolean enterMarket) {
        super(name);
        procQ = new Queue();
        addInport("inTransactions");
        addInport("inBitcoinPrice");
        addOutport("outOrders");
        addTestInput("inTransactions", new entity("Transaction"));
        addTestInput("inBitcoinPrice", new entity("Price"));
        this.id = id;
        this.numBitcoin = numBitcoin;
        this.cash = numBitcoin * bitcoinPrice * 5; // Nominal cash of initial trader equal to 5 times the nominal value of their crypto cash
        this.enterMarket = enterMarket;
        this.type = AgentType.MINER;
    }

    public void initialize() {
        super.initialize();
        procQ = new Queue();
        bitcoinPrice = 0;
        sigma = INFINITY;
        phase = "passive";
    }

    public void deltext(double e, message x) {
        Continue(e);

        // Only process when the agent is active
        if (enterMarket) {
            if (phaseIs("passive")) {
                for (int i = 0; i < x.getLength(); i++)
                    if (messageOnPort(x, "PBPort", i)) {
                        holdIn("ordering", 100);
                    } else if (messageOnPort(x, "TPort", i)) {
                        holdIn("updatingTrans", 50);
                    }
            }

            if (phaseIs("passive")) {
                for (int i = 0; i < x.getLength(); i++)
                    if (messageOnPort(x, "PBPort", i)) {
                        holdIn("updatingPrice", 100);
                    }
            }
        }
    }

    public void deltint() {
        passivate();

        // Wake up agent to enter market
        int t = 0; // TODO where to find the time attribute???
        if (enterToMarket(t))
        {
            enterMarket = true;
            this.cash = getInitialCash(t);
        }
    }

    private boolean enterToMarket(int t) {
        // Calculate the total number of traders at a particular time t
        // Nt(t) = a * e ^ (b * (608 + t))
        final int A = 2624;
        final double B = 0.002971;
        int n_t = (int) (A * Math.exp(B * (608 + t)));

        if (id <= n_t)
            return true;
        else
            return false;
    }

    private double getInitialCash(int t) {
        // Create a Zipf's law function per time
        // Bt = b1 * ln(Nt) + y;
        // TODO: Issue with the jsc package. Need to install a standalone package to import jsc.distributions.Pareto
        double b_t = 0;

        return b_t / id;
    }
    
    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltint();
        deltext(0, x);
    }

    public message out() {

        message m = new message();

        content con = makeContent("OrderPort", new entity("Order"));
        if (phaseIs("ordering")) {
            m.add(con);
        }

        return m;
    }

    public void showState() {
        super.showState();
    }

}
