package com.nabilanam.libdownloader;


import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author nabil
 */
public class HttpInfo implements Serializable {

	private URL url;
	private String name;
	private String eTag;
	private boolean partial;
	private String userAgent;
	private String contentType;
	private long contentLength;
	private String lastModified;

	HttpInfo(URL url, String userAgent) {
		HttpURLConnection con = null;
		try {
			this.url = url;
			this.userAgent = userAgent;
			con = (HttpURLConnection) url.openConnection();
			con.setInstanceFollowRedirects(true);
			con.setRequestProperty("Range", "bytes=0-");
			con.setRequestProperty("User-Agent", userAgent);
			con.setRequestMethod("GET");
			String contentDisposition = con.getHeaderField("Content-Disposition");
			if (!Util.isNull(contentDisposition) && contentDisposition.contains("=")) {
				name = contentDisposition.split("=")[1].replaceAll("\"", "");
			} else {
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
			if (!Util.isNull(con)) {
				con.disconnect();
			}
		}
	}

	public URL getUrl() {
		return url;
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
