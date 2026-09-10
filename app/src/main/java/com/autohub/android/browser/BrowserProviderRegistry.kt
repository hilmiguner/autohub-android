package com.autohub.android.browser

class BrowserProviderRegistry(
    descriptors: Collection<BrowserProviderDescriptor>,
) {
    private val descriptorsById: Map<String, BrowserProviderDescriptor>
    private val descriptorsByOrigin: Map<String, BrowserProviderDescriptor>

    init {
        require(descriptors.all(BrowserProviderBridgePolicy::isValidDescriptor)) {
            "All browser provider descriptors must be valid."
        }

        val byId = descriptors.associateBy { it.providerId }
        require(byId.size == descriptors.size) {
            "Browser provider IDs must be unique."
        }

        val originEntries = descriptors.flatMap { descriptor ->
            descriptor.allowedOrigins.map { origin ->
                checkNotNull(BrowserProviderBridgePolicy.normalizeHttpsOrigin(origin)) to descriptor
            }
        }
        val byOrigin = originEntries.toMap()
        require(byOrigin.size == originEntries.size) {
            "A browser provider origin can belong to only one provider."
        }

        descriptorsById = byId
        descriptorsByOrigin = byOrigin
    }

    fun get(providerId: String): BrowserProviderDescriptor? =
        descriptorsById[providerId]

    fun resolveForPage(pageUrl: String?): BrowserProviderDescriptor? {
        val origin = BrowserProviderBridgePolicy.normalizeHttpsOrigin(pageUrl) ?: return null
        return descriptorsByOrigin[origin]
    }
}
