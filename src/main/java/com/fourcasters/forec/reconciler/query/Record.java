package com.fourcasters.forec.reconciler.query;

public abstract class Record {

	public abstract boolean hasToIndex(Record prev);

	public abstract Long index();

}
