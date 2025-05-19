select group_concat( sql_check, ' UNION ')
from (
	select 
		   'SELECT ' 
				|| '  ''' || error_code || ''' as error_code'
				|| ', ''' || severity || ''' as severity'
		        || ', ''' || source_table_name || ''' as source_table_name '  
				|| ', ''' || source_key_name  || ''' as source_key_name' 
				|| ', ' || source_table_name || '.' || source_key_name  || ' as source_key_value' 
				|| ', ''' || source_column_name || ''' as source_column_name' 
				|| ', ' || source_table_name || '.' || source_column_name || ' as source_column_value'
				|| ', null as foreign_column_name' 
				|| ', null as foreign_column_value'
				|| ', null as associated_key_name'				
				|| ', null as associated_key_value'				
				|| ', null as associated_column_name'				
				|| ', null as associated_column_value'	
				|| ' FROM ' || source_table_name 
				|| ' WHERE ' || source_where 
				as sql_check
		   , MASTER.* 
	from integrity_checker as MASTER
	where associated_table_name = "" 
	  and inactive = ''

	UNION

	select 
		   'SELECT ' 
				|| '  ''' || error_code || ''' as error_code'
				|| ', ''' || severity || ''' as severity'
		        || ', ''' || source_table_name || ''' as source_table_name '  
				|| ', ''' || source_key_name  || ''' as source_key_name' 
				|| ', ' || source_table_name || '.' || source_key_name  || ' as source_key_value' 
				|| ', ' || iif( source_column_name <> '', '''' || source_column_name || '''' , 'null' ) || ' as source_column_name' 
				|| ', ' || iif( source_column_name <> '', source_column_name , 'null' ) || ' as source_column_value' 
				|| ', ' || iif( source_foreignkey_column <> '', '''' || source_foreignkey_column || '''' , 'null' ) || ' as source_foreignkey_column'
				|| ', ' || iif( source_foreignkey_column <> '', source_table_name || '.' ||source_foreignkey_column , 'null') || ' as source_foreignkey_value'				
				|| ', ' || iif( associated_key_name <> '', '''' || source_foreignkey_column  || '''', 'null') || ' as associated_key_name'				
				|| ', ' || iif( associated_key_name <> '', associated_table_name || '.' ||  associated_key_name , 'null') || ' as associated_key_value'				
				|| ', ' || iif( associated_column_name <> '', '''' || associated_column_name  || '''', 'null') || ' as associated_column_name'				
				|| ', ' || iif( associated_column_name <> '', associated_table_name || '.' ||  associated_column_name , 'null') || ' as associated_column_value'	
				|| ' FROM ' || source_table_name 
				|| ' LEFT JOIN ' || associated_table_name || ' ON ' || associated_table_name || '.' || associated_key_name || " = " || source_table_name || '.' || source_foreignkey_column
				|| ' WHERE ' || associated_table_name || '.' || associated_key_name || ' is null '
							 || ' AND ' || source_where 
				as sql_check
		   , MASTER.* 
	from integrity_checker as MASTER
	where not ( associated_table_name = "" ) 
		  and associated_column_name = ""
	  and inactive = ''

	UNION

	select 
		   'SELECT ' 
				|| '  ''' || error_code || ''' as error_code'
				|| ', ''' || severity || ''' as severity'
		        || ', ''' || source_table_name || ''' as source_table_name '  
				|| ', ''' || source_key_name  || ''' as source_key_name' 
				|| ', ' || source_table_name || '.' || source_key_name  || ' as source_key_value' 
				|| ', ' || iif( source_column_name <> '', '''' || source_column_name || '''' , 'null' ) || ' as source_column_name' 
				|| ', ' || iif( source_column_name <> '', source_column_name , 'null' ) || ' as source_column_value' 
				|| ', ' || iif( source_foreignkey_column <> '', '''' || source_foreignkey_column || '''' , 'null' ) || ' as source_foreignkey_column'
				|| ', ' || iif( source_foreignkey_column <> '', source_table_name || '.' ||source_foreignkey_column , 'null') || ' as source_foreignkey_value'				
				|| ', ' || iif( associated_key_name <> '', '''' || source_foreignkey_column  || '''', 'null') || ' as associated_key_name'				
				|| ', ' || iif( associated_key_name <> '', associated_table_name || '.' ||  associated_key_name , 'null') || ' as associated_key_value'				
				|| ', ' || iif( associated_column_name <> '', '''' || associated_column_name  || '''', 'null') || ' as associated_column_name'				
				|| ', ' || iif( associated_column_name <> '', associated_table_name || '.' ||  associated_column_name , 'null') || ' as associated_column_value'				
				|| ' FROM ' || source_table_name 
				|| ' JOIN ' || associated_table_name || ' ON ' || associated_table_name || '.' || associated_key_name || " = " || source_table_name || '.' || iif( source_foreignkey_column <> '', source_foreignkey_column, source_column_name)
				|| ' WHERE ' || source_where 
				as sql_check
		   , MASTER.* 
	from integrity_checker as MASTER
	where not ( associated_column_name = "" )
		  and inactive = ''
) order by id 
