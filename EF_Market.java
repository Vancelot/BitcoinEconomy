package BitcoinEcon;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import view.modeling.ViewableAtomic;
import view.modeling.ViewableComponent;
import view.modeling.ViewableDigraph;

public class EF_Market extends ViewableDigraph {
    
    public EF_Market() {
        this("EF_Market");
    }
    
    public EF_Market(String name) {
        super(name);
        
        ViewableDigraph ExpFrame = new ExpFrame("ExpFrame");
        ViewableDigraph Market = new Market();
        
        add(ExpFrame);
        add(Market);
        
        addOutport("totHashRate");
        addOutport("totNumBitcoin");
        addOutport("priceBitcoin");
        
        addCoupling(Market, "outTransactions", ExpFrame, "inTransaction");
        addCoupling(Market, "outHashRates", ExpFrame, "inHashRate");
        addCoupling(Market, "outNumBitcoin", ExpFrame, "inNumBitcoin");
                
        addCoupling(ExpFrame, "outPriceBitcoin", Market, "inBitcoinPrice");
        addCoupling(ExpFrame, "outTime", Market, "inTime");
        addCoupling(ExpFrame, "outPriceBitcoin", this, "priceBitcoin");
        addCoupling(ExpFrame, "outTotHashRate", this, "totHashRate");
        addCoupling(ExpFrame, "outTotNumBitcoin", this, "totNumBitcoin");
        
    }
    
    public void layoutForSimView() {
        preferredSize = new Dimension(1100, 620);
    }

}

