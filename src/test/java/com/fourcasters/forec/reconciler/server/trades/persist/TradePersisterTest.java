package com.fourcasters.forec.reconciler.server.trades.persist;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;
import com.fourcasters.forec.reconciler.server.ApplicationInterface;

@RunWith(MockitoJUnitRunner.class)
public class TradePersisterTest {

	private String topic = "Topic";
	private String singleData = "12345=SINGLE=a,b,c,d,e,f";
	private String openData = "12345=OPEN=a,b,c,d,e,f";
	private String fullData = "67890=FULL=a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x";
	private TradePersister persister;

	private ApplicationInterface application = new ApplicationMock();
	@Mock private TransactionManager transactionManager;

	@Before
	public void setup() {
		persister = new TradePersister(transactionManager, application);
	}

	@Test
	public void onNewMessageANewTaskIsCreated() {
		persister.enqueue(topic, singleData);
		verify(transactionManager).onSingleTransaction(12345, "a,b,c,d,e,f");
		persister.enqueue(topic, openData);
		verify(transactionManager).onOpenTransaction(12345, "a,b,c,d,e,f");
		persister.enqueue(topic, fullData);
		verify(transactionManager).onFullTransaction(67890, "a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x");
	}
}
