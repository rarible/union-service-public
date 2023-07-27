package com.rarible.protocol.union.core.restriction

import com.rarible.protocol.union.dto.ItemIdDto
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup

class RestrictionTemplateSubstitutor(
    private val itemId: ItemIdDto,
    private val parameters: Map<String, String>
) : StringLookup {

    companion object {
        val defaultParameters = mapOf<String, (itemId: ItemIdDto) -> String>(
            "itemId" to ({ itemId -> itemId.value })
        )
    }

    fun substitute(template: String): String {
        val substitutor = StringSubstitutor(this)
            .setEnableSubstitutionInVariables(false)
            .setDisableSubstitutionInValues(true)

        return substitutor.replace(template)
    }

    override fun lookup(key: String): String {
        val provider = defaultParameters[key] ?: return parameters[key] ?: ""
        return provider(itemId)
    }
}
