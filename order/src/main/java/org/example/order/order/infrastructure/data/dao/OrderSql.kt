package org.example.order.order.infrastructure.data.dao

import org.example.order.order.application.model.order.request.OrderFilterRequest

object OrderSql {

    fun filter(locationIds: List<Int>, request: OrderFilterRequest): String {
        return """
            WITH pg AS (
                SELECT o.id, o.store_id
                ${filterBody(locationIds, request)}
                ORDER BY o.id DESC
                OFFSET :pageSize * (:pageIndex - 1) ROWS
                FETCH NEXT :pageSize ROWS ONLY
            )
            SELECT o.* 
            FROM pg 
            JOIN orders as o ON pg.store_id = o.store_id AND pg.id = o.id 
            ORDER BY o.id DESC
        """.trimIndent()
    }

    private fun filterBody(locationIds: List<Int>, req: OrderFilterRequest): String {
        return """
            FROM orders as o
            ${if (req.tag?.isNotBlank() == true) " JOIN order_tags ot ON o.store_id = ot.store_id AND o.id = ot.order_id" else ""}
            WHERE o.store_id = :storeId
            ${if (req.ids?.isNotEmpty() == true) " AND o.id IN (:ids)" else ""}
            ${if (req.tag?.isNotBlank() == true) " AND ot.value = :tag" else ""}
            ${if (locationIds.isNotEmpty()) " ANd o.location_id IN (:locationIds)" else ""}
            ${if (req.query?.isNotBlank() == true) " AND o.name LIKE :query" else ""}
            ${if (req.status?.isNotBlank() == true) " AND o.status = :status" else ""}
            ${if (req.financialStatus?.isNotBlank() == true) " AND o.financial_status = :financialStatus" else ""}
            ${if (req.customerId > 0) " AND o.customer_id = :customerId" else ""}
            ${if (req.createdOnMin != null) " AND o.created_on >= :createdOnMin" else ""}
            ${if (req.createdOnMax != null) " AND o.created_on <= :createdOnMax" else ""}
            ${if (req.modifiedOnMin != null) " AND o.modified_on >= :modifiedOnMin" else ""}
            ${if (req.modifiedOnMax != null) " AND o.modified_on <= modifiedOnMax" else ""}
            ${if (req.processOnMin != null) " AND o.process_on >= :processOnMin" else ""}
            ${if (req.processOnMax != null) " AND o.process_on <= processOnMax" else ""}
        """.trimIndent()
    }

}