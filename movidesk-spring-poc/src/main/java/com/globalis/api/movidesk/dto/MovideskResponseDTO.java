package com.globalis.api.movidesk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MovideskResponseDTO(
    Integer id,
    String subject,
    String category,
    String status,
    String baseStatus,
    PersonDTO owner,
    PersonDTO createdBy,
    List<ClientDTO> clients,
    String serviceFirstLevel,
    String serviceSecondLevel,
    String serviceThirdLevel,
    String createdDate,
    String resolvedIn,
    Integer actionCount,
    Integer lifetimeWorkingTime,
    List<ActionDTO> actions,
    List<CustomFieldValueDTO> customFieldValues,
    List<String> tags
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonDTO(String id, Integer personType, Integer profileType, String businessName, String name, String email, String phone) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClientDTO(String id, Integer personType, Integer profileType, String businessName, String name, String email, String phone, String reference) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActionDTO(Integer id, Integer type, Integer origin, String description, PersonDTO createdBy, String createdDate, String status, String justification) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomFieldValueDTO(Integer customFieldId, List<CustomFieldItemDTO> items, String value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomFieldItemDTO(String customFieldItem) {}
}
