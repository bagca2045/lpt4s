package com.lpt4s.be;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lpt4s.be.InputParser.FormData.ColumnData;

import lombok.Getter;

@RestController
public class InputParser {

    // TODO: evtl. in property file auslagern
    private final String INPUT_FILE_PATH = "input";
    private final String INPUT_FILE_NAME = "schema.txt";

    private final String projectRoot = System.getProperty("user.dir");
    private final Path createSqlFilePath = Paths.get(projectRoot, "output", "create.sql");

    @PostMapping("/generateCRUD")
    public void generateCRUD(@RequestBody FormData formData) throws IOException {
        generateInputFile(formData);
        String createSqlStatement = consumeInputfileAndGenerateCreateSQLStatement();
        writeSQL2File(new StringBuilder(createSqlStatement));
        executeSQLScript();
    }


    /**
     * macht aus den eingaben aus der gui ein inputfile
     */
    private void generateInputFile(FormData formData) {
        String tableName = formData.getTableName();
        String filePath = "input/schema.txt";
    
        File file = new File(filePath);
        File folder = file.getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
    
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(tableName + "\n");
   
            for (ColumnData column : formData.getColumns()) {
                String columnName = column.getColumnName();
                String columnType = column.getColumnType();
                boolean mandatory = column.isMandatory();
   
                writer.write(columnName + " " + columnType + (mandatory ? " m" : "") + "\n");
            }
   
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        System.out.println("Schema file created successfully!");
    }

    @Getter
    public static class FormData {
        private String tableName;
        private List<ColumnData> columns;
    
        @Getter
        public static class ColumnData {
            private String columnName;
            private String columnType;
            private boolean mandatory;
        }
    }

    /**
     * macht aus dem inputfile ein create-sql statement
     * @return 
     * @throws IOException
     */
    public String consumeInputfileAndGenerateCreateSQLStatement() throws IOException {

        final String inputFile = INPUT_FILE_PATH + "/" + INPUT_FILE_NAME;
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));

        String tableName = reader.readLine();
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
        createTableSql.append("ID INT PRIMARY KEY NOT NULL, ");
        String line = reader.readLine();

        while (line != null) {
            StringTokenizer tokenizer = new StringTokenizer(line);

            String columnName = tokenizer.nextToken();
            String dataType = tokenizer.nextToken();
            String notNullString = "";
            if(tokenizer.hasMoreTokens()){
                String isMandatoryString = tokenizer.nextToken();
                if (isMandatoryString.equals("m")) {
                    notNullString = "NOT NULL";
                }
            }
            createTableSql.append(columnName + " " + convertColumnTypeLetterToText(dataType) + " " + notNullString +", ");     
            line = reader.readLine();
        }
        reader.close();
        replaceLastComma(createTableSql, ");");
        return createTableSql.toString();
    }

    private void replaceLastComma(StringBuilder input, String replacement) {
        int lastCommaIndex = input.lastIndexOf(",");
        if (lastCommaIndex >= 0) {
            input.replace(lastCommaIndex, lastCommaIndex + 1, replacement);
        }
    }

    private void executeSQLScript() {
        // TODO: hardcoded string ersetzen durch zugriff auf application.properties!
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "mysecretpassword")) {
            Statement statement;
                statement = connection.createStatement();
                String sqlScript = Files.readString(createSqlFilePath);
                statement.executeUpdate(sqlScript);
        } catch (SQLException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("SQL script executed successfully.");
    }

    /**
     * schreibt das create sql statement in eine datei "create.sql" im ordner "output"
     * wird überschrieben, wenn schon existiert.
     * 
     * @param createTableSql
     * @throws IOException
     */
    private void writeSQL2File(StringBuilder createTableSql) throws IOException {
        Files.createDirectories(createSqlFilePath.getParent());
        Files.write(createSqlFilePath, createTableSql.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String convertColumnTypeLetterToText(String dataType) {
        switch (dataType) {
            case "s":
                return "TEXT";
            case "d":
                return "REAL";
            case "i":
                return "INT";
            default:
                return "TEXT";
        }
    }

    // endpoint for table deletion
    @DeleteMapping("/deleteTable/{tableName}")
    public ResponseEntity<String> deleteTable(@PathVariable String tableName) {
        try {
            // Implement logic to delete the table with the given name
            deleteTableInDatabase(tableName);
            return ResponseEntity.ok("Table '" + tableName + "' has been deleted.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to delete table '" + tableName + "': " + e.getMessage());
        }
    }
    
    // method to delete one table in the database
    private void deleteTableInDatabase(String tableName) {
        // TODO: hardcoded string ersetzen durch zugriff auf application.properties!
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "mysecretpassword")) {
            Statement statement;
            statement = connection.createStatement();
            String sqlScript = "DROP TABLE " + tableName;
            statement.executeUpdate(sqlScript);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("SQL delete script executed successfully.");
    }

    @GetMapping("/getTablesAndColumns")
    public List<TableInfo> getTablesAndColumns() {
        List<TableInfo> tableInfoList = new ArrayList<>();

        //TODO: hardcoded string ersetzen durch zugriff auf application.properties! oder als datasource injecten
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "mysecretpassword")){
            DatabaseMetaData metaData = connection.getMetaData();
            // public is schema... is wichtig!
            ResultSet tables = metaData.getTables(null, "public", null, new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                List<String> columns = new ArrayList<>();

                ResultSet columnsResultSet = metaData.getColumns(null, null, tableName, null);
                while (columnsResultSet.next()) {
                    String columnName = columnsResultSet.getString("COLUMN_NAME");
                    columns.add(columnName);
                }

                TableInfo tableInfo = new TableInfo(tableName, columns);
                tableInfoList.add(tableInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tableInfoList;
    }

    public class TableInfo {
        private String tableName;
        private List<String> columns;
    
        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public TableInfo(String tableName, List<String> columns) {
            this.tableName = tableName;
            this.columns = columns;
        }

    }
    
}