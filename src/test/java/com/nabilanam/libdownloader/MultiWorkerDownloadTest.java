package com.nabilanam.libdownloader;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nabil
 */
public class MultiWorkerDownloadTest {
	private static Download download;

	@BeforeClass
	public static void setUp() throws IOException {
		URL url = new URL("https://example.com/les_misérables.pdf");

		HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getURL()).thenReturn(url);
		when(mockConnection.getResponseCode()).thenReturn(206);

		HttpInfo info = new HttpInfo(mockConnection, "");
		download = new Download.Builder(url)
				.threadCount(4)
				.httpInfo(info)
				.build();
	}

	@Test
	public void whenContentLengthIsGreaterThanOrEqualThreadCountNHttpInfoIsPartial_thenReturnTrue() {
		long contentLength = 5L;
		assertTrue(download.isMultiWorkerDownload(contentLength));
		contentLength = 4L;
		assertTrue(download.isMultiWorkerDownload(contentLength));
	}

	@Test
	public void whenContentLengthIsSmallerThanThreadCountNHttpInfoIsPartial_thenReturnFalse() {
		long contentLength = 3L;
		assertFalse(download.isMultiWorkerDownload(contentLength));
	}

	@Test
	public void whenBeginFileIsNotDirectory_thenBeginIsSumOfFileLengthPEndPOne() {
		File mockFile = mock(File.class);
		when(mockFile.length()).thenReturn(500L);
		when(mockFile.exists()).thenReturn(true);
		when(mockFile.isDirectory()).thenReturn(false);

		long end = 1000L;
		long begin = download.getBegin(mockFile, end);
		assertEquals(mockFile.length() + end + 1, begin);
	}

	@Test
	public void whenBeginFileIsDirectory_thenBeginIsSumOfEndPOne() {
		File mockDir = mock(File.class);
		when(mockDir.isDirectory()).thenReturn(true);

		long end = 1000L;
		long begin = download.getBegin(mockDir, end);
		assertEquals(end + 1, begin);
	}

	@Test
	public void whenEndCounterIsThreadCountMOne_thenReturnContentLength() {
		long end = download.getEnd(3, 500L, 1, 2);
		assertEquals(500L, end);
	}

	@Test
	public void whenCounterIsNotThreadCountMOne_thenReturnEndPSizePOne() {
		long end = download.getEnd(2, 500L, 1, 2);
		assertEquals(4L, end);
	}
}
