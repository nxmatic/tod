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

/**
 * A vector in the universe
 * @author gpothier
 */
public class UVector
{
	public static final UVector NULL = new UVector(0, 0);
	
	public final float dx;
	public final float dy;
	
	public UVector(float aDx, float aDy)
	{
		dx = aDx;
		dy = aDy;
	}
	
	/**
	 * Returns the norm (length) of this vector.
	 */
	public float norm() 
	{
		return (float) Math.sqrt(normSq());
	}
	
	/**
	 * Returns the squared norm (length) of this vector.
	 */
	public float normSq() 
	{
		return dx*dx + dy*dy;
	}

	/**
	 * Returns a new vector with coordinates multiplied by k
	 */
	public UVector mult(float k)
	{
		return new UVector(k*dx, k*dy);
	}
	
	public UVector add(UVector v) 
	{
		return new UVector(dx+v.dx, dy+v.dy);
	}
	
	/**
	 * Returns a unit-length vector with the same direction as this one.
	 */
	public UVector unit()
	{
		return mult(1f/norm());
	}
	
	/**
	 * Creates the vector from p1 to p2.
	 */
	public static UVector create(UPoint p1, UPoint p2)
	{
		return new UVector(p2.x-p1.x, p2.y-p1.y);
	}
	
	public static UVector radial(float norm, float angle)
	{
		return new UVector(
				(float) (norm*Math.cos(angle)), 
				(float) (norm*Math.sin(angle)));
	}
	
	/**
	 * Returns the sum of all specified vectors.
	 */
	public static UVector sum(UVector... vects)
	{
		float dx = 0;
		float dy = 0;
		for (UVector v : vects)
		{
			dx += v.dx;
			dy += v.dy;
		}
		return new UVector(dx, dy);
	}
}
