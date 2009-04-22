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
package games.snake2;

public class ForceUtils
{
	public static final float G = 100;
	
	public static UVector gravity(UPoint center, float radius, float mass1, UPoint p, float mass2)
	{
		UVector v = UVector.create(p, center);
		float centerDist = v.norm();
		
		float r;
		if (centerDist < radius) r = -radius;
		else r = centerDist-radius;
		
		return v.mult(G*mass1*mass2/(Math.abs(r)*r*centerDist));
	}
	
	public static UVector repell(UPoint center, float radius, UPoint p)
	{
		UVector v = UVector.create(center, p);
		float norm = v.norm();
		if (norm > radius) return UVector.NULL;
		float f = Math.min(1f/norm, radius/2f);
		return v.mult(f);
	}
	
	public static UVector ballAttract(UPoint center, float radius, UPoint p)
	{
		UVector v = UVector.create(center, p);
		float norm = v.norm();
		float d = norm-radius;
		float sgn = d/Math.abs(d);
		return v.mult(-sgn/norm);
	}
	
}
