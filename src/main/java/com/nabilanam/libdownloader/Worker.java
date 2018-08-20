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
class Worker implements Runnable {
	private URL url;
	private long end;
	private long begin;
	private String eTag;
	private Path filePath;
	private String userAgent;
	private String lastModified;
	private CountDownLatch doneLatch;
	private CountDownLatch stopLatch;
	private Download download;

	private Worker() {
	}

	@Override
	public void run() {
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("User-Agent", userAgent);
			con.setInstanceFollowRedirects(true);
			if (begin < end) {
				con.setRequestProperty("Range", "bytes=" + begin + "-" + end);
			} else if (begin > end) {
				con.setRequestProperty("Range", "bytes=" + begin + "-");
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

			if (responseCode == HttpURLConnection.HTTP_OK
					|| responseCode == HttpURLConnection.HTTP_PARTIAL) {
				try (InputStream inputStream = con.getInputStream();
				     OutputStream outputStream = new FileOutputStream(filePath.toFile(), true)) {
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

	static class Builder {
		private URL url;
		private long end;
		private long begin;
		private String eTag;
		private Path filePath;
		private String userAgent;
		private String lastModified;
		private CountDownLatch doneLatch;
		private CountDownLatch stopLatch;
		private Download download;

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

		Builder eTag(String eTag) {
			this.eTag = eTag;
			return this;
		}

		Builder userAgent(String userAgent) {
			this.userAgent = userAgent;
			return this;
		}

		Builder lastModified(String lastModified) {
			this.lastModified = lastModified;
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

		Worker build() {
			Worker worker = new Worker();
			worker.url = url;
			worker.end = end;
			worker.eTag = eTag;
			worker.begin = begin;
			worker.filePath = filePath;
			worker.download = download;
			worker.userAgent = userAgent;
			worker.doneLatch = doneLatch;
			worker.stopLatch = stopLatch;
			worker.lastModified = lastModified;
			return worker;
		}
	}
}
