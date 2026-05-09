package org.springblade.core.boot.file;

import java.io.File;

public interface IFileProxy {

	String[] path(File file, String dir);

	default String[] path(File file, String dir, String tenantId) {
		return path(file, dir);
	}

	File rename(File file, String path);

	void compress(String path);

	default void encryptFile(String filePath, String aesCode) {
		AESCoder.encryptFile(filePath, filePath, aesCode);
	}

	default void zipCompress(String sourceFilePath, String zipFilePath) {
		ZipCompressUtil.compressToZip(sourceFilePath, zipFilePath);
	}

}
