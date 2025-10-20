// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.servlet.explorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.braintribe.utils.lcd.StringTools;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SymbolTranslationServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private ServletContext servletContext;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String compilationId = getCompilationId(req);

		String content = null;

		if (req.getMethod().equals("POST")) {
			Reader reader = new BufferedReader(new InputStreamReader(req.getInputStream(), "ISO-8859-1"));

			StringBuilder builder = new StringBuilder();

			char buffer[] = new char[1024];
			int charsRead = 0;
			while ((charsRead = reader.read(buffer)) != -1) {
				builder.append(buffer, 0, charsRead);
			}

			reader.close();

			content = builder.toString();
		} else if (req.getMethod().equals("GET")) {
			content = req.getParameter("symbols");
		} else {
			resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		int s = content.indexOf('[');
		int e = content.lastIndexOf(']');
		String commaSeparatedList = content.substring(s + 1, e);

		PrintWriter printWriter = resp.getWriter();
		if (commaSeparatedList.isEmpty()) {
			printWriter.print("{");
			printWriter.println();
			printWriter.println("}");
			return;
		}

		String names[] = commaSeparatedList.split(",");

		Set<String> nameSet = new HashSet<>();

		for (String name : names) {
			String trimmedStr = name.trim();
			String extractedStr = trimmedStr.substring(1, trimmedStr.length() - 1);
			nameSet.add(extractedStr);
		}

		FileLookup lookup = new FileLookup(getNormalizedSymbolFile(compilationId));

		printWriter.print("{");
		boolean first = true;
		for (String name : nameSet) {
			String line = lookup.findLine(name);

			if (line == null)
				continue;

			String parts[] = line.split(",");
			String jsName = parts[0].trim();
			String className = parts[1].trim();
			String memberName = parts[2].trim();
			int lineNumber = Integer.valueOf(parts[3].trim());

			if (nameSet.contains(jsName)) {
				if (first) {
					first = false;
					printWriter.println();
				} else {
					printWriter.println(',');
					printWriter.println();
				}
				printWriter.println("  \"" + jsName + "\": {");
				printWriter.println("     \"className\": \"" + className + "\",");
				printWriter.println("     \"memberName\": \"" + memberName + "\",");
				printWriter.println("     \"lineNumber\": " + lineNumber);
				printWriter.print("  }");
			}
		}
		resp.getWriter().println();
		resp.getWriter().println("}");

		lookup.close();
	}

	private String getCompilationId(HttpServletRequest req) {
		String compilationId = req.getParameter("compilationId");
		if (StringTools.isEmpty(compilationId) || //
				compilationId.contains("..") || //
				compilationId.contains("/") || //
				compilationId.contains("\\") //
		)
			throw new IllegalArgumentException("Invalid compilationId: " + compilationId);

		return compilationId;
	}

	protected synchronized File getNormalizedSymbolFile(String compilationId) throws IOException {
		String mergedSymbolsFileName = "mergedSymbols";

		// This doesn't work, as our servlet does not have a ResourceManager
		String realPath = servletContext.getRealPath("/WEB-INF/symbolMaps");
		if (realPath == null)
			realPath = "web-app/tribefire-explorer/WEB-INF/symbolMaps";

		File symbolMapFolder = new File(realPath);
		File mergedSymbolsFile = new File(symbolMapFolder, mergedSymbolsFileName);

		String preparedFileName = compilationId + ".preparedSimpleSymbolMap";
		File preparedFile = new File(symbolMapFolder, preparedFileName);

		if (preparedFile.exists()) {
			return preparedFile;
		} else {
			int maxLineLength = 0;
			Set<String> lines = new TreeSet<>();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mergedSymbolsFile), "ISO-8859-1"))) {
				String line = reader.readLine();
				String cid = String.valueOf(Arrays.asList(line.substring(1).trim().split(",")).indexOf(compilationId));

				while ((line = reader.readLine()) != null) {
					int lastCol = line.lastIndexOf(',');
					if (Arrays.asList(line.substring(lastCol + 1).trim().split(";")).contains(cid)) {
						line = line.substring(0, lastCol);
						maxLineLength = Math.max(line.length(), maxLineLength);
						lines.add(line);
					}
				}
			}

			try (Writer writer = new OutputStreamWriter(new FileOutputStream(preparedFile), "ISO-8859-1")) {
				boolean first = true;
				for (String outputLine : lines) {
					if (first) {
						first = false;
					} else {
						writer.write('\n');
					}
					writeNormalizedLine(writer, outputLine, maxLineLength);
				}
			}

			return preparedFile;
		}
	}

	private static void writeNormalizedLine(Writer writer, String line, int lineLength) throws IOException {
		writer.write(line);
		int paddingCount = lineLength - line.length();
		char padding[] = new char[paddingCount];
		Arrays.fill(padding, ' ');
		writer.write(padding);
	}
	@Override
	public void init(ServletConfig config) throws ServletException {
		servletContext = config.getServletContext();
		super.init(config);
	}

	private static class FileLookup {
		private final RandomAccessFile file;
		private final long startIndex;
		private final long endIndex;
		private long rowSize;

		public FileLookup(File fromFile) throws IOException {
			this.file = new RandomAccessFile(fromFile, "r");
			long size = file.length();

			while (true) {
				byte b = file.readByte();
				rowSize++;
				if (b == '\n')
					break;
			}
			long rowNum = size / rowSize;
			startIndex = 0;
			endIndex = rowNum - 1;
		}

		public String readLine(long index) throws IOException {
			file.seek(index * rowSize);
			byte[] lineBytes = new byte[(int) rowSize - 1];
			file.readFully(lineBytes);
			return new String(lineBytes, "ISO-8859-1");
		}

		public String findLine(String key) throws IOException {
			long s = startIndex;
			long e = endIndex;

			// String signature = key + ",";

			do {
				long pivot = s + (e - s) / 2;

				String line = readLine(pivot);

				String pivotKey = extractKey(line);

				int res = pivotKey.compareTo(key);

				if (res == 0) {
					return line;
				} else if (res < 0) {
					s = pivot + 1;
				} else {
					e = pivot - 1;
				}
			} while (s <= e);

			return null;
		}

		protected String extractKey(String line) {
			int index = line.indexOf(',');
			String key = line.substring(0, index);
			return key;
		}

		public void close() {
			try {
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}