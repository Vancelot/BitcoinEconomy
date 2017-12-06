package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Transducer extends ViewableAtomic {
    public static final int TOTAL_SIMULATION_TIME_DEFAULT = 950;
    
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

        hashRateQ.clear();
        
        holdIn("outputTime", 1);
    }

    public void deltext(double e, message x) {
        Continue(e);

        totHashRate = 0;
        for (int i = 0; i < x.getLength(); i++)
            if (messageOnPort(x, "inHashRate", i)) {
                entity hashRateEntity;
                hashRateEntity = x.getValOnPort("inHashRate", i);
                double hashRate = Double.parseDouble(hashRateEntity.toString());
                totHashRate += hashRate;
            }
    }

    public void deltint() {
        time++;
        // Stop all outputs when end time is reached
        if (time <= modelTime) {
            holdIn("outputTime", 1);
        }
        else {
            passivate();
            System.out.println("Tranducer: Going to Passive state");
        }
        
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
        System.out.println("Transducer: Total Hash Rate " + totHashRate);
        
        con = makeContent("outTotNumBitcoin", new entity(String.valueOf(totNumBitcoin)));
        m.add(con);
        System.out.println("Transducer: Total Bitcoins " + totNumBitcoin);
        
        // Output stop to Generator
        if (time == modelTime) {
            con = makeContent("outStop", new entity("Stop"));
            m.add(con);
        }

        con = makeContent("outTime", new entity(String.valueOf(time)));
        m.add(con);
        System.out.println("Tranducer: output time at " + time);
        
        return m;
    }

    public void showState() {
        super.showState();
    }
}
