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
package tod.gui.components.eventsequences;

import infovis.panel.dqinter.DoubleRangeSlider;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tod.agent.AgentUtils;
import tod.core.database.browser.ILogBrowser;
import tod.core.session.ISession;
import tod.gui.GUIUtils;
import tod.gui.IGUIManager;
import tod.gui.components.eventsequences.mural.AbstractMuralPainter;
import tod.gui.components.eventsequences.mural.EventMural;
import tod.utils.TODUtils;
import zz.utils.ItemAction;
import zz.utils.properties.ArrayListProperty;
import zz.utils.properties.IListProperty;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.PropertyUtils;
import zz.utils.properties.SimpleRWProperty;

/**
 * A component that displays a stack of event views.
 * @author gpothier
 */
public class SequenceViewsDock extends JPanel
{
	private IListProperty<IEventSequenceSeed> pSeeds = new ArrayListProperty<IEventSequenceSeed>(this)
	{
		@Override
		protected void elementAdded(int aIndex, IEventSequenceSeed aSeed)
		{
			// TODO: provide proper display
			IEventSequenceView theView = aSeed.createView(getGUIManager());
			itsViews.add(aIndex, theView);
			
			SequencePanel thePanel = new SequencePanel(theView);
			itsViewsPanel.add(thePanel, aIndex);
			itsViewsPanel.revalidate();
			itsViewsPanel.repaint();
			
			computeBounds();
		}
		
		@Override
		protected void elementRemoved(int aIndex, IEventSequenceSeed aSeed)
		{
			itsViews.remove(aIndex);
			
			itsViewsPanel.remove(aIndex);
			itsViewsPanel.revalidate();
			itsViewsPanel.repaint();
			
			computeBounds();
		}
	};
	
	private List<IEventSequenceView> itsViews = new ArrayList<IEventSequenceView>();
	
	private IRWProperty<Long> pStart = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			if (itsTimestampSlider != null && aNewValue != null) 
				itsTimestampSlider.setRangeStart(aNewValue);
		}
		
		@Override
		protected Object canChange(Long aOldValue, Long aNewValue)
		{
			if (aNewValue == null) return REJECT;
			if (aNewValue < itsFirstTimestamp) return itsFirstTimestamp;
			else return ACCEPT;
		}
	};
	
	private IRWProperty<Long> pEnd = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			if (itsTimestampSlider != null && aNewValue != null)
				itsTimestampSlider.setRangeEnd(aNewValue);
		}
		
		@Override
		protected Object canChange(Long aOldValue, Long aNewValue)
		{
			if (aNewValue == null) return REJECT;
			if (aNewValue > itsLastTimestamp) return itsLastTimestamp;
			else return ACCEPT;
		}
	};

	//slider with double handles defining the timestamp range in the murals
	private TimestampRangeSlider itsTimestampSlider;
	
	private JPanel itsViewsPanel;
	
	private final IGUIManager itsGUIManager;

	private long itsFirstTimestamp;
	private long itsLastTimestamp;
	
	/**
	 * An optional mural painter for the murals, to replace the default one.
	 */
	private AbstractMuralPainter itsMuralPainter;
	
	private int itsPreferredStripeHeight = 80;
	private boolean itsShowStripeTitle = true;
	
	public SequenceViewsDock(IGUIManager aGUIManager)
	{
		itsGUIManager = aGUIManager;
		createUI();
	}
	
	private void createUI()
	{
		setLayout(new BorderLayout());
		itsViewsPanel = new JPanel (GUIUtils.createStackLayout());
		
		JScrollPane theScrollPane = new JScrollPane(
				itsViewsPanel, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		add (theScrollPane, BorderLayout.CENTER);
		
		itsTimestampSlider = new TimestampRangeSlider();
		add(itsTimestampSlider, BorderLayout.NORTH);
		
		new Timer(500, new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				computeBounds();
			}
		}).start();
	}

	public IGUIManager getGUIManager()
	{
		return itsGUIManager;
	}

	public void setMuralPainter(AbstractMuralPainter aMuralPainter)
	{
		itsMuralPainter = aMuralPainter;
	}
	
	/**
	 * Returns the mural that corresponds to the seed at the specified index.
	 */
	public EventMural getMural(int aIndex)
	{
		SequencePanel thePanel = (SequencePanel) itsViewsPanel.getComponent(aIndex);
		return thePanel.getMural();
	}
	
	private void computeBounds()
	{
//		itsFirstTimestamp = Long.MAX_VALUE;
//		itsLastTimestamp = 0;
//		
//		for (IEventSequenceView theView : itsViews)
//		{
//			itsFirstTimestamp = Math.min(itsFirstTimestamp, theView.getFirstTimestamp());
//			itsLastTimestamp = Math.max(itsLastTimestamp, theView.getLastTimestamp());
//		}
		
		ISession theSession = itsGUIManager.getSession();
		if (theSession == null) return;
		
		ILogBrowser theBrowser = theSession.getLogBrowser();
		itsFirstTimestamp = theBrowser.getFirstTimestamp();
		itsLastTimestamp = theBrowser.getLastTimestamp();
		itsTimestampSlider.setLimits(itsFirstTimestamp, itsLastTimestamp);
		
		for (IEventSequenceView theView : itsViews) theView.setLimits(itsFirstTimestamp, itsLastTimestamp);
	}
	
	/**
	 * First timestamp of the events displayed in the all the sequences of this dock.
	 */
	public IRWProperty<Long> pStart ()
	{
		return pStart;
	}
	
	/**
	 * Last timestamp of the events displayed in the all the sequences of this dock.
	 */
	public IRWProperty<Long> pEnd ()
	{
		return pEnd;
	}
	
	/**
	 * The seeds whose views are displayed in this dock
	 */
	public IListProperty<IEventSequenceSeed> pSeeds()
	{
		return pSeeds;
	}

	public void setPreferredStripeHeight(int aPreferredStripeHeight)
	{
		itsPreferredStripeHeight = aPreferredStripeHeight;
	}
	
	public void setShowStripeTitle(boolean aShowStripeTitle)
	{
		itsShowStripeTitle = aShowStripeTitle;
	}
	
	private class SequencePanel extends JPanel
	{
		private IEventSequenceView itsView;
		private EventMural itsMural;
		
		/**
		 * We keep references to connectors so that we can disconnect.
		 */
		private PropertyUtils.Connector<Long>[] itsConnectors = new PropertyUtils.Connector[2];

		public SequencePanel(IEventSequenceView aView)
		{
			itsView = aView;
			createUI();
		}
		
		@Override
		public void addNotify()
		{
			super.addNotify();
			itsConnectors[0] = PropertyUtils.connect(pStart(), itsView.pStart(), true, true);
			itsConnectors[1] = PropertyUtils.connect(pEnd(), itsView.pEnd(), true, true);
		}
		
		@Override
		public void removeNotify()
		{
			super.removeNotify();
			itsConnectors[0].disconnect();
			itsConnectors[1].disconnect();
		}
		
		private void createUI()
		{
			setLayout(new BorderLayout(5, 0));
			setPreferredSize(new Dimension(10, itsPreferredStripeHeight));
			
			itsView.setLimits(itsFirstTimestamp, itsLastTimestamp);
			itsMural = itsView.getEventStripe();
			
			if (itsMuralPainter != null) itsMural.setMuralPainter(itsMuralPainter);
			
			add (itsMural, BorderLayout.CENTER);
			if (itsShowStripeTitle) add (createNorthPanel(), BorderLayout.NORTH);
		}
		
		public JPanel createNorthPanel()
		{
			JPanel thePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
			thePanel.add (new JLabel(itsView.getTitle()));
			
			JToolBar theToolBar = new JToolBar();
			theToolBar.setFloatable(false);
			for (ItemAction theAction : itsView.getActions())
			{
				theToolBar.add(theAction);
			}
			
			thePanel.add(theToolBar);
			
			return thePanel;
		}
		
		public EventMural getMural()
		{
			return itsMural;
		}
	}
	
	/**
	 * shows a range slider for timestamp 
	 * it displays the fist and last timestamps above the slider 
	 * and the first and last timestamps of the chosen range below the slider 
	 * @author omotelet
	 */
	private class TimestampRangeSlider extends JPanel implements ChangeListener{

		private DoubleRangeSlider itsSlider;
		
		private JLabel itsStartLabel = new JLabel();
		private JLabel itsEndLabel= new JLabel();
		private JLabel itsStartRangeLabel= new JLabel();
		private JLabel itsEndRangeLabel= new JLabel();
		
		private long itsFirstTimestamp;
		private long itsLastTimestamp;
		private long itsRangeStart;
		private long itsRangeEnd;
		
		private int itsChanging = 0;
		
		public TimestampRangeSlider()
		{
			itsSlider = new DoubleRangeSlider(0, 1, 0, 1){
				@Override
				public String getToolTipText()
				{
					return "Modify the range in the slider in order to zoom in or out in the event murals";
				} 
			};
			
			itsSlider.getModel().addChangeListener(this);
	
			itsStartLabel.setText(0+"ms");
			updateLastLabel();
			updateRangeLabels();

			initLayout();
		}
		
		/**
		 * Call this method when the limits (ie. first/last timestamps) have changed.
		 */
		public void setLimits(long aFirst, long aLast)
		{
			if (aFirst == itsFirstTimestamp && aLast == itsLastTimestamp) return;
			
//			TODUtils.logf(1, "[TimestampRangeSlider.setLimits] setLimits(%d, %d)...", aFirst, aLast);
//			TODUtils.logf(1, "[TimestampRangeSlider.setLimits] range before[%d, %d]", itsRangeStart, itsRangeEnd);
			if (itsRangeEnd == itsLastTimestamp) itsRangeEnd = aLast;
			
			itsFirstTimestamp = aFirst;
			itsLastTimestamp = aLast;
			
			if (itsRangeStart < itsFirstTimestamp) itsRangeStart = itsFirstTimestamp;
			if (itsRangeEnd > itsLastTimestamp) itsRangeEnd = itsLastTimestamp;

//			TODUtils.logf(1, "[TimestampRangeSlider.setLimits] range after[%d, %d]", itsRangeStart, itsRangeEnd);
			
			long theDelta = itsLastTimestamp-itsFirstTimestamp;
			
			itsChanging++;
			itsSlider.setLowValue(1.0*(itsRangeStart-itsFirstTimestamp)/theDelta);
			itsSlider.setHighValue(1.0*(itsRangeEnd-itsFirstTimestamp)/theDelta);
			itsSlider.getModel().setValueIsAdjusting(false);
			itsChanging--;

			updateRangeLabels();
			updateLastLabel();
			updateViews();

//			TODUtils.logf(1, "[TimestampRangeSlider.setLimits] done.", aFirst, aLast);
		}
		
		public void setRangeStart(long aStart)
		{
			long theDelta = itsLastTimestamp-itsFirstTimestamp;
			if (theDelta == 0) return;
			
			if (aStart < itsFirstTimestamp) 
			{
				throw new RuntimeException("First: "+itsFirstTimestamp+", start: "+aStart);
			}
			
			itsChanging++;
			itsSlider.setLowValue(1.0*(aStart-itsFirstTimestamp)/(theDelta));
			itsChanging--;

			updateRangeLabels();
			
//			TODUtils.logf(1, "[TimestampRangeSlider.setRangeStart] old: %d, new: %d", itsRangeStart, aStart);
			itsRangeStart = aStart;
		}
		
		public void setRangeEnd(long aEnd)
		{
			long theDelta = itsLastTimestamp-itsFirstTimestamp;
			if (theDelta == 0) return;
			
			itsChanging++;
			itsSlider.setHighValue(1.0*(aEnd-itsFirstTimestamp)/(theDelta));
			itsChanging--;

			updateRangeLabels();
			
//			TODUtils.logf(1, "[TimestampRangeSlider.setRangeEnd] old: %d, new: %d", itsRangeEnd, aEnd);
			itsRangeEnd = aEnd;
		}
		
		private String formatTimestamp(long aTimestamp)
		{
			String theString = AgentUtils.formatTimestamp(aTimestamp);
			return theString.substring(0, theString.length()-8)+"ms";
		}
		
		private void updateRangeLabels()
		{
//			TODUtils.log(2, "range: ["+itsRangeStart+"-"+itsRangeEnd+"]");
			
			long theLow = itsRangeStart-itsFirstTimestamp;
			long theHigh = itsRangeEnd-itsFirstTimestamp;
			
//			itsStartRangeLabel.setText((int)(theLow/1000000)+"ms <");
//			itsEndRangeLabel.setText("< "+(int)(theHigh/1000000)+"ms");
			itsStartRangeLabel.setText(formatTimestamp(theLow)+" <");
			itsEndRangeLabel.setText("< "+formatTimestamp(theHigh));
		}
		
		private void updateLastLabel()
		{
			boolean rangeNeedUpdate = itsLastTimestamp == itsRangeEnd;
			
			itsEndLabel.setText(formatTimestamp(itsLastTimestamp-itsFirstTimestamp));
			if (rangeNeedUpdate) updateViews();
		}
		
		private void initLayout()
		{
			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			itsSlider.setEnabled(true);	
			
			Font theDerivedFont = itsStartLabel.getFont().deriveFont(9f);
			itsStartLabel.setFont(theDerivedFont);
			itsEndLabel.setFont(theDerivedFont);
			itsStartRangeLabel.setFont(theDerivedFont);
			itsEndRangeLabel.setFont(theDerivedFont);
			
			Box theTopBox = Box.createHorizontalBox();
			theTopBox.add(itsStartLabel);
			theTopBox.add(Box.createHorizontalGlue());
			theTopBox.add(itsStartRangeLabel);
			theTopBox.add(Box.createHorizontalGlue());
			theTopBox.add(itsEndRangeLabel);
			theTopBox.add(Box.createHorizontalGlue());
			theTopBox.add(itsEndLabel);
			
			add(theTopBox);
			add(itsSlider);
		}
		
		public void stateChanged(ChangeEvent aE)
		{
			if (itsChanging > 0) return;
			
			double theLowValue = itsSlider.getLowValue();
			double theLow = theLowValue*(itsLastTimestamp-itsFirstTimestamp);
			itsRangeStart=(long) (itsFirstTimestamp + theLow);
			double theHigh = itsSlider.getHighValue()*(itsLastTimestamp-itsFirstTimestamp);
			itsRangeEnd=(long) (itsFirstTimestamp + theHigh);			
			
			updateRangeLabels();
			updateViews();
		}

		private void updateViews()
		{
			if (!itsSlider.getModel().getValueIsAdjusting() && (itsRangeEnd-itsRangeStart) > 0)
			{
//				TODUtils.logf(1, "[TimestampRangeSlider] updateViews(%d, %d)", itsRangeStart, itsRangeEnd);
				pStart().set(itsRangeStart);
				pEnd().set(itsRangeEnd);
			}
		}
		
		
	}
	

}
