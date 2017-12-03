package BitcoinEcon;

import java.lang.Math;

import java.util.Random;

import BitcoinEcon.Order.OrderType;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

import model.modeling.message;

public class Agent extends ViewableAtomic {
    private static final double EULER_MASCHERONI_CONSTANT_GAMMA = 0.57721566490153286060651209008240243104215933593992;
    private static final double INITIAL_BITCOINS_OF_RICHEST_TRADER_AT_TIME_1 = 4117;

    public enum AgentType {
        NONE, MINER, RANDOM_TRADER, CHARTIST
    }

    protected AgentType type; // Types of agent, to be decided when the agent enters the market

    protected int id; // ID of an agent, all agents will have ID starting from 0 to the total number

    protected double bitcoinPrice; // Bitcoin price, updated from the input from the Experimental Frame
    protected double lastPrice;

    protected double numBitcoin; // Number of Bitcoins the agent holds
    protected double pendingBitcoin; // Pending Bitcoins in OrderBook
    protected double cash; // Fiat cash the agent holds
    protected double pendingCash; // pending cash to buy Bitcoins in OrderBook

    protected Order buyOrder;
    protected Order sellOrder;

    protected double chartistSumOfSquare;
    protected int chartistOrderTime; // counts number of days in a time window

    protected int marketTime; // Time from beginning of simulation
    protected int agentTime; // Time agent has been active
    protected int enterMarketTime; // Time that agent enters the market

    protected int minerDecisionTime; // Time Miner makes a decision to buy hardware

    protected boolean enterMarket; // Indication of whether the agent is active or not
    protected boolean buyHardware;

    protected double hashRate; // miner Hash Rate

    protected int updatePriceTime;
    protected int updateWalletTime;

    public Agent() {
        this(AgentType.NONE, 0, 0, 0, false);
    }

    public Agent(AgentType none, int id, double numBitcoin, double bitcoinPrice, boolean enterMarket) {
        super("Agent" + id);

        addInport("inTransactions");
        addInport("inBitcoinPrice");
        addInport("inTime");

        addOutport("outOrders");
        addOutport("outHashRates");

        addTestInput("inTransactions", new entity("Transaction"));
        addTestInput("inBitcoinPrice", new entity("Price"));

        this.id = id;
        this.numBitcoin = numBitcoin;
        this.cash = numBitcoin * bitcoinPrice * 5; // Nominal cash of initial trader equal to 5 times the nominal value
                                                   // of their crypto cash
        this.enterMarket = enterMarket;
        this.chartistSumOfSquare = 0;
        this.chartistOrderTime = 0;

    }

    public void initialize() {
        super.initialize();

        bitcoinPrice = 0;
        sigma = INFINITY;
        phase = "passive";
        this.marketTime = 0;
        this.agentTime = 0;
        this.enterMarketTime = 0;

        this.updatePriceTime = 0;
        this.updateWalletTime = 0;
        this.hashRate = 0;

        this.pendingBitcoin = 0;
        this.pendingCash = 0;

        buyOrder = null;
        sellOrder = null;
    }

    public void deltext(double e, message x) {
        Continue(e);

        for (int i = 0; i < x.getLength(); i++)
            // Clock for agents
            if (messageOnPort(x, "inTime", i)) {
                entity uTime = x.getValOnPort("inTime", i);
                marketTime = Integer.parseInt(uTime.toString()); // TODO: Takes message (entity) on port converts to
                                                                 // string, then to integer

                // Wake up agent to enter market
                if ((marketTime > 0) && (enterMarket == false) && enterToMarket(marketTime)) {
                    enterMarket = true;

                    enterMarketTime = marketTime;

                    this.numBitcoin = getInitialBitcoins(marketTime);
                    this.cash = 5 * (this.numBitcoin * bitcoinPrice);
                    this.type = determineAgentType(marketTime);
                    minerDecisionTime = decisionTime(agentTime);

                    pendingBitcoin = 0;
                    pendingCash = 0;
                }

                if (enterMarket) {
                    agentTime = marketTime - enterMarketTime;

                    if (type == AgentType.MINER)
                        runMiner();
                    else if (type == AgentType.RANDOM_TRADER)
                        runRandomTrader();
                    else if (type == AgentType.CHARTIST)
                        runChartist();
                }
            } else if (messageOnPort(x, "inBitcoinPrice", i)) { // Updating Price of Bitcoin
                entity marketPrice;
                marketPrice = x.getValOnPort("inBitcoinPrice", i);
                lastPrice = bitcoinPrice; // Updates last Price of Bitcoin
                bitcoinPrice = Double.parseDouble(marketPrice.toString()); // Updates Price of Bitcoin
            } else if (messageOnPort(x, "inTransactions", i) && enterMarket) {
                // Only process Transaction messages when the agent is active
                if (type == AgentType.MINER && buyHardware == true) {
                    hashRate = hashRate + minerHashRate(agentTime); // Updates miner Hash Rate
                }

                // Retrieve Transaction
                TransactionEntity message = (TransactionEntity) x.getValOnPort("InTransaction", i);
                Transaction transaction = message.getv();

                // Check if this transaction message is for an expired order, which means
                // either buy order or sell order in the transaction is null
                Order buyOrder = transaction.buyOrder;
                Order sellOrder = transaction.sellOrder;

                if ((buyOrder == null) && (sellOrder == null)) {
                    // Do nothing; unexpected transaction with both orders being NULL
                } else if (buyOrder == null) { // Sell order expired
                    // Update the pending Bitcoin amount
                    pendingBitcoin -= sellOrder.residualAmount;
                    numBitcoin += sellOrder.residualAmount;
                } else if (sellOrder == null) { // Buy order expired
                    // Update the pending Cash amount
                    pendingCash -= buyOrder.residualAmount;
                    cash += buyOrder.residualAmount;
                } else { // This is a valid transaction, check if this transaction is related to the
                         // Agent
                    if (buyOrder.agentId == id) {
                        // Update wallet
                        numBitcoin += transaction.bitcoinAmount;
                        pendingCash -= buyOrder.residualAmount;
                    } else if (sellOrder.agentId == id) {
                        // Update wallet
                        cash += transaction.bitcoinAmount * transaction.price;
                        pendingBitcoin -= transaction.bitcoinAmount;
                    }
                }
            }
    }

    public void deltint() {
        passivate();
    }

    // ************* End Deltint **********************

    // ************************ Miner Functions***********
    // Miners have:
    // Cost of electricity per day
    // Bitcoins mined per day
    // An amount of time to make a decision to buy and/or sell hardware
    private void runMiner() {
        cash = cash - electricityCost(agentTime); // TODO when to call?
        numBitcoin = numBitcoin + minedBitcoinPerMiner(agentTime); // TODO: when to call?

        // Check if it is time to purchase hardware
        if (cash > 0 && numBitcoin > 0) {
            if (agentTime == minerDecisionTime) {
                holdIn("issueSellOrder", 0);
                buyHardware = true;

                double numBitcoinSell = numBitcoinSell();
                double sellLimitPrice = sellLimitPrice();
                int expirationTime = expirationTime(marketTime);

                sellOrder = makeSellOrder(numBitcoinSell, sellLimitPrice, expirationTime);
                // cash = cash - (cash * percentCashForHardware(sigma));
                // numBitcoin = numBitcoin - (numBitcoin *
                // percentBitcoinCashForHardware(sigma));
                // These values must be extracted from Transaction messages
            }
        }
        // If running out of cash, sell Bitcoins to get cash to pay bills
        else if (cash <= 0) {
            // issue sell order
            holdIn("issueSellOrder", 0);
            buyHardware = false;
            double numBitcoinSell = numBitcoinSell();
            double sellLimitPrice = sellLimitPrice();
            int expirationTime = expirationTime(marketTime);

            sellOrder = makeSellOrder(numBitcoinSell, sellLimitPrice, expirationTime); // TODO: I think this is model
                                                                                       // time, because it goes to
                                                                                       // OrderBook
        }
    }

    // *************** Random Trader Functions ***********************
    // Random Traders issue orders randomly
    private void runRandomTrader() {
        if (issueOrder() == true) {

            Random r = new Random();
            double result = r.nextDouble(); // 0.0 to 1.0

            // Issue Buy order
            if (result >= .5 && cash > 0) {
                holdIn("issueBuyOrder", 0);

                double numBitcoinBuy = numBitcoinBuy();
                double buyLimitPrice = buyLimitPrice();
                int expirationTime = expirationTime(marketTime);

                buyOrder = makeBuyOrder(numBitcoinBuy, buyLimitPrice, expirationTime);
            }

            // Issue Sell order
            if (result < .5) {
                holdIn("issueSellOrder", 0);
                double numBitcoinSell = numBitcoinSell();
                double sellLimitPrice = sellLimitPrice();
                int expirationTime = expirationTime(marketTime);

                sellOrder = makeSellOrder(numBitcoinSell, sellLimitPrice, expirationTime);
            }

        }
    }

    // ******************Chartist Functions ******************************
    // Chartist issues orders conditionally
    private void runChartist() {
        if (issueOrder() == true && agentTime == chartistOrderTime) {

            double cVariance = getPRVariance(agentTime);

            // Issue Buy order
            if (cVariance > 0.01) {
                holdIn("issueBuyOrder", 0);
                double numBitcoinBuy = numBitcoinBuy();
                double buyLimitPrice = buyLimitPrice();
                int expirationTime = expirationTime(marketTime);

                buyOrder = makeBuyOrder(numBitcoinBuy, buyLimitPrice, expirationTime);
            }

            // Issue Sell order
            if (cVariance < 0.01) {
                holdIn("issueSellOrder", 0);
                double numBitcoinSell = numBitcoinSell();
                double sellLimitPrice = sellLimitPrice();
                int expirationTime = expirationTime(marketTime);

                sellOrder = makeSellOrder(numBitcoinSell, sellLimitPrice, expirationTime);
            }
        }
    }

    // Create a Buy order
    Order makeBuyOrder(double aAmount, double aLimitPrice, int aExpirationTime) {
        Order order = new Order(OrderType.BUY, id, aAmount, aLimitPrice, aExpirationTime);
        pendingCash = aAmount;

        return order;
    }

    // Create a Sell order
    Order makeSellOrder(double aAmount, double aLimitPrice, int aExpirationTime) {
        Order order = new Order(OrderType.BUY, id, aAmount, aLimitPrice, aExpirationTime);
        pendingBitcoin = aAmount;

        return order;
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
        // TODO: Issue with the jsc package. Need to install a stand alone package to
        // import jsc.distributions.Pareto
        double n_t = getTotalNumberOfTraders(t);
        double b_t = INITIAL_BITCOINS_OF_RICHEST_TRADER_AT_TIME_1 * Math.log(n_t) + EULER_MASCHERONI_CONSTANT_GAMMA;

        return b_t / id;
    }

    private int getTotalNumberOfTraders(double t) {
        // Calculate the total number of traders at a particular time t
        // Nt(t) = a * e ^ (b * (608 + t))
        final double A = 2.624; // Originally 2624 from page 2/8 of Appendix B
        final double B = 0.002971;
        int totalNumber = (int) Math.round(A * Math.exp(B * (608 + t)));
        return totalNumber;
    }

    // ************************************ MINER **********

    private int decisionTime(int t) {
        // Time that miners take to decide to buy new hardware units
        // dT = t+int(60+N(mu, sig))
        // Rounds to the nearest integer
        // Updates each time the i-th miner makes a decision

        Random r = new Random();
        double var = (double) Math.round(r.nextGaussian() * 6);

        return (int) (t + 60 + var);
    }

    private double percentCashForHardware(double t) {
        // Percentage of i-th miners cash allocated to buy new hardware at time t
        // g1i(t) = 0 if no hardware is bought by i-th trader at time t
        // g1i(t) = lognormal(mu,sig), mu = 0.6, sig = 0.15
        // if g1i > 1 return 1

        Random r = new Random();
        double g1i = (double) Math.exp(r.nextGaussian() * 0.15 + 0.6);

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

    private double newUnitHashRate(int t) {
        // Hashing capability of the hardware units bought at time t by the i-th miner
        // riu(t=tE>0) = g1i(t)ci(t)R(t) --> new miner without Bitcoin (new entrance)

        final double g1i = percentCashForHardware(t);
        final double R = bestHashRate(t);

        return (g1i * cash * R);
    }

    private double marketUnitHashRate(int t) {
        // Hashing capability of the hardware units bought at time t by the i-th miner
        // riu(t>tE) = [g1i(t)ci(t)+gi(t)bi(t)p(t)]R(t) --> miner with Bitcoin

        final double pW = personalWealthForHardware(t);
        final double R = bestHashRate(t);

        return (pW * R);
    }

    private double minerHashRate(int t) {
        // Hashing capability at time t of i-th miner
        // ri(0)=0.0173 GH/sec
        // ri(t)=sum(riu(t))

        double riu;
        if (agentTime == 0 && id > 160) { // agent time
            riu = newUnitHashRate(t);
        } else
            riu = marketUnitHashRate(t);

        double initMinerRi;
        if (marketTime == 0 && id <= 160) { // model time
            initMinerRi = 0.0173;
        } else
            initMinerRi = 0;

        double sumMinerRi = initMinerRi;
        sumMinerRi = sumMinerRi + riu;

        return (sumMinerRi);
    }

    private double bestPower(int t) {
        // Calculate the Power requirements at time t
        // P(t) = a * e ^ (b * t)
        final double A = 4.679 / 10000000;
        final double B = -0.004055;

        return (A * Math.exp(B * t));
    }

    private double electricityCost(int t) {
        // i-th electricity cost at time t --> $/day
        // called every day
        final double eRate = 1.4 / 1000;

        final double riu = marketUnitHashRate(t);
        double sumRiu = 0;
        sumRiu = sumRiu + riu;

        final double P = bestPower(t);
        double sumPower = 0;
        sumPower = sumPower + P;

        return (eRate * sumRiu * sumPower * 24);
    }

    private double totalHashRate(int t) {
        // Hashing capacity of the whole population of Miners Nm at time t
        // called every day

        final double ri = minerHashRate(t);
        double sumRi = 0;
        sumRi = sumRi + ri;

        return (sumRi);
    }

    private double minedBitcoinPerMiner(int t) {
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

    // **************** Chartist *******************

    private int timeWindow(int t) {
        // Window of time the i-th Chartist issues Orders
        // Characterized by a normal distribution
        // ave = 20, stdv = 1

        Random r = new Random();
        double window = Math.round(r.nextGaussian() + 20);

        return (int) (t + window);
    }

    private double getPRVariance(int t) {
        // Buy when price is rising --> Thc > 0.01
        // Sell when price is falling --> Thc < 0.01
        // Thc --> relative price variation

        double variance;
        variance = 0;

        double relativePrice;
        relativePrice = lastPrice / bitcoinPrice; // P1/P2, P2/P3, ..., Pn-1/Pn

        if (t < chartistOrderTime) {
            double sumRelativePrice = 0;

            sumRelativePrice = 0;
            sumRelativePrice = sumRelativePrice + relativePrice;

            double mean = sumRelativePrice / timeWindow(t) - agentTime;

            chartistSumOfSquare += Math.pow(bitcoinPrice - mean, 2);

            variance = chartistSumOfSquare / (timeWindow(t) - agentTime - 1);

            // This should be a calculation of relative price variation in
            // timeWindow()
        }
        return variance;
    }

    // ***************** End Chartist *********************

    // *************** Buy and Sell Orders **********

    private double activeTraderProb() {
        // Each type of trader has a different probability of being active in the market
        // Miner is active during decision making, defined above
        // Random trader = 0.1
        // Chartist = 0.5 and if price variation is above/below threshold

        double Pact = 0;
        if (type == AgentType.RANDOM_TRADER) {
            Pact = 0.1;
        } else if (type == AgentType.CHARTIST) {
            Pact = 0.5;
        }

        return (Pact);
    }

    private boolean issueOrder() {
        // Activates traders to issue orders
        // Random trader called every day

        Random r = new Random();

        double result = r.nextDouble(); // 0.0 to 1.0

        if (type == AgentType.RANDOM_TRADER && result < activeTraderProb()) {
            return true;
        } else if (type == AgentType.CHARTIST && result < activeTraderProb()) {
            return true;
        } else
            return false;
    }

    private double marketOrderProb() {
        // Market order trades at best price
        // Each Agent type has a probability to issue a market order
        // Market order sets limit price to 0
        // Miner Plim = 1
        // Random Trader Plim = 0.2
        // Chartist Plim = 0.7

        double Plim = 0;
        if (type == AgentType.MINER) {
            Plim = 1;
        }
        if (type == AgentType.RANDOM_TRADER) {
            Plim = 0.2;
        } else if (type == AgentType.CHARTIST) {
            Plim = 0.7;
        }

        return (Plim);
    }

    // **************************** Sell ********************

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
        // sa = Number of Bitcoin in sell order
        // bsi = Available Bitcoins to sell
        // Random variable from lognormal distribution
        // Random Trader --> ave = 0.25, stdv = 0.2
        // Chartist --> ave 0.4, stdv = 0.2

        Random r = new Random();
        double beta = 0;
        if (type == AgentType.CHARTIST) {
            beta = (double) Math.exp(r.nextGaussian() * 0.2 + 0.25);
        }
        if (type == AgentType.RANDOM_TRADER) {
            beta = (double) Math.exp(r.nextGaussian() * 0.2 + 0.4);
        }

        double sa;
        // TODO: define pendingBitcoin as Bitcoin still in OrderBook

        double bsi = numBitcoin - pendingBitcoin;

        if (type == AgentType.MINER) {
            sa = bsi * percentBitcoinCashForHardware(agentTime);

        } else
            sa = bsi * beta;

        return (sa);
    }

    // ****************** End Sell **************************

    // ******************* Buy *************************

    private double buyLimitPrice() {
        // Price to trade Bitcoin for Cash
        // N(mu, sig) = Random draw from a gaussian distribution
        // ave = 1, stdv << 1

        Random r = new Random();
        double var = (double) Math.round(r.nextGaussian() * .00001 + 1);

        double result = r.nextDouble(); // 0.0 to 1.0

        if (result < marketOrderProb())
            return (0);
        else {
            return (bitcoinPrice * var);
        }
    }

    private double numBitcoinBuy() {
        // ba = Number of Bitcoin in Buy order
        // beta = Random variable from lognormal distribution
        // Random Trader --> ave = 0.25, stdv = 0.2
        // Chartist --> ave 0.4, stdv = 0.2

        Random r = new Random();
        double beta = 0;
        if (type == AgentType.CHARTIST) {
            beta = (double) Math.exp(r.nextGaussian() * 0.2 + 0.25);
        }
        if (type == AgentType.RANDOM_TRADER) {
            beta = (double) Math.exp(r.nextGaussian() * 0.2 + 0.4);
        }

        double ba;
        // cbi = cash - cash in pending Transactions
        // cbi = cash available to buy Bitcoins
        // TODO: Define pendingCash as cash still in OrderBook

        double cbi = cash - pendingCash; // - cash in pending Transactions

        if (type == AgentType.MINER) {
            ba = 0;
        } else
            ba = cbi * beta / bitcoinPrice;

        return (ba);
    }

    private int expirationTime(int t) {
        // Time for an Order to be completed before it expires

        Random r = new Random();
        int tStep = 0;

        tStep = (int) Math.exp(r.nextGaussian() * 0.2 + 0.25);

        if (type == AgentType.RANDOM_TRADER) {
            return (t + tStep);
        } else if (type == AgentType.CHARTIST) {
            return 1;
        } else
            return (int) INFINITY;
    }

    // ************* End Buy and Sell Orders *************************************

    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltint();
        deltext(0, x);
    }

    public message out() {
        // Messages need to have id, buy/sell, limit price, and number of bitcoin,
        // expiration date

        message m = new message();

        if (phaseIs("issueSellOrder")) {
            content con = makeContent("outOrders", new OrderEntity(sellOrder));
            sellOrder = null;
            if (type == AgentType.MINER) {
                minerDecisionTime = decisionTime(agentTime); // Schedules next decision time for miner
                m.add(con);
            }
            if (type == AgentType.CHARTIST) {
                chartistOrderTime = timeWindow(agentTime); // Schedules next Order time for Chartist
                m.add(con);
            } else
                m.add(con);
        } else if (phaseIs("issueBuyOrder")) {
            content con = makeContent("outOrders", new OrderEntity(buyOrder));
            buyOrder = null;
            m.add(con);
        }

        return m;
    }

    public void showState() {
        super.showState();
    }

}
