package org.sqlmodel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class DzModelRendererTest {
    @Test
    public void rendersExactDzSourceWithConditionalImportsAndEscapedDescription() {
        String source = new DzModelRenderer().render(
                "com.company.hr.model",
                "EmployeeModel",
                Arrays.asList(
                        new ColumnSpec(1, "EMP_NM", "empNm", "String", "사원 \"이름\"\\설명\n둘째 줄"),
                        new ColumnSpec(2, "annualAmount", "annualAmount", "BigDecimal", ""),
                        new ColumnSpec(3, "CREATED_AT", "createdAt", "LocalDateTime", "생성일")));

        assertEquals(
                "package com.company.hr.model;\n" +
                "\n" +
                "import com.douzone.gpd.jdbc.mybatis.model.DzAbstractModel;\n" +
                "import com.douzone.gpd.restful.annotation.DzModel;\n" +
                "import com.douzone.gpd.restful.annotation.DzModelField;\n" +
                "import com.google.gson.annotations.SerializedName;\n" +
                "import java.math.BigDecimal;\n" +
                "import java.time.LocalDateTime;\n" +
                "\n" +
                "@DzModel(name = \"EmployeeModel\", desc = \"\")\n" +
                "public class EmployeeModel extends DzAbstractModel {\n" +
                "\n" +
                "    @SerializedName(\"EMP_NM\")\n" +
                "    @DzModelField(name = \"EMP_NM\", desc = \"사원 \\\"이름\\\"\\\\설명\\n둘째 줄\", colName = \"EMP_NM\")\n" +
                "    private String empNm;\n" +
                "\n" +
                "    @SerializedName(\"annualAmount\")\n" +
                "    @DzModelField(name = \"annualAmount\", desc = \"\", colName = \"annualAmount\")\n" +
                "    private BigDecimal annualAmount;\n" +
                "\n" +
                "    @SerializedName(\"CREATED_AT\")\n" +
                "    @DzModelField(name = \"CREATED_AT\", desc = \"생성일\", colName = \"CREATED_AT\")\n" +
                "    private LocalDateTime createdAt;\n" +
                "\n" +
                "    public String getEmpNm() {\n" +
                "        return empNm;\n" +
                "    }\n" +
                "\n" +
                "    public void setEmpNm(String empNm) {\n" +
                "        this.empNm = empNm;\n" +
                "    }\n" +
                "\n" +
                "    public BigDecimal getAnnualAmount() {\n" +
                "        return annualAmount;\n" +
                "    }\n" +
                "\n" +
                "    public void setAnnualAmount(BigDecimal annualAmount) {\n" +
                "        this.annualAmount = annualAmount;\n" +
                "    }\n" +
                "\n" +
                "    public LocalDateTime getCreatedAt() {\n" +
                "        return createdAt;\n" +
                "    }\n" +
                "\n" +
                "    public void setCreatedAt(LocalDateTime createdAt) {\n" +
                "        this.createdAt = createdAt;\n" +
                "    }\n" +
                "}\n",
                source);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidPackageName() {
        new DzModelRenderer().render("com.bad-package", "EmployeeModel", Collections.<ColumnSpec>emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidClassName() {
        new DzModelRenderer().render("com.company", "class", Collections.<ColumnSpec>emptyList());
    }
}
