package br.org.cria.splinkerapp.services.implementations;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFUtils;

public class DbfParser {
    String fileSourcePath;
	List<String> columnNameList = new ArrayList<String>();
    DBFReader reader = null;
    public DbfParser(String fileSourcePath){
        this.fileSourcePath = fileSourcePath;
		try 
		{
			reader = new DBFReader(new FileInputStream(fileSourcePath));
		} 
		catch (DBFException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		// finally 
		// {
		// 	DBFUtils.close(reader);
		// }
    }

	public void insertDataIntoTable() throws SQLException{
		Object[] rowObjects;
		var conn = getConnection();
		int numberOfColumns = reader.getFieldCount();
		var tableName = "spLinker";
		var columns = this.columnNameList.stream().map((col) -> normalizeString(col)).toList();
        var valuesStr = "?,".repeat(numberOfColumns);
        var columnNames = String.join(",", columns);
		while ((rowObjects = reader.nextRecord()) != null) 
		{
			
			var valuesList = getRowAsStringList(rowObjects, numberOfColumns).stream().map("'%s'"::formatted).toList();
            var commandBase = "INSERT INTO %s (%s) VALUES (%s);";
            var command = commandBase.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
            var statement = conn.prepareStatement(command);
                            
            for (int k = 0; k < valuesList.size(); k++) 
            {
            	statement.setString(k+1, valuesList.get(k));    
            }
            statement.executeUpdate();    
		}
		conn.close();
	}
	public String buildCreateTableCommand() 
	{
        
			int numberOfFields = reader.getFieldCount();
        	var builder = new StringBuilder();
			var tableName = "spLinker";
			builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
			
			for (int i = 0; i < numberOfFields; i++) 
			{

				String columnName = normalizeString(reader.getField(i).getName());
				columnNameList.add(columnName);
                builder.append("%s VARCHAR(1),".formatted(columnName));
					
			}
            
			builder.append(");");
        	return builder.toString().replace(",);", ");");
	}
	protected String normalizeString(String str) 
    {
    	return StringUtils.stripAccents(str.toLowerCase()).replace(" ", "_")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "_").trim();
    }
	protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:splinker.db");
    }

	 protected List<String> getRowAsStringList(Object[] row, int numberOfCells) {
        var list = new ArrayList<String>();
        
        for (int colNum = 0; colNum < numberOfCells; colNum++) {
            var value = row[colNum];
            list.add(value == null? "": value.toString());

        }

        return list;

    }
	public void createTableBasedOnSheet() throws SQLException {
        var command = buildCreateTableCommand();
        var conn = getConnection();
        var statement = conn.createStatement();
        var result = statement.executeUpdate(command);
        System.out.println(result);
    }

}
