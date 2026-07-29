package dev.agiro.healthygation.domain;

import java.util.Date;

public record PatientBrief(java.util.List<String> list, String name, String family, String gender, Date birthDate) {
}
