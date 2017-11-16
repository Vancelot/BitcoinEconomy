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
        NONE, MINER, RANDOM_TRADER, CHARTIST
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
                    if (messageOnPort(x, "inBitcoinPrice", i)) {
                        holdIn("updatePrice", 100);
                    } else if (messageOnPort(x, "inTransactions", i)) {
                        holdIn("updateWallet", 50);
                    }
            }

            if (phaseIs("passive")) {
                for (int i = 0; i < x.getLength(); i++)
                    if (messageOnPort(x, "inBitcoinPrice", i)) {
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

        // ************************ Miner Functions***********

        // Miners have:
        // Cost of electricity per day
        // Bitcoins mined per day
        // An amount of time to make a decision to buy and/or sell hardware

        if (AgentType.MINER != null) {
            cash = cash - electricityCost(sigma);
            numBitcoin = numBitcoin + minedBitcoinPerMiner(sigma);

            if (sigma == decisionTime(sigma)) {
                // purchase hardware

                if (cash > 0 && numBitcoin > 0) {

                    double hashRate = 0;
                    hashRate = hashRate + minerHashRate(sigma);
                    cash = cash - (cash * percentCashForHardware(sigma));
                    numBitcoin = numBitcoin - (numBitcoin * percentBitcoinCashForHardware(sigma));
                }
            }
            if (phaseIs("passive") && cash <= 0) {
                // issue sell order

                holdIn("ordering", 20);
            }

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
                return AgentType.CHARTIST;
        }
    }

    private boolean enterToMarket(double t) {
        if (id <= getTotalNumberOfTraders(t))
            return true;
        else
            return false;
    }

    private double getInitialBitcoins(double t) {
        // Create a Zipf's law function per time
        // Bt = b1 * ln(Nt) + y;
        // TODO: Issue with the jsc package. Need to install a standalone package to
        // import jsc.distributions.Pareto
        double n_t = getTotalNumberOfTraders(t);
        double b_t = INITIAL_BITCOINS_OF_RICHEST_TRADER_AT_TIME_1 * Math.log(n_t) + EULER_MASCHERONI_CONSTANT_GAMMA;

        return b_t / id;
    }

    private double getTotalNumberOfTraders(double t) {
        // Calculate the total number of traders at a particular time t
        // Nt(t) = a * e ^ (b * (608 + t))
        final int A = 2624;
        final double B = 0.002971;
        return (A * Math.exp(B * (608 + t)));
    }

    // ************************************ MINER **********

    private double decisionTime(double t) {
        // Time that miners take to decide to buy new hardware units
        // dT = t+int(60+N(mu, sig))
        // Rounds to the nearest integer
        // Updates each time the i-th miner makes a decision

        Random r = new Random();
        double var = (double) Math.round(r.nextGaussian() * 6);

        return (t + 60 + var);
    }

    private double percentCashForHardware(double t) {
        // Percentage of i-th miners cash allocated to buy new hardware at time t
        // g1i(t) = 0 if no hardware is bought by i-th trader at time t
        // g1i(t) = lognormal(mu,sig), mu = 0.6, sig = 0.15
        // if g1i > 1 return 1

        Random r = new Random((long) t);
        double g1i = (double) Math.log(r.nextGaussian() * 0.15 + 0.6);

        if (g1i > 1)
            return 1;
        else
            return g1i;
    }

    private double percentBitcoinCashForHardware(double t) {
        // Percentage of i-th miners Bitcoins to be sold for buying the new hardware at
        // time t
        // gi(t) = 0 if no hardware is bought by i-th trader at time t
        // gi(t) = 0.5 * g1i
        // if gi > 1 return 1

        final double gi = 0.5 * percentCashForHardware(t);

        if (gi > 1)
            return 1;
        else
            return gi;
    }

    private double personalWealthForHardware(double t) {
        // Amount of personal wealth that the miner wishes to allocate to buy new
        // hardware
        // pWFH = g1i*cash+gi*numBitcoin*bitcoinPrice
        // On average the miner will allocate 60% of cash and 30% of Bitcoins

        final double g1i = percentCashForHardware(t);
        final double gi = percentBitcoinCashForHardware(t);

        return (g1i * cash + gi * numBitcoin * bitcoinPrice);
    }

    private double bestHashRate(double t) {
        // Calculate the best hash rate at time t
        // R(t) = a * e ^ (b * t)
        final int A = 86350;
        final double B = 0.006318;

        return (A * Math.exp(B * t));
    }

    private double newUnitHashRate(double t) {
        // Hashing capability of the hardware units bought at time t by the i-th miner
        // riu(t=tE>0) = g1i(t)ci(t)R(t) --> new miner without Bitcoin (new entrance)

        final double g1i = percentCashForHardware(t);
        final double R = bestHashRate(t);

        return (g1i * cash * R);
    }

    private double marketUnitHashRate(double t) {
        // Hashing capability of the hardware units bought at time t by the i-th miner
        // riu(t>tE) = [g1i(t)ci(t)+gi(t)bi(t)p(t)]R(t) --> miner with Bitcoin

        final double pW = personalWealthForHardware(t);
        final double R = bestHashRate(t);

        return (pW * R);
    }

    private double minerHashRate(double t) {
        // TODO: Clear up initial unit hash rate
        // Hashing capability at time t of i-th miner
        // ri(0)=0.0173 GH/sec
        // ri(t)=sum(riu(t))

        double riu;
        if (t == 0) {
            riu = newUnitHashRate(t);
        } else
            riu = marketUnitHashRate(t);

        double initMinerRi;
        if (t == 0) {
            initMinerRi = 0.0173;
        } else
            initMinerRi = 0;

        double sumMinerRi = initMinerRi;
        sumMinerRi = sumMinerRi + riu;

        return (sumMinerRi);
    }

    private double bestPower(double t) {
        // Calculate the Power requirements at time t
        // P(t) = a * e ^ (b * t)
        final double A = 4.679 / 10000000;
        final double B = -0.004055;

        return (A * Math.exp(B * t));
    }

    private double electricityCost(double t) {
        // i-th electricity cost at time t --> $/day

        final double eRate = 1.4 / 1000;

        final double riu = marketUnitHashRate(t);
        double sumRiu = 0;
        sumRiu = sumRiu + riu;

        final double P = bestPower(t);
        double sumPower = 0;
        sumPower = sumPower + P;

        return (eRate * sumRiu * sumPower * 24);
    }

    private double totalHashRate(double t) {
        // Hashing capacity of the whole population of Miners Nm at time t

        final double ri = minerHashRate(t);
        double sumRi = 0;
        sumRi = sumRi + ri;

        return (sumRi);
    }

    private double minedBitcoinPerMiner(double t) {
        // Number of bitcoin mined by the i-th miner per day
        // B(t)*ri(t)/rtot(t)

        final double ri = minerHashRate(t);
        final double rtot = totalHashRate(t);

        int B;
        if (t < 853) {
            B = 72;
        } else
            B = 36;

        return (B * ri / rtot);
    }

    // ************************* End Miner *********

    // *************** Buy and Sell Orders **********

    private double marketOrderProb() {
        // Market order trades at best price
        // Each Agent type has a probability to issue a market order
        // Market order sets limit price to 0
        // Miner Plim = 1
        // Random Trader Plim = 0.2
        // Chartist Plim = 0.7

        double Plim = 0;
        if (AgentType.MINER != null) {
            Plim = 1;
        }
        if (AgentType.RANDOM_TRADER != null) {
            Plim = 0.2;
        } else if (AgentType.CHARTIST != null) {
            Plim = 0.7;
        }

        return (Plim);
    }

    private double sellLimitPrice() {
        // Price to trade Bitcoin for Cash
        // Random draw from a gaussian distribution
        // ave = 1, stdv << 1

        Random r = new Random();
        double var = (double) Math.round(r.nextGaussian() * .00001 + 1);

        double result = r.nextDouble(); // 0.0 to 1.0

        if (result < marketOrderProb())
            return (0);
        else {
            return (bitcoinPrice / var);
        }
    }

    private double numBitcoinSell() {
        // Number of Bitcoin in sell order
        // Random variable from lognormal distribution
        // Random Trader --> ave = 0.25, stdv = 0.2
        // Chartist --> ave 0.4, stdv = 0.2

        Random r = new Random();
        double beta = 0;
        if (AgentType.CHARTIST != null) {
            beta = (double) Math.log(r.nextGaussian() * 0.2 + 0.25);
        }
        if (AgentType.RANDOM_TRADER != null) {
            beta = (double) Math.log(r.nextGaussian() * 0.2 + 0.4);
        }

        double bsi;
        if (AgentType.MINER != null) {
            bsi = numBitcoin;
        } else
            bsi = numBitcoin * beta;

        return (bsi);
    }

    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltint();
        deltext(0, x);
    }

    public message out() {
        // TODO: Messages need to have id, limit price, and number of bitcoin

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
