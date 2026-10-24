package org.springblade.core.boot.file.dto;

import lombok.Data;
import org.springblade.core.boot.file.enums.FileStorageType;

/**
 * 上传选项（前端可配置的三个维度：压缩 / 加密 / 存储方式）
 *
 * <p>三个字段均为可空包装类型：{@code null} 表示「不指定」，由服务端配置
 * {@code blade.file.storage.default-*} 决定最终行为。</p>
 *
 * @author Blade
 */
@Data
public class FileUploadOptions {

	/**
	 * 业务目录（product / brand / doc / odoc ...），用于分目录归档
	 */
	private String dir = "common";

	/**
	 * 存储方式：LOCAL / OSS，null = 用服务端默认
	 */
	private FileStorageType storage;

	/**
	 * 是否打包为 ZIP（对任意类型附件生效），null = 用服务端默认
	 */
	private Boolean needZip;

	/**
	 * 是否 AES 加密，null = 用服务端默认
	 */
	private Boolean needEncrypt;

	/**
	 * 是否对图片做尺寸压缩，null = 用服务端默认
	 */
	private Boolean imageCompress;

	/**
	 * 租户ID
	 */
	private String tenantId;

	/**
	 * 附件来源（业务标识，便于排查）
	 */
	private String comefrom;

	/**
	 * 是否将元数据写入 ImageFile 表，null = 用服务端默认（true）
	 */
	private Boolean saveToDb;

	/**
	 * 是否允许覆盖同名文件（默认不允许）
	 */
	private boolean overwrite = false;

	public FileUploadOptions dir(String dir) {
		this.dir = dir;
		return this;
	}

	public FileUploadOptions storage(FileStorageType storage) {
		this.storage = storage;
		return this;
	}

	public FileUploadOptions needZip(Boolean needZip) {
		this.needZip = needZip;
		return this;
	}

	public FileUploadOptions needEncrypt(Boolean needEncrypt) {
		this.needEncrypt = needEncrypt;
		return this;
	}

	public FileUploadOptions tenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public FileUploadOptions comefrom(String comefrom) {
		this.comefrom = comefrom;
		return this;
	}

}
