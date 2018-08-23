package com.nabilanam.libdownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
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
public final class Download {

	private final int threadCount;
	private final Path directory;
	private final Path tmpDirectory;
	private final String fileName;
	private final Path filePath;
	private final HttpInfo httpInfo;
	private final Semaphore interrupt;
	private final DownloadListener listener;
	private Thread async;
	private boolean isComplete;
	private ExecutorService es;
	private List<Path> tmpPaths;

	private Download(int threadCount, Path directory, Path tmpDirectory, String fileName, Path filePath,
	                 HttpInfo httpInfo, Semaphore interrupt, DownloadListener listener) {
		this.threadCount = threadCount;
		this.directory = directory;
		this.tmpDirectory = tmpDirectory;
		this.fileName = fileName;
		this.filePath = filePath;
		this.httpInfo = httpInfo;
		this.interrupt = interrupt;
		this.listener = listener;
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
		interruptAcquire();
		interruptRelease();
	}

	/**
	 * Signal async download to stop.
	 */
	public void stopAsync() {
		async.interrupt();
	}

	synchronized void downloaded(int bytes) {
		if (!Util.isNull(listener)) listener.downloaded(bytes);
	}

	/**
	 * @return number of suggested threads
	 */
	public int getThreadCount() {
		return threadCount;
	}

	/**
	 * @return directory where download is saved
	 */
	public Path getDirectory() {
		return directory;
	}

	/**
	 * @return temporary directory
	 */
	public Path getTmpDirectory() {
		return tmpDirectory;
	}

	/**
	 * @return file name of this download
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Returns concrete file path
	 *
	 * @return filePath
	 */
	public Path getFilePath() {
		return filePath;
	}

	/**
	 * Get low level httpInfo for this download.
	 *
	 * @return DownloadInfo
	 */
	public HttpInfo getHttpInfo() {
		return httpInfo;
	}

	/**
	 * @return listener
	 */
	public DownloadListener getListener() {
		return listener;
	}

	/**
	 * Returns true if every steps to download are performed successfully.
	 *
	 * @return isComplete
	 */
	public boolean isComplete() {
		return isComplete;
	}

	private Runnable runnable() {
		return () -> {
			resetExecutor();

			List<Future<?>> futures = new ArrayList<>();
			CountDownLatch stopLatch = new CountDownLatch(threadCount);
			CountDownLatch doneLatch = new CountDownLatch(threadCount);
			interruptAcquire();

			try {
				futures = startDownload(stopLatch, doneLatch);
				doneLatch.await();
				interruptRelease();
			} catch (IOException | InterruptedException e) {
				cancelFutures(futures);
				countDownAwait(stopLatch);
				interruptRelease();
				es.shutdown();
				return;
			}

			mergeFiles();
			es.shutdown();
			isComplete = true;
		};
	}

	private void resetExecutor() {
		if (!Util.isNull(es))
			es.shutdownNow();
		es = Executors.newCachedThreadPool();
	}

	private void interruptAcquire() {
		try {
			interrupt.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void interruptRelease() {
		interrupt.release();
	}

	private List<Future<?>> startDownload(CountDownLatch stopLatch, CountDownLatch doneLatch) throws IOException {
		List<Future<?>> futures;
		Objects.requireNonNull(httpInfo);
		Util.createDirectory(directory);
		long contentLength = httpInfo.getContentLength();
		if (isMultiWorkerDownload()) {
			futures = multiWorker(contentLength, stopLatch, doneLatch);
		} else {
			futures = singleWorker(stopLatch, doneLatch);
		}
		return futures;
	}

	private boolean isMultiWorkerDownload() {
		return threadCount > 1;
	}

	private List<Future<?>> multiWorker(long contentLength, CountDownLatch stopLatch, CountDownLatch doneLatch) throws IOException {
		Util.createDirectory(tmpDirectory);
		List<Future<?>> futures = new ArrayList<>();
		tmpPaths = new ArrayList<>(threadCount);
		long size = contentLength / threadCount;
		long end = -1;
		long begin;
		for (int i = 0; i < threadCount; i++) {
			Path path = Paths.get(tmpDirectory.toAbsolutePath().toString(), fileName + i);
			tmpPaths.add(path);
			File file = path.toFile();
			begin = getBegin(file, end);
			end = getEnd(i, contentLength, size, end);
			if (isMultiWorkerFileAlreadyDownloaded(begin, end)) {
				evilCountDown(stopLatch, doneLatch);
				continue;
			}
			Worker worker = new Worker
					.Builder(httpInfo.getUrl(), path)
					.begin(begin)
					.end(end)
					.userAgent(httpInfo.getUserAgent())
					.stopLatch(stopLatch)
					.doneLatch(doneLatch)
					.download(this)
					.build();
			Future<?> future = es.submit(worker);
			futures.add(future);
		}
		return futures;
	}

	long getBegin(File file, long end) {
		return Util.isNonDirectoryFile(file) ? file.length() + end + 1 : end + 1;
	}

	long getEnd(int counter, long contentLength, long size, long end) {
		return (counter == (threadCount - 1)) ? contentLength : (end + size + 1);
	}

	boolean isMultiWorkerFileAlreadyDownloaded(long begin, long end) {
		return begin > end;
	}

	private void evilCountDown(CountDownLatch stopLatch, CountDownLatch doneLatch) {
		stopLatch.countDown();
		doneLatch.countDown();
	}

	private List<Future<?>> singleWorker(CountDownLatch stopLatch, CountDownLatch doneLatch) {
		List<Future<?>> futures = new ArrayList<>();
		File file = filePath.toFile();
		long begin = getBegin(file);
		long contentLength = httpInfo.getContentLength();
		if (isSingleWorkerFileAlreadyDownloaded(begin, contentLength)) {
			evilCountDown(stopLatch, doneLatch);
			return futures;
		}
		boolean append = true;
		if (contentLength == -1)
			append = false;
		Worker worker = new Worker
				.Builder(httpInfo.getUrl(), filePath)
				.begin(begin)
				.userAgent(httpInfo.getUserAgent())
				.stopLatch(stopLatch)
				.doneLatch(doneLatch)
				.download(this)
				.append(append)
				.build();
		Future<?> future = es.submit(worker);
		futures.add(future);
		return futures;
	}

	long getBegin(File file) {
		return Util.isNonDirectoryFile(file) ? file.length() : 0;
	}

	boolean isSingleWorkerFileAlreadyDownloaded(long begin, long contentLength) {
		return contentLength != -1 && begin > contentLength;
	}

	private void cancelFutures(List<Future<?>> futures) {
		for (Future<?> future : futures) {
			future.cancel(true);
		}
	}

	private void countDownAwait(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	private void mergeFiles() {
		if (!Util.isCollectionNullOrEmpty(tmpPaths)) {
			try {
				try (FileOutputStream fos = new FileOutputStream(filePath.toFile(), false)) {
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


	public static class Builder {
		private URL url;
		private String fileName;
		private Path directory;
		private Path filePath;
		private Path tmpDirectory;
		private int threadCount;
		private String userAgent;
		private HttpInfo httpInfo;
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
		 * Default value is 1 which is calling thread.
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
		 * @throws IOException if network is unavailable
		 */
		public Download build() throws IOException {
			if (Util.isNull(url))
				return null;
			initializeDefaults();
			Semaphore interrupt = new Semaphore(1);
			return new Download(threadCount,
					directory, tmpDirectory,
					fileName, filePath,
					httpInfo, interrupt, listener);
		}

		private void initializeDefaults() throws IOException {
			if (Util.isStringNullOrEmpty(userAgent))
				userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
						"(KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
			if (Util.isNull(httpInfo)) {
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				httpInfo = new HttpInfo(con, userAgent);
			}
			if (threadCount < 1 || !isMultiThreadDownload(httpInfo.getContentLength())) {
				threadCount = 1;
			}
			if (Util.isNull(directory))
				directory = Paths.get("").toAbsolutePath();
			if (Util.isNull(tmpDirectory))
				tmpDirectory = directory.toAbsolutePath();
			if (Util.isStringNullOrEmpty(fileName))
				fileName = httpInfo.getName();

			filePath = Paths.get(directory.toString(), fileName);
		}

		Builder httpInfo(HttpInfo info) {
			this.httpInfo = info;
			return this;
		}

		boolean isMultiThreadDownload(long contentLength) {
			return threadCount > 1 && httpInfo.isPartial() && contentLength >= threadCount;
		}
	}
}
