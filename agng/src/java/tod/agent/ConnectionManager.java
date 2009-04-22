/*
 * Created on Dec 15, 2007
 */
package tod.agent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import tod.core.bci.NativeAgentPeer;

/**
 * Manages the connection between the database and the agent
 * @author gpothier
 */
public class ConnectionManager
{
	private final NativeAgentConfig itsConfig;
	private DataInputStream in;
	private DataOutputStream out;

	public ConnectionManager(NativeAgentConfig aConfig)
	{
		itsConfig = aConfig;
	}
	
	public synchronized void connect()
	{
		Socket theSocket = null;
		
		try
		{
			itsConfig.log(1, "Connecting to "+itsConfig.getHost()+":"+itsConfig.getCollectorPort());
			theSocket = new Socket(itsConfig.getHost(), itsConfig.getCollectorPort());
			in = new DataInputStream(theSocket.getInputStream());
			out = new DataOutputStream(theSocket.getOutputStream());
		}
		catch (IOException e)
		{
			System.out.println("Could not connect to "+itsConfig.getHost()+":"+itsConfig.getCollectorPort()+", aborting.");
			System.exit(1);
		}
		
		try
		{
			//send CNX TYPE
			itsConfig.log(1, "Sending connection type");
			out.writeInt(AgentConfig.CNX_NATIVE);
			// Send host name
			itsConfig.log(1, "Sending host name: "+itsConfig.getHostName());
			out.writeUTF(itsConfig.getHostName());
			out.flush();
			
			// Read assigned host id
			int theHostId = in.readInt();
			itsConfig.log(1, "Assigned host id: "+theHostId);
			itsConfig.setHostId(theHostId);
			
			// Process configuration
			boolean theDone = false;
			while(! theDone)
			{
				int cmd = in.readByte();
				switch(cmd)
				{
				case NativeAgentPeer.SET_SKIP_CORE_CLASSES:
					itsConfig.setSkipCoreClasses(in.readByte() != 0);
					itsConfig.logf(1, "Skipping core classes: %s", itsConfig.getSkipCoreClasses() ? "Yes" : "No");
					break;

				case NativeAgentPeer.SET_CAPTURE_EXCEPTIONS:
					itsConfig.setCaptureExceptions(in.readByte() != 0);
					itsConfig.logf(1, "Capture exceptions: %s", itsConfig.getCaptureExceptions() ? "Yes" : "No");
					break;
					
				case NativeAgentPeer.SET_HOST_BITS:
					itsConfig.setHostBits(in.readByte());
					itsConfig.logf(1, "Host bits: %d", itsConfig.getHostBits());
					break;

				case NativeAgentPeer.SET_WORKING_SET:
					if (itsConfig.getWorkingSet()!=null)
						in.readUTF();
					else itsConfig.setWorkingSet(in.readUTF());
					itsConfig.logf(1, "Working set: %s", itsConfig.getWorkingSet());
					break;

				case NativeAgentPeer.SET_STRUCTDB_ID:
					itsConfig.setStructDbId(in.readUTF());
					itsConfig.logf(1, "Structure database id.: %s", itsConfig.getStructDbId());
					break;

				case NativeAgentPeer.CONFIG_DONE:
					// Check host id vs host bits
					if (itsConfig.getHostId() >= (1 << itsConfig.getHostBits()))
					{
						System.err.println("Host id overflow.");
						System.exit(1);
					}
					
					itsConfig.logf(1, "Config done.\n");
					theDone = true;
					break;
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		
	}
	
	/**
	 * Sends an instrumentation request to the database
	 * @param aName Name of the class
	 * @param aOriginal Original bytecode
	 * @return Instrumentation info for the class. This method never returns null,
	 * but if the class needs not be instrumented, the bytecode array of the returned 
	 * {@link InstrumentationResponse} will be null.
	 */
	public synchronized InstrumentationResponse sendInstrumentationRequest(String aName, byte[] aOriginal)
	{
		try
		{
			//Send request
			out.writeByte(NativeAgentPeer.INSTRUMENT_CLASS);
			out.writeUTF(aName);
			
			out.writeInt(aOriginal.length);
			out.write(aOriginal);
			out.flush();
			
			// Read instrumented bytecode
			int len = in.readInt();
			itsConfig.logf(2, "[TOD] Response from database for class %s: %d bytes", aName, len);
			
			if (len > 0)
			{
				byte[] theBytecode = new byte[len];
				in.readFully(theBytecode);
				
				int theTracedMethodsCount = in.readInt();
				int[] theTracedMethods = new int[theTracedMethodsCount];
				for(int i=0;i<theTracedMethodsCount;i++) theTracedMethods[i] = in.readInt();
				
				return new InstrumentationResponse(theBytecode, theTracedMethods);
			}
			else if (len == 0)
			{
				// The database says that this class should not be instrumented
				return new InstrumentationResponse();
			}
			else if (len == -1)
			{
				// The database reported a fatal error.
				String theError = in.readUTF();
				System.err.println(theError);
				System.exit(1);
			}
			else
			{
				throw new RuntimeException("Instrumentation response not handled: "+len);
			}
		}
		catch (IOException e)
		{
			System.err.println("Lost connection to the database:");
			e.printStackTrace();
			System.exit(1);
		}
		return null; // Otherwise the compiler complains
	}
	
	/**
	 * Sends a flush command to the database
	 */
	public synchronized void sendFlush()
	{
		try
		{
			out.write(NativeAgentPeer.FLUSH);
			out.flush();
			itsConfig.log(1, "Sent flush");
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Response sent by the database to an instrumentation request.
	 * @author gpothier
	 */
	public static class InstrumentationResponse
	{
		/**
		 * Bytecode of the instrumented class. 
		 */
		public final byte[] bytecode;
		
		/**
		 * Array of instrumented method ids. 
		 */
		public final int[] tracedMethods;
		
		/**
		 * Creates an empty response, meaning that the class should not be instrumented.
		 */
		public InstrumentationResponse()
		{
			this(null, new int[0]);
		}
		
		public InstrumentationResponse(byte[] aBytecode, int[] aTracedMethods)
		{
			bytecode = aBytecode;
			tracedMethods = aTracedMethods;
		}
	}
}
