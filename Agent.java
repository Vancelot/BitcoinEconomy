package BitcoinEcon;

import java.lang.Math;
import java.util.Random;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Agent extends ViewableAtomic {
    private final double EULER_MASCHERONI_CONSTANT_GAMMA = 0.57721566490153286060651209008240243104215933593992;
    private final double INITIAL_BITCOINS_OF_RICHEST_TRADER_AT_TIME_1 = 4117;
    
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
        this.cash = numBitcoin * bitcoinPrice * 5; // Nominal cash of initial trader equal to 5 times the nominal value
                                                   // of their crypto cash
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
        if ((enterMarket == false) && enterToMarket(sigma)) {
            enterMarket = true;
            this.numBitcoin = getInitialBitcoins(sigma);
            this.cash = 5 * this.numBitcoin;
            this.type = determineAgentType(sigma);
        }
    }

    private AgentType determineAgentType(double t) {
        // Probability of an agent to be a Miner:
        // pM(t) = a * e^(b*t)
        // a = 0.9425, b = -0.002654
        // Generator to generate a random value from 1 to 100: r
        // If the probability pM if 30%, and the generated random r is 31 -> not a Miner
        // If the probability pM if 30%, and the generated random r is 29 -> Miner
        // If not a Miner, generate another random number r2
        // pR = 0.7 * (1 - pM)
        // pC = 0.3 * (1 - pM)
        // If r2 <= pR -> Random Trader
        // Otherwise, Chartist

        Random r = new Random();
        double result = r.nextDouble(); // 0.0 to 1.0

        final double a = 0.9425;
        final double b = -0.002654;
        double pM = a * Math.exp(b * t); // 0 to 1

        if (result < pM)
            return AgentType.MINER;
        else {
            double pR = 0.7 * (1 - pM);
            result = r.nextDouble(); // 0.0 to 1.0
            if (result < pR)
                return AgentType.RANDOM_TRADER;
            else
                return AgentType.CHARTIRST;
        }
    }

    private boolean enterToMarket(double t) {
        if (id <= getTotalNumberOfBitcoins(t))
            return true;
        else
            return false;
    }

    private double getInitialBitcoins(double t) {
        // Create a Zipf's law function per time
        // Bt = b1 * ln(Nt) + y;
        // TODO: Issue with the jsc package. Need to install a standalone package to
        // import jsc.distributions.Pareto
        double n_t = getTotalNumberOfBitcoins(t);
        double b_t = INITIAL_BITCOINS_OF_RICHEST_TRADER_AT_TIME_1 * Math.log(n_t) + EULER_MASCHERONI_CONSTANT_GAMMA;
        
        return b_t / id;
    }

    private double getTotalNumberOfBitcoins(double t) {
        // Calculate the total number of traders at a particular time t
        // Nt(t) = a * e ^ (b * (608 + t))
        final int A = 2624;
        final double B = 0.002971;
        return (A * Math.exp(B * (608 + t)));
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
