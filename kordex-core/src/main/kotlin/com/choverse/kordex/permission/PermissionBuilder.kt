package com.choverse.kordex.permission

import com.choverse.kordex.KordexDsl

@KordexDsl
class PermissionChildrenBuilder internal constructor(
    private val children: MutableList<PermissionChild>,
) {
    fun permission(name: String, value: Boolean = true) {
        children += PermissionChild(name, value)
    }
}

@KordexDsl
class PermissionBuilder(private val name: String) {
    var description: String? = null
    var default: PermissionDefault = PermissionDefault.OP
    private val children = mutableListOf<PermissionChild>()

    fun children(block: PermissionChildrenBuilder.() -> Unit) {
        PermissionChildrenBuilder(children).apply(block)
    }

    fun build(): PermissionDefinition = PermissionDefinition(
        name = name,
        description = description,
        default = default,
        children = children.toList(),
    )
}
