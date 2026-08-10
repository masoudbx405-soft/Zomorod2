package com.example.data.model

/**
 * مدل داده‌ای اقلام، خدمات و تعرفه‌های مصوب فراخوانی‌شده از پنل مرکزی قالیشویی زمرد (panel.yaselectrical.ir)
 */
data class PanelCatalogItem(
    val id: String,
    val name: String,
    val category: String, // "CARPET_TYPE", "SERVICE", "DEFECT"
    val unitPrice: Long,
    val defaultLength: Double = 0.0,
    val defaultWidth: Double = 0.0,
    val unit: String = "متر مربع",
    val description: String = "",
    val isDefault: Boolean = false
)

/**
 * وضعیت کلی کاتالوگ و تعرفه‌های پنل در برنامه
 */
data class PanelCatalogState(
    val carpetTypes: List<PanelCatalogItem> = emptyList(),
    val services: List<PanelCatalogItem> = emptyList(),
    val defects: List<PanelCatalogItem> = emptyList(),
    val isFromLiveServer: Boolean = false,
    val lastFetchedTime: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
