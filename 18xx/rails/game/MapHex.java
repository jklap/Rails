/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/MapHex.java,v 1.28 2009/10/31 17:08:26 evos Exp $ */
package rails.game;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import rails.game.action.LayTile;
import rails.game.model.ModelObject;
import rails.game.move.Moveable;
import rails.game.move.TileMove;
import rails.util.LocalText;
import rails.util.Tag;

/**
 * Represents a Hex on the Map from the Model side.
 *
 * <p> <b>Tile orientations</b>. Tiles can be oriented NS or EW; the directions
 * refer to the "flat" hex sides. <p> The term "rotation" is used to indicate
 * the amount of rotation (in 60 degree units) from the standard orientation of
 * the tile (sometimes the term orientation is also used to refer to rotation).
 * <p>Rotation is always relative to the standard orientation, which has the
 * printed tile number on the S edge for NS oriented tiles, or on the SW edge
 * for EW oriented tiles. The rotation numbers are indicated in the below
 * picture for an NS-oriented tile: <p> <code>
 *
 *       ____3____
 *      /         \
 *     2           4
 *    /     NS      \
 *    \             /
 *     1           5
 *      \____0____/
 * </code> <p> For EW-oriented
 * tiles the above picture should be rotated 30 degrees clockwise.
 */
public class MapHex extends ModelObject implements ConfigurableComponentI,
        StationHolderI, TokenHolderI {

    public static final int EW = 0;
    public static final int NS = 1;
    protected static int tileOrientation;
    protected static boolean lettersGoHorizontal;
    protected static boolean letterAHasEvenNumbers;

    private static final String[] ewOrNames =
            { "SW", "W", "NW", "NE", "E", "SE" };
    private static final String[] nsOrNames =
            { "S", "SW", "NW", "N", "NE", "SE" };

    // Coordinates as used in the rails.ui.swing.hexmap package
    protected int x;
    protected int y;

    // Map coordinates as printed on the rails.game board
    protected String name;
    protected int row;
    protected int column;
    protected int letter;
    protected int number;
    protected String tileFileName;
    protected int preprintedTileId;
    protected TileI currentTile;
    protected int currentTileRotation;
    protected int[] tileCost;
    protected String cityName;

    /** Neighbouring hexes <i>to which track may be laid</i>. */
    protected MapHex[] neighbours = new MapHex[6];

    /** Values if this is an off-board hex */
    protected int[] offBoardValues = null;

    /*
     * Temporary storage for impassable hexsides. Once neighbours has been set
     * up, this attribute is no longer used. Only the black or blue bars on the
     * map need be specified, and each one only once. Impassable non-track sides
     * of "offboard" (red) and "fixed" (grey or brown) preprinted tiles will be
     * derived and need not be specified.
     */
    protected String impassable = null;

    protected List<City> cities;
    protected Map<Integer, City> mCities;

    protected boolean isBlocked = false;

    protected Map<PublicCompanyI, City> homes;
    protected List<PublicCompanyI> destinations;

    /** Tokens that are not bound to a Station (City), such as Bonus tokens */
    protected List<TokenI> offStationTokens;
    
    protected MapManager mapManager = null;

    protected static Logger log =
            Logger.getLogger(MapHex.class.getPackage().getName());

    public MapHex(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        Pattern namePattern = Pattern.compile("(\\D)(\\d+)");

        name = tag.getAttributeAsString("name");
        Matcher m = namePattern.matcher(name);
        if (!m.matches()) {
            throw new ConfigurationException("Invalid name format: " + name);
        }
        letter = m.group(1).charAt(0);
        try {
            number = Integer.parseInt(m.group(2));
        } catch (NumberFormatException e) {
            // Cannot occur!
        }

        /*
         * Translate hex names (as on the board) to coordinates used for
         * drawing.
         */
        if (lettersGoHorizontal) {
            row = number;
            column = letter - '@';
            if (tileOrientation == MapHex.EW) {
                // Tiles with flat EW sides, letters go horizontally.
                // Example: 1841 (NOT TESTED, PROBABLY WRONG).
                x = column;
                y = row / 2;
            } else {
                // Tiles with flat NS sides, letters go horizontally.
                // Tested for 1856.
                x = column;
                y = (row + 1) / 2;
            }
        } else
        // letters go vertical (normal case)
        {
            row = letter - '@';
            column = number;
            if (tileOrientation == MapHex.EW) {
                // Tiles with flat EW sides, letters go vertically.
                // Most common case.
                // Tested for 1830 and 1870.
                x = (column + (letterAHasEvenNumbers ? 1 : 0)) / 2;
                y = row;
            } else {
                // Tiles with flat NS sides, letters go vertically.
                // Tested for 18AL.
                x = column;
                y = (row + 1) / 2;
            }
        }

        preprintedTileId = tag.getAttributeAsInteger("tile", -999);

        currentTileRotation = tag.getAttributeAsInteger("orientation", 0);

        impassable = tag.getAttributeAsString("impassable");
        tileCost = tag.getAttributeAsIntegerArray("cost", new int[0]);

        // Off-board revenue values
        offBoardValues = tag.getAttributeAsIntegerArray("value", null);

        // City name
        cityName = tag.getAttributeAsString("city", "");

    }

    public void finishConfiguration (GameManagerI gameManager) {
        
        currentTile = gameManager.getTileManager().getTile(preprintedTileId);
        // We need completely new objects, not just references to the Tile's
        // stations.
        cities = new ArrayList<City>(4);
        mCities = new HashMap<Integer, City>(4);
         for (Station s : currentTile.getStations()) {
            // sid, type, value, slots
            City c = new City(this, s.getNumber(), s);
            cities.add(c);
            mCities.put(c.getNumber(), c);
        }

    }

    public boolean isNeighbour(MapHex neighbour, int direction) {
        /*
         * Various reasons why a bordering hex may not be a neighbour in the
         * sense that track may be laid to that border:
         */
        /* 1. The hex side is marked "impassable" */
        if (impassable != null && impassable.indexOf(neighbour.getName()) > -1)
            return false;
        /*
         * 2. The preprinted tile on this hex is offmap or fixed and has no
         * track to this side.
         */
        TileI tile = neighbour.getCurrentTile();
        if (!tile.isUpgradeable()
            && !tile.hasTracks(3 + direction
                               - neighbour.getCurrentTileRotation()))
            return false;

        return true;
    }

    public static void setTileOrientation(int orientation) {
        tileOrientation = orientation;
    }

    public static int getTileOrientation() {
        return tileOrientation;
    }

    public static void setLettersGoHorizontal(boolean b) {
        lettersGoHorizontal = b;
    }

    /**
     * @return Returns the letterAHasEvenNumbers.
     */
    public static boolean hasLetterAEvenNumbers() {
        return letterAHasEvenNumbers;
    }

    /**
     * @param letterAHasEvenNumbers The letterAHasEvenNumbers to set.
     */
    public static void setLetterAHasEvenNumbers(boolean letterAHasEvenNumbers) {
        MapHex.letterAHasEvenNumbers = letterAHasEvenNumbers;
    }

    /**
     * @return Returns the lettersGoHorizontal.
     */
    public static boolean isLettersGoHorizontal() {
        return lettersGoHorizontal;
    }

    public static String getOrientationName(int orientation) {

        if (tileOrientation == EW) {
            return ewOrNames[orientation % 6];
        } else {
            return nsOrNames[orientation % 6];
        }
    }

    /* ----- Instance methods ----- */

    /**
     * @return Returns the column.
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return Returns the row.
     */
    public int getRow() {
        return row;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /**
     * @return Returns the preprintedTileId.
     */
    public int getPreprintedTileId() {
        return preprintedTileId;
    }

    /**
     * @return Returns the image file name for the tile.
     */
    public String getTileFileName() {
        return tileFileName;
    }

    public void setNeighbor(int orientation, MapHex neighbour) {
        orientation %= 6;
        neighbours[orientation] = neighbour;
    }

    public MapHex getNeighbor(int orientation) {
        return neighbours[orientation % 6];
    }

    public MapHex[] getNeighbors() {
        return neighbours;
    }

    public boolean hasNeighbour(int orientation) {

        while (orientation < 0)
            orientation += 6;
        return neighbours[orientation % 6] != null;
    }

    public TileI getCurrentTile() {
        return currentTile;
    }

    public int getCurrentTileRotation() {
        return currentTileRotation;
    }

    /** Look for the Hex matching the Label in the terrain static map */
    /* EV: useful, but needs to be rewritten */
    public static MapHex getHexByLabel(String terrain, String label) {
        /*
         * int x = 0; int y = Integer.parseInt(new String(label.substring(1)));
         * switch (label.charAt(0)) { case 'A': case 'a': x = 0; break;
         *
         * case 'B': case 'b': x = 1; break;
         *
         * case 'C': case 'c': x = 2; break;
         *
         * case 'D': case 'd': x = 3; break;
         *
         * case 'E': case 'e': x = 4; break;
         *
         * case 'F': case 'f': x = 5; break;
         *
         * case 'X': case 'x': // entrances GUIHex[] gameEntrances = (GUIHex[])
         * entranceHexes.get(terrain); return gameEntrances[y].getMapHexModel();
         *
         * default: Log.error("Label " + label + " is invalid"); } y = 6 - y -
         * (int) Math.abs(((x - 3) / 2)); GUIHex[][] correctHexes = (GUIHex[][])
         * terrainH.get(terrain); return correctHexes[x][y].getMapHexModel();
         */
        return null;
    }

    public int getTileCost() {
        if (currentTile.getId() == preprintedTileId) {
            return getTileCost(0);
        } else {
            return getTileCost(currentTile.getColourNumber());
        }
    }

    public int getTileCost(int index) {
        if (index >= 0 && index < tileCost.length) {
            return tileCost[index];
        } else {
            return 0;
        }
    }

    /**
     * Prepare a tile upgrade. The actual tile replacement is done in
     * replaceTile(), via a TileMove object.
     */
    public void upgrade(LayTile action) {
        TileI newTile = action.getLaidTile();
        int newRotation = action.getOrientation();

        City newCity;
        String newTracks;
        List<City> newCities;

        Map<String, Integer> relaidTokens = action.getRelaidBaseTokens();
        if (relaidTokens == null) relaidTokens = new HashMap<String, Integer>();

        if (currentTile.getNumStations() == newTile.getNumStations()) {
            // If the number of stations does not change,
            // reassign new Stations to existing cities,
            // keeping the original numbers (which therefore
            // may become different from the new tile's
            // station numbers).
            Map<City, Station> citiesToStations = new HashMap<City, Station>();

            // Check for manual handling of tokens
            for (String compName : relaidTokens.keySet()) {
                for (City city : cities) {
                    if (city.hasTokenOf(compName)) {
                        citiesToStations.put(city, newTile.getStations().get(relaidTokens.get(compName)-1));
                    }
                }
            }

            // Scan the old cities/stations,
            // and assign new stations where tracks correspond
            for (City city : cities) {
                if (citiesToStations.containsKey(city)) continue;
                Station oldStation = city.getRelatedStation();
                int[] oldTrackEnds =
                        getTrackEndPoints(currentTile, currentTileRotation,
                                oldStation);
                if (oldTrackEnds.length == 0) continue;
                station: for (Station newStation : newTile.getStations()) {
                    int[] newTrackEnds =
                            getTrackEndPoints(newTile, newRotation, newStation);
                    for (int i = 0; i < oldTrackEnds.length; i++) {
                        for (int j = 0; j < newTrackEnds.length; j++) {
                            if (oldTrackEnds[i] == newTrackEnds[j]) {
                                // Match found!
                                citiesToStations.put(city, newStation);
                                continue station;
                            }
                        }
                    }
                }
            }

            // Map any unassigned cities randomly
            city: for (City city : cities) {
                if (citiesToStations.containsKey(city)) continue;
                for (Station newStation : newTile.getStations()) {
                    if (citiesToStations.values().contains(newStation)) continue;
                    citiesToStations.put(city, newStation);
                    continue city;
                }
            }


           // Assign the new Stations to the existing cities
           for (City city : citiesToStations.keySet()) {
               Station newStation = citiesToStations.get(city);
               Station oldStation = city.getRelatedStation();
               city.setRelatedStation(newStation);
               city.setSlots(newStation.getBaseSlots());
               newTracks =
                        getConnectionString(newTile,
                                newRotation,
                                newStation.getNumber());
               city.setTrackEdges(newTracks);
               log.debug("Assigned "
                          + city.getUniqueId()
                          + " from "
                          + oldStation.getId()
                          + " "
                          + getConnectionString(currentTile,
                                  currentTileRotation,
                                  oldStation.getNumber())
                          + " to " + newStation.getId() + " "
                          + newTracks);
            }
            newCities = cities;

        } else {
            // If the number of stations does change,
            // create a new set of cities.

            // Build a map from old to new cities,
            // so that we can move tokens at the end.
            newCities = new ArrayList<City>(4);
            Map<Integer, City> mNewCities = new HashMap<Integer, City>(4);
            Map<City, City> oldToNewCities = new HashMap<City, City>();
            Map<Station, City> newStationsToCities =
                    new HashMap<Station, City>();

            // Scan the old cities/stations,
            // and assign new stations where tracks correspond
            int newCityNumber = 0;
            for (City oldCity : cities) {
                int cityNumber = oldCity.getNumber();
                Station oldStation = oldCity.getRelatedStation();
                int[] oldTrackEnds =
                        getTrackEndPoints(currentTile, currentTileRotation,
                                oldStation);
                log.debug("Old city #"
                          + currentTile.getId()
                          + " city "
                          + oldCity.getNumber()
                          + ": "
                          + getConnectionString(currentTile,
                                  currentTileRotation, oldStation.getNumber()));
                station: for (Station newStation : newTile.getStations()) {
                    int[] newTrackEnds =
                            getTrackEndPoints(newTile, newRotation, newStation);
                    log.debug("New station #"
                              + newTile.getId()
                              + " station "
                              + newStation.getNumber()
                              + ": "
                              + getConnectionString(newTile, newRotation,
                                      newStation.getNumber()));
                    for (int i = 0; i < oldTrackEnds.length; i++) {
                        for (int j = 0; j < newTrackEnds.length; j++) {
                            if (oldTrackEnds[i] == newTrackEnds[j]) {
                                // Match found!
                                if (!newStationsToCities.containsKey(newStation)) {
                                    newCity =
                                            new City(this, ++newCityNumber,
                                                    newStation);
                                    newCities.add(newCity);
                                    mNewCities.put(cityNumber, newCity);
                                    newStationsToCities.put(newStation, newCity);
                                    newCity.setSlots(newStation.getBaseSlots());
                                } else {
                                    newCity =
                                            newStationsToCities.get(newStation);
                                }
                                oldToNewCities.put(oldCity, newCity);
                                newTracks =
                                        getConnectionString(newTile,
                                                newRotation,
                                                newStation.getNumber());
                                newCity.setTrackEdges(newTracks);
                                log.debug("Assigned from "
                                          + oldCity.getUniqueId()
                                          + " #"
                                          + currentTile.getId()
                                          + "/"
                                          + currentTileRotation
                                          + " "
                                          + oldStation.getId()
                                          + " "
                                          + getConnectionString(currentTile,
                                                  currentTileRotation,
                                                  oldStation.getNumber())
                                          + " to " + newCity.getUniqueId()
                                          + " #" + newTile.getId() + "/"
                                          + newRotation + " "
                                          + newStation.getId() + " "
                                          + newTracks);
                                break station;
                            }
                        }
                    }


                }
            }

            // If an old city is not yet connected, check if was
            // connected to another city it has merged into (1851 Louisville)
            for (City oldCity : cities) {
                if (oldToNewCities.containsKey(oldCity)) continue;
                Station oldStation = oldCity.getRelatedStation();
                int[] oldTrackEnds =
                    getTrackEndPoints(currentTile, currentTileRotation,
                            oldStation);
                station: for (int i = 0; i < oldTrackEnds.length; i++) {
                    log.debug("Old track ending at "+oldTrackEnds[i]);
                    if (oldTrackEnds[i] < 0) {
                        int oldStationNumber = -oldTrackEnds[i];
                        // Find the old city that has this number
                        for (City oldCity2 : cities) {
                            log.debug("Old city "+oldCity2.getNumber()+" has station "+oldCity2.getRelatedStation().getNumber());
                            log.debug("  and links to new city "+oldToNewCities.get(oldCity2));
                            if (oldCity2.getRelatedStation().getNumber()
                                    == oldStationNumber
                                    && oldToNewCities.containsKey(oldCity2)) {
                                newCity = oldToNewCities.get(oldCity2);
                                oldToNewCities.put(oldCity, newCity);
                                log.debug("Assigned from "
                                        + oldCity.getUniqueId()
                                        + " #"
                                        + currentTile.getId()
                                        + "/"
                                        + currentTileRotation
                                        + " "
                                        + oldStation.getId()
                                        + " "
                                        + getConnectionString(currentTile,
                                                currentTileRotation,
                                                oldStation.getNumber())
                                        + " to " + newCity.getUniqueId()
                                        + " #" + newTile.getId() + "/"
                                        + newRotation + " "
                                        + newCity.getRelatedStation().getId() + " "
                                        + newCity.getTrackEdges());
                              break station;


                            }
                        }

                    }
                }
            }

            // Check if there any new stations not corresponding
            // to an old city.
            for (Station newStation : newTile.getStations()) {
                if (newStationsToCities.containsKey(newStation)) continue;

                // Create a new city for such a station.
                int cityNumber;
                for (cityNumber = 1; mNewCities.containsKey(cityNumber); cityNumber++)
                    ;
                newCity = new City(this, ++newCityNumber, newStation);
                newCities.add(newCity);
                mNewCities.put(cityNumber, newCity);
                newStationsToCities.put(newStation, newCity);
                newCity.setSlots(newStation.getBaseSlots());
                newTracks =
                        getConnectionString(newTile, newRotation,
                                newStation.getNumber());
                newCity.setTrackEdges(newTracks);
                log.debug("New city added " + newCity.getUniqueId() + " #"
                          + newTile.getId() + "/" + newRotation + " "
                          + newStation.getId() + " " + newTracks);
            }

            // Move the tokens
            Map<TokenI, TokenHolderI> tokenDestinations =
                    new HashMap<TokenI, TokenHolderI>();

            for (City oldCity : cities) {
                newCity = oldToNewCities.get(oldCity);
                if (newCity != null) {
                    oldtoken: for (TokenI token : oldCity.getTokens()) {
                        if (token instanceof BaseToken) {
                            // Check if the new city already has such a token
                            PublicCompanyI company =
                                    ((BaseToken) token).getCompany();
                            for (TokenI token2 : newCity.getTokens()) {
                                if (token2 instanceof BaseToken
                                    && company == ((BaseToken) token2).getCompany()) {
                                    // No duplicate tokens in one city!
                                    tokenDestinations.put(token, company);
                                    log.debug("Duplicate token "
                                              + token.getUniqueId()
                                              + " moved from "
                                              + oldCity.getName() + " to "
                                              + company.getName());
                                    ReportBuffer.add(LocalText.getText(
                                            "DuplicateTokenRemoved",
                                                    company.getName(),
                                                    getName() ));
                                    continue oldtoken;
                                }
                            }
                        }
                        tokenDestinations.put(token, newCity);
                        log.debug("Token " + token.getUniqueId()
                                  + " moved from " + oldCity.getName() + " to "
                                  + newCity.getName());
                    }
                    if (!tokenDestinations.isEmpty()) {
                        for (TokenI token : tokenDestinations.keySet()) {
                            token.moveTo(tokenDestinations.get(token));
                        }
                    }
                } else {
                    log.debug("No new city!?");
                }

            }

        }

        // Replace the tile
        new TileMove(this, currentTile, currentTileRotation, cities,
                newTile, newRotation, newCities);

        /* TODO Further consequences to be processed here, e.g. new routes etc. */
    }

    /**
     * Execute a tile replacement. This method should only be called from
     * TileMove objects. It is also used to undo tile lays.
     *
     * @param oldTile The tile to be replaced (only used for validation).
     * @param newTile The new tile to be laid on this hex.
     * @param newTileOrientation The orientation of the new tile (0-5).
     */
    public void replaceTile(TileI oldTile, TileI newTile,
            int newTileOrientation, List<City> newCities) {

        if (oldTile != currentTile) {
            new Exception("ERROR! Hex " + name + " wants to replace tile #"
                          + oldTile.getId() + " but has tile #"
                          + currentTile.getId() + "!").printStackTrace();
        }
        if (currentTile != null) {
            currentTile.remove(this);
        }

        log.debug("On hex " + name + " replacing tile " + currentTile.getId()
                  + "/" + currentTileRotation + " by " + newTile.getId() + "/"
                  + newTileOrientation);

        newTile.lay(this);

        currentTile = newTile;
        currentTileRotation = newTileOrientation;

        cities = newCities;
        mCities.clear();
        if (cities != null) {
            for (City city : cities) {
                mCities.put(city.getNumber(), city);
                log.debug("Tile #"
                          + newTile.getId()
                          + " station "
                          + city.getNumber()
                          + " has tracks to "
                          + getConnectionString(newTile, newTileOrientation,
                                  city.getRelatedStation().getNumber()));
            }
        }
        /* TODO: Further consequences to be processed here, e.g. new routes etc. */

        update(); // To notify ViewObject (Observer)

    }

    public boolean layBaseToken(PublicCompanyI company, int station) {
        if (cities == null || cities.isEmpty()) {
            log.error("Tile " + getName()
                      + " has no station for home token of company "
                      + company.getName());
            return false;
        }
        City city = mCities.get(station);

        BaseToken token = company.getFreeToken();
        if (token == null) {
            log.error("Company " + company.getName() + " has no free token");
            return false;
        } else {
            token.moveTo(city);
            return true;
        }
    }

    /**
     * Lay a bonus token.
     * @param token The bonus token object to place
     * @param phaseManager The PhaseManager is also passed in case the
     * token must register itself for removal when a certain phase starts.
     * @return
     */
    public boolean layBonusToken(BonusToken token, PhaseManager phaseManager) {
        if (token == null) {
            log.error("No token specified");
            return false;
        } else {
            token.moveTo(this);
            token.prepareForRemoval (phaseManager);
            return true;
        }
    }

    public boolean addToken(TokenI token) {

        if (offStationTokens == null)
            offStationTokens = new ArrayList<TokenI>();
        if (offStationTokens.contains(token)) {
            return false;
        } else {
            token.setHolder(this);
            return offStationTokens.add(token);
        }
    }

    public List<BaseToken> getBaseTokens () {
        if (cities == null || cities.isEmpty()) return null;
        List<BaseToken> tokens = new ArrayList<BaseToken>();
        for (City city : cities) {
            for (TokenI token : city.getTokens()) {
                if (token instanceof BaseToken) {
                    tokens.add((BaseToken)token);
                }
            }
        }
        return tokens;
    }

    public List<TokenI> getTokens() {
        return offStationTokens;
    }

    public boolean hasTokens() {
        return offStationTokens.size() > 0;
    }

    public boolean removeToken(TokenI token) {

        return offStationTokens.remove(token);
    }

    public boolean addObject(Moveable object) {
        if (object instanceof TokenI) {
            return addToken((TokenI) object);
        } else {
            return false;
        }
    }

    public boolean removeObject(Moveable object) {
        if (object instanceof TokenI) {
            return removeToken((TokenI) object);
        } else {
            return false;
        }
    }

    public boolean hasTokenSlotsLeft(int station) {
        if (station == 0) station = 1; // Temp. fix for old save files
        City city = mCities.get(station);
        if (city != null) {
            return city.hasTokenSlotsLeft();
        } else {
            log.error("Invalid station " + station + ", max is "
                      + (cities.size() - 1));
            return false;
        }
    }

    public boolean hasTokenSlotsLeft() {
        for (City city : cities) {
            if (city.hasTokenSlotsLeft()) return true;
        }
        return false;
    }

    /** Check if the tile already has a token of a company in any station */
    public boolean hasTokenOfCompany(PublicCompanyI company) {

        for (City city : cities) {
            if (city.hasTokenOf(company)) return true;
        }
        return false;
    }

    public List<TokenI> getTokens(int cityNumber) {
        if (cities.size() > 0 && mCities.get(cityNumber) != null) {
            return (mCities.get(cityNumber)).getTokens();
        } else {
            return new ArrayList<TokenI>();
        }
    }

    /**
     * Return the city number (1,...) where a company has a base token. If none,
     * return zero.
     *
     * @param company
     * @return
     */
    public int getCityOfBaseToken(PublicCompanyI company) {
        if (cities == null || cities.isEmpty()) return 0;
        for (City city : cities) {
            for (TokenI token : city.getTokens()) {
                if (token instanceof BaseToken
                    && ((BaseToken) token).getCompany() == company) {
                    return city.getNumber();
                }
            }
        }
        return 0;
    }

    public List<City> getCities() {
        return cities;
    }

    public City getCity(int cityNumber) {
        return mCities.get(cityNumber);
    }

    public void addHome(PublicCompanyI company, int cityNumber) {
        if (homes == null) homes = new HashMap<PublicCompanyI, City>();
        if (cities.isEmpty()) {
            log.error("No cities for home station on hex " + name);
        } else {
            homes.put(company, cities.get(cityNumber - 1));
        }
    }

    public Map<PublicCompanyI, City> getHomes() {
        return homes;
    }

    public boolean isHome(PublicCompanyI company) {
        boolean result = homes != null && homes.get(company) != null;
        return result;
    }

    public void addDestination(PublicCompanyI company) {
        if (destinations == null)
            destinations = new ArrayList<PublicCompanyI>();
        destinations.add(company);
    }

    public List<PublicCompanyI> getDestinations() {
        return destinations;
    }

    public boolean isDestination(PublicCompanyI company) {
        return destinations != null && destinations.contains(company);
    }

    /**
     * @return Returns the isBlocked.
     */
    public boolean isBlocked() {
        return isBlocked;
    }

    /**
     * @param isBlocked The isBlocked to set.
     */
    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    public boolean isUpgradeableNow() {
        if (isBlocked) {
            log.debug("Hex " + name + " is blocked");
            return false;
        }
        if (currentTile != null) {
            if (currentTile.isUpgradeable()) {
                return true;
            } else {
                log.debug("Hex " + name + " tile #" + currentTile.getId()
                          + " is not upgradable now");
                return false;
            }
        }
        log.debug("No tile on hex " + name);
        return false;
    }

    public boolean hasOffBoardValues() {
        return offBoardValues != null && offBoardValues.length > 0;
    }

    public int[] getOffBoardValues() {
        return offBoardValues;
    }

    public int getCurrentOffBoardValue(PhaseI phase) {
        if (hasOffBoardValues() && phase != null) {
            return offBoardValues[Math.min(
                    offBoardValues.length,
                    phase.getOffBoardRevenueStep()) - 1];
        } else {
            return 0;
        }
    }

    public String getCityName() {
        return cityName;
    }

    public boolean equals(MapHex hex) {
        if (hex.getName().equals(getName()) && hex.row == row
            && hex.column == column) return true;
        return false;
    }
    
    public MapManager getMapManager() {
        return mapManager;
    }

    @Override
    public String toString() {
        return name + " (" + row + "," + column + ")";
    }

    /**
     * The string sent to the GUIHex as it is notified. Format is
     * tileId/orientation.
     *
     * @TODO include tokens??
     */
    @Override
    public String getText() {
        return currentTile.getId() + "/" + currentTileRotation;
    }

    /**
     * Get a String describing one stations's connection directions of a laid
     * tile, taking into account the current tile rotation.
     *
     * @return
     */
    public String getConnectionString(TileI tile, int rotation,
            int stationNumber) {
        StringBuffer b = new StringBuffer("");
        if (cities != null && cities.size() > 0) {
            Map<Integer, List<Track>> tracks = tile.getTracksPerStationMap();
            if (tracks != null && tracks.get(stationNumber) != null) {
                for (Track track : tracks.get(stationNumber)) {
                    int endPoint = track.getEndPoint(-stationNumber);
                    if (endPoint < 0) continue;
                    int direction = rotation + endPoint;
                    if (b.length() > 0) b.append(",");
                    b.append(MapHex.getOrientationName(direction));
                }
            }
        }
        return b.toString();
    }

    public String getConnectionString(int cityNumber) {
        int stationNumber =
                mCities.get(cityNumber).getRelatedStation().getNumber();
        return getConnectionString(currentTile, currentTileRotation,
                stationNumber);
    }

    public int[] getTrackEndPoints(TileI tile, int rotation, Station station) {
        List<Track> tracks = tile.getTracksPerStation(station.getNumber());
        if (tracks == null) {
            return new int[0];
        }

        int[] endpoints = new int[tracks.size()];
        int endpoint;
        for (int i = 0; i < tracks.size(); i++) {
            endpoint = tracks.get(i).getEndPoint(-station.getNumber());
            if (endpoint >= 0) {
                endpoints[i] = (rotation + endpoint) % 6;
            } else {
                endpoints[i] = endpoint;
            }
        }
        return endpoints;
    }

}
