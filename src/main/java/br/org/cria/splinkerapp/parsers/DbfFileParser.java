package br.org.cria.splinkerapp.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;

public class DbfFileParser extends FileParser
{
    String fileSourcePath;
	List<String> columnNameList = new ArrayList<String>();
    DBFReader reader = null;
    public DbfFileParser(String fileSourcePath)
	{
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
	@Override
	public void insertDataIntoTable() throws SQLException 
	{
		Object[] rowObjects;
		var conn = getConnection();
		int numberOfColumns = reader.getFieldCount();
		var tableName = "spLinker";
		//var columns = this.columnNameList.stream().map((col) ->  "`%s`".formatted(normalizeString(col))).toList();
        var valuesStr = "?,".repeat(numberOfColumns);
        var columnNames = String.join(",", columnNameList);
		var rowNum = 1;
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
            var response = statement.executeUpdate();    
			System.out.println("inserting %s line response: %s".formatted(rowNum, response));
			rowNum++;
		}
		System.out.println("inserted all data in the DB");
		conn.close();
	}
	@Override
	public String buildCreateTableCommand() 
	{
        
			int numberOfFields = reader.getFieldCount();
        	var builder = new StringBuilder();
			var tableName = "spLinker";
			builder.append("CREATE TABLE IF NOT EXISTS %s (".formatted(tableName));
			
			for (int i = 0; i < numberOfFields; i++) 
			{

				String columnName = "`%s`".formatted(normalizeString(reader.getField(i).getName()));
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
        
        for (int colNum = 0; colNum < numberOfColumns; colNum++) {
            var value = fullRow[colNum];
            list.add(value == null? "": value.toString());

        }

        return list;

    }

}
