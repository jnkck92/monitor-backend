package de.jkueck.monitor.backend.dto.response;

public record AlarmWebResponse(

        String title,

        String address,

        String label,

        String color,

        String hint

) {
}
