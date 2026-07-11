package org.sqlmodel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainTest {
    @Test
    public void helpReturnsSuccess() {
        assertEquals(0, Main.run(new String[]{"--help"}));
    }
}
