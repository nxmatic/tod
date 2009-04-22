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
package tod.gui.components.eventsequences.mural;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import tod.core.database.browser.IEventBrowser;
import tod.core.database.browser.LocationUtils;
import tod.core.database.event.ILogEvent;
import tod.core.database.structure.IBehaviorInfo.BytecodeRole;
import tod.gui.BrowserData;
import tod.gui.FontConfig;
import tod.gui.GUIUtils;
import tod.gui.IGUIManager;
import tod.gui.Resources;
import tod.gui.components.eventsequences.mural.IBalloonProvider.Balloon;
import tod.gui.formatter.EventFormatter;
import tod.utils.TODUtils;
import zz.utils.Cleaner;
import zz.utils.notification.IEvent;
import zz.utils.notification.IEventListener;
import zz.utils.notification.IFireableEvent;
import zz.utils.notification.SimpleEvent;
import zz.utils.properties.ArrayListProperty;
import zz.utils.properties.IListProperty;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;
import zz.utils.ui.GridStackLayout;
import zz.utils.ui.MouseModifiers;
import zz.utils.ui.MouseWheelPanel;
import zz.utils.ui.NullLayout;
import zz.utils.ui.Orientation;
import zz.utils.ui.StackLayout;
import zz.utils.ui.TransparentPanel;
import zz.utils.ui.ResourceUtils.ImageResource;

/**
 * A graphic object that represents a mural (see http://reflex.dcc.uchile.cl/?q=node/60) 
 * of sequences of events.
 * @author gpothier
 */
public class EventMural extends MouseWheelPanel
{
	private static final int CLICK_THRESHOLD = 5*5;
	private static final int ZOOM_MARK_COUNTDOWN = 4;
	
	private static final ImageUpdater itsUpdater = new ImageUpdater();
	private final Orientation itsOrientation;
	
	/**
	 * First allowed timestamp
	 */
	private long itsFirstTimestamp;
	
	/**
	 * Last allowed timestamp
	 */
	private long itsLastTimestamp;
	

	
	/**
	 * The first timestamp of the displayed time range.
	 */
	public final IRWProperty<Long> pStart = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			markDirty();
		}
	};
	
	/**
	 * The last timestamp of the displayed time range.
	 */
	public final IRWProperty<Long> pEnd = new SimpleRWProperty<Long>(this)
	{
		@Override
		protected void changed(Long aOldValue, Long aNewValue)
		{
			markDirty();
		}
	};
	
	/**
	 * The list of event browsers displayed in this mural.
	 */
	public final IListProperty<BrowserData> pEventBrowsers = new ArrayListProperty<BrowserData>(this)
	{
		@Override
		protected void contentChanged()
		{
			markDirty();
		}
	};
	
	/**
	 * This event is fired when the mural receices a click and has no 
	 * associated action to perform.
	 */
	public final IEvent<MouseEvent> eClicked = new SimpleEvent<MouseEvent>(); 

	/**
	 * This event is fired when an event is clicked.
	 */
	public final IEvent<ILogEvent> eEventClicked = new SimpleEvent<ILogEvent>();
	
	private Cleaner itsImageCleaner = new Cleaner()
	{
		@Override
		protected void clean()
		{
			if (itsImage != null) itsImage.setUpToDate(false);
			updateBaloons();
			repaint();
		}
	};

	private AbstractMuralPainter itsMuralPainter = StackMuralPainter.getInstance();
	
	/**
	 * Set to the latest mouse press position
	 */
	private Point itsClickStart;
	private Point itsZoomDirection;
	private Timer itsZoomTimer;
	private int itsZoomMarkCountdown = ZOOM_MARK_COUNTDOWN;
	
	private final IGUIManager itsGUIManager;
	
	/**
	 * Mural image (one version per display).
	 */
	private ImageData itsImage; 
	
	/**
	 * This timer permits to animate the mural while counts are being retrieved
	 * from the db.
	 */
	private Timer itsTimer;
	
	
	/**
	 * We keep the last available image so that we can
	 * repaint while a new image is being calculated
	 */
	private BufferedImage itsLastImage;

	/**
	 * The event whose details are currently displayed
	 */
	private ILogEvent itsCurrentEvent;
	private EventDetailsPanel itsCurrentEventPanel;
	
	/**
	 * Optional balloon provider.
	 */
	private IBalloonProvider itsBalloonProvider;
	
	private JPanel itsCurrentEventLayer;
	private JPanel itsBalloonsLayer;
	
	private final IEventListener<Void> itsBalloonListener = new IEventListener<Void>()
	{
		public void fired(IEvent< ? extends Void> aEvent, Void aData)
		{
			updateBaloons();
		}
	};
	
	public EventMural(
			IGUIManager aGUIManager, 
			Orientation aOrientation,
			long aFirstTimestamp,
			long aLastTimestamp)
	{
		super(new StackLayout());
		itsGUIManager = aGUIManager;
		itsOrientation = aOrientation;
		itsFirstTimestamp = aFirstTimestamp;
		itsLastTimestamp = aLastTimestamp;
		
		setPreferredSize(new Dimension(100, 20));
		itsTimer = new Timer(100, new ActionListener()
		{
			public void actionPerformed(ActionEvent aE)
			{
				repaint();
			}
		});
		itsTimer.setRepeats(false);
		
		itsZoomTimer = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent aE)
			{
				updateZoom();
			}
		});
		
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent aE)
			{
				markDirty();
			}
		});
		
		itsCurrentEventLayer = new TransparentPanel(new NullLayout());
		add(itsCurrentEventLayer);
		
		itsBalloonsLayer = new TransparentPanel(new NullLayout());
		add(itsBalloonsLayer);
		
		itsCurrentEventPanel = new EventDetailsPanel();
		itsCurrentEventPanel.setVisible(false);
		itsCurrentEventLayer.add(itsCurrentEventPanel);
	}

	public EventMural(
			IGUIManager aGUIManager, 
			Orientation aOrientation, 
			IEventBrowser aBrowser,
			long aFirstTimestamp,
			long aLastTimestamp)
	{
		this(aGUIManager, aOrientation, aFirstTimestamp, aLastTimestamp);
		pEventBrowsers.add(new BrowserData(aBrowser));
		updateBaloons();
	}

	public void setLimits(long aFirstTimestamp, long aLastTimestamp)
	{
		itsFirstTimestamp = aFirstTimestamp;
		itsLastTimestamp = aLastTimestamp;
	}
	
	public AbstractMuralPainter getMuralPainter()
	{
		return itsMuralPainter;
	}

	public void setMuralPainter(AbstractMuralPainter aMuralPainter)
	{
		itsMuralPainter = aMuralPainter;
		repaint();
	}

	/**
	 * Returns true only if all required information is available (bounds, range, etc).
	 */
	protected boolean isReady()
	{
		return pStart.get() != null 
			&& pEnd.get() != null;
	}
	
	protected void markDirty()
	{
		itsImageCleaner.markDirty();
		TODUtils.log(2, "[EventMural] markDirty");
	}
	
	/**
	 * Updates the timescale image.
	 */
	protected void updateImage()
	{
		TODUtils.log(2, "[EventMural] updateImage()");
		if (! isReady()) return;
		TODUtils.log(2, "[EventMural] updateImage - requesting");
		itsUpdater.request(this);
	}
	
	
	@Override
	protected void paintComponent(Graphics aGraphics)
	{
		if (pEventBrowsers.isEmpty()) 
		{
			super.paintComponent(aGraphics);
			return;
		}
		
		// Paint mural image
		ImageData theImageData = itsImage;
		
		BufferedImage theImage = null;
		if (theImageData == null || ! theImageData.isUpToDate()) updateImage();
		else 
		{
			theImage = theImageData.getImage();
			itsLastImage = theImage;
		}
		
		if (theImage == null && itsImageCleaner.dirtyTime() < 1000) theImage = itsLastImage;

		int w = getWidth();
		int h = getHeight();
		
		if (theImage != null)
		{
			aGraphics.drawImage(theImage, 0, 0, w, h, null);
			if (theImage.getWidth() != w || theImage.getHeight() != h) markDirty();
		}
		else
		{
			int theSize = 10;
			int theX = w/2;
			int theY = h/2;
			
			long theTime = System.currentTimeMillis();
			aGraphics.setColor(Color.WHITE);
			aGraphics.fillRect(0, 0, getWidth(), getHeight());
			aGraphics.setColor((theTime/200) % 2 == 0 ? Color.BLACK : Color.LIGHT_GRAY);
			aGraphics.fillRect(theX-theSize/2, theY-theSize/2, theSize, theSize);
			
			itsTimer.start();
		}
		
		// Draw zoom mark
		if (itsZoomMarkCountdown == 0)
		{
			BufferedImage theMarker = Resources.ICON_ZOOMSCROLLMARKER.getImage();
			aGraphics.drawImage(
					theMarker, 
					itsClickStart.x-theMarker.getWidth()/2,
					itsClickStart.y-theMarker.getHeight()/2,
					null);
		}
	}
	
	protected IFireableEvent<MouseEvent> eClicked()
	{
		return (IFireableEvent<MouseEvent>) eClicked;
	}
	
	protected IFireableEvent<ILogEvent> eEventClicked()
	{
		return (IFireableEvent<ILogEvent>) eEventClicked;
	}
	
	private ILogEvent getEventAt(int aX)
	{
		if (! itsImage.isUpToDate()) return null;
		
		Long t1 = pStart.get();
		Long t2 = pEnd.get();
		
		long w = t2-t1;
		
		// The timestamp corresponding to the mouse cursor
		long t = t1+(long)(1f*w*aX/getWidth());
		long d = (long)(5f*w/getWidth());
		
		return getEventAt(t, d);
	}
	
	/**
	 * Returns the event for which details should be displayed at the specified timestamp.
	 * Returns null by default.
	 * @param aTimestamp The timestamp of the event to retrieve
	 * @param aTolerance The allowed tolerance for the actual event timestamp
	 */
	protected ILogEvent getEventAt(long aTimestamp, long aTolerance)
	{
		return null;
	}
	
	private void updateEventInfo(int aX)
	{
		setCurrentEvent(getEventAt(aX));
	}
	
	public void setCurrentEvent(ILogEvent aEvent)
	{
		if (aEvent == itsCurrentEvent) return;
		itsCurrentEvent = aEvent;
		
		if (itsCurrentEvent == null) itsCurrentEventPanel.setVisible(false);
		else
		{
			// Find out event position.
			long t = itsCurrentEvent.getTimestamp();
			Long t1 = pStart.get();
			Long t2 = pEnd.get();
			
			long w = t2-t1;

			int theX = (int) ((t-t1)*getWidth()/w);
			
			itsCurrentEventPanel.setEvent(itsCurrentEvent);
			Dimension theSize = itsCurrentEventPanel.getPreferredSize();
			if (theX+theSize.width > getWidth()) theX = getWidth()-theSize.width;
			if (theX < 0) theX = 0;
			itsCurrentEventPanel.setBounds(theX, 5, theSize.width, theSize.height);
			itsCurrentEventPanel.setVisible(true);
		}
		repaint();
	}
	
	/**
	 * Whether the given mouse event has the modifiers that indicate that
	 * event tooltips should be displayed/clickable.
	 */
	private boolean hasTooltipModifier(MouseEvent aEvent)
	{
		return MouseModifiers.hasCtrl(aEvent) || MouseModifiers.hasShift(aEvent);
	}
	
	@Override
	public void mouseMoved(MouseEvent aE)
	{
		if (hasTooltipModifier(aE))
		{
			updateEventInfo(aE.getX());
		}
		else setCurrentEvent(null);
	}

	@Override
	public void mouseExited(MouseEvent aE)
	{
		setCurrentEvent(null);
	}
	
	@Override
	public void mousePressed(MouseEvent aE)
	{
		if (aE.getButton() == MouseEvent.BUTTON1)
		{
			if (hasTooltipModifier(aE))
			{
				updateEventInfo(aE.getX());
				if (itsCurrentEvent != null) eEventClicked().fire(itsCurrentEvent);
			}
			else 
			{
				if (aE.getClickCount() == 1) 
				{
					itsClickStart = aE.getPoint();
					itsZoomMarkCountdown = ZOOM_MARK_COUNTDOWN;
					itsZoomTimer.start();				
				}
				else if (aE.getClickCount() == 2) eClicked().fire(aE);
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent aE)
	{
		if (itsClickStart != null)
		{
			itsZoomDirection = new Point(aE.getX()-itsClickStart.x, aE.getY()-itsClickStart.y);			
			itsZoomMarkCountdown = 0;
		}
	}
	
	/**
	 * If the absolute value of value is less that limit, returns 0,
	 * otherwise returns value +/- limit
	 */
	private static int towards0(int aValue, int aLimit) 
	{
		if (aValue > aLimit) return aValue-aLimit;
		else if (aValue < -aLimit) return aValue+aLimit;
		else return 0;
	}
	
	/**
	 * Called at a fixed rate while the mouse button is pressed
	 */
	private void updateZoom()
	{
		if (itsZoomDirection != null)
		{
//			int theZoomCenter = (int) (itsClickStart.x+Math.pow(itsZoomDirection.x/10f, 3) * 10);
			
			float theZoomAmount = -towards0(itsZoomDirection.y, 10)/200f;
			float theScrollAmount = towards0(itsZoomDirection.x, 10)/100f;
			
			zoomAndScroll(theZoomAmount, itsClickStart.x, theScrollAmount);
			
		}
		else if (itsZoomMarkCountdown > 0)
		{
			itsZoomMarkCountdown--;
			if (itsZoomMarkCountdown == 0) repaint();
		}
	}
	
	private boolean inAngleRange(double aAngle, double aRef, double aTolerance)
	{
		double theDist = Math.abs(aAngle-aRef);
		
		while(theDist > Math.PI*2) theDist -= Math.PI*2;
		return (theDist < aTolerance) || (Math.PI*2-theDist < aTolerance);
	}
	
	@Override
	public void mouseReleased(MouseEvent aE)
	{
		if (itsClickStart != null)
		{
			itsZoomTimer.stop();
			itsClickStart = null;
			itsZoomDirection = null;
			itsZoomMarkCountdown = ZOOM_MARK_COUNTDOWN;
			repaint();
		}
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent aE)
	{
		if (MouseModifiers.getModifiers(aE) == MouseModifiers.CTRL)
		{
			zoom(-aE.getWheelRotation(), aE.getX());
		}
	}

	protected void zoom(float aAmount, int aX)
	{
		if (aAmount == 0) return;
		
		setCurrentEvent(null);
		
		Long t1 = pStart.get();
		Long t2 = pEnd.get();
		
		long w = t2-t1;
		
		// The timestamp corresponding to the mouse cursor
		long t = t1+(long)(1f*w*aX/getWidth());
		
		float k = (float) Math.pow(2, -0.5f*aAmount);
		long nw = (long) (k*w);
		
		if (nw < 2) return;
			
		long s = t - ((long) ((t-t1)*k));
		
		if (s < itsFirstTimestamp) s = itsFirstTimestamp;
		if (itsLastTimestamp > 0 && s+nw > itsLastTimestamp) s = itsLastTimestamp - nw;

		pEnd.set(s+nw);
		pStart.set(s);
	}
	
	protected void scroll(float aAmount)
	{
		if (aAmount == 0) return;

		setCurrentEvent(null);
		
		Long t1 = pStart.get();
		Long t2 = pEnd.get();
		
		long w = t2-t1;
		long s = t1+(long)(aAmount*w/10f);
		if (s < itsFirstTimestamp) s = itsFirstTimestamp;
		if (itsLastTimestamp > 0 && s+w > itsLastTimestamp) s = itsLastTimestamp - w;

		pEnd.set(s+w);
		pStart.set(s);
	}

	protected void zoomAndScroll(float aZoomAmount, int aZoomCenter, float aScrollAmount)
	{
		if (aZoomAmount == 0 && aScrollAmount == 0) return;

		setCurrentEvent(null);
		
		Long t1 = pStart.get();
		Long t2 = pEnd.get();
		
		long w = t2-t1;
		
		// The timestamp corresponding to the mouse cursor
		double t = 1.0*w*aZoomCenter/getWidth();
		
		double k = Math.pow(2, -0.5*aZoomAmount);
		double nw = k*w;
		
		if (nw < 2) return;
		
		double s = t - (t*k) + (aScrollAmount*w/10f); 
		double t1p = s+t1;
		
		if (t1p < itsFirstTimestamp) t1p = itsFirstTimestamp;
		if (itsLastTimestamp > 0 && t1p+nw > itsLastTimestamp) t1p = itsLastTimestamp - nw;
		
		pEnd.set((long)(t1p+nw));
		pStart.set((long)t1p);
	}
	
	public IBalloonProvider getBalloonProvider()
	{
		return itsBalloonProvider;
	}

	public void setBalloonProvider(IBalloonProvider aBalloonProvider)
	{
		if (itsBalloonProvider != null) itsBalloonProvider.eChanged().removeListener(itsBalloonListener);
		itsBalloonProvider = aBalloonProvider;
		if (itsBalloonProvider != null) itsBalloonProvider.eChanged().addListener(itsBalloonListener);
	}

	/**
	 * Updates the set of displayed baloons.
	 */
	protected void updateBaloons()
	{
		itsBalloonsLayer.removeAll();
		if (! isReady()) return;
		
		// Get parameters
		int w = getWidth();
		int x = 0;
		
		long t1 = pStart.get();
		long t2 = pEnd.get();
		
		if (t1 == t2) return;
		
		List<Balloon> theBaloons = itsBalloonProvider != null ? itsBalloonProvider.getBaloons(t1, t2) : null;
		
		long t = t1;
		
		// Start placing baloons
		SpaceManager theManager = new SpaceManager(getHeight());

		if (theBaloons != null) for (Balloon theBalloon : theBaloons)
		{
			t = theBalloon.getTimestamp();
			if (t > t2) break;
			
			x = (int) (w * (t - t1) / (t2 - t1));
			
			Range theRange = theManager.getAvailableRange(x);
			if (theRange == null) continue;
			
			JComponent theBaloon = createBalloon(theBalloon);
			
			if (theBaloon != null)
			{
				Dimension theSize = theBaloon.getPreferredSize();
				
				if (theSize.height > theRange.getSpan()) continue;

				int by = (int) theRange.getStart();
				double bw = theSize.width;
				double bh = theSize.height;
				
				theBaloon.setBounds(x-(theSize.width/2), by, theSize.width, theSize.height);
				itsBalloonsLayer.add(theBaloon);
				
				theManager.occupy(x, by, bw, bh);
			}
		}
		itsBalloonsLayer.repaint();
	}
	
	/**
	 * Creates the {@link JComponent} that represents a balloon.
	 */
	private JComponent createBalloon(Balloon aBalloon)
	{
		return new BalloonLabel(aBalloon);
	}
	
	private class BalloonLabel extends JLabel
	implements MouseListener
	{
		private final Balloon itsBalloon;

		public BalloonLabel(Balloon aBalloon)
		{
			super("<html>"+aBalloon.getText());
			itsBalloon = aBalloon;
			addMouseListener(this);
			setOpaque(false);
			setBackground(Color.LIGHT_GRAY);
		}

		public void mouseEntered(MouseEvent aE)
		{
			setOpaque(true);
			repaint();
		}

		public void mouseExited(MouseEvent aE)
		{
			setOpaque(false);
			repaint();
		}

		public void mousePressed(MouseEvent aE)
		{
			eEventClicked().fire(itsBalloon.getEvent());
		}

		public void mouseReleased(MouseEvent aE)
		{
		}

		public void mouseClicked(MouseEvent aE)
		{
		}

		
	}
	
	private static class ImageUpdater extends Thread
	{
		private BlockingQueue<EventMural> itsRequestsQueue =
			new LinkedBlockingQueue<EventMural>();
		
		private Set<EventMural> itsCurrentRequests =
			new HashSet<EventMural>();
		
		public ImageUpdater()
		{
			super("EventMural updater");
			start();
		}

		public synchronized void request(EventMural aMural)
		{
			try
			{
				if (! itsCurrentRequests.contains(aMural))
				{
					TODUtils.log(2, "[ImageUpdater] adding request");
					itsCurrentRequests.add(aMural);
					itsRequestsQueue.put(aMural);
				}
			}
			catch (InterruptedException e)
			{
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					EventMural theRequest = itsRequestsQueue.take();
					long t0 = System.currentTimeMillis();
					TODUtils.log(2,"[EventMural.Updater] Processing request: "+theRequest);
					try
					{
						doUpdateImage(theRequest);
					}
					catch(ConcurrentModificationException e)
					{
						System.err.println("EventMural.ImageUpdater.run] Concurrent modification, retrying");
						theRequest.markDirty();
					}
					catch (Throwable e)
					{
						System.err.println("Exception in EventMural.Updater:");
						e.printStackTrace();
					}
					long t1 = System.currentTimeMillis();
					float t = (t1-t0)/1000f;
					TODUtils.log(2,"[EventMural.Updater] Finished request in "+t+"s.");
					
					itsCurrentRequests.remove(theRequest);
				}
			}
			catch (InterruptedException e)
			{
			}
		}
		
		protected void doUpdateImage(EventMural aMural)
		{
			int width = aMural.getWidth();
			int height = aMural.getHeight();
			if (height == 0 || width == 0) return;
			
			int u = aMural.itsOrientation.getU(width, height);
			int v = aMural.itsOrientation.getV(width, height);
			
			// Ensure we have a fast image buffer
			ImageData theImageData = aMural.itsImage;
			BufferedImage theImage = theImageData != null ? theImageData.getImage() : null;
			if (theImage == null 
					|| theImage.getWidth() != width 
					|| theImage.getHeight() != height) 
			{
				GraphicsConfiguration theConfiguration = aMural.getGraphicsConfiguration();
				theImage = theConfiguration.createCompatibleImage(width, height);
				theImageData = new ImageData(theImage);
				aMural.itsImage = theImageData;
			}
			// Setup graphics
			Graphics2D theGraphics = theImage.createGraphics();
			if (aMural.itsOrientation == Orientation.VERTICAL)
			{
				AffineTransform theTransform = new AffineTransform();
				theTransform.translate(v, 0);
				theTransform.rotate(Math.PI/2);
				
				theGraphics.transform(theTransform);
			}
			theGraphics.setColor(Color.WHITE);
			theGraphics.fillRect(0, 0, u, v);
			Long theStart = aMural.pStart.get();
			Long theEnd = aMural.pEnd.get();
			
			TODUtils.log(2, "[ImageUpdater] doUpdateImage ["+theStart+"-"+theEnd+"]");
			
			if (theStart < theEnd)
			{
				// Paint
				// (cloning the browser data list in order to avoid concurrent modifications)
				long[][] theValues = aMural.itsMuralPainter.paintMural(
						aMural, 
						theGraphics, 
						new Rectangle(0, 0, u, v), 
						theStart, 
						theEnd, 
						new ArrayList<BrowserData>(aMural.pEventBrowsers)); 

				theImageData.setUpToDate(true);
				theImageData.setValues(theValues);
				aMural.repaint();
			}
		}
	}
	
	private static class ImageData
	{
		private BufferedImage itsImage;
		
		/**
		 * The count values displayed in the image.
		 */
		private long[][] itsValues;
		
		/**
		 * Whether the image/values are up to date.
		 */
		private boolean itsUpToDate;
		
		public ImageData(BufferedImage aImage)
		{
			itsImage = aImage;
			itsUpToDate = false;
		}

		public BufferedImage getImage()
		{
			return itsImage;
		}

		public boolean isUpToDate()
		{
			return itsUpToDate;
		}

		public void setUpToDate(boolean aUpToDate)
		{
			itsUpToDate = aUpToDate;
		}

		public long[][] getValues()
		{
			return itsValues;
		}

		public void setValues(long[][] aValues)
		{
			itsValues = aValues;
		}
	}
	
	private class EventDetailsPanel extends JPanel
	{
		private ILogEvent itsEvent;
		
		private JLabel itsKindLabel;
		private JLabel itsThreadLabel;
		private JLabel itsDetailsLabel;

		private EventFormatter itsFormatter;

		public EventDetailsPanel()
		{
			super(new GridStackLayout(1));
			setBackground(Color.WHITE);
			setBorder(BorderFactory.createLineBorder(Color.black));
			
			itsKindLabel = new JLabel();
			itsKindLabel.setFont(FontConfig.SMALL_FONT.getAWTFont());
			
			itsThreadLabel = new JLabel();
			itsThreadLabel.setFont(FontConfig.SMALL_FONT.getAWTFont());
			
			itsDetailsLabel = new JLabel();
			itsDetailsLabel.setFont(FontConfig.STD_FONT.getAWTFont());
			
			add(itsKindLabel);
			add(itsThreadLabel);
			add(itsDetailsLabel);
			itsFormatter = new EventFormatter(itsGUIManager.getSession().getLogBrowser());
		}
		
		public void setEvent(ILogEvent aEvent)
		{
			itsEvent = aEvent;
			itsThreadLabel.setText("Thread: "+itsEvent.getThread().getName());
			itsDetailsLabel.setText(itsFormatter.getHtmlText(itsEvent));
			
			BytecodeRole theRole = LocationUtils.getEventRole(aEvent);
			ImageResource theIcon = GUIUtils.getRoleIcon(theRole);
			itsDetailsLabel.setIcon(theIcon != null ? theIcon.asIcon(15) : null);
		}
	}
	
	/**
	 * This class permits to place baloons according to available space.
	 * TODO: Implement a better algorithm, for now we use discrete scanlines.
	 * @author gpothier
	 */
	private static class SpaceManager
	{
		private static double K = 4.0;
		private double itsHeight;
		private double[] itsLines;
		
		public SpaceManager(double aHeight)
		{
			itsHeight = aHeight;
			itsLines = new double[(int) (itsHeight/K)];
			
			for (int i = 0; i < itsLines.length; i++) itsLines[i] = -1;
		}

		/**
		 * Returns the biggest available range at the specified position.
		 * @return A {@link Range}, or null if there is no space.
		 */
		public Range getAvailableRange (double aX)
		{
			Range theBiggestRange = null;
			double theStart = -1;
			double theEnd = -1;
			
			for (int i = 0; i < itsLines.length; i++)
			{
				double x = itsLines[i];
				
				if (theStart < 0)
				{
					if (x < aX) theStart = i*K;
				}
				else
				{
					if (x < aX) theEnd = i*K;
					else
					{
						Range theRange = new Range(theStart, theEnd);
						if (theBiggestRange == null || theRange.getSpan() > theBiggestRange.getSpan())
							theBiggestRange = theRange;
						
						theStart = theEnd = -1;
					}
				}
			}
			
			if (theBiggestRange == null && theStart >= 0)
			{
				theBiggestRange = new Range(theStart, theEnd);
			}
			
			return theBiggestRange;
		}
		
		/**
		 * Marks the given bounds as occupied 
		 */
		public void occupy(double aX, double aY, double aW, double aH)
		{
			double theY1 = aY;
			double theY2 = aY+aH;
			
			int theI1 = (int) (theY1 / K);
			int theI2 = (int) (theY2 / K);
			
			for (int i=theI1;i<=theI2;i++) itsLines[i] = aX+aW;
		}
		
	}
	
	private static class Range
	{
		private double itsStart;
		private double itsEnd;
		
		public Range(double aStart, double aEnd)
		{
			itsStart = aStart;
			itsEnd = aEnd;
		}
		
		public double getEnd()
		{
			return itsEnd;
		}

		public double getStart()
		{
			return itsStart;
		}

		public boolean intersects (Range aRange)
		{
			return aRange.getStart() <= getEnd() || getStart() <= aRange.getEnd();
		}
		
		public boolean contains (Range aRange)
		{
			return aRange.getStart() >= getStart() && aRange.getEnd() <= getEnd();			
		}
		
		public double getSpan()
		{
			return itsEnd - itsStart;
		}
	}
	

}
