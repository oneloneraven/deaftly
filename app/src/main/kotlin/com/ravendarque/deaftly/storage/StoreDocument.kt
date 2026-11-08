package com.ravendarque.deaftly.storage

import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val STORE_VERSION = 1

@Serializable
data class StoreDocument(
    val version: Int,
    val templates: List<TemplateDto>,
    val settings: SettingsDto,
) {
    companion object {
        fun seeded(builtIns: List<Template>): StoreDocument = StoreDocument(STORE_VERSION, builtIns.map(Template::toDto), SettingsDto())
    }
}

@Serializable
data class SettingsDto(
    val showScheme: ShowSchemeDto? = null,
)

@Serializable
enum class ShowSchemeDto {
    @SerialName("black-on-white")
    BlackOnWhite,

    @SerialName("white-on-black")
    WhiteOnBlack,

    @SerialName("yellow-on-black")
    YellowOnBlack,
}

@Serializable
data class TemplateDto(
    val id: String,
    val title: String,
    val category: String,
    val body: String,
    val pinned: Boolean = false,
    val builtIn: Boolean = false,
    val placeholders: List<PlaceholderDto> = emptyList(),
)

@Serializable
data class PlaceholderDto(
    val key: String,
    val label: String,
    val type: PlaceholderTypeDto,
    val options: List<String> = emptyList(),
    val repeatable: Boolean = false,
)

@Serializable
enum class PlaceholderTypeDto {
    @SerialName("choices")
    Choices,

    @SerialName("typing")
    Typing,

    @SerialName("camera")
    Camera,
}

fun Template.toDto(): TemplateDto = TemplateDto(id, title, category, body, pinned, builtIn, placeholders.values.map(Placeholder::toDto))

fun TemplateDto.toDomain(): Template =
    Template(id, title, category, body, pinned, builtIn, placeholders.map(PlaceholderDto::toDomain).associateBy { it.key })

private fun Placeholder.toDto(): PlaceholderDto =
    PlaceholderDto(
        key = key,
        label = label,
        type =
            when (type) {
                PlaceholderType.Choices -> PlaceholderTypeDto.Choices
                PlaceholderType.Typing -> PlaceholderTypeDto.Typing
                PlaceholderType.Camera -> PlaceholderTypeDto.Camera
            },
        options = options,
        repeatable = repeatable,
    )

private fun PlaceholderDto.toDomain(): Placeholder =
    Placeholder(
        key = key,
        label = label,
        type =
            when (type) {
                PlaceholderTypeDto.Choices -> PlaceholderType.Choices
                PlaceholderTypeDto.Typing -> PlaceholderType.Typing
                PlaceholderTypeDto.Camera -> PlaceholderType.Camera
            },
        options = options,
        repeatable = repeatable,
    )
