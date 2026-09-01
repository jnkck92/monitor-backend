package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Rule;
import de.jkueck.monitor.backend.dto.configuration.RuleGroup;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Component
public class KeywordMatcher {

    public Optional<RuleGroup> matchRuleGroup(AlarmResponse alarm, Configuration configuration) {
        if (alarm.title() == null) {
            return Optional.empty();
        }
        return configuration.ruleGroups().stream()
                .filter(group -> group.rules().stream()
                        .anyMatch(rule -> matchesAnyKeyword(alarm.title(), rule)))
                .findFirst();
    }

    public Optional<Rule> matchRule(AlarmResponse alarm, RuleGroup ruleGroup) {
        if (alarm.title() == null) {
            return Optional.empty();
        }
        return ruleGroup.rules().stream()
                .filter(rule -> matchesAnyKeyword(alarm.title(), rule))
                .max(Comparator.comparingInt(rule -> longestMatchingKeyword(alarm.title(), rule)));
    }

    private boolean matchesAnyKeyword(String title, Rule rule) {
        MatchMode mode = resolveMode(rule);
        return rule.keywords().stream().anyMatch(kw -> matches(title, kw, mode));
    }

    private int longestMatchingKeyword(String title, Rule rule) {
        MatchMode mode = resolveMode(rule);
        return rule.keywords().stream()
                .filter(kw -> matches(title, kw, mode))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    private MatchMode resolveMode(Rule rule) {
        return rule.matchMode() != null ? rule.matchMode() : MatchMode.CONTAINS;
    }

    private boolean matches(String title, String keyword, MatchMode mode) {
        return switch (mode) {
            case CONTAINS -> title.contains(keyword);
            case EXACT -> title.equalsIgnoreCase(keyword);
            case STARTS_WITH -> title.startsWith(keyword);
            case REGEX -> {
                try {
                    yield Pattern.compile(keyword).matcher(title).find();
                } catch (java.util.regex.PatternSyntaxException e) {
                    log.warn("Invalid regex keyword '{}': {}", keyword, e.getMessage());
                    yield false;
                }
            }
        };
    }

}
