/*
 * Created on Dec 14, 2007
 */
package tod.agent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tod.agent.ConnectionManager.InstrumentationResponse;
import tod.tools.parsers.ParseException;
import tod.tools.parsers.workingset.AbstractClassSet;
import tod.tools.parsers.workingset.WorkingSetFactory;
import zz.utils.Utils;

/**
 * cached classes are defined as [cachePath]/[prefix]/[className].[md5].class
 * /[className].[md5].tm
 * 
 * @author omotelet
 * 
 */
public class InstrumentationManager
{
	private final NativeAgentConfig itsConfig;

	private final ConnectionManager itsConnectionManager;

	private ScopeManager itsScopeManager;


	/**
	 * A buffer of instrumented method ids. This is necessary because we can
	 * recieve instrumentation requests before the VMStart event occurs, and we
	 * cannot use {@link VMUtils#callTracedMethods_setTraced} before VMStart.
	 */
	private List<Integer> itsTmpTracedMethods = new ArrayList<Integer>();

	public InstrumentationManager(NativeAgentConfig aConfig, ConnectionManager aConnectionManager, ScopeManager aScopeManager)
	{
		itsConfig = aConfig;
		itsConnectionManager = aConnectionManager;
		itsScopeManager = aScopeManager;
		
		// Compute class cache prefix
		String theSigSrc = itsConfig.getWorkingSet() + "/" + itsConfig.getStructDbId();
		itsConfig.setClassCachePrefix(Utils.md5String(theSigSrc.getBytes()));
		itsConfig.logf(1, "Class cache prefix: %s", itsConfig.getClassCachePrefix());
	}

	public synchronized byte[] instrument(String aName, byte[] aOriginal)
	{
		if (itsScopeManager.isInScope(aName))
		{
			itsConfig.log(1, "Instrumentation of --" + aName + "--");
			InstrumentationResponse theResponse;

			String theMD5 = Utils.md5String(aOriginal);
			theResponse = null;
			lookupInCache(aName, theMD5);

			if (theResponse == null)
			{
				theResponse = itsConnectionManager.sendInstrumentationRequest(aName, aOriginal);
				cache(theResponse, aName, theMD5);
			}

			if (theResponse.bytecode != null)
			{
				itsConfig.logf(1, "Redefined class %s (%d bytes, %d traced methods)", aName,
						theResponse.bytecode.length, theResponse.tracedMethods.length);
			}

			registerTracedMethods(theResponse.tracedMethods);
			return theResponse.bytecode;
		}
		else return null;
	}

	/**
	 * Looks up a pre-instrumented class in the cache.
	 * 
	 * @param aName
	 *            Name of the class
	 * @return An {@link InstrumentationResponse} object if instrumentation data
	 *         for the class is found in the cache. Note that the returned
	 *         object can have a null bytecode array if the class is known to
	 *         not need instrumentation. If no information for the class is
	 *         found in the cache, returns null.
	 */
	private InstrumentationResponse lookupInCache(String aName, String aMD5Sum)
	{
		String thePath = itsConfig.getCachePath() + "/" + itsConfig.getClassCachePrefix() + "/" + aName + "." + aMD5Sum;
		File theClassFile = new File(thePath + ".class");
		if (!theClassFile.exists())
		{
			itsConfig.log(1, "No cache files for " + aName);
			return null;
		}
		File theClassFileTM = new File(thePath + ".tm");
		if (!theClassFileTM.exists()) return null;
		int theLength = (int) theClassFile.length();
		byte[] theClassBytes = null;
		int theTMLength = (int) (theClassFileTM.length() / 4);
		int[] theClassTMInt = new int[theTMLength];
		try
		{
			if (theLength != 0)
			{
				theClassBytes = new byte[theLength];
				new DataInputStream(new FileInputStream(theClassFile)).readFully(theClassBytes);
			}
			DataInputStream theDataTM = new DataInputStream(new FileInputStream(theClassFileTM));
			int i = 0;
			while (i < theTMLength)
			{
				theClassTMInt[i] = theDataTM.readInt();
				i++;
			}
		}
		catch (Exception e)
		{
			itsConfig.log(1, "Error while reading cache files of " + aName);
			e.printStackTrace();
			return null;
		}
		itsConfig.log(1, "Found Cache Files for " + aName);
		return new ConnectionManager.InstrumentationResponse(theClassBytes, theClassTMInt);
	}

	/**
	 * Stores instrumentation response in the cache.
	 */
	private void cache(InstrumentationResponse aResponse, String aName, String aMD5Sum)
	{
		String thePath = itsConfig.getCachePath() + "/" + itsConfig.getClassCachePrefix() + "/" + aName + "." + aMD5Sum;
		File theClassFile = new File(thePath + ".class");
		File theClassFileTM = new File(thePath + ".tm");
		try
		{
			theClassFile.getParentFile().mkdirs();
			theClassFile.createNewFile();
			theClassFileTM.createNewFile();
			if (aResponse.bytecode != null) new DataOutputStream(new FileOutputStream(theClassFile))
					.write(aResponse.bytecode);
			DataOutputStream theTMData = new DataOutputStream(new FileOutputStream(theClassFileTM));
			for (int theI = 0; theI < aResponse.tracedMethods.length; theI++)
			{
				theTMData.writeInt(aResponse.tracedMethods[theI]);
			}
			itsConfig.log(1, "Cached Files for " + aName);
		}
		catch (IOException e)
		{
			itsConfig.log(1, "Problem while writing cache files of " + aName);
			e.printStackTrace();
		}
	}

	private void registerTracedMethod(int aId)
	{
		TracedMethods.setTraced(aId);
	}

	/**
	 * Sends the traced methods buffer.
	 */
	public void flushTmpTracedMethods()
	{
		for (Integer theId : itsTmpTracedMethods) registerTracedMethod(theId);
		itsTmpTracedMethods = null;
	}

	private void registerTracedMethods(int[] aIds)
	{
		if (aIds.length == 0) return;

		if (TodAgent.isVmStarted())
		{
			itsConfig.logf(1, "Registering %d traced methods", aIds.length);
			for (int theId : aIds) registerTracedMethod(theId);
		}
		else
		{
			itsConfig.logf(1, "Buffering %d traced methods, will register later", aIds.length);
			for (int theId : aIds)
				itsTmpTracedMethods.add(theId);
		}
	}

	
}