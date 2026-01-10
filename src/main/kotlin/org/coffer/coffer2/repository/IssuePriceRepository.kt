package org.coffer.coffer2.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface IssuePriceRepository : JpaRepository<IssuePriceEntity, UUID>
