package com.fourcasters.forec.reconciler.server;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;
import com.fourcasters.forec.reconciler.server.cli.PerformanceCalc.PerformanceCalcTask;

@RunWith(MockitoJUnitRunner.class)
public class TradeEventCapturerTest {

	private TradeEventCapturer capture;
	private ApplicationMock application;

	@Before
	public void setup() {
		application = new ApplicationMock();
		capture = new TradeEventCapturer(application);
	}

	@Test
	public void onNewTradePerformanceCalcIsTriggered() {
		String topic = "hello" + "@" + "anycross" + "@" + "12345678";
		String data  = "status" + "," + "1" + "," + "87678" + "," + "987654321";
		capture.enqueue(topic, data);
		verify(application.executor(), times(1)).submit(isA(PerformanceCalcTask.class));
	}

}
