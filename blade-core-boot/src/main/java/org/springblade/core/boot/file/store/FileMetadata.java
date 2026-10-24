package org.springblade.core.boot.file.store;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 附件元数据（对应 ImageFile 表）
 *
 * @author Blade
 */
@Data
public class FileMetadata implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 附件ID
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
	 * 真实存储路径（LOCAL=物理路径，OSS=对象键）
	 */
	private String path;

	/**
	 * 存储方式：LOCAL / OSS
	 */
	private String storage;

	/**
	 * 是否 ZIP 打包
	 */
	private boolean isZip;

	/**
	 * 是否 AES 加密
	 */
	private boolean isEncrypt;

	/**
	 * AES 密钥
	 */
	private String aesCode;

	/**
	 * 租户ID
	 */
	private String tenantId;

	/**
	 * 附件来源
	 */
	private String comefrom;

	/**
	 * 存储状态
	 */
	private String storageStatus;

	/**
	 * 密级
	 */
	private Integer secretLevel;

	/**
	 * 创建时间
	 */
	private Date createdAt;

	/**
	 * 更新时间
	 */
	private Date updatedAt;

}