package org.springblade.core.boot.file;

import java.io.File;

/**
 * 文件管理类
 * 增强支持：加密、ZIP压缩、数据库存储
 *
 * @author Chill
 */
public class FileProxyManager {
	private IFileProxy defaultFileProxyFactory = new BladeFileProxyFactory();

	private static FileProxyManager me = new FileProxyManager();

	public static FileProxyManager me() {
		return me;
	}

	public IFileProxy getDefaultFileProxyFactory() {
		return defaultFileProxyFactory;
	}

	public void setDefaultFileProxyFactory(IFileProxy defaultFileProxyFactory) {
		this.defaultFileProxyFactory = defaultFileProxyFactory;
	}

	public String[] path(File file, String dir) {
		return defaultFileProxyFactory.path(file, dir);
	}

	public File rename(File file, String path) {
		return defaultFileProxyFactory.rename(file, path);
	}

	/**
	 * AES加密文件
	 *
	 * @param filePath 文件路径
	 * @param aesCode  密钥
	 */
	public void encryptFile(String filePath, String aesCode) {
		defaultFileProxyFactory.encryptFile(filePath, aesCode);
	}

	/**
	 * ZIP压缩文件
	 *
	 * @param sourceFilePath 源文件路径
	 * @param zipFilePath    ZIP输出路径
	 */
	public void zipCompress(String sourceFilePath, String zipFilePath) {
		defaultFileProxyFactory.zipCompress(sourceFilePath, zipFilePath);
	}
}