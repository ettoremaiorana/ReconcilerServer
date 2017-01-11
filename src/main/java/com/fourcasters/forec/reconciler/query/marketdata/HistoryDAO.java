/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fourcasters.forec.reconciler.query.marketdata;

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

/**
 *
 * @author ettoremaiorana
 */
public class HistoryDAO extends IndexableDAO {

	private static final Logger LOG = LogManager.getLogger(HistoryDAO.class);

	public long offset(String cross, Date date) throws IOException {
		return offset(cross, date.getTime(), projectFirst);
	}

	public long offsetPlusOne(String cross, Date date) throws IOException {
		return offset(cross, date.getTime(), sum);
	}

	@Override
	public Path getRootPath() {
		return Paths.get(ReconcilerConfig.MD_DATA_PATH);
	}
	
	@Override
	public Path getFilePath(String cross) {
		return Paths.get(ReconcilerConfig.MD_DATA_PATH, cross + ProtocolConstants.MD_DATA_EXTENSION);
	}
	@Override
	public RecordBuilder getRecordBuilder(String recordFormat) {
		return new HistoryRecordBuilder(recordFormat);
	}

	@Override
	public RecordBuilder getRecordBuilder() {
		return new HistoryRecordBuilder(ReconcilerConfig.DEFAULT_MD_PATTERN);
	}

	@Override
	public void onIndexingEnd(String cross) {
		LOG.info(cross + " generated an index file of " + indexSize(cross) + " entries");
	}
}
