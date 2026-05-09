package org.springblade.core.boot.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;

/**
 * 文件上传服务
 * 负责将文件元数据保存到ImageFile表
 * 支持手动设置Mapper（用于非Spring环境或延迟初始化）
 *
 * @author Blade
 */
@Slf4j
@Service
public class ImageFileService {

	private static ImageFileService instance;

	private ImageFileMapper imageFileMapper;

	public ImageFileService() {
	}

	@Autowired(required = false)
	public void setImageFileMapper(ImageFileMapper imageFileMapper) {
		this.imageFileMapper = imageFileMapper;
	}

	@PostConstruct
	private void init() {
		instance = this;
	}

	public static ImageFileService getInstance() {
		return instance;
	}

	/**
	 * 手动设置Mapper（非Spring环境使用）
	 */
	public static void setMapper(ImageFileMapper mapper) {
		if (instance != null) {
			instance.imageFileMapper = mapper;
		}
	}

	/**
	 * 检查是否可用
	 */
	public boolean isAvailable() {
		return imageFileMapper != null;
	}

	/**
	 * 保存文件记录到数据库
	 *
	 * @param entity 文件实体
	 * @return 文件ID
	 */
	public Long saveFile(ImageFileEntity entity) {
		if (imageFileMapper == null) {
			log.warn("ImageFileMapper未初始化，无法保存文件记录到数据库");
			return null;
		}
		if (entity.getImagefileid() == null) {
			entity.setImagefileid(System.currentTimeMillis());
		}
		if (entity.getImagefileused() == null) {
			entity.setImagefileused(1);
		}
		if (entity.getDownloads() == null) {
			entity.setDownloads(0);
		}
		if (entity.getSecretlevel() == null) {
			entity.setSecretlevel(4);
		}
		entity.setCreatedat(new Date());
		entity.setUpdatedat(new Date());

		imageFileMapper.insert(entity);
		log.info("文件记录已保存到数据库: fileId={}, fileName={}", entity.getImagefileid(), entity.getImagefilename());
		return entity.getImagefileid();
	}

	/**
	 * 根据ID获取文件信息
	 */
	public ImageFileEntity getFileById(Long fileId) {
		if (imageFileMapper == null) return null;
		return imageFileMapper.selectById(fileId);
	}

	/**
	 * 删除文件记录
	 */
	public boolean deleteFile(Long fileId) {
		if (imageFileMapper == null) return false;
		int result = imageFileMapper.deleteById(fileId);
		return result > 0;
	}

	/**
	 * 构建ImageFileEntity对象（静态方法）
	 */
	public static ImageFileEntity buildEntity(String filename, String contentType, String filePath,
											  Long fileSize, boolean isZip, boolean isEncrypt, String aesCode) {
		return buildEntity(filename, contentType, filePath, fileSize, isZip, isEncrypt, aesCode, null);
	}

	/**
	 * 构建ImageFileEntity对象（静态方法，带租户ID）
	 */
	public static ImageFileEntity buildEntity(String filename, String contentType, String filePath,
											  Long fileSize, boolean isZip, boolean isEncrypt, String aesCode,
											  String tenantId) {
		ImageFileEntity entity = new ImageFileEntity();
		entity.setImagefilename(filename);
		entity.setImagefiletype(contentType);
		entity.setFilerealpath(filePath);
		entity.setFilesize(fileSize);
		entity.setIszip(isZip ? 1 : 0);
		entity.setIsaesencrypt(isEncrypt ? 1 : 0);
		entity.setAescode(aesCode != null ? aesCode : "");
		entity.setTenantid(tenantId != null ? tenantId : "");
		return entity;
	}

}
