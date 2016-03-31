package com.fourcasters.forec.reconciler.server.mt4;

public class HistoryDataPoint {

	private int open;
	private int high;
	private int low;
	private int close;
	private int vol;
	//there's room for another int here
	private long dateInMillis;

	public HistoryDataPoint(int open, int high, int low, int close, int vol, long dateInMillis) {
		super();
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.vol = vol;
		this.dateInMillis = dateInMillis;
	}

	public int open() {
		return open;
	}

	public int high() {
		return high;
	}

	public int low() {
		return low;
	}

	public int close() {
		return close;
	}

	public int vol() {
		return vol;
	}

	public long date() {
		return dateInMillis;
	}

	public void open(int open) {
		this.open = open;
	}

	public void high(int high) {
		this.high = high;
	}

	public void low(int low) {
		this.low = low;
	}

	public void close(int close) {
		this.close = close;
	}

	public void vol(int vol) {
		this.vol = vol;
	}
	


}
