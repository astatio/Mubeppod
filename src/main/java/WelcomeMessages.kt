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
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class WelcomeMessages {

    fun welcomeMessageStoreChannel(event: GuildMessageReceivedEvent) {
        val guildId = event.guild.id
        val boostThanksChannel = event.message.mentionedChannels[0]
        val textChannelId = boostThanksChannel.id
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to Configs.db has been established. Storing \"boostthanks\" channel.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            statement.execute("UPDATE \"$guildId CONFIGS\" SET inside = \"$textChannelId\", notes = \"Obrigado pelo impulso @menção!\" WHERE id = \"boostthanks\";")
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description( "Ativado e definido como " + boostThanksChannel.asMention + "\n Nota: Foi definida uma mensagem padrão. Utilize !help para obter mais informações")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            println(e.message)
            EmbedUtilsBuilder.Regular()
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

    fun checkForWelcomeMessageChannel(event: GuildMemberUpdateBoostTimeEvent): ArrayList<String> {
        val guildId = event.guild.id
        val boostList = ArrayList<String>()
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to Configs.db has been established. Getting \"boostthanks\" channel.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            val rs = statement.executeQuery("SELECT * FROM \"$guildId CONFIGS\" WHERE id = \"boostthanks\";")
            while (rs.next()) {
                val inside: String? = rs.getString("inside")
                val notes: String? = rs.getString("notes")
                boostList.add(0, inside.toString())
                boostList.add(1, notes.toString())
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
        return boostList
    }

    fun checkForWelcomeMessage(event: GuildMessageReceivedEvent): Boolean {
        val guildId = event.guild.id
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

    fun deleteWelcomeMessage(event: GuildMessageReceivedEvent) {
        val guildId = event.guild.id
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to Configs.db has been established.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            statement.execute("UPDATE \"$guildId CONFIGS\" SET inside = NULL, notes = NULL WHERE id = \"boostthanks\";")
            // TODO: Preciso atualizar isso aqui para o meu novo sistema porque eu quero que fique vazio e não que coloque NULL. Mas também nao tenho a certeza se fica vazio ou nao...
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("O agradecimento automático por impulso está agora desativado.")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            println(e.message)
            EmbedUtilsBuilder.Regular()
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

    fun changeWelcomeMessage(event: GuildMessageReceivedEvent) {
        if (!event.member?.hasPermission(Permission.ADMINISTRATOR)!!) {
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description(Standard.ERROR_NO_ADMIN_PERMISSION)
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        }
        val boostThanksMessage = event.message.contentRaw.replace("(?i)^\\Wboost\\s*".toRegex(), "")
        val guildId = event.guild.id
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to Configs.db has been established.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            statement.execute("UPDATE \"$guildId CONFIGS\" SET notes = \"$boostThanksMessage\" WHERE id = \"boostthanks\";")
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("Foi definido com sucesso a nova mensagem de agradecimento.")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            println(e.message)
            EmbedUtilsBuilder.Regular()
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


}