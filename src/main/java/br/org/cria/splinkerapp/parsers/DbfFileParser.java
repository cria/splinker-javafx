package br.org.cria.splinkerapp.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;

import br.org.cria.splinkerapp.ApplicationLog;
import br.org.cria.splinkerapp.utils.StringStandards;

public class DbfFileParser extends FileParser
{
    String fileSourcePath;
	List<String> columnNameList = new ArrayList<String>();
    DBFReader reader = null;
	String fileName;
    public DbfFileParser(String fileSourcePath) throws Exception
	{
        this.fileSourcePath = fileSourcePath;
		try 
		{
			var file = new File(fileSourcePath);
			var name = file.getName();
			var stream = new FileInputStream(file);
			
			reader = new DBFReader(stream);
			this.fileName = name.substring(0, name.lastIndexOf("."));
		} 
		catch (DBFException e) 
		{
			ApplicationLog.error(e.getLocalizedMessage());
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			ApplicationLog.error(e.getLocalizedMessage());
			e.printStackTrace();
		}
    }
	@Override
	public void insertDataIntoTable() throws SQLException 
	{
		Object[] rowObjects;
		var conn = getConnection();
		int numberOfColumns = reader.getFieldCount();
		var tableName = getTableName();
		var valuesStr = "?,".repeat(numberOfColumns);
        var columnNames = String.join(",", columnNameList);
		totalRowCount = reader.getRecordCount();
		while ((rowObjects = reader.nextRecord()) != null) 
		{
			
			var valuesList = getRowAsStringList(rowObjects, numberOfColumns).stream().toList();
            var command = insertIntoCommand.formatted(tableName, columnNames, valuesStr).replace(",)", ")");
            var statement = conn.prepareStatement(command);
                            
            for (int k = 0; k < valuesList.size(); k++) 
            {
            	statement.setString(k+1, valuesList.get(k));    
            }
            statement.executeUpdate();
			statement.close();
			currentRow++;
			readRowEventBus.post(currentRow);    
		}
		conn.close();
		reader.close();
	}
	@Override
	public String buildCreateTableCommand() throws Exception
	{
        
			int numberOfFields = reader.getFieldCount();
			
        	var builder = new StringBuilder();
			var tableName = getTableName();
			builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
			dropTable(tableName);
			for (int i = 0; i < numberOfFields; i++) 
			{
				var field = reader.getField(i);
				var fieldName = field.getName();
				String columnName = "`%s`".formatted(StringStandards.normalizeString(fieldName));
				columnNameList.add(columnName);
                builder.append("%s VARCHAR(1),".formatted(columnName));
					
			}
            
			builder.append(");");
        	var command = builder.toString().replace(",);", ");");
			return command;
	}

	@Override
	protected List<String> getRowAsStringList(Object row, int numberOfColumns) 
	{
		var fullRow = (Object[]) row;
        var list = new ArrayList<String>();
        
        for (int colNum = 0; colNum < numberOfColumns; colNum++) 
		{
            var value = fullRow[colNum];
            list.add(value == null? "": value.toString());
        }

        return list;

    }

	@Override
	protected String getTableName()
	{
		return this.fileName;
	}

}
