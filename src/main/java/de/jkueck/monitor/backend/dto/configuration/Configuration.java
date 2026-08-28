package de.jkueck.monitor.backend.dto.configuration;

import java.util.List;
import java.util.Map;

public record Configuration(

        String departmentName,

        List<Unit> persons,

        List<Unit> vehicles,

        List<String> defaultOrder,

        String commandContact,

        Map<String, Status> statuses,

        List<RuleGroup> ruleGroups

) {
}
