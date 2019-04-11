
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class HTTPMonitor extends JFrame {
	private static final long serialVersionUID = 123L;
	JTextArea jta = new JTextArea(50, 50);
	private static Set<String> results = new HashSet<>();

	public HTTPMonitor() {
		super("HTTP Monitor");
		setResizable(true);
		JScrollPane scroll = new JScrollPane(jta, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jta.setText("");
		add(scroll);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	public void refresh(String s) {
		if (s != null)
			results.add(s);
		jta.setText("Total results: " + results.size() + "\n");
		for (String r : results)
			jta.append(r + "\n");
	}

	public void remove(String s) {
		Iterator<String> r = results.iterator();
		while (r.hasNext())
			if (r.next().contains(s))
				r.remove();
		refresh(null);
	}

}
