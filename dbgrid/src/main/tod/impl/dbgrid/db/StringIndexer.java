/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.dbgrid.db;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

import tod.core.config.TODConfig;
import tod.impl.database.IBidiIterator;
import tod.impl.dbgrid.dispatch.RINodeConnector.StringSearchHit;
import tod.utils.TODUtils;
import zz.utils.Utils;

public class StringIndexer
{
	private static final String FIELD_ID = "id";

	private static final String FIELD_VALUE = "value";

	private final TODConfig itsConfig;

	private final File itsFile;

	/**
	 * Lucene index writer for string indexing
	 */
	private IndexWriter itsIndexWriter;

	private Analyzer itsAnalyzer;

	private int itsIndexedDocumentNumber;

	private int itsOptimizedIndexedDocumentNumber;

	public StringIndexer(TODConfig aConfig, File aFile)
	{
		itsConfig = aConfig;
		itsFile = aFile;

		itsIndexWriter = null; // In case creation fails - we don't want a
		// stale index
		try
		{
			Utils.rmDir(itsFile);
			itsAnalyzer = new NeuneuAnalyzer();
			// itsAnalyzer = new SimpleAnalyzer();
			itsIndexWriter = new IndexWriter(itsFile, itsAnalyzer, true);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

	}

	public void register(long aId, String aString)
	{
		Document theDocument = new Document();
		theDocument.add(new Field(FIELD_ID, Long.toString(aId), Field.Store.YES, Field.Index.NO));

		theDocument.add(new Field(FIELD_VALUE, aString, Field.Store.NO, Field.Index.TOKENIZED));

		try
		{
			itsIndexWriter.addDocument(theDocument);
			TODUtils.logf(1, "Adding string: %s to string indexer ", aString);
			itsIndexedDocumentNumber++;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}

	}

	public HitIterator search(String aText)
	{
		try
		{
			if (itsIndexedDocumentNumber > 0)
			{
				// optimize only when a substantial change has been done (30%)
				if (itsOptimizedIndexedDocumentNumber * 100 / itsIndexedDocumentNumber < 70)
				{
					itsIndexWriter.optimize();
					itsOptimizedIndexedDocumentNumber = itsIndexedDocumentNumber;
				}
			}

			TODUtils.logf(0, "Searching for string '%s'  into %d terms ", aText, itsIndexWriter
					.docCount());

			// TODO check the need for closing the indexwriter
			// itsIndexWriter.close();

			IndexReader theReader = IndexReader.open(itsFile);
			Searcher theSearcher = new IndexSearcher(theReader);
			QueryParser theParser = new QueryParser(FIELD_VALUE, itsAnalyzer);
			Query theQuery = theParser.parse(aText);
			Hits theHits = theSearcher.search(theQuery);
			return new HitIterator(theHits);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static class HitIterator implements IBidiIterator<StringSearchHit>
	{
		private Hits itsHits;

		private int itsIndex;

		public HitIterator(Hits aHits)
		{
			itsHits = aHits;
		}

		private long getScore(float aLuceneScore)
		{
			return (long) (1000 - (aLuceneScore * 1000));
		}

		public boolean hasNext()
		{
			return itsIndex < itsHits.length();
		}

		public StringSearchHit next()
		{
			if (!hasNext()) throw new NoSuchElementException();

			try
			{
				Document theDoc = itsHits.doc(itsIndex);
				StringSearchHit theHit =
						new StringSearchHit(Long.parseLong(theDoc.get("id")), getScore(itsHits
								.score(itsIndex)));

				itsIndex++;
				return theHit;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		public boolean hasPrevious()
		{
			return itsIndex > 0;
		}

		public StringSearchHit previous()
		{
			if (!hasPrevious()) throw new NoSuchElementException();
			try
			{
				--itsIndex;
				Document theDoc = itsHits.doc(itsIndex);
				StringSearchHit theHit =
						new StringSearchHit(Long.parseLong(theDoc.get(FIELD_ID)), getScore(itsHits
								.score(itsIndex)));

				return theHit;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		public StringSearchHit peekNext()
		{
			throw new UnsupportedOperationException();
		}

		public StringSearchHit peekPrevious()
		{
			throw new UnsupportedOperationException();
		}
	}

	private static class NeuneuAnalyzer extends Analyzer
	{
		@Override
		public TokenStream tokenStream(String aFieldName, Reader aReader)
		{
			return new NeuneuTokenizer(aReader);
			// return new DelegateTokenizer(new LowerCaseTokenizer(aReader));
		}
	}

	private static class DelegateTokenizer extends TokenStream
	{
		private Tokenizer itsTokenizer;

		public DelegateTokenizer(Tokenizer aTokenizer)
		{
			itsTokenizer = aTokenizer;
		}

		@Override
		public Token next() throws IOException
		{
			Token theToken = itsTokenizer.next();
			System.out.println(theToken);
			return theToken;
		}

	}

	/**
	 * This tokenizer splits strings at each change between letter and non
	 * letter. Inspired of {@link CharTokenizer}
	 * 
	 * @author gpothier
	 */
	private static class NeuneuTokenizer extends Tokenizer
	{
		private int itsOffset = 0;

		private StringBuilder itsBuilder = new StringBuilder();

		private boolean itsWasLetter = false;

		public NeuneuTokenizer(Reader input)
		{
			super(input);
		}

		/**
		 * 
		 * Returns true if the given char starts a new token
		 */
		protected boolean isNewToken(char c)
		{
			boolean theIsLetter = Character.isLetter(c);
			boolean theResult = theIsLetter != itsWasLetter;
			itsWasLetter = theIsLetter;
			return theResult;
		}

		/**
		 * Returns true if the given char is valid as part of a token. If not,
		 * it will not be included in the token and a new token will be started
		 */
		protected boolean isValid(char c)
		{
			return !Character.isWhitespace(c);
		}

		/**
		 * Called on each token character to normalize it before it is added to
		 * the token. The default implementation does nothing. Subclasses may
		 * use this to, e.g., lowercase tokens.
		 */
		protected char normalize(char c)
		{
			return Character.toLowerCase(c);
		}

		/**
		 * Returns the next token in the stream, or null at EOS.
		 */
		@Override
		public final Token next() throws IOException
		{
			String theToken = null;

			while (true)
			{
				int c = input.read();
				itsOffset++;
				if (c == -1)
				{
					if (itsBuilder != null && itsBuilder.length() > 0) theToken =
							itsBuilder.toString();
					itsBuilder = null;
					break;
				}

				char ch = (char) c;
				if (!isValid(ch))
				{
					if (itsBuilder.length() > 0)
					{
						theToken = itsBuilder.toString();
						itsBuilder.delete(0, itsBuilder.length());
						break;
					}
				}
				else if (isNewToken(ch))
				{
					if (itsBuilder.length() > 0)
					{
						theToken = itsBuilder.toString();
						itsBuilder.delete(0, itsBuilder.length());
						itsBuilder.append(normalize(ch));
						break;
					}
					else itsBuilder.append(normalize(ch));
				}
				else
				{
					itsBuilder.append(normalize(ch));
				}
			}

			if (theToken != null)
			{
				return new Token(theToken, itsOffset, itsOffset + theToken.length());
			}
			else return null;
		}
	}
}
