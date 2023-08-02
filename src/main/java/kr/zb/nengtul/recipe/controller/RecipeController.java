package kr.zb.nengtul.recipe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.zb.nengtul.recipe.domain.constants.RecipeCategory;
import kr.zb.nengtul.recipe.domain.dto.RecipeAddDto;
import kr.zb.nengtul.recipe.domain.dto.RecipeUpdateDto;
import kr.zb.nengtul.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Tag(name = "RECIPE API", description = "레시피 API")
@RestController
@RequestMapping("/v1/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @Operation(summary = "레시피 작성", description = "토큰을 통해 유저 여부 확인 후 레시피를 작성합니다.")
    @PostMapping
    ResponseEntity<?> addRecipe(Principal principal,
                                @RequestPart @Valid RecipeAddDto recipeAddDto,
                                @RequestPart List<MultipartFile> images,
                                @RequestPart MultipartFile thumbnail) {

        recipeService.addRecipe(principal, recipeAddDto, images, thumbnail);

        return ResponseEntity.ok(null);
    }

    @Operation(summary = "레시피 모두 조회", description = "토큰을 통해 유저 여부 확인 후 조회합니다.")
    @GetMapping
    ResponseEntity<?> getAllRecipe(Pageable pageable) {

        return ResponseEntity.ok(recipeService.getAllRecipe(pageable));
    }

    @Operation(summary = "레시피 상세 조회", description = "토큰을 통해 유저 여부 확인 후 RecipeId를 받아 레시피 상세를 조회합니다.")
    @GetMapping("/detail/{recipeId}")
    ResponseEntity<?> getRecipeById(@PathVariable String recipeId) {

        return ResponseEntity.ok(recipeService.getRecipeDetailById(recipeId));
    }

    @Operation(summary = "레시피 카테고리별 조회", description = "토큰을 통해 유저 여부 확인 후 카테고리별로 조회합니다.")
    @GetMapping("/category/{category}")
    ResponseEntity<?> getRecipeByCategory(@PathVariable RecipeCategory category,
                                          Pageable pageable) {

        return ResponseEntity.ok(recipeService.getRecipeByCategory(category, pageable));
    }

    @Operation(summary = "레시피 제목 조회", description = "토큰을 통해 유저 여부 확인 후 제목으로 조회합니다.")
    @GetMapping("/title/{title}")
    ResponseEntity<?> getRecipeByTitle(@PathVariable String title,
                                       Pageable pageable) {

        return ResponseEntity.ok(recipeService.getRecipeByTitle(title, pageable));
    }

    @Operation(summary = "레시피 재료 조회", description = "토큰을 통해 유저 여부 확인 후 재료로 조회합니다.")
    @GetMapping("/ingredient/{ingredient}")
    ResponseEntity<?> getRecipeByIngredient(@PathVariable String ingredient,
                                       Pageable pageable) {

        return ResponseEntity.ok(recipeService.getRecipeByIngredient(ingredient, pageable));
    }

    @Operation(summary = "레시피 수정", description = "토큰을 통해 유저 여부 확인 후 레시피를 수정합니다.")
    @PutMapping("/{recipeId}")
    ResponseEntity<?> updateRecipe(Principal principal,
                                   @PathVariable String recipeId,
                                   @RequestPart RecipeUpdateDto recipeUpdateDto,
                                   @RequestPart List<MultipartFile> images,
                                   @RequestPart MultipartFile thumbnail) {

        recipeService.updateRecipe(principal, recipeId, recipeUpdateDto,
                images, thumbnail);

        return ResponseEntity.ok(null);
    }

    @Operation(summary = "레시피 수정", description = "토큰을 통해 유저 여부 확인 후 레시피를 삭제합니다.")
    @DeleteMapping("/{recipeId}")
    ResponseEntity<?> deleteRecipe(Principal principal, @PathVariable String recipeId) {

        recipeService.deleteRecipe(principal, recipeId);

        return ResponseEntity.ok(null);
    }

}
