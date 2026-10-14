package com.fiteasy.desktop

import com.fiteasy.shared.StateSql
import java.sql.Connection

fun jdbcStateSql(connection:Connection)=object:StateSql {
    override fun execute(sql:String,args:List<String?>){connection.prepareStatement(sql).use {statement->args.forEachIndexed {i,value->statement.setString(i+1,value)};statement.execute()}}
    override fun query(sql:String,args:List<String?>):List<List<String?>> =connection.prepareStatement(sql).use {statement->args.forEachIndexed {i,value->statement.setString(i+1,value)};statement.executeQuery().use {result->buildList {while(result.next())add((1..result.metaData.columnCount).map {result.getString(it)})}}}
    override fun transaction(action:()->Unit){check(connection.autoCommit);connection.autoCommit=false;try {action();connection.commit()}catch(t:Throwable){connection.rollback();throw t}finally {connection.autoCommit=true}}
}
