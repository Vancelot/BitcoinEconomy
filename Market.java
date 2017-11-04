package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

public class Market extends ViewableDigraph {
    private ArrayList<Agent> agents; // List of agents
    private int Nt; // Total number of agents
    private OrderBook orderBook;
    
    private final int NUMBER_OF_INITIAL_TRADERS = 160;
    private final int TOTAL_NUMBER_OF_TRADERS = 39649;
    
    public Market() {
        super("Market");

        Nt = 30000;
        agents = new ArrayList<Agent>();
        orderBook = new OrderBook();
        
        // Instantiate all agents at the start of simulation; each will enter the market later themselves
        for (int i = 0; i < TOTAL_NUMBER_OF_TRADERS; i++) {
            // Only let the first few agents enter at initialization
            boolean enterMarket = false;
            if (i <= NUMBER_OF_INITIAL_TRADERS)
                enterMarket = true;
            
            // index will be the ID  of agents 0, 1, 2, ...
            Agent agent = new Agent("Agent" + i, i, 0, 0, enterMarket);
            agents.add(agent);
            
            add(agent); // Add this agent to the Market model
        }
        add(orderBook); // Add the Model Book to the Market model
        
        addOutport("outBitcoins");
/*
        addCoupling(Requester, "outGreen", MSD, "inGreen");
        addCoupling(Requester, "outRed", MSD, "inRed");

        addCoupling(MSD, "outGreen", this, "outGreen");
        addCoupling(MSD, "outRed", this, "outRed");*/
    }

    /**
     * Automatically generated by the SimView program.
     * Do not edit this manually, as such changes will get overwritten.
     */
    public void layoutForSimView()
    {
        preferredSize = new Dimension(591, 145);
        ((ViewableComponent)withName("MSD")).setPreferredLocation(new Point(240, 50));
        ((ViewableComponent)withName("Requester")).setPreferredLocation(new Point(27, 50));
    }
}