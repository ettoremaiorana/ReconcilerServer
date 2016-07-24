package com.fourcasters.forec.reconciler.server;

import static org.junit.Assert.*;

import org.junit.Test;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;

public class MessageHandlerFactoryTest {

	private MessageHandlerFactory mhf = new MessageHandlerFactory(new ApplicationMock());
	
	@Test
	public void test() {
		assertEquals(Forwarder.class, mhf.get("RECONC@").getClass());
	}

}
