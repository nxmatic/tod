package pytod.core.server;

import hep.io.xdr.XDRInputStream;

import java.io.IOException;
import java.net.Socket;

import pytod.core.ValueWriter;

import tod.core.ILogCollector;
import tod.core.config.TODConfig;
import tod.core.database.structure.IMutableBehaviorInfo;
import tod.core.database.structure.IMutableClassInfo;
import tod.core.database.structure.IMutableStructureDatabase;
import tod.core.database.structure.ObjectId;
import tod.core.database.structure.IStructureDatabase.LineNumberInfo;
import tod.core.database.structure.IStructureDatabase.LocalVariableInfo;
import tod.core.server.TODServer;
import zz.utils.properties.IRWProperty;
import zz.utils.properties.SimpleRWProperty;

/**
 * A Python TOD server accepts connections from debugged script Python and process instrumentation
 * requests as well as logged events.
 * The actual implementation of the instrumenter and database are left
 * to delegates.
 * @author minostro
 */

public class PythonTODServer extends TODServer
{
	private final IMutableStructureDatabase itsStructureDatabase;
	private final ILogCollector itsLogCollector;
	
	private final IRWProperty<Boolean> pCaptureEnabled = 
		new SimpleRWProperty<Boolean>(this, (Boolean) null)
		{
			@Override
			protected Object canChange(Boolean aOldValue, Boolean aNewValue)
			{
				return aNewValue == null ? ACCEPT : REJECT;
			}
		};

	public PythonTODServer(
			TODConfig aConfig,
			IMutableStructureDatabase aStructureDatabase,
			ILogCollector aLogCollector) 
	{
		super(aConfig);
		itsStructureDatabase = aStructureDatabase;
		itsLogCollector = aLogCollector;
		//System.out.println("Hola soy Pilton");
		//Dirty trick proposed by Guillaume
		IMutableClassInfo theClass = itsStructureDatabase.addClass(100, "functionClass");
	}

	@Override
	protected void accepted(Socket aSocket) 
	{
		try
		{
			XDRInputStream theStream = new XDRInputStream(aSocket.getInputStream());
			new Receiver(theStream);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public IRWProperty<Boolean> pCaptureEnabled()
	{
		return pCaptureEnabled;
	}
	
	//events
	private static final int REGISTER_EVENT = 0;
	private static final int CALL_EVENT = 1;
	private static final int SET_EVENT = 2;
	private static final int RETURN_EVENT = 3;
	private static final int INSTANTIATION_EVENT = 4;	
	//objects
	private static final int OBJECT_CLASS = 0;
	private static final int OBJECT_METHOD = 1;
	private static final int OBJECT_ATTRIBUTE = 2;
	private static final int OBJECT_FUNCTION = 3;
	private static final int OBJECT_LOCAL = 4;
	private static final int OBJECT_PROBE = 5;
	private static final int OBJECT_THREAD = 6;
	private static final int OBJECT_STATICFIELD = 7;
	private static final int OBJECT_OBJECT = 8;
	private static final int OBJECT_EXCEPTION = 9;
	private static final int OBJECT_SPECIALMETHOD = 10;
	//dataTypes
	private static final int DATA_INT = 0;
	private static final int DATA_STR = 1;
	private static final int DATA_FLOAT = 2;
	private static final int DATA_LONG = 3;
	private static final int DATA_BOOL = 4;
	private static final int DATA_TUPLE = 5;
	private static final int DATA_LIST = 6;
	private static final int DATA_DICT = 7;
	private static final int DATA_OTHER = 8;
	
	
	private class Receiver extends Thread
	{
		private XDRInputStream itsStream;
		
		
		public Receiver(XDRInputStream aInputStream)
		{
			super("PythonTODServer.Receiver");
			itsStream = aInputStream;
			start();
		}

		private String generateSignature(int aArgsCount)
		{
			StringBuilder theSignatureBuilder = new StringBuilder("(");
			for(int i=0;i<aArgsCount;i++) theSignatureBuilder.append("Ljava.lang.Object;");
			theSignatureBuilder.append(")Ljava.lang.Object;");
			
			return theSignatureBuilder.toString();
		}

		public void registerFunction(XDRInputStream aInputStream)
		{
			IMutableClassInfo theClass;
			IMutableBehaviorInfo theBehavior;
			try
			{
				int theFunctionId = aInputStream.readInt();
				String theFunctionName = new String(aInputStream.readString());
				int theArgsCount = aInputStream.readInt();
				theClass = itsStructureDatabase.getClass(100, true);
				theBehavior = theClass.addBehavior(theFunctionId, theFunctionName, generateSignature(theArgsCount), false);
				if (theArgsCount > 0){
					for(int i=0;i<theArgsCount;i=i+1)
					{
						String theArgName = new String(aInputStream.readString());						
						int theArgId = aInputStream.readInt();
						theBehavior.addLocalVariableInfo(new LocalVariableInfo(0, 32000, theArgName, "java.lang.Object", theArgId));
						System.out.println("Registrando variable local "+theArgName);
					}
				}
				String theFileName = aInputStream.readString();
				int theCodeSize = aInputStream.readInt();
				int theLineNumbers = aInputStream.readInt();
				int theStartPc = -1;
				int theLineNumber = -1;
				LineNumberInfo[] theLineNumberInfo = new LineNumberInfo[theLineNumbers];
				for (int i = 0; i < theLineNumbers; i++) {
					theStartPc = aInputStream.readInt();
					theLineNumber = aInputStream.readInt();
					theLineNumberInfo[i] = new LineNumberInfo((short) theStartPc, (short) theLineNumber);
				}
				theBehavior.setup(true, null, theCodeSize, theLineNumberInfo, null);
				System.out.println("Registrando la funcion "+theFunctionName + "id = "+theFunctionId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}
		
		public void registerLocal(XDRInputStream aInputStream)
		{
			IMutableBehaviorInfo theBehavior;			
			try
			{
				int theLocalId = aInputStream.readInt();
				int theParentId = aInputStream.readInt();
				String theLocalName = new String(aInputStream.readString());
				//for this moment only register locals of methods
				//TODO: guillaume must write {I hope} a handler for functions
				System.out.println(theLocalId);
				System.out.println(theParentId);
				System.out.println(theLocalName);
				theBehavior = itsStructureDatabase.getBehavior(theParentId, true);
				theBehavior.addLocalVariableInfo(new LocalVariableInfo(0, 32000, theLocalName, "java.lang.Object", theLocalId));
				System.out.println("Registrando variable local "+theLocalName);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}

		public void registerClass(XDRInputStream aInputStream)
		{
			IMutableClassInfo theClass;
			try
			{
				int theClassId = aInputStream.readInt();
				System.out.println(theClassId);
				String theClassName = new String(aInputStream.readString());
				//TODO: change register format class  without field classBases
				int theClassBases = aInputStream.readInt();
				theClass = itsStructureDatabase.addClass(theClassId, theClassName);
				System.out.println("Registrando clase "+theClassName);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			
		}
		
		public void registerStaticField(XDRInputStream aInputStream)
		{
			IMutableClassInfo theClass;
			try
			{
				int theAttributeId = aInputStream.readInt();
				int theParentId = aInputStream.readInt();
				String theAttributeName = new String(aInputStream.readString());	
				theClass = itsStructureDatabase.getClass(theParentId, true);
				theClass.addField(theAttributeId, theAttributeName, null, true);
				System.out.println("Registrando un atributo estatico con id "+theAttributeId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}	
		}

		public void registerMethod(XDRInputStream aInputStream)
		{
			IMutableClassInfo theClass;
			IMutableBehaviorInfo theBehavior;
			try
			{
				int theMethodId = aInputStream.readInt();
				int theClassId = aInputStream.readInt();
				String theMethodName = aInputStream.readString();
				int theArgsCount = aInputStream.readInt();
				theClass = itsStructureDatabase.getClass(theClassId, true);
				theBehavior = theClass.addBehavior(theMethodId, theMethodName, generateSignature(theArgsCount), false);
				if (theArgsCount > 0){
					for(int i=0;i<theArgsCount;i=i+1)
					{
						String theArgName = aInputStream.readString();
						int theArgId = aInputStream.readInt();
						theBehavior.addLocalVariableInfo(new LocalVariableInfo(0, 32000, theArgName, "java.lang.Object", theArgId));
						System.out.println("Registrando variable local "+theArgName);
					}
				}
				String theFileName = aInputStream.readString();
				theBehavior.setSourceFile(theFileName);
				int theCodeSize = aInputStream.readInt();
				int theLineNumbers = aInputStream.readInt();
				int theStartPc = -1;
				int theLineNumber = -1;
				LineNumberInfo[] theLineNumberInfo = new LineNumberInfo[theLineNumbers];
				for (int i = 0; i < theLineNumbers; i++) {
					theStartPc = aInputStream.readInt();
					theLineNumber = aInputStream.readInt();
					theLineNumberInfo[i] = new LineNumberInfo((short) theStartPc, (short) theLineNumber);
				}
				theBehavior.setup(true, null, theCodeSize, theLineNumberInfo, null);
				System.out.println("Registrando el metodo "+theMethodName + "id = "+theMethodId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}	
		
		public void registerSpecialMethod(XDRInputStream aInputStream)
		{
			IMutableClassInfo theClass;
			IMutableBehaviorInfo theBehavior;
			try
			{
				int theMethodId = aInputStream.readInt();
				int theClassId = aInputStream.readInt();
				String theMethodName = aInputStream.readString();
				theClass = itsStructureDatabase.getClass(theClassId, true);
				theBehavior = theClass.addBehavior(theMethodId, theMethodName, generateSignature(0), false);
				String theFileName = aInputStream.readString();
				theBehavior.setSourceFile(theFileName);
				//theBehavior.setup(true, null, theCodeSize, theLineNumberInfo, null);
				System.out.println("Registrando el SpecialMetodo "+theMethodName + "id = "+theMethodId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}		
		
		public void registerAttribute(XDRInputStream aInputStream)
		{
			IMutableClassInfo theClass;
			try
			{
				int theAttributeId = aInputStream.readInt();
				int theParentId = aInputStream.readInt();
				String theAttributeName = new String(aInputStream.readString());	
				theClass = itsStructureDatabase.getClass(theParentId, true);
				theClass.addField(theAttributeId, theAttributeName, null, true);
				System.out.println("Registrando un atributo con id "+theAttributeId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}

		public void registerProbe(XDRInputStream aInputStream)
		{
			try
			{
				int theProbeId = aInputStream.readInt();
				int theParentId = aInputStream.readInt();				
				int theProbeCurrentLasti = aInputStream.readInt();
				int theCurrentLineno = aInputStream.readInt();
				itsStructureDatabase.addProbe(theProbeId, theParentId, theProbeCurrentLasti, null, 0);
				System.out.println("Registrando probe "+theProbeId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}
		
		public void registerThread(XDRInputStream aInputStream)
		{
			try
			{
				int theThreadId = aInputStream.readInt();
				int theSysId = aInputStream.readInt();
				itsLogCollector.thread(theThreadId, theSysId, "()V");
				System.out.println("Registrando thread");
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}

		public void registerObject(XDRInputStream aInputStream)
		{
			try
			{
				int theTypeId = aInputStream.readInt();
				long theId = aInputStream.readLong();
				Object theValue = getObjectValue(theTypeId, aInputStream);
				System.out.println(theValue);
				long theCurrentTimestamp = aInputStream.readLong();
				byte[] theByteValue = ValueWriter.serialize(theValue);
				itsLogCollector.register(
						theId,
						theByteValue, 
						theCurrentTimestamp, 
						true);
				System.out.println("Registrando un nuevo objeto: " + theId + " - " + theValue);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}

		public void registerException(XDRInputStream aInputStream)
		{
			try
			{
				Object theValue = null;
				long theValueId = 0;
				int theTypeId = aInputStream.readInt();
				if (theTypeId == 1) {
					theValueId = aInputStream.readLong();
				}
				else{
					theValue = getObjectValue(theTypeId, aInputStream);					
				}
				int theProbeId = aInputStream.readInt();
				long theParentTimestamp = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();
				itsLogCollector.exception(
						theThreadId, 
						theParentTimestamp,
						(short) theDepth, 
						theCurrentTimestamp, 
						null, 
						theProbeId, 
						theTypeId == 1 ? new ObjectId(theValueId) : theValue);
				System.out.println("Registrando exception 3");
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}		
		
		
		public void instantiationEvent(XDRInputStream aInputStream)
		{
			Object args[] = null;
			try
			{
				int theBehaviorId = aInputStream.readInt();
				int theTargetId = aInputStream.readInt();
				int theArgsCount = aInputStream.readInt();
				if (theArgsCount > 0){
					args = new Object[theArgsCount];
					for(int i=0;i<theArgsCount;i=i+1)
					{
						int theArgType = aInputStream.readInt();
						if (theArgType == 1) {
							long theValueId = aInputStream.readLong();
							args[i] = new ObjectId(theValueId);
						} 
						else {
							Object theValue = getObjectValue(theArgType, aInputStream);
							args[i] = theValue;
						}
					}
				}
				int theProbeId = aInputStream.readInt();
				long theParentTimestamp = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();
				itsLogCollector.instantiation(
						theThreadId,
						theParentTimestamp,
						(short)theDepth, 
						theCurrentTimestamp, 
						null,
						theProbeId,
						true, 
						-1,
						theBehaviorId,
						new ObjectId(theTargetId),
						args);
				System.out.println("instanciacion "+ theTargetId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		public void methodCall(XDRInputStream aInputStream)
		{
			Object theArgs[] = null;
			try
			{
				int theMethodId = aInputStream.readInt();
				int theTargetId = aInputStream.readInt();
				int theArgsCount = aInputStream.readInt();
				if (theArgsCount > 0){
					theArgs = new Object[theArgsCount];
					for(int i=0;i<theArgsCount;i=i+1)
					{
						int theArgType = aInputStream.readInt();
						if (theArgType == 1) {
							long theValueId = aInputStream.readLong();
							theArgs[i] = new ObjectId(theValueId);
						} 
						else {
							Object theValue = getObjectValue(theArgType, aInputStream);
							theArgs[i] = theValue;
						}
					}
				}
				int theProbeId = aInputStream.readInt();
				long theParentTimestamp = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();
				itsLogCollector.methodCall(
						theThreadId,
						theParentTimestamp,
						(short)theDepth, 
						theCurrentTimestamp, 
						null,
						theProbeId,
						true, 
						-1,
						theMethodId,
						new ObjectId(theTargetId),
						theArgs);
				System.out.println("llamando a método "+ theMethodId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		public void functionCall(XDRInputStream aInputStream)
		{
			Object theArgs[] = null;
			try
			{
				int theFunctionId = aInputStream.readInt();
				int theArgsCount = aInputStream.readInt();
				if (theArgsCount > 0){
					theArgs = new Object[theArgsCount];
					for(int i=0;i<theArgsCount;i=i+1)
					{
						int theArgType = aInputStream.readInt();
						if (theArgType == 1) {
							long theValueId = aInputStream.readLong();
							theArgs[i] = new ObjectId(theValueId);
						} else {
							Object theValue = getObjectValue(theArgType, aInputStream);
							theArgs[i] = theValue;

						}
					}
				}
				int theProbeId = aInputStream.readInt();
				long theParentTimestamp = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();
				//TODO: Guillaume must be write a handler for function
				itsLogCollector.methodCall(
						theThreadId,
						theParentTimestamp,
						(short)theDepth, 
						theCurrentTimestamp, 
						null,
						theProbeId,
						false, 
						-1,
						theFunctionId,
						null,
						theArgs);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		public Object getObjectValue(int aTypeId, XDRInputStream aInputStream)
		{
			try {
				Object theValue;
				switch (aTypeId) {
				case DATA_INT:
				{
					theValue = aInputStream.readInt();
					break;
				}
				case DATA_STR:
				{
					theValue = aInputStream.readString();
					break;
				}
				case DATA_FLOAT:
				{
					theValue = aInputStream.readFloat();
					break;
				}
				case DATA_LONG:
				{
					theValue = aInputStream.readLong();
					break;
				}
				case DATA_BOOL:
				{
					//theValue = aInputStream.readBoolean();
					theValue = aInputStream.readInt();
					if (theValue.equals(1)) {
						theValue = Boolean.TRUE;
					}
					else {
						theValue = Boolean.FALSE;
					}
					break;
				}
				case DATA_TUPLE:
				{
					theValue = aInputStream.readInt();
					break;
				}
				case DATA_LIST:
				{
					theValue = aInputStream.readInt();
					break;
				}
				case DATA_DICT:
				{
					theValue = aInputStream.readInt();
					break;
				}
				case DATA_OTHER:
				{
					theValue = aInputStream.readInt();
					break;
				}
				default:
					theValue = aInputStream.readInt();					
					break;
				}
				return theValue;
			} 
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		public void setStaticField(XDRInputStream aInputStream)
		{
			try
			{
				Object theValue = null;
				long theValueId = 0;
				int theStaticFieldId = aInputStream.readInt();
				int theTypeId = aInputStream.readInt();
				if (theTypeId == 1) {
					theValueId = aInputStream.readLong();
				} else {
					theValue = getObjectValue(theTypeId, aInputStream);
				}
				int theProbeId = aInputStream.readInt();
				long theParentTimestampFrame = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();
				itsLogCollector.fieldWrite(
						theThreadId, 
						theParentTimestampFrame, 
						(short)theDepth, 
						theCurrentTimestamp, 
						null, 
						theProbeId, 
						theStaticFieldId, 
						null, 
						theTypeId == 1 ? new ObjectId(theValueId) : theValue);
				System.out.println("modificando atributo estatico"+ theStaticFieldId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}
		
		public void setAttribute(XDRInputStream aInputStream)
		{
			try
			{
				Object theValue = null;
				long theValueId = 0;
				int attributeId = aInputStream.readInt();
				int targetId = aInputStream.readInt();
				int theTypeId = aInputStream.readInt();
				if (theTypeId == 1) {
					theValueId = aInputStream.readLong();
				} else {
					theValue = getObjectValue(theTypeId, aInputStream);
				}
				int probeId = aInputStream.readInt();
				long parentTimeStampFrame = aInputStream.readLong();
				int depth = aInputStream.readInt();
				long currentTimeStamp = aInputStream.readLong();
				int threadId = aInputStream.readInt();
				itsLogCollector.fieldWrite(
						threadId, 
						parentTimeStampFrame, 
						(short)depth, 
						currentTimeStamp, 
						null, 
						probeId, 
						attributeId, 
						new ObjectId(targetId), 
						theTypeId == 1 ? new ObjectId(theValueId) : theValue);
				System.out.println("modificando atributo "+ attributeId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}
		
		
		public void setLocal(XDRInputStream aInputStream)
		{
			try
			{
				Object theValue = null;
				long theValueId = 0;
				int theLocalId = aInputStream.readInt();
				int theParentId = aInputStream.readInt();
				int theTypeId = aInputStream.readInt();
				if (theTypeId == 1) {
					theValueId = aInputStream.readLong();
				} else {
					theValue = getObjectValue(theTypeId, aInputStream);
				}
				int theProbeId = aInputStream.readInt();
				long theParentTimestamp = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();				
				itsLogCollector.localWrite(
						theThreadId,
						theParentTimestamp, 
						(short)theDepth, 
						theCurrentTimestamp, 
						null, 
						theProbeId, 
						theLocalId, 
						theTypeId == 1 ? new ObjectId(theValueId) : theValue);
				System.out.println("modificando variable "+ theLocalId);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}			

		}
		
		public void returnEvent(XDRInputStream aInputStream)
		{
			try
			{
				long theValueId = 0;
				Object theValue = null;
				int theBehaviorId = aInputStream.readInt();
				int typeId = aInputStream.readInt();
				if (typeId == 1) {
					theValueId = aInputStream.readInt() & 0xffffffffL;
				}
				else{
					theValue = getObjectValue(typeId, aInputStream);					
				}
				int theHasThrown = aInputStream.readInt();
				int theProbeId = aInputStream.readInt();
				long theParentTimestamp = aInputStream.readLong();
				int theDepth = aInputStream.readInt();
				long theCurrentTimestamp = aInputStream.readLong();
				int theThreadId = aInputStream.readInt();
				itsLogCollector.behaviorExit(
						theThreadId, 
						theParentTimestamp, 
						(short)theDepth, 
						theCurrentTimestamp, 
						null,
						theProbeId, 
						theBehaviorId,
						theHasThrown == 1? true : false,
						typeId == 1 ? new ObjectId(theValueId) : theValue);
				System.out.println("Registrando return");
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void run()
		{
			try
			{
				while (true)
				{
					int theEvent = itsStream.readInt();
					switch (theEvent)
					{
					case REGISTER_EVENT:
					{
						int theObject = itsStream.readInt();
						switch(theObject)
						{
						case OBJECT_CLASS:
							registerClass(itsStream);
							break;
						case OBJECT_METHOD:
							registerMethod(itsStream);
							break;
						case OBJECT_ATTRIBUTE:
							registerAttribute(itsStream);						
							break;
						case OBJECT_FUNCTION:
							registerFunction(itsStream);
							break;
						case OBJECT_LOCAL:
							registerLocal(itsStream);
							break;
						case OBJECT_PROBE:
							registerProbe(itsStream);
							break;
						case OBJECT_THREAD:
							registerThread(itsStream);
							break;
						case OBJECT_STATICFIELD:
							registerStaticField(itsStream);
							break;
						case OBJECT_OBJECT:
							registerObject(itsStream);
							break;
						case OBJECT_EXCEPTION:
							registerException(itsStream);
							break;
						case OBJECT_SPECIALMETHOD:
							registerSpecialMethod(itsStream);
							break;
						default:
							break;
						}	
					}
					break;
					case CALL_EVENT:
					{
						int theObject = itsStream.readInt();
						switch (theObject)
						{
						case OBJECT_METHOD:
							methodCall(itsStream);
							break;
						case OBJECT_FUNCTION:
							functionCall(itsStream);
							break;
						default:
							break;
						}
					}
					break;
					case SET_EVENT:
					{
						int theObject = itsStream.readInt();
						switch (theObject)
						{
						case OBJECT_STATICFIELD:
							setStaticField(itsStream);
							break;
						case OBJECT_ATTRIBUTE:
							setAttribute(itsStream);
							break;
						case OBJECT_LOCAL:
							setLocal(itsStream);
							break;
						default:
							break;
						}
					}
					break;	
					case RETURN_EVENT:
						returnEvent(itsStream);
						break;
					case INSTANTIATION_EVENT:
						instantiationEvent(itsStream);
						break;
					default:
						break;
					}
				}
			}
			catch (IOException e)
			{
				itsLogCollector.flush();
				throw new RuntimeException(e);
			}
		}	
	}
}
