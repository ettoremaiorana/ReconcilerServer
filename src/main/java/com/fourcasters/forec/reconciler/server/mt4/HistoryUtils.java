package com.fourcasters.forec.reconciler.server.mt4;

public class HistoryUtils {

	enum Frame {
		m1(0, 60000),
		m5(1, 60000*5),
		m15(2, 60000*15),
		m30(3, 60000*30),
		h1(4, 60000*60),
		h4(5, 60000*240),
		d1(6, 60000*60*24),
		w1(7, 60000*60*24*5);
		
		private Frame(int index, long millis) {
			this.index = index;
			this.millis = millis;
		}
		int index;
		long millis;
	}
	
	enum Cross {
		EURUSD(0),
		AUDCAD(1);
		
		private Cross(int index) {
			this.index = index;
		}
		final int index;
	}
}
