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
package tod.gui;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import zz.utils.ui.ResourceUtils;
import zz.utils.ui.ResourceUtils.ImageResource;

public class Resources
{
	public static final ImageResource ICON_FORWARD_STEP_INTO = loadIcon("forwardStepInto.png");
	public static final ImageResource ICON_BACKWARD_STEP_INTO = loadIcon("backwardStepInto.png");
	public static final ImageResource ICON_FORWARD_STEP_OVER = loadIcon("forwardStepOver.png");
	public static final ImageResource ICON_BACKWARD_STEP_OVER = loadIcon("backwardStepOver.png");
	public static final ImageResource ICON_STEP_OUT = loadIcon("stepOut.png");
		
	public static final ImageResource ICON_ROLE_ADVICE_EXECUTION = loadIcon("roleAdviceExecution.png");
	public static final ImageResource ICON_ROLE_ASPECT_INSTANCE_SELECTION = loadIcon("roleAspectInstanceSelection.png");
	public static final ImageResource ICON_ROLE_CONTEXT_EXPOSURE = loadIcon("roleContextExposure.png");
	public static final ImageResource ICON_ROLE_RESIDUE_EVALUATION = loadIcon("roleResidueEvaluation.png");
	
	public static final ImageResource ICON_FULL_OBLIVIOUSNESS = loadIcon("fullObliviousness.png");
	public static final ImageResource ICON_INTIMACY = loadIcon("intimacy.png");
	
	public static final ImageResource ICON_TRIANGLE_UP = loadIcon("triangle_up.png");
	public static final ImageResource ICON_TRIANGLE_DOWN = loadIcon("triangle_down.png");
	public static final ImageResource ICON_TRIANGLE_LEFT = loadIcon("triangle_left.png");
	public static final ImageResource ICON_TRIANGLE_RIGHT = loadIcon("triangle_right.png");
	
	public static final ImageResource ICON_ZOOMSCROLLMARKER = loadIcon("zoomScrollMarker.png");
	public static final ImageResource ICON_ROLESELECTEDMARKER = loadIcon("roleSelectedMarker.png");
	public static final ImageResource ICON_ROLEUNSELECTEDMARKER = loadIcon("roleUnselectedMarker.png");
	
	public static final ImageResource ICON_BOOKMARK = loadIcon("bookmark.png");
	public static final ImageResource ICON_CAPTUREENABLED = loadIcon("captureEnabled.png");
	
	private static ImageResource loadIcon (String aName)
	{
		return ResourceUtils.loadImageResource(Resources.class, aName);
	}
	
}
