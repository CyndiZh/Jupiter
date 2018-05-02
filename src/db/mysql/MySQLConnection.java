package db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.DBConnection;
import entity.Item;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection {
	private Connection conn;
	
	public MySQLConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver").getConstructor().newInstance();
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		// only insert the first 2 columns
		String sql = "INSERT IGNORE INTO history (user_id, item_id) VALUES (?, ?)";
		
		
		try {
			for (String itemId : itemIds) {
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, userId);	// replace the 1st ? in statement
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			return;
		}
		String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
		
		try {
			for (String itemId : itemIds) {
				PreparedStatement statement = conn.prepareStatement(sql);
				statement.setString(1, userId);	// replace the 1st ? in statement
				statement.setString(2, itemId);
				statement.execute();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		Set<String> favoriteIds = new HashSet<>();
		if (conn == null) {
			return favoriteIds;
		}
		String sql = "SELECT item_id FROM history WHERE user_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, userId);
			
			// get a ResultSet
			ResultSet rs = statement.executeQuery();	
			// format is like: [{"category": "Sports"}{"category": "Music"}{"category": "Art"}]
			
			// loop through ResultSet like iterator
			while (rs.next()) {
				favoriteIds.add(rs.getString("item_id"));
			}	
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteIds;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		
		Set<Item> items = new HashSet<>();
		if (conn == null) {
			return items;
		}
		String sql = "SELECT T1.* FROM items T1 INNER JOIN (SELECT * FROM history WHERE user_id = ?) T2 ON T1.item_id = T2.item_id";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1,  userId);
			
			// get ResultSet
			ResultSet rs = statement.executeQuery();
			
			while (rs.next()) {
				Item.ItemBuilder itbdr = new Item.ItemBuilder();
				itbdr.setItemId(rs.getString("item_id"));
				itbdr.setName(rs.getString("name"));
				itbdr.setRating(rs.getDouble("rating"));
				itbdr.setAddress(rs.getString("address"));
				itbdr.setImageUrl(rs.getString("image_url"));
				itbdr.setUrl(rs.getString("url"));
				itbdr.setDistance(rs.getDouble("distance"));
				// call getCategories()
				itbdr.setCategories(getCategories(rs.getString("item_id")));
				
				items.add(itbdr.build());
			}		
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return items;
		
	}

	@Override
	public Set<String> getCategories(String itemId) {
		Set<String> categories = new HashSet<>();
		if (conn == null) {
			return categories;
		}
		String sql = "SELECT category FROM categories WHERE item_id = ?";
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, itemId);
			
			// get a ResultSet
			ResultSet rs = statement.executeQuery();	
			// format is like: [{"category": "Sports"}{"category": "Music"}{"category": "Art"}]
			
			// loop through ResultSet like iterator
			while (rs.next()) {
				categories.add(rs.getString("category"));
			}	
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return categories;
	}

	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		TicketMasterAPI tmAPI = new TicketMasterAPI();
		List<Item> items = tmAPI.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		if (conn == null) {
			return;
		}
		try {
			// 1) insert into item table
			// IGNORE means only save unique value, to avoid primary key conflict
			String sql = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, item.getItemId());	// replace the 1st ? in statement
			statement.setString(2, item.getName());
			statement.setDouble(3, item.getRating());
			statement.setString(4, item.getAddress());
			statement.setString(5, item.getImageUrl());
			statement.setString(6, item.getUrl());
			statement.setDouble(7, item.getDistance());
			statement.execute();
			
			// 2) update categories table for each category
			sql = "INSERT IGNORE INTO categories VALUES (?, ?)";
			for (String category : item.getCategories()) {
				statement = conn.prepareStatement(sql);
				statement.setString(1, item.getItemId());	// replace the 1st ? in statement
				statement.setString(2, category);
				statement.execute();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public String getFullname(String userId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		// TODO Auto-generated method stub
		return false;
	}

}
