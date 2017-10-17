package com.fourcasters.forec.reconciler.server;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Logger;

import static org.apache.logging.log4j.LogManager.getLogger;

public class StrategiesTracker {

	private static final Logger LOG = getLogger(InitialStrategiesLoader.class);

	private final CopyOnWriteArraySet<Integer> strategies = new CopyOnWriteArraySet<>();
	private int[] asArray;

	public StrategiesTracker(InitialStrategiesLoader loader) {
		loader.load(strategies);
		asArray = new int[strategies.size()];
		AtomicInteger i = new AtomicInteger(0);
		strategies.forEach((id) -> asArray[i.getAndIncrement()] = id);
	}

	public void add(int algoId) {
		CopyOnWriteArraySet<Integer> local = strategies;
		Integer algoIdBoxed = Integer.valueOf(algoId);
		if (!local.contains(algoIdBoxed)) {
			local.add(algoIdBoxed);
			int[] copy = new int[asArray.length + 1];
			System.arraycopy(asArray, 0, copy, 0, asArray.length);
			copy[asArray.length] = algoId;
			asArray = copy;
		}
	}

	public int[] getStrategies() {
		int[] copy = new int[asArray.length];
		System.arraycopy(asArray, 0, copy, 0, asArray.length);
		return copy;
	}
}
