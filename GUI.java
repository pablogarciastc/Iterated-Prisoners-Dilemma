import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import jade.core.AID;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import agents.*;

public final class GUI extends JFrame implements ActionListener {
	JLabel leftPanelRoundsLabel;
	JLabel leftPanelExtraInformation;
	JLabel PlayersLabel = new JLabel();
	JLabel GamesPlayedLabel = new JLabel();
	JLabel RoundsLabel = new JLabel();
	JList<String> list;
	JList<DefaultTableModel> lista = new JList<>();
	JList<String> listaString = new JList<>();
	JTable TablaUtil = null;
	private MainAgent mainAgent;
	private JPanel rightPanel;
	private JTextArea rightPanelLoggingTextArea;
	private LoggingOutputStream loggingOutputStream;
	int numberOfPlayers = 0;
	int numberOfGames = 0;
	Object[][] tablaStats = { { 0, 0, 3, 0 } };
	public HashMap<AID, PlayerStats> statsplayers = new HashMap<>();

	/**
	 * Initialize the interface
	 */
	public GUI() {
		initUI();
	}

	/**
	 * Instantiates a mainAgent object and initialize the interface
	 * 
	 * @param agent
	 */
	public GUI(MainAgent agent) {
		mainAgent = agent;
		initUI();
		loggingOutputStream = new LoggingOutputStream(rightPanelLoggingTextArea);
	}

	/**
	 * Print messages through the GUI
	 * 
	 * @param s
	 */
	public void log(String s) {
		Runnable appendLine = () -> {
			rightPanelLoggingTextArea
					.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
			rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
		};
		SwingUtilities.invokeLater(appendLine);
	}

	public OutputStream getLoggingOutputStream() {
		return loggingOutputStream;
	}

	public void logLine(String s) {
		log(s + "\n");
	}

	/**
	 * Gets all the players that are going to play and updates the GUI with them
	 * 
	 * @param players
	 */
	public void setPlayersUI(String[] players) {
		DefaultListModel<String> listModel = new DefaultListModel<>();
		int m = 0;
		// Adds the players to the model
		for (String s : players) {
			if (s != null) {
				if (s.contains("@")) {
					String[] partes = s.split("@");
					listModel.addElement(partes[0]);
				} else {
					listModel.addElement(s);
				}
			} else
				m++;

			numberOfPlayers = listModel.size(); // how many agents are playing

			PlayersLabel.setText("Number of players : " + numberOfPlayers);
		}
		list.setModel(listModel); // updates the list with the players
		if (m == players.length) {
			list.setModel(new DefaultListModel<>());

			numberOfPlayers = listModel.size();

			PlayersLabel.setText("Number of players : " + numberOfPlayers);
		}

	}

	/**
	 * This method gets the updated stats map and updates the table
	 * 
	 * @param HashMappu
	 */
	public void refreshPlayerStats(HashMap<AID, PlayerStats> HashMappu) {

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Name                   Cooperate                     Defeat                   Points"); // the
																														// parameters
		Collection<PlayerStats> playerstats = HashMappu.values();
		List<PlayerStats> list = new ArrayList<>(playerstats);
		Collections.sort(list, new MyComparator()); //this is used to sort the players by his puntuation.

		for (Iterator<PlayerStats> it = list.iterator(); it.hasNext();) {
			PlayerStats stdn = (PlayerStats) it.next();
			String[] nombre = stdn.name.split("@");
			String espacios = "                            ";
			String subespacios = espacios.substring(0, espacios.length() - nombre[0].length() * 2);
			listModel.addElement(nombre[0] + subespacios + stdn.coopPCTGE + "%" + "                                "
					+ stdn.defeatPCTGE + "%" + "                      " + stdn.points); // gets and sets the stats
																						// of each player in the
																							// stats table
		}

		listaString.setModel(listModel); // updated table

	}

	/**
	 * Initializes the window and all the panels that made up the GUI
	 */
	public void initUI() {
		setTitle("GUI");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 400));
		setPreferredSize(new Dimension(1000, 600));
		// setJMenuBar(createMainMenuBar());
		setContentPane(createMainContentPane());
		pack();
		setVisible(true);
	}

	/**
	 * This method initializes each panel
	 * 
	 * @return
	 */
	private Container createMainContentPane() {
		JPanel pane = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridy = 0;
		gc.weightx = 0.5;
		gc.weighty = 0.5;

		// LEFT PANEL
		gc.gridx = 0;
		gc.weightx = 1;
		gc.weighty = 0;
		pane.add(createLeftPanel(), gc);

		// CENTRAL PANEL
		gc.gridx = 1;
		gc.weightx = 8;
		pane.add(createCentralPanel(), gc);

		// RIGHT PANEL
		gc.gridx = 2;
		gc.weightx = 8;
		pane.add(createRightPanel(), gc);
		return pane;
	}

	/**
	 * Creates the left panel
	 * 
	 * @return
	 */
	private JPanel createLeftPanel() {
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridBagLayout());
		leftPanel.setBackground(Color.white);
		GridBagConstraints gc = new GridBagConstraints();
		int gcc = 0;

		JButton leftPanelNewButton = new JButton("New Game");
		leftPanelNewButton.addActionListener(actionEvent -> mainAgent.resetStats());
		leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame()); // if the button newgame bot newgame
																					// and resetstats methods are called
		leftPanelNewButton
				.addActionListener(actionEvent -> GamesPlayedLabel.setText("Games played: " + numberOfGames++)); // for
																													// each
																													// game,
																													// the
																													// number
																													// of
																													// games
																													// is
																													// increased

		JButton leftPanelStopButton = new JButton("Stop Game");// I assume that this and the following buttons are quite
																// obvious
		leftPanelStopButton.addActionListener(actionEvent -> {
			try {
				mainAgent.StopGame();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
		JButton leftPanelContinueButton = new JButton("Continue Game");
		leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.ContinueGame());

		JButton leftPanelStatsButton = new JButton("Reset Players");
		leftPanelStatsButton.addActionListener(actionEvent -> mainAgent.resetStats());

		JButton leftPanelResetButton = new JButton("Return Players");
		leftPanelResetButton.addActionListener(actionEvent -> mainAgent.returnPlayers());
		leftPanelResetButton.addActionListener(actionEvent -> returnPlayersGUI());

		JTextField textRemove = new JTextField(20);

		JButton leftPanelRemoveButton = new JButton("Remove Player");

		leftPanelRemoveButton.addActionListener(actionEvent -> mainAgent.Remove(textRemove.getText())); // I passed the
																										// number of the
																										// agent to
																										// removed as
																										// arg

		leftPanelRemoveButton.addActionListener(this);

		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;
		gc.weightx = 0;
		gc.weighty = 0;

		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelNewButton, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelStopButton, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelContinueButton, gc);
		gc.gridy = gcc;
		gcc++;
		JTextField text = new JTextField(20);
		JButton submitButton = new JButton("Submit Number of Rounds"); // this button is to update the number of rounds
																		// for each game
		submitButton.addActionListener(actionEvent -> mainAgent.Rounds(Integer.parseInt(text.getText())));
		submitButton.addActionListener(actionEvent -> RoundsLabel.setText("Number of Rounds: " + text.getText()));
		gc.gridy = gcc;
		gcc++;

		gc.gridy = gcc;
		gcc++;
		leftPanel.add(text, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(submitButton, gc);
		gc.weighty = 0;
		leftPanelRoundsLabel = new JLabel("Edit");
		gc.weighty = 0;
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelRoundsLabel, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelResetButton, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelStatsButton, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(textRemove, gc);
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(leftPanelRemoveButton, gc);
		gc.weighty = 0;

		// all this is to add buttons and info to the left panel

		JButton about = new JButton("About");

		about.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				JOptionPane.showMessageDialog(leftPanel, "Intelligent Systems Programming 20/21 \n\r\n"
						+ "Telecommunications Engineering UVigo \n \t\tPablo García Santaclara");
			}
		});
		gc.gridy = gcc;
		gcc++;

		leftPanel.add(about, gc);

		JCheckBox window = new JCheckBox("Verbose");

		window.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				JCheckBox window = (JCheckBox) event.getSource();
				if (window.isSelected()) {
					rightPanel.setVisible(true);
				} else {
					rightPanel.setVisible(false);
				}

			}
		});
		gc.gridy = gcc;
		gcc++;
		leftPanel.add(window, gc);

		window.setSelected(true);

		leftPanelRoundsLabel = new JLabel("Verbose");

		gc.weighty = 100;

		return leftPanel;
	}

	/**
	 * This method is to reset the dropped players
	 */
	public void returnPlayersGUI() {
		numberOfPlayers = 0;
		statsplayers = new HashMap<>();
	}

	/**
	 * Creates the central panel
	 * 
	 * @return
	 */
	private JPanel createCentralPanel() {
		JPanel centralPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;

		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.gridx = 0;

		gc.gridy = 0;
		gc.weighty = 1;
		centralPanel.add(createCentralTopSubpanel(), gc);
		gc.gridy = 1;
		gc.weighty = 4;
		centralPanel.add(createCentralBottomSubpanel(), gc);

		return centralPanel;
	}

	/**
	 * Creates the superior central subpanel
	 * 
	 * @return
	 */
	private JPanel createCentralTopSubpanel() {
		JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Empty");
		list = new JList<>(listModel); // list with players
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setVisibleRowCount(5);
		JScrollPane listScrollPane = new JScrollPane(list);

		PlayersLabel = new JLabel();

		PlayersLabel.setText("Number of players: " + numberOfPlayers); // shows the number of players

		String Rounds = "2";
		RoundsLabel.setText("Number of Rounds: " + Rounds); // shows the number of rounds

		leftPanelRoundsLabel = new JLabel("Round 0 / null");
		GamesPlayedLabel.setText("Games played: " + numberOfGames++); // shows the games already played

		PlayersLabel = new JLabel();
		PlayersLabel.setText("Number of players: " + numberOfPlayers); // shows the number of players currently playing

		GridBagConstraints gc = new GridBagConstraints();
		gc.weightx = 0.5;
		gc.weighty = 0.5;
		gc.anchor = GridBagConstraints.CENTER;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridheight = 666;
		gc.fill = GridBagConstraints.BOTH;
		centralTopSubpanel.add(listScrollPane, gc);
		gc.gridx = 1;
		gc.gridheight = 1;
		gc.fill = GridBagConstraints.NONE;
		gc.gridy = 2;

		centralTopSubpanel.add(RoundsLabel, gc);
		gc.gridy++;
		centralTopSubpanel.add(PlayersLabel, gc);
		gc.gridy++;
		centralTopSubpanel.add(GamesPlayedLabel, gc);
		// updates the info
		return centralTopSubpanel;
	}

	/**
	 * Creates the inferior central subpanel
	 * 
	 * @return
	 */
	private JPanel createCentralBottomSubpanel() {
		JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());

		Object[] nullPointerWorkAround = { "*", "*", "*" };

		Object[][] data = { { "P1/P2", "C", "D" }, { "C", "3,3", "0,5" }, { "D", "5,0", "1,1" }, }; // for the static
																									// matrix of payoff

		JLabel payoffLabel = new JLabel("Payoff matrix");
		JTable payoffTable = new JTable(data, nullPointerWorkAround);
		payoffTable.setTableHeader(null);
		payoffTable.setEnabled(false);

		JScrollPane player1ScrollPane = new JScrollPane(payoffTable); // construction of the static table

		DefaultListModel<String> listModel = new DefaultListModel<>();
		listModel.addElement("Empty");
		listaString = new JList<>(listModel);
		listaString.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listaString.setSelectedIndex(0);
		listaString.setVisibleRowCount(5);
		JScrollPane listScrollPane = new JScrollPane(listaString);

		Object[] names = { "Players", "D", "C", "Points" };

		JTable playerStats = new JTable(tablaStats, names); // updates the stats table

		GridBagConstraints gc = new GridBagConstraints();

		JScrollPane statScrollPane = new JScrollPane(playerStats, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		gc.weightx = 0.5;
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;

		gc.gridx = 0;
		gc.gridy = 0;
		gc.weighty = 0.5;
		centralBottomSubpanel.add(payoffLabel, gc);
		gc.gridy = 1;
		gc.gridx = 0;
		gc.weighty = 2;
		centralBottomSubpanel.add(player1ScrollPane, gc);
		gc.gridy = 2;
		centralBottomSubpanel.add(listScrollPane, gc);

		return centralBottomSubpanel;
	}

	/**
	 * Creates the right subpanel which contains the logs
	 * 
	 * @return
	 */
	private JPanel createRightPanel() {
		rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weighty = 1d;
		c.weightx = 1d;

		rightPanelLoggingTextArea = new JTextArea("");
		rightPanelLoggingTextArea.setEditable(false);
		JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
		rightPanel.add(jScrollPane, c);
		return rightPanel;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton button = (JButton) e.getSource();
			logLine("Button " + button.getText());
		} else if (e.getSource() instanceof JMenuItem) {
			JMenuItem menuItem = (JMenuItem) e.getSource();
			logLine("Menu " + menuItem.getText());
		}

	}

	public class LoggingOutputStream extends OutputStream {
		private JTextArea textArea;

		public LoggingOutputStream(JTextArea jTextArea) {
			textArea = jTextArea;
		}

		@Override
		public void write(int i) throws IOException {
			textArea.append(String.valueOf((char) i));
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}

	}

	/**
	 * this class is used to sort the players by his points
	 * 
	 *
	 */
	class MyComparator implements Comparator<PlayerStats> { 

		@Override
		public int compare(PlayerStats s1, PlayerStats s2) {


			return Double.compare(s2.getPoints(), s1.getPoints());
		}
	}

}
