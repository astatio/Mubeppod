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
package connections



import db.*
import embedutils.EmbedUtilsBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class DatabaseActions {


    fun checkForPrefixOld(guild: Guild): String {

        val guildId = guild.id
        var prefix = "!"
        //Tentar pegar o prefixo da tabela. Se ela nao existir, será feito o create table if not exists.
        //ESTA FUNÇÃO SO SERVE PARA VER SE EXISTE E RETORNAR.
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            // println("Connection to Configs.db has been established. Checking if prefix matches.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.


            statement.execute(
                "CREATE TABLE IF NOT EXISTS \"$guildId\" (\"id\" TEXT UNIQUE, \"status\" TEXT);"
                        +
                        "INSERT INTO \"$guildId\" (id, status) SELECT \"prefix\",\"!\" WHERE NOT EXISTS(SELECT * FROM \"$guildId\" WHERE \"id\"=\"prefix\");"
                        +
                        "INSERT INTO \"$guildId\" (id) SELECT \"boostthanks\" WHERE NOT EXISTS(SELECT * FROM \"$guildId\" WHERE \"id\"=\"boostthanks\");"
                        +
                        "INSERT INTO \"$guildId\" (id) SELECT \"modrole\" WHERE NOT EXISTS(SELECT * FROM \"$guildId\" WHERE \"id\"=\"modrole\");"
            ) // Só se nao existir é que todos esses comandos serao executados. Se a tabela ja existir, nada irá acontecer.
            val rs = statement.executeQuery("SELECT * FROM \"$guildId CONFIGS\" WHERE id = \"prefix\";")
            while (rs.next()) {
                prefix = rs.getString("status")
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
        return prefix
    }

    fun deleteGuildTables(guildId: String) {
        //Elimina toda a informação sobre a guilda que o bot possui.
        //TODO: Esta pronto!!
        val anniversary = PreparedStatements(anniversary).dropTableIfExists()
        val configs = PreparedStatements(configs).dropTableIfExists()
        val welcome = PreparedStatements(welcome).dropTableIfExists()
        val boost = PreparedStatements(boost).dropTableIfExists()

        anniversary.setString(1, guildId)
        configs.setString(1, guildId)
        welcome.setString(1, guildId)
        boost.setString(1, guildId)

        anniversary.execute()
        configs.execute()
        welcome.execute()
        boost.execute()
    }

    fun disableBoostThanks(guildId: String) {
        //Também elimina a tabela da guilda de Boost.db
        //TODO: Esta pronto!!
        val dropTable = PreparedStatements(boost).dropTableIfExists()
        val noBoostThanks = PreparedStatements(configs).updateSetStatusWhereId(guildId, "0", "boostthanks")

        dropTable.setString(1, guildId)

        dropTable.execute()
        noBoostThanks.execute()
    }

    fun disableWelcome(guildId: String) {
        //Também elimina a tabela da guilda de Welcome.db
        //TODO: Esta pronto!!
        val dropTable = PreparedStatements(welcome).dropTableIfExists()
        val noWelcome = PreparedStatements(configs).updateSetStatusWhereId(guildId, "0", "welcome")

        dropTable.setString(1, guildId)

        dropTable.execute()
        noWelcome.execute()
    }


    fun enableAndStoreBoostThanks(guildId: String, targetChannel: GuildChannel, message: String) {
        //Também cria uma tabela da guilda no Boost.db
        //Se tiver "@menção" (sem aspas, mas literalmente @menção) será convertido em uma menção quando o impulso ocorrer


        val createTable =
            StatementGenerator().toExecute("CREATE TABLE IF NOT EXISTS \"$guildId\"(\"id\" TEXT UNIQUE, \"channel\" TEXT, \"message\" TEXT);")
        val setChannel =
            StatementGenerator().toExecute("UPDATE \"$guildId\" SET channel = \"${targetChannel.id}\", message = \"$message\" WHERE id = \"boostthanks\";")
        val enableBoostThanks =
            StatementGenerator().toExecute("UPDATE \"$guildId\" SET status = 1 WHERE id = \"boostthanks\";")
        Connect(boost).set(createTable)
        Connect(boost).set(setChannel)
        Connect(configs).set(enableBoostThanks)
    }

    fun setModRole(guildId: String, modRoleID: String) {
        //O Modrole funciona e necessita apenas de Configs.db devido a sua simplicidade.
        //TODO: Esta pronto!!
        val createIfNotExists =
            PreparedStatements(configs).newCustom("CREATE TABLE IF NOT EXISTS \"$guildId\"(\"id\" TEXT UNIQUE, \"status\" TEXT);")
        val updateModRole = PreparedStatements(configs).updateSetStatusWhereId(guildId, modRoleID, "modrole")

        createIfNotExists.execute()
        updateModRole.execute()
    }

    fun getModRoleId(guildId: String): String? {
        //O Modrole funciona e necessita apenas de Configs.db devido a sua simplicidade.
        //TODO: Esta pronto!!
        val checkForModRole = PreparedStatements(configs).selectFromWhereId(guildId, "modrole")
        return checkForModRole.executeQuery().getString(2)
    }


    fun hasModRole(event: SlashCommandEvent, guildId: String, hook: InteractionHook): Boolean {
        val modRoleError = hook.sendMessageEmbeds(
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description("Não está definido o cargo que permite utilizar comandos Especiais neste servidor.")
                .result(EmbedUtilsBuilder.ResultType.ERROR))

        var dbModRoleId = "0"
            try {
                val checkingModRole = PreparedStatements(configs).selectFromWhereId(guildId, "modrole")
                dbModRoleId = checkingModRole.executeQuery().getString(2)
                println("dbModRoleId = $dbModRoleId")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        if (dbModRoleId == "0") {
            modRoleError.queue()
            return false
        }
        val actualModRole: Role? = event.guild?.getRoleById(dbModRoleId)
        // Antes de continuar, verificar se o cargo deixou de existir
        if (actualModRole == null) {
            modRoleError.queue()
            //Se deixou de existir, pode ser apagado.
            val deleteModRoleId = PreparedStatements(configs).updateSetStatusWhereId(guildId, "0", "modrole")
            deleteModRoleId.execute()
            return false
        }
        if (event.member?.roles?.contains(actualModRole) != true) {
            hook.sendMessageEmbeds(
                EmbedUtilsBuilder.Regular()
                    .channel(event.textChannel)
                    .description("Não tem o cargo ${actualModRole.asMention} que lhe permite utilizar este comando.")
                    .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
            return false
        }
        return true
    }

    fun checkForBoostThanksChannel(guildId: String): Boolean {
        //TODO: Necessário testar. Mas a principio funciona
        val isBoostThanks1or0 = PreparedStatements(configs).selectFromWhereId(guildId, "boostthanks")
        return isBoostThanks1or0.executeQuery().getString(2).equals("1")
    }

    fun getBoostMessage(guildId: String): ArrayList<String> {
        //TODO: Necessário testar. Mas a principio funciona
        val boostList = ArrayList<String>()

        val boostThanksLine = PreparedStatements(boost).selectFromWhereId(guildId, "boostthanks")
        val rs = boostThanksLine.executeQuery()

        boostList.add(0, rs.getString("channel"))
        boostList.add(1, rs.getString("message"))
        return boostList
    }
}