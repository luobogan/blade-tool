package org.springblade.core.boot.file.enums;

/**
 * 附件存储方式
 *
 * @author Blade
 */
public enum FileStorageType {

	/**
	 * 本地磁盘存储
	 */
	LOCAL,

	/**
	 * 对象存储（七牛/阿里/MinIO/腾讯COS，由 oss.name 决定具体实现）
	 */
	OSS

	;
}
