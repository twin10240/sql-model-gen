package org.sqlmodel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GeneratedSourceCompilationTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void generatedSourceCompilesAsJava8WithDzAndGsonTypes() throws Exception {
        String source = new DzModelRenderer().render(
                "com.company.hr.model",
                "EmployeeModel",
                Arrays.asList(
                        new ColumnSpec(1, "EMP_NM", "empNm", "String", ""),
                        new ColumnSpec(2, "ANNUAL_AMOUNT", "annualAmount", "BigDecimal", ""),
                        new ColumnSpec(3, "CREATED_AT", "createdAt", "LocalDateTime", "")));

        assertTrue(source.contains("import java.math.BigDecimal;"));
        assertTrue(source.contains("import java.time.LocalDateTime;"));
        assertTrue(source.contains("private String empNm;"));

        List<File> files = Arrays.asList(
                write("com/company/hr/model/EmployeeModel.java", source),
                write("com/douzone/gpd/jdbc/mybatis/model/DzAbstractModel.java",
                        "package com.douzone.gpd.jdbc.mybatis.model; public abstract class DzAbstractModel {}"),
                write("com/douzone/gpd/restful/annotation/DzModel.java",
                        "package com.douzone.gpd.restful.annotation; public @interface DzModel { String name(); String desc(); }"),
                write("com/douzone/gpd/restful/annotation/DzModelField.java",
                        "package com.douzone.gpd.restful.annotation; public @interface DzModelField { String name(); String desc(); String colName(); }"),
                write("com/google/gson/annotations/SerializedName.java",
                        "package com.google.gson.annotations; public @interface SerializedName { String value(); }"));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull("Tests require a JDK, not a JRE", compiler);
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            Boolean compiled = compiler.getTask(null, fileManager, null,
                    Arrays.asList("-source", "8", "-target", "8", "-d", temporaryFolder.newFolder("classes").getPath()),
                    null, fileManager.getJavaFileObjectsFromFiles(files)).call();
            assertTrue("Generated source must compile with Java 8 syntax and types", compiled);
        }
    }

    private File write(String relativePath, String contents) throws Exception {
        File file = new File(temporaryFolder.getRoot(), relativePath);
        Files.createDirectories(file.toPath().getParent());
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
