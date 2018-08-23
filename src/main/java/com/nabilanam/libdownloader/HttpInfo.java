package com.nabilanam.libdownloader;


import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author nabil
 */
public class HttpInfo implements Serializable {

	private String userAgent;
	private String name;
	private String eTag;
	private boolean partial;
	private String contentType;
	private long contentLength;
	private String lastModified;
	private HttpURLConnection con;

	HttpInfo(HttpURLConnection con, String userAgent) {
		this.con = con;
		this.userAgent = userAgent;
		try {
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Range", "bytes=0-");
			con.setRequestProperty("User-Agent", userAgent);
			con.setRequestMethod("GET");
			String contentDisposition = con.getHeaderField("Content-Disposition");
			if (!Util.isNull(contentDisposition) && contentDisposition.contains("=")) {
				name = contentDisposition.split("=")[1].replaceAll("\"", "");
			} else {
				URL url = con.getURL();
				name = url.getPath().substring(url.getPath().lastIndexOf('/') + 1, url.getPath().length())
						.replaceAll("%20", " ");
			}
			con.connect();
			if (con.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
				partial = true;
			}
			eTag = con.getHeaderField("ETag");
			lastModified = con.getHeaderField("Last-Modified");
			contentType = con.getHeaderField("Content-Type");
			contentLength = con.getContentLengthLong();
			con.disconnect();
		} catch (IOException ex) {
			con.disconnect();
		}
	}

	public URL getUrl() {
		return con.getURL();
	}

	public String getName() {
		return name;
	}

	public String getETag() {
		return eTag;
	}

	public boolean isPartial() {
		return partial;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public String getContentType() {
		return contentType;
	}

	public long getContentLength() {
		return contentLength;
	}

	public String getLastModified() {
		return lastModified;
	}
}
