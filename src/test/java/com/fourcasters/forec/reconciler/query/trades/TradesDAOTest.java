package com.fourcasters.forec.reconciler.query.trades;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

public class TradesDAOTest {

	private static String oldProp;
	private static TradesDAO dao;


	@BeforeClass
	public static void setupAll() throws IOException {
		oldProp = System.getProperty("ENV");
		System.setProperty("ENV", "test");
		dao = new TradesDAO();
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
	
	@Test
	public void testNewRecordBuilder() {
		assertEquals(TradeRecordBuilder.class, dao.getRecordBuilder().getClass());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testNewRecordBuilderWithPatternIsNotSupported() {
		dao.getRecordBuilder("any pattern");
	}

	@Test
	public void testGetRootPath() {
		assertEquals(Paths.get(ReconcilerConfig.TRADES_DATA_PATH), dao.getRootPath());
	}

	@Test
	public void testGetFilePath() {
		String pathToString = dao.getFilePath("Trades").toString();
		assertTrue(pathToString.startsWith(ReconcilerConfig.TRADES_DATA_PATH));
		assertTrue(pathToString.endsWith(".csv"));
	}
}
