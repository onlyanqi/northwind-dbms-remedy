# northwind-dbms-remedy
## Purpose
Make some meaningful changes to a database.
## Problems
The Northwind food distribution company decided to improve its inventory management system. The database has been recognized the following deficiencies:
* The company doesn’t have a history of the purchases made from suppliers or the price
at which it bought its products.
* The company doesn’t know when it placed an order to restock its products.
* The company doesn’t know how frequently it reorders from its suppliers so it can’t know if its current reorder levels are good.
* The process of reordering products is currently done manually.
* There is no clean way for the company to record when products arrive from suppliers.

## Tasks
1. Modify the database so that we can track orders from suppliers, know the cost at which we bought the product, know when the products have arrived, know who will be delivering the product to us, and have a reference to track the product while it is in transit.
2. Record when an order for one of our own clients is shipped, update the inventory, and automatically trigger a reorder from the supplier, if necessary, knowing that the number to reorder can vary by product. We only want one reorder to be sent at the end of the day so we’ll have two methods:
* a. Ship_order – happens for each order that we send out
* b. Issue_reorders – happens once per day where we place orders to our own suppliers; we only want at most one order to suppliers each day.
3. Record when we receive an order from one of our suppliers.
