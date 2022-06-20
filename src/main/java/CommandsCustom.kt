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
import club.minnced.jda.reactor.asMono
import connections.DatabaseActions
import db.PreparedStatements
import db.commands
import embedutils.EmbedUtilsBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import reactor.core.publisher.Mono


object CommandsCustom : ListenerAdapter() {

    fun addCustomCommand(event: SlashCommandEvent, triggerMessage: String, message: String) {
        event.deferReply(false).queue()
        val hook = event.hook
        val guildId = event.guild!!.id

        if(!DatabaseActions().hasModRole(event, guildId, hook))
            return
        PreparedStatements(commands).createTableIfNotExists(guildId)
        try {
            PreparedStatements(commands).upsertIdStatus(event.guild!!.id, triggerMessage, message).execute()
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description("Comando personalizado criado com sucesso!")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
        } catch (e: Error){
            e.printStackTrace()
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description("Ocorreu um erro ao guardar o comando.\nTente novamente.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
        }
    }

    fun removeCustomCommand(event: SlashCommandEvent, triggerMessage: String) {
        event.deferReply(false).queue()
        val hook = event.hook
        val guildId = event.guild!!.id
        if(!DatabaseActions().hasModRole(event, guildId, hook))
            return
        try {
            PreparedStatements(commands).deleteFromWhereId(event.guild!!.id, triggerMessage).execute()
            hook.sendMessageEmbeds(
                EmbedUtilsBuilder
                    .Regular()
                    .channel(event.textChannel)
                    .description("Comando personalizado eliminado com sucesso!")
                    .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
        } catch (e: Error) {
            e.printStackTrace()
            hook.sendMessageEmbeds(
                EmbedUtilsBuilder
                    .Regular()
                    .channel(event.textChannel)
                    .description("Ocorreu um erro ao eliminar o comando.\nTente novamente.")
                    .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
        }
    }

    fun playCustomCommand(event: MessageReceivedEvent): Mono<*> {
        val guildId = event.guild.id
        val inputMessage = event.message.contentRaw
        var customCommand : String? = null

        return try {
            val lookupTrigger = PreparedStatements(commands).newCustom("SELECT * FROM \"$guildId\" WHERE id LIKE %;")
                lookupTrigger.setString(1, inputMessage)
            val lookupTrigger2 = lookupTrigger.executeQuery()
            while (lookupTrigger2.next()) {
                customCommand = lookupTrigger2.getString("status")
            }
            if( customCommand == null){
                return Mono.empty<Unit>()
            }
            event.channel.sendMessage(customCommand).asMono()
        } catch (e: Error) {
            Mono.empty<Unit>()
        }
    }


/*        if (!DatabaseActions().checkModRoleOld(event)) {
            return@coroutineScope
        }
        val guildId = event.guild!!.id
        val inputMessage = event.message.contentRaw
        var processedMessage : List<String>
        var cTrigger : String?
        var customCommand : String?
        // SPLIT[0] é !add [ctrigger]
        // SPLIT[1] é o customCommand
        try {
            processedMessage = inputMessage.split("\"") //parece estar certo
            // Se o input for !aadd oi "caguei"
            cTrigger = trim(processedMessage[0].replace("(?i)^\\Wadd\\s*".toRegex(), "")) // aqui o ctrigger vira "oi " (com espaço)
            customCommand = processedMessage[1]
        } catch (e: Exception) {
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("Verifique se digitou corretamente. Coloque a resposta ao comando entre aspas e o *gatilho* antes das mesmas.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return@coroutineScope
        }
        var conn: Connection? = null
        try {
            val url = "jdbc:sqlite:databases/Configs.db"
            // create a connection to the database
            conn = DriverManager.getConnection(url)
            println("Connection to Configs.db has been established.")
            val statement = conn.createStatement()
            statement.queryTimeout = 30

            //Queries go down here.
            statement.execute("CREATE TABLE IF NOT EXISTS\"$guildId COMMANDS\" (\"id\" INTEGER, \"ctrigger\" TEXT UNIQUE, \"modsonlychannel\" TEXT, \"generalannounc\" TEXT, \"customcommands\" TEXT, PRIMARY KEY(\"id\" AUTOINCREMENT));")
            //Linha acima serve para criar uma tabela para a guilda caso ela nao exista
            statement.execute("INSERT INTO \"$guildId COMMANDS\"(ctrigger, customcommands) VALUES (\"$cTrigger\", \"$customCommand\");")
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("Comando adicionado.")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)
            //O insert into vai criar uma nova linha sempre que este comando for executado.
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            println(e.message)
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("Ocorreu um erro. Tente fazer o seguinte:\n- Verifique se já existe um comando com o mesmo *gatilho*. Se sim e pretende substituir, remova-o primeiro e tente novamente.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)
        } finally {
            try {
                conn?.close()
            } catch (ex: SQLException) {
                println(ex.message)
            }
        }*/
    }