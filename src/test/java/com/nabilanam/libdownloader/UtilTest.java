package com.nabilanam.libdownloader;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author nabil
 */
public class UtilTest {

	@Test
	public void test_isNull() {
		assertTrue(Util.isNull(null));
		assertFalse(Util.isNull(new String()));
	}

	@Test
	public void test_isStringNullOrEmpty() {
		assertTrue(Util.isStringNullOrEmpty(null));
		assertTrue(Util.isStringNullOrEmpty(""));
		assertFalse(Util.isStringNullOrEmpty("Mark Twain"));
	}

	@Test
	public void test_isPathNullOrEmpty() {
		assertTrue(Util.isPathNullOrEmpty(null));
		assertTrue(Util.isPathNullOrEmpty(Paths.get("")));
		assertFalse(Util.isPathNullOrEmpty(Paths.get("src")));
	}

	@Test
	public void test_isCollectionNullOrEmpty() {
		List<Boolean> bools = new ArrayList<>();
		bools.add(true);

		assertTrue(Util.isCollectionNullOrEmpty(null));
		assertTrue(Util.isCollectionNullOrEmpty(new ArrayList<>()));
		assertFalse(Util.isCollectionNullOrEmpty(bools));
	}

	@Test
	public void test_isNotDirectory() {
		assertTrue(Util.isNonDirectoryFile(new File(".gitignore")));
		assertFalse(Util.isNonDirectoryFile(new File("")));
		assertFalse(Util.isNonDirectoryFile(new File("target")));
	}
}
