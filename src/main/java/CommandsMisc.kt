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
import embedutils.EmbedUtilsBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import reactor.core.publisher.Mono

object CommandsMisc : ListenerAdapter() {

    private fun togglingMessage(hook: InteractionHook, textChannel: TextChannel, module: String, status : String) : WebhookMessageAction<Message> {
        return hook.sendMessageEmbeds(
            EmbedUtilsBuilder.Regular()
                .channel(textChannel)
                .description("O $module está agora $status.")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS))
    }


    fun boostThanksToggle(event: SlashCommandEvent, targetChannel: GuildChannel?, message: String?) {
        event.deferReply(false).queue()
        val hook = event.hook
        val guildId = event.guild!!.id
        if (!DatabaseActions().hasModRole(event, guildId, hook)) {
            return
        }
        //Primeiramente, verifica se está ativado ou desativado. É boolean.
        val isBoostThanksEnabled = Connect.checkForBoostThanks(event)
        if (!isBoostThanksEnabled) { // Se tiver desativado
            if ((targetChannel == null) || (message == null) || (targetChannel.type != ChannelType.TEXT)) { //Mas nao tiver canal de texto
                hook.sendMessageEmbeds(
                    EmbedUtilsBuilder.Regular()
                        .channel(event.textChannel)
                        .description("É necessário indicar um canal de texto e mensagem para ativar a funcionalidade.")
                        .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
                return
            } else { //E se tiver canal de texto e mensagem - ou seja tudo certo!
                DatabaseActions().enableAndStoreBoostThanks(guildId, targetChannel, message)
                togglingMessage(hook, event.textChannel, "agradecimento automático por impulso" , "ativado").queue()
                return
            }
        } else { //Se não estiver desativado
            DatabaseActions().disableBoostThanks(guildId)
            togglingMessage(hook, event.textChannel, "agradecimento automático por impulso" , "desativado").queue()
            return
        }
    }

    fun welcomeToggle(event: SlashCommandEvent, targetChannel: GuildChannel?, message: String?) {
        event.deferReply(false).queue()
        val hook = event.hook
        val guildId = event.guild!!.id
        if (!DatabaseActions().hasModRole(event, guildId, hook)) {
            return
        }
        //Primeiramente, verifica se está ativado ou desativado. É boolean.
        val isWelcomeEnabled = Connect.checkForWelcome(event)
        if (!isWelcomeEnabled) { // Se tiver desativado
            if ((targetChannel == null) || (message == null) || (targetChannel.type != ChannelType.TEXT)) { //Mas nao tiver canal de texto
                hook.sendMessageEmbeds(
                    EmbedUtilsBuilder.Regular()
                        .channel(event.textChannel)
                        .description("É necessário indicar um canal de texto e mensagem para ativar a funcionalidade.")
                        .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
                return
            } else { //E se tiver canal de texto e mensagem — ou seja, tudo certo!
                DatabaseActions().enableAndStoreWelcome(guildId, targetChannel, message)
                togglingMessage(hook, event.textChannel, "envio automático de mensagem por entrada de membro" , "ativado").queue()
                return
            }
        } else { //Se não estiver desativado
            DatabaseActions().disableWelcome(guildId)
            togglingMessage(hook, event.textChannel, "envio automático de mensagem por entrada de membro" , "desativado").queue()
            return
        }
    }

    fun setModRole(event: SlashCommandEvent, modRole: Role) {
        event.deferReply().queue()
        val hook = event.hook
        if (!event.member?.hasPermission(Permission.ADMINISTRATOR)!!) {
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description("Você não tem permissões de administrador.")
                .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
            return
        }
        DatabaseActions().setModRole(event.guild!!.id, modRole.id)
        hook.sendMessageEmbeds(
        EmbedUtilsBuilder
            .Regular()
            .channel(event.textChannel)
            .description("Foi definido com sucesso o novo cargo de Moderador")
            .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
        return
    }

    fun whatsTheModrole(event: SlashCommandEvent){
        event.deferReply(true).queue()
        val hook = event.hook
        val guildID = event.guild!!.id
        if(!DatabaseActions().hasModRole(event,guildID,hook)){
            hook.sendMessageEmbeds(
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description("Não há um cargo de Moderador definido neste servidor")
                .result(EmbedUtilsBuilder.ResultType.ERROR)).queue()
            return
        }
        val modRoleId = DatabaseActions().getModRoleId(guildID)
        // Se for null, vai dar erro na linha a seguir. Isso não irá acontecer.
        val modRole = event.guild!!.getRoleById(modRoleId!!)
        hook.sendMessageEmbeds(
            EmbedUtilsBuilder
                .Regular()
                .channel(event.textChannel)
                .description("O cargo de Moderador é ${modRole!!.asMention}")
                .result(EmbedUtilsBuilder.ResultType.SUCCESS)).queue()
    }


    fun getGuilds(event: MessageReceivedEvent): Mono<*> {
        event.jda.guilds.size
        val guildNames: java.util.ArrayList<String> = java.util.ArrayList<String>()
        for (item in event.jda.guilds) {
            guildNames.add(item.name + " `ID: ${item.id}`")
        }
        return event.channel.sendMessage("O bot está atualmente em " + event.jda.guilds.size + " guildas com os seguintes nomes: " + guildNames.toString()).asMono()
    }

    fun leaveGuild(guildID: String?,event: MessageReceivedEvent): Mono<*> {
        return if (guildID.isNullOrEmpty()){
            event.textChannel.sendMessage("ID inválido ou não consegui encontrar a guilda.").asMono()
        }else{
            val guild = event.jda.getGuildById(guildID)!!
            val guildName = guild.name
            guild.leave().asMono()
                .doOnSuccess { event.textChannel.sendMessage("O abandono da guilda \"$guildName\" foi bem sucedido.").asMono() }
                .doOnError { event.textChannel.sendMessage("Não foi possível abandonar a guilda \"$guildName\".").asMono() }
        }
    }

}