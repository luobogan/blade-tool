package org.springblade.core.boot.file.service;

import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.store.FileMetadata;
import org.springblade.core.boot.file.vo.FileDownloadContent;
import org.springblade.core.boot.file.vo.FileStorageConfigVO;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件服务：所有业务模块的上传/下载统一走这里
 *
 * <p>支持：本地磁盘 / 对象存储双通道、ZIP 打包（任意类型附件）、AES 加密、图片压缩、元数据落库。</p>
 *
 * @author Blade
 */
public interface IFileStorageService {

	/**
	 * 上传 MultipartFile
	 *
	 * @param file    文件
	 * @param options 上传选项（压缩/加密/存储方式）
	 * @return 上传结果
	 */
	FileUploadResult upload(MultipartFile file, FileUploadOptions options);

	/**
	 * 保存字节内容（业务内部生成文件，如公文版式、签章产物）
	 *
	 * @param content 文件内容
	 * @param fileName 原始文件名
	 * @param options 上传选项
	 * @return 上传结果
	 */
	FileUploadResult store(byte[] content, String fileName, FileUploadOptions options);

	/**
	 * 按附件ID读取文件内容（自动解密）
	 *
	 * @param fileId 附件ID
	 * @return 下载内容
	 */
	FileDownloadContent download(Long fileId);

	/**
	 * 按附件ID获取访问地址
	 *
	 * @param fileId 附件ID
	 * @return 访问地址
	 */
	String link(Long fileId);

	/**
	 * 按附件ID查询元数据
	 *
	 * @param fileId 附件ID
	 * @return 元数据
	 */
	java.util.Optional<FileMetadata> metadata(Long fileId);

	/**
	 * 删除附件（含物理文件与元数据）
	 *
	 * @param fileId 附件ID
	 * @return 是否删除成功
	 */
	boolean delete(Long fileId);

	/**
	 * 获取上传能力配置（供前端渲染上传选项）
	 *
	 * @return 上传能力配置
	 */
	FileStorageConfigVO config();

}