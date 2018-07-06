/*
* Copyright (c) 2005, Joe Desbonnet, (jdesbonnet@gmail.com)
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the <organization> nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY <copyright holder> ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL <copyright holder> BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package pt.inescid.safecloudfs.recovery.jbdiff;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 * Java Binary Diff utility. Based on bsdiff (v4.2) by Colin Percival (see
 * http://www.daemonology.net/bsdiff/ ) and distributed under BSD license.
 *
 * <p>
 * Running this on large files may require an increase of the default maximum
 * heap size (use java -Xmx200m)
 * </p>
 *
 * @author Joe Desbonnet, jdesbonnet@gmail.com
 *
 */

public class JBDiff {

	private static final String VERSION = "jbdiff-0.1.1";

	private static final int min(int x, int y) {
		return x < y ? x : y;
	}

	private final static void split(int[] I, int[] V, int start, int len, int h) {

		int i, j, k, x, tmp, jj, kk;

		if (len < 16) {
			for (k = start; k < start + len; k += j) {
				j = 1;
				x = V[I[k] + h];
				for (i = 1; k + i < start + len; i++) {
					if (V[I[k + i] + h] < x) {
						x = V[I[k + i] + h];
						j = 0;
					}

					if (V[I[k + i] + h] == x) {
						tmp = I[k + j];
						I[k + j] = I[k + i];
						I[k + i] = tmp;
						j++;
					}

				}

				for (i = 0; i < j; i++)
					V[I[k + i]] = k + j - 1;
				if (j == 1)
					I[k] = -1;
			}

			return;
		}

		x = V[I[start + len / 2] + h];
		jj = 0;
		kk = 0;
		for (i = start; i < start + len; i++) {
			if (V[I[i] + h] < x)
				jj++;
			if (V[I[i] + h] == x)
				kk++;
		}

		jj += start;
		kk += jj;

		i = start;
		j = 0;
		k = 0;
		while (i < jj) {
			if (V[I[i] + h] < x) {
				i++;
			} else if (V[I[i] + h] == x) {
				tmp = I[i];
				I[i] = I[jj + j];
				I[jj + j] = tmp;
				j++;
			} else {
				tmp = I[i];
				I[i] = I[kk + k];
				I[kk + k] = tmp;
				k++;
			}

		}

		while (jj + j < kk) {
			if (V[I[jj + j] + h] == x) {
				j++;
			} else {
				tmp = I[jj + j];
				I[jj + j] = I[kk + k];
				I[kk + k] = tmp;
				k++;
			}

		}

		if (jj > start) {
			split(I, V, start, jj - start, h);
		}

		for (i = 0; i < kk - jj; i++) {
			V[I[jj + i]] = kk - 1;
		}

		if (jj == kk - 1) {
			I[jj] = -1;
		}

		if (start + len > kk) {
			split(I, V, kk, start + len - kk, h);
		}

	}

	/**
	 * Fast suffix sorting (qsufsort). See paper "Faster Suffix Sorting" by N.
	 * Jesper Larsson, Kunihiko Sadakane. http://www.larsson.dogma.net/research.html
	 *
	 * @param I
	 * @param V
	 * @param oldBuf
	 */
	private static void qsufsort(int[] I, int[] V, byte[] oldBuf) {

		int oldsize = oldBuf.length;

		int[] buckets = new int[256];
		int i, h, len;

		for (i = 0; i < 256; i++) {
			buckets[i] = 0;
		}

		for (i = 0; i < oldsize; i++) {
			buckets[(int) oldBuf[i] & 0xff]++;
		}

		for (i = 1; i < 256; i++) {
			buckets[i] += buckets[i - 1];
		}

		for (i = 255; i > 0; i--) {
			buckets[i] = buckets[i - 1];
		}

		buckets[0] = 0;

		for (i = 0; i < oldsize; i++) {
			I[++buckets[(int) oldBuf[i] & 0xff]] = i;
		}

		I[0] = oldsize;
		for (i = 0; i < oldsize; i++) {
			V[i] = buckets[(int) oldBuf[i] & 0xff];
		}
		V[oldsize] = 0;

		for (i = 1; i < 256; i++) {
			if (buckets[i] == buckets[i - 1] + 1) {
				I[buckets[i]] = -1;
			}
		}

		I[0] = -1;

		for (h = 1; I[0] != -(oldsize + 1); h += h) {
			len = 0;
			for (i = 0; i < oldsize + 1;) {
				if (I[i] < 0) {
					len -= I[i];
					i -= I[i];
				} else {
					// if(len) I[i-len]=-len;
					if (len != 0) {
						I[i - len] = -len;
					}
					len = V[I[i]] + 1 - i;
					split(I, V, i, len, h);
					i += len;
					len = 0;
				}

			}

			if (len != 0) {
				I[i - len] = -len;
			}
		}

		for (i = 0; i < oldsize + 1; i++) {
			I[V[i]] = i;
		}
	}

	/**
	 * Count the number of bytes that match in oldBuf (starting at offset oldOffset)
	 * and newBuf (starting at offset newOffset).
	 *
	 * @param oldBuf
	 * @param oldOffset
	 * @param newBuf
	 * @param newOffset
	 * @return
	 */
	private final static int matchlen(byte[] oldBuf, int oldOffset, byte[] newBuf, int newOffset) {
		int end = min(oldBuf.length - oldOffset, newBuf.length - newOffset);
		int i;
		for (i = 0; i < end; i++) {
			if (oldBuf[oldOffset + i] != newBuf[newOffset + i]) {
				break;
			}
		}
		return i;
	}

	private final static int search(int[] I, byte[] oldBuf, byte[] newBuf, int newBufOffset, int start, int end,
			IntByRef pos) {
		int x, y;

		if (end - start < 2) {
			x = matchlen(oldBuf, I[start], newBuf, newBufOffset);
			y = matchlen(oldBuf, I[end], newBuf, newBufOffset);

			if (x > y) {
				pos.value = I[start];
				return x;
			} else {
				pos.value = I[end];
				return y;
			}
		}

		x = start + (end - start) / 2;
		if (Util.memcmp(oldBuf, I[x], newBuf, newBufOffset) < 0) {
			return search(I, oldBuf, newBuf, newBufOffset, x, end, pos);
		} else {
			return search(I, oldBuf, newBuf, newBufOffset, start, x, pos);
		}

	}

	public static byte[] bsdiff(byte[] oldBuf, byte[] newBuf) throws IOException {

		int oldsize = Math.toIntExact(oldBuf.length);

		int[] I = new int[oldBuf.length + 1];
		int[] V = new int[oldBuf.length + 1];

		qsufsort(I, V, oldBuf);

		// free(V)
		V = null;
		System.gc();

		int newsize = (int) newBuf.length;

		// diff block
		int dblen = 0;
		byte[] db = new byte[newsize];

		// extra block
		int eblen = 0;
		byte[] eb = new byte[newsize];

		/*
		 * Diff file is composed as follows:
		 *
		 * Header (32 bytes) Data (from offset 32 to end of file)
		 *
		 * Header: Offset 0, length 8 bytes: file magic "jbdiff40" Offset 8, length 8
		 * bytes: length of ctrl block Offset 16, length 8 bytes: length of compressed
		 * diff block Offset 24, length 8 bytes: length of new file
		 *
		 * Data: 32 (length ctrlBlockLen): ctrlBlock 32+ctrlBlockLen (length
		 * diffBlockLen): diffBlock (gziped) 32+ctrlBlockLen+diffBlockLen (to end of
		 * file): extraBlock (gziped)
		 *
		 * ctrlBlock comprises a set of records, each record 12 bytes. A record
		 * comprises 3 x 32 bit integers. The ctrlBlock is not compressed.
		 */

		// DataOutputStream diffOut = new DataOutputStream(new
		// FileOutputStream(diffFile));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream diffOut = new DataOutputStream(baos);

		/*
		 * Write as much of header as we have now. Size of ctrlBlock and diffBlock must
		 * be filled in later.
		 */
		diffOut.write("jbdiff40".getBytes("US-ASCII"));
		diffOut.writeLong(-1); // place holder for ctrlBlockLen
		diffOut.writeLong(-1); // place holder for diffBlockLen
		diffOut.writeLong(newsize);

		int oldscore, scsc;

		int overlap, Ss, lens;
		int i;
		int scan = 0;
		int len = 0;
		int lastscan = 0;
		int lastpos = 0;
		int lastoffset = 0;

		IntByRef pos = new IntByRef();
		int ctrlBlockLen = 0;

		while (scan < newsize) {

			oldscore = 0;

			for (scsc = scan += len; scan < newsize; scan++) {

				len = search(I, oldBuf, newBuf, scan, 0, oldsize, pos);

				for (; scsc < scan + len; scsc++) {
					if ((scsc + lastoffset < oldsize) && (oldBuf[scsc + lastoffset] == newBuf[scsc])) {
						oldscore++;
					}
				}

				if (((len == oldscore) && (len != 0)) || (len > oldscore + 8)) {
					break;
				}

				if ((scan + lastoffset < oldsize) && (oldBuf[scan + lastoffset] == newBuf[scan])) {
					oldscore--;
				}
			}

			if ((len != oldscore) || (scan == newsize)) {
				int s = 0;
				int Sf = 0;
				int lenf = 0;
				for (i = 0; (lastscan + i < scan) && (lastpos + i < oldsize);) {
					if (oldBuf[lastpos + i] == newBuf[lastscan + i])
						s++;
					i++;
					if (s * 2 - i > Sf * 2 - lenf) {
						Sf = s;
						lenf = i;
					}
				}

				int lenb = 0;
				if (scan < newsize) {
					s = 0;
					int Sb = 0;
					for (i = 1; (scan >= lastscan + i) && (pos.value >= i); i++) {
						if (oldBuf[pos.value - i] == newBuf[scan - i])
							s++;
						if (s * 2 - i > Sb * 2 - lenb) {
							Sb = s;
							lenb = i;
						}
					}
				}

				if (lastscan + lenf > scan - lenb) {
					overlap = (lastscan + lenf) - (scan - lenb);
					s = 0;
					Ss = 0;
					lens = 0;
					for (i = 0; i < overlap; i++) {
						if (newBuf[lastscan + lenf - overlap + i] == oldBuf[lastpos + lenf - overlap + i]) {
							s++;
						}
						if (newBuf[scan - lenb + i] == oldBuf[pos.value - lenb + i]) {
							s--;
						}
						if (s > Ss) {
							Ss = s;
							lens = i + 1;
						}
					}

					lenf += lens - overlap;
					lenb -= lens;
				}

				// ? byte casting introduced here -- might affect things
				for (i = 0; i < lenf; i++) {
					db[dblen + i] = (byte) (newBuf[lastscan + i] - oldBuf[lastpos + i]);
				}

				for (i = 0; i < (scan - lenb) - (lastscan + lenf); i++) {
					eb[eblen + i] = newBuf[lastscan + lenf + i];
				}

				dblen += lenf;
				eblen += (scan - lenb) - (lastscan + lenf);

				/*
				 * Write control block entry (3 x int)
				 */
				diffOut.writeInt(lenf);
				diffOut.writeInt((scan - lenb) - (lastscan + lenf));
				diffOut.writeInt((pos.value - lenb) - (lastpos + lenf));
				ctrlBlockLen += 12;

				lastscan = scan - lenb;
				lastpos = pos.value - lenb;
				lastoffset = pos.value - scan;
			} // end if
		} // end while loop

		GZIPOutputStream gzOut;

		/*
		 * Write diff block
		 */
		gzOut = new GZIPOutputStream(diffOut);
		gzOut.write(db, 0, dblen);
		gzOut.finish();
		int diffBlockLen = diffOut.size() - ctrlBlockLen - 32;
		// System.err.println ("diffBlockLen=" + diffBlockLen);

		/*
		 * Write extra block
		 */
		gzOut = new GZIPOutputStream(diffOut);
		gzOut.write(eb, 0, eblen);
		gzOut.finish();
		long extraBlockLen = diffOut.size() - diffBlockLen - ctrlBlockLen - 32;
		// System.err.println ("extraBlockLen=" + extraBlockLen);

		// /*
		// * Write missing header info. Need to reopen the file with RandomAccessFile
		// for
		// * this.
		// */
		// RandomAccessFile diff = new RandomAccessFile(diffFile, "rw");
		// diff.seek(8);
		// diff.writeLong(ctrlBlockLen); // ctrlBlockLen (compressed) @offset 8
		// diff.writeLong(diffBlockLen); // diffBlockLen (compressed) @offset 16
		// diff.close();

		/*
		 * Write missing header info. Need to reopen the file with RandomAccessFile for
		 * this. //
		 */
		// baos.write(longToBytes(ctrlBlockLen), 8, 8); // ctrlBlockLen (compressed)
		// @offset 8
		// baos.write(longToBytes(diffBlockLen), 16, 8); // diffBlockLen (compressed)
		// @offset 16

		byte[] result = baos.toByteArray();

		diffOut.close();

		System.arraycopy(longToBytes(ctrlBlockLen), 0, result, 8, 8);
		System.arraycopy(longToBytes(diffBlockLen), 0, result, 16, 8);

		return result;
	}

	public static byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	/**
	 * Run JBDiff from the command line. Params: oldfile newfile difffile. diff file
	 * will be created.
	 *
	 * @param arg
	 * @throws IOException
	 */
	public static void main(String[] arg) throws IOException {

		String version1 = "This is a big text file with a lot of information";
		String version2 = "This is a big invalid text file with a lot of information"; // faulty
		String version3 = "This is a big invalid text file with a lot of new information";

		byte[] version1bytes = version1.getBytes();
		byte[] version2bytes = version2.getBytes();
		byte[] version3bytes = version3.getBytes();

		// patches - log entries
		byte[] delta1 = bsdiff(version2bytes, version1bytes); // allows to return from version2 to version1
		byte[] delta2 = bsdiff(version3bytes, version2bytes); // allows to return from version3 to version2

		System.out.println("1:\t" + version1 + "\t\tSize: " + version1bytes.length);
		System.out.println("2:\t" + version2 + "\tSize: " + delta1.length);
		System.out.println("3:\t" + version3 + "\tSize: " + delta2.length);

		// This will undo version 2 in a roll back manner
		// version3 -> version2 -> version1
		// This costs less then the other way around for files with many versions that
		// were attacked recently
		byte[] corrected = JBPatch.bspatch(JBPatch.bspatch(version3bytes, delta2), delta1);

		System.out.println("\n\nCorrected: " + new String(corrected));

		// another test
		byte[] dummy = new byte[10000];
		new Random().nextBytes(dummy);
		byte[] firstVersion = Arrays.copyOf(dummy, 10000);
		firstVersion[9999] = 5;
		byte[] secondVersion = Arrays.copyOf(dummy, 10000);
		secondVersion[9999] = 9;

		delta1 = bsdiff(secondVersion, firstVersion);
		System.out.println("\n\nFirst version size = " + firstVersion.length + " patch size = " + delta1.length);

		// Another test

		byte[] version1bytesWithPad = new byte[100];
		byte[] version2bytesWithPad = new byte[100];
		byte[] version3bytesWithPad = new byte[100];

		Arrays.fill(version1bytesWithPad, (byte) 1);
		Arrays.fill(version2bytesWithPad, (byte) 1);
		Arrays.fill(version3bytesWithPad, (byte) 1);

		System.arraycopy(version1bytes, 0, version1bytesWithPad, 0, version1bytes.length);
		System.arraycopy(version2bytes, 0, version2bytesWithPad, 0, version2bytes.length);
		System.arraycopy(version3bytes, 0, version3bytesWithPad, 0, version3bytes.length);

		// patches - log entries
		byte[] delta1WithPad = bsdiff(version2bytes, version1bytes); // allows to return from version2 to version1
		byte[] delta2WithPad = bsdiff(version3bytes, version2bytes); // allows to return from version3 to version2

		byte[] correctedWithPad = JBPatch.bspatch(version3bytesWithPad, delta1WithPad);
		System.out.println(new String(correctedWithPad));

	}

	private static class IntByRef {
		public int value;
	}
}