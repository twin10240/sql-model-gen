package org.sqlmodel;

import static org.sqlmodel.FileCreator.createFile;

public class SqlToModelTest {
    public static void main(String[] args) {
        String packageName = "org.sqlmodel.model";
        String className = "PU_PURRCV_DTL";
        /**
         * 1. SqlModel.generate(SQL문, 클래스이름)
         * ex) SELECT 평문 -> SELECT A, B, C FROM DUAL
         *
         * 2. SqlModel.generatePagingAware(SQL문, 클래스이름, 페이징 컬럼 표기여부)
         * 가장 바깥쪽 컬럼이 *로 된 경우에 사용 -> 오라클 페이징 쿼리 같은...
         * **/
        String mstModel = SqlModel.generatePagingAware(SqlContents.mstList, className, false);
//        String dtlModel = SqlModel.generate(SqlContents.dtlList, "dtlModel");
//        String batchModel = SqlModel.generate(SqlContents.batchList, "batchModel");
//        String serModel = SqlModel.generate(SqlContents.serialList, "serModel");

        /**
         * 파라미터
         * 1. 패키지 이름
         * 2. 클래스 이름
         * 3. SQL -> Model로 변경된 텍스트
         * 4. 같은 경로에 있을 경우 덮어쓰기 유무(기본값 false) -> createFile(packageName, className, mstModel)
         * **/
        createFile(packageName, className, mstModel);
//        createFile("org.sqlmodel.model", "dtlModel", dtlModel);
//        createFile("org.sqlmodel.model", "batchModel", batchModel);
//        createFile("org.sqlmodel.model", "serModel", serModel);
    }
}