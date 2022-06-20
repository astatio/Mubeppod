/**
 * Copyright 2022 Vitor Sousa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package db


import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.sql.*

class Database(val dbFileName: String, val url: String = "jdbc:sqlite:databases/$dbFileName", val asConnection: Connection = DriverManager.getConnection(url) )


//TODO: Os comandos ainda não estão separados do restante na base de dados.
var wiki: Database = Database("Wiki.db")
var configs: Database = Database("Configs.db")
var anniversary: Database = Database("Anniversary.db")
var welcome: Database = Database("Welcome.db")
var boost: Database = Database("Boost.db")
var commands: Database = Database("Commands.db")
var test: Database = Database("Test.db")






class PreparedStatements(private val database: Database) {

    fun createTableIfNotExists(table: String): PreparedStatement {
        return database.asConnection.prepareStatement("CREATE TABLE IF NOT EXISTS \"$table\" (" +
                "id TEXT UNIQUE PRIMARY KEY," +
                "status TEXT DEFAULT '0' NOT NULL" +
                ");")
    }

    fun insertOrIgnoreConfig(table: String): PreparedStatement{
        return database.asConnection.prepareStatement(
            "INSERT OR IGNORE INTO \"$table\" (id) VALUES ('anniversary'),('welcome'),('boost'),('commands'),('modrole');")
    }

    //Colocar aqui em cima preparedstatements quando as tabelas nao existem.

    fun selectFromWhereId(table: String, id: String): PreparedStatement {
        return database.asConnection.prepareStatement("SELECT * FROM \"$table\" WHERE id LIKE \"$id\";")
    }

    fun updateSetStatusWhereId(table: String, status: String, id: String): PreparedStatement {
        return database.asConnection.prepareStatement("UPDATE \"$table\" SET status = \"$status\" WHERE id = \"$id\";")
    }


    fun dropTableIfExists(): PreparedStatement {
        return database.asConnection.prepareStatement("DROP TABLE IF EXISTS ?;")
    }

    fun upsertIdStatus(table: String, id: String, status: String): PreparedStatement {
        return database.asConnection.prepareStatement("INSERT INTO \"$table\" (id,status) VALUES(\"$id\",\"$status\")" +
                "ON CONFLICT(\"id\") DO UPDATE SET status=excluded.status;")
    }

    fun deleteFromWhereId(table: String, id: String): PreparedStatement {
        return database.asConnection.prepareStatement("DELETE FROM \"$table\" WHERE id LIKE '$id';")
    }


    fun newCustom(sql: String): PreparedStatement {
        return database.asConnection.prepareStatement(sql)
    }



}

class StatementGenerator {

    /**
     *
     *  Executes the given SQL statement, which may return multiple results. In some (uncommon) situations, a single SQL statement may return multiple result sets and/or update counts. Normally you can ignore this unless you are (1) executing a stored procedure that you know may return multiple results or (2) you are dynamically executing an unknown SQL string.
     *   The execute method executes an SQL statement and indicates the form of the first result. You must then use the methods getResultSet or getUpdateCount to retrieve the result, and getMoreResults to move to any subsequent result(s).
     *   Note:This method cannot be called on a PreparedStatement or CallableStatement.
     * @param sql Any SQL statement.
     * @returns true if the first result is a ResultSet object; false if it is an update count or there are no results
     * @throws java.sql.SQLException if a database access error occurs, this method is called on a closed Statement, the method is called on a PreparedStatement or CallableStatement
     * @throws java.sql.SQLTimeoutException when the driver has determined that the timeout value that was specified by the setQueryTimeout method has been exceeded and has at least attempted to cancel the currently running Statement
     */

    fun toExecute(vararg sql: String): (Statement) -> Unit {
        return { statementTypeVal: Statement ->
            sql.iterator().forEach { sql ->
                statementTypeVal.execute(sql)
            }
        }
    }

    fun toQuery(sql: String): (Statement) -> (ResultSet) {
        return { statementTypeVal: Statement ->
            statementTypeVal.executeQuery(sql)
            }
        }

}



fun main() {

    val testingStatement = PreparedStatements(test).selectFromWhereId("1999", "modrole")
    val rs = testingStatement.executeQuery()
    println(rs.getString(2))
}

class Connect(databaseObj: Database) : ListenerAdapter() {
    private val url = databaseObj.url
    private val dbFileName = databaseObj.dbFileName

    fun set(statementEx: (Statement) -> Unit) {
        var conn: Connection? = null
        try {
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to $dbFileName has been established.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here
            statementEx(statement)
        } catch (e: Error) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace()
        } finally {
            try {
                conn?.close()
            } catch (sqlError: SQLException) {
                sqlError.printStackTrace()
            }
        }

    }
    fun query(statementEx: (Statement) -> ResultSet): ArrayList<String> {
        var conn: Connection? = null
        val resultList : ArrayList<String> = ArrayList()
        try {
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to $dbFileName has been established.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Testei e funciona. em SQL começa por 1 em vez do normal 0.

            //Queries go down here
            val rs = statementEx(statement)
            var i = 2
            val columnCount = rs.metaData.columnCount
            while (i<=columnCount) {
                resultList.add(rs.getString(i))
                i++
            }
        } catch (e: Error) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace()
        } finally {
            try {
                conn?.close()
            } catch (sqlError: SQLException) {
                sqlError.printStackTrace()
            }
        }
        return resultList
    }

}