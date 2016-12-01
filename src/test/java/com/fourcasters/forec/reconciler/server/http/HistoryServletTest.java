package com.fourcasters.forec.reconciler.server.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.query.history.HistoryDAO;

@RunWith(MockitoJUnitRunner.class)
public class HistoryServletTest {

	@Mock HttpParser parser;
	private static SocketChannel channel;
	private static HistoryDAO dao;
	private static String oldProp;
	private static Socket socket;
	private static ServerSocketChannel server;
	private static Thread t;
	private static Socket client;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		oldProp = System.getProperty("ENV");
		System.setProperty("ENV", "test");
		dao = new HistoryDAO();
		dao.dbhash("eurusd", "yyyy.mm.dd,HH:MM,o,h,l,c,v");
		server = ServerSocketChannel.open();
		server.configureBlocking(true);
		server.socket().bind(new InetSocketAddress("localhost", 54345));
		CountDownLatch latch = new CountDownLatch(1);
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					while(true) {
						latch.countDown();
						client = server.accept().socket();
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

	@Test
	public void testJsonFormat() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		when(parser.getParam("format")).thenReturn("json");
		final byte[] buff = new byte[1024*1024];
		final ByteBuffer bb = ByteBuffer.allocate(1024*1024);
		Thread t = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					int totRead = 0;
					while (totRead < 100) {
						bb.limit(bb.capacity());
						totRead += channel.read(bb); //body
					}
					latch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, "Read-me");
		t.start();
		new HistoryServlet(parser, dao).respond(client.getChannel());
		latch.await(10000, TimeUnit.MILLISECONDS);
		bb.flip();
		bb.get(buff,0,bb.limit());
		String resp = new String(buff);
		assertTrue(resp.contains("{\"data\":"));
	}
}
