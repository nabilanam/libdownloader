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
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PARTIAL);

		int threadCount = 4;
		HttpInfo info = new HttpInfo(mockConnection, "");
		download = new Download.Builder(url)
				.threadCount(threadCount)
				.httpInfo(info)
				.build();
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
		long end = download.getEnd(download.getThreadCount() - 1, 500L, 1, 2);
		assertEquals(500L, end);
	}

	@Test
	public void whenCounterIsNotThreadCountMOne_thenReturnEndPSizePOne() {
		long end = download.getEnd(download.getThreadCount() - 2, 500L, 1, 2);
		assertEquals(4L, end);
	}

	@Test
	public void whenBeginGreaterThanEnd_thenReturnTrue() {
		assertTrue(download.isMultiWorkerFileAlreadyDownloaded(2, 1));
	}

	@Test
	public void whenBeginSmallerThanEnd_thenReturnFalse() {
		assertFalse(download.isMultiWorkerFileAlreadyDownloaded(1, 2));
	}
}
