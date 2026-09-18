package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("dev")
public class MockDiveraApiClient implements DiveraClient {

    private final AtomicBoolean alarmActive = new AtomicBoolean(false);

    public void setAlarmActive(boolean active) {
        alarmActive.set(active);
    }

    public boolean isAlarmActive() {
        return alarmActive.get();
    }

    @Override
    public DiveraResponse pullAll(DiveraConfig diveraConfig) {
        if (!alarmActive.get()) {
            return new DiveraResponse(true, new DiveraResponse.Data(Map.of()));
        }
        return new DiveraResponse(
                true,
                new DiveraResponse.Data(
                        Map.of("123",
                                new AlarmResponse(
                                        123L, "H 052 - Türnotöffnung",
                                        "Brennt ein Wahlplakat an einer Laterne",
                                        "Kiepelbergstraße, 27721 Ritterhude Ritterhude",
                                        Instant.now().minus(10, ChronoUnit.MINUTES).getEpochSecond(),
                                        false,
                                        true
                                )
                        )
                )
        );
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig) {
        List<VehicleStatus> mockStatuses = List.of(
                new VehicleStatus(4716L, alarmActive.get() ? 4 : 2), // ELW
                new VehicleStatus(4714L, alarmActive.get() ? 3 : 2), // HLF
                new VehicleStatus(4715L, alarmActive.get() ? 3 : 2), // TLF
                new VehicleStatus(7185L, 2), // RW
                new VehicleStatus(7184L, 2), // SW
                new VehicleStatus(44882L, 2), // MTW
                new VehicleStatus(45764L, 2), // RTB
                new VehicleStatus(55884L, alarmActive.get() ? 4 : 2), // OBM
                new VehicleStatus(55885L, alarmActive.get() ? 4 : 2), // OBMV
                new VehicleStatus(55886L, alarmActive.get() ? 4 : 2), // ZF
                new VehicleStatus(86298L, alarmActive.get() ? 4 : 2) // ZFV
        );

        return new VehicleStatusGroupResponse(true, mockStatuses);
    }

    @Override
    public EventsResponse pullEvents(DiveraConfig diveraConfig) {
        return new EventsResponse(
                true,
                new EventsResponse.Data(
                        Map.of("1",
                                new EventResponse(
                                        1L, null, "Rechtsgrundlagen / Grundlagen Zivil- und KatS",
                                        "MGA 13.1 Grundlagen des Zivil- und Katastrophenschutzes",
                                        "Feuerwehrhaus",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, 0, null, false
                                ),
                                "2",
                                new EventResponse(
                                        2L, null, "Unterweisung UVV",
                                        "jährliche Unterweisung Unfallverhütungsvorschriften",
                                        "Feuerwehrhaus",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, 0, null, false
                                ),
                                "3",
                                new EventResponse(
                                        3L, null, "Fahrzeugreservierung MTW, TLF und Sprinter",
                                        "JF-DIENST Sportwettbewerb",
                                        "OHZ",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, 0, null, false
                                )
                        ),
                        List.of(1L, 2L, 3L)
                )
        );
    }

    @Override
    public NewsListResponse pullNews(DiveraConfig diveraConfig) {
        return new NewsListResponse(
                true,
                new NewsListResponse.Data(
                        Map.of("1",
                                new NewsResponse(
                                        1L, null, "Boot im Wasser",
                                        "Moin zusammen, \\nDas Boot liegt im Wasser. \\nDer Schlüssel zum Anleger und für das Boot wird auf dem Vito platziert, ebenso wie das Funkgerät. \\nDie Rettungswesten befinden sich im Holzschuppen des Vereins, vorne auf der Rechten Seite. Dort ist ein Spind, welcher mit einem der Schlüssel am Bootsschlüssel geöffnet werden kann. \\n\\nWenn das Boot in den Einsatz geht bitte folgendes beachten: \\n\\n- Persenning abnehmen\\n- Batterie Schlüssel an der Sitzbank (in Fahrtrichtung links) einstecken\\n- orangenes Ladekabel abziehen\\n- Luftschraube vom Kanister öffnen\\n- Schlüssel einstecken und auch den kleinen Clip am Notaus anstecken, sonst startet der Motor nicht\\n- unter Umständen Sprit über den Balk vor dem Motor vorpumpen\\n- das Boot ist mit ROTEN Feuerwehrleinen am Steck befestigt, alle lösen und los geht's",
                                        "Feuerwehrhaus Ritterhude",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, null, false
                                ),
                                "2",
                                new NewsResponse(
                                        1L, null, "Getränke",
                                        "Hallo zusammen, ich melde mich als Getränkewart. Die Getränke in unserer Halle beim SW sind für die Feuerwehren der Gemeinde gedacht, sie können sich dort je nach Bedarf was abholen. Das gleiche gilt auch für uns. \\\"Unsere\\\" Getränke sollen weiterhin wie gewohnt aus dem Getränkebunker entnommen und auch wieder reingestellt werden. Wir versuchen im Getränkebunker immer alles auf Stand zu halten, falls eine Kiste  leer ist unterstützt das Team und tauscht diese mit einer vom SW Lager durch. Es sollen also keine Getränke einfach so vom Lager beim SW entnommen werden, unser Weg ist über den Getränkebunker. Weiter ist das Powerrade für Einsätze gedacht bei denen wir hart arbeiten mussten. Also bitte auch nur im Einsatz verzehren. Vielen Dank \uD83D\uDE42",
                                        "Feuerwehrhaus Ritterhude",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, null, false
                                ),
                                "3",
                                new NewsResponse(
                                        1L, null, "Führungskräfte Fortbildung",
                                        "Hallo zusammen, ich würde eine Art Fortbildung für (angehende) Führungskräfte anbieten. Dabei möchte ich auf den Führungsvorgang, Hintergrundwissen und Vorgehen bei „Standardeinsätzen“, wie BMA, privater Rauchmelder, VU, Türnotöffnung eingehen und Wissen und Erfahrungen teilen. Gerade für die eingesetzten GF ohne Qualifikation eine Vorbereitung. Weitere Themenwünsche könnt ihr in der Freitextrückmeldung geben. \\n\\nWenn der Wunsch von eurer Seite besteht, wird im nächsten Schritt eine Terminabfrage erfolgen.",
                                        "Feuerwehrhaus Ritterhude",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, null, false
                                ),
                                "4",
                                new NewsResponse(
                                        1L, null, "Interesse an GF/ZF Schulung/FoBi",
                                        "",
                                        "Feuerwehrhaus Ritterhude",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, null, false
                                ),
                                "5",
                                new NewsResponse(
                                        1L, null, "Mini-Kreuzfahrt mit Color-Line nach Oslo",
                                        "Moin,\\nanbei die LFV Info zu den Mini-Kreuzfahrten mit Color-Line nach Oslo",
                                        "Feuerwehrhaus Ritterhude",
                                        Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(),
                                        true, null, false
                                )
                        ),

                        List.of(1L, 2L, 3L, 4L, 5L)
                )
        );
    }

}