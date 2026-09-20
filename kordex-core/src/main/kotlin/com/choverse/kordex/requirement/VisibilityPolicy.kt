package com.choverse.kordex.requirement

/**
 * Per-[com.choverse.kordex.KordexInstance] policy for command-tree visibility filtering.
 *
 * When [hideEmptyParents] is `true` (the default), a literal node with children but no
 * currently-visible child and no executor of its own is hidden from suggestions entirely,
 * instead of showing an empty/dead branch.
 */
class VisibilityPolicy {
    var hideEmptyParents: Boolean = true
}
