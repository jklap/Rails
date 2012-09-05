package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

import com.google.common.base.Objects;

import rails.game.*;
import rails.game.special.SpecialProperty;
import rails.game.special.SpecialTileLay;
import rails.util.Util;

public class LayTile extends PossibleORAction {

    /* LayTile types */
    public final static int GENERIC = 0; // Stop-gap only
    public final static int LOCATION_SPECIFIC = 1; // Valid hex and allowed
    // tiles
    public final static int SPECIAL_PROPERTY = 2; // Directed by a special
    // property

    protected int type = 0;

    /*--- Preconditions ---*/

    /** Where to lay a tile (null means anywhere) */
    transient private List<MapHex> locations = null;
    private String locationNames;

    /** Highest tile colour (empty means unspecified) */
    private Map<String, Integer> tileColours = null;

    /** Allowed tiles on a specific location (empty means unspecified) */
    transient private List<Tile> tiles = null;
    private int[] tileIds;

    /**
     * Special property that will be fulfilled by this tile lay. If null, this
     * is a normal tile lay.
     */
    transient private SpecialTileLay specialProperty = null;
    private int specialPropertyId;

    /**
     * Need base tokens be relaid?
     */
    private boolean relayBaseTokens = false;

    /*--- Postconditions ---*/

    /** The tile actually laid */
    transient private Tile laidTile = null;
    private int laidTileId;

    /** The map hex on which the tile is laid */
    transient private MapHex chosenHex = null;
    private String chosenHexName;

    /** The tile orientation */
    private int orientation;

    /** Any manually assigned base token positions */
    private Map<String, Integer> relaidBaseTokens = null;
    private String relaidBaseTokensString = null;

    public static final long serialVersionUID = 1L;

    public LayTile(Map<String, Integer> tileColours) {
        type = GENERIC;
        setTileColours (tileColours);
        // NOTE: tileColours is currently only used for Help purposes.
    }

    public LayTile(SpecialTileLay specialProperty) {
        type = SPECIAL_PROPERTY;
        this.locations = specialProperty.getLocations();
        if (locations != null) buildLocationNameString();
        this.specialProperty = specialProperty;
        if (specialProperty != null) {
            this.specialPropertyId = specialProperty.getUniqueId();
            Tile tile = specialProperty.getTile();
            if (tile != null) {
                tiles = new ArrayList<Tile>();
                tiles.add(tile);
            }
        }

        
    }

    /**
     * @return Returns the chosenHex.
     */
    public MapHex getChosenHex() {
        return chosenHex;
    }

    /**
     * @param chosenHex The chosenHex to set.
     */
    public void setChosenHex(MapHex chosenHex) {
        this.chosenHex = chosenHex;
        this.chosenHexName = chosenHex.getId();
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    /**
     * @return Returns the laidTile.
     */
    public Tile getLaidTile() {
        return laidTile;
    }

    /**
     * @param laidTile The laidTile to set.
     */
    public void setLaidTile(Tile laidTile) {
        this.laidTile = laidTile;
        this.laidTileId = laidTile.getNb();
    }

    /**
     * @return Returns the specialProperty.
     */
    public SpecialTileLay getSpecialProperty() {
        return specialProperty;
    }

    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialTileLay specialProperty) {
        this.specialProperty = specialProperty;
        // TODO this.specialPropertyName = specialProperty.getUniqueId();
    }

    /**
     * @return Returns the tiles.
     */
    public List<Tile> getTiles() {
        return tiles;
    }

    /**
     * @param tiles The tiles to set.
     */
    public void setTiles(List<Tile> tiles) {
        this.tiles = tiles;
        this.tileIds = new int[tiles.size()];
        for (int i = 0; i < tiles.size(); i++) {
            tileIds[i] = tiles.get(i).getNb();
        }
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public int getType() {
        return type;
    }

    /**
     * @return Returns the tileColours.
     */
    public Map<String, Integer> getTileColours() {
        return tileColours;
    }

    public boolean isTileColourAllowed(String tileColour) {
        return tileColours != null
            && tileColours.containsKey(tileColour)
            && tileColours.get(tileColour) > 0;
    }

    public void setTileColours(Map<String, Integer> map) {
        tileColours = new HashMap<String, Integer>();
        // Check the map. Sometimes 0 values creep in, and these can't easily
        // be intercepted in the UI code (see comment at previous method).
        // TODO This is a dirty fix, but the quickest one too.
        if (map != null) {
            for (String colourName : map.keySet()) {
                if (map.get(colourName) > 0) tileColours.put(colourName, map.get(colourName));
            }
        }
    }


    public boolean isRelayBaseTokens() {
        return relayBaseTokens;
    }

    public void setRelayBaseTokens(boolean relayBaseTokens) {
        this.relayBaseTokens = relayBaseTokens;
    }

    public void addRelayBaseToken (String companyName, Integer cityNumber) {
        if (relaidBaseTokens == null) {
            relaidBaseTokens = new HashMap<String, Integer>();
        }
        relaidBaseTokens.put(companyName, cityNumber);
        relaidBaseTokensString = Util.appendWithDelimiter(relaidBaseTokensString,
                Util.appendWithDelimiter(companyName, String.valueOf(cityNumber), ":"),
                ",");
    }

    public Map<String, Integer> getRelaidBaseTokens() {
        return relaidBaseTokens;
    }

    @Override
    public boolean equalsAsOption(PossibleAction action) {
        if (!(action.getClass() == LayTile.class)) return false;
        LayTile a = (LayTile) action;
        return (Objects.equal(a.locationNames, locationNames)
               && a.type == type
               && Objects.equal(a.tiles, tiles) 
               && Objects.equal(a.specialProperty, specialProperty));
//              && a.tileColours == tileColours
        // TODO: Replace this by testing if the tile is part of the map 
        // Remark: this test is invalid as sometimes zero values got stored
        // This is a quick fix for Rails2.0 (see comment above)
    }

    @Override
    public boolean equalsAsAction (PossibleAction action) {
        if (!(action instanceof LayTile)) return false;
        LayTile a = (LayTile) action;
        return (a.laidTileId == laidTileId
               && a.chosenHexName.equals(chosenHexName)
               && a.orientation == orientation
               && (a.relaidBaseTokensString == null && relaidBaseTokensString == null
                       || a.relaidBaseTokensString.equals(relaidBaseTokensString)));
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("LayTile");
        if (laidTile == null) {
            b.append(" type=").append(type);
            if (locations != null)
                b.append(" location=").append(locationNames);
            if (specialProperty != null)
                b.append(" spec.prop=").append(specialProperty);
            if (tileColours != null && !tileColours.isEmpty()) {
                int value;
                for (String key : tileColours.keySet()) {
                    value = tileColours.get(key);
                    b.append(" ").append(key).append(":").append(value);
                }
            }
        } else {
            b.append(" tile=").append(laidTile.getNb()).append(" hex=").append(
                    chosenHex.getId()).append(" orientation=").append(
                    orientation).append(" tokens=").append(relaidBaseTokensString);
        }
        return b.toString();
    }

    /** Deserialize */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        //in.defaultReadObject();
        // Custom reading for backwards compatibility
        ObjectInputStream.GetField fields = in.readFields();
        locationNames = (String) fields.get("locationNames", locationNames);
        tileColours = (Map<String, Integer>) fields.get("tileColours", tileColours);
        tileIds = (int[]) fields.get("tileIds", tileIds);
        specialPropertyId = fields.get("specialPropertyId", specialPropertyId);
        laidTileId = fields.get("laidTileId", laidTileId);
        chosenHexName = (String) fields.get("chosenHexName", chosenHexName);
        orientation = fields.get("orientation", orientation);
        relayBaseTokens = fields.get("relayBaseTokens", relayBaseTokens);
        relaidBaseTokens = (Map<String,Integer>)fields.get("relaidBaseTokens", relaidBaseTokens);
        relaidBaseTokensString = (String) fields.get("relaidBaseTokensString", relaidBaseTokensString);

        MapManager mmgr = gameManager.getMapManager();
        TileManager tmgr = gameManager.getTileManager();
        locations = new ArrayList<MapHex>();
        if (Util.hasValue(locationNames)) {
            for (String hexName : locationNames.split(",")) {
                locations.add(mmgr.getHex(hexName));
            }
        }

        if (tileIds != null && tileIds.length > 0) {
            tiles = new ArrayList<Tile>();
            for (int i = 0; i < tileIds.length; i++) {
                tiles.add(tmgr.getTile(tileIds[i]));
            }
        }
        if (specialPropertyId > 0) {
            specialProperty =
                    (SpecialTileLay) SpecialProperty.getByUniqueId(specialPropertyId);
        }
        if (laidTileId != 0) {
            laidTile = tmgr.getTile(laidTileId);
        }
        if (chosenHexName != null && chosenHexName.length() > 0) {
            chosenHex = mmgr.getHex(chosenHexName);
        }

    }

    private void buildLocationNameString() {
        StringBuffer b = new StringBuffer();
        for (MapHex hex : locations) {
            if (b.length() > 0) b.append(",");
            b.append(hex.getId());
        }
        locationNames = b.toString();
    }
}
