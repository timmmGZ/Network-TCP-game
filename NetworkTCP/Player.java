import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Player extends Socket {
	PrintWriter out = null;
	BufferedReader in = null;
	static BufferedReader send = new BufferedReader(new InputStreamReader(System.in));
	public static String $0_255Regex = "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])",
			portRegex = "([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])",
			IPPortRegex = "(" + $0_255Regex + "\\.){3}" + $0_255Regex + ":" + portRegex,
			forSettingEnemySelectedNumber = "\\[.*" + IPPortRegex + "\\]: (\\d*\\(encrypted\\)\\d*)$";
	Pattern IPPortPattern = Pattern.compile(IPPortRegex);

	public Player(String ip, int port) throws UnknownHostException, IOException {
		super(ip, port);System.out.println(this.getLocalPort());
	}

	public static void main(String[] args) throws Exception {
		String serverIP;
		System.out.println("Please enter the Server IP£º");
		serverIP = send.readLine();
		Player client = new Player(serverIP, 9999);
		client.start();
		client.close();
	}

	public void start() throws IOException {
		try {
			out = new PrintWriter(new OutputStreamWriter(getOutputStream(), "UTF-8"), true);
			in = new BufferedReader(new InputStreamReader(getInputStream(), "UTF-8"));
			setName();
			setIntroducingAgent();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(new ServerListener()).start();
		String msg;
		while ((msg = send.readLine()) != null) {
			if (msg.equals("COMMAND")) {
				System.out.println("                                   *****************************\n"
						+ "                                   *Welcome to the Command List*\n"
						+ "                                   *****************************\n"
						+ "[SHOWIA]--------------show your introducing agent.\n"
						+ "[@name--message]-----send private message to a single agent.\n"
						+ "[JOIN]---------------when you send JOIN to your introducing agent, you will join the game which your introducing agent is in, if not to your introducing agent, then you just normally send message \"JOIN\"."
						+ "[SHOWHISTORY]--------show all deul results history.\n"
						+ "[SHOWALLPLAYER]------show all the agents who join the game as players.\n"
						+ "[SHOWALLAGENT]------show all the agents.\n"
						+ "[SHOWENEMY]----------show all detail about enemy section.\n"
						+ "[SHOWHISTORY]--------show your duel history.\n"
						+ "[SHOWINDUEL]---------show which players are currently in battel, \"Y\" means in, \"N\" means not in.\n"
						+ "[$number]------------select number for duel, it works only when you are in duel"
						+ "[QUIT]---------------quit the game but keep being as agent in the chatting room.\n");
			} else
				out.println(msg);
		}
		out.close();
		in.close();
		send.close();
		close();
	}

	private void setName() throws IOException {
		String name;
		while (true) {
			System.out.println("Please create your nickname: ");
			name = send.readLine();
			if (name.trim().equals("")) {
				System.out.println("cant be empty!");
			} else {
				out.println(name);
				break;
			}
		}
	}

	private void setIntroducingAgent() throws IOException {
		String msg = in.readLine();
		if (msg.equals("NOTFIRST")) {
			String IPPort;

			Matcher m;
			while (true) {
				System.out
						.println("Please enter your introducing player's IP and port number, e.g.[127.0.0.1:56789]: ");
				IPPort = send.readLine();
				m = IPPortPattern.matcher(IPPort);
				if (IPPort.trim().equals("")) {
					System.out.println("cant be empty!");
				} else if (IPPort.equals(getInetAddress().getHostAddress() + ":" + getLocalPort())) {
					System.out.println(
							"Since you are not the first player in the unique game, you can not be your introducing player!");
				} else if (!m.matches()) {
					System.out.println("wrong ip format!");
				} else {
					out.println(IPPort);
					msg = in.readLine();
					if (msg.equals("N")) {
						System.out.println("this IP and port doesn't exist!");
					} else {
						System.out.println("Introducing agent is seted");
						break;
					}

				}
			}
		} else
			System.out.println(msg);
	}

	class ServerListener implements Runnable {

		BufferedReader inn = null;

		@Override
		public void run() {
			try {
				inn = new BufferedReader(new InputStreamReader(getInputStream(), "UTF-8"));
				String msg;
				while ((msg = inn.readLine()) != null) {
					if (msg.matches("SETENEMY.*")) {
						out.println(msg);
					} else if (msg.matches(forSettingEnemySelectedNumber)) {
						out.println(msg);
						System.out.println(msg.substring(0, msg.length() - 1));
					} else if (msg.matches("SETHISTORYMe\\(.*")) {
						out.println(msg);
					} else if (msg.contains("###DUEL NOTIFICATION###") && msg.contains("quit the game")) {
						out.println(msg);
						System.out.println(msg);

					} else
						System.out.println(msg);
				}
			} catch (Exception e) {
			} finally {
				if (inn != null)
					try {
						inn.close();
					} catch (IOException e) {
					}
			}
		}
	}

}
