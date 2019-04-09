package com.ssc.ec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.codec.binary.Base64;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Attachment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;

public class CheckingJarConflict {

	static List<String> arrayList = new ArrayList<String>();

	public static void main(String[] args) {

		if (args.length < 5) {
			try {
				throw new Exception(
						"Please provide username,password,jira number,patch request document and jarName to find the conflict");
			} catch (Exception e) { // TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		URI jiraURI = null;
		try {
			jiraURI = new URI("https://engjira.int.kronos.com");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String username = args[0];
		String password = args[1];
		String jiraNumber = args[2];
		String jarName = args[3];
		String patchRequestDocumentName = "";
		if (args.length == 5) {
			patchRequestDocumentName = args[4];
		}

		else {
			for (int i = 4; i < args.length; i++) {
				if (i == args.length - 1) {
					patchRequestDocumentName = patchRequestDocumentName + args[i];
				} else {
					patchRequestDocumentName = patchRequestDocumentName + args[i] + " ";
				}
			}
		}

		AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();

		JiraRestClient client = factory.createWithBasicHttpAuthentication(jiraURI, username, password);
		IssueRestClient issueClient = client.getIssueClient();
		Promise<Issue> issue = issueClient.getIssue(jiraNumber);
		Iterable<Attachment> attachments = null;
		Issue keyIssue = null;
		try {
			keyIssue = issue.get();
			attachments = keyIssue.getAttachments();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Iterator<Attachment> attachMentsIterator = attachments.iterator();
		while (attachMentsIterator.hasNext()) {
			Attachment attachment = attachMentsIterator.next();
			// System.out.println(attachment.getFilename());
			if (attachment.getFilename().equalsIgnoreCase(patchRequestDocumentName)) {
				System.out.println(attachment.getContentUri());
				downloadUsingApacheIO(attachment.getContentUri(), username, password);
			}

		}

		JSONObject jsonObj = (JSONObject) keyIssue.getFieldByName("Patch Version").getValue();
		String patchVersion = null;
		try {
			patchVersion = (String) jsonObj.get("name");
			System.out.println(patchVersion);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String spVersion = patchVersion.substring(patchVersion.lastIndexOf(".") + 1);
		StringBuilder stringBuilder = new StringBuilder(patchVersion);
		if (Integer.parseInt(spVersion) > 9) {
			stringBuilder.replace(0, 1, "");
			stringBuilder.replace(2, 3, "");
		} else {
			stringBuilder.replace(0, 1, "");
			stringBuilder.replace(2, 3, "");
			stringBuilder.replace(4, 5, "");

		}
		System.out.println(stringBuilder);
		String suiteVersion = null;
		if (stringBuilder.toString().startsWith("8")) {
			suiteVersion = "8.0SUITE";
		} else {
			if (stringBuilder.toString().startsWith("7")) {
				suiteVersion = "7.0SUITE";
			}
		}
		readContentsFromAttachmentAndExtractPatches(stringBuilder.toString(), suiteVersion);
		System.out.println("Now Check the Code Conflict");
		conflictChecker(jarName);
		System.out.println("Now Delete the Files and Folders");
		recursiveDelete();

	}

	public static void downloadUsingApacheIO(URI uri, String username, String password) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) (uri.toURL()).openConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String userpass = username + ":" + password;
		String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
		conn.setRequestProperty("Authorization", basicAuth);
		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		conn.setRequestProperty("Accept", "application/json");
		try {
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Path targetPath = new File("C:\\Users\\" + System.getProperty("user.name") + "\\Downloads\\TempPatchDoc.docx")
				.toPath();
		try {
			Files.copy(conn.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void readContentsFromAttachmentAndExtractPatches(String patchVersion, String suiteVersion) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(
					"C:\\Users\\" + System.getProperty("user.name") + "\\Downloads\\TempPatchDoc.docx");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XWPFDocument xdoc = null;
		try {
			xdoc = new XWPFDocument(OPCPackage.open(fis));
		} catch (InvalidFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Iterator<IBodyElement> bodyElementIterator = xdoc.getBodyElementsIterator();
		while (bodyElementIterator.hasNext()) {
			IBodyElement element = bodyElementIterator.next();

			if ("TABLE".equalsIgnoreCase(element.getElementType().name())) {
				List<XWPFTable> tableList = element.getBody().getTables();
				for (XWPFTable table : tableList) {
					System.out.println("Total Number of Rows of Table:" + table.getNumberOfRows());
					for (int i = 0; i < table.getRows().size(); i++) {

						for (int j = 0; j < table.getRow(i).getTableCells().size(); j++) {
							if (i == 6 && j == 2) {
								System.out.println(table.getRow(i).getCell(j).getText());
								searchTheNetworkForThePatchesAndExtractThem(
										listOfPatches(table.getRow(i).getCell(j).getText()), patchVersion,
										suiteVersion);
							}

						}
					}
				}
			}
		}
	}

	public static void searchTheNetworkForThePatchesAndExtractThem(List<String> list, String patchVersion,
			String suiteVersion) {
		String pathpart1 = "\\\\kfs-us-devblds\\kits\\" + suiteVersion + "\\PATCHES\\WFC\\" + patchVersion
				+ "\\kronos\\INSTALL\\PATCHES";

		for (int i = 0; i < list.size(); i++) {
			String str = list.get(i);
			String pathpart2 = "\\" + str + "\\WINDOWS";
			String path = pathpart1 + pathpart2;
			String localPath = "C:\\Users\\" + System.getProperty("user.name") + "\\Downloads\\" + str + "\\";
			arrayList.add(localPath);
			copyFiles(path, localPath);
		}
	}

	public static List<String> listOfPatches(String str) {

		String array = str.replaceAll("-", "");
		List<String> listOfPatches = new ArrayList<String>();

		String strAfterReplacing = array.replaceAll("\\s", "");
		String[] strArray = strAfterReplacing.split("CENG");
		for (int i = 0; i < strArray.length; i++) {
			// System.out.println(strArray[i]);
			try {
				if (isInteger(strArray[i])) {
					System.out.println(Integer.parseInt(strArray[i]));
					listOfPatches.add("CENG" + strArray[i]);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return listOfPatches;
	}

	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		if (str.isEmpty()) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (str.length() == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < str.length(); i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	private static void copyFile(String sourceFileName, String destionFileName) {
		try {
			System.out.println("Reading..." + sourceFileName);
			File sourceFile = new File(sourceFileName);
			File destinationFile = new File(destionFileName);
			InputStream in = new FileInputStream(sourceFile);
			OutputStream out = new FileOutputStream(destinationFile);

			byte[] buffer = new byte[1024];
			int length;
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
			in.close();
			out.close();
			System.out.println("Copied: " + sourceFileName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void copyFiles(String fileLocationSource, String fileLocationDestination) {
		File inputLocation = new File(fileLocationSource);
		File destinationLocation = new File(fileLocationDestination);
		if (!destinationLocation.exists()) {
			destinationLocation.mkdirs();
		}

		if (inputLocation.isDirectory()) {
			System.out.println("Listing the files...");
			File[] attachmentFiles = inputLocation.listFiles();
			System.out.println("Total files in the folder: " + attachmentFiles.length);
			for (File aFile : attachmentFiles) {
				if (!aFile.isDirectory()) {
					String fileName = aFile.getName();
					String sourceFileName = aFile.getAbsolutePath();
					String destinationFileName = fileLocationDestination + fileName;
					copyFile(sourceFileName, destinationFileName);
				}

			}
		}
		System.out.println("Completed...");
	}

	public static void conflictChecker(String jarName) {

		if (arrayList.size() > 0) {
			for (int i = 0; i < arrayList.size(); i++) {
				UnzippingArchive.extractPatchAndReadJar(arrayList.get(i), arrayList.get(i));
				File file = new File(arrayList.get(i));
				if (file.exists()) {
					if (file.isDirectory()) {
						File[] attachMentFiles = file.listFiles();
						for (File afile : attachMentFiles) {
							if (afile.getName().equals(jarName)) {
								System.out.println("Code Conflict Present for the jar " + afile.getName()
										+ " Conflict present for issue : " + file.getName());
								break;
							}

						}

					}
				}

			}
		}
	}

	public static void recursiveDelete() {
		if (arrayList.size() > 0) {

			for (int i = 0; i < arrayList.size(); i++) {

				File file = new File(arrayList.get(i));

				if (!file.exists())
					return;
						// call recursively
					recursiveDelete(file);
					}
				}
			
		
	}

	public static void recursiveDelete(File f) {

		if (f.isDirectory()) {
			for (File fi : f.listFiles()) {
				// call recursively
				recursiveDelete(fi);
			}
		}
		// call delete to delete files and empty directory
		f.delete();
		System.out.println("Deleted file/folder: " + f.getAbsolutePath());

	}

}
