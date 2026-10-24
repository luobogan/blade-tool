package org.springblade.core.boot.file.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 下载内容（统一下载接口的返回体）
 *
 * @author Blade
 */
@Data
public class FileDownloadContent implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 文件名（下载时作为 Content-Disposition 文件名）
	 */
	private String filename;

	/**
	 * MIME 类型
	 */
	private String contentType;

	/**
	 * 文件内容
	 */
	private byte[] content;

	/**
	 * 是否为 ZIP 打包文件（前端可自动解压预览）
	 */
	private boolean zip;

}