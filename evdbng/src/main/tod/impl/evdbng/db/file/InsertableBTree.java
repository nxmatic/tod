package tod.impl.evdbng.db.file;

/**
 * A B+Tree that supports insertion of keys in any order
 * @author gpothier
 */
public class InsertableBTree
{
	private final PagedFile itsFile;
	private final Page itsRootPage;
	
	public InsertableBTree(PagedFile aFile, Page aRootPage)
	{
		itsFile = aFile;
		itsRootPage = aRootPage;
	}
	
	
}
