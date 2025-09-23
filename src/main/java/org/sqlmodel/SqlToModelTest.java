package org.sqlmodel;

import java.util.*;

public class SqlToModelTest {
    public static void main(String[] args) {
        String sql = "SELECT COMPANY_CD, PLANT_CD, ITEM_CD FROM TEST_TABLE";
        String src = SqlModel.generate(sql, "TestTableModel");
        System.out.println(src);

        String pagingSql = """
            SELECT *
            FROM (
                SELECT employee_id, first_name, last_name, salary,
                       ROW_NUMBER() OVER (ORDER BY employee_id) AS rn
                FROM employees
            ) WHERE rn > 10 AND rn <= 20
        """;
        String src2 = SqlModel.generatePagingAware(pagingSql, "EmpSlice", false);
        System.out.println(src2);
    }

}