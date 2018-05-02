package external;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class TicketMasterAPI {
	private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
	private static final String DEFAULT_KEYWORD = ""; // no restriction
	private static final String API_KEY = "25K5nfAyne9BRYn3BmkFogy1Lmk41ra0";
	
    public List<Item> search(double lat, double lon, String keyword) {
    	if (keyword == null) {
    		keyword = DEFAULT_KEYWORD;
    	}
    	
    	try {
    		keyword = java.net.URLEncoder.encode(keyword,  "UTF-8");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	String geoHash = GeoHash.encodeGeohash(lat,  lon,  8);
    	String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s", API_KEY, geoHash, keyword, 50);

    	try {
    		// generate a connector to send request and receive response
    		HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();
    		connection.setRequestMethod("GET");
    		// System.out.println("Sending GET to URL: " + URL);	// debug
    		
    		// connection.getResponseCode()： 200 means OK
    		// this can be used to debug
    		int responseCode = connection.getResponseCode();
    		// System.out.println(responseCode);		// debug
    		
    		// BufferedReader reads it line by line
    		// connection.getInputStream(): get response information from server
    		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    		String inputLine;
    		StringBuilder response = new StringBuilder();
    		
    		while ((inputLine = in.readLine()) != null) {
    			response.append(inputLine);	// response: append each line together
    		}
    		in.close();
    		
    		// constructor can build from string to key-value pair
    		JSONObject obj = new JSONObject(response.toString());

    		if (obj.isNull("_embedded")) {
        		return new ArrayList<>();
        	}
    		JSONObject embedded = obj.getJSONObject("_embedded");
    		if (embedded.isNull("events")) {
    			return new ArrayList<>();
    		} else {
    			JSONArray events = embedded.getJSONArray("events");
        		return getItemList(events);
    		}
    		
    		
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
		return new ArrayList<>();    	
    }
    
    // print query result to print JSON array
    // a helper method used for debugging
	private void queryAPI(double lat, double lon) {
		List<Item> events = search(lat, lon, null);
		try {
		    for (Item item: events) {
		        JSONObject obj = item.toJSONObject();
		        System.out.println(obj);	// print into console
		    }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main entry for sample TicketMaster API requests.
	 */
	public static void main(String[] args) {
		TicketMasterAPI tmApi = new TicketMasterAPI();
		// Mountain View, CA
		// tmApi.queryAPI(37.38, -122.08);
		// London, UK
		// tmApi.queryAPI(51.503364, -0.12);
		// Houston, TX
		tmApi.queryAPI(29.682684, -95.295410);
	}
	
	
	
	/**
	 * Helper methods
	 */
	// sample structure: {key: value}
	//  {
	//    "name": "laioffer",
	//    		"id": "12345",
	//    		"url": "www.laioffer.com",
	//    ...
	//    "_embedded": {
	//	    "venues": [
	//	        {
	//		        "address": {
	//		           "line1": "101 First St,",
	//		           "line2": "Suite 101",
	//		           "line3": "...",
	//		        },
	//		        "city": {
	//		        	"name": "San Francisco"
	//		        }
	//		        ...
	//	        },
	//	        ...
	//	    ]
	//    }
	//    ...
	//  }

	private String getAddress(JSONObject event) throws JSONException {
		if (! event.isNull("_embedded")) {
			JSONObject embedded = event.getJSONObject("_embedded");
			if (! embedded.isNull("venues")) {
				JSONArray venues = embedded.getJSONArray("venues");
				for (int i = 0; i < venues.length(); i++) {
					JSONObject venObj = venues.getJSONObject(i);
					StringBuilder sb = new StringBuilder();
					if (! venObj.isNull("address")) {
						JSONObject addObj = venObj.getJSONObject("address");
						if (! addObj.isNull("line1")) {
							sb.append(addObj.getString("line1")); 
						}
						if (! addObj.isNull("line2")) {
							sb.append(addObj.getString("line2"));
						}
						if (! addObj.isNull("line3")) {
							sb.append(addObj.getString("line3"));
						}
					}
					sb.append(", ");
					if (! venObj.isNull("city")) {
						JSONObject cityObj = venObj.getJSONObject("city");
						if (! cityObj.isNull("name")) {
							sb.append(cityObj.getString("name")); 
						}
					}
						
					if (sb.length() > 0) {	// else -> continue going into the next loop
						return sb.toString();
					}
				}
			}
		}
				
		return "";
	}


	// {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
	private String getImageUrl(JSONObject event) throws JSONException {
		if (! event.isNull("images")) {
			JSONArray img = event.getJSONArray("images");
			String urlString = new String();
			for (int i = 0; i < img.length(); i++) {
				JSONObject imgObj = img.getJSONObject(i);
				if (! imgObj.isNull("url")) {
					return imgObj.getString("url");
				}
			}
		}
		return "";
	}

	// {"classifications" : [{"segment": {"name": "music"}}, ...]}
	private Set<String> getCategories(JSONObject event) throws JSONException {
		Set<String> categories = new HashSet<>();
		if (! event.isNull("classifications")) {
			JSONArray classfctn = event.getJSONArray("classifications");
			for (int i = 0; i < classfctn.length(); i++) {
				JSONObject classfctnObj= classfctn.getJSONObject(i);
				if (! classfctnObj.isNull("segment")) {
					JSONObject segObj = classfctnObj.getJSONObject("segment");
					if ( ! segObj.isNull("name")) {
						categories.add(segObj.getString("name"));
					}
				}
			}
		}
		return categories;
	}

	// Convert JSONArray to a list of item objects.
	// Response Structure: https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/#search-events-v2
	private List<Item> getItemList(JSONArray events) throws JSONException {
		List<Item> itemList = new ArrayList<>();
		
		for (int i = 0; i < events.length(); i++) {
			JSONObject event = events.getJSONObject(i);
			
			ItemBuilder builder = new ItemBuilder();
			if (! event.isNull("name")) {	// whether key "name" exists in object event
				builder.setName(event.getString("name"));
			}
			if (! event.isNull("id")) {
				builder.setItemId(event.getString("id"));
			}
			if (! event.isNull("url")) {
				builder.setUrl(event.getString("url"));
			}
			if (! event.isNull("rating")) {
				builder.setRating(event.getDouble("rating"));
			}
			if (! event.isNull("distance")) {
				builder.setDistance(event.getDouble("distance"));
			}
			builder.setCategories(getCategories(event));
			builder.setImageUrl(getImageUrl(event));
			builder.setAddress(getAddress(event));
			
			itemList.add(builder.build());
		}
		return itemList;
	}

}
