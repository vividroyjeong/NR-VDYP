package ca.bc.gov.nrs.vdyp.common_calculators.custom_exceptions;

public abstract class CommonCalculatorException extends Exception {

	private static final long serialVersionUID = -2731778673508981452L;

	public CommonCalculatorException(String message) {
		super(message);
	}

	public CommonCalculatorException(Throwable cause) {
		super(cause);
	}

	public CommonCalculatorException(String message, Throwable cause) {
		super(cause);
	}

	public CommonCalculatorException() {
		super();
	}
}
