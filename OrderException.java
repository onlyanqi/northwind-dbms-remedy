public class OrderException extends Exception {
	private int OrderReferenceNumber = 0;

	public OrderException(String Exception) {
		super(Exception);
		OrderReferenceNumber = 0;
	}

	public OrderException(String Exception, int referenceNumber) {
		super(Exception);
		OrderReferenceNumber = referenceNumber;
	}

	public int getReference() {
		return OrderReferenceNumber;
	}
}