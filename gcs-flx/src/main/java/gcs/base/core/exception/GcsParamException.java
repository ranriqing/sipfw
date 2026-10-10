package gcs.base.core.exception;

import gcs.base.core.bean.ParamViolation;
import lombok.Data;

import java.util.List;

@Data
public class GcsParamException extends GcsException {
	private static final long serialVersionUID = 1L;

	List<ParamViolation> violations;

    public GcsParamException(String message, List<ParamViolation> violations) {
		super(message);
		this.violations = violations;
	}

}
