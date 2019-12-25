package my.windows;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * Utility class for managing the windows registry.
 * 
 * @author stephan
 *
 */
public enum Reg {
	INSTANCE;

	private static final int SUCCESS = 0;
	private static final int ERROR = 1;

	private Reg() {

	}

	public static boolean exists(String key) {
		ExecutionResult er = reg("query", "key");

		if (er.getExitCode() == SUCCESS) {
			return true;
		} else {
			// FIXME: Parse returned and determine if key doesn't exist or an
			// error occurred...
			return false;
		}
	}

	public static void copy(String sourceKey, String targetKey) {
		copy(sourceKey, targetKey, false);
	}

	public static void copy(String sourceKey, String targetKey, boolean overwriteTargetKeyIfExists) {
		ExecutionResult er = reg("copy", sourceKey, targetKey, overwriteTargetKeyIfExists ? "/f" : "");

		if (er.getExitCode() != SUCCESS) {
			// FIXME: Handle problem...
		}
	}

	public static void delete(String originalKey) {
		// TODO Auto-generated method stub

	}

	private static ExecutionResult reg(String... params) {
return null;
	}

	class ExecutionException extends Exception {

		private static final long serialVersionUID = 2908933347459177769L;

	}

	class ExecutionResult {
		@Getter
		private int exitCode;
	}
}
