package com.fourcasters.forec.reconciler.query.trades;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fourcasters.forec.reconciler.query.IndexableDAO;
import com.fourcasters.forec.reconciler.query.RecordBuilder;
import com.fourcasters.forec.reconciler.server.ProtocolConstants;
import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

public class TradesDAO extends IndexableDAO {


	private static final Logger LOG = LogManager.getLogger(TradesDAO.class);

	public long tradeOffset(String cross, long toSearch) throws IOException {
		return offsetExact(cross, toSearch);
	}
	@Override
	public Path getRootPath() {
		return Paths.get(ReconcilerConfig.TRADES_DATA_PATH);
	}

	@Override
	public Path getFilePath(String table) {
		return Paths.get(ReconcilerConfig.TRADES_DATA_PATH,table + ProtocolConstants.TRADES_DATA_EXTENSION);
	}

	@Override
	public RecordBuilder getRecordBuilder(String recordFormat) {
		throw new UnsupportedOperationException("No other format of trade record is allowed");
	}

	@Override
	public RecordBuilder getRecordBuilder() {
		return new TradeRecordBuilder(ReconcilerConfig.DEFAULT_TRADE_PATTERN);
	}

	@Override
	public void onIndexingEnd(String table) {
		LOG.info(table + " indexing has completed with an index of size " + indexSize(table));
	}

}
