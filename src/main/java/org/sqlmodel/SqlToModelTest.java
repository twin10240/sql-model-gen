package org.sqlmodel;

import static org.sqlmodel.FileCreator.createFile;

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
}