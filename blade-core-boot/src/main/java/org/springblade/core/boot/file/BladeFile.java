package org.springblade.core.boot.file;

import org.springblade.core.tool.constant.SystemConstant;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

public class BladeFile {

	private static BladeFileProperties properties;
	private static final Random RANDOM = new Random();

	public static void setProperties(BladeFileProperties props) {
		properties = props;
	}

	private Object fileId;

	private ImageFileEntity fileEntity;

	private MultipartFile file;

	private String dir;

	private String uploadPath;

	private String uploadVirtualPath;

	private String fileName;

	private String originalFileName;

	private String fileExt;

	/**
	 * 租户ID
	 */
	private String tenantId;

	private Boolean needZip = null;

	private Boolean needEncrypt = null;

	private Boolean saveToDb = null;

	private String aesCode;

	private Boolean imageCompress = null;

	public BladeFile() {
	}

	public BladeFile(MultipartFile file, String dir) {
		this.dir = dir;
		this.file = file;
		this.originalFileName = file.getOriginalFilename();

		String uuid = UUID.randomUUID().toString();
		this.fileExt = getFileExt(this.originalFileName);
		this.fileName = uuid;

		String dateDir = getDateDir();
		String randomChar = getRandomChar();

		StringBuilder pathBuilder = new StringBuilder();
		pathBuilder.append(File.separator).append(SystemConstant.me().getUploadRealPath());
		if (this.tenantId != null && !this.tenantId.isEmpty()) {
			pathBuilder.append(File.separator).append(this.tenantId);
		}
		pathBuilder.append(File.separator).append(dir)
			.append(File.separator).append(dateDir)
			.append(File.separator).append(randomChar)
			.append(File.separator).append(this.fileName).append(this.fileExt);

		StringBuilder virtualPathBuilder = new StringBuilder();
		virtualPathBuilder.append(SystemConstant.me().getUploadCtxPath().replace(SystemConstant.me().getContextPath(), ""));
		if (this.tenantId != null && !this.tenantId.isEmpty()) {
			virtualPathBuilder.append(File.separator).append(this.tenantId);
		}
		virtualPathBuilder.append(File.separator).append(dir)
			.append(File.separator).append(dateDir)
			.append(File.separator).append(randomChar)
			.append(File.separator).append(this.fileName).append(this.fileExt);

		this.uploadPath = BladeFileUtil.formatUrl(pathBuilder.toString());
		this.uploadVirtualPath = BladeFileUtil.formatUrl(virtualPathBuilder.toString());
		initFromConfig();
	}

	private String getFileExt(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return ".jpg";
		}
		return fileName.substring(fileName.lastIndexOf("."));
	}

	private String getDateDir() {
		Calendar today = Calendar.getInstance();
		String currentYear = String.format("%04d", today.get(Calendar.YEAR));
		String currentMonth = String.format("%02d", today.get(Calendar.MONTH) + 1);
		return currentYear + currentMonth;
	}

	private String getRandomChar() {
		int randomInt = 1 + RANDOM.nextInt(26);
		return String.valueOf((char) ('A' + randomInt - 1));
	}

	public BladeFile(MultipartFile file, String dir, String uploadPath, String uploadVirtualPath) {
		this(file, dir);
		if (null != uploadPath) {
			this.uploadPath = BladeFileUtil.formatUrl(uploadPath);
			this.uploadVirtualPath = BladeFileUtil.formatUrl(uploadVirtualPath);
		}
	}

	private void initFromConfig() {
		if (properties != null && properties.isEnabled()) {
			if (this.needZip == null) {
				this.needZip = properties.isZipEnabled();
			}
			if (this.needEncrypt == null) {
				this.needEncrypt = properties.isEncryptEnabled();
			}
			if (this.saveToDb == null) {
				this.saveToDb = properties.isSaveToDb();
			}
			if (this.imageCompress == null) {
				this.imageCompress = properties.isImageCompress();
			}
		} else {
			if (this.needZip == null) this.needZip = false;
			if (this.needEncrypt == null) this.needEncrypt = false;
			if (this.saveToDb == null) this.saveToDb = false;
			if (this.imageCompress == null) this.imageCompress = true;
		}
	}

	public BladeFile setNeedZip(boolean needZip) {
		this.needZip = needZip;
		return this;
	}

	public BladeFile setNeedEncrypt(boolean needEncrypt) {
		this.needEncrypt = needEncrypt;
		return this;
	}

	public BladeFile setSaveToDb(boolean saveToDb) {
		this.saveToDb = saveToDb;
		return this;
	}

	public BladeFile setImageCompress(boolean compress) {
		this.imageCompress = compress;
		return this;
	}

	public BladeFile setTenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public void transfer() {
		transfer(getEffectiveImageCompress());
	}

	public void transfer(boolean compress) {
		IFileProxy fileFactory = FileProxyManager.me().getDefaultFileProxyFactory();
		transfer(fileFactory, compress);
	}

	public void transfer(IFileProxy fileFactory, boolean compress) {
		try {
			File f = new File(uploadPath);

			if (null != fileFactory) {
				String[] path = fileFactory.path(f, dir, this.tenantId);
				this.uploadPath = path[0];
				this.uploadVirtualPath = path[1];
				f = fileFactory.rename(f, path[0]);
			}

			File pfile = f.getParentFile();
			if (!pfile.exists()) {
				pfile.mkdirs();
			}

			this.file.transferTo(f);

			if (compress) {
				fileFactory.compress(this.uploadPath);
			}

		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}
	}

	public BladeFileProxyFactory.UploadResult transferEnhanced() {
		boolean compress = getEffectiveImageCompress();
		boolean zip = getEffectiveNeedZip();
		boolean encrypt = getEffectiveNeedEncrypt();

		long fileSize = this.file.getSize();
		String contentType = this.file.getContentType();

		IFileProxy fileFactory = FileProxyManager.me().getDefaultFileProxyFactory();

		try {
			File f = new File(uploadPath);

			if (null != fileFactory) {
				String[] path = fileFactory.path(f, dir, this.tenantId);
				this.uploadPath = path[0];
				this.uploadVirtualPath = path[1];
				f = new File(this.uploadPath);
			}

			File pfile = f.getParentFile();
			if (!pfile.exists()) {
				pfile.mkdirs();
			}

			this.file.transferTo(f);

			if (compress && isImageFile()) {
				fileFactory.compress(this.uploadPath);
			}

			if (fileFactory instanceof BladeFileProxyFactory) {
				BladeFileProxyFactory.UploadResult result = ((BladeFileProxyFactory) fileFactory).processFile(
					this.uploadPath,
					this.fileExt,
					this.originalFileName,
					contentType,
					fileSize,
					zip,
					encrypt,
					this.tenantId
				);

				if (result.getFilePath() != null) {
					this.uploadPath = result.getFilePath();
				}
				if (result.getEntity() != null) {
					this.fileEntity = result.getEntity();
				}
				if (result.getFileId() != null) {
					this.fileId = result.getFileId();
				}
				if (result.getAesCode() != null) {
					this.aesCode = result.getAesCode();
				}

				return result;
			}

		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private boolean getEffectiveImageCompress() {
		return imageCompress != null ? imageCompress :
			(properties != null ? properties.isImageCompress() : true);
	}

	private boolean getEffectiveNeedZip() {
		return needZip != null ? needZip :
			(properties != null ? properties.isZipEnabled() : false);
	}

	private boolean getEffectiveNeedEncrypt() {
		return needEncrypt != null ? needEncrypt :
			(properties != null ? properties.isEncryptEnabled() : false);
	}

	private boolean isImageFile() {
		String contentType = file.getContentType();
		return contentType != null && contentType.startsWith("image/");
	}

	public MultipartFile getFile() {
		return file;
	}

	public void setFile(MultipartFile file) {
		this.file = file;
	}

	public String getUploadPath() {
		return uploadPath;
	}

	public void setUploadPath(String uploadPath) {
		this.uploadPath = uploadPath;
	}

	public String getUploadVirtualPath() {
		return uploadVirtualPath;
	}

	public void setUploadVirtualPath(String uploadVirtualPath) {
		this.uploadVirtualPath = uploadVirtualPath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFileExt() {
		return fileExt;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public Object getFileId() {
		return fileId;
	}

	public ImageFileEntity getFileEntity() {
		return fileEntity;
	}

	public String getAesCode() {
		return aesCode;
	}

	public Boolean isNeedZip() {
		return needZip;
	}

	public Boolean isNeedEncrypt() {
		return needEncrypt;
	}

	public Boolean isSaveToDb() {
		return saveToDb;
	}

	public Boolean getImageCompress() {
		return imageCompress;
	}

	public String getDir() {
		return dir;
	}

	public String getTenantId() {
		return tenantId;
	}

}
