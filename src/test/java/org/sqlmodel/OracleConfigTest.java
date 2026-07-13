package org.sqlmodel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OracleConfigTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void loadsUtf8Properties() throws Exception {
        Path path = temporaryFolder.newFile("oracle.properties").toPath();
        Files.write(path, ("oracle.url=jdbc:oracle:thin:@localhost:1521/ORCL\n"
                + "oracle.username=사용자\n"
                + "oracle.password=비밀번호\n"
                + "oracle.schema=인사\n").getBytes(StandardCharsets.UTF_8));

        OracleConfig config = OracleConfig.load(path);

        assertEquals("jdbc:oracle:thin:@localhost:1521/ORCL", config.url());
        assertEquals("사용자", config.username());
        assertEquals("비밀번호", config.password());
        assertEquals("인사", config.schema());
    }

    @Test
    public void usesFixedDefaultPath() {
        assertEquals(Paths.get("C:\\Douzone\\dews-web\\config\\modelconvertor\\oracle.properties"),
                OracleConfig.defaultPath());
    }

    @Test
    public void reportsEveryMissingOrBlankRequiredKey() throws Exception {
        Path path = temporaryFolder.newFile("incomplete.properties").toPath();
        Files.write(path, ("oracle.url= \n"
                + "oracle.username=user\n").getBytes(StandardCharsets.UTF_8));

        try {
            OracleConfig.load(path);
            fail("Expected missing configuration to be rejected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("oracle.url"));
            assertTrue(e.getMessage().contains("oracle.password"));
            assertTrue(e.getMessage().contains("oracle.schema"));
        }
    }

    @Test
    public void toStringRedactsPassword() throws Exception {
        Path path = temporaryFolder.newFile("secret.properties").toPath();
        Files.write(path, ("oracle.url=jdbc:test\n"
                + "oracle.username=user\n"
                + "oracle.password=top-secret-value\n"
                + "oracle.schema=APP\n").getBytes(StandardCharsets.UTF_8));

        String text = OracleConfig.load(path).toString();

        assertFalse(text.contains("top-secret-value"));
        assertTrue(text.contains("password=***"));
    }

    @Test
    public void preservesWhitespaceInPassword() throws Exception {
        Path path = temporaryFolder.newFile("whitespace-password.properties").toPath();
        Files.write(path, ("oracle.url=jdbc:test\n"
                + "oracle.username=user\n"
                + "oracle.password=\\ secret \n"
                + "oracle.schema=APP\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(" secret ", OracleConfig.load(path).password());
    }
}
