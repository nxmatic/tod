package tod.impl.bci.asm;

import org.objectweb.asm.MethodAdapter;

import zz.utils.ArrayStack;
import zz.utils.Stack;

/**
 * This visitor acts as a filter before a {@link CCMethodVisitor}. It permits
 * to differentiate between instantiations and constructor chaining
 * @author gpothier
 */
public class InstantiationAnalyserVisitor extends MethodAdapter
{
	private CCMethodVisitor mv;
	private Stack<InstantiationAnalyserVisitor.InstantiationInfo> itsInstantiationInfoStack = new ArrayStack<InstantiationAnalyserVisitor.InstantiationInfo>();
	private final ASMMethodInfo itsMethodInfo;
	
	public InstantiationAnalyserVisitor(CCMethodVisitor mv, ASMMethodInfo aMethodInfo)
	{
		super (mv);
		this.mv = mv;
		itsMethodInfo = aMethodInfo;
	}

	@Override
	public void visitTypeInsn(int aOpcode, String aDesc)
	{
		if (aOpcode == LogBCIVisitor.NEW)
		{
			itsInstantiationInfoStack.push(new InstantiationInfo(aDesc));
		}
		mv.visitTypeInsn(aOpcode, aDesc);
	}
	
	@Override
	public void visitMethodInsn(int aOpcode, String aOwner, String aName, String aDesc)
	{
		if (aOpcode == LogBCIVisitor.INVOKESPECIAL && "<init>".equals(aName))
		{
			// We are invoking a constructor.
			
			String theCalledType = aOwner;
			
			if ("<init>".equals(itsMethodInfo.getName()))
			{
				// We are in a constructor
				boolean theSuper = theCalledType.equals(itsMethodInfo.getOwnerSuper());
				boolean theThis = theCalledType.equals(itsMethodInfo.getOwner());
				
				if (theSuper || theThis)
				{
					// Potential call to super or this
					assert theSuper != theThis;
					InstantiationAnalyserVisitor.InstantiationInfo theInfo = itsInstantiationInfoStack.peek();
					if (theInfo == null || ! theInfo.getTypeName().equals(theCalledType))
					{
						mv.visitSuperOrThisCallInsn(aOpcode, aOwner, aName, aDesc, theSuper);
						return;
					}
				}
			}
			
			InstantiationAnalyserVisitor.InstantiationInfo theInfo = itsInstantiationInfoStack.pop();
			if (! theInfo.getTypeName().equals(theCalledType))
				throw new RuntimeException(String.format(
						"[LogBCIVisitor] Type mismatch in %s.%s (found %s, expected %s)",
						itsMethodInfo.getOwner(),
						itsMethodInfo.getName(),
						theCalledType,
						theInfo.getTypeName()));
			
			mv.visitConstructorCallInsn(aOpcode, aOwner, aName, aDesc);
			return;
		}
		
		mv.visitMethodInsn(aOpcode, aOwner, aName, aDesc);
	}

	private static class InstantiationInfo
	{
		/**
		 * Name of the instantiated type
		 */
		private String itsTypeName;
		
		public InstantiationInfo(String aTypeName)
		{
			itsTypeName = aTypeName; 
		}

		public String getTypeName()
		{
			return itsTypeName;
		}
	}
}