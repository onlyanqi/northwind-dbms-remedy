import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;

public class PurchaseHistory {
	PreparedStatement ps = null;
	PreparedStatement ps1 = null;
	ResultSet rs = null;
	Connection connect;

	String day;
	int daysale;
	int pID;
	int sID;
	int day_end_inv;
	int sale_end_inv;
	int day_start_inv;
	int reordered_units;
	int reorder_level;
	int counter = 0;
	int unit_on_order;
	int temp = 0;
	int sum;
	float cost;
	double unitcost;

	ArrayList<Integer> product_id = new ArrayList<Integer>();

	public void buildhistory(Connection connect) {
		this.connect = connect;
		try {

			// Select the useful information to for a history of purchases

			ps = connect.prepareStatement(
					"select p.productid as ProductID,p.SupplierID as SupplierID,p.unitsinstock as Day_End_Inventory, p.unitsonorder as Reordered_Units, p.reorderlevel as Reorder_Level, o.shippeddate as Shipped_Date, sum(od.quantity) as Day_Sales, sum(od.unitprice) as Product_Cost "
							+ "from products p join orderdetails od using (productid) "
							+ "join orders o using (orderid) " + "where o.ShippedDate is not null "
							+ "group by p.productid,o.shippeddate " + "order by p.productid,o.shippeddate desc;");

			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int colNum = rsmd.getColumnCount();

			while (rs.next()) {
				temp = pID;
				for (int i = 1; i <= colNum; i++) {
					String columnName = rsmd.getColumnLabel(i);
					if (columnName.equals("ProductID")) {
						pID = rs.getInt(i);
					} else if (columnName.equals("SupplierID")) {
						sID = rs.getInt(i);
					} else if (columnName.equals("Day_End_Inventory")) {
						day_end_inv = rs.getInt(i);
					} else if (columnName.equals("Reordered_Units")) {
						reordered_units = rs.getInt(i);
						unit_on_order = reordered_units;
					} else if (columnName.equals("Reorder_Level")) {
						reorder_level = rs.getInt(i);
					} else if (columnName.equals("Shipped_Date")) {
						day = rs.getString(i);
					} else if (columnName.equals("Day_Sales")) {
						daysale = rs.getInt(i);
					} else if (columnName.equals("Product_Cost")) {

						// Estimate the item cost using the standard 15% markup on the price

						cost = rs.getInt(i);
						unitcost = (cost / 1.15);
					}
				}

				// if there is no sale in the day

				if (unit_on_order == 0 && reorder_level > 0) {
					if (temp != pID) {

						// define sale_end_inv as day_end_inv before the reorder operation
						sale_end_inv = day_end_inv - reordered_units;
						if (day_end_inv > (reorder_level * 4)) {
							sale_end_inv = reorder_level;
							reordered_units = day_end_inv - reorder_level;
						} else {
							sale_end_inv = day_end_inv;
						}
					} else {
						day_end_inv = day_start_inv;
						if (day_end_inv > (reorder_level * 4)) {
							sale_end_inv = reorder_level;
							// enrich the stock
							reordered_units = day_end_inv - reorder_level;
						} else {
							sale_end_inv = day_end_inv;
						}
					}
					day_start_inv = sale_end_inv + daysale;
					ps1 = connect.prepareStatement(
							"insert into Purchase_History (ProductID,SupplierID, ReorderedDate,ArrivedDate,Day_End_Inventory,Reordered_Units,Sale_End_Inventory,Day_sales,Day_start_Inventory, Unit_Price) values ('"
									+ pID + "','" + sID + "','" + day + "','" + day + "','" + day_end_inv + "','"
									+ reordered_units + "','" + sale_end_inv + "','" + daysale + "','" + day_start_inv
									+ "','" + unitcost + "');");
					ps1.executeUpdate();
				}

				// if the products have been sold and should be reordered

				if (unit_on_order > 0 && reorder_level > 0) {
					if (temp != pID) {
						sum = day_end_inv + reordered_units;
						if (sum > (reorder_level * 4)) {
							sale_end_inv = reorder_level;

							// the number of reorder is the number of (inventory after reordered)- sold

							reordered_units = sum - sale_end_inv;
						} else {
							sale_end_inv = day_end_inv;
						}
					} else {
						sum = day_start_inv;
						if (sum > (reorder_level * 4)) {
							sale_end_inv = reorder_level;
							reordered_units = sum - sale_end_inv;
						} else {

							// no need to reorder

							sale_end_inv = sum;
							reordered_units = 0;
						}
					}
					day_start_inv = sale_end_inv + daysale;
					ps1 = connect.prepareStatement(
							"insert into Purchase_History (ProductID,SupplierID, ReorderedDate,ArrivedDate,Day_End_Inventory,Reordered_Units,Sale_End_Inventory,Day_sales,Day_start_Inventory,Unit_Price) "
									+ "values ('" + pID + "','" + sID + "','" + day + "','" + day + "','" + sum + "','"
									+ reordered_units + "','" + sale_end_inv + "','" + daysale + "','" + day_start_inv
									+ "','" + unitcost + "');");
					ps1.executeUpdate();
				}

				// keep the same inventory

				if (unit_on_order == 0 && reorder_level == 0) {
					reorder_level = (day_end_inv / 4);
					if (temp != pID) {
						day_end_inv = day_end_inv + reordered_units;
						if (day_end_inv > (reorder_level * 4)) {
							sale_end_inv = reorder_level;
							reordered_units = day_end_inv - reorder_level;
						} else {

							// nothing is sold

							sale_end_inv = day_end_inv;
						}
					} else {
						day_end_inv = day_start_inv;
						if (day_end_inv > (reorder_level * 4)) {
							sale_end_inv = reorder_level;
							reordered_units = day_end_inv - reorder_level;
						} else {
							sale_end_inv = day_end_inv;
						}
					}
					day_start_inv = sale_end_inv + daysale;
					ps1 = connect.prepareStatement(
							"insert into Purchase_History (ProductID,SupplierID, ReorderedDate,ArrivedDate,Day_End_Inventory,Reordered_Units,Sale_End_Inventory,Day_sales,Day_start_Inventory, Unit_Price) "
									+ "values ('" + pID + "','" + sID + "','" + day + "','" + day + "','" + day_end_inv
									+ "','" + reordered_units + "','" + sale_end_inv + "','" + daysale + "','"
									+ day_start_inv + "','" + unitcost + "');");
					ps1.executeUpdate();
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

}