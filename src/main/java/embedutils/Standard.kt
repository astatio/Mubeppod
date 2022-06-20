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

enum class Standard(val description: String) {
    ERROR_GENERIC("Ocorreu um erro. Por favor, verifique se digitou corretamente e tente novamente."),
    ERROR_MANAGE_RULES("Você não tem permissões para gerir cargos."),
    ERROR_NO_MENTIONED_MEMBERS("Nenhum membro foi mencionado neste comando."),
    ERROR_NO_MENTIONED_USERS("Nenhum usuário foi mencionado neste comando."),
    ERROR_BOT_INSUFFICIENT_PERMISSION("Eu não tenho permissões suficientes para fazer isso."),
    ERROR_AUTHOR_INSUFFICIENT_PERMISSION("Você não tem permissões suficientes para fazer isso."),
    ERROR_MEMBER_NOT_IN_GUILD("O membro não faz parte da guilda atualmente."),
    ERROR_NO_ADMIN_PERMISSION("Você não tem permissões de administrador.")
}