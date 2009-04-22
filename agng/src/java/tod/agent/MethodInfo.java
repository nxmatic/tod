package tod.agent;

public class MethodInfo
{
	public final String methodName;
	public final String methodSignature;
	public final String declaringClassSignature;
	
	public MethodInfo(
		String aMethodName, 
		String aMethodSignature, 
		String aDeclaringClassSignature)
	{
		methodName = aMethodName;
		methodSignature = aMethodSignature;
		declaringClassSignature = aDeclaringClassSignature;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((declaringClassSignature == null) ? 0 : declaringClassSignature.hashCode());
		result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result + ((methodSignature == null) ? 0 : methodSignature.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final MethodInfo other = (MethodInfo) obj;
		if (declaringClassSignature == null)
		{
			if (other.declaringClassSignature != null) return false;
		}
		else if (!declaringClassSignature.equals(other.declaringClassSignature)) return false;
		if (methodName == null)
		{
			if (other.methodName != null) return false;
		}
		else if (!methodName.equals(other.methodName)) return false;
		if (methodSignature == null)
		{
			if (other.methodSignature != null) return false;
		}
		else if (!methodSignature.equals(other.methodSignature)) return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		return methodName+"("+methodSignature+") in "+declaringClassSignature;
	}
	
}
