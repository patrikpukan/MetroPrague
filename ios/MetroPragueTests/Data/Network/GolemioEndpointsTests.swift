import Foundation
import Testing

@testable import MetroPrague

@Suite("GolemioEndpoints")
struct GolemioEndpointsTests {
    @Test("The board URL sends the station's metro stop IDs and the board parameters")
    func boardURL() {
        #expect(
            GolemioEndpoints.departureBoard(stopIDs: MetroStopIDs.ids(for: "andel")).absoluteString
                == "https://api.golemio.cz/v2/pid/departureboards"
                + "?ids%5B%5D=U1040Z101P&ids%5B%5D=U1040Z102P"
                + "&minutesAfter=60&limit=30&mode=departures&order=real")
    }

    @Test("Catalogue URLs")
    func catalogueURLs() {
        #expect(
            GolemioEndpoints.routes().absoluteString
                == "https://api.golemio.cz/v2/gtfs/routes?limit=10000")
        #expect(
            GolemioEndpoints.stops(names: ["Nemocnice Motol", "Petřiny"]).absoluteString
                == "https://api.golemio.cz/v2/gtfs/stops"
                + "?names%5B%5D=Nemocnice+Motol&names%5B%5D=Pet%C5%99iny&limit=10000")
        #expect(
            GolemioEndpoints.trips(stopID: "U400Z121P", offset: 1000).absoluteString
                == "https://api.golemio.cz/v2/gtfs/trips?stopId=U400Z121P&limit=1000&offset=1000")
        #expect(
            GolemioEndpoints.trip(id: "991_1/2").absoluteString
                == "https://api.golemio.cz/v2/gtfs/trips/991_1%2F2?includeStops=true&includeStopTimes=true"
        )
    }
}
