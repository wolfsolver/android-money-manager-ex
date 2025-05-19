    SELECT 
        'CHECKINGACCOUNT_V1 MISSING ACCOUNT' AS error_code, 
        'E' as severity, 
        'CHECKINGACCOUNT_V1' AS source_table_name, 
        'TRANSID' AS source_key_name, 
        CHECKINGACCOUNT_V1.TRANSID AS source_key_value, 
        NULL AS source_column_name, 
        NULL AS source_column_value, 
        'ACCOUNTID' AS source_foreignkey_column, 
        CHECKINGACCOUNT_V1.ACCOUNTID AS source_foreignkey_value, 
        'ACCOUNTID' AS associated_key_name, 
        ACCOUNTLIST_V1.ACCOUNTID AS associated_key_value, 
        NULL AS associated_column_name, 
        NULL AS associated_column_value 
    FROM CHECKINGACCOUNT_V1 
    LEFT JOIN ACCOUNTLIST_V1 ON ACCOUNTLIST_V1.ACCOUNTID = CHECKINGACCOUNT_V1.ACCOUNTID 
    WHERE ACCOUNTLIST_V1.ACCOUNTID IS NULL AND CHECKINGACCOUNT_V1.ACCOUNTID > 0 AND (CHECKINGACCOUNT_V1.DELETEDTIME IS NULL OR CHECKINGACCOUNT_V1.DELETEDTIME = "")
    
    UNION SELECT 
        'CHECKINGACCOUNT_V1 MISSING TOACCOUNT' AS error_code, 
        'E' as severity, 
        'CHECKINGACCOUNT_V1' AS source_table_name, 
        'TRANSID' AS source_key_name, 
        CHECKINGACCOUNT_V1.TRANSID AS source_key_value, 
        NULL AS source_column_name, 
        NULL AS source_column_value, 
        'TOACCOUNTID' AS source_foreignkey_column, 
        CHECKINGACCOUNT_V1.TOACCOUNTID AS source_foreignkey_value, 
        'TOACCOUNTID' AS associated_key_name, 
        ACCOUNTLIST_V1.ACCOUNTID AS associated_key_value, 
        NULL AS associated_column_name, 
        NULL AS associated_column_value 
    FROM CHECKINGACCOUNT_V1 
    LEFT JOIN ACCOUNTLIST_V1 ON ACCOUNTLIST_V1.ACCOUNTID = CHECKINGACCOUNT_V1.TOACCOUNTID 
    WHERE ACCOUNTLIST_V1.ACCOUNTID IS NULL AND CHECKINGACCOUNT_V1.TOACCOUNTID > 0 AND (CHECKINGACCOUNT_V1.DELETEDTIME IS NULL OR CHECKINGACCOUNT_V1.DELETEDTIME = "")
    
    UNION SELECT 
        'DELETEDTIME_NULL' AS error_code, 
        'I' as severity, 
        'CHECKINGACCOUNT_V1' AS source_table_name, 
        'TRANSID' AS source_key_name, 
        CHECKINGACCOUNT_V1.TRANSID AS source_key_value, 
        'DELETEDTIME' AS source_column_name, 
        CHECKINGACCOUNT_V1.DELETEDTIME AS source_column_value, 
        NULL AS source_foreignkey_column,
        NULL AS source_foreignkey_value,
        NULL AS associated_key_name, 
        NULL AS associated_key_value, 
        NULL AS associated_column_name, 
        NULL AS associated_column_value 
    FROM CHECKINGACCOUNT_V1 
    WHERE CHECKINGACCOUNT_V1.DELETEDTIME IS NULL
    
    UNION SELECT 
        'TRANSDATE_BEFORE_ACCOUNT' AS error_code, 
        'E' as severity, 
        'CHECKINGACCOUNT_V1' AS source_table_name, 
        'TRANSID' AS source_key_name, 
        CHECKINGACCOUNT_V1.TRANSID AS source_key_value, 
        'TRANSDATE' AS source_column_name, 
        CHECKINGACCOUNT_V1.TRANSDATE AS source_column_value, 
        'ACCOUNTID' AS source_foreignkey_column, 
        CHECKINGACCOUNT_V1.ACCOUNTID AS source_foreignkey_value, 
        'ACCOUNTID' AS associated_key_name, 
        ACCOUNTLIST_V1.ACCOUNTID AS associated_key_value, 
        'INITIALDATE' AS associated_column_name, 
        ACCOUNTLIST_V1.INITIALDATE AS associated_column_value 
    FROM CHECKINGACCOUNT_V1 
    JOIN ACCOUNTLIST_V1 ON ACCOUNTLIST_V1.ACCOUNTID = CHECKINGACCOUNT_V1.ACCOUNTID 
    WHERE CHECKINGACCOUNT_V1.TRANSDATE < ACCOUNTLIST_V1.INITIALDATE AND (CHECKINGACCOUNT_V1.DELETEDTIME IS NULL OR CHECKINGACCOUNT_V1.DELETEDTIME = "")
