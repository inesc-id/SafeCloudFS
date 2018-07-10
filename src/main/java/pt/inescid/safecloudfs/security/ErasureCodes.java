package pt.inescid.safecloudfs.security;

import java.nio.ByteBuffer;

import org.bouncycastle.util.Arrays;

import com.backblaze.erasure.ReedSolomon;

public class ErasureCodes {

	public static final int BYTES_IN_INT = Integer.BYTES;

	public static byte[][] encode(byte[] bytes, int dataShards, int parity) {

		int totalShards = dataShards + parity;

		final int bytesLength = (int) bytes.length;

		// Figure out how big each shard will be. The total size stored
		// will be the file size (8 bytes) plus the file.
		final int storedSize = bytesLength + BYTES_IN_INT;
		final int shardSize = (storedSize + dataShards - 1) / dataShards;

		// Create a buffer holding the file size, followed by
		// the contents of the file.
		final int bufferSize = shardSize * dataShards;
		final byte[] allBytes = new byte[bufferSize];
		ByteBuffer.wrap(allBytes).putInt(bytesLength);

		System.arraycopy(bytes, 0, allBytes, BYTES_IN_INT, bytes.length);

		// Make the buffers to hold the shards.
		byte[][] shards = new byte[totalShards][shardSize];

		// Fill in the data shards
		for (int i = 0; i < dataShards; i++) {
			System.arraycopy(allBytes, i * shardSize, shards[i], 0, shardSize);
		}

		// Use Reed-Solomon to calculate the parity.
		ReedSolomon reedSolomon = ReedSolomon.create(dataShards, parity);
		reedSolomon.encodeParity(shards, 0, shardSize);

		return shards;

	}

	public static byte[] decode(byte[][] shards, int dataShards, int parity) {

		int totalShards = dataShards + parity;

		final boolean[] shardPresent = new boolean[totalShards];
		int shardSize = 0;
		int shardCount = 0;

		for (int i = 0; i < totalShards; i++) {

			if (shards[i] != null) {
				shardPresent[i] = shards[i] != null;
				shardCount += 1;
				shardSize = shards[i].length;
			}

		}

		// We need at least DATA_SHARDS to be able to reconstruct the file.
		if (shardCount < dataShards) {
			System.err.println("Not enough shards present");
			return null;
		}

		// Make empty buffers for the missing shards.
		for (int i = 0; i < totalShards; i++) {
			if (!shardPresent[i]) {
				shards[i] = new byte[shardSize];
			}
		}

		// Use Reed-Solomon to fill in the missing shards
		ReedSolomon reedSolomon = ReedSolomon.create(dataShards, parity);
		reedSolomon.decodeMissing(shards, shardPresent, 0, shardSize);

		// Combine the data shards into one buffer for convenience.
		// (This is not efficient, but it is convenient.)
		byte[] allBytes = new byte[shardSize * dataShards];
		for (int i = 0; i < dataShards; i++) {
			System.arraycopy(shards[i], 0, allBytes, shardSize * i, shardSize);
		}

		int size = ByteBuffer.wrap(Arrays.copyOfRange(allBytes, 0, BYTES_IN_INT)).getInt();
		int padding = allBytes.length - BYTES_IN_INT - size;

		byte[] result = Arrays.copyOfRange(allBytes, BYTES_IN_INT, allBytes.length - padding);

		return result;
	}

}
