package com.nabilanam.libdownloader;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nabil
 */
public class DownloadBuilderTest {

	private static Download.Builder downloadBuilder;

	@BeforeClass
	public static void setUp() throws IOException {

		URL url = new URL("https://example.com/les_misérables.pdf");

		HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getURL()).thenReturn(url);
		when(mockConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_PARTIAL);

		int threadCount = 4;
		HttpInfo info = new HttpInfo(mockConnection, "");

		downloadBuilder = new Download.Builder(url);
		downloadBuilder.httpInfo(info);
		downloadBuilder.threadCount(threadCount);
	}

	@Test
	public void whenContentLengthIsGreaterThanOrEqualThreadCountNHttpInfoIsPartial_thenReturnTrue() {
		long contentLength = 5L;
		assertTrue(downloadBuilder.isMultiThreadDownload(contentLength));
		contentLength = 4L;
		assertTrue(downloadBuilder.isMultiThreadDownload(contentLength));
	}

	@Test
	public void whenContentLengthIsSmallerThanThreadCountNHttpInfoIsPartial_thenReturnFalse() {
		long contentLength = 3L;
		assertFalse(downloadBuilder.isMultiThreadDownload(contentLength));
	}
}
