package com.ravendarque.deaftly.templates

import android.content.res.Resources
import com.ravendarque.deaftly.R
import com.ravendarque.deaftly.core.template.Placeholder
import com.ravendarque.deaftly.core.template.PlaceholderType
import com.ravendarque.deaftly.core.template.Template

fun builtInTemplates(resources: Resources): List<Template> = BuiltInTemplateFactory(resources).create()

private class BuiltInTemplateFactory(
    private val resources: Resources,
) {
    private val howToReply =
        placeholder(
            "how to reply",
            R.string.placeholder_how_to_reply_label,
            PlaceholderType.Choices,
            R.array.placeholder_how_to_reply_options,
        )
    private val name = placeholder("name", R.string.placeholder_name_label, PlaceholderType.Typing)

    fun create(): List<Template> =
        listOf(
            template(
                BuiltInIds.CHECK_IN,
                R.string.template_check_in_title,
                R.string.category_everyday,
                R.string.template_check_in_body,
                name,
                howToReply,
                pinned = true,
            ),
            template(
                BuiltInIds.ORDER_FOOD,
                R.string.template_order_food_title,
                R.string.category_food_drink,
                R.string.template_order_food_body,
                placeholder("menu item", R.string.placeholder_menu_item_label, PlaceholderType.Camera, repeatable = true),
                pinned = true,
            ),
            template(
                BuiltInIds.EXPLAIN_DEAF,
                R.string.template_explain_deaf_title,
                R.string.category_everyday,
                R.string.template_explain_deaf_body,
                howToReply,
            ),
        ) + askingForHelp() + health() + travel()

    private fun askingForHelp(): List<Template> =
        listOf(
            template(
                BuiltInIds.ASK_HELP,
                R.string.template_ask_help_title,
                R.string.category_everyday,
                R.string.template_ask_help_body,
                placeholder("thing", R.string.placeholder_thing_label, PlaceholderType.Choices, R.array.placeholder_thing_options),
                howToReply,
            ),
            template(
                BuiltInIds.FOOD_ALLERGY,
                R.string.template_food_allergy_title,
                R.string.category_food_drink,
                R.string.template_food_allergy_body,
                placeholder(
                    "allergen",
                    R.string.placeholder_allergen_label,
                    PlaceholderType.Choices,
                    R.array.placeholder_allergen_options,
                    repeatable = true,
                ),
                howToReply,
            ),
        )

    private fun health(): List<Template> =
        listOf(
            template(
                BuiltInIds.DOCTOR,
                R.string.template_doctor_title,
                R.string.category_health,
                R.string.template_doctor_body,
                placeholder("time", R.string.placeholder_time_label, PlaceholderType.Typing),
                placeholder(
                    "how I communicate",
                    R.string.placeholder_how_i_communicate_label,
                    PlaceholderType.Choices,
                    R.array.placeholder_how_i_communicate_options,
                    repeatable = true,
                ),
                howToReply,
            ),
            template(
                BuiltInIds.PHARMACY,
                R.string.template_pharmacy_title,
                R.string.category_health,
                R.string.template_pharmacy_body,
                placeholder("patient", R.string.placeholder_patient_label, PlaceholderType.Typing),
                howToReply,
            ),
        )

    private fun travel(): List<Template> =
        listOf(
            template(
                BuiltInIds.BUY_TICKET,
                R.string.template_buy_ticket_title,
                R.string.category_travel,
                R.string.template_buy_ticket_body,
                placeholder("place", R.string.placeholder_place_label, PlaceholderType.Typing),
                howToReply,
            ),
            template(
                BuiltInIds.PAYING,
                R.string.template_paying_title,
                R.string.category_everyday,
                R.string.template_paying_body,
                placeholder("payment", R.string.placeholder_payment_label, PlaceholderType.Choices, R.array.placeholder_payment_options),
            ),
        )

    private fun placeholder(
        key: String,
        label: Int,
        type: PlaceholderType,
        options: Int? = null,
        repeatable: Boolean = false,
    ) = Placeholder(
        key = key,
        label = resources.getString(label),
        type = type,
        options = options?.let { resources.getStringArray(it).toList() }.orEmpty(),
        repeatable = repeatable,
    )

    private fun template(
        id: String,
        title: Int,
        category: Int,
        body: Int,
        vararg placeholders: Placeholder,
        pinned: Boolean = false,
    ) = Template(
        id = id,
        title = resources.getString(title),
        category = resources.getString(category),
        body = resources.getString(body),
        pinned = pinned,
        builtIn = true,
        placeholders = placeholders.associateBy { it.key },
    )
}
