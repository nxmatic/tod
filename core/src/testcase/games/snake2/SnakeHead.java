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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SnakeHead extends FreeEntity implements ISnakePart
{
	private static final float RADIUS = 15;
	private static final Color COLOR = Color.GREEN;
	
	private float itsDesiredSpeed = 0;
	private UVector itsDirection = UVector.NULL;
//	private LinkedList<UPoint> itsObjectives = new LinkedList<UPoint>();
	private UPoint itsObjective;

	private boolean itsLeft = false;
	private boolean itsRight = false;
	
	private Spring itsSpring = new Spring(this, RADIUS, SPRING_K);
	
	
	public SnakeHead(Universe aUniverse)
	{
		super(aUniverse);
		getUniverse().addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent event)
			{
				switch(event.getKeyCode())
				{
				case KeyEvent.VK_LEFT:
					itsLeft = true;
					break;
					
				case KeyEvent.VK_RIGHT:
					itsRight = true;
					break;
					
				case KeyEvent.VK_UP:
					itsDesiredSpeed = 3;
					break;
					
				case KeyEvent.VK_DOWN:
					itsDesiredSpeed = 0;
					break;
				}
			}
			
			@Override
			public void keyReleased(KeyEvent event)
			{
				switch(event.getKeyCode())
				{
				case KeyEvent.VK_LEFT:
					itsLeft = false;
					break;
					
				case KeyEvent.VK_RIGHT:
					itsRight = false;
					break;
				}
			}
		});
		
		getUniverse().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
//				itsObjectives.add(new UPoint(event.getX(), event.getY()));
				itsObjective = new UPoint(event.getX(), event.getY());
				itsDesiredSpeed = event.getClickCount()+3;
			}
		});
	}
	
	public Spring getTailSpring()
	{
		return itsSpring;
	}

	public void updatePosition()
	{
//		itsDesiredSpeed = itsObjectives.size() * 1f;
//		while (itsObjectives.size() > 0)
//		{
//			UPoint theObjective = itsObjectives.getFirst();
//			itsDirection = UVector.create(getPos(), theObjective);
//			if (itsDirection.normSq() < RADIUS*RADIUS) itsObjectives.removeFirst();
//			else break;
//		}
//		if (itsObjectives.size() == 0) 
//		{
//			itsDesiredSpeed = 0; // Absence of this line causes a bug 
//			itsDirection = UVector.NULL;
//		}
		
		if (itsObjective != null) itsDirection = UVector.create(getPos(), itsObjective);
		else itsDesiredSpeed = 0;
			
		if (itsDirection.normSq() < RADIUS*RADIUS) itsObjective = null;
		
		Apple theAppleAt = getUniverse().getAppleAt(getPos(), RADIUS);
		if (theAppleAt != null)
		{
			theAppleAt.eat();
		}
		
		super.updatePosition();
	}
	
	@Override
	protected UVector getForces()
	{
		UVector f;
		if (itsDesiredSpeed > 0)
		{
			UVector ds = itsDirection.unit().mult(getMass()*itsDesiredSpeed);
			UVector s = getSpeed();
			f = new UVector(ds.dx-s.dx, ds.dy-s.dy);
			float norm = f.norm();
			float mn = 1f;
			if (norm > mn) f = f.mult(mn/norm);
		}
		else f = UVector.NULL;
			

		return UVector.sum(
				getUniverse().getForce("matter", this).mult(5),
				f,
				viscosity(0.1f));
	}


	@Override
	public UVector getForceFor(String aField, Entity e)
	{
		if ("matter".equals(aField))
		{
			return ForceUtils.repell(getPos(), RADIUS, e.getPos());
		}
		else return super.getForceFor(aField, e);
	}

	@Override
	public void draw(Graphics2D g)
	{
		UPoint p = getPos();
		g.setColor(COLOR);
		g.fill(new Ellipse2D.Float(p.x-RADIUS, p.y-RADIUS, RADIUS*2, RADIUS*2));
	}

}
