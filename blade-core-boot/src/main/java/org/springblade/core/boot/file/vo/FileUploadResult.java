package org.springblade.core.boot.file.vo;

import lombok.Data;
import org.springblade.core.boot.file.enums.FileStorageType;

import java.io.Serializable;

/**
 * 上传结果（统一返回结构，各业务模块共用）
 *
 * @author Blade
 */
@Data
public class FileUploadResult implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 附件ID（ImageFile.imagefileid）
	 */
	private Long id;

	/**
	 * 原始文件名
	 */
	private String filename;

	/**
	 * MIME 类型
	 */
	private String contentType;

	/**
	 * 文件大小（字节）
	 */
	private Long filesize;

	/**
	 * 存储方式
	 */
	private FileStorageType storage;

	/**
	 * 实际落盘路径（LOCAL 为物理路径，OSS 为对象键）
	 */
	private String path;

	/**
	 * 访问地址（OSS 为外链，LOCAL 为统一下载接口）
	 */
	private String url;

	/**
	 * 统一下载地址
	 */
	private String downloadUrl;

	/**
	 * 是否已打包为 ZIP
	 */
	private boolean isZip;

	/**
	 * 是否已 AES 加密
	 */
	private boolean isEncrypt;

	/**
	 * AES 密钥（仅加密时返回，由调用方自行保管）
	 */
	private String aesCode;

	/**
	 * 租户ID
	 */
	private String tenantId;

}
