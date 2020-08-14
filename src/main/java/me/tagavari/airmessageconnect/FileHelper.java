package me.tagavari.airmessageconnect;

import java.io.File;

public class FileHelper {
	static final String regExSplitFilename = "\\.(?=[^.]+$)";
	
	static File findFreeFile(File directory, String fileName, String separator, int startIndex) {
		//Creating the file
		File file = new File(directory, fileName);
		
		//Checking if the file directory doesn't exist
		if(!directory.exists()) {
			//Creating the directory
			directory.mkdir();
			
			//Returning the file
			return file;
		}
		
		//Getting the file name and extension
		String[] fileData = file.getName().split(regExSplitFilename);
		String baseFileName = fileData[0];
		String fileExtension = fileData.length > 1 ? fileData[1] : "";
		int currentIndex = startIndex;
		
		//Finding a free file
		while(file.exists()) file = new File(directory, baseFileName + separator + currentIndex++ + '.' + fileExtension);
		
		//Returning the file
		return file;
	}
}