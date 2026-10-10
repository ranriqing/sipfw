package gcs.base.core.exception;

import gcs.base.core.bean.GcsState;
import lombok.Data;

@Data
public class GcsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private String code = GcsState.FAILED.getCode();
    private String message;

    
    public GcsException() {
	}

    public GcsException(String message) {
		super(message);
		this.message = message;
	}
	
	public GcsException(String message, Throwable e) {
		super(message, e);
		this.message = message;
	}
	
	public GcsException(String code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}
	
	public GcsException(String code, String message, Throwable e) {
		super(message, e);
		this.code = code;
		this.message = message;
	}

}
