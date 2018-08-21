package com.nabilanam.libdownloader;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nabil
 */
public class SingleWorkerDownloadTest {

	private static Download download;
	private static DownloadListener listener;

	@BeforeClass
	public static void setUp() throws IOException {
		URL url = new URL("https://example.com/treasure_island.pdf");

		HttpURLConnection mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getURL()).thenReturn(url);
		when(mockConnection.getResponseCode()).thenReturn(206);

		HttpInfo info = new HttpInfo(mockConnection, "");
		listener = mock(DownloadListener.class);
		download = new Download.Builder(url)
				.directory(Paths.get("src/test/java/output"))
				.tmpDirectory(Paths.get("src/test/java/output/temp"))
				.httpInfo(info)
				.listener(listener)
				.build();
	}

	@Test
	public void whenBuildDownload_thenReturnGetters() {
		assertEquals(listener, download.getListener());
		assertEquals("treasure_island.pdf", download.getFileName());
		assertEquals(Paths.get("src/test/java/output"), download.getDirectory());
		assertEquals(Paths.get("src/test/java/output/temp"), download.getTmpDirectory());
		assertEquals(Paths.get("src/test/java/output/treasure_island.pdf"),
				download.getFilePath());
	}

	@Test
	public void whenFileIsNotDirectory_thenBeginReturnsFileLength() {
		File mockFile = mock(File.class);
		when(mockFile.length()).thenReturn(500L);
		when(mockFile.exists()).thenReturn(true);
		when(mockFile.isDirectory()).thenReturn(false);

		long begin = download.getBegin(mockFile);
		assertEquals(500L, begin);
	}

	@Test
	public void whenFileIsDirectory_thenBeginReturnsZero() {
		File mockDir = mock(File.class);
		when(mockDir.isDirectory()).thenReturn(true);

		long begin = download.getBegin(mockDir);
		assertEquals(0L, begin);
	}
}
