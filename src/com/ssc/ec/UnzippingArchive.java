package com.ssc.ec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

public class UnzippingArchive {
	private static Logger logger = Logger.getLogger(UnzippingArchive.class.getCanonicalName());

	public static void extractPatchAndReadJar(String localPath, String destinationPath) {

		// TODO Auto-generated method stub

		File inputLocation = new File(localPath);
		if (inputLocation.isDirectory()) {
			System.out.println("Listing the files...");
			File[] attachmentFiles = inputLocation.listFiles();
			System.out.println("Total files in the folder: " + attachmentFiles.length);
			for (File aFile : attachmentFiles) {
				if (!aFile.isDirectory()) {
					if (aFile.getName().endsWith("exe"))
						try {
							extract(aFile, destinationPath);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}

			}
		}
		System.out.println("Completed...");
	}

	public static void extract(File file, String extractPath) throws Exception {
		ISevenZipInArchive inArchive = null;
		RandomAccessFile randomAccessFile = null;
		randomAccessFile = new RandomAccessFile(file, "r");
		inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
		inArchive.extract(null, false, new MyExtractCallback(inArchive, extractPath));
		if (inArchive != null) {
			inArchive.close();
		}
		if (randomAccessFile != null) {
			randomAccessFile.close();
		}
	}

	public static class MyExtractCallback implements IArchiveExtractCallback {
		private final ISevenZipInArchive inArchive;
		private final String extractPath;

		public MyExtractCallback(ISevenZipInArchive inArchive, String extractPath) {
			this.inArchive = inArchive;
			this.extractPath = extractPath;
		}
		
		public String dirPathForJarFile(String path)
		{
			String pathStr = null;
			if(path.contains(".jar"))
			{
			    pathStr = path.substring(path.lastIndexOf("\\"));				
			}
			return pathStr;
			
		}

		@Override
		public ISequentialOutStream getStream(final int index, ExtractAskMode extractAskMode) throws SevenZipException {
			return new ISequentialOutStream() {
				@Override
				public int write(byte[] data) throws SevenZipException {
					String filePath = inArchive.getStringProperty(index, PropID.PATH);
					FileOutputStream fos = null;
					try {
						File dir = new File(extractPath);
						File path = new File(extractPath + filePath);
						if (!dir.exists()) {
							dir.mkdirs();
						}
						if (!path.exists()) {
							String fileAbsolutePath = path.getAbsolutePath();
							if(fileAbsolutePath.contains(".jar"))
							{
							String pathForDirectory = dirPathForJarFile(fileAbsolutePath);
							path = new File(extractPath + pathForDirectory);
							}
							path.createNewFile();
						}
						
						fos = new FileOutputStream(path, true);
						fos.write(data);
					} catch (IOException e) {
						logger.severe(e.getLocalizedMessage());
					} finally {
						try {
							if (fos != null) {
								fos.flush();
								fos.close();
							}
						} catch (IOException e) {
							logger.severe(e.getLocalizedMessage());
						}
					}
					return data.length;
				}
			};
		}

		@Override
		public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {
		}

		@Override
		public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
		}

		@Override
		public void setCompleted(long completeValue) throws SevenZipException {
		}

		@Override
		public void setTotal(long total) throws SevenZipException {
		}
	}
}
