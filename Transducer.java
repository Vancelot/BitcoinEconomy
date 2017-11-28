package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Transducer extends ViewableAtomic {

    protected Queue hashRateQ;

    protected Double totHashRate;

    protected Double totNumBitcoin;

    protected double clock;
   
    protected Double modelTime;
    protected Double marketTime;

    public Transducer() {
        this("Transducer", 1856);
    }

    public Transducer(String name, int modelTime) {
        super(name);

        hashRateQ = new Queue();

        addInport("inHashRate");
        addInport("inTime");

        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");
        addOutport("outTime");
        addOutport("out");
        
        addTestInput("inTime", new entity("time"));

        this.modelTime = (double) modelTime;
    }

    public void initialize() {
        super.initialize();
        phase = "active";
        sigma = modelTime;
        totNumBitcoin = 0.0;
        totHashRate = 0.0;
        marketTime = 0.0;
    }

    public void deltext(double e, message x) {
        clock = clock + e;
        Continue(e);
        
        for (int i = 0; i < x.size(); i++) {
            if (messageOnPort(x, "inTime", i)) {
                marketTime++;
            }
        }
        
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
        clock = clock + sigma;
        
        totHashRate = 0.0;
        entity hashRate;

        while (!hashRateQ.isEmpty()) {

            hashRate = (entity) hashRateQ.first();
            double hRate = Double.parseDouble(hashRate.toString());
            totHashRate = totHashRate + hRate;
            hashRateQ.remove(hashRate);

        }
        
        if (marketTime < 853) {

            totNumBitcoin = marketTime * 72;
        } else

            totNumBitcoin = marketTime * 36;
    
        passivate();
        showState();
       
    }

   

    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltext(e, x);
        deltint();

    }

    public message out() {

        message m = new message();

        content con1 = makeContent("totHashRate", new entity(totHashRate.toString())); // TODO: message for totHashRate
        content con2 = makeContent("totNumBitcoin", new entity(totNumBitcoin.toString())); // TODO: message for
                                                                                           // totNumBitcoin
        content con3 = makeContent("out", new entity("Stop"));

        m.add(con1);
        m.add(con2);
        m.add(con3);

        return m;
    }

    public void showState() {
        super.showState();
    }
}
