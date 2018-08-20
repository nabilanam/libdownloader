# libdownloader
Simple and lightweight download library for Java.

## Features:
* Multithreading
* Auto resume

## How

Let's create a Download object.
```
Download download = new Download.Builder(url)
				.directory(Paths.get("My Downloads"))
				.threadCount(4)
				.listener(this)
				.build();
```
Let's go full async and wait for a sec.
```
download.startAsync();
Thread.sleep(1000);
```
Now stop this download.
```
download.stop();
```
Again start this download. This time it's blocking. It resumes!
```
download.start();
```
Previously we subscribed to the download object when we said ".listener(this)". Which required us to implement "DownloadListener" interface.
```
public class Application implements DownloadListener
```
By implenting we override a method and get download progress
```
private long downloaded = 0;

@Override
public void downloaded(int bytes) { 
  downloaded += bytes;
}
```
Lets print total amount of bytes we downloaded
```
System.out.println(downloaded); //mine is 2061457
```
