/*
 * Created on Apr 29, 2005
 */
package ui;

import game.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import ui.StatusWindow;
import ui.elements.*;

import java.util.*;

/**
 * This class is incorporated into StatusWindow and displays the bulk of game
 * status information.
 * 
 * @author blentz
 */
public class GameStatus extends JPanel implements ActionListener
{

	private static final int NARROW_GAP = 1;
	private static final int WIDE_GAP = 3;
	private static final int WIDE_LEFT = 1;
	private static final int WIDE_RIGHT = 2;
	private static final int WIDE_TOP = 4;
	private static final int WIDE_BOTTOM = 8;

	private static GameStatus gameStatus;
	private JFrame parent;

	private GridBagLayout gb;
	private GridBagConstraints gbc;
	private Color buttonHighlight = new Color(255, 160, 80);

	// Grid elements per function
	private Field certPerPlayer[][];
	private ClickField certPerPlayerButton[][];
	private int certPerPlayerXOffset, certPerPlayerYOffset;
	private Field certInIPO[];
	private ClickField certInIPOButton[];
	private int certInIPOXOffset, certInIPOYOffset;
	private Field certInPool[];
	private ClickField certInPoolButton[];
	private int certInPoolXOffset, certInPoolYOffset;
	private Field parPrice[];
	private int parPriceXOffset, parPriceYOffset;
	private Field currPrice[];
	private int currPriceXOffset, currPriceYOffset;
	private Field compCash[];
	private int compCashXOffset, compCashYOffset;
	private Field compRevenue[];
	private int compRevenueXOffset, compRevenueYOffset;
	private Field compTrains[];
	private int compTrainsXOffset, compTrainsYOffset;
	private Field compPrivates[];
	private int compPrivatesXOffset, compPrivatesYOffset;
	private Field playerCash[];
	private int playerCashXOffset, playerCashYOffset;
	private Field playerPrivates[];
	private int playerPrivatesXOffset, playerPrivatesYOffset;
	private Field playerWorth[];
	private int playerWorthXOffset, playerWorthYOffset;
	private Field bankCash;
	private int bankCashXOffset, bankCashYOffset;
	private Field poolTrains;
	private int poolTrainsXOffset, poolTrainsYOffset;
	private Field newTrains;
	private int newTrainsXOffset, newTrainsYOffset;
	private Field futureTrains;
	private int futureTrainsXOffset, futureTrainsYOffset;

	private Caption[] upperPlayerCaption;
	private Caption[] lowerPlayerCaption;

	private int np; // Number of players
	private int nc; // NUmber of companies
	private Player[] players;
	private PublicCompanyI[] companies;
	private CompanyManagerI cm;

	private Player p;
	private PublicCompanyI c;
	private JComponent f;

	// Current state
	private int srPlayerIndex = -1;
	private int compSellIndex = -1;
	private int compBuyIPOIndex = -1;
	private int compBuyPoolIndex = -1;

	private ButtonGroup buySellGroup = new ButtonGroup();
	private ClickField dummyButton; // To be selected if none else is.

	private Map companyIndex = new HashMap();
	private Map playerIndex = new HashMap();

	public GameStatus(JFrame parent)
	{
		super();
		gameStatus = this;
		this.parent = parent;

		gb = new GridBagLayout();
		this.setLayout(gb);
		UIManager.put("ToggleButton.select", buttonHighlight);

		gbc = new GridBagConstraints();
		// updateStatus();
		setSize(800, 300);
		setLocation(0, 450);
		setBorder(BorderFactory.createEtchedBorder());
		setOpaque(false);

		players = Game.getPlayerManager().getPlayersArray();
		np = GameManager.getNumberOfPlayers();
		cm = Game.getCompanyManager();
		companies = (PublicCompanyI[]) cm.getAllPublicCompanies()
				.toArray(new PublicCompanyI[0]);
		nc = companies.length;

		init();

	}

	private void init()
	{

		certPerPlayer = new Field[nc][np];
		certPerPlayerButton = new ClickField[nc][np];
		certInIPO = new Field[nc];
		certInIPOButton = new ClickField[nc];
		certInPool = new Field[nc];
		certInPoolButton = new ClickField[nc];
		parPrice = new Field[nc];
		currPrice = new Field[nc];
		compCash = new Field[nc];
		compRevenue = new Field[nc];
		compTrains = new Field[nc];
		compPrivates = new Field[nc];
		playerCash = new Field[np];
		playerPrivates = new Field[np];
		playerWorth = new Field[np];
		upperPlayerCaption = new Caption[np];
		lowerPlayerCaption = new Caption[np];

		certPerPlayerXOffset = 1;
		certPerPlayerYOffset = 2;
		certInIPOXOffset = np + 1;
		certInIPOYOffset = 2;
		certInPoolXOffset = np + 2;
		certInPoolYOffset = 2;
		parPriceXOffset = np + 3;
		parPriceYOffset = 2;
		currPriceXOffset = np + 4;
		currPriceYOffset = 2;
		compCashXOffset = np + 5;
		compCashYOffset = 2;
		compRevenueXOffset = np + 6;
		compRevenueYOffset = 2;
		compTrainsXOffset = np + 7;
		compTrainsYOffset = 2;
		compPrivatesXOffset = np + 8;
		compPrivatesYOffset = 2;
		playerCashXOffset = 1;
		playerCashYOffset = nc + 2;
		playerPrivatesXOffset = 1;
		playerPrivatesYOffset = nc + 3;
		playerWorthXOffset = 1;
		playerWorthYOffset = nc + 4;
		bankCashXOffset = np + 2;
		bankCashYOffset = nc + 3;
		poolTrainsXOffset = np + 3;
		poolTrainsYOffset = nc + 3;
		newTrainsXOffset = np + 5;
		newTrainsYOffset = nc + 3;
		futureTrainsXOffset = np + 6;
		futureTrainsYOffset = nc + 3;

		addField(new Caption("Company"), 0, 0, 1, 2, WIDE_RIGHT + WIDE_BOTTOM);
		addField(new Caption("Players"), certPerPlayerXOffset, 0, np, 1, 0);
		for (int i = 0; i < np; i++)
		{
			playerIndex.put(players[i], new Integer(i));
			f = upperPlayerCaption[i] = new Caption(players[i].getName());
			addField(f, certPerPlayerXOffset + i, 1, 1, 1, WIDE_BOTTOM);
		}
		addField(new Caption("Bank shares"),
				certInIPOXOffset,
				0,
				2,
				1,
				WIDE_LEFT + WIDE_RIGHT);
		addField(new Caption("IPO"), certInIPOXOffset, 1, 1, 1, WIDE_LEFT
				+ WIDE_BOTTOM);
		addField(new Caption("Pool"), certInPoolXOffset, 1, 1, 1, WIDE_RIGHT
				+ WIDE_BOTTOM);
		addField(new Caption("Prices"), parPriceXOffset, 0, 2, 1, WIDE_RIGHT);
		addField(new Caption("Par"), parPriceXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("Curr"), currPriceXOffset, 1, 1, 1, WIDE_RIGHT
				+ WIDE_BOTTOM);
		addField(new Caption("Company details"), compCashXOffset, 0, 4, 1, 0);
		addField(new Caption("Cash"), compCashXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("Rev"), compRevenueXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("Trains"), compTrainsXOffset, 1, 1, 1, WIDE_BOTTOM);
		addField(new Caption("Privates"),
				compPrivatesXOffset,
				1,
				1,
				1,
				WIDE_BOTTOM);
		addField(new Caption("Company"),
				compPrivatesXOffset + 1,
				0,
				1,
				2,
				WIDE_LEFT + WIDE_BOTTOM);

		for (int i = 0; i < nc; i++)
		{
			c = companies[i];
			companyIndex.put(c, new Integer(i));
			f = new Caption(c.getName());
			f.setForeground(c.getFgColour());
			f.setBackground(c.getBgColour());
			addField(f, 0, certPerPlayerYOffset + i, 1, 1, WIDE_RIGHT);

			for (int j = 0; j < np; j++)
			{
				f = certPerPlayer[i][j] = new Field("");
				addField(f,
						certPerPlayerXOffset + j,
						certPerPlayerYOffset + i,
						1,
						1,
						0);
				f = certPerPlayerButton[i][j] = new ClickField("",
						"Sell",
						"Click to select for selling",
						this,
						buySellGroup);
				addField(f,
						certPerPlayerXOffset + j,
						certPerPlayerYOffset + i,
						1,
						1,
						0);
			}
			f = certInIPO[i] = new Field(Bank.getIpo().ownsShare(c) + "%");
			addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, WIDE_LEFT);
			f = certInIPOButton[i] = new ClickField(Bank.getIpo().ownsShare(c)
					+ "%",
					"BuyIPO",
					"Click to select for buying",
					this,
					buySellGroup);
			f.setVisible(false);
			addField(f, certInIPOXOffset, certInIPOYOffset + i, 1, 1, WIDE_LEFT);

			f = certInPool[i] = new Field("");
			addField(f,
					certInPoolXOffset,
					certInPoolYOffset + i,
					1,
					1,
					WIDE_RIGHT);
			f = certInPoolButton[i] = new ClickField("",
					"BuyPool",
					"Click to buy",
					this,
					buySellGroup);
			f.setVisible(false);
			addField(f,
					certInPoolXOffset,
					certInPoolYOffset + i,
					1,
					1,
					WIDE_RIGHT);

			f = parPrice[i] = new Field(c.getParPrice() != null ? Bank.format(c.getParPrice()
					.getPrice())
					: "");
			addField(f, parPriceXOffset, parPriceYOffset + i, 1, 1, 0);
			f = currPrice[i] = new Field(c.getCurrentPrice() != null ? Bank.format(c.getCurrentPrice()
					.getPrice())
					: "");
			addField(f,
					currPriceXOffset,
					currPriceYOffset + i,
					1,
					1,
					WIDE_RIGHT);

			f = compCash[i] = new Field(Bank.format(c.getCash()));
			addField(f, compCashXOffset, compCashYOffset + i, 1, 1, 0);
			f = compRevenue[i] = new Field(Bank.format(c.getLastRevenue()));
			addField(f, compRevenueXOffset, compRevenueYOffset + i, 1, 1, 0);
			f = compTrains[i] = new Field(TrainManager.get()
					.makeFullList(c.getPortfolio()));
			addField(f, compTrainsXOffset, compTrainsYOffset + i, 1, 1, 0);
			f = compPrivates[i] = new Field("");
			addField(f, compPrivatesXOffset, compPrivatesYOffset + i, 1, 1, 0);
			f = new Caption(c.getName());
			f.setForeground(c.getFgColour());
			f.setBackground(c.getBgColour());
			addField(f,
					compPrivatesXOffset + 1,
					compPrivatesYOffset + i,
					1,
					1,
					WIDE_LEFT);
		}

		// Player possessions
		addField(new Caption("Cash"), 0, playerCashYOffset, 1, 1, WIDE_TOP
				+ WIDE_RIGHT);
		for (int i = 0; i < np; i++)
		{
			f = playerCash[i] = new Field(Bank.format(players[i].getCash()));
			addField(f,
					playerCashXOffset + i,
					playerCashYOffset,
					1,
					1,
					WIDE_TOP);
		}

		addField(new Caption("Privates"),
				0,
				playerPrivatesYOffset,
				1,
				1,
				WIDE_RIGHT);
		for (int i = 0; i < np; i++)
		{
			f = playerPrivates[i] = new Field("");
			addField(f,
					playerPrivatesXOffset + i,
					playerPrivatesYOffset,
					1,
					1,
					0);
		}

		addField(new Caption("Worth"), 0, playerWorthYOffset, 1, 1, WIDE_RIGHT);
		for (int i = 0; i < np; i++)
		{
			f = playerWorth[i] = new Field(Bank.format(players[i].getWorth()));
			addField(f, playerWorthXOffset + i, playerWorthYOffset, 1, 1, 0);
		}

		for (int i = 0; i < np; i++)
		{
			f = lowerPlayerCaption[i] = new Caption(players[i].getName());
			addField(f, i + 1, playerWorthYOffset + 1, 1, 1, WIDE_TOP);
		}

		// Bank
		addField(new Caption("Bank"),
				bankCashXOffset - 1,
				bankCashYOffset - 1,
				1,
				2,
				WIDE_TOP + WIDE_LEFT);
		addField(new Caption("Cash"),
				bankCashXOffset,
				bankCashYOffset - 1,
				1,
				1,
				WIDE_TOP);
		bankCash = new Field(Bank.format(Bank.getInstance().getCash()));
		addField(bankCash, bankCashXOffset, bankCashYOffset, 1, 1, 0);
		addField(new Caption("Used trains"),
				poolTrainsXOffset,
				poolTrainsYOffset - 1,
				2,
				1,
				WIDE_TOP + WIDE_RIGHT);
		poolTrains = new Field(TrainManager.get().makeFullList(Bank.getPool()));
		addField(poolTrains,
				poolTrainsXOffset,
				poolTrainsYOffset,
				2,
				1,
				WIDE_RIGHT);

		// New trains
		addField(new Caption("New tr."),
				newTrainsXOffset,
				newTrainsYOffset - 1,
				1,
				1,
				WIDE_TOP);
		newTrains = new Field(TrainManager.get()
				.makeAbbreviatedList(Bank.getIpo()));
		addField(newTrains, newTrainsXOffset, newTrainsYOffset, 1, 1, 0);

		dummyButton = new ClickField("", "", "", this, buySellGroup);

		// Future trains
		addField(new Caption("Future trains"),
				futureTrainsXOffset,
				futureTrainsYOffset - 1,
				3,
				1,
				WIDE_LEFT + WIDE_TOP);
		futureTrains = new Field(TrainManager.get()
				.makeAbbreviatedList(Bank.getUnavailable()));
		addField(futureTrains,
				futureTrainsXOffset,
				futureTrainsYOffset,
				3,
				1,
				WIDE_LEFT);

		dummyButton = new ClickField("", "", "", this, buySellGroup);

	}

	private void addField(JComponent comp, int x, int y, int width, int height,
			int wideGapPositions)
	{

		int padTop, padLeft, padBottom, padRight;
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.gridheight = height;
		gbc.weightx = gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.BOTH;
		padTop = (wideGapPositions & WIDE_TOP) > 0 ? WIDE_GAP : NARROW_GAP;
		padLeft = (wideGapPositions & WIDE_LEFT) > 0 ? WIDE_GAP : NARROW_GAP;
		padBottom = (wideGapPositions & WIDE_BOTTOM) > 0 ? WIDE_GAP
				: NARROW_GAP;
		padRight = (wideGapPositions & WIDE_RIGHT) > 0 ? WIDE_GAP : NARROW_GAP;
		gbc.insets = new Insets(padTop, padLeft, padBottom, padRight);

		add(comp, gbc);

	}

	public static GameStatus getInstance()
	{
		return gameStatus;
	}

	public void repaint()
	{
		super.repaint();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent actor)
	{
		JComponent source = (JComponent) actor.getSource();
		String command = actor.getActionCommand();
		if (source instanceof ClickField)
		{
			gbc = gb.getConstraints(source);
			if (command.equals("Sell"))
			{
				compSellIndex = gbc.gridy - certPerPlayerYOffset;
				compBuyIPOIndex = compBuyPoolIndex = -1;
				((StatusWindow) parent).enableSellButton(true);
			}
			else if (command.equals("BuyIPO"))
			{
				compBuyIPOIndex = gbc.gridy - certInIPOYOffset;
				compSellIndex = compBuyPoolIndex = -1;
				((StatusWindow) parent).enableBuyButton(true);
			}
			else if (command.equals("BuyPool"))
			{
				compBuyPoolIndex = gbc.gridy - certInPoolYOffset;
				compSellIndex = compBuyIPOIndex = -1;
				((StatusWindow) parent).enableBuyButton(true);
			}
		}
		repaint();

	}

	public int getCompIndexToSell()
	{
		return compSellIndex;
	}

	public int getCompIndexToBuyFromIPO()
	{
		return compBuyIPOIndex;
	}

	public int getCompIndexToBuyFromPool()
	{
		return compBuyPoolIndex;
	}

	public void updatePlayer(int compIndex, int playerIndex)
	{
		int share = players[playerIndex].getPortfolio()
				.ownsShare(companies[compIndex]);
		String text = share > 0 ? share + "%" : "";
		certPerPlayer[compIndex][playerIndex].setText(text);
		certPerPlayerButton[compIndex][playerIndex].setText(text);
		if (share == 0)
			setPlayerCertButton(compIndex, playerIndex, false);
		playerCash[playerIndex].setText(Bank.format(players[playerIndex].getCash()));
		playerWorth[playerIndex].setText(Bank.format(players[playerIndex].getWorth()));
	}

	public void updateIPO(int compIndex)
	{
		int share = Bank.getIpo().ownsShare(companies[compIndex]);
		String text = share > 0 ? share + "%" : "";
		certInIPO[compIndex].setText(text);
		certInIPOButton[compIndex].setText(text);
		if (share == 0)
			setIPOCertButton(compIndex, false);
		parPrice[compIndex].setText(Bank.format(companies[compIndex].getParPrice()
				.getPrice()));
		currPrice[compIndex].setText(Bank.format(companies[compIndex].getCurrentPrice()
				.getPrice()));
	}

	public void updatePool(int compIndex)
	{
		int share = Bank.getPool().ownsShare(companies[compIndex]);
		String text = share > 0 ? share + "%" : "";
		certInPool[compIndex].setText(text);
		certInPoolButton[compIndex].setText(text);
		if (share == 0)
			setPoolCertButton(compIndex, false);
		currPrice[compIndex].setText(Bank.format(companies[compIndex].getCurrentPrice()
				.getPrice()));
	}

	public void updateBank()
	{
		bankCash.setText(Bank.format(Bank.getInstance().getCash()));
	}

	public void updateCompany(int compIndex)
	{
		compCash[compIndex].setText(Bank.format(companies[compIndex].getCash()));
		if (companies[compIndex].hasStockPrice())
		{
			currPrice[compIndex].setText(Bank.format(companies[compIndex].getCurrentPrice()
					.getPrice()));
		}
	}

	public void setSRPlayerTurn(int selectedPlayerIndex)
	{

		int i, j, share;

		dummyButton.setSelected(true);

		if ((j = this.srPlayerIndex) >= 0)
		{
			upperPlayerCaption[j].setHighlight(false);
			lowerPlayerCaption[j].setHighlight(false);
			for (i = 0; i < nc; i++)
			{
				setPlayerCertButton(i, j, false);
			}
		}

		this.srPlayerIndex = selectedPlayerIndex;

		if ((j = this.srPlayerIndex) >= 0)
		{

			StockRound stockRound = (StockRound) GameManager.getInstance()
					.getCurrentRound();

			upperPlayerCaption[j].setHighlight(true);
			lowerPlayerCaption[j].setHighlight(true);
			for (i = 0; i < nc; i++)
			{
				share = players[j].getPortfolio().ownsShare(companies[i]);
				if (share > 0
						&& stockRound.isCompanySellable(companies[i].getName()))
				{
					setPlayerCertButton(i, j, true);
				}
			}

			for (i = 0; i < nc; i++)
			{
				if ((share = Bank.getIpo().ownsShare(companies[i])) > 0
						&& (stockRound.isCompanyBuyable(companies[i].getName(),
								Bank.getIpo()) || stockRound.isCompanyStartable(companies[i].getName())))
				{
					setIPOCertButton(i, true);
				}
			}

			for (i = 0; i < nc; i++)
			{
				if ((share = Bank.getPool().ownsShare(companies[i])) > 0
						&& stockRound.isCompanyBuyable(companies[i].getName(),
								Bank.getPool()))
				{
					setPoolCertButton(i, true);
				}
			}
		}
		else
		{
			for (i = 0; i < nc; i++)
			{
				setIPOCertButton(i, false);
				setPoolCertButton(i, false);
			}
		}

		((StatusWindow) parent).enableBuyButton(false);
		((StatusWindow) parent).enableSellButton(false);
		repaint();
	}

	public String getSRPlayer()
	{
		if (srPlayerIndex >= 0)
			return players[srPlayerIndex].getName();
		else
			return "";
	}

	private void setPlayerCertButton(int i, int j, boolean clickable)
	{
		if (clickable)
		{
			certPerPlayerButton[i][j].setText(certPerPlayer[i][j].getText());
		}
		certPerPlayer[i][j].setVisible(!clickable);
		certPerPlayerButton[i][j].setVisible(clickable);
	}

	private void setIPOCertButton(int i, boolean clickable)
	{
		if (clickable)
		{
			certInIPOButton[i].setText(certInIPO[i].getText());
		}
		certInIPO[i].setVisible(!clickable);
		certInIPOButton[i].setVisible(clickable);
	}

	private void setPoolCertButton(int i, boolean clickable)
	{
		if (clickable)
		{
			certInPoolButton[i].setText(certInPool[i].getText());
		}
		certInPool[i].setVisible(!clickable);
		certInPoolButton[i].setVisible(clickable);
	}

	/**
	 * ----- Actions from other windows that have consequences here -----
	 * (perhaps we should do all this via Events) We will check all players, as
	 * some items my have been assigned "behind the curtains".
	 */
	public void updatePlayers()
	{

		Player p;
		Portfolio pf;
		String compName;
		int compIndex, share;

		for (int i = 0; i < np; i++)
		{
			p = players[i];
			pf = p.getPortfolio();

			Iterator it = pf.getCertPerCompany().keySet().iterator();
			while (it.hasNext())
			{
				compName = (String) it.next();
				compIndex = cm.getPublicCompany(compName).getPublicNumber();
				share = pf.ownsShare(companies[compIndex]);
				String text = share > 0 ? share + "%" : "";
				certPerPlayer[compIndex][i].setText(text);
			}
			updatePlayerPrivates(i);
			updateCash(p);
		}
		parent.pack();
	}

	public void updateCash(CashHolder c)
	{
		if (c instanceof Player)
		{
			int i = ((Integer) playerIndex.get(c)).intValue();
			setCash(playerCash[i], c);
			playerWorth[i].setText(Bank.format(((Player) c).getWorth()));
		}
		else if (c instanceof PublicCompanyI)
		{
			int i = ((Integer) companyIndex.get(c)).intValue();
			setCash(compCash[i], c);
		}
		else if (c instanceof Bank)
		{
			setCash(bankCash, c);
		}
		else
		{
			System.out.println("!!! unknown cash holder: " + c);
		}
	}

	private void setCash(Field f, CashHolder c)
	{
		f.setText(Bank.format(c.getCash()));
	}

	public void updatePlayerPrivates(int playerIndex)
	{
		setPrivates(playerPrivates[playerIndex],
				players[playerIndex].getPortfolio(),
				true);
	}

	public void updateCompanyPrivates(int compIndex)
	{
		setPrivates(compPrivates[compIndex],
				companies[compIndex].getPortfolio(),
				false);
	}

	public void updateRevenue(int compIndex)
	{
		compRevenue[compIndex].setText(Bank.format(companies[compIndex].getLastRevenue()));
	}

	private void setPrivates(Field f, Portfolio p, boolean breakLine)
	{
		StringBuffer buf = new StringBuffer("<html>");
		Iterator it = p.getPrivateCompanies().iterator();
		PrivateCompanyI priv;
		while (it.hasNext())
		{
			priv = (PrivateCompanyI) it.next();
			if (buf.length() > 6)
				buf.append(breakLine ? "<br>" : "&nbsp;");
			buf.append(priv.getName());
		}
		if (buf.length() > 6)
		{
			buf.append("</html>");
			f.setText(buf.toString());
		}

	}

	public void updateTrains(Portfolio p)
	{
		if (p == Bank.getIpo())
		{
			newTrains.setText(TrainManager.get().makeAbbreviatedList(p));
		}
		else if (p == Bank.getPool())
		{
			poolTrains.setText(TrainManager.get().makeFullList(p));
		}
		else if (p == Bank.getUnavailable())
		{
			futureTrains.setText(TrainManager.get().makeAbbreviatedList(p));
		}
		else if (p.getOwner() instanceof PublicCompanyI)
		{
			compTrains[((PublicCompanyI) p.getOwner()).getPublicNumber()].setText(TrainManager.get()
					.makeFullList(p));
		}
		else
		{
			System.out.println("!!! unknown portfolio owner:"
					+ p.getOwner().getName());
		}
	}

	public void updateTrains()
	{
		for (int i = 0; i < nc; i++)
		{
			updateTrains(companies[i].getPortfolio());
		}
		newTrains.setText(TrainManager.get().makeAbbreviatedList(Bank.getIpo()));
		poolTrains.setText(TrainManager.get().makeFullList(Bank.getPool()));
		futureTrains.setText(TrainManager.get()
				.makeAbbreviatedList(Bank.getUnavailable()));
	}

	public void updatePlayerCash()
	{
		for (int i = 0; i < np; i++)
		{
			p = players[i];
			playerCash[i].setText(Bank.format(p.getCash()));
			playerWorth[i].setText(Bank.format(p.getWorth()));
		}

	}
}
