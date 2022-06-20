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
import db.PreparedStatements
import embedutils.EmbedUtilsBuilder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.apache.commons.lang3.StringUtils


object CommandsGC : ListenerAdapter() {

    fun wiki(event: SlashCommandEvent, typedHero: String) {
        event.deferReply(false).queue()
        val hook = event.hook
        val gcList: ArrayList<String> = ArrayList()
        try {
            val rs = PreparedStatements(db.wiki).selectFromWhereId("SrHeroes", typedHero).executeQuery()
                gcList.add(rs.getString("id"))
                gcList.add(rs.getString("image"))
                gcList.add(rs.getString("setColor"))
                gcList.add(rs.getString("traits"))
                gcList.add(rs.getString("transSkills"))
                gcList.add(rs.getString("chaserSkills"))
                gcList.add(rs.getString("wikiUrl"))
            // Para imagem definir sempre a versão com transcendência.
            StringUtils.upperCase(gcList[0])
            val heroEmbedMessage = EmbedBuilder().setTitle(gcList[0], gcList[6])
                .addField("Cor dos equipamentos", gcList[2], false)
                .addField("Especializações", gcList[3], false)
                .addField("Habilidades para aprimorar", gcList[4], false)
                .addField("Especializações Chaser", gcList[5], false)
                .setImage(gcList[1])
                .setFooter("Bot criado por @justinthedog#7100")
            hook.sendMessageEmbeds(heroEmbedMessage.build()).queue()
        } catch (e: IndexOutOfBoundsException) {
            // if the error message is "out of memory", it probably means no database file is found.
            // Se na linha de comandos aparecer "Index: 0" é porque nao foi introduzido um heroi SR valido.
            println(e)
            hook.sendMessageEmbeds(
                EmbedUtilsBuilder
                    .Regular()
                    .channel(event.textChannel)
                    .description("Herói não encontrado. Verifique se introduziu o nome de um herói de rank SR corretamente.")
                    .result(EmbedUtilsBuilder.ResultType.ERROR)
            ).queue()
        }
    }
}