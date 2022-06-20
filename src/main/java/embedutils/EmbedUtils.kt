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
package embedutils


import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.requests.restaction.MessageAction
import org.jetbrains.annotations.NotNull

class EmbedUtilsBuilder {

    // https://www.baeldung.com/kotlin/constructors
    /**
     * Creates an Embed with predefined values.
     *
     * This class has no useful logic; it's just a documentation example.
     *
     * @param T the type of a member in this group.
     * @property name the name of this group.
     * @constructor Creates an empty group.
     */

    enum class ResultType {
        ERROR, DUBIOUS, SUCCESS
    }

    class Regular {


        private lateinit var resu: ResultType
        private lateinit var chan: TextChannel
        private lateinit var desc: String

        @NotNull
        fun result(resultType: ResultType): MessageEmbed {
            this.resu = resultType
            return send()
        }

        @NotNull
        fun channel(channel: TextChannel): Regular {
            this.chan = channel
            return this
        }

        @NotNull
        fun description(description: Any): Regular {
            this.desc = description.toString()
            return this
        }

        @NotNull
        private fun send(): MessageEmbed {
            val embedMessage = when (this.resu) {
                ResultType.ERROR -> EmbedBuilder().setTitle("<a:deniedbox:755935838851956817>")
                    .setColor(0xf04947)
                    .addField("Erro", this.desc, false)
                ResultType.DUBIOUS -> EmbedBuilder().setTitle("\u2754")
                    .setColor(0xccd6dd)
                    .addField("Sucesso Incerto", this.desc, false)
                ResultType.SUCCESS -> EmbedBuilder().setTitle("<a:acceptedbox:755935963875901471>")
                    .setColor(0x43b481)
                    .addField("Sucesso", this.desc, false)
            }
            return embedMessage.build()
        }
    }

    class TimeOut {

        fun timeout(channel: TextChannel, description: String, timeoutTime: Int = 10): MessageAction {
            // Por defeito, são 10 segundos
            val embedMessage = EmbedBuilder()
                .setColor(0xccd6dd)
                .setDescription(description)
                .setFooter("Expira em $timeoutTime segundos")
            return channel.sendMessageEmbeds(embedMessage.build())   //Nao tem queue aqui porque este embed é utilizado junto com Waiters.
        }

    }

}







