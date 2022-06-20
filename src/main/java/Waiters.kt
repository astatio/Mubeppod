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

/*

class Waiters : ListenerAdapter() {
    fun initWaiterUnbanInvite(
        waiter: EventWaiter,
        userId: User,
        guildName: String,
        guildChannel: TextChannel,
        executorUser: User,
        botMessageId: String
    ) {
        waiter.waitForEvent(
            GuildMessageReactionAddEvent::class.java, { event: GuildMessageReactionAddEvent ->
                val emote = event.reactionEmote
                val user = event.user
                !user.isBot && emote.asReactionCode == "\u2705" && event.messageId == botMessageId && executorUser === event.user
            },
            { event: GuildMessageReactionAddEvent ->
                val inviteUrl: Any =
                    Objects.requireNonNull(event.guild.defaultChannel)!!.createInvite().setTemporary(false)
                        .complete().url
                userId.openPrivateChannel().flatMap { channel: PrivateChannel ->
                    channel.sendMessage("Você foi desbanido de $guildName. Aqui está um convite caso pretenda retornar. $inviteUrl")
                }.queue(
                    {
                        EmbedUtilsBuilder
                            .Regular()
                            .channel(guildChannel)
                            .description("O usuário " + userId.asTag + " recebeu um convite para voltar.")
                            .result(EmbedUtilsBuilder.ResultType.SUCCESS)
                    }
                ) {
                    EmbedUtilsBuilder
                        .Regular()
                        .channel(guildChannel)
                        .description("O usuário " + userId.asTag + " não recebeu um convite devido a um erro. É possível que não partilhe nenhum outro servidor com o bot.")
                        .result(EmbedUtilsBuilder.ResultType.ERROR)
                }
            },
            10, TimeUnit.SECONDS
        ) { guildChannel.sendMessage("Tempo expirado.").queue() }
    }

}*/
