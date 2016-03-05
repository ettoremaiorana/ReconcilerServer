package com.fourcasters.forec.reconciler.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class TestRemoteConnection {

	public static void main(String[] args) throws UnknownHostException, IOException {
		Socket s = new Socket("52.88.34.166", 51234);
		s.getOutputStream().write("CiaoMammaGuardaComeMiDiverto".getBytes());
		s.getOutputStream().flush();
		s.close();
	}
	
}
