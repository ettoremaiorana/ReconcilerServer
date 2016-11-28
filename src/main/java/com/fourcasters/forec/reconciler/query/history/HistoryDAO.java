/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fourcasters.forec.reconciler.query.history;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fourcasters.forec.reconciler.query.IndexableDAO;
import com.fourcasters.forec.reconciler.query.RecordBuilder;
import com.fourcasters.forec.reconciler.server.ProtocolConstants;
import com.fourcasters.forec.reconciler.server.ReconcilerConfig;

/**
 *
 * @author ettoremaiorana
 */
public class HistoryDAO extends IndexableDAO {

	@Override
	public Path getRootPath() {
		return Paths.get(ReconcilerConfig.BKT_DATA_PATH);
	}
	
	@Override
	public String getDefaultFormat() {
		return ReconcilerConfig.DEFAULT_HISTORY_PATTERN;
	}

	@Override
	public Path getFilePath(String cross) {
		return Paths.get(ReconcilerConfig.BKT_DATA_PATH, cross + ProtocolConstants.BKT_DATA_EXTENSION);
	}
	@Override
	public RecordBuilder getRecordBuilder(String recordFormat) {
		return new HistoryRecordBuilder(recordFormat);
	}

	@Override
	public RecordBuilder getRecordBuilder() {
		return new HistoryRecordBuilder(ReconcilerConfig.DEFAULT_HISTORY_PATTERN);
	}
}
