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
import club.minnced.jda.reactor.createManager
import club.minnced.jda.reactor.on
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import connections.DatabaseActions
import db.*
import dev.minn.jda.ktx.injectKTX
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import javax.security.auth.login.LoginException
import kotlin.concurrent.thread

object Main : ListenerAdapter() {

    @Throws(LoginException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val token = "TOKEN"

        var count = 0
        val executor = Executors.newScheduledThreadPool(ForkJoinPool.getCommonPoolParallelism()) {
            thread(start = false, block = it::run, name = "jda-thread-${count++}", isDaemon = true)
        }

        // Wrap executor in scheduler for flux processor
        val schedulerWrap = Schedulers.fromExecutor(executor)

        // Create a reactive event manager with the scheduler
        val manager = createManager {
            scheduler = schedulerWrap
            isDispose =
                false // The scheduler uses a daemon pool so it doesn't need to be shutdown here since the JVM will terminate anyway
        }


        val jda = JDABuilder.create(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_BANS,
            GatewayIntent.GUILD_EMOJIS,
            GatewayIntent.GUILD_INVITES,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.DIRECT_MESSAGES
        )
            .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
            .injectKTX()
            .setEventManager(manager)
            .setToken(token)
            .setActivity(Activity.playing("/help"))
            .addEventListeners(Main, ModLog, SlashCommands())
            .build().awaitReady()
        Locale.setDefault(Locale("pt", "PT"))

        runBlocking {
            jda.guilds.forEach { guild ->
                val guildId = guild.id
                println("Creating tables for : $guildId")
                try {
                    PreparedStatements(configs).createTableIfNotExists(guildId).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    PreparedStatements(configs).insertOrIgnoreConfig(guildId).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    PreparedStatements(anniversary).createTableIfNotExists(guildId).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    PreparedStatements(welcome).createTableIfNotExists(guildId).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    PreparedStatements(boost).createTableIfNotExists(guildId).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    PreparedStatements(commands).createTableIfNotExists(guildId).execute()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            println("Done!")
        }


        // Add commands. These just make them "exists" it does not execute them in any form.
        jda.updateCommands()
            .addCommands(CommandData("help", "Retorna lista com comandos disponíveis numa mensagem temporária"))
            .addCommands(CommandData("ping", "Retorna o tempo de resposta (em ms) entre o  Discord e o bot"))
            .addCommands(CommandData("prefixo", "Devolve o prefixo utilizado no servidor"))
            .addCommands(CommandData("limpa", "Remove mensagens deste canal")
                .addOption(OptionType.INTEGER, "quantidade", "Quantidade de mensagens a apagar (Padrão: 100)", false))
            .addCommands(CommandData("uptime", "Devolve a informação de há quanto tempo o bot está online"))
            .addCommands(CommandData("serverinfo", "Devolve um conjunto de informações sobre o servidor"))
            .addCommands(CommandData("antiraid",
                "Bloqueia a escrita em todos os canais para todos os membros exceto moderadores"))
            .addCommands(CommandData("antilink", "Bloqueia o envio de links para membros sem um cargo de confiança")
                .addOption(OptionType.ROLE,
                    "cargo",
                    "Cargo de confiança para permitir o envio de links. Deixe vazio para desativar o antilink.",
                    false))
            .addCommands(CommandData("antispam",
                "Elimina e avisa os membros quando enviarem x mensagens iguais seguidas. Só se aplica a texto")
                .addOption(OptionType.ROLE, "limite", "Limite de mensagens iguais permitidas antes de considerar como spam."))
            .addCommands(CommandData("anuncio", "Anuncia num canal de anúncios do servidor e publica automaticamente")
                .addOption(OptionType.STRING, "canal", "O canal onde o anuncio será efetuado", true)
                .addOption(OptionType.STRING, "conteudo", "O conteúdo a ser anunciado", true))


            //TODO: Adicionar possibilidade de mutar por vários dias. Será utilizada uma base de dados.
            //TODO: O bot nao adiciona de volta o cargo mute quando o membro abandona o servidor. Implementar isso.
            .addCommands(CommandData("mute", "Impede um usuário de enviar mensagens no servidor")
                .addOption(OptionType.USER, "usuário", "O usuário a ser mutado", true)
                .addOption(OptionType.INTEGER, "tempo", "Duração do mute", false)
                .addOptions(OptionData(OptionType.STRING, "unidade", "Unidade de tempo").addChoices(
                    Command.Choice("segundos", "s"),
                    Command.Choice("minutos", "m"),
                    Command.Choice("horas", "h")
                )
                )
            )

            .addCommands(CommandData("boost",
                "Ativa/desativa o envio de uma mensagem personalizada por impulso no servidor")
                .addOption(OptionType.CHANNEL,
                    "canal",
                    "Canal para onde as mensagens personalizadas deverão ser enviadas. Necessário para ativar.")
                .addOption(OptionType.STRING,
                    "mensagem",
                    "Mensagem personalizada a ser enviada. Necessário para ativar."))

            //TODO: É basicamente uma copia do boost.
            .addCommands(CommandData("welcome",
                "Ativa/desativa o envio de uma mensagem personalizada por entrada de membro no servidor")
                .addOption(OptionType.CHANNEL,
                    "canal",
                    "Canal para onde as mensagens personalizadas deverão ser enviadas. Necessário para ativar.")
                .addOption(OptionType.STRING,
                    "mensagem",
                    "Mensagem personalizada a ser enviada. Necessário para ativar."))


            .addCommands(CommandData("modrole", "Comandos sobre cargo de moderadores")
                .addSubcommands(SubcommandData("verificar",
                    "Retorna informação sobre o atual cargo de moderadores numa mensagem temporária"))
                .addSubcommands(SubcommandData("definir", "Define cargo de moderadores")
                    .addOption(OptionType.ROLE, "cargo", "Cargo de moderadores", true)))


            //TODO: Adicionar uma possibilidade de desmutar ate 3 membros de uma so vez
            .addCommands(CommandData("unmute", "Desfaz os efeitos do comando mute num usuário")
                .addOption(OptionType.USER, "usuário", "O usuário a ser desmutado", true))


            .addCommands(CommandData("ban", "Bane usuários do servidor. Requer permissão para banir usuários.")
                .addOption(OptionType.USER, "usuário", "O membro a ser banido.", true)
                .addOption(OptionType.INTEGER, "dias", "Mensagens dos últimos x dias a excluir.")
                .addOption(OptionType.STRING, "mensagem", "Mensagem a enviar ao usuário ao ser banido"))

            //TODO: O kick está aqui mas nao esta nos SlashCommands
            .addCommands(CommandData("kick", "Expulsa usuários do servidor. Requer permissão para banir usuários.")
                .addOption(OptionType.USER, "usuário", "O membro a ser expulso.", true))
            //TODO: O hardban está aqui mas nao esta nos SlashCommands
            .addCommands(CommandData("hardban", "Bane usuários do servidor. Requer permissão para banir usuários.")
                .addOption(OptionType.INTEGER, "usuário", "O ID do membro a ser banido.", true)
                .addOption(OptionType.INTEGER, "dias", "Mensagens dos últimos x dias a excluir."))
            //TODO: O unban está aqui mas nao esta nos SlashCommands
            .addCommands(CommandData("unban", "Desbane usuários do servidor. Requer permissão para banir usuários.")
                .addOption(OptionType.INTEGER, "usuário", "O ID do usuário a ser desbanido.", true))

            .addCommands(CommandData("comandos", "Para criar comandos personalizados")
                .addSubcommands(SubcommandData("adicionar", "Cria um comando personalizado para o servidor")
                    .addOption(OptionType.STRING, "gatilho", "O texto que irá acionar o comando", true)
                    .addOption(OptionType.STRING, "conteúdo", "O conteúdo personalizado que o bot irá enviar", true))
                .addSubcommands(SubcommandData("remover", "Remove um comando personalizado do servidor")
                    .addOption(OptionType.STRING, "gatilho", "O gatilho do comando a ser removido", true))
            )

            .addCommands(CommandData("userinfo", "Retorna informações de um usuário")

                .addSubcommands(SubcommandData("menção", "Utiliza uma menção para obter as informações")
                    .addOption(OptionType.USER, "usuário", "Usuário a ser inspecionado", true))

                .addSubcommands(SubcommandData("id", "Utiliza o userID para obter as informações")
                    .addOption(OptionType.STRING, "usuário", "O id do usuário a ser inspecionado", true)))


            .addCommands(CommandData("gc", "Comandos relacionados com Grand Chase")
                .addSubcommands(SubcommandData("wiki", "Retorna um cartão com as informações mais relevantes do herói")
                    .addOption(OptionType.STRING, "heroi", "Heroi SR de Grand Chase", true)))
            .queue()

        jda.on<MessageReceivedEvent>()
            .filter { !it.author.isBot }  // don't respond to bots
            .flatMap(::handleReceivedMessage)
            .subscribe()

        jda.on<GuildJoinEvent>()
            .flatMap(::handleGuildJoin)
            .subscribe()

        jda.on<GuildMemberUpdateBoostTimeEvent>()
            .flatMap(::handleGuildBoost)

    }

    private fun handleGuildBoost(event: GuildMemberUpdateBoostTimeEvent) = mono {
        //Se não houver TextChannelId não será enviado nada - por motivos óbvios.
        // O evento pode ser causado por membros que pararam de impulsionar. Se tal acontecer, timeBoosted sera null.
        if (event.member.timeBoosted == null) {
            return@mono Mono.empty<Unit>()
        }
        val guildId = event.guild.id
        if (!DatabaseActions().checkForBoostThanksChannel(guildId)) {
            println("A server was boosted - but it didn't have a channel set up for that.")
            return@mono Mono.empty<Unit>()
        }
        val boostList = DatabaseActions().getBoostMessage(guildId)
        val targetChannel: TextChannel? = event.jda.getTextChannelById(boostList[0])
        if (targetChannel == null) {
            println("I found a null boost thanks channel. I will be disabling it.")
            DatabaseActions().disableBoostThanks(guildId)
            return@mono Mono.empty<Unit>()
        }
        // Para comandos personalizados o regex faz o seguinte:
        // [menção] para ser substituída por uma menção real.
        // [tag] para ser substituída por uma tag ex. DV8FromTheWorld#6297
        val boostThanksMessage = boostList[1].replace("(?i)\\[menção]".toRegex(), event.member.asMention)
            .replace("(?i)\\[tag]".toRegex(), event.user.asTag)
        return@mono event.jda.getTextChannelById(boostList[0])?.sendMessage(boostThanksMessage)?.asMono()
    }


    private fun handleGuildJoin(event: GuildJoinEvent) = mono {
        var botCount = 0
        val guildId = event.guild.id
        event.guild.loadMembers().asMono().doOnSuccess { membersList ->
            membersList.forEach { member ->
                if (member.user.isBot) {
                    botCount++
                }
                if (botCount > (membersList.size * 0.75)) {
                    println("I was invited to a guild where the bot count represents >75% of the total member count.")
                    if (event.guild.id != "717239989863186512") {
                        event.guild.leave().asMono()
                    }
                }
            }
        }
        try {
            PreparedStatements(configs).createTableIfNotExists(guildId).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            PreparedStatements(configs).insertOrIgnoreConfig(guildId).execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleReceivedMessage(event: MessageReceivedEvent) = mono {
        if (event.isFromGuild && event.textChannel.canTalk()) {
            onGuildCommand(event).awaitFirstOrNull()
        }
        if ((!event.isFromGuild) && (event.author.id == "186147402078617600")) {
            onPrivateCommand(event).awaitFirstOrNull()
        }
    }

        private fun onGuildCommand(event: MessageReceivedEvent): Mono<*> {
            return CommandsCustom.playCustomCommand(event) //Ocorre praticamente sempre.
        }

        private fun onPrivateCommand(event: MessageReceivedEvent): Mono<*> {
            val content = event.message.contentDisplay
            val parts = content.split(" ", limit = 2)
            return when (parts[0].lowercase(Locale.getDefault())) {
                "guilds" -> CommandsMisc.getGuilds(event)
                "leaveguild" -> CommandsMisc.leaveGuild(parts.getOrNull(1), event)
                else -> Mono.empty<Unit>()
            }
        }
}

/*
    LoggingDiscord.memberJoin(event)
    LoggingDiscord.MemberLeft(event)
    LoggingDiscord.MemberBanned(event)
    LoggingDiscord.MemberUnbanned(event)
    LoggingDiscord.MessageEdited(event)
    LoggingDiscord.MessageDeleted(event)
    LoggingDiscord.BulkMessageDeletion(event)
    LoggingDiscord.ChannelCreated(event)
    LoggingDiscord.ChannelDeleted(event)
    LoggingDiscord.RoleCreated(event)
    LoggingDiscord.RoleDeleted(event)
    LoggingDiscord.RoleGiven(event)
    LoggingDiscord.RoleRemoved(event)
    LoggingDiscord.NicknameChanged(event)
*/