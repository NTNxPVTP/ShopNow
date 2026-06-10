package com.example.shopnow.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.models.Category;
import com.example.shopnow.product.rest.dto.CategoryResponse;
import com.example.shopnow.product.rest.dto.CreateCategoryRequest;

/**
 * Unit test cho {@link CategoryService}.
 *
 * <p>Pure unit test theo convention của repo: JUnit 5 + Mockito + AssertJ, không
 * khởi động Spring context. Chỉ {@link CategoryRepository} được mock; phần logic
 * normalize tên (trim) và sắp xếp case-insensitive trong service được kiểm tra
 * thông qua tương tác và giá trị trả về.
 *
 * <p>Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    /**
     * Helper: set id cho entity dùng {@link ReflectionTestUtils} vì
     * {@code BaseEntity.id} là {@code @GeneratedValue} và không có persistence
     * layer chạy trong unit test.
     */
    private static Category categoryWithId(UUID id, String name) {
        Category c = new Category();
        c.setName(name);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    @Test
    @DisplayName("createCategory lưu category với tên đã trim và trả về response chứa id & name")
    void createCategory_savesTrimmedName_andReturnsResponse() {
        // Arrange
        UUID savedId = UUID.randomUUID();
        when(categoryRepository.existsByNameIgnoreCase("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category arg = invocation.getArgument(0);
                    ReflectionTestUtils.setField(arg, "id", savedId);
                    return arg;
                });

        // Act
        CategoryResponse response = categoryService.createCategory(new CreateCategoryRequest("Books"));

        // Assert: response mang id & name của entity đã lưu
        assertThat(response.id()).isEqualTo(savedId);
        assertThat(response.name()).isEqualTo("Books");

        // Assert: tên persist đúng giá trị đã trim (ở đây không có whitespace nên trim no-op)
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Books");
    }

    @Test
    @DisplayName("createCategory trim whitespace trước khi check tồn tại và lưu")
    void createCategory_trimsWhitespace_beforeExistenceCheckAndSave() {
        // Arrange: tên có khoảng trắng bao quanh
        UUID savedId = UUID.randomUUID();
        when(categoryRepository.existsByNameIgnoreCase("Books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category arg = invocation.getArgument(0);
                    ReflectionTestUtils.setField(arg, "id", savedId);
                    return arg;
                });

        // Act
        CategoryResponse response = categoryService.createCategory(new CreateCategoryRequest("  Books  "));

        // Assert: existence check phải dùng tên đã trim
        verify(categoryRepository).existsByNameIgnoreCase("Books");

        // Assert: entity persist mang tên đã trim
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Books");

        // Assert: response vọng đúng tên đã trim
        assertThat(response.name()).isEqualTo("Books");
        assertThat(response.id()).isEqualTo(savedId);
    }

    @Test
    @DisplayName("createCategory ném CATEGORY_ALREADY_EXISTS và KHÔNG save khi tên trùng (case-insensitive)")
    void createCategory_throwsCategoryAlreadyExists_andNeverSaves_whenDuplicate() {
        // Arrange: existence check trả true cho tên đã trim
        when(categoryRepository.existsByNameIgnoreCase("Books")).thenReturn(true);

        // Act + Assert: ném đúng DomainException với errorCode CATEGORY_ALREADY_EXISTS
        assertThatThrownBy(() -> categoryService.createCategory(new CreateCategoryRequest("  Books  ")))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_ALREADY_EXISTS);

        // Assert: không ghi DB
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("getCategories trả về list đã sort case-insensitive theo name")
    void getCategories_returnsCaseInsensitivelySortedList() {
        // Arrange: dữ liệu cố tình unsorted với hỗn hợp chữ hoa/thường
        Category banana = categoryWithId(UUID.randomUUID(), "banana");
        Category apple = categoryWithId(UUID.randomUUID(), "Apple");
        Category cherry = categoryWithId(UUID.randomUUID(), "cherry");
        when(categoryRepository.findAll()).thenReturn(List.of(banana, apple, cherry));

        // Act
        List<CategoryResponse> result = categoryService.getCategories();

        // Assert: thứ tự sau sort case-insensitive là Apple, banana, cherry
        assertThat(result)
                .extracting(CategoryResponse::name)
                .containsExactly("Apple", "banana", "cherry");

        // Assert: id được map đúng theo tên (đảm bảo không mất gốc dữ liệu)
        assertThat(result)
                .extracting(CategoryResponse::id)
                .containsExactly(apple.getId(), banana.getId(), cherry.getId());
    }

    @Test
    @DisplayName("getCategories trả về list rỗng khi repository không có category nào")
    void getCategories_returnsEmptyList_whenNoCategories() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of());

        // Act
        List<CategoryResponse> result = categoryService.getCategories();

        // Assert
        assertThat(result).isEmpty();
    }
}
