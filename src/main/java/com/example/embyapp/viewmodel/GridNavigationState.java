package com.example.embyapp.viewmodel;

import com.example.embyapp.viewmodel.detail.TagModel;
import java.util.Objects;

/**
 * Lớp POJO (Plain Old Java Object) lưu trữ một trạng thái điều hướng
 * của ItemGridView để phục vụ cho chức năng "Back".
 */
public class GridNavigationState {

    // (MỚI) Định nghĩa các loại trạng thái
    public enum StateType {
        FOLDER, // Đang xem một thư mục (dùng parentId)
        SEARCH, // Đang xem kết quả tìm kiếm (dùng searchKeywords)
        CHIP    // Đang xem kết quả click chip (dùng chipModel, chipType)
    }

    private final StateType type;
    private final String primaryParam; // Dùng cho parentId hoặc searchKeywords
    private final TagModel chipModel;  // Dùng cho CHIP
    private final String chipType;   // Dùng cho CHIP
    private final String sortBy;
    private final String sortOrder;
    private final int pageIndex;
    private final String selectedItemId; // ID của item đang được chọn

    /**
     * Constructor cho FOLDER và SEARCH
     */
    public GridNavigationState(StateType type, String primaryParam, String sortBy, String sortOrder, int pageIndex, String selectedItemId) {
        this.type = type;
        this.primaryParam = primaryParam;
        this.chipModel = null;
        this.chipType = null;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.pageIndex = pageIndex;
        this.selectedItemId = selectedItemId;
    }

    /**
     * Constructor cho CHIP
     */
    public GridNavigationState(StateType type, TagModel chipModel, String chipType, String sortBy, String sortOrder, int pageIndex, String selectedItemId) {
        this.type = type;
        this.primaryParam = null;
        this.chipModel = chipModel;
        this.chipType = chipType;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.pageIndex = pageIndex;
        this.selectedItemId = selectedItemId;
    }

    // Getters
    public StateType getType() { return type; }
    public String getPrimaryParam() { return primaryParam; }
    public TagModel getChipModel() { return chipModel; }
    public String getChipType() { return chipType; }
    public String getSortBy() { return sortBy; }
    public String getSortOrder() { return sortOrder; }
    public int getPageIndex() { return pageIndex; }
    public String getSelectedItemId() { return selectedItemId; }

    /**
     * Dùng để ngăn việc push các trạng thái y hệt nhau vào stack.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridNavigationState that = (GridNavigationState) o;
        return pageIndex == that.pageIndex &&
                type == that.type &&
                Objects.equals(primaryParam, that.primaryParam) &&
                Objects.equals(chipModel, that.chipModel) &&
                Objects.equals(chipType, that.chipType) &&
                Objects.equals(sortBy, that.sortBy) &&
                Objects.equals(sortOrder, that.sortOrder) &&
                Objects.equals(selectedItemId, that.selectedItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, primaryParam, chipModel, chipType, sortBy, sortOrder, pageIndex, selectedItemId);
    }
}