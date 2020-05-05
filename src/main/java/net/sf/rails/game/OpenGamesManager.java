package net.sf.rails.game;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.Config;
import net.sf.rails.ui.swing.GameUIManager;

public class OpenGamesManager {
    private static final Logger log = LoggerFactory.getLogger(OpenGamesManager.class);

    private static final OpenGamesManager instance = new OpenGamesManager();

    private final Map<String, GameUIManager> openGames = new HashMap<>();
    private OpenGamesManager() { }

    public static OpenGamesManager getInstance() {
        return instance;
    }

    public GameUIManager getGame(String id) {
        return openGames.get(id);
    }

    public void addGame(GameUIManager gameUIManager) {
        openGames.put(getGameIdentifier(gameUIManager), gameUIManager);
    }

    public void addGame(GameUIManager gameUIManager, boolean makeGameActive) {
        addGame(gameUIManager);
        if ( makeGameActive ) {
            makeGameActive(gameUIManager);
        }
    }

    public boolean containsGame(GameUIManager gameUIManager) {
        return openGames.containsKey(getGameIdentifier(gameUIManager));
    }

    public static String getGameIdentifier(GameUIManager gameUIManager) {
        String key = gameUIManager.getRoot().getGameData().getUsersGameName();
        if ( StringUtils.isBlank(key) ) {
            String saveDirectory = gameUIManager.getSaveDirectory();

            if ( gameUIManager.getLastSavedFilename() != null ) {
                // if the file wasn't saved in the saved directory then it's just a filename and we don't know where it is located..
                log.warn("checking {}", saveDirectory);
//                key = new File(gameUIManager.getLastSavedFilename()).getParent().substring(dirPathLength + 1);
                key = saveDirectory;
            } else {
                // TODO: figure out how to update this once a game has been saved
                key = "New Game";
            }
            // this is a hack-- we can't use the saved game file name as it changes
        }
        return key;
    }

    public void removeGame(GameUIManager gameUIManager) {
        openGames.remove(getGameIdentifier(gameUIManager));
    }

    public List<String> getGameNames() {
        return new ArrayList<>(openGames.keySet());
    }

    public List<GameState> getGameStates() {
        return openGames.values().stream().map(p -> new GameState(getGameIdentifier(p), p.getCurrentPlayer().toText())).collect(Collectors.toList());
    }

    public void makeGameActive(String name) {
        if ( openGames.containsKey(name) ) {
            openGames.values().forEach(GameUIManager::hideGame);
            openGames.get(name).showGame();
        }
    }

    public void makeGameActive(GameUIManager gameUIManager) {
        makeGameActive(getGameIdentifier(gameUIManager));
    }

    public void updatedGameState(GameUIManager gameUIManager) {
        for ( GameUIManager game : openGames.values() ) {
            game.updateStatusWindowGamesMenu();
        }
    }

    public int countOfOpenGames() {
        return openGames.size();
    }

    public static class GameState {
        protected String gameName;
        protected String currentPlayer;

        public GameState(String gameName, String currentPlayer) {
            this.gameName = gameName;
            this.currentPlayer = currentPlayer;
        }

        public String getGameName() {
            return gameName;
        }

        public String getCurrentPlayer() {
            return currentPlayer;
        }

    }

}
