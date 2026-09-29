package com.fitnessplatform.program;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProgramRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        String description
) {
}
