// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.cli.processing.help;

import static com.braintribe.console.ConsoleOutputs.text;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import com.braintribe.console.output.ConsoleOutput;

public class BlockFormatter {

	private String indent = "";
	private int firstLineConsumed;
	private int width = 80;

	public BlockFormatter setWidth(int width) {
		this.width = width;
		return this;
	}

	public BlockFormatter setIndent(String indent) {
		this.indent = indent;
		return this;
	}

	public BlockFormatter setIndent(int indent) {
		char[] padChars = new char[indent];
		Arrays.fill(padChars, ' ');
		this.indent = new String(padChars);
		return this;
	}

	public BlockFormatter setFirstLineConsumed(int firstLineConsumed) {
		this.firstLineConsumed = firstLineConsumed;
		return this;
	}

	public ConsoleOutput writeOutput(String text) {
		return text(formatAsBuilder(text));
	}

	public StringBuilder formatAsBuilder(String text) {
		StringBuilder builder = new StringBuilder();
		format(builder, text);
		return builder;
	}

	public void format(Appendable builder, String description) {
		try {
			_format(builder, description);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private enum TokenType {
		whitespace,
		text,
		linebreak,
	}

	private static class Token {
		TokenType type;
		StringBuilder text = new StringBuilder();

		Token(TokenType type) {
			this.type = type;
		}
	}

	private void _format(Appendable appendable, String description) throws IOException {
		new BlockFormatterRun(appendable, description).run();
	}

	private class BlockFormatterRun {

		private final Appendable appendable;
		private final String description;

		private final int charsPerRowAfterIndex = width - indent.length();

		private int charsOnCurrentLine;
		private int wordsOnCurrentLine;

		public BlockFormatterRun(Appendable appendable, String description) {
			this.appendable = appendable;
			this.description = description;
		}

		public void run() throws IOException {
			Deque<Token> tokens = new TokenParser().parseTokens(description);

			int missingFirstLineIndent = indent.length() - firstLineConsumed;

			if (missingFirstLineIndent > 0) {
				String missingIndent = indent.substring(0 + firstLineConsumed);
				appendable.append(missingIndent);
			} else if (missingFirstLineIndent < 0) {
				charsOnCurrentLine = -missingFirstLineIndent;
			}

			firstLineConsumed = 0;

			while (!tokens.isEmpty()) {
				Token token = tokens.pop();

				if (token.type == TokenType.linebreak) {
					printNewLine();
					continue;
				}

				String word = token.text.toString();

				charsOnCurrentLine += word.length();

				if (charsOnCurrentLine > charsPerRowAfterIndex && charsOnCurrentLine > word.length()) {
					printNewLine();
					charsOnCurrentLine = word.length();

					if (word.length() > charsPerRowAfterIndex) {
						String trimmedWord = word.substring(0, charsPerRowAfterIndex);
						String hangover = word.substring(charsPerRowAfterIndex);
						Token hangoverToken = new Token(token.type);
						hangoverToken.text.append(hangover);
						tokens.push(hangoverToken);
						word = trimmedWord;
					}
				}

				if (wordsOnCurrentLine == 0 && token.type == TokenType.whitespace) {
					word = word.substring(1);
				}

				appendable.append(word);

				wordsOnCurrentLine++;
			}

			appendable.append("\n");
		}

		private void printNewLine() throws IOException {
			charsOnCurrentLine = 0;
			wordsOnCurrentLine = 0;
			appendable.append('\n');
			appendable.append(indent);
		}

	}

	private static class TokenParser {
		private Token token = null;
		private final Deque<Token> tokens = new ArrayDeque<>();

		public Deque<Token> parseTokens(String text) {
			int length = text.length();

			for (int i = 0; i < length; i++) {
				char c = text.charAt(i);
				final TokenType type;
				switch (c) {
					case ' ':
						type = TokenType.whitespace;
						break;
					case '\n':
						type = TokenType.linebreak;
						break;
					default:
						type = TokenType.text;
						break;
				}

				Token token = acquireToken(type);
				token.text.append(c);
			}
			return tokens;
		}

		private Token acquireToken(TokenType type) {
			if (token == null || token.type != type || token.type == TokenType.linebreak)
				tokens.addLast(token = new Token(type));

			return token;
		}

	}

	public static void main(String[] args) {
		BlockFormatter formatter = new BlockFormatter();
		formatter.setIndent(20);
		StringBuilder s = formatter.formatAsBuilder(
				"Hello World, this   is a text to be tested by the block formatter to be wrapped correctly when it exceeds certain line length and which respects indents."
						+ " \n\n This should be a separate line. \n\n Wir probieren einfach auch mal ein ganz langes Wort wie zum Beispiel: "
						+ "Donaudampfschifffahrtsgesellschaftskapitänskajütenjungenschulausflugsbushaltestellenmülleimerfabrik.");
		System.out.println(s);
	}

}
