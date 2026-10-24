package org.springblade.core.boot.file.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 上传能力配置（供前端渲染上传选项面板）
 */
@Data
public class FileStorageConfigVO implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 组件是否启用
	 */
	private boolean enabled = true;

	/**
	 * 默认存储方式：LOCAL / OSS
	 */
	private String defaultStorage = "LOCAL";

	/**
	 * 是否存在可用的 OSS 通道（未配置 oss.name 时为 false）
	 */
	private boolean ossAvailable = false;

	/**
	 * 默认是否打包 ZIP
	 */
	private boolean defaultNeedZip = false;

	/**
	 * 默认是否 AES 加密
	 */
	private boolean defaultNeedEncrypt = false;

	/**
	 * 是否允许选择 ZIP 打包
	 */
	private boolean allowZip = true;

	/**
	 * 是否允许选择 AES 加密
	 */
	private boolean allowEncrypt = true;

	/**
	 * 是否允许切换存储方式
	 */
	private boolean allowStorageSwitch = true;

	/**
	 * 单文件大小上限（字节）
	 */
	private long maxSize = 0L;

	/**
	 * 允许的扩展名白名单（空 = 不限制）
	 */
	private List<String> allowedExtensions;

}