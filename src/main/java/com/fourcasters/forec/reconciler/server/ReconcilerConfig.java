package com.fourcasters.forec.reconciler.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class ReconcilerConfig {


    private enum ENV {
		prod,
		demo,
		dev,
		test
	}

    public static final String ALGO_PATH;

	//market data
	public static final String MD_DATA_PATH;
	public static final String DEFAULT_MD_PATTERN;

	//trades
	public static final String TRADES_DATA_PATH;
	public static final String DEFAULT_TRADE_PATTERN;

	static {
		Properties p = new Properties();
		try(FileInputStream in = new FileInputStream(new File("config.properties"))) {
			p.load(in);
			
			String envAsProperty = System.getProperty("ENV");
			if (envAsProperty == null) {
				envAsProperty = System.getenv("ENV");
				if (envAsProperty == null) {
					throw new IllegalStateException("The system property ENV is not set");
				}
			}
			ENV env = ENV.valueOf(envAsProperty );
			if (env == null) {
				throw new IllegalStateException("ENV is not defined, please specify one of the following: " + Arrays.toString(ENV.values()));
			}
			MD_DATA_PATH = p.getProperty("md.data.path."+env);
			if (MD_DATA_PATH == null) {
				throw new IllegalStateException("Unspecified bkt data path for env " + env.toString());
			}
			DEFAULT_MD_PATTERN = p.getProperty("md.data.format", "o,h,l,c,v\u0020dd/mm/yyyy\u0020HH:MM");

			TRADES_DATA_PATH = p.getProperty("trades.data.path."+env);
			if (TRADES_DATA_PATH == null) {
				throw new IllegalStateException("Unspecified trades data path for env " + env.toString());
			}
			DEFAULT_TRADE_PATTERN = p.getProperty("md.data.format", "o,h,l,c,v\u0020dd/mm/yyyy\u0020HH:MM");

			ALGO_PATH = p.getProperty("algo.path."+env);
			if (ALGO_PATH == null) {
				throw new IllegalStateException("Unspecified algo path");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
