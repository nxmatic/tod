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

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * An abstract html element that can contain children elements.
 * @author gpothier
 */
public abstract class HtmlParentElement extends HtmlElement
{
	private List<HtmlElement> itsChildren = null;

	@Override
	public synchronized void setDoc(HtmlDoc aDoc)
	{
		super.setDoc(aDoc);
		if (itsChildren != null) for (HtmlElement theElement : itsChildren)
		{
			theElement.setDoc(aDoc);
		}
	}
	
	public void add(HtmlElement aElement)
	{
		assert aElement != null;
		if (itsChildren == null) itsChildren = new ArrayList<HtmlElement>();
		itsChildren.add(aElement);
		if (getDoc() != null)
		{
			aElement.setDoc(getDoc());
			update();
		}
	}
	
	public void remove(HtmlElement aElement)
	{
		itsChildren.remove(aElement);
		aElement.setDoc(null);
		update();
	}
	
	public void clear()
	{
		if (itsChildren != null) 
		{
			for (HtmlElement theElement : itsChildren) theElement.setDoc(null);
			itsChildren.clear();
			update();
		}
	}
	
	public void addText(String aText)
	{
		add(HtmlText.create(aText));
	}
	
	public void addBr()
	{
		add(new HtmlRaw("<br>"));
	}
	
	@Override
	public void render(StringBuilder aBuilder)
	{
		aBuilder.append('<');
		aBuilder.append(getTag());
		aBuilder.append(" id='");
		aBuilder.append(getId());
		aBuilder.append("' ");
		renderAttributes(aBuilder);
		aBuilder.append('>');
		renderChildren(aBuilder);
		aBuilder.append("</");
		aBuilder.append(getTag());
		aBuilder.append('>');
	}
	
	protected abstract String getTag();
	protected abstract void renderAttributes(StringBuilder aBuilder);
	
	
	protected void renderChildren(StringBuilder aBuilder)
	{
		// We don't use an iterator here to avoid concurrency issues.
		if (itsChildren != null) for (int i=0;i<itsChildren.size();i++)
		{
			HtmlElement theElement = itsChildren.get(i);
			theElement.render(aBuilder);
		}
	}
}
