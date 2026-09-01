package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Rule;
import de.jkueck.monitor.backend.dto.configuration.RuleGroup;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KeywordMatcherTest {

    private KeywordMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new KeywordMatcher();
    }

    private AlarmResponse alarm(String title) {
        return new AlarmResponse(1L, title, null, null, null, false, false);
    }

    private Rule rule(String label, List<String> keywords) {
        return new Rule(label, keywords, List.of(), null, null, null);
    }

    private Rule rule(String label, List<String> keywords, MatchMode mode) {
        return new Rule(label, keywords, List.of(),null, mode, null);
    }

    private RuleGroup group(String category, String label, List<Rule> rules) {
        return new RuleGroup(category, label, "#000000", rules);
    }

    private Configuration config(List<RuleGroup> ruleGroups) {
        return new Configuration("Test-Wehr", List.of(), List.of(), List.of(), "ELW", Map.of(), ruleGroups);
    }

    @Nested
    @DisplayName("matchRuleGroup()")
    class MatchRuleGroupTests {

        @Test
        @DisplayName("findet die richtige RuleGroup anhand des Keywords")
        void shouldMatchCorrectRuleGroup() {
            RuleGroup brandGroup = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));
            RuleGroup hilfeGroup = group("H", "Hilfeleistung", List.of(
                    rule("Hilfe klein", List.of("H01"))
            ));
            Configuration cfg = config(List.of(brandGroup, hilfeGroup));

            Optional<RuleGroup> result = matcher.matchRuleGroup(alarm("F01 Feuer Musterstraße"), cfg);

            assertTrue(result.isPresent());
            assertEquals("F", result.get().category());
        }

        @Test
        @DisplayName("findet die zweite RuleGroup wenn erste nicht matcht")
        void shouldMatchSecondGroup() {
            RuleGroup brandGroup = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));
            RuleGroup hilfeGroup = group("H", "Hilfeleistung", List.of(
                    rule("Hilfe klein", List.of("H01"))
            ));
            Configuration cfg = config(List.of(brandGroup, hilfeGroup));

            Optional<RuleGroup> result = matcher.matchRuleGroup(alarm("H01 Tür klemmt"), cfg);

            assertTrue(result.isPresent());
            assertEquals("H", result.get().category());
        }

        @Test
        @DisplayName("gibt empty zurück wenn kein Keyword matcht")
        void shouldReturnEmptyWhenNoMatch() {
            RuleGroup brandGroup = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));
            Configuration cfg = config(List.of(brandGroup));

            Optional<RuleGroup> result = matcher.matchRuleGroup(alarm("XYZ Unbekannt"), cfg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("gibt empty zurück bei null Titel")
        void shouldReturnEmptyForNullTitle() {
            RuleGroup brandGroup = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));
            Configuration cfg = config(List.of(brandGroup));

            Optional<RuleGroup> result = matcher.matchRuleGroup(alarm(null), cfg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("gibt empty zurück bei leerer RuleGroups-Liste")
        void shouldReturnEmptyForEmptyRuleGroups() {
            Configuration cfg = config(List.of());

            Optional<RuleGroup> result = matcher.matchRuleGroup(alarm("F01 Feuer"), cfg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("gibt empty zurück wenn RuleGroup keine Rules hat")
        void shouldReturnEmptyForGroupWithoutRules() {
            RuleGroup emptyGroup = group("F", "Brand", List.of());
            Configuration cfg = config(List.of(emptyGroup));

            Optional<RuleGroup> result = matcher.matchRuleGroup(alarm("F01 Feuer"), cfg);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("matchRule() – CONTAINS (default)")
    class ContainsTests {

        @Test
        @DisplayName("matcht wenn Keyword im Titel enthalten ist")
        void shouldMatchSubstring() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer Musterstraße"), rg);

            assertTrue(result.isPresent());
            assertEquals("Kleinbrand", result.get().label());
        }

        @Test
        @DisplayName("matcht auch wenn Keyword mitten im Titel steht")
        void shouldMatchMiddleOfTitle() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("Alarm F01 Musterstraße"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("matcht NICHT bei case-mismatch (case-sensitiv)")
        void shouldNotMatchDifferentCase() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("f01 feuer"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matcht mit zweitem Keyword wenn erstes nicht passt")
        void shouldMatchSecondKeyword() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01", "F 01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F 01 Feuer"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("gibt empty zurück bei null Titel")
        void shouldReturnEmptyForNullTitle() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm(null), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("gibt empty zurück bei leerer Keywords-Liste")
        void shouldReturnEmptyForEmptyKeywords() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of())
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("gibt empty zurück bei leerer Rules-Liste")
        void shouldReturnEmptyForEmptyRules() {
            RuleGroup rg = group("F", "Brand", List.of());

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("matchRule() – Longest Match / Spezifität")
    class LongestMatchTests {

        @Test
        @DisplayName("F012 matcht spezifischere Rule F012 statt F01")
        void shouldPreferF012OverF01() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01", "F 01")),
                    rule("PKW-Brand", List.of("F012", "F 012"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F012 PKW-Brand Musterstraße"), rg);

            assertTrue(result.isPresent());
            assertEquals("PKW-Brand", result.get().label());
        }

        @Test
        @DisplayName("F012 matcht spezifischere Rule auch wenn sie NACH F01 steht")
        void shouldPreferLongerMatchRegardlessOfOrder() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01")),           // kürzer, steht zuerst
                    rule("PKW-Brand", List.of("F012"))            // länger, steht danach
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F012 PKW"), rg);

            assertTrue(result.isPresent());
            assertEquals("PKW-Brand", result.get().label());
        }

        @Test
        @DisplayName("F01 matcht korrekt wenn nur F01 im Titel steht")
        void shouldMatchF01WhenNoLongerKeywordMatches() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01")),
                    rule("PKW-Brand", List.of("F012"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isPresent());
            assertEquals("Kleinbrand", result.get().label());
        }

        @Test
        @DisplayName("H 021 matcht H021 statt H01 (Leerzeichen-Variante)")
        void shouldPreferH021OverH01WithSpaces() {
            RuleGroup rg = group("H", "Hilfe", List.of(
                    rule("Hilfe klein", List.of("H01", "H 01")),
                    rule("Ölspur", List.of("H021", "H 021"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("H 021 Ölspur Musterstraße"), rg);

            assertTrue(result.isPresent());
            assertEquals("Ölspur", result.get().label());
        }

        @Test
        @DisplayName("H 071 matcht H071 statt H01")
        void shouldPreferH071OverH01() {
            RuleGroup rg = group("H", "Hilfe", List.of(
                    rule("Hilfe klein", List.of("H01", "H 01")),
                    rule("Person im Wasser", List.of("H071", "H 071"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("H 071 Person im Wasser"), rg);

            assertTrue(result.isPresent());
            assertEquals("Person im Wasser", result.get().label());
        }

        @Test
        @DisplayName("CBRN011 matcht spezifischer als CBRN01")
        void shouldPreferCBRN011OverCBRN01() {
            RuleGroup rg = group("CBRN", "Gefahrgut", List.of(
                    rule("Gefahrgut Messeinsatz", List.of("CBRN011", "CBRN 011")),
                    rule("Gefahrgut klein", List.of("CBRN01", "CBRN 01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("CBRN011 Messung"), rg);

            assertTrue(result.isPresent());
            assertEquals("Gefahrgut Messeinsatz", result.get().label());
        }

        @Test
        @DisplayName("bei gleicher Keyword-Länge gewinnt irgendeine (beide valide)")
        void shouldMatchOneWhenKeywordLengthIsEqual() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Rule A", List.of("ABC")),
                    rule("Rule B", List.of("ABC"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("ABC Einsatz"), rg);

            assertTrue(result.isPresent());
            // Beide wären korrekt – Hauptsache es crasht nicht
        }

        @Test
        @DisplayName("S011 matcht spezifischer als S01")
        void shouldPreferS011OverS01() {
            RuleGroup rg = group("S", "Unterstützung", List.of(
                    rule("Unterstützung RD", List.of("S011", "S 011")),
                    rule("Unterstützung", List.of("S01", "S 01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("S011 Tragehilfe"), rg);

            assertTrue(result.isPresent());
            assertEquals("Unterstützung RD", result.get().label());
        }
    }

    @Nested
    @DisplayName("matchRule() – EXACT")
    class ExactTests {

        @Test
        @DisplayName("matcht bei exakter Übereinstimmung")
        void shouldMatchExact() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.EXACT)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("matcht case-insensitiv")
        void shouldMatchCaseInsensitive() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.EXACT)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("f01"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("matcht NICHT wenn Titel mehr enthält")
        void shouldNotMatchWhenTitleHasMore() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.EXACT)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer Musterstraße"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("matcht NICHT bei Substring")
        void shouldNotMatchSubstring() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.EXACT)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F012"), rg);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("matchRule() – STARTS_WITH")
    class StartsWithTests {

        @Test
        @DisplayName("matcht wenn Titel mit Keyword beginnt")
        void shouldMatchPrefix() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.STARTS_WITH)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("matcht NICHT wenn Keyword mitten im Titel steht")
        void shouldNotMatchMiddle() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.STARTS_WITH)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("Alarm F01 Feuer"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("STARTS_WITH ist case-sensitiv")
        void shouldBeCaseSensitive() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.STARTS_WITH)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("f01 feuer"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("longest match funktioniert auch mit STARTS_WITH")
        void shouldPreferLongerStartsWith() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"), MatchMode.STARTS_WITH),
                    rule("PKW-Brand", List.of("F012"), MatchMode.STARTS_WITH)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F012 PKW"), rg);

            assertTrue(result.isPresent());
            assertEquals("PKW-Brand", result.get().label());
        }
    }

    @Nested
    @DisplayName("matchRule() – REGEX")
    class RegexTests {

        @Test
        @DisplayName("matcht mit einfacher Regex")
        void shouldMatchSimpleRegex() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Brand Stufe 2-5", List.of("F0[2-5]"), MatchMode.REGEX)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F04 Großbrand"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("matcht NICHT wenn Regex nicht passt")
        void shouldNotMatchWhenRegexDoesNotMatch() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Brand Stufe 2-5", List.of("F0[2-5]"), MatchMode.REGEX)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Kleinbrand"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Word-Boundary verhindert Substring-Match")
        void shouldRespectWordBoundary() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("\\bF01\\b"), MatchMode.REGEX)
            ));

            // F01 als ganzes Wort → Match
            assertTrue(matcher.matchRule(alarm("F01 Feuer"), rg).isPresent());

            // F012 → kein Match wegen Word-Boundary
            assertTrue(matcher.matchRule(alarm("F012 PKW"), rg).isEmpty());
        }

        @Test
        @DisplayName("ungültige Regex gibt empty zurück, kein Crash")
        void shouldNotCrashOnInvalidRegex() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kaputt", List.of("[ungültig"), MatchMode.REGEX)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("leere Regex matcht alles")
        void shouldMatchEverythingWithEmptyRegex() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Alles", List.of(".*"), MatchMode.REGEX)
            ));

            Optional<Rule> result = matcher.matchRule(alarm("Irgendwas"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Regex mit Gruppen funktioniert")
        void shouldWorkWithCapturingGroups() {
            RuleGroup rg = group("H", "Hilfe", List.of(
                    rule("Wasser", List.of("H07[12]"), MatchMode.REGEX)
            ));

            assertTrue(matcher.matchRule(alarm("H071 Person im Wasser"), rg).isPresent());
            assertTrue(matcher.matchRule(alarm("H072 Boot"), rg).isPresent());
            assertTrue(matcher.matchRule(alarm("H073 Anderes"), rg).isEmpty());
        }
    }

    @Nested
    @DisplayName("matchRule() – gemischte MatchModes in einer RuleGroup")
    class MixedModeTests {

        @Test
        @DisplayName("Rules mit unterschiedlichen MatchModes in einer Gruppe")
        void shouldHandleMixedModes() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Exact Match", List.of("F01"), MatchMode.EXACT),
                    rule("Contains Match", List.of("F01"), MatchMode.CONTAINS)
            ));

            // "F01 Feuer" → EXACT matcht nicht, CONTAINS matcht
            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isPresent());
            assertEquals("Contains Match", result.get().label());
        }

        @Test
        @DisplayName("REGEX und CONTAINS in einer Gruppe – longest match entscheidet")
        void shouldPreferLongerMatchAcrossModes() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Catch-All Brand", List.of("F0[1-9]"), MatchMode.REGEX),      // Keyword-Länge: 6
                    rule("Spezifisch F012", List.of("F012"), MatchMode.CONTAINS)        // Keyword-Länge: 4
            ));

            Optional<Rule> result = matcher.matchRule(alarm("F012 PKW-Brand"), rg);

            assertTrue(result.isPresent());
            // Regex-Keyword "F0[1-9]" hat Länge 6, "F012" hat Länge 4
            // → Regex gewinnt wegen längerer Keyword-String-Länge
            assertEquals("Catch-All Brand", result.get().label());
        }

        @Test
        @DisplayName("null matchMode wird als CONTAINS behandelt")
        void shouldTreatNullModeAsContains() {
            Rule ruleWithNull = new Rule("Nullmode", List.of("F01"), List.of(), null, null, null);
            RuleGroup rg = group("F", "Brand", List.of(ruleWithNull));

            Optional<Rule> result = matcher.matchRule(alarm("F01 Feuer"), rg);

            assertTrue(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("leerer Titel matcht nicht bei CONTAINS")
        void shouldNotMatchEmptyTitleContains() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm(""), rg);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("leerer Titel matcht leeres Keyword bei EXACT")
        void shouldMatchEmptyTitleWithEmptyKeywordExact() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Leer", List.of(""), MatchMode.EXACT)
            ));

            Optional<Rule> result = matcher.matchRule(alarm(""), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Keyword mit Sonderzeichen im CONTAINS-Modus wird literal behandelt")
        void shouldTreatSpecialCharsLiterallyInContains() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Sonder", List.of("F.01"))
            ));

            // "F.01" literal → matcht nur "F.01", nicht "FA01"
            assertTrue(matcher.matchRule(alarm("F.01 Feuer"), rg).isPresent());
            assertTrue(matcher.matchRule(alarm("FA01 Feuer"), rg).isEmpty());
        }

        @Test
        @DisplayName("sehr langer Titel funktioniert")
        void shouldHandleLongTitle() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            String longTitle = "F01 " + "A".repeat(10000);
            Optional<Rule> result = matcher.matchRule(alarm(longTitle), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("Keyword am Ende des Titels")
        void shouldMatchKeywordAtEnd() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("Alarm Stichwort F01"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("mehrere Rules matchen – spezifischste gewinnt unabhängig von Reihenfolge")
        void shouldAlwaysPreferMostSpecific() {
            // Absichtlich falsche Reihenfolge: allgemein → spezifisch
            RuleGroup rg = group("H", "Hilfe", List.of(
                    rule("Hilfe klein", List.of("H01", "H 01")),
                    rule("Hilfe mittel", List.of("H02", "H 02")),
                    rule("Ölspur", List.of("H021", "H 021")),
                    rule("Drehleiter", List.of("H022", "H 022"))
            ));

            assertEquals("Ölspur", matcher.matchRule(alarm("H 021 Ölspur"), rg).get().label());
            assertEquals("Drehleiter", matcher.matchRule(alarm("H 022 Drehleiter"), rg).get().label());
            assertEquals("Hilfe mittel", matcher.matchRule(alarm("H 02 Sturm"), rg).get().label());
            assertEquals("Hilfe klein", matcher.matchRule(alarm("H 01 Tür"), rg).get().label());
        }

        @Test
        @DisplayName("Titel enthält mehrere Stichworte – spezifischstes gewinnt")
        void shouldHandleTitleWithMultipleKeywords() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01")),
                    rule("Mittelbrand", List.of("F02")),
                    rule("BMA", List.of("F021"))
            ));

            // Titel enthält sowohl "F02" als auch "F021"
            Optional<Rule> result = matcher.matchRule(alarm("F021 BMA ausgelöst"), rg);

            assertTrue(result.isPresent());
            assertEquals("BMA", result.get().label());
        }

        @Test
        @DisplayName("nur ein Keyword von mehreren matcht – reicht aus")
        void shouldMatchIfOnlyOneKeywordMatches() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01", "F 01", "BRAND01"))
            ));

            // Nur das dritte Keyword matcht
            Optional<Rule> result = matcher.matchRule(alarm("BRAND01 Scheune"), rg);

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("kein Keyword matcht → empty")
        void shouldReturnEmptyWhenNothingMatches() {
            RuleGroup rg = group("F", "Brand", List.of(
                    rule("Kleinbrand", List.of("F01")),
                    rule("Mittelbrand", List.of("F02"))
            ));

            Optional<Rule> result = matcher.matchRule(alarm("H01 Hilfeleistung"), rg);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Realistische Szenarien")
    class RealisticTests {

        private RuleGroup hGruppe;

        @BeforeEach
        void setUp() {
            hGruppe = group("H", "Hilfeleistung", List.of(
                    rule("Hilfeleistung klein", List.of("H01", "H 01")),
                    rule("Hilfeleistung Ölspur", List.of("H021", "H 021")),
                    rule("Hilfeleistung Drehleiter", List.of("H022", "H 022")),
                    rule("Hilfeleistung mittel", List.of("H02", "H 02")),
                    rule("Hilfeleistung groß", List.of("H03", "H 03")),
                    rule("Hilfeleistung Tiernotlage", List.of("H04", "H 04")),
                    rule("Hilfeleistung VU eingeklemmt", List.of("H051", "H 051")),
                    rule("Hilfeleistung Türnotöffnung", List.of("H052", "H 052")),
                    rule("Hilfeleistung Personenschaden", List.of("H05", "H 05")),
                    rule("Hilfeleistung Person im Wasser", List.of("H071", "H 071")),
                    rule("Hilfeleistung Bootsunfall", List.of("H072", "H 072")),
                    rule("Hilfeleistung Wasserrettung", List.of("H07", "H 07")),
                    rule("Hilfeleistung verschüttet", List.of("H081", "H 081")),
                    rule("Hilfeleistung Höhenrettung", List.of("H08", "H 08"))
            ));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "H 021 Ölspur B-Straße",
                "H021 Ölspur Musterstraße"
        })
        @DisplayName("Ölspur wird korrekt erkannt, nicht als H01")
        void shouldMatchOelspurCorrectly(String title) {
            Optional<Rule> result = matcher.matchRule(alarm(title), hGruppe);

            assertTrue(result.isPresent());
            assertEquals("Hilfeleistung Ölspur", result.get().label());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "H 071 Person im Wasser Flussufer",
                "H071 Person im Wasser"
        })
        @DisplayName("Person im Wasser wird korrekt erkannt, nicht als H01")
        void shouldMatchPersonImWasserCorrectly(String title) {
            Optional<Rule> result = matcher.matchRule(alarm(title), hGruppe);

            assertTrue(result.isPresent());
            assertEquals("Hilfeleistung Person im Wasser", result.get().label());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "H 01 Baum auf Straße",
                "H01 Wasserschaden Keller"
        })
        @DisplayName("H01 matcht nur wenn wirklich H01 gemeint ist")
        void shouldMatchH01WhenIntended(String title) {
            Optional<Rule> result = matcher.matchRule(alarm(title), hGruppe);

            assertTrue(result.isPresent());
            assertEquals("Hilfeleistung klein", result.get().label());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "H 051 VU eingeklemmt A1",
                "H051 VU eingeklemmte Person"
        })
        @DisplayName("VU eingeklemmt wird korrekt erkannt, nicht als H05")
        void shouldMatchVUEingeklemmtCorrectly(String title) {
            Optional<Rule> result = matcher.matchRule(alarm(title), hGruppe);

            assertTrue(result.isPresent());
            assertEquals("Hilfeleistung VU eingeklemmt", result.get().label());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "H 052 Türnotöffnung Musterweg",
                "H052 Wohnungstür"
        })
        @DisplayName("Türnotöffnung wird korrekt erkannt, nicht als H05")
        void shouldMatchTuernotoeffnungCorrectly(String title) {
            Optional<Rule> result = matcher.matchRule(alarm(title), hGruppe);

            assertTrue(result.isPresent());
            assertEquals("Hilfeleistung Türnotöffnung", result.get().label());
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "H 081 Person verschüttet Baugrube",
                "H081 Kanalarbeiter verschüttet"
        })
        @DisplayName("Verschüttet wird korrekt erkannt, nicht als H08")
        void shouldMatchVerschuettetCorrectly(String title) {
            Optional<Rule> result = matcher.matchRule(alarm(title), hGruppe);

            assertTrue(result.isPresent());
            assertEquals("Hilfeleistung verschüttet", result.get().label());
        }

        @Test
        @DisplayName("komplett unbekanntes Stichwort matcht nichts")
        void shouldNotMatchUnknownKeyword() {
            Optional<Rule> result = matcher.matchRule(alarm("Z99 Unbekannt"), hGruppe);

            assertTrue(result.isEmpty());
        }
    }
}