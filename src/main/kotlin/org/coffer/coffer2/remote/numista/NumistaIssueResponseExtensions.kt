package org.coffer.coffer2.remote.numista

import org.coffer.coffer2.domain.coin.Issue

/**
 * Converts NumistaIssueResponse to domain Issue.
 *
 * @return Issue domain object
 */
fun NumistaIssueResponse.toIssue(): Issue {
    return Issue(
        numistaId = this.id.toString(),
        year = this.gregorianYear ?: this.year,
        mintage = this.mintage,
        mintLetter = this.mintLetter,
        comment = this.comment,
        isProof = this.isProof
    )
}
