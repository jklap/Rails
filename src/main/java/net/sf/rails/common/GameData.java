package net.sf.rails.common;

import java.util.List;

public class GameData {
    private final GameInfo game;

    private final GameOptionsSet gameOptions;

    private final List<String> players;

    private final String usersGameName;

    private GameData(GameInfo game, GameOptionsSet gameOptions, List<String> players, String usersGameName) {
        this.game = game;
        this.gameOptions = gameOptions;
        this.players = players;
        this.usersGameName = usersGameName;
    }

    public static GameData create(GameInfo game, GameOptionsSet.Builder gameOptions, List<String> players, String usersGameName) {
        return new GameData(game, gameOptions.withNumberOfPlayers(players.size()).build(), players, usersGameName);
    }

    public String getGameName() {
        return game.getName();
    }

    public String getUsersGameName() {
        return usersGameName;
    }

    public GameOptionsSet getGameOptions() {
        return gameOptions;
    }

    public List<String> getPlayers() {
        return players;
    }
}
