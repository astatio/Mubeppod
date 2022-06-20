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
import embedutils.EmbedUtilsBuilder
import embedutils.Standard
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


object Connect : ListenerAdapter() {
    fun changePrefix(event: GuildMessageReceivedEvent, prefix: String) {
        val guildId = event.guild.id
        //Tentar pegar o prefixo da tabela. Se ela nao existir, será feito o create table if not exists.
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            statement.execute("UPDATE \"$guildId CONFIGS\" SET inside = \"$prefix\" WHERE id = \"prefix\";")
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            println(e.message)
            EmbedUtilsBuilder
                .Regular()
                .channel(event.channel)
                .description(Standard.ERROR_GENERIC)
                .result(EmbedUtilsBuilder.ResultType.ERROR)
        } finally {
            try {
                conn?.close()
            } catch (ex: SQLException) {
                println(ex.message)
            }
        }
    }

    fun checkForBoostThanks(event: SlashCommandEvent): Boolean {
        val guildId = event.guild?.id
        var conn: Connection? = null
        var checkResult = false
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to Configs.db has been established.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            val rs = statement.executeQuery("SELECT * FROM \"$guildId CONFIGS\" WHERE id = \"boostthanks\";")
            while (rs.next()) {
                if (rs.getString("inside") != null) {
                    checkResult = true
                }
            }
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            println(e.message)
        } finally {
            try {
                conn?.close()
            } catch (ex: SQLException) {
                println(ex.message)
            }
        }
        return checkResult
    }





}