package com.fourcasters.forec.reconciler.server;

import static org.junit.Assert.*;

import com.fourcasters.forec.reconciler.EmailSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;

@RunWith(MockitoJUnitRunner.class)
public class MessageHandlerFactoryTest {

	@Mock private ReconcilerMessageSender rms;
	@Mock private StrategiesTracker strTracker;
	@Mock private EmailSender emailSender;
	private MessageHandlerFactory mhf = new MessageHandlerFactory(new ApplicationMock(), rms, strTracker, emailSender);
	
	@Test
	public void test() {
		assertEquals(Forwarder.class, mhf.get("RECONC@").getClass());
	}

}
