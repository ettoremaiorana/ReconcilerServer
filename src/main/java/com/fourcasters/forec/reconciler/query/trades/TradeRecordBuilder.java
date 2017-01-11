package com.fourcasters.forec.reconciler.query.trades;

import com.fourcasters.forec.reconciler.query.Record;
import com.fourcasters.forec.reconciler.query.RecordBuilder;

public class TradeRecordBuilder extends RecordBuilder {

	private static final Record EMPTY_RECORD = new TradeRecord(-1);

	public TradeRecordBuilder(String defaultTradePattern) {
	}

	//-1,1.13196,1.13167,29,-1,1,04/27/2016 22:00,04/28/2016 06:01,1,-1,-1,EURUSD,1.14103,1.12283,0,0,commento,1002,104166918,
	@Override
	public Record newRecord(String recordAsString) {
		if (!recordAsString.isEmpty()) {
			int lastComma = recordAsString.lastIndexOf(",");
			long tradeId = Long.parseLong(recordAsString.substring(lastComma+1));
			return new TradeRecord(tradeId);
		}
		return EMPTY_RECORD;
	}

	static final class TradeRecord extends Record {

		private final long tradeId;
		
		public TradeRecord(long tradeId) {
			this.tradeId = tradeId;
		}

		@Override
		public boolean shouldIndex(Record prev) {
			if (tradeId != -1) {
				return true;
			}
			return false;
		}

		@Override
		public Long index() {
			return tradeId;
		}
	}
}
