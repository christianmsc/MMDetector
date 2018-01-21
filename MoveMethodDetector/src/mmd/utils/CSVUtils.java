package mmd.utils;

import java.io.FileWriter;
import java.io.IOException;

public class CSVUtils {

	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	public static void writeValidMoveMethod(String method, String target) {

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(System.getProperty("user.dir") + "/refactorings.csv", true);

			fileWriter.append(method);
			fileWriter.append(COMMA_DELIMITER);

			fileWriter.append(target);

			fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
	}
	
	public static void writeInGoldset(String method, String target) {

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(System.getProperty("user.home") + "/goldset.txt", true);

			fileWriter.append(method);
			
			fileWriter.append(" deve ser movido para ");

			fileWriter.append(target);

			fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error in CsvFileWriter !!!");
			e.printStackTrace();
		} finally {

			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}

		}
	}
}
