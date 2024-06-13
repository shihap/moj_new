package com.opentext.moj.service;

import com.opentext.moj.entity.Category;
import com.opentext.moj.repository.CategoryRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * get category json list
     * @return category json list
     */
    public String getCategories() {
        return constructCategories(categoryRepository.getCategories());
    }

    /**
     * construct category list to specific format
     * @param categoryList list of categories from database
     * @return constructed category json
     */
    private String constructCategories(List<Category> categoryList) {
        Map<Long, JSONObject> resultMap = new HashMap<>();

        for (Category category : categoryList) {

            JSONObject properties;
            if (resultMap.containsKey(category.getCatId())) {
                properties = resultMap.get(category.getCatId()).getJSONObject("properties");
            } else {
                properties = new JSONObject();
            }
            properties.put(category.getAttrName(), category.getAttrId().substring(5));

            JSONObject newEntry = new JSONObject();
            newEntry.put("id", category.getCatId());
            newEntry.put("name", category.getCatName());
            newEntry.put("properties", properties);

            resultMap.put(category.getCatId(), newEntry);
        }

        JSONArray reconstructedArray = new JSONArray();
        for (JSONObject entry : resultMap.values()) {
            reconstructedArray.put(entry);
        }

        return reconstructedArray.toString();
    }
}
