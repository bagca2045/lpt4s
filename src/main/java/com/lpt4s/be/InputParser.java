package com.lpt4s.be;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InputParser {

    // TODO: evtl. in property file auslagern
    private final String INPUT_FILE_PATH = "input";
    private final String INPUT_FILE_NAME = "schema.txt";

    private final String projectRoot = System.getProperty("user.dir");
    private final Path createSqlFilePath = Paths.get(projectRoot, "output", "create.sql");

    @PostMapping("/generateCRUD")
    public void generateCRUD() throws IOException {
        consume();
    }

    public void consume() throws IOException {

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
            createTableSql.append(columnName + " " + getColumnType(dataType) + " " + notNullString +", ");     
            line = reader.readLine();
        }
        reader.close();
        replaceLastComma(createTableSql, ");");
        writeSQL2File(createTableSql, tableName);
        executeSQLScript();

        // TODO: 
        // DROP TABLE tableName;
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

    private void writeSQL2File(StringBuilder createTableSql, String tableName) throws IOException {
        
        if (!Files.exists(createSqlFilePath)) {
            Files.createDirectories(createSqlFilePath.getParent());
            Files.createFile(createSqlFilePath);
            Files.write(createSqlFilePath, createTableSql.toString().getBytes(), java.nio.file.StandardOpenOption.CREATE);
        }
    }

    private String getColumnType(String dataType) {
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
}