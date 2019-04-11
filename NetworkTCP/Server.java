import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class Server extends ServerSocket {
	public static final String SYSTEMNOTIFICATION = "###SYSTEM NOTIFICATION### ";
	public static final String DUELNOTIFICATION = "###DUEL NOTIFICATION### ";
	public static final String HTTPMONITOR = "###HTTP MONITOR### ";
	private Map<String, PrintWriter> agentList = new HashMap<String, PrintWriter>();
	private Map<String, PrintWriter> playerList = new HashMap<String, PrintWriter>();
	private Map<String, String> inDuelList = new HashMap<String, String>();
	public static final Random RANDOM = new Random();
	private MyHTTPServer http = new MyHTTPServer();

	public Server() throws IOException {
		super(9999);
		System.out.println(InetAddress.getLocalHost().getHostAddress().toString());
	}

	private void addToAgentList(String key, PrintWriter value) {
		synchronized (this) {
			agentList.put(key, value);
		}
	}

	private void addToPlayerList(String key, PrintWriter value) {
		synchronized (this) {
			playerList.put(key, value);
			inDuelList.put(key, "N");
		}
	}

	private synchronized String inCasePlayersMatchSamePersonAtSameTime(ListenrClient p) {
		p.refreshEnemyList();// in case new agents join the game
		String tmpCurrentEnemy = p.enemyList.toArray(new String[0])[RANDOM.nextInt(p.enemyList.size())];
		if (inDuelList.get(tmpCurrentEnemy).equals("N")) {
			inDuelList.replace(tmpCurrentEnemy, "Y");
			inDuelList.replace(p.toString(), "Y");
			return tmpCurrentEnemy;
		}
		return null;
	}

	private synchronized void remove(String key) {
		if (playerList.containsKey(key))
			quitGame(key);
		agentList.remove(key);
		System.out.println(SYSTEMNOTIFICATION + key + " disconnects.\nCurrent online agents: " + agentList.size());
	}

	private synchronized void quitGame(String key) {
		playerList.remove(key);
		inDuelList.remove(key);
		http.remove(key);
		sendToAllPlayers(DUELNOTIFICATION + key + " quit the game.");
	}

	private synchronized void sendToAllAgents(String message) {
		System.out.println(message);
		for (PrintWriter out : agentList.values()) {
			out.println(message);
		}
	}

	private synchronized void sendToAllPlayers(String message) {
		System.out.println(message);
		for (PrintWriter out : playerList.values()) {
			out.println(message);
		}
	}

	private synchronized void sendToSomeone(String name, String message) {
		PrintWriter out = agentList.get(name);
		if (out != null)
			out.println(message);
	}

	public void start() {
		try {
			while (true)
				new Thread(new ListenrClient(accept())).start();
		} catch (Exception e) {
		}
	}
	@SuppressWarnings("unused")

	class ListenrClient extends Socket implements Runnable {
		private BufferedReader in;
		private PrintWriter out;
		private Socket socket;
		private String IP, name, iIP, iFullName, currentEnemy;
		private Integer selectedNumber, enemySelectedNumber, encryptedMultiplicationFactor;
		private int port, iPort, startFrom;
		private boolean inGame = false;
		private Set<String> myHistory = new HashSet<>(), enemyList = new HashSet<>(), playedEnemyList = new HashSet<>();

		public ListenrClient(Socket s) {
			socket = s;
			port = s.getPort();
			IP = s.getInetAddress().getHostAddress();
		}

		@Override
		public void run() {
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
				name = in.readLine();
				System.out.println(SYSTEMNOTIFICATION + this + " connects.");
				addToAgentList(this.toString(), out);
				setIntroducingAgent();
				sendToAllAgents(SYSTEMNOTIFICATION + this + " is online");
				String msg = null;
				while ((msg = in.readLine()) != null) {
					if (msg.startsWith("@")) {
						int index = msg.indexOf("--");
						if (index >= 0) {
							String theName = msg.substring(1, index);
							String info = msg.substring(index + 2, msg.length());
							if (!agentList.containsKey(theName)) {
								out.println("Agent doesn't exist");
								continue;
							}
							if (theName.equals(iFullName) && info.equals("JOIN")) {
								joinGame();
							}
							info = this + ": " + info;
							sendToSomeone(theName, info);
							continue;
						}
					} else if (msg.startsWith(DUELNOTIFICATION + "Your current enemy is: ")) {
						msg = msg.substring(43);
						continue;
					} else if (msg.matches("SETENEMY.*")) {
						passivelyBeingSelectedAsEnemy(msg);
						continue;
					} else if (msg.matches("SETHISTORYMe\\(.*")) {
						passivelyBeingSetHistory(msg);
						continue;
					} else if (msg.matches("\\$-*\\d++")) {
						sendSelectedNumberToEnemy(msg);
						continue;
					} else if (msg.matches(Player.forSettingEnemySelectedNumber)) {
						encryptedMultiplicationFactor = Integer.parseInt(msg.substring(msg.length() - 1, msg.length()));
						enemySelectedNumber = (Integer.parseInt(msg.substring(msg.indexOf("]") + 3, msg.length() - 12)))
								/ encryptedMultiplicationFactor;
						continue;
					} else if (msg.contains(DUELNOTIFICATION) && msg.contains("quit the game")) {
						removeQuitedPlayerFromAllPlayersMemories(msg);
						continue;
					} else
						switch (msg) {
						case "SHOWIA":
							out.println("Your Introducing agent is: " + iFullName);
							continue;
						case "SHOWALLPLAYER":
							commandSHOWALLPLAYER();
							continue;
						case "SHOWALLAGENT":
							out.println(agentList.keySet());
							continue;
						case "SHOWENEMY":
							commandENEMY();
							continue;
						case "SHOWHISTORY":
							continue;
						case "SHOWINDUEL":
							commandInDuel();
							continue;
						case "QUIT":
							commandQUIT();
							continue;
						default:
							sendToAllAgents(this + ": " + msg);
						}
				}
			} catch (Exception e) {
			} finally {
				remove(this.toString());
				sendToAllAgents(SYSTEMNOTIFICATION + this + " is offline.");
				try {
					in.close();
					out.close();
					socket.close();
				} catch (IOException e) {
				}
			}
		}

		private void setIntroducingAgent() throws IOException {
			if (agentList.size() == 1) {
				iIP = IP;
				iPort = port;
				iFullName = this.toString();
				out.println("You are the first agent, your introdcuing agent is yourself.");
				
			} else {
				out.println("NOTFIRST");
				while (true) {
					String IPPort;
					if (checkIPPortExistence(IPPort = in.readLine())) {
						out.println("Y");
						String s[] = IPPort.split(":");
						iIP = s[0];
						iPort = Integer.parseInt(s[1]);
						break;
					} else
						out.println("N");
				}
			}
		}

		private boolean checkIPPortExistence(String IPPort) {
			for (String name : agentList.keySet())
				if (name.matches(".*" + IPPort + "]$")) {
					iFullName = name;
					return true;
				}
			return false;
		}

		private void joinGame() {
			if (!inGame) {
				inGame = true;
				addToPlayerList(this.toString(), out);
				sendToAllPlayers(SYSTEMNOTIFICATION + this + " join the game.");
				out.println(
						"You join the game, now you have to play with all agents who join the game as players, random matching will start!");
				refreshEnemyList();
				commandENEMY();
				if (enemyList.size() > 0)
					startOneDuel();
			} else
				out.println(SYSTEMNOTIFICATION + "You had joined the game already, can not join again!");
		}

		private void startOneDuel() {
			new Thread(() -> {
				String tmpCurrentEnemy = null;
				while (tmpCurrentEnemy == null && inDuelList.get(this.toString()).equals("N")) 
					tmpCurrentEnemy = inCasePlayersMatchSamePersonAtSameTime(this);
				if (tmpCurrentEnemy != null)
					setMeAsItsCurrentEnemy(tmpCurrentEnemy);
			}).start();
		}

		public void setMeAsItsCurrentEnemy(String e) {
			sendToSomeone(currentEnemy = e, "SETENEMY" + (startFrom = RANDOM.nextInt(2) + 1 == 1 ? 2 : 1) + this);
			out.println(DUELNOTIFICATION + "Your current enemy is: " + currentEnemy + ", counting will be start from "
					+ (startFrom == 1 ? "you" : currentEnemy)
					+ ", now please select a natural and send to your matched player in a form like \"$--selectedNumber\", then it will automatically send to your current enemy");
			sendToSomeone(currentEnemy, DUELNOTIFICATION + "Your current enemy is: " + this
					+ ", counting will be start from " + (startFrom == 1 ? this : "you")
					+ ", now please select a natural and send to your matched player in a form like \"$--selectedNumber\", then it will automatically send to your current enemy");
		}

		public void passivelyBeingSelectedAsEnemy(String msg) {
			startFrom = Integer.parseInt(msg.substring(8, 9));
			currentEnemy = msg.substring(9, msg.length());
			refreshEnemyList();
		}

		private void startNextDuel() {
			refreshEnemyList();
			setNullsAfterDuel();
			inDuelList.replace(this.toString(), "N");
			if (enemyList.size() > 0)
				startOneDuel();
		}

		private void setNullsAfterDuel() {
			startFrom = 0;
			selectedNumber = null;
			enemySelectedNumber = null;
			currentEnemy = null;
			encryptedMultiplicationFactor = null;
		}

		private void sendSelectedNumberToEnemy(String msg) {
			if (inGame) {
				if (currentEnemy == null) {
					out.println(DUELNOTIFICATION + "you don't have any enemy now!");
				} else {
					if (selectedNumber == null) {
						if (msg.substring(1, 2).equals("-")) {
							out.println(DUELNOTIFICATION + "Please enter only natural number > 0!");
						} else if (Integer.parseInt(msg.substring(1, msg.length())) > 0) {
							setAndSendNumberAndEncryptedFactor(msg);
						} else
							out.println(DUELNOTIFICATION + "Please enter only natural number > 0!");
					} else
						out.println(
								DUELNOTIFICATION + "you have already selected your number, it is: " + selectedNumber);
					if (selectedNumber != null && enemySelectedNumber != null)
						checkWinner();
				}
			} else
				sendToAllAgents(this + ": " + msg);
		}

		private void setAndSendNumberAndEncryptedFactor(String msg) {
			if (encryptedMultiplicationFactor == null)
				encryptedMultiplicationFactor = RANDOM.nextInt(9) + 1;
			selectedNumber = Integer.parseInt(msg.substring(1, msg.length()));
			sendToSomeone(currentEnemy, this + ": " + selectedNumber * encryptedMultiplicationFactor + "(encrypted)"
					+ encryptedMultiplicationFactor);
		}

		private void checkWinner() {
			int tatol = selectedNumber + enemySelectedNumber;
			String winner = (startFrom == 1) ? (tatol % 2 == 0 ? currentEnemy : this.toString())
					: (tatol % 2 == 1 ? currentEnemy : this.toString());
			String result = "Me(" + selectedNumber + ") " + (winner.equals(this.toString()) ? "wins " : "loses ")
					+ currentEnemy + "(" + enemySelectedNumber + ") start from: " + (startFrom == 1 ? "me" : "enemy");
			String enemyResult = "Me(" + enemySelectedNumber + ") "
					+ (winner.equals(this.toString()) ? "loses " : "wins ") + this.toString() + "(" + selectedNumber
					+ ") start from: " + (startFrom == 1 ? "enemy" : "me");
			String forHTTPMonitorResult = this.toString() + "(" + selectedNumber + ") "
					+ (winner.equals(this.toString()) ? "wins " : "loses ") + currentEnemy + "(" + enemySelectedNumber
					+ ") start from: " + (startFrom == 1 ? this.toString() : currentEnemy);
			out.println(DUELNOTIFICATION + "Both players in current match have already chosen numbers.\n" + result
					+ " and encrypted multiplication factor was: " + encryptedMultiplicationFactor);
			sendToSomeone(currentEnemy,
					DUELNOTIFICATION + "Both players in current match have already chosen numbers.\n" + enemyResult
							+ " and encrypted multiplication factor was: " + encryptedMultiplicationFactor);
			http.add(forHTTPMonitorResult);
			System.out.println(HTTPMONITOR + forHTTPMonitorResult);
			myHistory.add(result);
			sendToSomeone(currentEnemy, "SETHISTORY" + enemyResult);
			playedEnemyList.add(currentEnemy);
			startNextDuel();
		}

		private void passivelyBeingSetHistory(String msg) {
			myHistory.add(msg.substring(10));
			playedEnemyList.add(currentEnemy);
			startNextDuel();
		}

		private void refreshEnemyList() {
			enemyList = new HashSet<>(playerList.keySet()).stream()
					.filter(e -> !playedEnemyList.contains(e) && !e.equals(this.toString()))
					.collect(Collectors.toSet());
		}

		private void removeQuitedPlayerFromAllPlayersMemories(String msg) {
			String name = msg.substring(24, msg.length() - 15);
			playedEnemyList.remove(name);
			myHistory = myHistory.stream().filter(e -> !e.contains(name)).collect(Collectors.toSet());
			if (currentEnemy != null && currentEnemy.equals(name)) {
				out.println(DUELNOTIFICATION + "your enemy disconnect accidentally");
				startNextDuel();
			}
		}

		private void commandQUIT() {
			if (inGame) {
				if (enemyList.size() == 0) {
					commandHISTORY();
					quitGame(this.toString());
					myHistory.clear();
					playedEnemyList.clear();
					inGame = false;
				} else
					out.println(DUELNOTIFICATION
							+ "You can not quit the game now, because you haven't played with all the players yet.");
			} else
				out.println(SYSTEMNOTIFICATION + "You are not in any game!");
		}

		private void commandHISTORY() {
			if (inGame)
				myHistory.forEach(e -> {
					out.println(e);
				});
			else
				out.println(SYSTEMNOTIFICATION + "You are not in any game!");
		}

		private void commandSHOWALLPLAYER() {
			out.println(inGame ? playerList.keySet() : SYSTEMNOTIFICATION + "You are not in any game!");
		}

		private void commandInDuel() {
			out.println(inGame ? inDuelList : SYSTEMNOTIFICATION + "You are not in any game!");
		}

		private void commandENEMY() {
			refreshEnemyList();
			out.println(inGame
					? "Current enemy: " + currentEnemy + "    its selected number: " + enemySelectedNumber + "  yours:"
							+ selectedNumber + "  counting start from: "
							+ (startFrom == 0 ? null : (startFrom == 1 ? "you." : "enemy.")) + "\nRemaining enemies: "
							+ enemyList + "\nEnemies already played with: " + playedEnemyList
					: SYSTEMNOTIFICATION + "You are not in any game!");
		}

		public String toString() {
			return "[" + name + " " + IP + ":" + port + "]";
		}

	}

	public static void main(String[] args) throws IOException {
		Server server = new Server();
		server.start();
		server.close();
	}
}
