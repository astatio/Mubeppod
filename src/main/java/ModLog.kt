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
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Period
import kotlin.concurrent.thread
import kotlin.properties.Delegates

object ModLog : ListenerAdapter() {

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        thread {
            lateinit var channelId: String
            try {
                channelId = getModLogChannels(event.guild.id)[1]
            } catch (e: IndexOutOfBoundsException) {
                println("A member was removed, but the modlog2 channel was not set.")
                return@thread
            }
            val guild = event.guild
            val member = event.member
            var roles = ""
            var title = ""
            var desc = ""
            var moderator = ""
            val ifMemberNickname = fun(): String {
                return if (member?.nickname != null) {
                    "- " + member.nickname
                } else {
                    ""
                }
            }
            var color by Delegates.notNull<Int>()
            for (i in member?.roles!!) {
                roles += "${i.asMention}\n"
            }

            val retrievedAuditLogs = guild.retrieveAuditLogs()
                .limit(1)
                .complete()
            for (entry in retrievedAuditLogs) {
                if (retrievedAuditLogs.isEmpty()) //É improvável estar vazio mas nunca se sabe.
                    break
                if (entry.targetId != member.id) {
                    break
                }
                moderator = entry.user!!.asTag
                if (entry.type == ActionType.KICK) {
                    if (entry.user?.isBot == true) {
                        moderator = entry.reason!!
                        moderator = moderator.replace("^Comando executado por ".toRegex(), "")
                        moderator = moderator.replace("\\(ID \\w+\\)$".toRegex(), "")
                    }
                    title = "Membro Expulso"
                    desc =
                        "**${member.asMention} - ${member.user.asTag} ${ifMemberNickname()}** foi expulso/a por $moderator"
                    color = 0xF5A623
                }
                if (entry.type == ActionType.BAN) {
                    if (entry.user?.isBot == true) {
                        moderator = entry.reason!!
                        moderator = moderator.replace("^Comando executado por ".toRegex(), "")
                        moderator = moderator.replace("\\(ID \\w+\\)$".toRegex(), "")
                    }
                    title = "Membro Banido"
                    desc =
                        "**${member.asMention} - ${member.user.asTag} ${ifMemberNickname()}** foi banido/a por $moderator"
                    color = 0xD0021B
                }
            }
            if (title.isBlank()) {
                title = "Membro Saiu"
                desc = "**${member.asMention} - ${member.user.asTag} ${ifMemberNickname()}** saiu do servidor"
                color = 0xF8E71C
            }
            val emb = EmbedBuilder().setTitle(title)
                .setDescription(desc + "\n**Cargos (${member.roles.size}):**\n${roles}")
                .setColor(color)
                .setTimestamp(java.time.Instant.now())
                .setThumbnail(member.user.effectiveAvatarUrl)
                .setFooter("User ID: ${member.id}")

            val channel = event.jda.getTextChannelById(channelId)
            channel!!.sendMessageEmbeds(emb.build()).queue()
            // enviar para channel[1] (que e o segundo)
        }

    }


    fun antiZeroTimeCalculation(lastDiff: Period): String {
        // In order, will checkIfFullfiled if the hour is bigger than 0, if yes will go for hours and so on and so forth.
        val lastDiffDays = lastDiff.days
        val lastDiffHours = lastDiff.hours
        val lastDiffMinutes = lastDiff.minutes

        return if (lastDiffDays > 0) {
            "$lastDiffDays days ago"
        } else {
            if (lastDiffHours > 0) {
                "$lastDiffHours hours ago"
            } else {
                if (lastDiffMinutes > 0) {
                    "$lastDiffMinutes minutes ago"
                } else {
                    "${lastDiff.seconds} seconds ago"
                }

            }
        }
    }

    private fun getModLogChannels(guildId: String): ArrayList<String> {
        val guildConfigTable = object : Table(name = "$guildId CONFIGS") {
            val id = text("id")
            val channelId = text("inside")
            val notes = text("notes")
        }
        var modLogChannelsList: ArrayList<String> = ArrayList<String>()
        Database.connect("jdbc:sqlite:databases/Configs.db", "org.sqlite.JDBC")
        try {
            transaction {
                guildConfigTable.select { (guildConfigTable.id eq "modlog1") }.forEach {
                    modLogChannelsList.add(0, it[guildConfigTable.channelId])
                }
                guildConfigTable.select { (guildConfigTable.id eq "modlog2") }.forEach {
                    modLogChannelsList.add(1, it[guildConfigTable.channelId])
                }
            }

        } catch (e: Exception) {
            // if the error message is "out of memory", it probably means no database file is found.
            // i cant think of other errors that could happen
            println(e)
            println("An error occurred while trying to get the modlog channels.")
        }
        return modLogChannelsList
    }


}