package de.jkueck.monitor.backend.dto.response;

public record AlarmWebResponse(

        String title,

        String keyword,

        String description,

        String address,

        String label,

        String color,

        String hint

) {
}
