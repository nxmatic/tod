/*
 * Created on Jun 19, 2007
 */
package tod.plugin.views;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.Timer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import zz.utils.ui.StackLayout;

public abstract class AbstractAWTView extends ViewPart 
{
	private static boolean x11ErrorHandlerFixInstalled = false;
	private Frame itsFrame;
	
	@Override
	public void createPartControl(Composite parent) 
	{
		final Composite theEmbedded = new Composite(parent, SWT.EMBEDDED | SWT.CENTER);
		parent.setLayout(new FillLayout());
		
		itsFrame = SWT_AWT.new_Frame(theEmbedded);
		
		if (!x11ErrorHandlerFixInstalled && "gtk".equals(SWT.getPlatform()))
		{
			x11ErrorHandlerFixInstalled = true;
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					initX11ErrorHandlerFix();
				}
			});
		}

		
		Panel theRootPanel = new Panel(new BorderLayout());
		itsFrame.setLayout(new StackLayout());
		itsFrame.add(theRootPanel);
		
		theEmbedded.addControlListener(new ControlListener()
		{
			public void controlMoved(ControlEvent aE)
			{
			}

			public void controlResized(ControlEvent aE)
			{
				itsFrame.setSize(theEmbedded.getSize().x, theEmbedded.getSize().y);
			}
		});

		JRootPane theRootPane = new JRootPane();
		theRootPanel.add(theRootPane);
		theRootPane.getContentPane().add(createComponent());
	}
	
	public void hop()
	{
//		itsFrame = SWT_AWT.new_Frame(theEmbedded);
////		itsFrame.setLayout(new StackLayout());
//		
//		JRootPane theRootPane = new JRootPane();
//		itsFrame.add(theRootPane);
//		java.awt.Container theContentPane = theRootPane.getContentPane();
//
//		theContentPane.add(createComponent());
//
////		theEmbedded.addControlListener(new ControlListener()
////		{
////			public void controlMoved(ControlEvent aE)
////			{
////			}
////
////			public void controlResized(ControlEvent aE)
////			{
////				itsFrame.setSize(theEmbedded.getSize().x, theEmbedded.getSize().y);
////			}
////		});
////		
//		
	}
	
	protected abstract JComponent createComponent();
	
	@Override
	public void setFocus()
	{
	}

	/**
	 * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=171432
	 */
	private static void initX11ErrorHandlerFix() {
		assert EventQueue.isDispatchThread();

		try {
			// get XlibWrapper.SetToolkitErrorHandler() and XSetErrorHandler() methods
			Class xlibwrapperClass = Class.forName( "sun.awt.X11.XlibWrapper" );
			final Method setToolkitErrorHandlerMethod = xlibwrapperClass.getDeclaredMethod( "SetToolkitErrorHandler", null );
			final Method setErrorHandlerMethod = xlibwrapperClass.getDeclaredMethod( "XSetErrorHandler", new Class[] { Long.TYPE } );
			setToolkitErrorHandlerMethod.setAccessible( true );
			setErrorHandlerMethod.setAccessible( true );

			// get XToolkit.saved_error_handler field
			Class xtoolkitClass = Class.forName( "sun.awt.X11.XToolkit" );
			final Field savedErrorHandlerField = xtoolkitClass.getDeclaredField( "saved_error_handler" );
			savedErrorHandlerField.setAccessible( true );

			// determine the current error handler and the value of XLibWrapper.ToolkitErrorHandler
			// (XlibWrapper.SetToolkitErrorHandler() sets the X11 error handler to
			// XLibWrapper.ToolkitErrorHandler and returns the old error handler)
			final Object defaultErrorHandler = setToolkitErrorHandlerMethod.invoke( null, null );
			final Object toolkitErrorHandler = setToolkitErrorHandlerMethod.invoke( null, null );
			setErrorHandlerMethod.invoke( null, new Object[] { defaultErrorHandler } );

			// create timer that watches XToolkit.saved_error_handler whether its value is equal
			// to XLibWrapper.ToolkitErrorHandler, which indicates the start of the trouble
			Timer timer = new Timer( 200, new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					try {
						Object savedErrorHandler = savedErrorHandlerField.get( null );
						if( toolkitErrorHandler.equals( savedErrorHandler ) ) {
							// Last saved error handler in XToolkit.WITH_XERROR_HANDLER
							// is XLibWrapper.ToolkitErrorHandler, which will cause
							// the StackOverflowError when the next X11 error occurs.
							// Workaround: restore the default error handler.
							// Also update XToolkit.saved_error_handler so that
							// this is done only once.
							setErrorHandlerMethod.invoke( null, new Object[] { defaultErrorHandler } );
							savedErrorHandlerField.setLong( null, ((Long)defaultErrorHandler).longValue() );
						}
					} catch( Exception ex ) {
						// ignore
					}
					
				}
			} );
			timer.start();
		} catch( Exception ex ) {
			// ignore
		}
	}

}