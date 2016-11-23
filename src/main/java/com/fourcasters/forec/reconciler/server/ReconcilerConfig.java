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

	public static final String BKT_DATA_PATH;

	static {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream(new File("config.properties")));
			
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
			BKT_DATA_PATH = p.getProperty("bkt.data.path."+env);
			if (BKT_DATA_PATH == null) {
				throw new IllegalStateException("Unspecified bkt data path for env " + env.toString());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
