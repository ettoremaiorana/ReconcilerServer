package com.fourcasters.forec.reconciler.server;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

public class StrategiesTracker {

	private final CopyOnWriteArraySet<Integer> strategies = new CopyOnWriteArraySet<>();
	private int[] asArray;

	public StrategiesTracker(ApplicationInterface application, InitialStrategiesLoader loader) throws IOException {
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
