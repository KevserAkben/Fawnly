package com.fawnly.dto.finding;

import jakarta.validation.constraints.Size;

public class UpdateNoteRequest {

    @Size(max = 2000, message = "Note must be at most 2000 characters")
    private String note;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
