package com.nabilanam.libdownloader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * @author nabil
 */
final class Worker implements Runnable {

	private final URL url;
	private final long end;
	private final long begin;
	private final Path filePath;
	private final String userAgent;
	private final CountDownLatch doneLatch;
	private final CountDownLatch stopLatch;
	private final Download download;
	private final boolean append;

	private Worker(URL url, String userAgent, long begin, long end, Path filePath,
	               CountDownLatch doneLatch, CountDownLatch stopLatch, Download download, boolean append) {
		this.url = url;
		this.end = end;
		this.begin = begin;
		this.filePath = filePath;
		this.userAgent = userAgent;
		this.doneLatch = doneLatch;
		this.stopLatch = stopLatch;
		this.download = download;
		this.append = append;
	}

	@Override
	public void run() {
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("User-Agent", userAgent);
			con.setInstanceFollowRedirects(true);
			if (begin != end) {
				String range = getRange();
				con.setRequestProperty("Range", range);
			}
			con.setRequestMethod("GET");

			int responseCode;
			try {
				responseCode = con.getResponseCode();
				con.connect();
			} catch (IOException ex) {
				con.disconnect();
				return;
			}

			if (isDownloadable(responseCode)) {
				try (InputStream inputStream = con.getInputStream();
				     OutputStream outputStream = new FileOutputStream(filePath.toFile(), append)) {
					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
						if (Thread.currentThread().isInterrupted()) {
							con.disconnect();
							outputStream.flush();
							inputStream.close();
							outputStream.close();
							if (!Util.isNull(stopLatch))
								stopLatch.countDown();
							return;
						}
						outputStream.write(buffer, 0, bytesRead);
						outputStream.flush();
						if (!Util.isNull(download))
							download.downloaded(bytesRead);
					}
				}
			}
			con.disconnect();
			if (!Util.isNull(stopLatch))
				stopLatch.countDown();
			if (!Util.isNull(doneLatch))
				doneLatch.countDown();
		} catch (IOException e) {
			if (!Util.isNull(con))
				con.disconnect();
		}
	}

	String getRange() {
		String range = "";
		if (begin < end) {
			range = "bytes=" + begin + "-" + end;
		} else if (begin > end) {
			range = "bytes=" + begin + "-";
		}
		return range;
	}

	boolean isDownloadable(int responseCode) {
		return responseCode == HttpURLConnection.HTTP_OK
				|| responseCode == HttpURLConnection.HTTP_PARTIAL;
	}

	static class Builder {
		private URL url;
		private long end;
		private long begin;
		private Path filePath;
		private String userAgent;
		private CountDownLatch doneLatch;
		private CountDownLatch stopLatch;
		private Download download;
		private boolean append = true;

		Builder(URL url, Path filePath) {
			this.url = url;
			this.filePath = filePath;
		}

		Builder begin(long begin) {
			this.begin = begin;
			return this;
		}

		Builder end(long end) {
			this.end = end;
			return this;
		}

		Builder userAgent(String userAgent) {
			this.userAgent = userAgent;
			return this;
		}

		Builder doneLatch(CountDownLatch doneLatch) {
			this.doneLatch = doneLatch;
			return this;
		}

		Builder stopLatch(CountDownLatch stopLatch) {
			this.stopLatch = stopLatch;
			return this;
		}

		Builder download(Download download) {
			this.download = download;
			return this;
		}

		Builder append(boolean append) {
			this.append = append;
			return this;
		}

		Worker build() {
			return new Worker(url, userAgent, begin, end, filePath,
					doneLatch, stopLatch, download, append);
		}
	}
}
