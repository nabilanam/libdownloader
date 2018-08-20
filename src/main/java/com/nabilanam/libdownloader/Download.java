package com.nabilanam.libdownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author nabil
 */
public class Download {

	private String fileName;
	private int threadCount;
	private boolean isComplete;
	private Path directory;
	private Path tmpDirectory;
	private List<Path> tmpPaths;
	private Thread async;
	private HttpInfo info;
	private ExecutorService es;
	private WorkerFactory factory;
	private Semaphore interruptSignal;
	private DownloadListener listener;

	private Download() {
	}

	/**
	 * Start blocking download.
	 */
	public void start() {
		runnable().run();
	}

	/**
	 * Start async download.
	 */
	public void startAsync() {
		async = new Thread(runnable());
		async.start();
	}

	/**
	 * Stop async download. Wait until it's fully stopped.
	 */
	public void stop() {
		stopAsync();
		semaphoreAcquire(interruptSignal);
		interruptSignal.release();
	}

	/**
	 * Signal async download to stop.
	 */
	public void stopAsync() {
		async.interrupt();
	}

	/**
	 * Get low level info for this download.
	 *
	 * @return DownloadInfo
	 */
	public HttpInfo getHttpInfo() {
		return info;
	}

	/**
	 * Returns true if every steps to download are performed successfully.
	 *
	 * @return isComplete
	 */
	public boolean isDownloadComplete() {
		return isComplete;
	}

	synchronized void downloaded(int bytes) {
		if (!Util.isNull(listener)) listener.downloaded(bytes);
	}

	private Runnable runnable() {
		resetExecutor();
		return () -> {
			List<Future<?>> futures = new ArrayList<>();
			CountDownLatch stopLatch = new CountDownLatch(threadCount);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);
			semaphoreAcquire(interruptSignal);
			try {
				futures = startDownload(stopLatch, doneLatch);
				doneLatch.await();
				interruptSignal.release();
			} catch (IOException | InterruptedException e) {
				cancelFutures(futures);
				countDownAwait(stopLatch);
				interruptSignal.release();
				es.shutdown();
				return;
			}
			mergeFiles();
			isComplete = true;
			es.shutdown();
		};
	}

	private void resetExecutor() {
		if (!Util.isNull(es))
			es.shutdownNow();
		es = Executors.newCachedThreadPool();
	}

	private List<Future<?>> startDownload(CountDownLatch stopLatch, CountDownLatch doneLatch) throws IOException {
		List<Future<?>> futures;
		Objects.requireNonNull(info);
		Util.createDirectory(directory);
		long contentLength = info.getContentLength();
		if (threadCount > 1 && info.isPartial() && contentLength >= threadCount) {
			futures = multiWorker(contentLength, stopLatch, doneLatch);
		} else {
			futures = singleWorker(stopLatch, doneLatch);
		}
		return futures;
	}

	private List<Future<?>> multiWorker(long contentLength, CountDownLatch stopLatch, CountDownLatch doneLatch) throws IOException {
		Util.createDirectory(tmpDirectory);
		List<Future<?>> futures = new ArrayList<>();
		tmpPaths = new ArrayList<>(threadCount);
		long size = contentLength / threadCount;
		long end = -1;
		long begin;
		for (int i = 0; i < threadCount; i++) {
			Path path = getFilePath(tmpDirectory.toAbsolutePath(), info.getName() + i);
			tmpPaths.add(path);
			File file = path.toFile();
			begin = end + 1;
			end = chooseEnd(contentLength, size, begin, i);
			begin = chooseBegin(file, begin, file.length() + begin);
			if (begin >= end) {
				evilCountDown(stopLatch, doneLatch);
				continue;
			}
			Worker worker = factory.getWorker(stopLatch, doneLatch, path, begin, end);
			Future<?> future = es.submit(worker);
			futures.add(future);
		}
		return futures;
	}

	private List<Future<?>> singleWorker(CountDownLatch stopLatch, CountDownLatch doneLatch) {
		List<Future<?>> futures = new ArrayList<>();
		Path path;
		path = getFinalFilePath();
		File file = path.toFile();
		long begin = chooseBegin(file, 0, file.length());
		if (begin >= info.getContentLength()) {
			evilCountDown(stopLatch, doneLatch);
			return futures;
		}
		Worker worker = factory.getWorker(stopLatch, doneLatch, path, begin);
		Future<?> future = es.submit(worker);
		futures.add(future);
		return futures;
	}

	private void mergeFiles() {
		if (!Util.isCollectionNullOrEmpty(tmpPaths)) {
			try {
				Path path = getFinalFilePath();
				try (FileOutputStream fos = new FileOutputStream(path.toFile(), false)) {
					for (Path tmpPath : tmpPaths) {
						if (tmpPath.toFile().exists())
							Files.copy(tmpPath, fos);
					}
				}
				for (Path tmpPath : tmpPaths) {
					Files.delete(tmpPath);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private Path getFinalFilePath() {
		Path path;
		if (Util.isStringNullOrEmpty(fileName))
			path = getFilePath(directory.toAbsolutePath(), info.getName());
		else
			path = getFilePath(directory.toAbsolutePath(), fileName);
		return path;
	}

	private Path getFilePath(Path path, String name) {
		return Paths.get(path.toString(), name);
	}

	private void countDownAwait(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private void semaphoreAcquire(Semaphore semaphore) {
		try {
			semaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void cancelFutures(List<Future<?>> futures) {
		for (Future<?> future : futures) {
			future.cancel(true);
		}
	}

	private void evilCountDown(CountDownLatch stopLatch, CountDownLatch doneLatch) {
		stopLatch.countDown();
		doneLatch.countDown();
	}

	private long chooseBegin(File file, long oldValue, long newValue) {
		return (file.exists() && !file.isDirectory()) ? newValue : oldValue;
	}

	private long chooseEnd(long contentLength, long size, long begin, int counter) {
		return (counter == (threadCount - 1)) ? contentLength : (begin + size);
	}


	public static class Builder {
		private URL url;
		private String fileName;
		private Path directory;
		private Path tmpDirectory;
		private int threadCount = 1;
		private String userAgent;
		private DownloadListener listener;

		/**
		 * Use this class to build download object.
		 *
		 * @param url download file url.
		 */
		public Builder(URL url) {
			this.url = url;
		}

		/**
		 * Default download file name will be overridden.
		 *
		 * @param name file name.
		 * @return Builder
		 */
		public Builder fileName(String name) {
			this.fileName = name;
			return this;
		}

		/**
		 * Directory where downloaded file will be stored.
		 *
		 * @param directory Download file directory.
		 * @return Builder
		 */
		public Builder directory(Path directory) {
			this.directory = directory;
			return this;
		}

		/**
		 * Number of threads to download.
		 * Default value is 1.
		 * Falls back to 1 if provided value is 0 or negative or if server doesn't support partial download.
		 *
		 * @param threadCount Number of threads to use while downloading.
		 * @return Builder
		 */
		public Builder threadCount(int threadCount) {
			if (threadCount > 1)
				this.threadCount = threadCount;
			return this;
		}

		/**
		 * Custom user-agent string.
		 *
		 * @param userAgent User-agent browser string.
		 * @return Builder
		 */
		public Builder userAgent(String userAgent) {
			this.userAgent = userAgent;
			return this;
		}

		/**
		 * Useful when threadCount > 1.
		 *
		 * @param tmpDirectory Temporary directory where multipart download files will be stored.
		 * @return Builder
		 */
		public Builder tmpDirectory(Path tmpDirectory) {
			this.tmpDirectory = tmpDirectory;
			return this;
		}

		/**
		 * Subscribed DownloadListener will get continuous update on how many bytes are being downloaded.
		 *
		 * @param listener DownloadListener implementor.
		 * @return Builder
		 */
		public Builder listener(DownloadListener listener) {
			this.listener = listener;
			return this;
		}

		/**
		 * The download object.
		 *
		 * @return Download
		 */
		public Download build() {
			if (Util.isNull(url)) {
				// :)
				return null;
			}
			if (Util.isStringNullOrEmpty(userAgent)) {
				userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
						"(KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
			}
			if (Util.isNull(directory)) {
				directory = Paths.get("");
			}
			if (Util.isNull(tmpDirectory)) {
				tmpDirectory = directory;
			}
			Download download = new Download();
			download.fileName = fileName;
			download.listener = listener;
			download.directory = directory;
			download.tmpDirectory = tmpDirectory;
			download.threadCount = threadCount;
			download.info = new HttpInfo(url, userAgent);
			download.interruptSignal = new Semaphore(1);
			download.factory = new WorkerFactory(download, download.info);
			return download;
		}
	}
}
