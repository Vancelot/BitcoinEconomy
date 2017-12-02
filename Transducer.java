package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Transducer extends ViewableAtomic {
    public static final int TOTAL_SIMULATION_TIME_DEFAULT = 1856;
    
    protected Queue hashRateQ;

    protected double totHashRate;

    protected double totNumBitcoin;

    protected int time;
    protected int modelTime;

    public Transducer() {
        this("Transducer", TOTAL_SIMULATION_TIME_DEFAULT);
    }

    public Transducer(String name, int modelTime) {
        super(name);

        hashRateQ = new Queue();

        addInport("inHashRate");

        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");
        addOutport("outTime");
        addOutport("outStop");

        this.modelTime = modelTime;
    }

    public void initialize() {
        super.initialize();
        phase = "passive";
        sigma = INFINITY;
        totNumBitcoin = 0.0;
        totHashRate = 0.0;
        time = 0;

        holdIn("outputTime", 1);
    }

    public void deltext(double e, message x) {
        Continue(e);

        for (int i = 0; i < x.getLength(); i++)
            if (messageOnPort(x, "inHashRate", i)) {
                entity hashRate;
                hashRate = x.getValOnPort("inHashRate", i);

                hashRateQ.add(hashRate);

                // TODO: extract current hash rate from message
                // Remove old and hash rate
                // Sum current total hash rate

            }
    }

    public void deltint() {

        totHashRate = 0.0;
        entity hashRate;

        time++;

        while (!hashRateQ.isEmpty()) {
            hashRate = (entity) hashRateQ.first();
            double hRate = Double.parseDouble(hashRate.toString());
            totHashRate = totHashRate + hRate;
            hashRateQ.remove(hashRate);
        }

        // Stop all outputs when end time is reached
        if (time <= modelTime)
            holdIn("outputTime", 1);
        else
            passivate();

        if (time < 853) {
            totNumBitcoin = time * 72;
        } else
            totNumBitcoin = time * 36;
    }

    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltext(e, x);
        deltint();
    }

    public message out() {

        message m = new message();

        content con;
        con = makeContent("outTotHashRate", new entity(String.valueOf(totHashRate)));
        m.add(con);

        con = makeContent("outTotNumBitcoin", new entity(String.valueOf(totNumBitcoin)));
        m.add(con);

        // Output stop to Generator
        if (time == modelTime) {
            con = makeContent("outStop", new entity("Stop"));
            m.add(con);
        }

        con = makeContent("outTime", new entity(String.valueOf(time)));
        m.add(con);

        return m;
    }

    public void showState() {
        super.showState();
    }
}
