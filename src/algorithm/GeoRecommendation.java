package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

public class GeoRecommendation {
	public List<Item> recommendItems(String userId, double lat, double lon) {
		List<Item> recommendedItems = new ArrayList<>();

		DBConnection conn = DBConnectionFactory.getConnection();
		if (conn == null) {
			return recommendedItems;
		}
		// Step 1 Get all favorited itemIds
		Set<String> favoriteItemIds = conn.getFavoriteItemIds(userId);
		
		// Step 2 Get all categories of favorited items
		Map<String, Integer> allCategories = new HashMap<>();

		for (String itemId : favoriteItemIds) {
			Set<String> categories = conn.getCategories(itemId);
			for (String category : categories) {
				if (! allCategories.containsKey(category)) {
					allCategories.put(category, 1);
				}
				allCategories.put(category, allCategories.get(category) + 1);
			}
		}
		
		List<Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
		Collections.sort(categoryList, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return Integer.compare(o2.getValue(), o1.getValue());
			}
		});
		
		// Step 3 Do search based on catgory, filer out favorited items, sort by
		// distance
		Set<String> visitedItemId = new HashSet<>();
		for (Entry<String, Integer> categoryEntry : categoryList) {
			List<Item> searchResultItem = conn.searchItems(lat, lon, categoryEntry.getKey());
			List<Item> filteredItem = new ArrayList<>();
			for (Item curItem : searchResultItem) {
				if (! visitedItemId.contains(curItem.getItemId()) && ! favoriteItemIds.contains(curItem.getItemId())) {
					visitedItemId.add(curItem.getItemId());
					filteredItem.add(curItem);
				}
			}
			
			Collections.sort(filteredItem, new Comparator<Item>() {
				@Override
				public int compare(Item o1, Item o2) {
					return Double.compare(o1.getDistance(), o2.getDistance());
				}
			});
			recommendedItems.addAll(filteredItem);
		}
		
		conn.close();
		return recommendedItems;
	}

}
