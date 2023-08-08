package kr.zb.nengtul.recipe.domain.entity;


import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import kr.zb.nengtul.recipe.domain.constants.RecipeCategory;
import kr.zb.nengtul.recipe.domain.dto.RecipeUpdateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Document(indexName = "recipe")
@Setting(settingPath = "elasticsearch/elasticsearch-settings.json")
@Mapping(mappingPath = "elasticsearch/recipe-mappings.json")
public class RecipeDocument {

  @Id
  @Field(type = FieldType.Keyword)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  @Field(type = FieldType.Keyword)
  private Long userId;

  @Field(type = FieldType.Text)
  private String title;

  @Field(type = FieldType.Keyword, index = false)
  private String intro;

  @Field(type = FieldType.Text)
  private String ingredient;

  @Field(type = FieldType.Keyword, index = false)
  private String cookingStep;

  @Field(type = FieldType.Keyword, index = false)
  private String thumbnailUrl;

  @Field(type = FieldType.Keyword, index = false)
  private String imageUrl;

  @Field(type = FieldType.Keyword)
  private String cookingTime;

  @Field(type = FieldType.Keyword)
  private String serving;

  @Field(type = FieldType.Keyword)
  private RecipeCategory category;

  @Field(type = FieldType.Keyword, index = false)
  private String videoUrl;

  @Field(type = FieldType.Integer)
  private Long viewCount;

  @Field(type = FieldType.Date, format = DateFormat.date_hour_minute)
  private LocalDateTime createdAt;

  @Field(type = FieldType.Date, format = DateFormat.date_hour_minute)
  private LocalDateTime modifiedAt;

  public void updateViewCount() {
    this.viewCount += 1;
  }

  public void updateRecipe(RecipeUpdateDto recipeUpdateDto) {
    this.title = recipeUpdateDto.getTitle();
    this.intro = recipeUpdateDto.getIntro();
    this.ingredient = recipeUpdateDto.getIngredient();
    this.cookingStep = recipeUpdateDto.getCookingStep();
    this.cookingTime = recipeUpdateDto.getCookingTime();
    this.serving = recipeUpdateDto.getServing();
    this.category = recipeUpdateDto.getCategory();
    this.videoUrl = recipeUpdateDto.getVideoUrl();
  }

}
