package org.sqlmodel;

import java.io.FileWriter;
import java.io.IOException;

public class FileCreator {
    public static void createFile(String packageName, String className, String classCode) {
        createFile(packageName, className, classCode, false);
    }

    public static void createFile(String packageName, String className, String classCode, boolean isOverWrite) {
        String filePath = "src/main/java/" + packageName.replace('.', '/') + "/" + className + ".java";

        try (FileWriter writer = new FileWriter(filePath, isOverWrite)) {
            writer.write(classCode);
            System.out.println("모델 파일이 성공적으로 생성되었습니다: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
