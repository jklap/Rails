/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/TrainType.java,v 1.1 2005/09/18 21:36:24 evos Exp $
 * 
 * Created on 19-Aug-2005
 * Change Log:
 */
package game;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.*;

import util.XmlUtils;

/**
 * @author Erik Vos
 */
public class TrainType implements TrainTypeI, ConfigurableComponentI,
	Cloneable {
    
    public final static int TOWN_COUNT_MAJOR = 2;
    public final static int TOWN_COUNT_MINOR = 1;
    public final static int NO_TOWN_COUNT = 0;
    
    protected String name;
    protected int amount;
    protected boolean infiniteAmount = false;
    
    
    private String reachBasis = "stops";
    protected boolean countHexes = false;
    
    private String countTowns = "major";
    protected int townCountIndicator = TOWN_COUNT_MAJOR;
    
    private String scoreTowns = "yes";
    protected int townScoreFactor = 1;
    
    private String scoreCities = "single";
    protected int cityScoreFactor = 1;
    
    protected boolean firstExchange = false;
    private boolean real; // Only to determine if top-level attributes must be read.
    
    protected int cost;
    protected int majorStops;
    protected int minorStops;
    protected int firstExchangeCost;
    
    protected String startedPhaseName = null;
    //Phase startedPhase;
    
    private String rustedTrainTypeName = null;
    protected TrainTypeI rustedTrainType = null;
    
    private String releasedTrainTypeName = null;
    protected TrainTypeI releasedTrainType = null;
    
    protected ArrayList trains = null;
    
    boolean available = false;
    boolean rusted = false;

    /**
     * @param real False for the default type, else real.
     * The default type does not have top-level attributes. 
     */
    public TrainType (boolean real) {
        this.real = real;
        if (real) trains = new ArrayList();
    }
    
    public void configureFromXML (Element el) throws ConfigurationException {
        
        if (real) {
            NamedNodeMap attr = el.getAttributes();
 
            // Name
            name = XmlUtils.extractStringAttribute(attr, "name");
            if (name == null) {
                throw new ConfigurationException ("No name specified");
            }
            
            // Cost
            cost = XmlUtils.extractIntegerAttribute(attr, "cost");
            if (cost == 0) {
                throw new ConfigurationException ("Invalid or zero cost specified");
            }
            
            // Amount
            amount = XmlUtils.extractIntegerAttribute(attr, "amount");
            if (amount == -1) {
                infiniteAmount = true;
            } else if (amount <= 0) {
                throw new ConfigurationException ("Invalid or zero amount specified");
            }
                
            
            // Major stops
            majorStops = XmlUtils.extractIntegerAttribute(attr, "majorStops");
            if (majorStops == 0) {
                throw new ConfigurationException ("Invalid or zero major stops specified");
            }
            
            // Minor stops
            minorStops = XmlUtils.extractIntegerAttribute(attr, "minorStops", 0);

            // Phase started
            startedPhaseName = XmlUtils.extractStringAttribute(attr, "startPhase", "");
            
            // Train type rusted
            rustedTrainTypeName = XmlUtils.extractStringAttribute(attr, "rustedTrain");
            
            // Other train type released for buying
            releasedTrainTypeName =  XmlUtils.extractStringAttribute(attr, "releasedTrain");
            
            // To be added: ExchangeFirst parsing
            
        } else {
            name = "";
            amount = 0;
        }
        
        // Reach
        NodeList nl = el.getElementsByTagName("Reach");
        if (nl != null && nl.getLength() > 0) {
            NamedNodeMap reachAttr = nl.item(0).getAttributes();

            // Reach basis
            reachBasis = XmlUtils.extractStringAttribute(reachAttr, 
                    "base", reachBasis);
            
            // Are towns counted (only relevant is reachBasis = "stops")
            countTowns = XmlUtils.extractStringAttribute(reachAttr, 
                    "countTowns", countTowns); 
        }
        
        // Score
        nl = el.getElementsByTagName("Score");
        if (nl != null && nl.getLength() > 0) {
            NamedNodeMap scoreAttr = nl.item(0).getAttributes();

            // Reach basis
            scoreTowns = XmlUtils.extractStringAttribute(scoreAttr, 
                    "scoreTowns", scoreTowns);
            
            // Are towns counted (only relevant is reachBasis = "stops")
            scoreCities = XmlUtils.extractStringAttribute(scoreAttr, 
                    "scoreCities", scoreCities); 
        }
        
        if (real) {
            
            // Check the reach and score values
            countHexes = reachBasis.equals("hexes");
            townCountIndicator = countTowns.equals("no") ? NO_TOWN_COUNT
                    : minorStops > 0 ? TOWN_COUNT_MINOR : TOWN_COUNT_MAJOR;
            cityScoreFactor = scoreCities.equals("double") ? 2 : 1;
            townScoreFactor = scoreTowns.equals("yes") ? 1 : 0;
            // Actually we should meticulously check all values....
            
            // Now create the trains of this type
            if (infiniteAmount) {
                /* We create one train, but will add one more each time
                 * a train of this type is bought.
                 */
                trains.add (new Train(this));
            } else {
                for (int i=0; i<amount; i++) {
                    trains.add (new Train (this));
                }
            }
        }
    }
    
    
    
    /**
     * @return Returns the amount.
     */
    public int getAmount() {
        return amount;
    }
    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return cityScoreFactor;
    }
    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return cost;
    }
    /**
     * @return Returns the countHexes.
     */
    public boolean countsHexes() {
        return countHexes;
    }
    /**
     * @return Returns the firstExchange.
     */
    public boolean isFirstExchange() {
        return firstExchange;
    }
    /**
     * @return Returns the firstExchangeCost.
     */
    public int getFirstExchangeCost() {
        return firstExchangeCost;
    }
    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return majorStops;
    }
    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return minorStops;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @return Returns the releasedTrainType.
     */
    public TrainTypeI getReleasedTrainType() {
        return releasedTrainType;
    }
    /**
     * @return Returns the rustedTrainType.
     */
    public TrainTypeI getRustedTrainType() {
        return rustedTrainType;
    }
    /**
     * @return Returns the startedPhaseName.
     */
    public String getStartedPhaseName() {
        return startedPhaseName;
    }
    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return townCountIndicator;
    }
    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return townScoreFactor;
    }
    
    
    /**
     * @return Returns the releasedTrainTypeName.
     */
    public String getReleasedTrainTypeName() {
        return releasedTrainTypeName;
    }
    /**
     * @return Returns the rustedTrainTypeName.
     */
    public String getRustedTrainTypeName() {
        return rustedTrainTypeName;
    }
    /**
     * @param releasedTrainType The releasedTrainType to set.
     */
    public void setReleasedTrainType(TrainTypeI releasedTrainType) {
        this.releasedTrainType = releasedTrainType;
    }
    /**
     * @param rustedTrainType The rustedTrainType to set.
     */
    public void setRustedTrainType(TrainTypeI rustedTrainType) {
        this.rustedTrainType = rustedTrainType;
    }
    /**
     * @return Returns the available.
     */
    public boolean isAvailable() {
        return available;
    }
    /**
     * @param available The available to set.
     */
    public void setAvailable(boolean available) {
        this.available = available;
        if (available) {
            //System.out.println("Train type "+name+ " set to available");
            for (Iterator it = trains.iterator(); it.hasNext(); ) {
                Portfolio.transferTrain (((TrainI)it.next()), 
                        Bank.getUnavailable(), Bank.getIpo());
                //Bank.getIpo().addTrain((TrainI)it.next());
            }
        }
    }
    
    public boolean hasInfiniteAmount () {
        return infiniteAmount;
    }
    
    public void setRusted () {
        this.rusted = true;
        for (Iterator it = trains.iterator(); it.hasNext(); ) {
            ((TrainI)it.next()).setRusted();
        }
    }
    
    public boolean getRusted () {
        return rusted;
    }
    
	public Object clone () {
	    
	    Object clone = null;
	    try {
	        clone = super.clone();
	        ((TrainType)clone).real = true;
	    } catch (CloneNotSupportedException e) {
	        Log.error("Cannot clone traintype "+name);
            System.out.println(e.getStackTrace());
	    }
	    
 	    return clone;
	}
        
}
