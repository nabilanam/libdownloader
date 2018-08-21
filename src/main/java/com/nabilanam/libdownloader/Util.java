package com.nabilanam.libdownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * @author nabil
 */
public class Util {

	public static boolean isNull(Object ob) {
		return ob == null;
	}

	public static boolean isStringNullOrEmpty(String str) {
		return isNull(str) || str.isEmpty();
	}

	public static boolean isPathNullOrEmpty(Path path) {
		return isNull(path) || path.toString().isEmpty();
	}

	public static boolean isCollectionNullOrEmpty(Collection<?> collection) {
		return isNull(collection) || collection.isEmpty();
	}

	public static void createDirectory(Path directory) throws IOException {
		if (!isPathNullOrEmpty(directory)) {
			Files.createDirectories(directory);
		}
	}

	public static boolean isNonDirectoryFile(File file) {
		return file.exists() && !file.isDirectory();
	}
}
