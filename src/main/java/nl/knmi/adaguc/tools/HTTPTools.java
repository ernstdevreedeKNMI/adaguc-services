package nl.knmi.adaguc.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

public class HTTPTools {
	public static class InvalidHTTPKeyValueTokensException extends Exception {
		private static final long serialVersionUID = 1L;
		String message = null;

		public InvalidHTTPKeyValueTokensException(String result) {
			this.message= result;
		}

		public String getMessage() {
			return message;
		}
	}

	static byte[] validTokens = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
			'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '-', '|', '&',
			'.', ',', '~', ' ','/',':','?','_','#','=' ,'(',')',';','%','[',']'};
	/**
	 * Validates input for valid tokens, preventing XSS attacks. Throws Exception when invalid tokens are encountered.
	 * @param input The string as input
	 * @return returns the same string
	 * @throws Exception when invalid tokens are encountered
	 */
	public static String validateInputTokens(String input) throws InvalidHTTPKeyValueTokensException {
		if(input == null)return null;

		byte[] str = input.getBytes();
		for (int c = 0; c < str.length; c++) {
			boolean found = false;
			for (int v = 0; v < validTokens.length; v++) {
				if (validTokens[v] == str[c]) {
					found = true;
					break;
				}
			}
			if (found == false) {

				String message = "Invalid token given: '"
						+ Character.toString((char) str[c]) + "', code (" + str[c] + ").";
				Debug.errprintln("Invalid string given: " + message + " in string "+input);
				throw new InvalidHTTPKeyValueTokensException(message);
			}
		}
		return input;
	}

	/**
	 * Returns the value of a key, but does checking on valid tokens for XSS attacks and decodes the URL.
	 * @param request The HTTPServlet containing the KVP's
	 * @param name Name of the key
	 * @return The value of the key
	 * @throws Exception (UnsupportedEncoding and InvalidHTTPKeyValueTokensException)
	 */
	public static String getHTTPParam(HttpServletRequest request, String name)
			throws Exception {
		//String param = request.getParameter(name);

		Map<String, String[]> paramMap= request.getParameterMap();
		String [] value = null;
		for (Entry<String, String[]> entry : paramMap.entrySet()){
			String key = entry.getKey();
			if(key.equalsIgnoreCase(name)){
				value = entry.getValue();
				break;
			}
		}

		if(value==null||value[0]==null||value.length==0){
			throw new Exception("UnableFindParam " + name);
		}

		String paramValue = value[0];
		paramValue = URLDecoder.decode(paramValue, "UTF-8");
		paramValue = validateInputTokens(paramValue);
		return paramValue;
	}
	/**
	 * Returns a list of values of a key, but does checking on valid tokens for XSS attacks and decodes the URL.
	 * @param request The HTTPServlet containing the KVP's
	 * @param name Name of the key
	 * @return The values of the key in a String array
	 * @throws Exception (UnsupportedEncoding and InvalidHTTPKeyValueTokensException)
	 */
	public static String[] getHTTPParamList(HttpServletRequest request, String name)
			throws Exception {

		StringBuffer url=request.getRequestURL();
		String queryString = request.getQueryString();
		if (queryString!=null) {
			url.append('?').append(queryString);
		}
		List <String>values=getKVPList(url.toString(), name);
		
		if (values.size()==0){
			throw new Exception("UnableFindParam " + name);
		}

		return values.toArray(new String[0]);
	}
	
	/**
	   * Get values for a multiple keys with the same name in a URL, 
	   * e.g. ?variable=psl&variable=tas means: key="variable" value="psl,tas" (as list) 
	   * @param url The URL containging the KVP encoded data
	   * @param key The key we want to search for
	   * @return value, null if not found.
	 * @throws InvalidHTTPKeyValueTokensException 
	   * @throws Exception 
	   */
	  static public List<String> getKVPList(String url, String key) throws InvalidHTTPKeyValueTokensException {

	    String urlParts[] = url.split("\\?");
	    String queryString = urlParts[urlParts.length - 1];
	    List<String> values = new ArrayList<String>();
	    // System.out.println("*********QU"+queryString);
	    String[] kvpparts = queryString.split("&");
	    for (int j = 0; j < kvpparts.length; j++) {
	      // System.out.println("*********KV"+kvpparts[j]);
	      int firstEqualsSign = kvpparts[j].indexOf("=");
	      if(firstEqualsSign>=0){
	        String foundKey = kvpparts[j].substring(0, firstEqualsSign);
	        String foundValue = kvpparts[j].substring(firstEqualsSign+1);
	        if (foundKey.equalsIgnoreCase(key)){
	          String valueChecked = validateInputTokens(foundValue);
	          values.add(valueChecked);
	        }
	      }
	    }
	    return values;

	  }

	  /**
	   * Finds a KVP from the querystring. Returns null if not found.
	   * @param queryString
	   * @param string
	   * @return
	 * @throws InvalidHTTPKeyValueTokensException 
	   * @throws Exception
	   */
	  public static String getKVPItem(String queryString, String string) throws InvalidHTTPKeyValueTokensException {
	    List<String> items = getKVPList(queryString, string);
	    if (items.size() == 0)
	      return null;
	    return items.get(0);
	  }
	  

	  static public String makeCleanURL(String url) {
	    // DebugConsole.println("oldURL="+url);
	    if (url.length() == 0)
	      return url;
	    // Remove double && signs
	    String newURL = "";
	    String urlParts[] = url.split("\\?");
	    if (urlParts.length == 2) {
	      newURL = urlParts[0] + "?";
	    }
	    boolean requireAmp = false;
	    String queryString = urlParts[urlParts.length - 1];
	    // System.out.println("*********QU"+queryString);
	    String[] kvpparts = queryString.split("&");
	    for (int j = 0; j < kvpparts.length; j++) {
	      // System.out.println("*********KV"+kvpparts[j]);
	      String kvp[] = kvpparts[j].split("=");

	      if (kvp.length == 2) {
	        if (requireAmp)
	          newURL += "&";
	        newURL += kvp[0] + "=" + kvp[1];
	        requireAmp = true;
	      }
	      if (kvp.length == 1) {
	        if (kvp[0].length() != 0) {
	          newURL += kvp[0];
	          if (urlParts.length == 1 && j == 0) {
	            newURL += "?";
	          }
	        }
	      }
	    }
	    // return newURL;

	    try {
	      // DebugConsole.println("+newURL: "+newURL);
	      String rootCatalog = new URL(newURL).toString();
	      String path = new URL(rootCatalog).getFile();
	      String hostPath = rootCatalog.substring(0,
	          rootCatalog.length() - path.length());

	      // DebugConsole.println("Catalog: "+rootCatalog);
	      // DebugConsole.println("hostPath: "+hostPath);
	      path = path.replace("//", "/");

	      newURL = hostPath + path;
	      // DebugConsole.println("newURL: "+newURL);
	      // DebugConsole.println("/newURL: "+newURL);
	      return newURL;
	    } catch (MalformedURLException e) {
	      return newURL;
	    }

	  }

}
