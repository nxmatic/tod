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
package tod.gui.kit.html;

import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import tod.gui.FontConfig;

public class HtmlDoc implements HyperlinkListener
{
	private HtmlComponent itsComponent;
	
	private HTMLEditorKit itsEditorKit;
	private HTMLDocument itsDocument;
	private HtmlBody itsRoot;
	private int itsCurrentId;
	
	private boolean itsUpdatePosted = false;
	private boolean itsUpToDate = true;
	
	private Map<String, IHyperlinkListener> itsHyperlinkListeners =
		new HashMap<String, IHyperlinkListener>();
	
	public HtmlDoc()
	{
		this(new HtmlBody());
	}
	
	public HtmlDoc(HtmlBody aRoot)
	{
		itsEditorKit = new HTMLEditorKit();
		itsRoot = aRoot;
		itsRoot.setDoc(this);
	}
	
	public HtmlComponent getComponent()
	{
		return itsComponent;
	}

	public void setComponent(HtmlComponent aComponent)
	{
		itsComponent = aComponent;
	}
	
	public HtmlBody getRoot()
	{
		return itsRoot;
	}

	public HTMLEditorKit getEditorKit()
	{
		return itsEditorKit;
	}

	public HTMLDocument createDocument()
	{
		try
		{
			itsDocument = (HTMLDocument) itsEditorKit.createDefaultDocument();
			StringBuilder theBuilder = new StringBuilder("<html>");
			if (itsRoot != null) itsRoot.render(theBuilder);
			theBuilder.append("</html>");
			itsEditorKit.read(new StringReader(theBuilder.toString()), itsDocument, 0);

			return itsDocument;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		catch (BadLocationException e)
		{
			throw new RuntimeException(e);
		}
	}

	public String createId()
	{
		return ""+(itsCurrentId++);
	}
	
	public void registerListener(String aId, IHyperlinkListener aListener)
	{
		itsHyperlinkListeners.put(aId, aListener);
	}
	
	public void unregisterListener(String aId)
	{
		itsHyperlinkListeners.remove(aId);
	}
	
	public synchronized void update(HtmlElement aElement)
	{
		if (! itsUpdatePosted)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					synchronized (HtmlDoc.this)
					{
						updateHtml();
						itsUpdatePosted = false;
					}
				}
			});
			itsUpdatePosted = true;
		}
	}
	
	public void updateHtml()
	{
		if (itsDocument == null) return;
		
		try
		{
			Element theElement = itsDocument.getElement(itsRoot.getId());
			
			StringBuilder theText = new StringBuilder();
			itsRoot.render(theText);

			itsDocument.setOuterHTML(theElement, theText.toString());
		}
		catch (BadLocationException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent aEvent)
	{
		if (aEvent.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
		
		String theId = aEvent.getDescription();
		IHyperlinkListener theListener = itsHyperlinkListeners.get(theId);
		if (theListener != null) theListener.traverse();
	}
	
	/**
	 * Creates a simple document whose content is the given string.
	 */
	public static HtmlDoc create(String aText)
	{
		return create(aText, FontConfig.NORMAL, Color.BLACK);
	}
	
	/**
	 * Creates a simple document whose content is the given string, 
	 * with the specified font size and color.
	 */
	public static HtmlDoc create(String aText, int aFontSize, Color aColor)
	{
		HtmlDoc theDoc = new HtmlDoc();
		HtmlBody theBody = theDoc.getRoot();
		
		theBody.add(HtmlText.create(aText, aFontSize, aColor));
		
		return theDoc;
	}
	
	public static HtmlDoc create(HtmlElement aElement)
	{
		assert aElement != null;
		if (aElement instanceof HtmlBody)
		{
			HtmlBody theBody = (HtmlBody) aElement;
			return new HtmlDoc(theBody);
		}
		else
		{
			HtmlBody theBody = new HtmlBody();
			theBody.add(aElement);
			return new HtmlDoc(theBody);
		}
	}
}
