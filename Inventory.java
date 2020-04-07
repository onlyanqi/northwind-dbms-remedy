import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class Inventory implements InventoryControl {
	Connection connect = null;
	PreparedStatement ps = null;
	PreparedStatement ps1 = null;
	PreparedStatement ps2 = null;
	PreparedStatement reorder = null;
	PreparedStatement suppliercount = null;
	ResultSet rs = null;
	ResultSet rs1 = null;
	ResultSet rs3 = null;

	int count = 0;
	int productID;
	int unitsInStock;
	int unitsOrdered;
	String date;
	int daysale;
	int pID;
	int day_end_inv;
	int sale_end_inv;
	int day_start_inv;
	int reordered_units;
	int reorder_level;
	int counter = 0;
	int unit_on_order;
	int temp = 0;
	int discontinued;
	int sum;
	float cost;
	double unitcost;
	int internal_order_reference = 0;
	int sID;
	int units;
	int unitins;
	int add;

	void getconnection(Connection connect) {
		this.connect = connect;
	}

	public void Ship_order(int orderNumber) throws OrderException {
		int oid = orderNumber;
		try {

			// Update the shipped date as the current date when we call this method

			ps = connect.prepareStatement("update orders set ShippedDate= curdate() where OrderID='" + oid + "';");
			ps.executeUpdate();

			// Select the information details of all the products belonging to this order

			ps2 = connect.prepareStatement(
					"select od.ProductID as Product_id, p.UnitsInStock as UnitsinStock , od.Quantity as Ordered_Quantity "
							+ "from orderdetails as od, products as p "
							+ "where od.OrderID= ? and od.ProductID=p.ProductID;");
			ps2.setInt(1, orderNumber);
			rs = ps2.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int colCount = rsmd.getColumnCount();

			// Throw a new exception if nothing is selected in the result set

			if (!rs.next()) {
				throw new OrderException("Unable to ship a order because the order ID is not in the Database");
			} else {

				// For each product, update the number of units in stock once shipped away

				while (rs.next()) {
					for (int i = 1; i < colCount; i++) {
						String columnName = rsmd.getColumnLabel(i);
						if (columnName.equals("Product_id")) {
							productID = rs.getInt(i);
						} else if (columnName.equals("UnitsinStock")) {
							unitsInStock = rs.getInt(i);
						} else if (columnName.equals("Ordered_Quantity")) {
							unitsOrdered = rs.getInt(i);
						}
					}
					if (unitsInStock >= unitsOrdered) {
						int newStock = unitsInStock - unitsOrdered;
						ps1 = connect.prepareStatement("update products set UnitsinStock= '" + newStock
								+ "' where ProductID='" + productID + "'");
						ps1.executeUpdate();
					}

					// When the units in stock are less than units ordered, throw a new
					// OrderException

					else {
						throw new OrderException(
								"Unable to ship a order because the number of units in stock is less than the ordered quantity");
					}
				}
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	public int Issue_reorders(int year, int month, int day) {
		String yearString = Integer.toString(year);
		String monthString = Integer.toString(month);
		String dayString = Integer.toString(day);
		String dateNumber = yearString + monthString + dayString;
		String dateString = yearString + "-" + monthString + "-" + dayString;

		// Select the information details of all the products reordered on that day

		try {
			reorder = connect.prepareStatement(
					"select p.productid as Product_ID,p.SupplierID as SupplierID,p.UnitsInStock as Day_End_Inventory, p.UnitsOnOrder as Reordered_Units, p.reorderlevel as Reorder_Level, o.shippeddate as Shipped_Date, p.Discontinued as Discontinued "
							+ "from products p join orderdetails od using (productid) "
							+ "join orders o using (orderid) " + "where o.ShippedDate is not null "
							+ "and o.ShippedDate = '" + dateString + "' " + "group by p.productid,o.shippeddate "
							+ "order by p.productid,o.shippeddate desc;");
			rs1 = reorder.executeQuery();
			ResultSetMetaData rsmd1 = rs1.getMetaData();
			int colCount = rsmd1.getColumnCount();

			// For each product, store all the information necessary for table Reorder as
			// elements

			while (rs1.next()) {
				temp = sID;
				for (int i = 1; i <= colCount; i++) {
					String columnName = rsmd1.getColumnLabel(i);
					if (columnName.equals("Product_ID")) {
						pID = rs1.getInt(i);
					} else if (columnName.equals("SupplierID")) {
						sID = rs1.getInt(i);
					} else if (columnName.equals("Day_End_Inventory")) {
						day_end_inv = rs1.getInt(i);
					} else if (columnName.equals("Reordered_Units")) {
						reordered_units = rs1.getInt(i);
					} else if (columnName.equals("Reorder_Level")) {
						reorder_level = rs1.getInt(i);
					} else if (columnName.equals("Shipped_Date")) {
						date = rs1.getString(i);
					} else if (columnName.equals("Day_Sales")) {
						daysale = rs1.getInt(i);
					} else if (columnName.equals("Discontinued")) {
						discontinued = rs1.getInt(i);
					}
				}
				String p_id = Integer.toString(pID);

				// Create internal order reference through the date and product id

				String ior = dateNumber + p_id;
				internal_order_reference = Integer.parseInt(ior);

				// For those products that will continue to be ordered but without any unit on order

				if (reordered_units == 0 && discontinued != 1) {

					// If the reorder level is empty, set it as 1/4 of the stock units

					if (reorder_level == 0) {
						reorder_level = (day_end_inv / 4);
					}

					// If the stock is less than four times of reorder level,
					// then set the reorder units to make up the gap

					if (day_end_inv <= (reorder_level * 4)) {
						int sum = (reorder_level * 4) - day_end_inv;
						PreparedStatement ps3 = connect.prepareStatement(
								"insert into Reorder (Internal_order_reference, ProductID, SupplierID, OrderedDate, Reordered_units, Order_status) "
										+ "values ('" + internal_order_reference + "','" + pID + "','" + sID + "','"
										+ date + "','" + sum + "','ordered');");
						ps3.executeUpdate();
					}
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
		try {

			// get the number of suppliers from whom we will be placing an order

			suppliercount = connect.prepareStatement("select count(distinct(SupplierID)) as count from Reorder;");
			rs3 = suppliercount.executeQuery();
			ResultSetMetaData rsmd2 = rs3.getMetaData();
			int colcount = rsmd2.getColumnCount();
			while (rs3.next()) {
				for (int i = 1; i <= colcount; i++) {
					String columnName = rsmd2.getColumnLabel(i);
					if (columnName.equals("count")) {
						count = rs3.getInt(i);
					}
				}
			}
			System.out.println(count);
		} catch (Exception ex) {
			System.out.println(ex);
		}
		return count;
	}

	public void Receive_order(int reference) throws OrderException {
		int id = reference;
		int pid = 0;
		try {
			PreparedStatement receive = null;
			ResultSet rs4 = null;
			PreparedStatement prpstmt = null;

			// Update the Order_status in Reorder table when receiving a order

			receive = connect.prepareStatement(
					"update Reorder set Order_status='Received' where Internal_order_reference=" + id + ";");
			receive.executeUpdate();

			// Select the units quantity information from Reorder

			prpstmt = connect.prepareStatement("select ProductID as ProductID,Reordered_Units as reordered_units "
					+ "from Reorder where Internal_order_reference=" + id + ";");
			rs4 = prpstmt.executeQuery();
			ResultSetMetaData rsmd = rs4.getMetaData();
			int colcount = rsmd.getColumnCount();
			
			// Get the number of reordered units
			
			if (!rs4.next()) {
				throw new OrderException("Unable to receive a order because the internal order reference is invalid",
						reference);
			} else {
				while (rs4.next()) {
					for (int i = 1; i <= colcount; i++) {
						String columnName = rsmd.getColumnLabel(i);
						if (columnName.equals("ProductID")) {
							pid = rs4.getInt(i);
						}
						if (columnName.equals("reordered_units")) {
							units = rs4.getInt(i);
						}
					}
					
					// Get the number of units in stock 
					
					PreparedStatement prd = connect.prepareStatement(
							"select UnitsInStock as stockUnits from products where ProductID=" + pid + ";");
					ResultSet rs5 = prd.executeQuery();
					ResultSetMetaData rsmd3 = rs5.getMetaData();
					int colcount1 = rsmd3.getColumnCount();
					while (rs5.next()) {
						for (int i = 1; i <= colcount1; i++) {
							String columnName = rsmd3.getColumnLabel(i);
							if (columnName.equals("stockUnits")) {
								unitins = rs5.getInt(i);
							}
						}
					}
					
					// Update the number of stock units
					
					add = units + unitins;
					PreparedStatement prpst = connect.prepareStatement(
							"update products set UnitsInStock=" + add + " where ProductID=" + pid + ";");
					prpst.executeUpdate();
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		}
	}
}