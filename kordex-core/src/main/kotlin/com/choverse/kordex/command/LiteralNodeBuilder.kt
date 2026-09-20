package com.choverse.kordex.command

class LiteralNodeBuilder internal constructor() : NodeBuilder() {
    internal fun buildNode(name: String): CommandNode.Literal = CommandNode.Literal(
        name = name,
        children = childNodes.toList(),
        executor = nodeExecutor,
        requirement = nodeRequirement,
        visibilityOverride = nodeVisibility,
        hidden = nodeHidden,
        permission = nodePermission,
    )
}
