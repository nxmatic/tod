/*
 * Created on Jan 31, 2007
 */
package tod.plugin.launch;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.PageBook;

public class TODDatabaseConfigControl extends Composite
{
	public TODDatabaseConfigControl(Composite aParent)
	{
		super(aParent, SWT.BORDER);
		createUI();
	}
	
	private void createUI()
	{
		ComboViewer theBackendCombo = new ComboViewer(this);
		theBackendCombo.setLabelProvider(new BackendLabelProvider());
	}

	private static class BackendLabelProvider extends LabelProvider
	{
		@Override
		public String getText(Object aElement)
		{
			return ((Backend) aElement).getName();
		}
	}
	
	private static abstract class Backend
	{
		public abstract String getName(); 
		public abstract String getDescription(); 
	}
	
	private static class MemoryBackend extends Backend
	{
		@Override
		public String getName()
		{
			return "Memory";
		}

		@Override
		public String getDescription()
		{
			return "Events are stored in memory, in the Eclipse process. " +
					"This is the less scalable option. The maximum number " +
					"of events depends on the amount of heap memory allocated " +
					"the the JVM that runs Eclipse.";
		}
	}
	
	private static class LocalBackend extends Backend
	{
		@Override
		public String getName()
		{
			return "Local database";
		}

		@Override
		public String getDescription()
		{
			return "Events are stored on the hard disk. This option " +
					"provides good scalability but performance may be " +
					"a problem.";
		}
	}

	private static class GridBackend extends Backend
	{
		@Override
		public String getName()
		{
			return "Grid";
		}

		@Override
		public String getDescription()
		{
			return "Events are stored in a dedicated distributed database. " +
					"This option provides good scalability and performance " +
					"(depending on the size of the database cluster).";
		}
	}
}
