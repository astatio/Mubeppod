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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.Button
import org.joda.time.Instant
import org.joda.time.Period
import javax.script.ScriptEngineManager


class SlashCommands : ListenerAdapter() {

    override fun onSlashCommand(event: SlashCommandEvent) {
        // Only accept commands from guilds
        if (event.guild == null) return
        when (event.name) {
            "modrole" -> {
                when (event.subcommandName) {
                    "verificar" -> CommandsMisc.whatsTheModrole(event)
                    "definir" -> {
                        val modRole : Role = event.getOption("cargo")!!.asRole
                        CommandsMisc.setModRole(event, modRole)
                    }
                }
            }
            "comandos" -> {
                when (event.subcommandName) {
                    "adicionar" -> {
                        val triggerMessage = event.getOption("gatilho")!!.asString
                        val message = event.getOption("conteúdo")!!.asString
                        CommandsCustom.addCustomCommand(event, triggerMessage, message)
                    }
                    "remover" -> {
                        val triggerMessage = event.getOption("gatilho")!!.asString
                       CommandsCustom.removeCustomCommand(event, triggerMessage)
                    }
                }
            }
            "boost" -> {
                val targetChannel: GuildChannel? = event.getOption("canal")?.asGuildChannel
                val message: String? = event.getOption("mensagem")?.asString
                CommandsMisc.boostThanksToggle(event, targetChannel, message)
            }
            "welcome" -> {
                val targetChannel: GuildChannel? = event.getOption("canal")?.asGuildChannel
                val message: String? = event.getOption("mensagem")?.asString
                CommandsMisc.welcomeToggle(event, targetChannel, message)
            }
            "prefixo" -> {
                CommandsGeneral.prefixInfo(event)
            }
            "mute" -> {
                val member = event.getOption("usuário")!!.asMember
                val time: Long = event.getOption("tempo")?.asLong ?: 0
                val timeUnit: String? = event.getOption("unidade")?.asString
                CommandsGeneral.mute(event, member, time, timeUnit)
            }
            "unmute" -> {
                val memberToMute: Member? = event.getOption("usuário")?.asMember
                CommandsGeneral.unmute(event, memberToMute)
            }
            "uptime" -> CommandsGeneral.uptime(event)
            "ping" -> CommandsGeneral.ping(event)
            "serverinfo" -> {
                CommandsGeneral.serverinfo(event)
            }
            "limpa" -> {
                limpa(event)
            }
            "ban" -> {
                val member =
                    event.getOption("usuário")!!.asMember // the "user" option is required so it doesn't need a null-checkIfFullfiled here
                val user: User = event.getOption("usuário")!!.asUser
                val banMessage: String? = event.getOption("mensagem")?.asString
                CommandsGeneral.ban(event, user, member, banMessage)
            }
            "help" -> CommandsGeneral.help(event)
            "gc" -> {
                when (event.subcommandName) {
                    "wiki" -> CommandsGC.wiki(event,
                        event.getOption("heroi")!!.asString)  // content is required so no null-checkIfFullfiled here
                }
            }
            "eval" -> eval(event,
                event.getOption("kotlin")!!.asString) // content is required so no null-checkIfFullfiled here
            else -> event.reply("Eu não posso executar esse comando agora :(").setEphemeral(true).queue()
        }
    }

    fun eval(event: SlashCommandEvent, scriptContent: String) {
        val startTime = Instant.now()
        val result = with(ScriptEngineManager().getEngineByExtension("kts")) {
            eval(scriptContent)
        }
        val resultAsString = result.toString()
        val timeTaken = Period(startTime).millis
        event.deferReply(false).queue()
        event.replyEmbeds(EmbedBuilder()
            .setTitle("Resultado")
            .addField("Duração", "${timeTaken}ms", false)
            .addField("Código", "```${scriptContent}```", false)
            .addField("Resultado", resultAsString, false)
            .build())
            .queue()
    }

/*
    fun newEval(message: Message, code: String): Task = Mono.defer {
        engine["event"] = message
        engine["message"] = message
        engine["author"] = message.author
        engine["api"] = message.jda
        engine["member"] = message.member
        engine["guild"] = message.guild
        engine["channel"] = message.channel

        val output: String? = evalWrap(code)

        if (output == null)
            message.addReaction(SUCCESS).asMono()
        else
            message.channel.sendMessage(output).asMono()
    }.subscribeOn(Schedulers.elastic())
*/


    fun limpa(event: SlashCommandEvent) {
        // Let the user know we received the command before doing anything else
        val hook = event.hook
        // This is a special webhook that allows you to send messages without having permissions in the channel
        // and also allows ephemeral messages
        hook.setEphemeral(true) // All messages here will now be ephemeral implicitly
        if (!event.member?.hasPermission(Permission.MESSAGE_MANAGE)!!) {
            hook.sendMessage("Você não tem permissões para gerir mensagens.")
            return
        }
        val amountOption = event.getOption("quantidade") // This is configured to be optional so check for null
        val amount = if (amountOption == null) 100 // default 100
        else 200.coerceAtMost(2.coerceAtLeast(amountOption.asLong.toInt())) // enforcement: must be between 2-200
        val userId = event.user.id
        event.reply("Esta ação irá eliminar $amount mensagens.\nPretende continuar?") // prompt the user with a button menu
            .addActionRow( // this means "<style>(<id>, <label>)" the id can be spoofed by the user so setup some kinda verification system
                Button.secondary("$userId:cancelled", "Mudei de ideia."),
                Button.danger("$userId:prune:$amount",
                    "Sim!")) // the first parameter is the component id we use in onButtonClick above
            .queue()
    }

    override fun onButtonClick(event: ButtonClickEvent) {
        // users can spoof this id so be careful what you do with this
        val id = event.componentId.split(":").toTypedArray() // this is the custom id we specified in our button
        val authorId = id[0]
        val type = id[1]
        // When storing state like this is it is highly recommended to do some kind of verification that it was generated by you, for instance a signature or local cache
        if (authorId != event.user.id) return
        event.deferEdit().queue() // acknowledge the button was clicked, otherwise the interaction will fail
        val channel = event.channel
        when (type) {
            "prune" -> {
                val amount = id[2].toInt()
                event.channel.iterableHistory
                    .skipTo(event.messageIdLong)
                    .takeAsync(amount)
                    .thenAccept { messages: List<Message?>? ->
                        channel.purgeMessages(
                            messages!!)
                    }
                event.hook.deleteOriginal().queue()
            }
            "cancelled" -> event.hook.deleteOriginal().queue()
        }
    }


}