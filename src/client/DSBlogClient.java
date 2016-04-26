package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

public class DSBlogClient {

	private static InetAddress desAddress;
	private static int CLIENTS_PORT = 8887;

	public static void main(String[] args) throws UnknownHostException {
		if (args.length != 1) {
			System.out.println("args[0] should be the IP address of the datacenter!");
			DSBlogClient.desAddress = InetAddress.getByName("127.0.0.1");
			// return;
		}

		// System.out.println(args[0]);
		DSBlogClient.desAddress = InetAddress.getByName(args[0]);
		DSBlogClient client = new DSBlogClient();

		Scanner scanner = new Scanner(System.in);

		while (true) {
			String s = scanner.nextLine().trim();
			boolean isPost = false, isLookup = false, isSync = false;
			isPost = s.matches("((POST)|(post))\\s+(\\S|\\s)+");
			if (!isPost) {
				isLookup = s.matches("((LOOKUP)|(lookup))");
				if (!isLookup) {
					isSync = s.matches("((SYNC)|(sync))\\s+\\S+");
				}
			}

			StringBuilder request = new StringBuilder();
			String req = null;
			if (isPost) {
				String[] ss = s.split("\\s+", 2);
				request.append("p ");
				request.append(ss[1]);
				req = request.toString();
				// System.out.println("post req: \n" + req);
			} else if (isLookup) {
				req = "l";
				// System.out.println("lookup req: \n" + req);
			} else if (isSync) {
				String[] ss = s.split("\\s+", 2);
				request.append("s ");
				request.append(ss[1]);
				req = request.toString();
				// System.out.println("sync req: \n" + req);
			} else {
				System.out.println("Invalid request!");
				continue;
			}
			Thread t = new Thread(client.new SendRequestThread(req));
			t.start();
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public class SendRequestThread implements Runnable {
		String req;
		Socket socket;

		public SendRequestThread(String req) {
			this.req = req;
		}

		@Override
		public void run() {
			try {
				socket = new Socket(DSBlogClient.desAddress, DSBlogClient.CLIENTS_PORT);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			PrintWriter pw = null;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			pw.println(req);
			pw.flush();

			if (req.compareTo("l") == 0) {
				List<String> messages = null;
				try {
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					messages = (List<String>) ois.readObject();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
				for (String message : messages) {
					System.out.println(message);
				}
			}

			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
