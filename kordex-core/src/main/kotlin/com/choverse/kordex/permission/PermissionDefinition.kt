package com.choverse.kordex.permission

data class PermissionDefinition(
    val name: String,
    val description: String? = null,
    val default: PermissionDefault = PermissionDefault.OP,
    val children: List<PermissionChild> = emptyList(),
)
