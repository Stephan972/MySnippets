package my;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ExitCode {
	SUCCESS(0), ERROR(1);

	private int value;
}
