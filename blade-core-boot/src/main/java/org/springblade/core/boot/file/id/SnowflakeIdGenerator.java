package org.springblade.core.boot.file.id;

/**
 * 轻量雪花ID生成器（与 MyBatis-Plus IdWorker 结构兼容，避免引入 ORM 依赖）
 *
 * @author Blade
 */
public final class SnowflakeIdGenerator {

	private static final long EPOCH = 1700000000000L;
	private static final long WORKER_BITS = 5L;
	private static final long DATA_CENTER_BITS = 5L;
	private static final long SEQUENCE_BITS = 12L;

	private static final long WORKER_ID = 1L;
	private static final long DATA_CENTER_ID = 1L;

	private static long sequence = 0L;
	private static long lastTimestamp = -1L;

	private SnowflakeIdGenerator() {
	}

	public static synchronized long nextId() {
		long timestamp = System.currentTimeMillis();
		if (timestamp < lastTimestamp) {
			timestamp = lastTimestamp;
		}
		if (lastTimestamp == timestamp) {
			sequence = (sequence + 1) & 0xFFF;
			if (sequence == 0) {
				while (timestamp <= lastTimestamp) {
					timestamp = System.currentTimeMillis();
				}
			}
		} else {
			sequence = 0L;
		}
		lastTimestamp = timestamp;
		return ((timestamp - EPOCH) << (DATA_CENTER_BITS + WORKER_BITS + SEQUENCE_BITS))
			| (DATA_CENTER_ID << (WORKER_BITS + SEQUENCE_BITS))
			| (WORKER_ID << SEQUENCE_BITS)
			| sequence;
	}

}