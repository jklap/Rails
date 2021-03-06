package net.sf.rails.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rails.game.action.PossibleAction;

import net.sf.rails.common.GameData;


/**
 * Combines all elements for gameIO
 */
class GameIOData {

    private GameData gameData;
    private String version;
    private String date;
    private long fileVersionID;
    private List<PossibleAction> actions;

    private final Map<String, String> gameConfig = new HashMap<>();

    GameIOData(GameData gameData, String version, String date, Long fileVersionID, List<PossibleAction> actions) {
        this.gameData = gameData;
        this.version = version;
        this.date = date;
        this.fileVersionID = fileVersionID;
        this.actions = actions;
    }
    
    GameIOData() {}
    
    
    void setGameData(GameData gameData) {
        this.gameData = gameData;
    }
    
    GameData getGameData() {
        return gameData;
    }
    
    void setVersion(String version) {
        this.version = version;
    }
    
    String getVersion() {
        return version;
    }
    
    void setDate(String date) {
        this.date = date;
    }
    
    String getDate() {
        return date;
    }
    
    void setFileVersionID(long fileVersionID) {
        this.fileVersionID = fileVersionID;
    }
    
    long getFileVersionID() {
        return fileVersionID;
    }
    
    void setActions(List<PossibleAction> actions) {
        this.actions = actions;
    }

    List<PossibleAction> getActions() {
        return actions;
    }

    public Map<String, String> getGameConfig() {
        return gameConfig;
    }

    public void addGameConfig(String name, String value) {
        gameConfig.put(name, value);
    }

    public void setGameConfig(Map<String, String> config) {
        gameConfig.clear();
        gameConfig.putAll(config);
    }

    String metaDataAsText() {
        StringBuilder s = new StringBuilder();
        s.append("Rails saveVersion = ").append(version).append("\n");
        s.append("File was saved at ").append(date).append("\n");
        s.append("Saved versionID=").append(fileVersionID).append("\n");
        s.append("Save game=").append(gameData.getGameName()).append("\n");
        return s.toString();
    }

    String gameOptionsAsText() {
        StringBuilder s = new StringBuilder();
        for (String key : gameData.getGameOptions().getOptions().keySet()) {
            s.append("Option ").append(key).append("=").append(gameData.getGameOptions().get(key)).append("\n");
        }
        return s.toString();
    }

    String playerNamesAsText() {
        StringBuilder s = new StringBuilder();
        int i=1;
        for (String player : gameData.getPlayers()) {
            s.append("Player ").append(i++).append(": ").append(player).append("\n");
        }
        return s.toString();
    }

}
