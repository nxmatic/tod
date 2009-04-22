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

import javax.swing.JComponent;

public abstract class HtmlElement
{
	private HtmlDoc itsDoc;
	private String itsId;

	protected JComponent getComponent()
	{
		return getDoc().getComponent();
	}
	
	public void setDoc(HtmlDoc aDoc)
	{
		itsDoc = aDoc;
		itsId = itsDoc != null ? itsDoc.createId() : null;
	}
	
	protected String getId()
	{
		return itsId;
	}

	public HtmlDoc getDoc()
	{
		return itsDoc;
	}
	
	/**
	 * Transforms this element into a string
	 */
	public abstract void render(StringBuilder aBuilder);
	
	/**
	 * Updates the document to reflect changes in this element
	 */
	protected void update()
	{
		HtmlDoc theDoc = getDoc();
		if (theDoc != null) theDoc.update(this);
	}
}
