package my.webdriver;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

//TODO: Add HTTP response headers
@RequiredArgsConstructor
public class AjaxResponse {
	@NonNull
	@Getter
	private String body;
}
