/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this 
      list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the University of Chile nor the names of its contributors 
      may be used to endorse or promote products derived from this software without 
      specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

Parts of this work rely on the MD5 algorithm "derived from the RSA Data Security, 
Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.replay;

import gnu.trove.TDoubleStack;
import gnu.trove.TFloatStack;
import gnu.trove.TIntStack;
import gnu.trove.TLongStack;
import tod.core.database.structure.ObjectId;
import zz.utils.ArrayStack;

/**
 * A stack that natively stores the various JVM primtive types.
 * @author gpothier
 */
public class PrimitiveMultiStack
{
	private final ArrayStack<ObjectId> itsRefStack = new ArrayStack<ObjectId>();
	private final TIntStack itsIntStack = new TIntStack();
	private final TDoubleStack itsDoubleStack = new TDoubleStack();
	private final TFloatStack itsFloatStack = new TFloatStack();
	private final TLongStack itsLongStack = new TLongStack();

	public void clear()
	{
		itsRefStack.clear();
		itsIntStack.clear();
		itsDoubleStack.clear();
		itsFloatStack.clear();
		itsLongStack.clear();
	}
	
	// Push to stack
	protected void refPush(ObjectId v) { itsRefStack.push(v); }
	protected void intPush(int v) { itsIntStack.push(v); }
	protected void doublePush(double v) { itsDoubleStack.push(v); }
	protected void floatPush(float v) { itsFloatStack.push(v); }
	protected void longPush(long v) { itsLongStack.push(v); }
	
	// Pop from stack
	protected ObjectId refPop() { return itsRefStack.pop(); }
	protected int intPop() { return itsIntStack.pop(); }
	protected double doublePop() { return itsDoubleStack.pop(); }
	protected float floatPop() { return itsFloatStack.pop(); }
	protected long longPop() { return itsLongStack.pop(); }
	
	// Peek stack
	protected ObjectId refPeek() { return itsRefStack.peek(); }
	protected int intPeek() { return itsIntStack.peek(); }
	protected double doublePeek() { return itsDoubleStack.peek(); }
	protected float floatPeek() { return itsFloatStack.peek(); }
	protected long longPeek() { return itsLongStack.peek(); }
}
