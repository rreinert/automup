package com.automup.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.automup.Constants;
import com.automup.util.Utils;
import com.automup.util.mxBase64;

@Controller
public class GraphEditorController {

	private static final Logger log = Logger.getLogger(GraphEditorController.class.getName());
	
	@RequestMapping(value = "/open", method = { RequestMethod.GET, RequestMethod.POST })
	public void open(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("open...");

		request.setCharacterEncoding("UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html; charset=UTF-8");

		OutputStream out = response.getOutputStream();
		String encoding = request.getHeader("Accept-Encoding");

		// Supports GZIP content encoding
		if (encoding != null && encoding.indexOf("gzip") >= 0) {
			response.setHeader("Content-Encoding", "gzip");
			out = new GZIPOutputStream(out);
		}

		PrintWriter writer = new PrintWriter(out);
		writer.println("<html>");
		writer.println("<head>");
		writer.println("</head>");
		writer.println("<body>");
		writer.println("<script type=\"text/javascript\">");

		try {
			if (request.getContentLength() < 50000) {
				Map<String, String> post = parseMultipartRequest(request);
				String xml = new String(post.get("upfile").getBytes(ENCODING), "UTF-8");
				String filename = post.get("filename");

				// Uses JavaScript to load the XML on the client-side
				writer.println("window.parent.openFile.setData(decodeURIComponent('" + encodeURIComponent(xml) + "'), '"
						+ filename + "');");
			} else {
				error(writer, "drawingTooLarge");
			}
		} catch (Exception e) {
			error(writer, "invalidOrMissingFile");
		}

		writer.println("</script>");
		writer.println("</body>");
		writer.println("</html>");

		writer.flush();
		writer.close();

	}
	
	@RequestMapping(value = "/save", method = { RequestMethod.GET, RequestMethod.POST })
	public void save(HttpServletRequest request, HttpServletResponse response) throws Exception {
		System.out.println("save...");
		
		handlePost(request, response);
	}

	public static void handlePost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException
	{
		if (request.getContentLength() < Constants.MAX_REQUEST_SIZE)
		{
			long t0 = System.currentTimeMillis();
			String mime = request.getParameter("mime");
			String filename = request.getParameter("filename");
			byte[] data = null;

			// Data in data param is base64 encoded and deflated
			String enc = request.getParameter("data");
			String xml = null;

			try
			{
				if (enc != null && enc.length() > 0)
				{
					// NOTE: Simulate is used on client-side so the value is double-encoded
					xml = Utils.inflate(mxBase64.decode(URLDecoder
							.decode(enc, Utils.CHARSET_FOR_URL_ENCODING)
							.getBytes()));
				}
				else
				{
					xml = request.getParameter("xml");
				}

				// Decoding is optional (no plain text values allowed here so %3C means encoded)
				if (xml != null && xml.startsWith("%3C"))
				{
					xml = URLDecoder.decode(xml,
							Utils.CHARSET_FOR_URL_ENCODING);
				}

				String binary = request.getParameter("binary");

				if (binary != null && binary.equals("1") && xml != null
						&& (mime != null || filename != null))
				{
					response.setStatus(HttpServletResponse.SC_OK);

					if (filename != null)
					{
						filename = validateFilename(filename);

						response.setContentType("application/x-unknown");
						response.setHeader("Content-Disposition",
								"attachment; filename=\"" + filename
										+ "\"; filename*=UTF-8''" + filename);
					}
					else if (mime != null)
					{
						response.setContentType(mime);
					}

					response.getOutputStream()
							.write(mxBase64.decodeFast(URLDecoder.decode(xml,
									Utils.CHARSET_FOR_URL_ENCODING)));
				}
				else if (xml != null)
				{
					data = xml.getBytes(Utils.CHARSET_FOR_URL_ENCODING);
					String format = request.getParameter("format");

					if (format == null)
					{
						format = "xml";
					}

					if (filename != null && filename.length() > 0
							&& !filename.toLowerCase().endsWith(".svg")
							&& !filename.toLowerCase().endsWith(".html")
							&& !filename.toLowerCase().endsWith(".png")
							&& !filename.toLowerCase().endsWith("." + format))
					{
						filename += "." + format;
					}

					response.setStatus(HttpServletResponse.SC_OK);

					if (filename != null)
					{
						filename = validateFilename(filename);

						if (mime != null)
						{
							response.setContentType(mime);
						}
						else
						{
							response.setContentType("application/x-unknown");
						}

						response.setHeader("Content-Disposition",
								"attachment; filename=\"" + filename
										+ "\"; filename*=UTF-8''" + filename);
					}
					else if (mime != null && mime.equals("image/svg+xml"))
					{
						response.setContentType("image/svg+xml");
					}
					else
					{
						// Required to avoid download of file
						response.setContentType("text/plain");
					}

					OutputStream out = response.getOutputStream();
					out.write(data);
					out.close();
				}
				else
				{
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				}

				long mem = Runtime.getRuntime().totalMemory()
						- Runtime.getRuntime().freeMemory();

				log.fine("save: ip=" + request.getRemoteAddr() + " ref=\""
						+ request.getHeader("Referer") + "\" in="
						+ request.getContentLength() + " enc="
						+ ((enc != null) ? enc.length() : "[none]") + " xml="
						+ ((xml != null) ? xml.length() : "[none]") + " dt="
						+ request.getContentLength() + " mem=" + mem + " dt="
						+ (System.currentTimeMillis() - t0));
			}
			catch (OutOfMemoryError e)
			{
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
			catch (IllegalArgumentException e)
			{
				log.warning("Error parsing xml contents : " + xml
						+ System.getProperty("line.separator")
						+ "Original stack trace : " + e.getMessage());
			}
		}
		else
		{
			response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
		}
	}

	protected static String validateFilename(String filename)
	{
		// Only limited characters allowed
		try
		{
			filename = URLDecoder.decode(filename, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			// ignore unsupported encoding
		}
		
		filename = filename.replaceAll("[\\/:;*?\"<>|]", "");
		
		if (filename.length() == 0)
		{
			filename = "export.xml";
		}
		else if (!filename.toLowerCase().endsWith(".svg") &&
			!filename.toLowerCase().endsWith(".html") &&
			!filename.toLowerCase().endsWith(".xml") &&
			!filename.toLowerCase().endsWith(".png") &&
			!filename.toLowerCase().endsWith(".jpg") &&
			!filename.toLowerCase().endsWith(".pdf") &&
			!filename.toLowerCase().endsWith(".vsdx") &&
			!filename.toLowerCase().endsWith(".txt"))
		{
			filename = filename + ".xml";
		}
		
		filename = Utils.encodeURIComponent(filename, "UTF-8");
		
		return filename;
	}

	
	public static String encodeURIComponent(String s) {
		String result = null;

		try {
			result = URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!")
					.replaceAll("\\%28", "(").replaceAll("\\%29", ")").replaceAll("\\%7E", "~");
		}

		// This exception should never occur.
		catch (UnsupportedEncodingException e) {
			result = s;
		}

		return result;
	}

	/**
	 * Encoding for the multipart/form-data.
	 */
	protected static final String ENCODING = "ISO-8859-1";

	/**
	 * Parses the given multipart/form-data request into a map that maps from names
	 * to values. Note that this implementation ignores the file type and filename
	 * and does only return the actual data as the value for the name of the file
	 * input in the form. Returns an empty map if the form does not contain any
	 * multipart/form-data.
	 */
	protected Map<String, String> parseMultipartRequest(HttpServletRequest request) throws IOException {
		Map<String, String> result = new Hashtable<String, String>();
		String contentType = request.getHeader("Content-Type");

		// Checks if the form is of the correct content type
		if (contentType != null && contentType.indexOf("multipart/form-data") == 0) {
			// Extracts the boundary from the header
			int boundaryIndex = contentType.indexOf("boundary=");
			String boundary = "--" + contentType.substring(boundaryIndex + 9).trim();

			// Splits the multipart/form-data into its different parts
			Iterator<String> it = splitFormData(readStream(request.getInputStream()), boundary).iterator();

			while (it.hasNext()) {
				parsePart(it.next(), result);
			}
		}

		return result;
	}

	/**
	 * Parses the values in the given form-data part into the given map. The value
	 * of the name attribute will be used as the name for the data. The filename
	 * will be stored under filename in the given map and the content-type is
	 * ignored in this implementation.
	 */
	protected void parsePart(String part, Map<String, String> into) {
		String[] lines = part.split("\r\n");

		if (lines.length > 1) {
			// First line contains content-disposition in the following format:
			// form-data; name="upfile"; filename="avatar.jpg"
			String[] tokens = lines[1].split(";");

			// Stores the value of the name attribute for the form-data
			String name = null;

			for (int i = 0; i < tokens.length; i++) {
				String tmp = tokens[i];
				int index = tmp.indexOf("=");

				// Checks if the token contains a key=value pair
				if (index >= 0) {
					String key = tmp.substring(0, index).trim();
					String value = tmp.substring(index + 2, tmp.length() - 1);

					if (key.equals("name")) {
						name = value;
					} else {
						into.put(key, value);
					}
				}
			}

			// Parses all lines starting from the first empty line
			if (name != null && lines.length > 2) {
				boolean active = false;
				StringBuffer value = new StringBuffer();

				for (int i = 2; i < lines.length; i++) {
					if (active) {
						value.append(lines[i]);
					} else if (!active) {
						active = lines[i].length() == 0;
					}
				}

				into.put(name, value.toString());
			}
		}
	}

	/**
	 * Returns the parts of the given multipart/form-data.
	 */
	protected List<String> splitFormData(String formData, String boundary) {
		List<String> result = new LinkedList<String>();
		int nextBoundary = formData.indexOf(boundary);

		while (nextBoundary >= 0) {
			if (nextBoundary > 0) {
				result.add(formData.substring(0, nextBoundary));
			}

			formData = formData.substring(nextBoundary + boundary.length());
			nextBoundary = formData.indexOf(boundary);
		}

		return result;
	}

	/**
	 * Reads the complete stream into memory as a String.
	 */
	protected String readStream(InputStream is) throws IOException {
		if (is != null) {
			StringBuffer buffer = new StringBuffer();
			try {
				Reader in = new BufferedReader(new InputStreamReader(is, ENCODING));
				int ch;

				while ((ch = in.read()) > -1) {
					buffer.append((char) ch);
				}
			} finally {
				is.close();
			}

			return buffer.toString();
		} else {
			return "";
		}
	}

	public static void error(PrintWriter w, String key) {
		w.println("window.parent.openFile.error(window.parent.mxResources.get('" + key + "'));");
	}

}
