package com.fourcasters.forec.reconciler.query;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class DataFetcher {

	public int fetch(RandomAccessFile f, int start, int end, byte[] dest) {
		Scanner sc = new Scanner("");
		while(sc.hasNext()) {
			System.out.println(sc.nextLine());
		}
		try {
			f.seek(start);
			f.read(dest, 0, end - start);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	public static void main(String[] args) {
		System.out.println(System.currentTimeMillis());
	}
}
