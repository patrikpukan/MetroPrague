import Testing

@testable import MetroPrague

@Suite("AppConfiguration")
struct AppConfigurationTests {
    @Test("A valid key is returned trimmed")
    func validKeyIsTrimmed() {
        let configuration = AppConfiguration(infoDictionary: ["GolemioAPIKey": "  abc123\n"])
        #expect(configuration.golemioAPIKey == "abc123")
    }

    @Test("A missing key resolves to nil")
    func missingKeyIsNil() {
        let configuration = AppConfiguration(infoDictionary: [:])
        #expect(configuration.golemioAPIKey == nil)
    }

    @Test(
        "Unusable values resolve to nil",
        arguments: ["", "   ", "\n", "$(GOLEMIO_API_KEY)"]
    )
    func unusableValueIsNil(value: String) {
        let configuration = AppConfiguration(infoDictionary: ["GolemioAPIKey": value])
        #expect(configuration.golemioAPIKey == nil)
    }

    @Test("A non-string value resolves to nil")
    func nonStringValueIsNil() {
        let configuration = AppConfiguration(infoDictionary: ["GolemioAPIKey": 42])
        #expect(configuration.golemioAPIKey == nil)
    }
}
