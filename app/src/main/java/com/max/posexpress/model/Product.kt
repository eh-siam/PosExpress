package com.max.posexpress.model

import androidx.annotation.Keep

@Keep
data class Product(
    var id: Int = 0,
    var name: String = "",
    var price: Double = 0.0,
    var category: String = "General",
    var imageUrl: String = ""
) {
    // Custom setters logic handled via primary constructor or secondary if needed
    // But for Firebase, default values in data class are usually sufficient.
}
