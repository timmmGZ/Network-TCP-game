import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class MyHTTPServer {
	private static Set<String> results = new HashSet<>();
	HttpServer httpServer;

	public MyHTTPServer() throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
		httpServer.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange httpExchange) throws IOException {
				StringBuilder sb = new StringBuilder();
				sb.append("Total results: " + results.size() + "<br/>");
				for (String s : results)
					sb.append(s + "<br/>");
				byte[] respContents = sb.toString().getBytes("UTF-8");
				httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
				httpExchange.sendResponseHeaders(200, respContents.length);
				httpExchange.getResponseBody().write(respContents);
 				httpExchange.close();
			}
		});
		httpServer.start();
	}

	public void add(String s) {
		results.add(s);
	}

	public void remove(String s) {
		Iterator<String> r = results.iterator();
		while (r.hasNext())
			if (r.next().contains(s))
				r.remove();
	}
}
