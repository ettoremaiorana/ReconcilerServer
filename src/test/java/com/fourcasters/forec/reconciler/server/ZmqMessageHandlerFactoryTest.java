package com.fourcasters.forec.reconciler.server;

import static org.junit.Assert.*;

import com.fourcasters.forec.reconciler.EmailSender;
import com.fourcasters.forec.reconciler.server.trades.TradeEventCapturer;
import com.fourcasters.forec.reconciler.server.trades.TradeReconcilerMessageSender;
import com.fourcasters.forec.reconciler.server.trades.persist.TradePersister;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;

@RunWith(MockitoJUnitRunner.class)
public class ZmqMessageHandlerFactoryTest {

	private ZmqModule zmqModule;

	@Before
	public void setup() {
		zmqModule = new ZmqModule(new ApplicationMock());
		zmqModule.start();
	}

	@Test
	public void test() {
		assertEquals(Forwarder.class, zmqModule.getMsgHandler("RECONC@").getClass());
		assertEquals(TradePersister.class, zmqModule.getMsgHandler("HISTORY@").getClass());
		assertEquals(TradeEventCapturer.class, zmqModule.getMsgHandler("STATUS@").getClass());
	}

	@After
    public void tearDown() {
	    zmqModule.stop();
    }
}
