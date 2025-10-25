package com.example.embyapp.viewmodel.detail;

import embyclient.model.BaseItemDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Model đơn giản hóa cho các đối tượng gợi ý như Genre, Studio, People.
 */
public class SuggestionItemModel {

    private final String id;
    private final String name;
    private final String type;
    private final Map<String, String> imageTags;
    private final List<String> backdropImageTags;

    public SuggestionItemModel(BaseItemDto dto) {
        this.id = dto.getId();
        this.name = dto.getName();
        this.type = dto.getType();
        this.imageTags = dto.getImageTags() != null ? dto.getImageTags() : Collections.emptyMap();
        this.backdropImageTags = dto.getBackdropImageTags() != null ? dto.getBackdropImageTags() : Collections.emptyList();
    }

    public static List<SuggestionItemModel> fromBaseItemDtoList(List<BaseItemDto> dtoList) {
        if (dtoList == null) return Collections.emptyList();
        return dtoList.stream()
                .filter(Objects::nonNull)
                .map(SuggestionItemModel::new)
                .collect(Collectors.toList());
    }

    public String getName() { return name; }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuggestionItemModel that = (SuggestionItemModel) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type);
    }
}