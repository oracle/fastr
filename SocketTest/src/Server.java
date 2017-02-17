import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

public class Server {

	public static void main(String[] args) {

		try (ServerSocket serverSocket = new ServerSocket(5678)) {
			System.out.println("server started");
			while (true) {
				System.out.println("waiting for client");
				Socket client = serverSocket.accept();
				System.out.println("Client connected");
				getLine();
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
				pw.println("hello");
				pw.flush();
				client.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void getLine() throws IOException {
		new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset())).readLine();
	}

}
