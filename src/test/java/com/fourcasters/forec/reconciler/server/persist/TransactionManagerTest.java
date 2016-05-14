package com.fourcasters.forec.reconciler.server.persist;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.server.ApplicationInterface;
@RunWith(MockitoJUnitRunner.class)
public class TransactionManagerTest {

//	private String singleData = "12345=SINGLE=a,b,c,d,e,f";
//	private String fullData = "67890=FULL=a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x";
//	private String fullDataRequiringMore = "67890=FULL=z,z,z,z,z,z||y,y,y,y,y,y||more";

	private ApplicationInterface application = new ApplicationMock();
	private TransactionManager transactionManager;
	private TaskFactory taskFactory;
	
	@Before
	public void setup() {
		taskFactory = new TaskFactory(application);
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
	}

	@Test
	public void onNewTaskPollingRequestIsEnqueued() {
		transactionManager.onFullTransaction(67890, "z,z,z,z,z,z||y,y,y,y,y,y||more");
		assertEquals(1, application.selectorTasks().size());
		transactionManager.onFullTransaction(67890, "a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x");
		assertEquals(2, application.selectorTasks().size());
	}


	@Test
	public void onNewTaskTheTaskCountIncreasesByOne() {
		transactionManager.onSingleTransaction(12345, "a,b,c,d,e,f");
		assertEquals(1, transactionManager.tasksToRun());
		transactionManager.onFullTransaction(67890, "a,b,c,d,e,f||g,h,i,j,k,l||m,n,o,p,q,r||s,t,u,v,w,x");
		assertEquals(2, transactionManager.tasksToRun());
	}

	@Test
	public void onNewSingleTaskTransactionIsAddedAndThenRemoved() {
		transactionManager.onSingleTransaction(12345, "a,b,c,d,e,f");
		assertEquals(1, transactionManager.numberOfTransactions()); //one task
		assertEquals(1, transactionManager.tasksToRun()); //one task
		assertEquals(1, application.selectorTasks().size()); //decrease tasksToRun
		
	}
}
