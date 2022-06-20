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

import net.dv8tion.jda.api.MessageBuilder


import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.commons.lang3.StringUtils

class Ticketing : ListenerAdapter() {

    //TODO: Criar ModMail
    //Com a criação dos botoes Discord, apos enviar para um canal destinado aos mods
    // um mod poderá clicar no botão para assinar a si próprio um determinado "ticket" . A mensagem enviada pelo bot
    // no canal em questão será editada para remover os botões e indicar a que moderador ficou assinado ao "ticket".
    //
    // O mod deve então falar por DM com o membro em questão e, assim que for tratado, deverá fechar o ticket em questão.
    // O mod terá que usar um comando como "!ticket close 199923444 Tratei da situação eliminando 1 mensagem".
    // A mensagem será editada novamente a indicar que ficou tratado e uma mensagem a indicar que foi fechado
    // sera enviado no canal em que o comando foi executado. Não é uma mensagem temporaria.
    // Tickets em aberto e tratamento ficam numa base de dados/tabela. Tickets fechados são removidos da base de dados.

    @Override
    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        if (event.author.isBot) return
        var theMessage = event.message.contentDisplay
        
        //TODO: Comando abaixo é o inicio do ModMail. Não se encontra funcional.
        if (theMessage.startsWith("!ticket")) {
            //Allows sending messages to ticketing channel to a certain guild through PM.
            val split = StringUtils.split(theMessage)
            theMessage = theMessage.replace(("(?i)!ticket\\s" + split[1] + "\\s").toRegex(), "")
            //TODO: Estou aqui.
            event.jda.getGuildById(split[1])

            val sendToChannel = event.jda.getTextChannelById(split[1])
            val messageBuilder = MessageBuilder()
            messageBuilder.setContent(theMessage)
            assert(sendToChannel != null)
            sendToChannel!!.sendMessage(messageBuilder.build()).queue()
            event.channel.sendMessage("Mensagem enviada - ou talvez não.").queue()
        }
    }


}