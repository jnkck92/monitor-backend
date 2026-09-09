package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.service.AlarmTitleParser.TitleParts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmTitleParserTest {

    @Test
    void returnsNullPartsWhenTitleIsNull() {
        TitleParts result = AlarmTitleParser.parse(null);

        assertThat(result.keyword()).isNull();
        assertThat(result.description()).isNull();
    }

    @Test
    void splitsKeywordAndDescriptionOnSeparator() {
        TitleParts result = AlarmTitleParser.parse("F 01 - Kleinbrand");

        assertThat(result.keyword()).isEqualTo("F 01");
        assertThat(result.description()).isEqualTo("Kleinbrand");
    }

    @Test
    void trimsWhitespaceAroundKeywordAndDescription() {
        TitleParts result = AlarmTitleParser.parse("  F 01   -   Kleinbrand  ");

        assertThat(result.keyword()).isEqualTo("F 01");
        assertThat(result.description()).isEqualTo("Kleinbrand");
    }

    @Test
    void usesOnlyFirstSeparatorWhenMultipleOccur() {
        TitleParts result = AlarmTitleParser.parse("F 01 - Kleinbrand - Garage");

        assertThat(result.keyword()).isEqualTo("F 01");
        assertThat(result.description()).isEqualTo("Kleinbrand - Garage");
    }

    @Test
    void treatsWholeTitleAsKeywordWhenSeparatorMissing() {
        TitleParts result = AlarmTitleParser.parse("B2 Zimmerbrand");

        assertThat(result.keyword()).isEqualTo("B2 Zimmerbrand");
        assertThat(result.description()).isNull();
    }

    @Test
    void trimsKeywordWhenNoSeparatorPresent() {
        TitleParts result = AlarmTitleParser.parse("   B2 Zimmerbrand   ");

        assertThat(result.keyword()).isEqualTo("B2 Zimmerbrand");
        assertThat(result.description()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void returnsEmptyKeywordForBlankTitleWithoutSeparator(String title) {
        TitleParts result = AlarmTitleParser.parse(title);

        assertThat(result.keyword()).isEmpty();
        assertThat(result.description()).isNull();
    }

    @Test
    void handlesSeparatorAtStartOfTitle() {
        TitleParts result = AlarmTitleParser.parse(" - Kleinbrand");

        assertThat(result.keyword()).isEmpty();
        assertThat(result.description()).isEqualTo("Kleinbrand");
    }

    @Test
    void handlesSeparatorAtEndOfTitle() {
        TitleParts result = AlarmTitleParser.parse("F 01 - ");

        assertThat(result.keyword()).isEqualTo("F 01");
        assertThat(result.description()).isEmpty();
    }

    @Test
    void handlesTitleThatIsOnlySeparator() {
        TitleParts result = AlarmTitleParser.parse(" - ");

        assertThat(result.keyword()).isEmpty();
        assertThat(result.description()).isEmpty();
    }

    @Test
    void doesNotSplitOnPartialSeparatorMatch() {
        // z.B. Bindestrich ohne umgebende Leerzeichen darf nicht als Trenner erkannt werden
        TitleParts result = AlarmTitleParser.parse("F-01 Kleinbrand");

        assertThat(result.keyword()).isEqualTo("F-01 Kleinbrand");
        assertThat(result.description()).isNull();
    }
}