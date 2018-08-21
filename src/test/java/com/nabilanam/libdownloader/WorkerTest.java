package com.nabilanam.libdownloader;

import org.junit.BeforeClass;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author nabil
 */
public class WorkerTest {

	private static URL url;

	@BeforeClass
	public static void setUp() {
		try {
			url = new URL("http://");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void whenBeginSmallerThanEnd_thenBytesEqualBeginToEnd() {
		Worker worker = new Worker.Builder(url, Paths.get(""))
				.begin(0L)
				.end(1L)
				.build();
		String range = worker.getRange();
		assertEquals("bytes=0-1", range);
	}

	@Test
	public void whenBeginGreaterThanEnd_thenBytesEqualBeginTo() {
		Worker worker = new Worker.Builder(url, Paths.get(""))
				.begin(1L)
				.end(0L)
				.build();
		String range = worker.getRange();
		assertEquals("bytes=1-", range);
	}

	@Test
	public void whenResponseOkOrPartial_thenIsDownloadable() {
		Worker worker = new Worker.Builder(url, Paths.get(""))
				.begin(1L)
				.build();
		assertTrue(worker.isDownloadable(200));
		assertTrue(worker.isDownloadable(206));
	}
}
