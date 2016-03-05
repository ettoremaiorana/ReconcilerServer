package com.fourcasters.forec.reconciler.server;

import static org.junit.Assert.*;

import org.junit.Test;

public class MessageHandlerFactoryTest {

	private MessageHandlerFactory mhf = new MessageHandlerFactory();
	
	@Test
	public void test() {
		assertEquals(Forwarder.class, mhf.get("RECONC@").getClass());
	}

}
