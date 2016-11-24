package com.fourcasters.forec.reconciler.server.http;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.query.history.HistoryDAO;
import com.fourcasters.forec.reconciler.server.http.HttpParser;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class HistoryServletTest {

	@Mock HttpParser parser;
	private static SocketChannel channel;
	private static HistoryDAO dao;
	private static String oldProp;
	private static Socket socket;
	private static ServerSocket server;
	private static Thread t;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		oldProp = System.getProperty("ENV");
		System.setProperty("ENV", "test");
		dao = new HistoryDAO();
		dao.dbhash("eurusd", "yyyy.mm.dd,HH:MM,o,h,l,c,v");
		server = new ServerSocket(54345);
		CountDownLatch latch = new CountDownLatch(1);
		t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(true) {
						latch.countDown();
						server.accept();
					}
				} catch (IOException e) {
				}
			}
		});
		t.start();
		latch.await(10, TimeUnit.SECONDS);
		channel = SocketChannel.open();
		socket = channel.socket();
		socket.connect(new InetSocketAddress("localhost", 54345));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (oldProp != null) {
			System.setProperty("ENV", oldProp);
		}
		else {
			System.clearProperty("ENV");
		}
		channel.close();
		socket.close();
		server.close();
		t.interrupt();
	}

	@Before
	public void setUp() throws Exception {
		when(parser.getParam("cross")).thenReturn("eurusd");
		when(parser.getParam("from")).thenReturn("201512231321");
		when(parser.getParam("to")).thenReturn("201512231331");
		when(parser.getMethod()).thenReturn("GET");
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException, ParseException {
		long result = new HistoryServlet(parser, dao).respond(channel);
		String pattern = "yyyy.MM.dd,HH:mm";
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		Date d = sdf.parse("2015.12.23,13:21");
		long start = dao.offset("eurusd", d, false);
		d = sdf.parse("2015.12.23,13:31");
		long stop = dao.offset("eurusd", d, true);
		assertEquals(stop-start, result);
	}

}
