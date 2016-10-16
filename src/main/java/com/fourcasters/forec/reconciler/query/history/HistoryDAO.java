/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fourcasters.forec.reconciler.query.history;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author ettoremaiorana
 */
public class HistoryDAO {

    static {
        System.loadLibrary("historydb");
        init();
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
