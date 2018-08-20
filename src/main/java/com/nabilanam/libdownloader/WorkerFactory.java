package com.nabilanam.libdownloader;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * @author nabil
 */
class WorkerFactory {

	private Download download;
	private HttpInfo info;

	WorkerFactory(Download download, HttpInfo info) {
		this.download = download;
		this.info = info;
	}

	Worker getWorker(CountDownLatch stopLatch, CountDownLatch doneLatch, Path path, long begin) {
		return new Worker
				.Builder(info.getUrl(), path)
				.begin(begin)
				.userAgent(info.getUserAgent())
				.eTag(info.getETag())
				.stopLatch(stopLatch)
				.doneLatch(doneLatch)
				.lastModified(info.getLastModified())
				.download(download)
				.build();
	}

	Worker getWorker(CountDownLatch stopLatch, CountDownLatch doneLatch, Path path, long begin, long end) {
		return new Worker
				.Builder(info.getUrl(), path)
				.begin(begin)
				.end(end)
				.userAgent(info.getUserAgent())
				.eTag(info.getETag())
				.stopLatch(stopLatch)
				.doneLatch(doneLatch)
				.lastModified(info.getLastModified())
				.download(download)
				.build();
	}
}
