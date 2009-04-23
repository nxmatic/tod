package tod.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;

import tod.core.database.structure.ILocationInfo;
import tod.core.database.structure.IStructureDatabase.ProbeInfo;
import tod.core.session.ISession;

/**
 * Interface that should be implemented by plugins that are capable
 * to link locations or probes to source code locations 
 * @author minostro
 */
public interface ISourceRevealer
{
	public static final int CANT = 0;
	public static final int NORMAL = 100;
	public static final int SPECIALIZED = 110;
	
	
	/**
	 * Whether this revealer can handle the given location.
	 * @return The confidence level of this revealer about the source range to reveal.
	 * See the constants in this interface for reasonable return values.
	 */
	public int canHandle(ISession aSession, ILocationInfo aLocation);
	
	/**
	 * Whether this revealer can handle the given probe.
	 * @return The confidence level of this revealer about the source range to reveal.
	 * See the constants in this interface for reasonable return values.
	 */
	public int canHandle(ISession aSession, ProbeInfo aProbe);
	
	/**
	 * Reveal a particular location.
	 * @return Whether revealing was successful.
	 */
	public boolean reveal(ISession aSession, ILocationInfo aLocation) throws CoreException, BadLocationException;
	
	/**
	 * Reveal a particular probe.
	 * @return Whether revealing was successful.
	 */
	public boolean reveal(ISession aSession, ProbeInfo aProbe) throws CoreException, BadLocationException;

}
