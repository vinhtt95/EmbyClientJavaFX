package com.example.embyapp.service;

import com.example.emby.EmbyClient.ApiException;
import com.example.emby.EmbyClient.Java.UserServiceApi;
import com.example.emby.modelEmby.UserDto;

/**
 * Repository (Kho chứa) chuyên xử lý các tác vụ liên quan đến dữ liệu User.
 * Tách biệt logic gọi API User ra khỏi EmbyService.
 */
public class UserRepository {

    private static UserRepository instance;
    private final EmbyService embyService; // Phụ thuộc vào EmbyService để lấy ApiClient và auth state

    /**
     * Constructor private để đảm bảo Singleton.
     */
    private UserRepository() {
        this.embyService = EmbyService.getInstance();
    }

    /**
     * Lấy instance Singleton của UserRepository.
     *
     * @return instance duy nhất của UserRepository.
     */
    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    /**
     * Lấy thông tin chi tiết của một UserDto bằng ID.
     * Đã bao gồm kiểm tra đăng nhập và xử lý lỗi.
     *
     * @param userId ID của user cần lấy thông tin.
     * @return UserDto nếu thành công, null nếu thất bại.
     */
    public UserDto getUserById(String userId) {
        if (userId == null || userId.isEmpty()) {
            System.err.println("UserRepository: UserId is null or empty.");
            return null;
        }

        // Kiểm tra trạng thái đăng nhập
        if (!embyService.isLoggedIn()) {
            System.err.println("UserRepository: Not logged in. Cannot get user.");
            return null;
        }

        try {
            UserServiceApi userService = embyService.getUserServiceApi();
            if (userService == null) {
                System.err.println("UserRepository: UserServiceApi is null (chưa đăng nhập hoặc lỗi).");
                return null;
            }

            // Gọi API
            return userService.getUsersById(userId);

        } catch (ApiException e) {
            System.err.println("API Exception in UserRepository.getUserById (Code: " + e.getCode() + "): " + e.getMessage());
            System.err.println("Response body: " + e.getResponseBody());
            e.printStackTrace();
            return null; // Trả về null khi có lỗi API
        } catch (Exception e) {
            System.err.println("Unexpected Exception in UserRepository.getUserById: " + e.getMessage());
            e.printStackTrace();
            return null; // Trả về null khi có lỗi ngoài lệ
        }
    }

    /**
     * Lấy UserDto của người dùng hiện tại đang đăng nhập.
     *
     * @return UserDto của người dùng hiện tại, hoặc null nếu chưa đăng nhập/lỗi.
     */
    public UserDto getCurrentUser() {
        if (!embyService.isLoggedIn() || embyService.getCurrentAuthResult() == null) {
            System.err.println("UserRepository: Not logged in. Cannot get current user.");
            return null;
        }

        UserDto user = embyService.getCurrentAuthResult().getUser();
        if (user != null) {
            return user;
        } else {
            // Trường hợp Edge case: AuthResult có nhưng UserDto bị null
            // Thử lấy lại qua ID (nếu có)
            System.err.println("UserRepository: UserDto is null in authResult, trying to fetch by ID...");
            String currentUserId = embyService.getCurrentUserId(); // Giả sử EmbyService có hàm này
            if(currentUserId != null) {
                return getUserById(currentUserId);
            }
        }

        System.err.println("UserRepository: Could not determine current user.");
        return null;
    }
}
