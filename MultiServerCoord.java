package BitcoinEcon;

import GenCol.*;
import view.modeling.ViewableAtomic;
import model.modeling.*;
import model.simulation.*;
import view.simView.*;

public class MultiServerCoord extends ViewableAtomic {

    protected Queue procs;
    protected Queue jobs;
    // protected proc pcur;
    protected message yMessage;

    public MultiServerCoord(String name) {
        super(name);

        jobs = new Queue();
        procs = new Queue();
    }

    public MultiServerCoord() {
        super("MultiServerCoord");

        jobs = new Queue();
        procs = new Queue();

        addInport("inTransactions");
        addInport("inBitcoinPrice");
        addInport("inOrders");
        addInport("inTimer");

        // TODO - add test inputs
        
        addInport("outTransactions");
        addInport("outBitcoinPrice");
        addInport("outOrders");
        addInport("outTimer");

        // initialize();
    }

    public void initialize() {
        phase = "passive";
        sigma = INFINITY;
        //job = null;
        super.initialize();
        ;
    }

    public void showState() {
        super.showState();
        System.out.println("number of jobs: " + jobs.size());
        System.out.println("number of procs: " + procs.size());
    }

    // protected void add_procs(proc p){ //BPZ 99 this cause problem
    protected void add_procs(devs p) {

        procs.add(p);
        // pcur = p;
    }

    public void deltext(double e, message x) {

        Continue(e);

        if (phaseIs("passive")) {
            //for (int i = 0; i < x.size(); i++)
                //if (messageOnPort(x, "setup", i))
                    // TODO add_procs(new proc("p", 1000));
        }

        if (phaseIs("passive")) {
            yMessage = new message();
            for (int i = 0; i < x.size(); i++)
                if (messageOnPort(x, "in", i)) {
                    //job = x.getValOnPort("in", i);

                    if (!procs.isEmpty()) {
                        entity pcur = (entity) procs.first();
                        procs.remove();
                        //yMessage.add(makeContent("y", new Pair(pcur, job)));
                        holdIn("send_y", 0);
                    }
                }
        }
        // (proc,job) Pairs returned on port x
        // always accept so that no processor is lost

        for (int i = 0; i < x.size(); i++)
            if (messageOnPort(x, "x", i)) {
                entity val = x.getValOnPort("x", i);
                Pair pr = (Pair) val;
                procs.add(pr.getKey());
                entity jb = (entity) pr.getValue();
                jobs.add(jb);
            }
        // output completed jobs at earliest opportunity
        if (phaseIs("passive") && !jobs.isEmpty())
            holdIn("send_out", 0);

    }

    public void deltint() {
        if (phaseIs("send_out")) {
            jobs = new Queue();
            passivate();
        }
        // output completed jobs at earliest opportunity

        else if (phaseIs("send_y") && !jobs.isEmpty())
            holdIn("send_out", 0);
        else
            passivate();
    }

    public message out() {
        message m = new message();
        if (phaseIs("send_out"))
            for (int i = 0; i < jobs.size(); i++) {
                entity job = (entity) jobs.get(i);
                m.add(makeContent("out", job));
            }
        else if (phaseIs("send_y"))
            m = yMessage;
        return m;
    }

}
