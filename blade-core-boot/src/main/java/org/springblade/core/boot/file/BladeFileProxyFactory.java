package org.springblade.core.boot.file;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.tool.constant.SystemConstant;
import org.springblade.core.tool.utils.ImageUtil;
import org.springblade.core.tool.utils.StringPool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

@Slf4j
public class BladeFileProxyFactory implements IFileProxy {

	private static final Random RANDOM = new Random();

	@Override
	public File rename(File f, String path) {
		File dest = new File(path);
		f.renameTo(dest);
		return dest;
	}

	@Override
	public String[] path(File f, String dir) {
		return path(f, dir, null);
	}

	@Override
	public String[] path(File f, String dir, String tenantId) {
		String uuid = UUID.randomUUID().toString();
		String ext = getFileExt(f.getName());

		StringBuilder uploadPath = new StringBuilder()
			.append(getFileDir(dir, SystemConstant.me().getUploadRealPath(), tenantId))
			.append(uuid)
			.append(ext);

		StringBuilder virtualPath = new StringBuilder()
			.append(getFileDir(dir, SystemConstant.me().getUploadCtxPath(), tenantId))
			.append(uuid)
			.append(ext);

		return new String[]{BladeFileUtil.formatUrl(uploadPath.toString()), BladeFileUtil.formatUrl(virtualPath.toString())};
	}

	public static String getFileExt(String fileName) {
		if (fileName == null || !fileName.contains(StringPool.DOT)) {
			return "";
		} else {
			return fileName.substring(fileName.lastIndexOf(StringPool.DOT));
		}
	}

	public static String getFileDir(String dir, String saveDir) {
		return getFileDir(dir, saveDir, null);
	}

	public static String getFileDir(String dir, String saveDir, String tenantId) {
		StringBuilder newFileDir = new StringBuilder();

		Calendar today = Calendar.getInstance();
		String currentYear = String.format("%04d", today.get(Calendar.YEAR));
		String currentMonth = String.format("%02d", today.get(Calendar.MONTH) + 1);

		int randomInt = 1 + RANDOM.nextInt(26);
		char randomChar = (char) ('A' + randomInt - 1);

		newFileDir.append(saveDir);

		if (!saveDir.endsWith(File.separator)) {
			newFileDir.append(File.separator);
		}

		if (tenantId != null && !tenantId.isEmpty()) {
			newFileDir.append(tenantId).append(File.separator);
		}

		newFileDir.append(dir)
			.append(File.separator)
			.append(currentYear).append(currentMonth)
			.append(File.separator)
			.append(randomChar)
			.append(File.separator);

		return newFileDir.toString();
	}

	@Override
	public void compress(String path) {
		try {
			ImageUtil.zoomScale(ImageUtil.readImage(path), new FileOutputStream(new File(path)), null, SystemConstant.me().getCompressScale(), SystemConstant.me().isCompressFlag());
		} catch (FileNotFoundException e) {
			log.error("图片压缩失败", e);
		}
	}

	public UploadResult processFile(String filePath, String fileExt, String originalName,
									String contentType, long fileSize,
									boolean needZip, boolean needEncrypt) {
		return processFile(filePath, fileExt, originalName, contentType, fileSize, needZip, needEncrypt, null);
	}

	public UploadResult processFile(String filePath, String fileExt, String originalName,
									String contentType, long fileSize,
									boolean needZip, boolean needEncrypt,
									String tenantId) {
		String aesCode = null;
		String finalPath = filePath;
		ImageFileEntity entity = null;
		Long fileId = null;

		try {
			String uuid = filePath;
			int lastSlashIdx = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
			if (lastSlashIdx >= 0) {
				uuid = filePath.substring(lastSlashIdx + 1);
			}
			if (uuid.contains(".")) {
				uuid = uuid.substring(0, uuid.lastIndexOf("."));
			}

			if (needZip) {
				String zipPath = filePath.substring(0, filePath.lastIndexOf(".")) + ".zip";
				ZipCompressUtil.compressToZip(filePath, zipPath, uuid);
				finalPath = zipPath;
				log.info("文件已压缩为ZIP: {}", zipPath);

				File originalFile = new File(filePath);
				if (originalFile.exists() && originalFile.delete()) {
					log.info("已删除原始文件: {}", filePath);
				}
			}

			if (needEncrypt) {
				aesCode = AESCoder.generateRandomKey();
				AESCoder.encryptFile(finalPath, finalPath, aesCode);
				log.info("文件已AES加密: {}", finalPath);
			}

			entity = ImageFileService.buildEntity(
				originalName, contentType, finalPath, fileSize,
				needZip, needEncrypt, aesCode, tenantId
			);

			ImageFileService imageFileService = ImageFileService.getInstance();
			if (imageFileService != null && imageFileService.isAvailable()) {
				fileId = imageFileService.saveFile(entity);
				log.info("文件记录已保存到ImageFile表: fileId={}", fileId);
			} else {
				log.warn("ImageFileService不可用，跳过数据库保存");
			}

			return new UploadResult(finalPath, entity, aesCode, fileId);

		} catch (Exception e) {
			log.error("处理文件失败: {}", filePath, e);
			return new UploadResult(filePath, null, null, null);
		}
	}

	public static class UploadResult {
		private final String filePath;
		private final ImageFileEntity entity;
		private final String aesCode;
		private final Long fileId;

		public UploadResult(String filePath, ImageFileEntity entity, String aesCode, Long fileId) {
			this.filePath = filePath;
			this.entity = entity;
			this.aesCode = aesCode;
			this.fileId = fileId;
		}

		public String getFilePath() { return filePath; }
		public ImageFileEntity getEntity() { return entity; }
		public Long getFileId() { return fileId != null ? fileId : (entity != null ? entity.getImagefileid() : null); }
		public String getAesCode() { return aesCode; }
	}

}
