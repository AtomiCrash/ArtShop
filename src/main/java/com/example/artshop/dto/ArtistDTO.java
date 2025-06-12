package com.example.artshop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public class ArtistDTO {
    private Integer id;
    @Schema(description = "First name of the artist", example = "Vincent")
    @Size(max = 60, message = "First name must be 60 characters or less")
    private String firstName;
    @Schema(description = "Middle name of the artist", example = "Willem")
    @Size(max = 60, message = "Middle name must be 60 characters or less")
    private String middleName;
    @Schema(description = "Last name of the artist", example = "van Gogh", required = true)
    @Size(max = 60, message = "Last name must be 60 characters or less")
    private String lastName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @AssertTrue(message = "Artist must have at least first name or last name")
    public boolean isNameValid() {
        return (firstName != null && !firstName.trim().isEmpty()) ||
                (lastName != null && !lastName.trim().isEmpty());
    }
}
