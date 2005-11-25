/*
 * Created on 30-Apr-2005
 */
package game;

import java.util.List;

/**
 * A common interface to the various "Rounds". A Round is defined as any process
 * in an 18xx game where different players have "turns".
 * 
 * @author Erik Vos
 */
public interface Round
{

	/**
	 * Get the player that has the next turn.
	 * 
	 * @return Player object.
	 */
	public Player getCurrentPlayer();
	
	public String getHelp();
	
	public List getSpecialProperties();

}
