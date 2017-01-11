package com.fourcasters.forec.reconciler.query.trades;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TradeRecordBuilderTest {

	private static String oldProp;
	private TradesDAO dao;

	@BeforeClass
	public static void setupAll() throws IOException {
		oldProp = System.getProperty("ENV");
		System.setProperty("ENV", "test");
	}

	@AfterClass
	public static void afterAll() {
		if (oldProp != null) {
			System.setProperty("ENV", oldProp);
		}
		else {
			System.clearProperty("ENV");
		}
	}

	@Before
	public void setUp() {
		dao = new TradesDAO();
	}

	@Test
	public void testCanIndexProperly() throws IOException {
		boolean didNotExist = dao.createIndex("trades");
		assertTrue(didNotExist);

		int size = dao.indexSize("trades");
		assertEquals(466, size);
	}

	@Test
	public void testCanIndexEmptyFile() throws IOException {
		boolean didNotExist = dao.createIndex("open");
		assertTrue(didNotExist);

		int size = dao.indexSize("open");
		assertEquals(0, size);
	}

	@Test
	public void testOffsetExistingTrade() throws IOException {
		dao.createIndex("trades");
		long offset = dao.tradeOffset("trades", 123559358L);
		assertEquals(49112, offset);
	}

	@Test
	public void testOffsetNonExistingTrade() throws IOException {
		dao.createIndex("trades");
		long offset = dao.tradeOffset("trades", 123559359L);
		assertEquals(-1, offset);
	}


}
