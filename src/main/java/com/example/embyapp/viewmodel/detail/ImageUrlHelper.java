package com.example.embyapp.viewmodel.detail;

import com.example.emby.modelEmby.ImageInfo;
import com.example.emby.modelEmby.ImageType;

public class ImageUrlHelper {

    /**
     * Tạo URL hình ảnh từ ImageInfo (chủ yếu cho Backdrops dùng ImageIndex).
     */
    public static String getImageUrl(String serverUrl, String itemId, ImageInfo imageInfo, int maxWidth) {
        if (imageInfo == null || imageInfo.getImageType() == null || serverUrl == null || itemId == null) {
            return null;
        }

        String imageType = imageInfo.getImageType().getValue();
        Integer index = imageInfo.getImageIndex();

        // Chỉ xử lý các ảnh có ImageIndex (như Backdrop)
        if (index != null) {
            return String.format("%s/Items/%s/Images/%s/%d?maxWidth=%d&quality=90",
                    serverUrl, itemId, imageType, index, maxWidth);
        }

        // Không xử lý ảnh dùng Tag (như Primary) ở đây nữa
        return null;
    }
}