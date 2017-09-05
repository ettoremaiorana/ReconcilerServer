package com.fourcasters.forec.reconciler.server;

import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.verify;

import com.fourcasters.forec.reconciler.EmailSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fourcasters.forec.reconciler.mocks.ApplicationMock;
import com.fourcasters.forec.reconciler.server.PerformanceCalc.PerformanceCalcTask;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class TradeEventCapturerTest {

	private TradeEventCapturer capture;
	private ApplicationMock application;
	@Mock private ReconcilerMessageSender sender;
	@Mock private StrategiesTracker strTracker;
	@Mock private EmailSender emailSender;

	@Before
	public void setup() {
		application = new ApplicationMock();
		capture = new TradeEventCapturer(application, sender, strTracker, emailSender);
	}

	@After
    public void tearDown() {
	    application.reset();
    }

	@Test
	public void onNewTradePerformanceCalcIsTriggered() {
		String topic = "hello" + "@" + "anycross" + "@" + "12345678";
		String data  = 1 + "," + "close" + "," + "87678" + "," + "987654321";
		capture.enqueue(topic, data);
		assertTrue(application.hadSubmittedEvents(new ArrayList<>(Arrays.asList(PerformanceCalcTask.class, StrategiesCaptureTask.class))));
	}

}
