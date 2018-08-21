package com.nabilanam.libdownloader;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author nabil
 */
public class HttpInfoTest {
	private static String userAgent;
	private static HttpURLConnection mockConnection;

	@BeforeClass
	public static void setUp() throws IOException {
		URL url = new URL("https://www.example.com/her_benny.pdf");
		userAgent = "userAgent";

		mockConnection = mock(HttpURLConnection.class);
		when(mockConnection.getURL()).thenReturn(url);
		when(mockConnection.getResponseCode()).thenReturn(206);
		when(mockConnection.getContentLengthLong()).thenReturn(5L);
		when(mockConnection.getHeaderField("Content-Type")).thenReturn("application/pdf");
	}

	@Test
	public void test_HttpInfo() {
		HttpInfo info = new HttpInfo(mockConnection, userAgent);

		assertTrue(info.isPartial());
		assertEquals(userAgent, info.getUserAgent());
		assertEquals(5L, info.getContentLength());
		assertEquals("application/pdf", info.getContentType());
		assertEquals("her_benny.pdf", info.getName());
	}
}
