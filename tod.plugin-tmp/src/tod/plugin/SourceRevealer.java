package tod.plugin;

import org.eclipse.debug.core.ILaunch;

import tod.core.database.structure.IBehaviorInfo;
import tod.core.database.structure.SourceRange;

/**
 * An interface for source code lookups.
 * @author gpothier
 */
public abstract class SourceRevealer
{
	private ILaunch itsLaunch;
	
	public SourceRevealer(ILaunch aLaunch)
	{
		itsLaunch = aLaunch;
	}

	protected ILaunch getLaunch()
	{
		return itsLaunch;
	}

	/**
	 * Goes to the specified source range
	 * @return True on success.
	 */
	public abstract boolean gotoSource(SourceRange aSourceRange);

	/**
	 * Goes to the source of the specified behavior
	 * @return True on success.
	 */
	public abstract boolean gotoSource(IBehaviorInfo aBehavior);

}