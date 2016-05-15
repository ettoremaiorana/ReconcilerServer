package com.fourcasters.forec.reconciler.server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SimpleHttpClient {

	private static final String REQUEST0 = "GET /history/csv HTTP/1.1\r\n";
	private static final String REQUEST1 = "Host: localhost:9090\r\n\r\n";
	public static void main(String[] args) throws IOException, InterruptedException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("localhost", 9090));
		PrintWriter writer = new PrintWriter(new BufferedOutputStream(s.getOutputStream()));
		writer.write(REQUEST0);
		writer.flush();
		Thread.sleep(4000L);
		writer.write(REQUEST1);
		writer.flush();
		BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String line;
		while ((line = reader.readLine()) != null ) {
			System.out.println(line);
		}
		s.close();
	}
}
