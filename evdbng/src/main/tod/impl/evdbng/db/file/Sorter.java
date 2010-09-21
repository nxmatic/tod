package tod.impl.evdbng.db.file;

/**
 * A copy of Java's Arrays sorting algorithm, but allows to override the swap
 * method so that matched arrays (for instance) can be sorted efficiently.
 * The {@link #mergeSort()} methods perform a stable sort, but do not sort in place 
 * (although the API hides this).
 * 
 * @author gpothier
 */
public abstract class Sorter
{
	public void sort(int[] a)
	{
		sort(a, 0, a.length);
	}

	public void sort(long[] a)
	{
		sort(a, 0, a.length);
	}

	/**
	 * Sorts the specified sub-array of longs into ascending order.
	 */
	public void sort(long x[], int off, int len)
	{
		// Insertion sort on smallest arrays
		if (len < 7)
		{
			for (int i = off; i < len + off; i++)
				for (int j = i; j > off && x[j - 1] > x[j]; j--)
					swap0(x, j, j - 1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1); // Small arrays, middle element
		if (len > 7)
		{
			int l = off;
			int n = off + len - 1;
			if (len > 40)
			{ // Big arrays, pseudomedian of 9
				int s = len / 8;
				l = med3(x, l, l + s, l + 2 * s);
				m = med3(x, m - s, m, m + s);
				n = med3(x, n - 2 * s, n - s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		long v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while (true)
		{
			while (b <= c && x[b] <= v)
			{
				if (x[b] == v) swap0(x, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v)
			{
				if (x[c] == v) swap0(x, c, d--);
				c--;
			}
			if (b > c) break;
			swap0(x, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(x, off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(x, b, n - s, s);

		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) sort(x, off, s);
		if ((s = d - c) > 1) sort(x, n - s, s);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private void swap0(long x[], int a, int b)
	{
		swap(x, a, b);
		swap(a, b);
	}

	public static void swap(long x[], int a, int b)
	{
		long t = x[a];
		x[a] = x[b];
		x[b] = t;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private void vecswap(long x[], int a, int b, int n)
	{
		for (int i = 0; i < n; i++, a++, b++)
			swap0(x, a, b);
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(Sortable x, int a, int b, int n)
	{
		for (int i = 0; i < n; i++, a++, b++)
			x.swap(a, b);
	}
	
	/**
	 * Returns the index of the median of the three indexed longs.
	 */
	private static int med3(long x[], int a, int b, int c)
	{
		return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}

	/**
	 * Returns the index of the median of the three indexed longs.
	 */
	private static int med3(Sortable x, int a, int b, int c)
	{
		return (x.compare(a, b) < 0 ? 
				(x.compare(b, c) < 0 ? b : x.compare(a, c) < 0 ? c : a)
				: (x.compare(b, c) > 0 ? b : x.compare(a, c) > 0 ? c : a));
	}
	
	/**
	 * Sorts the specified sub-array of integers into ascending order.
	 */
	public void sort(int x[], int off, int len)
	{
		// Insertion sort on smallest arrays
		if (len < 7)
		{
			for (int i = off; i < len + off; i++)
				for (int j = i; j > off && x[j - 1] > x[j]; j--)
					swap0(x, j, j - 1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1); // Small arrays, middle element
		if (len > 7)
		{
			int l = off;
			int n = off + len - 1;
			if (len > 40)
			{ // Big arrays, pseudomedian of 9
				int s = len / 8;
				l = med3(x, l, l + s, l + 2 * s);
				m = med3(x, m - s, m, m + s);
				n = med3(x, n - 2 * s, n - s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		int v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while (true)
		{
			while (b <= c && x[b] <= v)
			{
				if (x[b] == v) swap0(x, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v)
			{
				if (x[c] == v) swap0(x, c, d--);
				c--;
			}
			if (b > c) break;
			swap0(x, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(x, off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(x, b, n - s, s);

		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) sort(x, off, s);
		if ((s = d - c) > 1) sort(x, n - s, s);
	}

	/**
	 * Sorts the specified sub-array of integers into ascending order.
	 */
	public static void sort(Sortable x, int off, int len)
	{
		// Insertion sort on smallest arrays
		if (len < 7)
		{
			for (int i = off; i < len + off; i++)
				for (int j = i; j > off && x.compare(j-1, j) > 0; j--)
					x.swap(j, j-1);
			return;
		}
		
		// Choose a partition element, v
		int m = off + (len >> 1); // Small arrays, middle element
		if (len > 7)
		{
			int l = off;
			int n = off + len - 1;
			if (len > 40)
			{ // Big arrays, pseudomedian of 9
				int s = len / 8;
				l = med3(x, l, l + s, l + 2 * s);
				m = med3(x, m - s, m, m + s);
				n = med3(x, n - 2 * s, n - s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		x.setPivot(m);
		
		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while (true)
		{
			while (b <= c && x.compare(b, Sortable.PIVOT) <= 0)
			{
				if (x.compare(b, Sortable.PIVOT) == 0) x.swap(a++, b);
				b++;
			}
			while (c >= b && x.compare(c, Sortable.PIVOT) >= 0)
			{
				if (x.compare(c, Sortable.PIVOT) == 0) x.swap(c, d--);
				c--;
			}
			if (b > c) break;
			x.swap(b++, c--);
		}
		
		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a - off, b - a);
		vecswap(x, off, b - s, s);
		s = Math.min(d - c, n - d - 1);
		vecswap(x, b, n - s, s);
		
		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) sort(x, off, s);
		if ((s = d - c) > 1) sort(x, n - s, s);
	}
	
	public void mergeSort(long[] x)
	{
        long[] aux = x.clone();
        mergeSort(aux, x, 0, x.length, 0);
	}
	
	private void mergeSort(long[] src, long[] dest, int low, int high, int off)
	{
		int length = high - low;

		// Insertion sort on smallest arrays
		if (length < 7)
		{
			for (int i = low; i < high; i++)
				for (int j = i; j > low && dest[j - 1] > dest[j]; j--)
					swap(dest, j, j - 1);
			return;
		}

		// Recursively sort halves of dest into src
		int destLow = low;
		int destHigh = high;
		low += off;
		high += off;
		int mid = (low + high) >>> 1;
		mergeSort(dest, src, low, mid, -off);
		mergeSort(dest, src, mid, high, -off);

		// If list is already sorted, just copy from src to dest. This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (src[mid - 1] <= src[mid])
		{
			System.arraycopy(src, low, dest, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for (int i = destLow, p = low, q = mid; i < destHigh; i++)
		{
			if (q >= high || p < mid && src[p] <= src[q]) dest[i] = src[p++];
			else dest[i] = src[q++];
		}
	}
	
	/**
	 * Swaps x[a] with x[b].
	 */
	private void swap0(int x[], int a, int b)
	{
		swap(x, a, b);
		swap(a, b);
	}

	public static void swap(int x[], int a, int b)
	{
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private void vecswap(int x[], int a, int b, int n)
	{
		for (int i = 0; i < n; i++, a++, b++)
			swap0(x, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed integers.
	 */
	private static int med3(int x[], int a, int b, int c)
	{
		return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}

	public static void swap(byte x[], int a, int b)
	{
		byte t = x[a];
		x[a] = x[b];
		x[b] = t;
	}

	public static void swap(short x[], int a, int b)
	{
		short t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
	
	/**
	 * Swaps the items at positions a and b.
	 */
	protected abstract void swap(int a, int b);
	
	public static abstract class Sortable
	{
		public static final int PIVOT = -1;
		
		protected abstract void setPivot(int aIndex);
		
		/**
		 * returns the sign of s[a] - s[b]
		 * s[a] <op> s[b] <=> s[a] - s[b] <op> 0 <=> compare(a, b) <op> 0
		 * @param a An index, or {@link #PIVOT}
		 * @param b An index, or {@link #PIVOT}
		 */
		protected abstract int compare(int a, int b);
		
		protected abstract void swap(int a, int b);
	}
}
