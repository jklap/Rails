/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PlayerManager.java,v 1.8 2009/09/03 21:36:53 evos Exp $ */
package rails.game;

import java.util.*;

import rails.util.LocalText;
import rails.util.Tag;

public class PlayerManager implements ConfigurableComponentI {

    private int numberOfPlayers;
    private List<Player> players;
    private List<String> playerNames;
    private Map<String, Player> playerMap;

    public int maxPlayers;

    public int minPlayers;

    private int[] playerStartCash = new int[Player.MAX_PLAYERS];

    private int[] playerCertificateLimits = new int[Player.MAX_PLAYERS];

    private int playerCertificateLimit = 0;

    public PlayerManager() {
        
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        int number, startCash, certLimit;

        List<Tag> playerTags = tag.getChildren("Players");
        minPlayers = 99;
        maxPlayers = 0;
        for (Tag playerTag : playerTags) {
            number = playerTag.getAttributeAsInteger("number");
            startCash = playerTag.getAttributeAsInteger("cash");
            playerStartCash[number] = startCash;
            certLimit = playerTag.getAttributeAsInteger("certLimit");
            playerCertificateLimits[number] = certLimit;
    
            minPlayers = Math.min(minPlayers, number);
            maxPlayers = Math.max(maxPlayers, number);
        }
    }
    
   public void setPlayers (List<String> playerNames, int startCash) {
        
        Player player;

        this.playerNames = playerNames;
        numberOfPlayers = playerNames.size();

        players = new ArrayList<Player>(numberOfPlayers);
        playerMap = new HashMap<String, Player>(numberOfPlayers);

        int playerIndex = 0;
        for (String playerName : playerNames) {
            player = new Player(playerName, playerIndex++);
            players.add(player);
            playerMap.put(playerName, player);
            Bank.transferCash(null, player, getStartCash());
            ReportBuffer.add(LocalText.getText("PlayerIs",
                    playerIndex,
                    player.getName() ));
        }
        ReportBuffer.add(LocalText.getText("PlayerCash", Bank.format(startCash)));
        ReportBuffer.add(LocalText.getText("BankHas",
                Bank.format(Bank.getInstance().getCash())));

        playerCertificateLimit = playerCertificateLimits[numberOfPlayers];
    }

    /**
     * @return Returns an array of all players.
     */
    public List<Player> getPlayers() {
        return players;
    }

    public Player getPlayerByName(String name) {
        return playerMap.get(name);
    }

    public List<String> getPlayerNames() {
        return playerNames;
    }

    public Player getPlayerByIndex(int index) {
        return players.get(index);
    }
    
    public int getStartCash () {
        return playerStartCash[numberOfPlayers];
    }

    public int getPlayerCertificateLimit() {
        return playerCertificateLimit;
    }

    public void setPlayerCertificateLimit(int playerCertificateLimit) {
        this.playerCertificateLimit = playerCertificateLimit;
    }

}
