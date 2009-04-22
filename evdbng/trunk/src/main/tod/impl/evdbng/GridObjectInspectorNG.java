/*
 * Created on Nov 30, 2008
 */
package tod.impl.evdbng;

import tod.core.database.structure.IArrayTypeInfo;
import tod.core.database.structure.IClassInfo;
import tod.core.database.structure.IStructureDatabase;
import tod.core.database.structure.ITypeInfo;
import tod.core.database.structure.ObjectId;
import tod.impl.dbgrid.GridLogBrowser;
import tod.impl.dbgrid.GridObjectInspector;
import tod.impl.dbgrid.RIGridMaster;

public class GridObjectInspectorNG extends GridObjectInspector
{
	private ITypeInfo aType;
	
	public GridObjectInspectorNG(GridLogBrowser aLogBrowser, IClassInfo aClass)
	{
		super(aLogBrowser, aClass);
	}

	public GridObjectInspectorNG(GridLogBrowser aLogBrowser, ObjectId aObjectId)
	{
		super(aLogBrowser, aObjectId);
	}

	@Override
	public ITypeInfo getType()
	{
		if (aType == null)
		{
			RIGridMaster theMaster = getLogBrowser().getMaster();
			aType = theMaster.getObjectType(getObject().getId());
			
			// We need to obtain an usable type through the structure database
			// (because of the transient fields).
			// TODO: de-gorify this
			aType = reimport(aType, getLogBrowser().getStructureDatabase());
			
			if (aType == null) aType = getLogBrowser().getStructureDatabase().getUnknownClass();
		}
		
		return aType;
	}
	
	private static ITypeInfo reimport(ITypeInfo aType, IStructureDatabase aStructureDatabase)
	{
		if (aType instanceof IClassInfo)
		{
			IClassInfo theClass = (IClassInfo) aType;
			return aStructureDatabase.getType(theClass.getId(), true);
		}
		else if (aType instanceof IArrayTypeInfo)
		{
			IArrayTypeInfo theArrayType = (IArrayTypeInfo) aType;
			return aStructureDatabase.getArrayType(
					reimport(theArrayType.getElementType(), aStructureDatabase), 
					theArrayType.getDimensions());
		}
		else return aType;
	}
}
