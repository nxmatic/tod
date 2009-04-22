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

public class SnakeBodyRing extends FreeEntity implements ISnakePart
{
	private static final float BASE_RADIUS = 10;
	private static final Color COLOR = Color.BLUE;
	
	/**
	 * Rank of this ring in the body
	 */
	private int itsRank;
	
	private float itsRadius = BASE_RADIUS;
	
	private Spring itsHeadSpring;
	private Spring itsTailSpring = new Spring(this, BASE_RADIUS, SPRING_K);

	public SnakeBodyRing(Universe aUniverse, int aRank)
	{
		super(aUniverse);
		itsRank = aRank;
	}
	
	public Spring getTailSpring()
	{
		return itsTailSpring;
	}
	
	public void connect(ISnakePart aSnakePart)
	{
		aSnakePart.getTailSpring().setEntity2(this, itsRadius);
		itsHeadSpring = aSnakePart.getTailSpring();
	}
	
	@Override
	public void updatePosition()
	{
		if (isLastRing())
		{
			float w = getUniverse().getEnnemyWeightAt(getPos(), itsRadius+2f);
			itsRadius -= w*0.01f;
			
			if (itsRadius < 1f) getUniverse().ringLost(this);
			
			itsRadius = Math.min(BASE_RADIUS, itsRadius+0.01f); 
		}
		super.updatePosition();
	}
	
	protected boolean isLastRing()
	{
		return itsRank == getUniverse().getSnakeSize();
	}

	@Override
	public UVector getForceFor(String aField, Entity e)
	{
		if ("matter".equals(aField))
		{
			return ForceUtils.repell(getPos(), itsRadius, e.getPos());
		}
		else if ("snakeTail".equals(aField) && isLastRing())
		{
			UVector v = UVector.create(getPos(), e.getPos());
			float norm = v.norm();
			float d = norm-itsRadius;
			float sgn = d/Math.abs(d);
			return norm > itsRadius*5 ?
					v.mult(-100f*sgn/(float) Math.pow(norm, 2f))
					: v.mult(-1f*sgn/norm);
		}
		else return super.getForceFor(aField, e);
	}

	@Override
	protected UVector getForces()
	{
		return UVector.sum(
				getUniverse().getForce("matter", this),
//				getUniverse().getForce(itsPrevious, this),
				itsHeadSpring != null ? itsHeadSpring.getForce2() : UVector.NULL,
//				itsTailSpring.getForce1(),
				viscosity(0.85f)
				);
	}

	@Override
	public void draw(Graphics2D g)
	{
		UPoint p = getPos();
		DrawUtils.drawBall(g, p, itsRadius, COLOR);
	}
	
	
}
