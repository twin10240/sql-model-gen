package org.sqlmodel;

public class SqlContents {

    public static String mstList = "SELECT *\n" +
            "          FROM ( SELECT MAX(RCVIF.INTL_HIS_SQ) OVER(PARTITION BY RCVIF.COMPANY_CD, RCVIF.INTL_SYS_CD, RCVIF.RELATION_CD, RCVIF.SEND_RCV_FG, RCVIF.INTL_DOC_NO, RCVIF.CNCL_YN) AS MAX_INTL_HIS_SQ,\n" +
            "                        RCVIF.COMPANY_CD,\n" +
            "                        RCVIF.INTL_SYS_CD,\n" +
            "                        RCVIF.INTL_HIS_SQ,\n" +
            "                        RCVIF.RELATION_CD,\n" +
            "                        RCVIF.SEND_RCV_FG,\n" +
            "                        RCVIF.CRUD_FG,\n" +
            "                        RCVIF.INTL_DOC_NO,\n" +
            "                        ( CASE WHEN RCVIF.CNCL_YN = 'Y' THEN RCVIF.CNCL_YN ELSE 'N' END )AS CNCL_YN\n" +
            "\n" +
            "                        ,COALESCE(COMLAN.COMPANY_NM, CC.COMPANY_NM)    AS COMPANY_NM\n" +
            "                        ,COALESCE(RELLAN.SYSDEF_NM,  P00090.SYSDEF_NM) AS RELATION_NM\n" +
            "                        ,COALESCE(SECLEN.SYSDEF_NM,  P00080.SYSDEF_NM) AS SEND_RCV_FG_NM\n" +
            "                        ,COALESCE(SYSLAN.INTL_SYS_NM,CIM.INTL_SYS_NM)  AS INTL_SYS_NM\n" +
            "\n" +
            "                   FROM PU_PURRCV_IF RCVIF\n" +
            "                             INNER JOIN CI_SYSCOMIF_INFO CSI ON CSI.INTL_SYS_CD = RCVIF.INTL_SYS_CD\n" +
            "                                                            AND CSI.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                             INNER JOIN CI_SYSTEMIF_MST CIM ON CIM.INTL_SYS_CD = RCVIF.INTL_SYS_CD\n" +
            "                             INNER JOIN CI_SYSTEMIF_DTL CID ON CID.INTL_SYS_CD = CIM.INTL_SYS_CD\n" +
            "                                                           AND CID.RELATION_CD = RCVIF.RELATION_CD\n" +
            "                        LEFT OUTER JOIN CI_COMPANY CC ON CC.COMPANY_CD = RCVIF.COMPANY_CD\n" +
            "                        LEFT OUTER JOIN MA_CODEDTL P00090 ON P00090.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                         AND P00090.SYSDEF_CD   = RCVIF.RELATION_CD\n" +
            "                                                         AND P00090.MODULE_CD   = 'CI'\n" +
            "                                                         AND P00090.FIELD_CD    = 'P00090'\n" +
            "                        LEFT OUTER JOIN MA_CODEDTL P00080 ON P00080.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                         AND P00080.SYSDEF_CD   = RCVIF.SEND_RCV_FG\n" +
            "                                                         AND P00080.MODULE_CD   = 'CI'\n" +
            "                                                         AND P00080.FIELD_CD    = 'P00080'\n" +
            "\n" +
            "                        LEFT OUTER JOIN MA_COMPANY_LDTL COMLAN ON COMLAN.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                              AND COMLAN.LANG_CD     = 'KO'\n" +
            "                        LEFT OUTER JOIN MA_CODEDTL_SDTL RELLAN ON RELLAN.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                              AND RELLAN.SYSDEF_CD   = RCVIF.RELATION_CD\n" +
            "                                                              AND RELLAN.MODULE_CD   = 'CI'\n" +
            "                                                              AND RELLAN.FIELD_CD    = 'P00090'\n" +
            "                                                              AND RELLAN.LANG_CD     = 'KO'\n" +
            "                        LEFT OUTER JOIN MA_CODEDTL_SDTL SECLEN ON SECLEN.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                              AND SECLEN.SYSDEF_CD   = RCVIF.SEND_RCV_FG\n" +
            "                                                              AND SECLEN.MODULE_CD   = 'CI'\n" +
            "                                                              AND SECLEN.FIELD_CD    = 'P00080'\n" +
            "                                                              AND SECLEN.LANG_CD     = 'KO'\n" +
            "                        LEFT OUTER JOIN CI_SYSTEMIF_LDTL SYSLAN ON SYSLAN.INTL_SYS_CD = RCVIF.INTL_SYS_CD\n" +
            "                                                               AND SYSLAN.LANG_CD     = 'KO'\n" +
            "\n" +
            "                  WHERE RCVIF.COMPANY_CD   = '6000'\n" +
            "                    AND RCVIF.PLANT_CD     = '1000'\n" +
            "\n" +
            "                    AND RCVIF.RELATION_CD  = '320'\n" +
            "\n" +
            "                    AND RCVIF.INTL_DTS BETWEEN FN_CM_TO_DATE('20250901000000') AND FN_CM_TO_DATE('20250924235959')\n" +
            "\n" +
            "               )A\n" +
            "         WHERE A.INTL_HIS_SQ = A.MAX_INTL_HIS_SQ\n" +
            "         ORDER BY A.INTL_SYS_CD ASC ,A.MAX_INTL_HIS_SQ ASC";

    public static String dtlList = "SELECT RCVIF.INTL_SYS_CD,\n" +
            "               RCVIF.INTL_DOC_NO,\n" +
            "               RCVIF.INTL_DOC_SQ,\n" +
            "               RCVIF.INTL_DTS,\n" +
            "               RCVIF.INTL_ST,\n" +
            "               RCVIF.ITEM_CD,\n" +
            "               RCVIF.ITEM_GRP_CD,\n" +
            "               RCVIF.RCPT_PROC_DT,\n" +
            "               RCVIF.PO_UNIT_CD,\n" +
            "               RCVIF.PROC_QT,\n" +
            "               RCVIF.SL_CD,\n" +
            "               RCVIF.PURDOC_NO,\n" +
            "               RCVIF.PURDOC_SQ,\n" +
            "               RCVIF.PURDVYSCH_SQ,\n" +
            "               RCVIF.INTL_ERR_MSG_DC,\n" +
            "               RCVIF.RCPT_NO,\n" +
            "               RCVIF.RCPT_SQ\n" +
            "\n" +
            "               ,COALESCE(INSLAN.SYSDEF_NM,  P00070.SYSDEF_NM) AS INTL_NM\n" +
            "               ,COALESCE(CILAN.ITEM_NM,     RCVIF.ITEM_NM)    AS ITEM_NM\n" +
            "               ,COALESCE(IGLAN.ITEM_GRP_NM, MI.ITEM_GRP_NM)   AS ITEM_GRP_NM\n" +
            "               ,COALESCE(MSLAN.SL_NM,       MSI.SL_NM)        AS SL_NM\n" +
            "\n" +
            "          FROM PU_PURRCV_IF RCVIF\n" +
            "                    INNER JOIN CI_SYSCOMIF_INFO CSI ON CSI.INTL_SYS_CD = RCVIF.INTL_SYS_CD\n" +
            "                                                   AND CSI.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                    INNER JOIN CI_SYSTEMIF_MST  CIM ON CIM.INTL_SYS_CD   = RCVIF.INTL_SYS_CD\n" +
            "                    INNER JOIN CI_SYSTEMIF_DTL CID ON CID.INTL_SYS_CD   = CIM.INTL_SYS_CD\n" +
            "                                                  AND CID.RELATION_CD   = RCVIF.RELATION_CD\n" +
            "               LEFT OUTER JOIN MA_CODEDTL P00070 ON P00070.COMPANY_CD = RCVIF.COMPANY_CD\n" +
            "                                                AND P00070.SYSDEF_CD  = RCVIF.INTL_ST\n" +
            "                                                AND P00070.MODULE_CD  = 'CI'\n" +
            "                                                AND P00070.FIELD_CD   = 'P00070'\n" +
            "               LEFT OUTER JOIN MA_ITEMGRP_INFO MI ON MI.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                 AND MI.ITEM_GRP_CD = RCVIF.ITEM_GRP_CD\n" +
            "               LEFT OUTER JOIN MA_SL_INFO MSI ON MSI.COMPANY_CD = RCVIF.COMPANY_CD\n" +
            "                                             AND MSI.PLANT_CD   = RCVIF.PLANT_CD\n" +
            "                                             AND MSI.SL_CD      = RCVIF.SL_CD\n" +
            "\n" +
            "               LEFT OUTER JOIN CI_ITEMLANG_SDTL CILAN ON CILAN.ITEM_CD = RCVIF.ITEM_CD\n" +
            "                                                     AND CILAN.LANG_CD = 'KO'\n" +
            "               LEFT OUTER JOIN MA_SLLANG_INFO MSLAN ON MSLAN.COMPANY_CD = RCVIF.COMPANY_CD\n" +
            "                                                   AND MSLAN.PLANT_CD   = RCVIF.PLANT_CD\n" +
            "                                                   AND MSLAN.SL_CD      = RCVIF.SL_CD\n" +
            "                                                   AND MSLAN.LANG_CD    = 'KO'\n" +
            "               LEFT OUTER JOIN MA_ITEMGRPLANG_DTL IGLAN ON IGLAN.COMPANY_CD  = RCVIF.COMPANY_CD\n" +
            "                                                       AND IGLAN.ITEM_GRP_CD = RCVIF.ITEM_GRP_CD\n" +
            "                                                       AND IGLAN.LANG_CD     = 'KO'\n" +
            "               LEFT OUTER JOIN MA_CODEDTL_SDTL INSLAN ON INSLAN.COMPANY_CD = RCVIF.COMPANY_CD\n" +
            "                                                     AND INSLAN.SYSDEF_CD  = RCVIF.INTL_ST\n" +
            "                                                     AND INSLAN.MODULE_CD  = 'CI'\n" +
            "                                                     AND INSLAN.FIELD_CD   = 'P00070'\n" +
            "                                                     AND INSLAN.LANG_CD    = 'KO'\n" +
            "\n" +
            "         WHERE RCVIF.COMPANY_CD  = '6000'\n" +
            "           AND RCVIF.INTL_DOC_NO = 'RCV2025090001'\n" +
            "\n" +
            "           AND RCVIF.INTL_SYS_CD  = 'PU_API_TEST'\n" +
            "\n" +
            "           AND (RCVIF.CNCL_YN IS NULL OR RCVIF.CNCL_YN = '' OR RCVIF.CNCL_YN = 'N')";

    public static String batchList = "SELECT BAT.INTL_SYS_CD,\n" +
            "               BAT.BATCH_NO,\n" +
            "               BAT.ORD_QT\n" +
            "          FROM PU_PURRCVLOT_IF BAT\n" +
            "         WHERE BAT.COMPANY_CD  = '6000'\n" +
            "           AND BAT.INTL_SYS_CD = 'PU_API_TEST'\n" +
            "           AND BAT.INTL_DOC_NO = 'RCV2025090001'\n" +
            "           AND BAT.INTL_DOC_SQ = '1'";

    public static String serialList = "SELECT SER.INTL_SYS_CD,\n" +
            "               SER.SER_NO\n" +
            "          FROM PU_PURRCVSER_IF SER\n" +
            "         WHERE SER.COMPANY_CD  = '6000'\n" +
            "           AND SER.INTL_SYS_CD = 'PU_API_TEST'\n" +
            "           AND SER.INTL_DOC_NO = 'RCV2025090001'\n" +
            "           AND SER.INTL_DOC_SQ = '1'";
}
