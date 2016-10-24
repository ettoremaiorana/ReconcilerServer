/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fourcasters.forec.reconciler.query.history;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author ettoremaiorana
 */
public class HistoryDAO {

	private static final Logger LOG = LogManager.getLogger(HistoryDAO.class);
    static {
    	try {
    		System.loadLibrary("historydb");
    		init();
    	}
    	catch (UnsatisfiedLinkError e) {
    		LOG.warn("History service is unavailable on this instance");
    	}
    }
    public boolean dbhash(String pathToFile) {
        return dbhash(pathToFile.getBytes(StandardCharsets.US_ASCII));
    }
    public long offset(String pathToFile, int year, int month, int day, int hour, int minute) {
        return offset(pathToFile.getBytes(StandardCharsets.US_ASCII), year, month, day, hour, minute);
    }
    private static native boolean init();
    private native boolean dbhash(byte[] pathToFile);
    private native long offset(byte[] pathToFile, int year, int month, int day, int hour, int minute);
}
