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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

public class Bug extends FreeEntity 
{
	public static final float RADIUS = 2;
	private static final Color COLOR = Color.BLACK;
	
	private int itsMaxAge;
	private int itsAge;

	public Bug(Universe aUniverse)
	{
		super(aUniverse);
		itsMaxAge = 500 + (int) (500 * Math.random());
	}

	@Override
	public void updatePosition()
	{
		itsAge++;
		if (itsAge > itsMaxAge) getUniverse().bugDied(this);
		super.updatePosition();
	}
	
	@Override
	public UVector getForceFor(String aField, Entity e)
	{
		if ("bugRepell".equals(aField))
		{
			return ForceUtils.repell(getPos(), RADIUS*4, e.getPos());
		}
		return super.getForceFor(aField, e);
	}
	
	@Override
	protected UVector getForces()
	{
		return UVector.sum(
				getUniverse().getForce("matter", this),
				getUniverse().getForce("snakeTail", this),
				getUniverse().getForce("bugRepell", this),
				viscosity(0.85f));
	}

	@Override
	public float ennemyWeight()
	{
		return 1;
	}
	
	@Override
	public void draw(Graphics2D g)
	{
		UPoint p = getPos();
		g.setColor(COLOR);
		g.fill(new Ellipse2D.Float(p.x-RADIUS, p.y-RADIUS, RADIUS*2, RADIUS*2));
	}
}
