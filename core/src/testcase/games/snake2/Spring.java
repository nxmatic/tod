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

public class Spring
{
	private Entity itsEntity1;
	private float itsRadius1;

	private Entity itsEntity2;
	private float itsRadius2;
	
	private float itsK;

	public Spring(Entity aEntity1, float aRadius1, Entity aEntity2, float aRadius2, float k)
	{
		itsEntity1 = aEntity1;
		itsRadius1 = aRadius1;
		itsEntity2 = aEntity2;
		itsRadius2 = aRadius2;
		itsK = k;
	}
	
	public Spring(Entity aEntity1, float aRadius1, float aK)
	{
		itsEntity1 = aEntity1;
		itsRadius1 = aRadius1;
		itsK = aK;
	}

	public void setEntity2(Entity aEntity2, float aRadius2)
	{
		itsEntity2 = aEntity2;
		itsRadius2 = aRadius2;
	}

	public UVector getForce1()
	{
		if (itsEntity1 == null || itsEntity2 == null) return UVector.NULL;
		UVector v = UVector.create(itsEntity1.getPos(), itsEntity2.getPos());
		float norm = v.norm();
		final float D = 1f; 
		final float D3 = D*D*D;
		float d = norm - (itsRadius1+itsRadius2);
		float sgn = d > 0 ? 1f : -1f;
//		float f = Math.abs(d) > D ? d - (D-1)*sgn : d*d*d/D3;
		float f = d;
		return v.mult(itsK*f/norm);
	}
	
	public UVector getForce2()
	{
		return getForce1().mult(-1f);
	}
	
}
