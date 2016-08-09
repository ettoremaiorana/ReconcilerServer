package com.fourcasters.forec.reconciler.server;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StrategiesTrackerTest {

	@Mock private InitialStrategiesLoader loader;
	private StrategiesTracker tracker;
	private ApplicationInterface application;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		tracker = new StrategiesTracker(application, loader);
		when(loader.load(any(Set.class))).thenReturn(0);
	}

	@Test
	public void testAdd() {
		int[] array = tracker.getStrategies();
		assertEquals(0, array.length);
		
		tracker.add(1);
		array = tracker.getStrategies();
		assertEquals(1, array.length);
		
		tracker.add(1);
		array = tracker.getStrategies();
		assertEquals(1, array.length);
		
		tracker.add(2);
		array = tracker.getStrategies();
		assertEquals(2, array.length);
		
		tracker.add(3);
		array = tracker.getStrategies();
		assertEquals(3, array.length);
		
		tracker.add(2);
		array = tracker.getStrategies();
		assertEquals(3, array.length);
	}
}
