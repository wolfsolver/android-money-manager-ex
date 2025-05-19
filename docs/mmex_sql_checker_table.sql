DROP TABLE IF EXISTS "integrity_checker";
CREATE TABLE integrity_checker (
	id INTEGER PRIMARY KEY,
    error_code TEXT CONSTRAINT ERROR_CODE_DUPL UNIQUE,  -- Unique code for the error type (e.g., 'DATE_BEFORE_INIT')
	inactive TEXT,                                      -- inactive flag
    severity TEXT,     								    -- E 'ERROR' W 'WARNING' I 'INFO'
	-- Siurce table
    source_table_name TEXT,         					-- Table where the error was found
    source_key_name TEXT,              					-- Identifier for the specific row 
    source_column_name TEXT,         					-- Column where the error manifested
	source_foreignkey_column TEXT, 	                    -- FOREIGN key for associated table
	source_where TEXT,   					            -- Additional where condition
    -- associated table
    associated_table_name TEXT,							-- Table involved in the association (if applicable)
    associated_key_name TEXT,   						-- Identifier for the associated row (if applicable)
    associated_column_name TEXT,						-- Column in the associated table (if applicable)
    description TEXT,        						 	-- Human-readable description of the error
    action  TEXT,               			  		    -- NOT YET USED: UPDATE OR DELETE
	sql_action TEXT										-- NOT YET USED: sql action for fix issue id action_name eq UPDATE                                    
);

INSERT INTO "integrity_checker" VALUES (1,   -- ok
	'DELETEDTIME_NULL',  												-- Unique code for the error type (e.g., 'DATE_BEFORE_INIT')
	'',                                                                 -- inative flag
	'I',									                            -- E 'ERROR' W 'WARNING' I 'INFO'
	'CHECKINGACCOUNT_V1',					                            -- Table where the error was found
	'TRANSID',								                            -- Identifier for the specific row 
	'DELETEDTIME',							                            -- Column where the error manifested
	'',                                                                  -- FOREIGN key
	'DELETEDTIME is null',					                            -- Additional where condition
	'',										                            -- Table involved in the association (if applicable)
	'',							                                        -- Identifier for the associated row (cast to TEXT)
	'',							                                        -- Column in the associated table (if applicable)
	'DELETEDTIME is null instead of blank',                             -- Human-readable description of the error
	'UPDATE',								                            -- Suggested action to fix (e.g., 'ALIGN_DATE', 'LOGICAL_DELETE')
	'DELETEDTIME = ""');					                            -- sql action for fix issue                                      
INSERT INTO "integrity_checker" VALUES (2,
	'TRANSDATE_BEFORE_ACCOUNT',											-- error_code
	'',                                                                 -- inative flag
	'E',                                                                -- severity
	'CHECKINGACCOUNT_V1',                                               -- Source table
	'TRANSID',                                                          -- source key
	'TRANSDATE',                                                        -- source column
	'ACCOUNTID',                                                         -- FOREIGN key	
	'CHECKINGACCOUNT_V1.TRANSDATE < ACCOUNTLIST_V1.INITIALDATE AND ( DELETEDTIME is null OR DELETEDTIME = "" )',                          -- source field
	'ACCOUNTLIST_V1',                                                   -- associated_table
	'ACCOUNTID',                                                        -- associated_key
	'INITIALDATE',                                                      -- associated_field
	'transaction date before account initial date',                     -- errro description
	'UPDATE',                                                           -- action comment
	'CHECKINGACCOUNT_V1.TRANSDATE = ACCOUNTLIST_V1.INITIALDATE');       -- sql fixer

INSERT INTO "integrity_checker" VALUES (3,
	'CHECKINGACCOUNT_V1 MISSING ACCOUNT',											-- error_code
	'',                                                                 -- inative flag
	'E',                                                                -- severity
	'CHECKINGACCOUNT_V1',                                               -- Source table
	'TRANSID',                                                          -- source key
	'',                                                        			-- source column
	'ACCOUNTID' ,                                                       -- FOREIGN key	
	'CHECKINGACCOUNT_V1.ACCOUNTID > 0 AND ( DELETEDTIME is null OR DELETEDTIME = "" )',                          -- source field
	'ACCOUNTLIST_V1',                                                   -- associated_table
	'ACCOUNTID',                                                        -- associated_key
	'',                                                                 -- associated_field
	'Account id not existnt for CHECKINGACCOUNT_V1',                    -- errro description
	'DELETE',                                                           -- action comment
	'');     -- sql fixer

INSERT INTO "integrity_checker" VALUES (4,
	'CHECKINGACCOUNT_V1 MISSING TOACCOUNT',											-- error_code
	'',                                                                 -- inative flag
	'E',                                                                -- severity
	'CHECKINGACCOUNT_V1',                                               -- Source table
	'TRANSID',                                                          -- source key
	'',                                               			        -- source column
	'TOACCOUNTID',                                                      -- foreingkey column
	'CHECKINGACCOUNT_V1.TOACCOUNTID > 0 AND ( DELETEDTIME is null OR DELETEDTIME = "" )',                          -- source field
	'ACCOUNTLIST_V1',                                                   -- associated_table
	'ACCOUNTID',                                                        -- associated_key
	'',                                                                 -- associated_field
	'Account id not existnt for CHECKINGACCOUNT_V1',                    -- errro description
	'',                                                           -- action comment
	'');     -- sql fixer
