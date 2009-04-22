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

import java.awt.Graphics2D;

public abstract class Entity
{
	private Universe itsUniverse;
	private UPoint pos = UPoint.ORIGIN;
	private float mass = 1;

	
	public Entity(Universe aUniverse)
	{
		itsUniverse = aUniverse;
	}

	public Universe getUniverse()
	{
		return itsUniverse;
	}

	/**
	 * Moves this entity by the given vector.
	 */
	public void move(UVector v)
	{
		pos = pos.translate(v);
	}
	
	public UPoint getPos()
	{
		return pos;
	}

	public void setPos(UPoint aPos)
	{
		pos = aPos;
	}
	
	public float getMass()
	{
		return mass;
	}

	public void setMass(float aMass)
	{
		mass = aMass;
	}

	/**
	 * Returns the force contributed by this entity for the specified field
	 * at the specified position.
	 */
	public UVector getForceFor(String aField, Entity e)
	{
		return UVector.NULL;
	}
	
	public abstract void draw(Graphics2D g);
	
	public abstract void updatePosition();

	public float ennemyWeight()
	{
		return 0;
	}
}
