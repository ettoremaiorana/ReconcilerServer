package com.fourcasters.forec.reconciler.server;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class InitialStrategiesLoader {

	public int load(Set<Integer> strategies) throws IOException {
		final Path start = Paths.get("./");
		AtomicInteger counter = new AtomicInteger(0);
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException
			{
				final String fileName = file.getFileName().toString();
				final int indexOf = fileName.indexOf("_performance");
				if (indexOf >= 0) {
					strategies.add(Integer.parseInt(fileName.substring(0, indexOf)));
					counter.incrementAndGet();
				}
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException e)
					throws IOException
			{
				return FileVisitResult.CONTINUE;
			}
		});
		return counter.get();
	}
}
