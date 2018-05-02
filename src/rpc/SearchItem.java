package rpc;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
import external.TicketMasterAPI;

/**
 * Servlet implementation class SearchItem
 */
@WebServlet("/search")
public class SearchItem extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchItem() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String userId = request.getParameter("user_id");
		
		double lat = Double.parseDouble(request.getParameter("lat"));
		double lon = Double.parseDouble(request.getParameter("lon"));
		String keyword = request.getParameter("term");	// term can be empty
	
		
		DBConnection connection = DBConnectionFactory.getConnection();
		List<Item> items = connection.searchItems(lat, lon, keyword);
		Set<String> favorite = connection.getFavoriteItemIds(userId);
		connection.close(); 

		// TicketMasterAPI tmAPI = new TicketMasterAPI();
		// List<Item> items = tmAPI.search(lat, lon, keyword);
		
		JSONArray array = new JSONArray();
		try {
			for (Item item: items) {
				JSONObject object = item.toJSONObject(); 
				// check whether the item from search result is already a favorite of this customer
				object.put("favorite", favorite.contains(item.getItemId()));	// reserve for FrontEnd
				array.put(object);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		RpcHelper.writeJsonArray(response, array);
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

}
