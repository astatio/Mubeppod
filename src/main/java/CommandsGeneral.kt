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
import connections.DatabaseActions
import embedutils.EmbedUtilsBuilder
import embedutils.Standard
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.TimeFormat
import org.apache.commons.lang3.StringUtils
import org.joda.time.DateTime
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import java.awt.Color
import java.lang.management.ManagementFactory
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*


object CommandsGeneral : ListenerAdapter() {
    fun mute(event: SlashCommandEvent, member: Member?, time: Long, timeUnit: String?) {
        event.deferReply(false).queue()
        val hook = event.hook
        if (!event.member?.hasPermission(Permission.MANAGE_ROLES)!!) {
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description(Standard.ERROR_MANAGE_RULES.description)
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        }
        if (member == null) {
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description(Standard.ERROR_NO_MENTIONED_MEMBERS.description)
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        }
        val muteMember: Member = member
        var timeMs = if (timeUnit == null) {
            0
        } else {
            time
        }
        var newTimeUnit = ""
        lateinit var muteRole: Role
        //Agora vai pegar o cargo com nome "mute"
        val rolesWithMuteList = event.guild?.getRolesByName("mute", true)
        if (rolesWithMuteList.isNullOrEmpty()) {
            //Se nao existir nenhum cargo chamado mute, o bot criará um aqui.
            event.channel.sendMessage("O cargo de *mute* não existia anteriormente. Foi agora criado.").queue()
            //Com o uso de .complete() o bot irá criar o cargo de forma síncrona e evitar erros.
            muteRole = event.guild?.createRole()?.setName("mute")?.setColor(null as Color?)?.complete()!!
            val allGuildChannels = event.guild?.channels?.toList()
            if (allGuildChannels != null) {
                for (item in allGuildChannels) {
                    item.createPermissionOverride(muteRole)
                        .deny(Permission.MESSAGE_WRITE)
                        .deny(Permission.VOICE_SPEAK).queue()
                }
            }
        }
        else{
            muteRole = event.guild?.getRolesByName("mute", true)!![0]
        }
        // Aqui é que vai adicionar role de "mute" ao membro. Se houver erros, os catch retornarão em mensagens.
        val guildTarget: Guild = event.guild!!
        try {
            event.guild!!.addRoleToMember(muteMember, muteRole).queue()
        } catch (e: Error) {
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description(Standard.ERROR_GENERIC)
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return // Se não deu, não vale a pena prosseguir.
        }
        if (timeUnit == null) {
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description("Membro mutado.")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
        } else {
            when (timeUnit) {
                "s" -> {
                    newTimeUnit = " segundo(s)"
                    timeMs *= 1000
                }
                "m" -> {
                    newTimeUnit = " minuto(s)"
                    timeMs *= 60 * 1000
                }
                "h" -> {
                    newTimeUnit = " hora(s)"
                    timeMs *= 60 * 60 * 1000
                }
                "d" -> {
                    hook.sendMessageEmbeds(
                        EmbedUtilsBuilder
                            .Regular()
                            .channel(event.textChannel)
                            .description("Este comando tem um limite máximo de 24 horas (por enquanto)\n\nExemplo de mute de 24 horas:  **!mute @menção 24h**.\n*Poderá substituir 24h por 1440m ou 86400s*.")
                            .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
                }
            }
            // Cria um temporizador para desmutar assim que o tempo passar.
            val timer = Timer()
            Timer("future unmute $muteMember")
            timer.schedule(object : TimerTask() {
                override fun run() {
                    //Guarda o ID/nome do membro mutado, e remove a role "mute" dele. Caso não tenha, nada de especial irá acontecer.
                    // A linha de código abaixo está correta
                    guildTarget.removeRoleFromMember(muteMember, muteRole).queue()
                    muteMember.user.openPrivateChannel().complete()
                        .sendMessage("O seu mute em ${guildTarget.name} terminou.").queue()
                    // Cancelar após ter desmutado para evitar 'lixo' na memória.
                    timer.cancel()
                }
            }, timeMs)
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description(muteMember.effectiveName + " encontra-se agora mutado/a por " + time + newTimeUnit + ".")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
        }
    }

    fun unmute(event: SlashCommandEvent, memberToMute: Member?) {
        event.deferReply(false).queue()
        val hook = event.hook
        if (memberToMute == null) {
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description(Standard.ERROR_NO_MENTIONED_MEMBERS.description)
                .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
            return
        }
        if (!event.member?.hasPermission(Permission.MANAGE_ROLES)!!) {
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description(Standard.ERROR_MANAGE_RULES.description)
                .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
            return
        }
        val muteRole = event.guild!!.getRolesByName("mute", true)
        event.guild?.removeRoleFromMember(memberToMute, muteRole[0])?.complete()
        hook.sendMessageEmbeds(
        EmbedUtilsBuilder.Regular()
            .channel(event.textChannel)
            .description(memberToMute.effectiveName + " foi desmutado/a com sucesso.")
            .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
    }

    fun ban(event: SlashCommandEvent, user: User, member: Member?, banMessage: String?) {
        event.deferReply(false).queue()
        val hook = event.hook
        event.isAcknowledged
        hook.setEphemeral(false)

        if (!event.member?.hasPermission(Permission.BAN_MEMBERS)!!) {
            hook.sendMessageEmbeds(
                EmbedUtilsBuilder.Regular()
                    .channel(event.textChannel)
                    .description("O usuário ${user.asTag} foi banido.")
                    .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
        }
        if (member != null && !event.guild!!.selfMember.canInteract(member)) {
            hook.sendMessage("O membro é superior a mim").queue()
            return
        }
        var delDays = 0
        val option = event.getOption("dias")
        if (option != null) // null = not provided
            delDays = 0.coerceAtLeast(7.coerceAtMost(option.asLong.toInt()))
        // Ban the user and send a success response
        val processedBanMessage =
            if (banMessage.isNullOrEmpty()) {
                ""
            } else {
                "| $banMessage"
            }
        try {
            user.openPrivateChannel().flatMap {
                it.sendMessage("Você foi banido de ${event.guild?.name} $processedBanMessage ")
            }.complete()
        } catch (e: Exception) {
            println(e)
        }
        event.guild!!.ban(user, delDays)
            .flatMap {
                hook.sendMessageEmbeds(
                    EmbedUtilsBuilder.Regular()
                        .channel(event.textChannel)
                        .description("O usuário ${user.asTag} foi banido.")
                        .result(EmbedUtilsBuilder.ResultType.SUCCESS))
            }
            .queue()


/*        try {
            memberTarget.ban(0, reason).queue()
        } catch (hierarchyException: HierarchyException) {
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description("O membro é hierarquicamente superior a mim.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        } catch (insufficientPermissionException: InsufficientPermissionException) {
            EmbedUtilsBuilder.Regular()
                .channel(event.textChannel)
                .description(Standard.ERROR_INSUFFICIENT_PERMISSION)
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        }
        EmbedUtilsBuilder.Regular()
            .channel(event.textChannel)
            .description("O usuário " + event.message.mentionedMembers[0].effectiveName + " foi banido.")
            .result(EmbedUtilsBuilder.ResultType.SUCCESS)
            */
    }


    fun unban(event: GuildMessageReceivedEvent) {
        if (!event.member?.hasPermission(Permission.BAN_MEMBERS)!!) {
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("Você não tem permissões para banir membros - e por isso não pode desbanir.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        }
        val inputMessage = event.message.contentRaw
        val split = StringUtils.split(inputMessage)
        val userId = event.jda.retrieveUserById(split[1]).complete()
        val guildName = event.guild.name
        val guildChannel = event.channel
        val executorUser = event.author
        // Não da para usar catch & try porque resulta em um erro que não é 'catchable'.
        event.guild.unban(userId).queue(
            {
                EmbedUtilsBuilder.TimeOut().timeout(event.channel,
                    "O usuário ${userId.asTag} foi desbanido. Gostaria de enviar um convite?")
                    .queue { message: Message ->
                        val botMessageId = message.id
                        message.addReaction("\u2705").queue()
                    }
            }
        ) {
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("O userID que introduziu não é válido ou o usuário não se encontra banido.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)
        }
    }

    //TODO: Criar comando que permita definir um cargo pertencente aos Moderadores que poderão utilizar cargos de Moderador do bot.
    fun help(event: SlashCommandEvent) {
        event.deferReply(true).queue() // Let the user know we received the command before doing anything else
        val hook = event.hook
        // This is a special webhook that allows you to send messages without having permissions in the channel
        // and also allows ephemeral messages
        hook.setEphemeral(true) // All messages here will now be ephemeral implicitly
        val prefix = "!"


        val helpEmbedMessage = EmbedBuilder().setTitle("Lista de Comandos Legados")
            .setDescription("Aviso: Comandos utilizados com / não estão listados aqui.")
            .addField("> Comuns", "> Comandos que qualquer membro pode utilizar", false)
            .addField(prefix + "help", "Retorna esta lista de comandos por PM.", false)
            .addField("> Moderação", "> Comandos e ferramentas de moderação da guilda", false)
            .addField(
                prefix + "mute [@membro] [tempo (opcional)] [s/m/h (opcional)]",
                "Faz com que o membro mencionado seja incapaz de enviar mensagens por um tempo definido ou indefinido, de acordo com o uso.\nPermissão necessária: Gerenciar cargos",
                false
            )
            .addField(
                prefix + "unmute [@membro]",
                "Faz com que o membro mencionado volte a conseguir enviar mensagens (se estava previamente mutado).\nPermissão necessária: Gerenciar cargos",
                false
            )
            .addField(
                prefix + "unban [userID]",
                "Resulta na revogação do banimento do usuário mencionado.\nPermissão necessária: Banir membros",
                false
            )
            .addField(
                prefix + "modrole [@cargo]",
                "Permite definir o cargo que poderá utilizar os comandos Especiais.\nPermissão necessária: Administrador",
                false
            )
            .addField(
                prefix + "modroleID [ID do cargo]",
                "Versão alternativa do comando anterior que utiliza o ID do cargo. Recomendado para evitar menções.\nPermissão necessária: Administrador",
                false
            )
            .addField(
                "> Especiais",
                "> Comandos especiais para membros com cargo especial definido por um Administrador",
                false
            )
            .addField(
                prefix + "add [gatilho] [\"resultado entre aspas\"]",
                "Cria um comando personalizado.\nPermissão necessária: Administrador",
                false
            )
            .addField(
                prefix + "remove [gatilho]",
                "Remove o comando personalizado que possui o gatilho indicado.\nPermissão necessária: Administrador.",
                false
            )
            .addField(prefix + "prefix [caráter]", "Muda o prefixo dos comandos do bot.", false)
            .addField(
                prefix + "boost",
                "Ativa/desativa o agradecimento por impulso no servidor. Ao ativar será necessário indicar o canal onde deverão ser enviados os agradecimentos.",
                false
            )
            .addField(
                prefix + "boost [texto]",
                "Muda a mensagem de agradecimento. Para incluir uma menção ao membro impulsionador digite @menção. Por padrão é \"Obrigado pelo impulso @menção!\".",
                false
            )
            .setFooter("github.com/joseph-e-lou/DiscordBot", "https://i.imgur.com/w3duR07.png")
        hook.sendMessageEmbeds(helpEmbedMessage.build()).queue()
    }


    fun prefixInfo(event: SlashCommandEvent) {
        val prefix = event.guild?.let { DatabaseActions().checkForPrefixOld(it) }
        event.reply("Prefixo")
        event.channel.sendMessage("Prefixo do servidor: $prefix | Prefixo padrão: !").queue()
    }

    fun uptime(event: SlashCommandEvent) {
        val runtimeMXBean = ManagementFactory.getRuntimeMXBean()
        val dateFormat: DateFormat = SimpleDateFormat("HH:mm:ss")
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        val uptime = runtimeMXBean.uptime
        val finalUptime = (uptime / (3600 * 1000 * 24)).toString() + ":" + dateFormat.format(uptime)
        event.reply("Online há: $finalUptime").queue()
    }

    fun ping(event: SlashCommandEvent) {
        event.jda.restPing.queue { time ->
            event.channel.sendMessageFormat("Ping: %d ms | Websocket ${event.jda.gatewayPing} ",
                time).queue()
        }
    }

    fun userinfo(event: GuildMessageReceivedEvent) {
        val inputMessage = event.message.contentRaw
        val userID = StringUtils.split(inputMessage)[1]
        val theUser: User = try {
            event.jda.retrieveUserById(userID).complete()
        } catch (e: Exception) {
            EmbedUtilsBuilder.Regular()
                .channel(event.channel)
                .description("Não introduziu um userID válido.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)
            return
        }
        //TheUserAsMember se forem nulls nao vao ser utilizadas no Embed.
        //RetrieveMemberById nao retorna null por padrão - dá um erro. Este Try and Catch transforma o erro em um resultado válido.
        var theUserAsMember: Member? = null
        try {
            theUserAsMember = event.guild.retrieveMemberById(userID).complete()
        } catch (error: ErrorResponseException) {
            println("Error: Not a guild member.")
        }
        val theMemberTimeBoosted = theUserAsMember?.timeBoosted
        val userBotInfo = if (theUser.isBot) {
            "Sim"
        } else {
            "Não"
        }
        val userinfoMessage = EmbedBuilder().setTitle(theUser.name)
            .setColor(0x43b481)
            .setThumbnail(theUser.effectiveAvatarUrl)
            .setDescription(theUser.asMention + " \n" + theUser.asTag)
            .addField(
                "Data de Criação",
                theUser.timeCreated.format(DateTimeFormatter.ofPattern("dd MMMM yyyy  HH:mm O",
                    Locale.getDefault())),
                true
            )
        if (theUserAsMember != null) {
            userinfoMessage.addField(
                "Data de Entrada",
                theUserAsMember.timeJoined.format(
                    DateTimeFormatter.ofPattern(
                        "dd MMMM yyyy  HH:mm O",
                        Locale.getDefault()
                    )
                ),
                true
            )
        }
        if (theMemberTimeBoosted != null) {
            userinfoMessage.addField(
                "Data de Impulso",
                theMemberTimeBoosted.format(DateTimeFormatter.ofPattern("dd MMMM yyyy  HH:mm O",
                    Locale.getDefault())),
                false
            )
        }
        userinfoMessage
            .addField("Emblemas", theUser.flags.toString(), false)
            .addField("Bot", userBotInfo, false)
            .setFooter("Comando executado por @" + event.author.asTag)
        event.channel.sendMessage(userinfoMessage.build()).queue()
    }

    fun serverinfo(event: SlashCommandEvent) {
        event.deferReply(false).queue() // Let the user know we received the command before doing anything else
        val hook = event.hook

        val guild: Guild = event.guild!!
        val approximateOnlineMemberCount = guild.retrieveMetaData().complete().approximatePresences
        val owner = guild.retrieveOwner().complete()
        var boostTier = guild.boostTier.toString()
        when (boostTier) {
            "NONE" -> boostTier = "0"
            "TIER_1" -> boostTier = "1"
            "TIER_2" -> boostTier = "2"
            "TIER_3" -> boostTier = "3"
        }
        val epochMilli: Long = guild.timeCreated.toInstant().toEpochMilli()
        val timeCreatedConverted = DateTime(epochMilli)
        val period = Period(timeCreatedConverted, DateTime())
        val formattedTimeCreated = PeriodFormatterBuilder()
            .appendYears().appendSuffix(" anos ")
            .appendMonths().appendSuffix(" meses ")
            .appendDays().appendSuffix(" dias ")
            .appendHours().appendSuffix(" horas ")
            .appendMinutes().appendSuffix(" minutos ")
            .appendSeconds().appendSuffix(" segundos ")
            .printZeroNever()
            .toFormatter()
            .print(period)
            .trim()

        var botCount = 0
        guild.loadMembers().onSuccess { membersList ->
            membersList.forEach { member ->
                if (member.user.isBot) {
                    botCount++
                }
            }
        }
        // Complete the solve function below.
        var guildMaxFileSize = guild.maxFileSize / 1024 / 1024
        var guildMaxBitrate = guild.maxBitrate / 1000
        val serverinfoMessage = EmbedBuilder().setTitle(guild.name)
            .setDescription(guild.description)
            .setColor(0x43b481)
            .setThumbnail(guild.iconUrl)
            .addField(
                "Data de Criação",
                "" + TimeFormat.DATE_TIME_LONG.format(guild.timeCreated.toZonedDateTime()) + "\n(há $formattedTimeCreated)",
                true
            ).addField(
                "Membros Online",
                "${approximateOnlineMemberCount}/${guild.memberCount}",
                true
            )
            .addField(
                "Canais",
                "Texto: ${guild.textChannels.size}\nVoz: ${guild.voiceChannels.size}",
                true
            )
            .addField(
                "Dono/a",
                owner.user.asTag + " <:owner:823661010932989952>",
                true
            )
            .addField(
                "Emojis personalizados",
                guild.emotes.size.toString(), false
            )
            .addField(
                "Stickers personalizados",
                "*Em breve*", false
            )
            .addField(
                "Impulsos Nitro",
                "Nível: ${boostTier}\nTamanho máximo de ficheiros: ${guildMaxFileSize}MBs\nLimite de Emojis: ${guild.maxEmotes}\nBitrate máximo: ${guildMaxBitrate}kbps",
                false
            )
            .setFooter("ID do servidor: ${guild.id}")
        hook.sendMessageEmbeds(serverinfoMessage.build()).queue()
    }


}
