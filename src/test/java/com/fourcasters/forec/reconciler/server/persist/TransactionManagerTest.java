package com.fourcasters.forec.reconciler.server.persist;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;
import com.fourcasters.forec.reconciler.server.ApplicationInterface;
import com.fourcasters.forec.reconciler.server.ReconcilerMessageSender;
import com.fourcasters.forec.reconciler.server.SelectorTask;
@RunWith(MockitoJUnitRunner.class)
public class TransactionManagerTest {

//	private String singleData = "12345=SINGLE=a,b,c,d,e,f";
//	private String fullData = "67890=FULL=a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x";
//	private String fullDataRequiringMore = "67890=FULL=z,z,z,z,z,z||y,y,y,y,y,y||more";

	@Mock private ReconcilerMessageSender sender;
	private ApplicationInterface application = new ApplicationMock();
	private TransactionManager transactionManager;
	private TradeTaskFactory taskFactory;
	
	@Before
	public void setup() {
		taskFactory = new TradeTaskFactory(application, sender);
		transactionManager = new TransactionManager(taskFactory, application);
	}


	@Test
	public void onNewTaskNewTransactionIsCreatedIfDidNotExisted() {
		transactionManager.onSingleTransaction(12345, "a,b,c,d,e,f");
		assertEquals(1, transactionManager.numberOfTransactions());
		transactionManager.onFullTransaction(67890, "a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x");
		assertEquals(2, transactionManager.numberOfTransactions());
		transactionManager.onFullTransaction(67890, "z,z,z,z,z,z||y,y,y,y,y,y||more");
		assertEquals(2, transactionManager.numberOfTransactions());
		transactionManager.onOpenTransaction(54321, "f,e,d,c,b,a");
		assertEquals(3, transactionManager.numberOfTransactions());
	}

	@Test
	public void onNewTaskPollingRequestIsEnqueued() {
		transactionManager.onFullTransaction(67890, "z,z,z,z,z,z||y,y,y,y,y,y||more");
		verify(application.selectorTasks(), times(1)).add(any(SelectorTask.class));
		transactionManager.onFullTransaction(67890, "a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x");
		verify(application.selectorTasks(), times(2)).add(any(SelectorTask.class));
		transactionManager.onOpenTransaction(54321, "f,e,d,c,b,a");
		verify(application.selectorTasks(), times(3)).add(any(SelectorTask.class));
		transactionManager.onSingleTransaction(12345, "a,b,c,d,e,f");
		verify(application.selectorTasks(), times(4)).add(any(SelectorTask.class));
	}


	@Test
	public void onNewTaskTheTaskCountIncreasesByOne() {
		transactionManager.onSingleTransaction(12345, "a,b,c,d,e,f");
		assertEquals(1, transactionManager.tasksToRun());
		transactionManager.onFullTransaction(67890, "a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x");
		assertEquals(2, transactionManager.tasksToRun());
		transactionManager.onOpenTransaction(54321, "f,e,d,c,b,a");
		assertEquals(3, transactionManager.tasksToRun());
	}

	@Test
	public void onNewSingleTaskTransactionIsAddedAndThenRemoved() {
		transactionManager.onSingleTransaction(12345, "a,b,c,d,e,f");
		assertEquals(1, transactionManager.numberOfTransactions()); //one task
		assertEquals(1, transactionManager.tasksToRun()); //one task
		verify(application.selectorTasks(), times(1)).add(any(SelectorTask.class)); //decrease tasksToRun
	}

	@Test
	public void onNewOpenTaskTransactionIsAddedAndThenRemoved() {
		transactionManager.onOpenTransaction(54321, "f,e,d,c,b,a");
		assertEquals(1, transactionManager.numberOfTransactions()); //one task
		assertEquals(1, transactionManager.tasksToRun()); //one task
		verify(application.selectorTasks(), times(1)).add(any(SelectorTask.class));//decrease tasksToRun
	}

	@SuppressWarnings("unchecked")
	@Test
	public void onClosedOpenTaskTransactionNewOpenRequestIsIssued() {
		transactionManager.onOpenTransaction(54321, "f,e,d,c,b,a");
		ArgumentCaptor<SelectorTask> taskCapture = ArgumentCaptor.forClass(SelectorTask.class);
		verify(application.selectorTasks(), times(1)).add(taskCapture.capture());
		reset(application.selectorTasks());
		
		taskCapture.getValue().run(); //Polling task
		ArgumentCaptor<Runnable> taskCapture1 = ArgumentCaptor.forClass(Runnable.class);
		verify(application.executor(), times(1)).submit(taskCapture1.capture());
		reset(application.executor());

		taskCapture1.getValue().run();//Open trade task
		verify(sender, never()).askForOpenTrades(any(String.class));
		ArgumentCaptor<SelectorTask> taskCapture2 = ArgumentCaptor.forClass(SelectorTask.class);
		//4 tasks added: 
		//1- on transaction start of transaction manager
		//2- on transaction end of transaction manager
		//3- on transaction end of message sender
		//4- decrease task count
		verify(application.selectorTasks(), times(4)).add(taskCapture2.capture());
		
		taskCapture2.getAllValues().forEach(r -> r.run());
		verify(sender, times(1)).askForOpenTrades(any(String.class));
	}
}
