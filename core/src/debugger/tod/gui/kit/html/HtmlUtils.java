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

public class HtmlUtils
{
	/**
	 * Converts an awt {@link Color} to an html color string.
	 */
	public static String toString(Color aColor)
	{
		StringBuilder theBuilder = new StringBuilder('#');
		theBuilder.append(Integer.toHexString(aColor.getRed()+0x100).substring(1));
		theBuilder.append(Integer.toHexString(aColor.getGreen()+0x100).substring(1));
		theBuilder.append(Integer.toHexString(aColor.getBlue()+0x100).substring(1));
		
		return theBuilder.toString();
	}

	/**
		 * From http://www.rgagnon.com/javadetails/java-0306.html
		 */
		public static final String escapeHTML(String s)
		{
			StringBuilder sb = new StringBuilder();
			int n = s.length();
			for (int i=0;i<n;i++)
			{
				char c = s.charAt(i);
				switch (c)
				{
				case '<':
					sb.append("&lt;");
					break;
				case '>':
					sb.append("&gt;");
					break;
				case '&':
					sb.append("&amp;");
					break;
				case '"':
					sb.append("&quot;");
					break;
				case 'à':
					sb.append("&agrave;");
					break;
				case 'À':
					sb.append("&Agrave;");
					break;
				case 'â':
					sb.append("&acirc;");
					break;
				case 'Â':
					sb.append("&Acirc;");
					break;
				case 'ä':
					sb.append("&auml;");
					break;
				case 'Ä':
					sb.append("&Auml;");
					break;
				case 'å':
					sb.append("&aring;");
					break;
				case 'Å':
					sb.append("&Aring;");
					break;
				case 'æ':
					sb.append("&aelig;");
					break;
				case 'Æ':
					sb.append("&AElig;");
					break;
				case 'ç':
					sb.append("&ccedil;");
					break;
				case 'Ç':
					sb.append("&Ccedil;");
					break;
				case 'é':
					sb.append("&eacute;");
					break;
				case 'É':
					sb.append("&Eacute;");
					break;
				case 'è':
					sb.append("&egrave;");
					break;
				case 'È':
					sb.append("&Egrave;");
					break;
				case 'ê':
					sb.append("&ecirc;");
					break;
				case 'Ê':
					sb.append("&Ecirc;");
					break;
				case 'ë':
					sb.append("&euml;");
					break;
				case 'Ë':
					sb.append("&Euml;");
					break;
				case 'ï':
					sb.append("&iuml;");
					break;
				case 'Ï':
					sb.append("&Iuml;");
					break;
				case 'ô':
					sb.append("&ocirc;");
					break;
				case 'Ô':
					sb.append("&Ocirc;");
					break;
				case 'ö':
					sb.append("&ouml;");
					break;
				case 'Ö':
					sb.append("&Ouml;");
					break;
				case 'ø':
					sb.append("&oslash;");
					break;
				case 'Ø':
					sb.append("&Oslash;");
					break;
				case 'ß':
					sb.append("&szlig;");
					break;
				case 'ù':
					sb.append("&ugrave;");
					break;
				case 'Ù':
					sb.append("&Ugrave;");
					break;
				case 'û':
					sb.append("&ucirc;");
					break;
				case 'Û':
					sb.append("&Ucirc;");
					break;
				case 'ü':
					sb.append("&uuml;");
					break;
				case 'Ü':
					sb.append("&Uuml;");
					break;
				case '®':
					sb.append("&reg;");
					break;
				case '©':
					sb.append("&copy;");
					break;
				case '€':
					sb.append("&euro;");
					break;
				// be carefull with this one (non-breaking whitee space)
	//			case ' ':
	//				sb.append("&nbsp;");
	//				break;
	
				default:
					sb.append(c);
					break;
				}
			}
			return sb.toString();
		}
}
