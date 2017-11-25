package BitcoinEcon;

import GenCol.*;

import model.modeling.*;

import view.modeling.ViewableAtomic;

public class Transducer extends ViewableAtomic {

    protected Queue hashRateQ;
   
    protected Double totHashRate;

    protected Double totNumBitcoin;

    protected Double modelTime;

    public Transducer() {
        this("Transducer", 1856);
    }

    public Transducer(String name, int modelTime) {
        super(name);

        hashRateQ = new Queue();
        
        addInport("inHashRate");
        
        addOutport("outTotHashRate");
        addOutport("outTotNumBitcoin");
        addOutport("outTime");
        addOutport("out");

        this.modelTime = (double) modelTime;
    }

    public void initialize() {
        super.initialize();
        phase = "passive";
        sigma = INFINITY;
        totNumBitcoin = 0.0;
        totHashRate = 0.0;
    }

    public void deltext(double e, message x) {
        modelTime = modelTime + e;
        Continue(e);

        if (phaseIs("passive")) {
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
    }

    public void deltint() {
        
        totHashRate = 0.0;
        entity hashRate;
        
        while (!hashRateQ.isEmpty()){
            
           hashRate = (entity) hashRateQ.first();
           double hRate = Double.parseDouble(hashRate.toString());
           totHashRate = totHashRate + hRate;
           hashRateQ.remove(hashRate);          
            
        }
        
        modelTime = modelTime + sigma;
        
        holdIn("outputTime", 1);
        
        passivate();

        if (modelTime < 853) {

            totNumBitcoin = modelTime * 72;
        } else
            
            totNumBitcoin = modelTime * 36;
    }

    public void deltcon(double e, message x) {
        System.out.println("confluent");
        deltext(e, x);
        deltint();
    }

    public message out() {

        message m = new message();

        content con1 = makeContent("totHashRate", new entity(totHashRate.toString())); // TODO: message for totHashRate
        content con2 = makeContent("totNumBitcoin", new entity(totNumBitcoin.toString())); // TODO: message for totNumBitcoin
        content con3 = makeContent("out", new entity("Stop"));
        content con4 = makeContent("outTime", new entity(modelTime.toString()));

        m.add(con1);
        m.add(con2);
        m.add(con3);
        m.add(con4);

        return m;
    }

    public void showState() {
        super.showState();
    }
}
