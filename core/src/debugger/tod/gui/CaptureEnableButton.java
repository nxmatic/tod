/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.gui;

import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import zz.utils.properties.IProperty;
import zz.utils.properties.IPropertyListener;
import zz.utils.properties.IRWProperty;
import zz.utils.ui.UIUtils;

/**
 * A toggle button that handles the capture enable property.
 * its particularity is that when the property has a null value,
 * the button gets disabled.
 * @author gpothier
 */
public class CaptureEnableButton extends JToggleButton
implements IPropertyListener<Boolean>, ChangeListener
{
	private IRWProperty<Boolean> itsCaptureEnableProperty;
	private boolean itsUpdating = false;

	public CaptureEnableButton()
	{
		super(Resources.ICON_CAPTUREENABLED.asIcon(20));
		setMargin(UIUtils.NULL_INSETS);
		addChangeListener(this);
	}
	
	public void setCaptureEnableProperty(IRWProperty<Boolean> aCaptureEnableProperty)
	{
		if (itsCaptureEnableProperty != null) itsCaptureEnableProperty.removeListener(this);
		itsCaptureEnableProperty = aCaptureEnableProperty;
		if (itsCaptureEnableProperty != null) itsCaptureEnableProperty.addListener(this);
		updateState();
	}
	
	private Boolean getValue()
	{
		return itsCaptureEnableProperty != null ? itsCaptureEnableProperty.get() : null;
	}
	
	protected void updateState()
	{
		itsUpdating = true;
		Boolean theValue = getValue();
		if (theValue == null)
		{
			setEnabled(false);
		}
		else
		{
			setEnabled(true);
			setToolTipText(theValue ? "Disable trace capture" : "Enable trace capture");
			setSelected(theValue);
		}
		itsUpdating = false;
	}

	public void stateChanged(ChangeEvent aE)
	{
		if (itsUpdating || itsCaptureEnableProperty == null) return;
		if (itsCaptureEnableProperty.get() != null) 
			itsCaptureEnableProperty.set(isSelected());
	}

	public void propertyChanged(IProperty<Boolean> aProperty, Boolean aOldValue, Boolean aNewValue)
	{
		updateState();
	}

	public void propertyValueChanged(IProperty<Boolean> aProperty)
	{
	}
}
