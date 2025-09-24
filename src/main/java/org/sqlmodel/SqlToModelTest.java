package org.sqlmodel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SqlToModelTest {
    public static void main(String[] args) {

        String mstModel = SqlModel.generatePagingAware(SqlContents.mstList, "mstModel", false);
        String dtlModel = SqlModel.generate(SqlContents.dtlList, "dtlModel");
        String batchModel = SqlModel.generate(SqlContents.batchList, "batchModel");
        String serModel = SqlModel.generate(SqlContents.serialList, "serModel");

        createFile("org.sqlmodel.model", "mstModel", mstModel);
        createFile("org.sqlmodel.model", "dtlModel", dtlModel);
        createFile("org.sqlmodel.model", "batchModel", batchModel);
        createFile("org.sqlmodel.model", "serModel", serModel);
    }

    private static void createFile(String packageName, String className, String classCode) {
        String filePath = "src/main/java/" + packageName.replace('.', '/') + "/" + className + ".java";

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(classCode);
            System.out.println("모델 파일이 성공적으로 생성되었습니다: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}