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
//
//object CommandsAnniv : ListenerAdapter() {
//
//
//    // Estes comandos sao copiados de CommandsCustom e ainda nao foram modificados - apenas os nomes das functions/methods.
//
//    fun addAnniversaryCommand(event: GuildMessageReceivedEvent) {
//        if (!Objects.requireNonNull(event.member)!!.hasPermission(Permission.ADMINISTRATOR)) {
//            EmbedUtils.EmbedUtilsBuilder
//                .Regular()
//                .channel(event.channel)
//                .description("Apenas membros com permissões de administrador podem fazer isso!")
//                .result(EmbedUtils.EmbedUtilsBuilder.ResultType.ERROR)
//            return
//        }
//        val guildId = event.guild.id
//        val inputMessage = event.message.contentRaw
//        var processedMessage: List<String>
//        var cTrigger: String?
//        var customCommand: String?
//        // SPLIT[0] é !aadd [ctrigger]
//        // SPLIT[1] é o customCommand
//        try {
//            processedMessage = inputMessage.split("\"") //parece estar certo
//            // Se o input for !aadd oi "caguei"
//            cTrigger = trim(processedMessage[0].replace("(?i)^\\Wadd\\s*".toRegex(), "")) // aqui o ctrigger vira "oi " (com espaço)
//            customCommand = processedMessage[1]
//        } catch (e: Exception) {
//            embedResults(event.channel, "Verifique se digitou corretamente. Coloque a resposta ao comando entre aspas e o *gatilho* antes das mesmas.", Main.ResultType.ERROR)
//            return
//        }
//        var conn: Connection? = null
//        try {
//            val url = "jdbc:sqlite:databases/Configs.db"
//            // create a connection to the database
//            conn = DriverManager.getConnection(url)
//            println("Connection to Configs.db has been established.")
//            val statement = conn.createStatement()
//            statement.queryTimeout = 30
//
//            //Queries go down here.
//            statement.execute("CREATE TABLE IF NOT EXISTS\"$guildId\" (\"id\" INTEGER, \"ctrigger\" TEXT UNIQUE, \"modsonlychannel\" TEXT, \"generalannounc\" TEXT, \"customcommands\" TEXT, PRIMARY KEY(\"id\" AUTOINCREMENT));")
//            //Linha acima serve para criar uma tabela para a guilda caso ela nao exista
//            statement.execute("INSERT INTO \"$guildId\"(ctrigger, customcommands) VALUES (\"$cTrigger\", \"$customCommand\");")
//            embedResults(event.channel, "Comando adicionado.", Main.ResultType.SUCCESS)
//            //O insert into vai criar uma nova linha sempre que este comando for executado.
//        } catch (e: SQLException) {
//            // if the error message is "out of memory",
//            // it probably means no database file is found
//            println(e.message)
//            embedResults(event.channel, "Ocorreu um erro. Tente fazer o seguinte:\n- Verifique se já existe um comando com o mesmo *gatilho*. Se sim e pretende substituir, remova-o primeiro e tente novamente.", Main.ResultType.ERROR)
//        } finally {
//            try {
//                conn?.close()
//            } catch (ex: SQLException) {
//                println(ex.message)
//            }
//        }
//    }
//
//    fun removeAnniversaryCommand(event: GuildMessageReceivedEvent) {
//        val guildId = event.guild.id
//        val inputMessage = event.message.contentRaw
//        var cTrigger: String? = null
//        // SPLIT[0] é !aremove [ctrigger]
//        // SPLIT[1] é o customCommand
//        try {
//            cTrigger = trim(inputMessage.replace("(?i)!aremove\\s*".toRegex(), ""))
//        } catch (e: Exception) {
//            embedResults(event.channel, "Verifique se digitou corretamente. Coloque apenas o *gatilho* a ser removido.", Main.ResultType.ERROR)
//        }
//        var conn: Connection? = null
//        try {
//            val url = "jdbc:sqlite:databases/Configs.db"
//            // create a connection to the database
//            conn = DriverManager.getConnection(url)
//            println("Connection to Configs.db has been established.")
//            val statement = conn.createStatement()
//            statement.queryTimeout = 30
//
//            //Queries go down here.
//            statement.execute("DELETE FROM \"$guildId\" WHERE ctrigger LIKE '$cTrigger';")
//            embedResults(event.channel, "O comando foi executado, mas é impossível verificar se existia anteriormente ou não.", Main.ResultType.DUBIOUS)
//        } catch (e: SQLException) {
//            // if the error message is "out of memory",
//            // it probably means no database file is found
//            println(e.message)
//            embedResults(event.channel, "Tente fazer o seguinte:\n- Verifique se digitou corretamente o comando.\n- Verifique se o comando existe antes de tentar novamente.", Main.ResultType.ERROR)
//        } finally {
//            try {
//                conn?.close()
//            } catch (ex: SQLException) {
//                println(ex.message)
//            }
//
//        }
//    }
//
//    fun checkForAnniversaryCommand(event: GuildMessageReceivedEvent) {
//        val guildId = event.guild.id
//        var conn: Connection? = null
//        var checkResult = false
//        try {
//            val url = "jdbc:sqlite:databases/Configs.db"
//            // create a connection to the database
//            conn = DriverManager.getConnection(url)
//            println("Connection to Configs.db has been established.")
//            val statement = conn.createStatement()
//            statement.queryTimeout = 30
//
//            //Queries go down here.
//            val rs = statement.executeQuery("SELECT * FROM \"$guildId ANNIVERSARY\" WHERE id LIKE anniCheck;")
//            while (rs.next()) {
//                if (rs.getString("inside") != null) {
//                    checkResult = true  // se nao for null, então é porque ta ativado
//                }
//            }
//        } catch (e: SQLException) {
//            // if the error message is "out of memory",
//            // it probably means no database file is found
//            println(e.message)
//        } finally {
//            try {
//                conn?.close()
//            } catch (ex: SQLException) {
//                println(ex.message)
//            }
//        }
//        return //checkResult
//    }
//
//}
