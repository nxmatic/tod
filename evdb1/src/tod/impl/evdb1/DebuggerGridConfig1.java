/*
TOD - Trace Oriented Debugger.
Copyright (c) 2006-2008, Guillaume Pothier
All rights reserved.

This program is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
version 2 as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful, 
but WITHOUT ANY WARRANTY; without even the implied warranty of 
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
General Public License for more details.

You should have received a copy of the GNU General Public License 
along with this program; if not, write to the Free Software 
Foundation, Inc., 59 Temple Place, Suite 330, Boston, 
MA 02111-1307 USA

Parts of this work rely on the MD5 algorithm "derived from the 
RSA Data Security, Inc. MD5 Message-Digest Algorithm".
*/
package tod.impl.evdb1;

import tod.impl.dbgrid.db.DatabaseNode;
import tod.impl.dbgrid.db.ObjectsDatabase;
import tod.impl.dbgrid.messages.MessageType;
import tod.impl.evdb1.db.HierarchicalIndex;
import tod.impl.evdb1.db.file.HardPagedFile;
import tod.impl.evdb1.db.file.TupleIterator;
import tod.impl.evdb1.db.file.TupleWriter;
import tod.utils.ConfigUtils;
import zz.utils.bit.BitUtils;

public class DebuggerGridConfig1
{
	/**
	 * Number of bits used to represent the thread of an event.
	 */
	public static final int EVENT_THREAD_BITS = 16;
	
	/**
	 * Number of bits used to represent the depth of an event.
	 */
	public static final int EVENT_DEPTH_BITS = 16;
	
	/**
	 * Number of bits used to represent the serial number of an event.
	 */
	public static final int EVENT_TIMESTAMP_BITS = 64; 
	
	/**
	 * Number of bits used to represent a behavior id in an event.
	 */
	public static final int EVENT_PROBEID_BITS = 32;
	
	/**
	 * Number of bits used to represent a behavior id in an event.
	 */
	public static final int EVENT_BEHAVIOR_BITS = 16;
	
	/**
	 * Number of bits used to represent a type id in an event.
	 */
	public static final int EVENT_TYPE_BITS = 16; 
	
	/**
	 * Number of bits used to represent a field id in an event.
	 */
	public static final int EVENT_FIELD_BITS = 16; 
	
	/**
	 * Number of bits used to represent a variable id in an event.
	 */
	public static final int EVENT_VARIABLE_BITS = 16; 
	
	/**
	 * Number of bits used to represent the bytecode location of an event
	 */
	public static final int EVENT_BYTECODE_LOCATION_BITS = 16; 
	
	/**
	 * Number of bits used to represent the advice source id of an event
	 */
	public static final int EVENT_ADVICE_SRC_ID_BITS = 16; 
	
	/**
	 * Number of bits used to represent the number of arguments of a behavior call.
	 */
	public static final int EVENT_ARGS_COUNT_BITS = 8; 
	
	/**
	 * Number of bits used to represent the number of items in an advice cflow
	 */
	public static final int EVENT_ADCFLOW_COUNT_BITS = 4; 
	
	/**
	 * Maximum number of event types 
	 */
	public static final int STRUCTURE_TYPE_COUNT = 40000;

	/**
	 * Maximum number of threads
	 */
	public static final int STRUCTURE_THREADS_COUNT = 10000;

	/**
	 * Maximum number of different depths
	 */
	public static final int STRUCTURE_DEPTH_RANGE = BitUtils.pow2i(EVENT_DEPTH_BITS);
	
	/**
	 * Maximum number of bytecode locations
	 */
	public static final int STRUCTURE_BYTECODE_LOCS_COUNT = 65536;

	/**
	 * Maximum number of behaviors
	 */
	public static final int STRUCTURE_BEHAVIOR_COUNT = 200000;

	/**
	 * Maximum number of advice source ids.
	 */
	public static final int STRUCTURE_ADVICE_SRC_ID_COUNT = 10000;
	
	/**
	 * Maximum number of bytecode roles
	 */
	public static final int STRUCTURE_ROLE_COUNT = 10;
	
	/**
	 * Maximum number of fields
	 */
	public static final int STRUCTURE_FIELD_COUNT = 100000;

	/**
	 * Maximum number of variable indexes
	 */
	public static final int STRUCTURE_VAR_COUNT = 1000;

	/**
	 * Maximum number of objects 
	 */
	public static final int STRUCTURE_OBJECT_COUNT = BitUtils.pow2i(14);
	
	/**
	 * Maximum number of array indexes.
	 */
	public static final int STRUCTURE_ARRAY_INDEX_COUNT = BitUtils.pow2i(14);
	
	/**
	 * Number of partitions of key values for the objects index.
	 * @see SplittedConditionHandler
	 */
	public static final int[] INDEX_OBJECT_PARTS = {16, 16};

	/**
	 * Number of partitions of key values for the array index index.
	 * @see SplittedConditionHandler
	 */
	public static final int[] INDEX_ARRAY_INDEX_PARTS = {14, 14};
	

	/**
	 * Number of bits used to represent the message type
	 */
	public static final int MESSAGE_TYPE_BITS = BitUtils.log2ceil(MessageType.VALUES.length); 
	
	/**
	 * Number of bits necessary to represent an external event pointer.
	 */
	public static final int EVENTID_POINTER_SIZE = 
		+DebuggerGridConfig1.EVENT_THREAD_BITS
		+DebuggerGridConfig1.EVENT_TIMESTAMP_BITS;

	/**
	 * Size of file pages in the database
	 */
	public static final int DB_PAGE_SIZE = 4096;
	
	/**
	 * Number of bits to represent a page pointer in a linked pages list,
	 * as used by {@link TupleWriter} and {@link TupleIterator}
	 */
	public static final int DB_PAGE_POINTER_BITS = 32;
	
	/**
	 * NUmber of bits to represent an offset (in bits) in a page.
	 */
	public static final int DB_PAGE_BITOFFSET_BITS = BitUtils.log2ceil(DB_PAGE_SIZE*8);
	
	/**
	 * NUmber of bits to represent an offset (in bytes) in a page.
	 */
	public static final int DB_PAGE_BYTEOFFSET_BITS = BitUtils.log2ceil(DB_PAGE_SIZE);
	
	/**
	 * Average event size.
	 */
	public static final int DB_AVG_EVENT_SIZE = 55;
	
	public static final int DB_AVG_EVENTS_PER_PAGE = DB_PAGE_SIZE/DB_AVG_EVENT_SIZE;
	
	/**
	 * Number of bits used to represent the record index in an internal
	 * event pointer.
	 */
	public static final int DB_EVENTID_INDEX_BITS = 
		BitUtils.log2ceil(DB_AVG_EVENTS_PER_PAGE) + 1;
	
	/**
	 * Number of bits used to represent the node in an internal event
	 * pointer. 
	 */
	public static final int DB_EVENTID_NODE_BITS = 6;
	
	/**
	 * Number of bits used to represent the page in an internal
	 * event pointer.
	 */
	public static final int DB_EVENTID_PAGE_BITS = 64 - DB_EVENTID_INDEX_BITS - DB_EVENTID_NODE_BITS;

	/**
	 * Number of bits used to represent event sizes in event pages.
	 */
	public static final int DB_EVENT_SIZE_BITS = 16;
	
	/**
	 * Maximum number of index levels for {@link HierarchicalIndex}.
	 */
	public static final int DB_MAX_INDEX_LEVELS = 6;
	
	/**
	 * Maximum size allocated to page buffers.
	 * See {@link HardPagedFile.PageDataManager}
	 */
	public static final long DB_PAGE_BUFFER_SIZE = 
		ConfigUtils.readSize("page-buffer-size", getDefaultPageBufferSize());
	
	private static String getDefaultPageBufferSize()
	{
		int theSize = (int) (Runtime.getRuntime().maxMemory() / (1024*1024));
		int theBufferSize = theSize * 6 / 10;
		return theBufferSize + "m";
	}
}
